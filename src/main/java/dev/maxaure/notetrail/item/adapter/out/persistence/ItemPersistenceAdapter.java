package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.error.VersionConflictException;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class ItemPersistenceAdapter implements ItemRepository {

    private final ItemJpaRepository repository;

    ItemPersistenceAdapter(ItemJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Item save(Item item) {
        try {
            return repository.saveAndFlush(ItemJpaEntity.from(item)).toDomain();
        } catch (OptimisticLockingFailureException exception) {
            int currentVersion = repository.findById(item.id())
                    .map(entity -> entity.toDomain().version())
                    .orElseThrow(() -> new NotFoundException("item not found"));
            throw new VersionConflictException(currentVersion);
        }
    }

    @Override
    public Optional<Item> findById(UUID id) {
        return repository.findByIdAndDeletedFalse(id).map(ItemJpaEntity::toDomain);
    }

    @Override
    public List<Item> findAllByOwnerId(UUID ownerId) {
        return repository.findAllByOwnerIdAndDeletedFalse(ownerId).stream()
                .map(ItemJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<Item> findAllByIds(Collection<UUID> ids) {
        return repository.findAllByIdInAndDeletedFalse(ids).stream()
                .map(ItemJpaEntity::toDomain)
                .toList();
    }
}
