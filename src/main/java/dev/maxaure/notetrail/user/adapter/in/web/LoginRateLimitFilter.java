package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.application.port.out.LoginRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/login";

    private final LoginRateLimiter loginRateLimiter;

    LoginRateLimitFilter(LoginRateLimiter loginRateLimiter) {
        this.loginRateLimiter = loginRateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        if (!LOGIN_PATH.equals(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }
        Optional<Duration> retryAfter = loginRateLimiter.tryConsume(request.getRemoteAddr());
        if (retryAfter.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        long retryAfterSeconds = Math.max(1, retryAfter.get().toSeconds() + 1);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {"title":"Too Many Requests","status":429,"detail":"login rate limit exceeded"}
                """
        );
    }
}
