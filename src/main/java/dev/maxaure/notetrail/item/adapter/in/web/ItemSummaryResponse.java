package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.application.ItemWithRole;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemRole;

import java.time.Instant;
import java.util.UUID;

record ItemSummaryResponse(
        UUID id,
        String title,
        String content,
        int version,
        UUID ownerId,
        ItemRole myRole,
        Instant updatedAt
) {

    static ItemSummaryResponse from(ItemWithRole itemWithRole) {
        Item item = itemWithRole.item();
        return new ItemSummaryResponse(
                item.id(),
                item.title(),
                item.content(),
                item.version(),
                item.ownerId(),
                itemWithRole.role(),
                item.updatedAt()
        );
    }
}
