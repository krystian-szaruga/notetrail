package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.item.application.port.in.GetItemHistoryUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemHistoryRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemRevision;

import java.util.List;
import java.util.UUID;

public class GetItemHistoryService implements GetItemHistoryUseCase {

    private final ItemRepository itemRepository;
    private final ItemHistoryRepository historyRepository;
    private final ItemAccessPolicy accessPolicy;

    public GetItemHistoryService(
            ItemRepository itemRepository,
            ItemHistoryRepository historyRepository,
            ItemAccessPolicy accessPolicy
    ) {
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.accessPolicy = accessPolicy;
    }

    @Override
    public List<ItemRevision> history(UUID itemId, UUID requesterId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("item not found"));
        accessPolicy.requireRead(item, requesterId);
        return historyRepository.findRevisions(itemId);
    }
}
