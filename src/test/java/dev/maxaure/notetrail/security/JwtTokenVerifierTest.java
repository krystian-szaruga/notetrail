package dev.maxaure.notetrail.security;

import dev.maxaure.notetrail.core.error.UnauthorizedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenVerifierTest {

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
    );
    private static final SecretKey OTHER_KEY = Keys.hmacShaKeyFor(
            "abcdefghijklmnopqrstuvwxyzabcdef".getBytes(StandardCharsets.UTF_8)
    );
    private static final Duration TTL = Duration.ofHours(1);
    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("8f7d6c5e-4b3a-4291-8f6a-000000000001");

    private final JwtTokenVerifier verifier = new JwtTokenVerifier(KEY, CLOCK);

    @Test
    void whenTokenIsValidThenReturnsSameIdAndLogin() {
        String token = issue(USER_ID.toString(), KEY);

        AuthenticatedUser user = verifier.verify(token);

        assertEquals(USER_ID, user.id());
        assertEquals("jan.kowalski", user.login());
    }

    @Test
    void whenTokenIsExpiredThenThrowsUnauthorized() {
        JwtTokenVerifier shifted = new JwtTokenVerifier(
                KEY, Clock.fixed(NOW.plus(TTL).plusSeconds(1), ZoneOffset.UTC)
        );
        String token = issue(USER_ID.toString(), KEY);

        assertThrows(UnauthorizedException.class, () -> shifted.verify(token));
    }

    @Test
    void whenTokenIsSignedWithDifferentKeyThenThrowsUnauthorized() {
        String token = issue(USER_ID.toString(), OTHER_KEY);

        assertThrows(UnauthorizedException.class, () -> verifier.verify(token));
    }

    @Test
    void whenTokenIsGarbageThenThrowsUnauthorized() {
        assertThrows(UnauthorizedException.class, () -> verifier.verify("abc"));
        assertThrows(UnauthorizedException.class, () -> verifier.verify(""));
    }

    @Test
    void whenTokenHasNoExpirationThenThrowsUnauthorized() {
        String token = Jwts.builder()
                .subject(USER_ID.toString())
                .claim("login", "jan.kowalski")
                .issuedAt(Date.from(NOW))
                .signWith(KEY)
                .compact();

        assertThrows(UnauthorizedException.class, () -> verifier.verify(token));
    }

    @Test
    void whenSubjectIsNotUuidThenThrowsUnauthorized() {
        String token = issue("not-a-uuid", KEY);

        assertThrows(UnauthorizedException.class, () -> verifier.verify(token));
    }

    private static String issue(String subject, SecretKey key) {
        return Jwts.builder()
                .subject(subject)
                .claim("login", "jan.kowalski")
                .issuedAt(Date.from(JwtTokenVerifierTest.NOW))
                .expiration(Date.from(JwtTokenVerifierTest.NOW.plus(JwtTokenVerifierTest.TTL)))
                .signWith(key)
                .compact();
    }
}
