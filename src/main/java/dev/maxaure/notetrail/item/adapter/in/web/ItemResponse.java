package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.domain.Item;

import java.time.Instant;
import java.util.UUID;

record ItemResponse(
        UUID id,
        String title,
        String content,
        int version,
        UUID ownerId,
        Instant createdAt,
        Instant updatedAt
) {

    static ItemResponse from(Item item) {
        return new ItemResponse(
                item.id(),
                item.title(),
                item.content(),
                item.version(),
                item.ownerId(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}
