package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.ForbiddenException;
import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.core.error.ValidationException;
import dev.maxaure.notetrail.item.application.ShareItemCommand;
import dev.maxaure.notetrail.item.application.ShareOutcome;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.application.port.out.UserLookup;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemPermission;
import dev.maxaure.notetrail.item.domain.ItemRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ShareItemServiceTest {

    private static final UUID ITEM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID STRANGER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant GRANTED_AT = Instant.parse("2026-01-01T11:00:00Z");

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final ItemPermissionRepository permissionRepository = mock(ItemPermissionRepository.class);
    private final UserLookup userLookup = mock(UserLookup.class);
    private final ShareItemService service = new ShareItemService(
            itemRepository,
            permissionRepository,
            userLookup,
            Clock.fixed(GRANTED_AT, ZoneOffset.UTC)
    );

    @Test
    void whenOwnerSharesWithNewUserThenPermissionIsCreated() {
        givenItem();
        when(userLookup.existsById(TARGET_ID)).thenReturn(true);
        when(permissionRepository.save(any(ItemPermission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShareOutcome outcome = service.share(new ShareItemCommand(ITEM_ID, OWNER_ID, TARGET_ID, ItemRole.EDITOR));

        assertTrue(outcome.created());
        assertEquals(ItemRole.EDITOR, outcome.permission().role());
        assertEquals(GRANTED_AT, outcome.permission().grantedAt());
        verify(permissionRepository).save(any(ItemPermission.class));
    }

    @Test
    void whenTargetAlreadyHasAccessThenRoleIsUpdated() {
        givenItem();
        when(userLookup.existsById(TARGET_ID)).thenReturn(true);
        when(permissionRepository.find(ITEM_ID, TARGET_ID)).thenReturn(Optional.of(
                new ItemPermission(ITEM_ID, TARGET_ID, ItemRole.VIEWER, CREATED_AT)
        ));
        when(permissionRepository.save(any(ItemPermission.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShareOutcome outcome = service.share(new ShareItemCommand(ITEM_ID, OWNER_ID, TARGET_ID, ItemRole.EDITOR));

        assertFalse(outcome.created());
        assertEquals(ItemRole.EDITOR, outcome.permission().role());
        assertEquals(CREATED_AT, outcome.permission().grantedAt());
    }

    @Test
    void whenOwnerSharesWithSelfThenValidationFails() {
        givenItem();

        assertThrows(
                ValidationException.class,
                () -> service.share(new ShareItemCommand(ITEM_ID, OWNER_ID, OWNER_ID, ItemRole.VIEWER))
        );
        verify(permissionRepository, never()).save(any(ItemPermission.class));
    }

    @Test
    void whenTargetUserDoesNotExistThenNotFoundIsThrown() {
        givenItem();
        when(userLookup.existsById(TARGET_ID)).thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> service.share(new ShareItemCommand(ITEM_ID, OWNER_ID, TARGET_ID, ItemRole.VIEWER))
        );
        verify(permissionRepository, never()).save(any(ItemPermission.class));
    }

    @Test
    void whenRequesterIsNotOwnerThenForbiddenIsThrown() {
        givenItem();

        assertThrows(
                ForbiddenException.class,
                () -> service.share(new ShareItemCommand(ITEM_ID, STRANGER_ID, TARGET_ID, ItemRole.VIEWER))
        );
        verifyNoInteractions(userLookup);
        verify(permissionRepository, never()).save(any(ItemPermission.class));
    }

    @Test
    void whenItemDoesNotExistThenNotFoundIsThrown() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.share(new ShareItemCommand(ITEM_ID, OWNER_ID, TARGET_ID, ItemRole.VIEWER))
        );
    }

    private void givenItem() {
        Item item = new Item(ITEM_ID, OWNER_ID, "title", "content", 0, false, CREATED_AT, CREATED_AT);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
    }
}
