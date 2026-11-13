package dev.maxaure.notetrail.user.domain;

import dev.maxaure.notetrail.shared.error.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void whenLoginHasTwoCharactersThenValidationFails() {
        assertThrows(ValidationException.class, () -> User.create("ab", new PasswordHash("$2a$10$hashed"), NOW));
    }

    @Test
    void whenLoginHasSixtyFiveCharactersThenValidationFails() {
        assertThrows(ValidationException.class, () -> User.create("a".repeat(65), new PasswordHash("$2a$10$hashed"), NOW));
    }

    @Test
    void whenLoginHasThreeOrSixtyFourCharactersThenUserIsCreated() {
        assertNotNull(User.create("abc", new PasswordHash("$2a$10$hashed"), NOW));
        assertNotNull(User.create("a".repeat(64), new PasswordHash("$2a$10$hashed"), NOW));
    }

    @Test
    void whenLoginHasSurroundingWhitespaceThenItIsTrimmed() {
        assertEquals("jan.kowalski", User.create("  jan.kowalski  ", new PasswordHash("$2a$10$hashed"), NOW).login());
    }
}
