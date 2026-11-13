package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.application.port.out.LoginRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class LoginRateLimitFilterConfiguration {

    @Bean
    LoginRateLimitFilter loginRateLimitFilter(LoginRateLimiter loginRateLimiter) {
        return new LoginRateLimitFilter(loginRateLimiter);
    }
}
