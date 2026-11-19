package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.core.error.ConflictException;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.domain.ItemPermission;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

class ItemPermissionPersistenceAdapter implements ItemPermissionRepository {

    private final ItemPermissionJpaRepository repository;

    ItemPermissionPersistenceAdapter(ItemPermissionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ItemPermission insert(ItemPermission permission) {
        try {
            return repository.saveAndFlush(ItemPermissionJpaEntity.from(permission)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            // Assumes no surrounding transaction: inside one, the caught exception would
            // leave it rollback-only and fail later with UnexpectedRollbackException.
            throw new ConflictException("concurrent share request, retry");
        }
    }

    @Override
    public ItemPermission updateRole(ItemPermission permission) {
        ItemPermissionJpaEntity existing = repository
                .findByItemIdAndUserId(permission.itemId(), permission.userId())
                .orElseThrow(() -> new ConflictException("concurrent revoke request, retry"));
        existing.updateRole(permission.role());
        return repository.saveAndFlush(existing).toDomain();
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
    public boolean delete(UUID itemId, UUID userId) {
        return repository.deleteByItemIdAndUserId(itemId, userId) > 0;
    }
}
