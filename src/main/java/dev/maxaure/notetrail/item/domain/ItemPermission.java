package dev.maxaure.notetrail.item.domain;

import dev.maxaure.notetrail.core.error.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ItemPermission(UUID itemId, UUID userId, ItemRole role, Instant grantedAt) {

    public ItemPermission {
        Objects.requireNonNull(itemId, "itemId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(role, "role is required");
        Objects.requireNonNull(grantedAt, "grantedAt is required");
        if (role == ItemRole.OWNER) {
            throw new ValidationException("only VIEWER or EDITOR can be granted");
        }
    }

    public ItemPermission withRole(ItemRole newRole) {
        return new ItemPermission(itemId, userId, newRole, grantedAt);
    }
}
