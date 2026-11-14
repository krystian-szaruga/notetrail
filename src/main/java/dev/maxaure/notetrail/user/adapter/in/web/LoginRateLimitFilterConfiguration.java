package dev.maxaure.notetrail.user.adapter.in.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.time.Duration;

@Configuration
class LoginRateLimitFilterConfiguration {

    @Bean
    FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilter(
            @Value("${app.ratelimit.login-capacity:5}") int capacity,
            @Value("${app.ratelimit.login-refill-seconds:60}") long refillSeconds
    ) {
        var registration = new FilterRegistrationBean<>(
                new LoginRateLimitFilter(
                        capacity, Duration.ofSeconds(refillSeconds)
                )
        );
        registration.addUrlPatterns("/login");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
