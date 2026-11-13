package dev.maxaure.notetrail.user.adapter.out.ratelimit;

import dev.maxaure.notetrail.user.application.port.out.LoginRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
class LoginRateLimiterConfiguration {

    @Bean
    LoginRateLimiter loginRateLimiter(
            @Value("${app.ratelimit.login-capacity:5}") int capacity,
            @Value("${app.ratelimit.login-refill-seconds:60}") long refillSeconds
    ) {
        return new Bucket4jLoginRateLimiter(capacity, Duration.ofSeconds(refillSeconds));
    }
}
