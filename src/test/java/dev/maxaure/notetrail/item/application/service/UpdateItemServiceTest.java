package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.ForbiddenException;
import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.item.application.UpdateItemCommand;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemPermission;
import dev.maxaure.notetrail.item.domain.ItemRole;
import dev.maxaure.notetrail.item.domain.error.VersionConflictException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateItemServiceTest {

    private static final UUID ITEM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EDITOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID VIEWER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID STRANGER_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-01-01T11:00:00Z");
    private static final Clock CLOCK = Clock.fixed(UPDATED_AT, ZoneOffset.UTC);

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final ItemPermissionRepository permissionRepository =
            mock(ItemPermissionRepository.class);
    private final UpdateItemService service = new UpdateItemService(
            itemRepository,
            new ItemAccessPolicy(permissionRepository),
            CLOCK
    );

    @Test
    void whenOwnerUpdatesThenItemIsSaved() {
        givenItem(0);
        givenSavedItem();

        Item updated = service.update(new UpdateItemCommand(
                ITEM_ID,
                OWNER_ID,
                "new title",
                "new content",
                0
        ));

        assertEquals("new title", updated.title());
        assertEquals("new content", updated.content());
        assertEquals(UPDATED_AT, updated.updatedAt());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void whenEditorUpdatesThenItemIsSaved() {
        givenItem(0);
        givenPermission(EDITOR_ID, ItemRole.EDITOR);
        givenSavedItem();

        Item updated = service.update(new UpdateItemCommand(
                ITEM_ID,
                EDITOR_ID,
                "new title",
                "new content",
                0
        ));

        assertEquals("new title", updated.title());
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void whenViewerUpdatesThenItemIsNotSaved() {
        givenItem(0);
        givenPermission(VIEWER_ID, ItemRole.VIEWER);

        assertThrows(
                ForbiddenException.class,
                () -> service.update(new UpdateItemCommand(
                        ITEM_ID,
                        VIEWER_ID,
                        "new title",
                        "new content",
                        0
                ))
        );
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void whenStrangerUpdatesThenItemIsNotSaved() {
        givenItem(0);

        assertThrows(
                ForbiddenException.class,
                () -> service.update(new UpdateItemCommand(
                        ITEM_ID,
                        STRANGER_ID,
                        "new title",
                        "new content",
                        0
                ))
        );
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void whenVersionDoesNotMatchThenConflictContainsCurrentVersion() {
        givenItem(2);

        VersionConflictException exception = assertThrows(
                VersionConflictException.class,
                () -> service.update(new UpdateItemCommand(
                        ITEM_ID,
                        OWNER_ID,
                        "new title",
                        "new content",
                        1
                ))
        );

        assertEquals(2, exception.currentVersion());
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void whenItemDoesNotExistThenNotFoundIsThrown() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> service.update(new UpdateItemCommand(
                        ITEM_ID,
                        OWNER_ID,
                        "new title",
                        "new content",
                        0
                ))
        );
    }

    @Test
    void whenContentIsNullThenOldContentIsKept() {
        givenItem(0);
        givenSavedItem();

        Item updated = service.update(new UpdateItemCommand(
                ITEM_ID,
                OWNER_ID,
                "new title",
                null,
                0
        ));

        assertEquals("new title", updated.title());
        assertEquals("old content", updated.content());
    }

    @Test
    void whenTitleIsNullThenOldTitleIsKept() {
        givenItem(0);
        givenSavedItem();

        Item updated = service.update(new UpdateItemCommand(
                ITEM_ID,
                OWNER_ID,
                null,
                "new content",
                0
        ));

        assertEquals("old title", updated.title());
        assertEquals("new content", updated.content());
    }

    private void givenItem(int version) {
        Item item = new Item(
                ITEM_ID,
                OWNER_ID,
                "old title",
                "old content",
                version,
                false,
                CREATED_AT,
                CREATED_AT
        );
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
    }

    private void givenPermission(UUID userId, ItemRole role) {
        when(permissionRepository.find(ITEM_ID, userId))
                .thenReturn(Optional.of(new ItemPermission(ITEM_ID, userId, role, CREATED_AT)));
    }

    private void givenSavedItem() {
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
