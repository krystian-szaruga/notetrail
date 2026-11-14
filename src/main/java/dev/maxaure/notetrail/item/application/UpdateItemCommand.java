package dev.maxaure.notetrail.item.application;

import java.util.UUID;

public record UpdateItemCommand(UUID itemId, UUID requesterId, String title, String content, int version) {
}
