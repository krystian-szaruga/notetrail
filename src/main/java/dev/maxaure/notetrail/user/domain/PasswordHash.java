package dev.maxaure.notetrail.user.domain;

import java.util.Objects;

public record PasswordHash(String value) {

    public PasswordHash {
        Objects.requireNonNull(value, "value is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("password hash must not be blank");
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public String toString() {
        return "PasswordHash[***]";
    }
}
