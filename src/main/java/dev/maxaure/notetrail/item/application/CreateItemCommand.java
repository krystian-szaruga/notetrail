package dev.maxaure.notetrail.item.application;

import java.util.UUID;

public record CreateItemCommand(UUID ownerId, String title, String content) {
}
