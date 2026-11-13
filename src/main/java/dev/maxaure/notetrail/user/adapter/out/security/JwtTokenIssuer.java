package dev.maxaure.notetrail.user.adapter.out.security;

import dev.maxaure.notetrail.user.application.IssuedToken;
import dev.maxaure.notetrail.user.application.port.out.TokenIssuer;
import dev.maxaure.notetrail.user.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

class JwtTokenIssuer implements TokenIssuer {

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;

    JwtTokenIssuer(String secret, Duration ttl, Clock clock) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        String token = Jwts.builder()
                .subject(user.id().toString())
                .claim("login", user.login())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
        return new IssuedToken(token, ttl.toSeconds());
    }
}
