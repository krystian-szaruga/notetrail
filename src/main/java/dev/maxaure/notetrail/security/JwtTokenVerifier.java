package dev.maxaure.notetrail.security;

import dev.maxaure.notetrail.core.error.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Clock;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

class JwtTokenVerifier {

    private final SecretKey key;
    private final Clock clock;

    JwtTokenVerifier(SecretKey key, java.time.Clock clock) {
        this.key = key;
        this.clock = () -> Date.from(clock.instant());
    }

    AuthenticatedUser verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(clock)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String subject = claims.getSubject();
            if (subject == null || claims.getExpiration() == null) {
                throw new IllegalArgumentException("missing subject or expiration");
            }
            UUID id = UUID.fromString(subject);
            return new AuthenticatedUser(id, claims.get("login", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("invalid or expired token");
        }
    }
}
