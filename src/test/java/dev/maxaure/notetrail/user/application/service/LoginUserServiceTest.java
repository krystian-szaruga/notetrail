package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.core.error.UnauthorizedException;
import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.application.IssuedToken;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.TokenIssuer;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoginUserServiceTest {

    private static final PasswordHash HASH = new PasswordHash("$2a$10$hashed");
    private static final User USER = new User(
            UUID.fromString("550e8400-e29b-41d4-a716-446655440000"),
            "jan.kowalski",
            HASH,
            Instant.parse("2026-01-01T10:00:00Z")
    );
    private static final IssuedToken TOKEN = new IssuedToken("token", 3600);

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordHasher passwordHasher = mock(PasswordHasher.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final LoginUserService service = new LoginUserService(userRepository, passwordHasher, tokenIssuer);

    @Test
    void whenLoginIsUnknownThenClientIsUnauthorized() {
        when(userRepository.findByLogin("nobody")).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> service.login(new CredentialsCommand("nobody", "strong-pass")));
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void whenPasswordDoesNotMatchThenClientIsUnauthorized() {
        when(userRepository.findByLogin("jan.kowalski")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches("wrong-pass", HASH)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> service.login(new CredentialsCommand("jan.kowalski", "wrong-pass")));
        verifyNoInteractions(tokenIssuer);
    }

    @Test
    void whenLoginHasSurroundingWhitespaceThenUserIsFoundByTrimmedLogin() {
        when(userRepository.findByLogin("jan.kowalski")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches("strong-pass", HASH)).thenReturn(true);
        when(tokenIssuer.issue(USER)).thenReturn(TOKEN);

        IssuedToken issuedToken = service.login(new CredentialsCommand(" jan.kowalski ", "strong-pass"));

        assertEquals(TOKEN, issuedToken);
    }

    @Test
    void whenCredentialsAreValidThenTokenIsIssued() {
        when(userRepository.findByLogin("jan.kowalski")).thenReturn(Optional.of(USER));
        when(passwordHasher.matches("strong-pass", HASH)).thenReturn(true);
        when(tokenIssuer.issue(USER)).thenReturn(TOKEN);

        IssuedToken issuedToken = service.login(new CredentialsCommand("jan.kowalski", "strong-pass"));

        assertEquals("token", issuedToken.token());
        assertEquals(3600, issuedToken.expiresIn());
    }
}
