package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.item.application.CreateItemCommand;
import dev.maxaure.notetrail.item.application.port.in.CreateItemUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;

import java.time.Clock;

public class CreateItemService implements CreateItemUseCase {

    private final ItemRepository itemRepository;
    private final Clock clock;

    public CreateItemService(ItemRepository itemRepository, Clock clock) {
        this.itemRepository = itemRepository;
        this.clock = clock;
    }

    @Override
    public Item create(CreateItemCommand command) {
        Item item = Item.create(command.ownerId(), command.title(), command.content(), clock.instant());
        return itemRepository.save(item);
    }
}
