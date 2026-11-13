package dev.maxaure.notetrail.user.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface LoginRateLimiter {

    Optional<Duration> tryConsume(String clientAddress);
}
