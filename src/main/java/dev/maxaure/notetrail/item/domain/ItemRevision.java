package dev.maxaure.notetrail.item.domain;

import java.time.Instant;
import java.util.Objects;

public record ItemRevision(
        int revision,
        RevisionType type,
        Instant timestamp,
        String changedBy,
        String title,
        String content
) {

    public ItemRevision {
        Objects.requireNonNull(type, "type is required");
        Objects.requireNonNull(timestamp, "timestamp is required");
        Objects.requireNonNull(title, "title is required");
    }
}
