package dev.maxaure.notetrail.user.adapter.out.security;

import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.TokenIssuer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;

@Configuration
class SecurityConfiguration {

    @Bean
    PasswordHasher passwordHasher(
            @Value("${notetrail.security.bcrypt-strength:10}") int strength
    ) {
        return new BCryptPasswordHasher(strength);
    }

    @Bean
    TokenIssuer tokenIssuer(
            SecretKey jwtSecretKey,
            @Value("${app.jwt.ttl-seconds:3600}") long ttlSeconds,
            Clock clock
    ) {
        return new JwtTokenIssuer(jwtSecretKey, Duration.ofSeconds(ttlSeconds), clock);
    }
}
