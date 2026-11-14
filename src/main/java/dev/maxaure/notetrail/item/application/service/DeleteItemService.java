package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.ForbiddenException;
import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.item.application.port.in.DeleteItemUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;

import java.time.Clock;
import java.util.UUID;

public class DeleteItemService implements DeleteItemUseCase {

    private final ItemRepository itemRepository;
    private final Clock clock;

    public DeleteItemService(ItemRepository itemRepository, Clock clock) {
        this.itemRepository = itemRepository;
        this.clock = clock;
    }

    @Override
    public void delete(UUID itemId, UUID requesterId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("item not found"));
        if (!item.isOwnedBy(requesterId)) {
            throw new ForbiddenException("only owner can delete item");
        }
        itemRepository.save(item.deleted(clock.instant()));
    }
}
