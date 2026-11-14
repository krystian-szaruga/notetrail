package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.ForbiddenException;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemPermission;
import dev.maxaure.notetrail.item.domain.ItemRole;

import java.util.Optional;
import java.util.UUID;

final class ItemAccessPolicy {

    private final ItemPermissionRepository permissionRepository;

    ItemAccessPolicy(ItemPermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    Optional<ItemRole> roleOf(Item item, UUID userId) {
        if (item.isOwnedBy(userId)) {
            return Optional.of(ItemRole.OWNER);
        }
        return permissionRepository.find(item.id(), userId).map(ItemPermission::role);
    }

    ItemRole requireRead(Item item, UUID userId) {
        return roleOf(item, userId)
                .orElseThrow(() -> new ForbiddenException("no access to item"));
    }

    ItemRole requireEdit(Item item, UUID userId) {
        ItemRole role = requireRead(item, userId);
        if (!role.canEdit()) {
            throw new ForbiddenException("no permission to edit item");
        }
        return role;
    }
}
