package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.domain.ItemPermission;
import dev.maxaure.notetrail.item.domain.ItemRole;

import java.time.Instant;
import java.util.UUID;

record ShareResponse(
        UUID itemId,
        UUID userId,
        ItemRole role,
        Instant grantedAt
) {

    static ShareResponse from(ItemPermission permission) {
        return new ShareResponse(
                permission.itemId(),
                permission.userId(),
                permission.role(),
                permission.grantedAt()
        );
    }
}
