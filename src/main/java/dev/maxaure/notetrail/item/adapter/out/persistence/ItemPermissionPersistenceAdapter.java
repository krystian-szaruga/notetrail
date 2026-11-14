package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.domain.ItemPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

class ItemPermissionPersistenceAdapter implements ItemPermissionRepository {

    private final ItemPermissionJpaRepository repository;

    ItemPermissionPersistenceAdapter(ItemPermissionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ItemPermission save(ItemPermission permission) {
        Optional<ItemPermissionJpaEntity> existing =
                repository.findByItemIdAndUserId(permission.itemId(), permission.userId());
        if (existing.isPresent()) {
            existing.get().updateRole(permission.role());
            return repository.saveAndFlush(existing.get()).toDomain();
        }
        return repository.saveAndFlush(ItemPermissionJpaEntity.from(permission)).toDomain();
    }

    @Override
    public Optional<ItemPermission> find(UUID itemId, UUID userId) {
        return repository.findByItemIdAndUserId(itemId, userId).map(ItemPermissionJpaEntity::toDomain);
    }

    @Override
    public List<ItemPermission> findAllByUserId(UUID userId) {
        return repository.findAllByUserId(userId).stream()
                .map(ItemPermissionJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void delete(UUID itemId, UUID userId) {
        repository.deleteByItemIdAndUserId(itemId, userId);
    }
}
