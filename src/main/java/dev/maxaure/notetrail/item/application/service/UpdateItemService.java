package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.item.application.UpdateItemCommand;
import dev.maxaure.notetrail.item.application.port.in.UpdateItemUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.error.VersionConflictException;

import java.time.Clock;

public class UpdateItemService implements UpdateItemUseCase {

    private final ItemRepository itemRepository;
    private final ItemAccessPolicy accessPolicy;
    private final Clock clock;

    public UpdateItemService(ItemRepository itemRepository, ItemAccessPolicy accessPolicy, Clock clock) {
        this.itemRepository = itemRepository;
        this.accessPolicy = accessPolicy;
        this.clock = clock;
    }

    @Override
    public Item update(UpdateItemCommand command) {
        Item item = itemRepository.findById(command.itemId())
                .orElseThrow(() -> new NotFoundException("item not found"));
        accessPolicy.requireEdit(item, command.requesterId());
        if (!item.hasVersion(command.version())) {
            throw new VersionConflictException(item.version());
        }
        String title = command.title() != null ? command.title() : item.title();
        String content = command.content() != null ? command.content() : item.content();
        return itemRepository.save(item.withContent(title, content, clock.instant()));
    }
}
