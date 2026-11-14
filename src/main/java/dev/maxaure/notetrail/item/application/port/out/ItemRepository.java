package dev.maxaure.notetrail.item.application.port.out;

import dev.maxaure.notetrail.item.domain.Item;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemRepository {

    Item save(Item item);

    Optional<Item> findById(UUID id);

    List<Item> findAllByOwnerId(UUID ownerId);

    List<Item> findAllByIds(Collection<UUID> ids);
}
