package dev.maxaure.notetrail.item.application;

import dev.maxaure.notetrail.item.domain.ItemRole;

import java.util.UUID;

public record ShareItemCommand(UUID itemId, UUID requesterId, UUID targetUserId, ItemRole role) {
}
