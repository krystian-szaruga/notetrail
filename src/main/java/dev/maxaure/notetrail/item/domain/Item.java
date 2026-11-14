package dev.maxaure.notetrail.item.domain;

import dev.maxaure.notetrail.core.error.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record Item(
        UUID id,
        UUID ownerId,
        String title,
        String content,
        int version,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt
) {

    private static final int TITLE_MAX_LENGTH = 255;

    public Item {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(ownerId, "ownerId is required");
        title = normalizeTitle(title);
        Objects.requireNonNull(createdAt, "createdAt is required");
        Objects.requireNonNull(updatedAt, "updatedAt is required");
    }

    public static Item create(UUID ownerId, String title, String content, Instant now) {
        return new Item(UUID.randomUUID(), ownerId, title, content, 0, false, now, now);
    }

    public Item withContent(String title, String content, Instant now) {
        return new Item(id, ownerId, title, content, version, deleted, createdAt, now);
    }

    public Item deleted(Instant now) {
        return new Item(id, ownerId, title, content, version, true, createdAt, now);
    }

    public boolean hasVersion(int expectedVersion) {
        return version == expectedVersion;
    }

    private static String normalizeTitle(String title) {
        String trimmed = Objects.requireNonNull(title, "title is required").trim();
        if (trimmed.isEmpty() || trimmed.length() > TITLE_MAX_LENGTH) {
            throw new ValidationException("title must be between 1 and " + TITLE_MAX_LENGTH + " characters");
        }
        return trimmed;
    }
}
