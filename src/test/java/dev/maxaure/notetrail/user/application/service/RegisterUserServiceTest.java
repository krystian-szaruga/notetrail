package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.core.error.ValidationException;
import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegisterUserServiceTest {

    private static final PasswordHash HASH = new PasswordHash("$2a$10$hashed");
    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final RegisterUserService service = new RegisterUserService(userRepository, passwordHasher, clock);

    @Test
    void whenPasswordIsShorterThanEightCharactersThenNothingIsSaved() {
        CredentialsCommand command = new CredentialsCommand("jan.kowalski", "short");
        assertThrows(ValidationException.class, () -> service.register(command));
        verifyNoInteractions(userRepository, passwordHasher);
    }

    @Test
    void whenPasswordHasExactlyEightCharactersThenUserIsSaved() {
        when(passwordHasher.hash("exactly8")).thenReturn(HASH);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        User saved = service.register(new CredentialsCommand("jan.kowalski", "exactly8"));
        assertEquals(HASH, saved.passwordHash());
        verify(userRepository).save(saved);
    }

    @Test
    void whenPasswordExceedsSeventyTwoBytesThenNothingIsSaved() {
        CredentialsCommand command = new CredentialsCommand("jan.kowalski", "a".repeat(73));
        assertThrows(ValidationException.class, () -> service.register(command));
        verifyNoInteractions(userRepository, passwordHasher);
    }

    @Test
    void whenPasswordExceedsSeventyTwoBytesInUtf8ThenNothingIsSaved() {
        String password = "ł".repeat(37);
        CredentialsCommand command = new CredentialsCommand("jan.kowalski", password);
        assertThrows(ValidationException.class, () -> service.register(command));
        verifyNoInteractions(userRepository, passwordHasher);
    }

    @Test
    void whenPasswordHasExactlySeventyTwoBytesThenUserIsSaved() {
        String password = "a".repeat(72);
        when(passwordHasher.hash(password)).thenReturn(HASH);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        User saved = service.register(new CredentialsCommand("jan.kowalski", password));
        assertEquals(HASH, saved.passwordHash());
    }

    @Test
    void whenCommandIsValidThenUserIsSavedWithHashedPassword() {
        when(passwordHasher.hash("strong-pass")).thenReturn(HASH);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        User saved = service.register(new CredentialsCommand("  jan.kowalski  ", "strong-pass"));
        assertNotNull(saved.id());
        assertEquals("jan.kowalski", saved.login());
        assertEquals(HASH, saved.passwordHash());
        assertEquals(now, saved.createdAt());
        verify(userRepository).save(saved);
    }
}
