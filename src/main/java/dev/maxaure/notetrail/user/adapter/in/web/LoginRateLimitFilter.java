package dev.maxaure.notetrail.user.adapter.in.web;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/login";

    private final RateLimiter rateLimiter;

    LoginRateLimitFilter(int capacity, Duration refillPeriod) {
        this.rateLimiter = new RateLimiter(capacity, refillPeriod);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        boolean loginAttempt = HttpMethod.POST.matches(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI());
        if (!loginAttempt) {
            chain.doFilter(request, response);
            return;
        }
        Optional<Duration> rejectedFor = rateLimiter.tryConsume(request.getRemoteAddr());
        if (rejectedFor.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        long retryAfterSeconds = Math.max(1, (long) Math.ceil(rejectedFor.get().toMillis() / 1000.0));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"title":"Too Many Requests","status":429,"detail":"login rate limit exceeded"}
                """);
    }

    private static final class RateLimiter {

        private final Cache<String, Bucket> buckets;
        private final int capacity;
        private final Duration refillPeriod;

        RateLimiter(int capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
            this.buckets = Caffeine.newBuilder()
                    .expireAfterAccess(refillPeriod)
                    .build();
        }

        Optional<Duration> tryConsume(String clientAddress) {
            Bucket bucket = buckets.get(clientAddress, address -> newBucket());
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }

        private Bucket newBucket() {
            return Bucket.builder()
                    .addLimit(limit -> limit.capacity(capacity).refillIntervally(capacity, refillPeriod))
                    .build();
        }
    }
}