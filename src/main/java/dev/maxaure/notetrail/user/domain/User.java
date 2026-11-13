package dev.maxaure.notetrail.user.domain;

import dev.maxaure.notetrail.shared.error.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record User(UUID id, String login, PasswordHash passwordHash, Instant createdAt) {

    private static final int LOGIN_MIN_LENGTH = 3;
    private static final int LOGIN_MAX_LENGTH = 64;

    public User {
        Objects.requireNonNull(id, "id is required");
        login = normalizeLogin(login);
        Objects.requireNonNull(passwordHash, "passwordHash is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public static User create(String login, PasswordHash passwordHash, Instant createdAt) {
        return new User(UUID.randomUUID(), login, passwordHash, createdAt);
    }

    private static String normalizeLogin(String login) {
        String trimmed = Objects.requireNonNull(login, "login is required").trim();
        if (trimmed.length() < LOGIN_MIN_LENGTH || trimmed.length() > LOGIN_MAX_LENGTH) {
            throw new ValidationException("login must be between " + LOGIN_MIN_LENGTH + " and " + LOGIN_MAX_LENGTH + " characters");
        }
        return trimmed;
    }
}
