package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.domain.Item;

import java.time.Instant;
import java.util.UUID;

record ItemPatchResponse(
        UUID id,
        String title,
        String content,
        int version,
        Instant updatedAt
) {

    static ItemPatchResponse from(Item item) {
        return new ItemPatchResponse(
                item.id(),
                item.title(),
                item.content(),
                item.version(),
                item.updatedAt()
        );
    }
}
