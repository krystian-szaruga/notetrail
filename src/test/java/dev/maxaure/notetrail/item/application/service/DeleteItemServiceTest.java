package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.ForbiddenException;
import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteItemServiceTest {

    private static final UUID ITEM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OWNER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID STRANGER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant DELETED_AT = Instant.parse("2026-01-01T11:00:00Z");

    private final ItemRepository itemRepository = mock(ItemRepository.class);
    private final DeleteItemService service = new DeleteItemService(
            itemRepository,
            Clock.fixed(DELETED_AT, ZoneOffset.UTC)
    );

    @Test
    void whenOwnerDeletesThenItemIsSoftDeleted() {
        givenItem();
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(ITEM_ID, OWNER_ID);

        verify(itemRepository).save(argThat(item ->
                item.deleted() && item.updatedAt().equals(DELETED_AT)
        ));
    }

    @Test
    void whenRequesterIsNotOwnerThenItemIsNotSaved() {
        givenItem();

        assertThrows(ForbiddenException.class, () -> service.delete(ITEM_ID, STRANGER_ID));
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void whenItemDoesNotExistThenNotFoundIsThrown() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.delete(ITEM_ID, OWNER_ID));
        verify(itemRepository, never()).save(any(Item.class));
    }

    private void givenItem() {
        Item item = new Item(
                ITEM_ID,
                OWNER_ID,
                "title",
                "content",
                0,
                false,
                CREATED_AT,
                CREATED_AT
        );
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
    }
}
