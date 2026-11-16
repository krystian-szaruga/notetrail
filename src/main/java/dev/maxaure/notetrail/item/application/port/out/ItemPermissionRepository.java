package dev.maxaure.notetrail.item.application.port.out;

import dev.maxaure.notetrail.item.domain.ItemPermission;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemPermissionRepository {

    ItemPermission insert(ItemPermission permission);

    ItemPermission updateRole(ItemPermission permission);

    Optional<ItemPermission> find(UUID itemId, UUID userId);

    List<ItemPermission> findAllByUserId(UUID userId);

    void delete(UUID itemId, UUID userId);
}
