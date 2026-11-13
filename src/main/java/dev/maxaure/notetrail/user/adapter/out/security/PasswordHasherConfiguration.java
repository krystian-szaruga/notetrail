package dev.maxaure.notetrail.user.adapter.out.security;

import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PasswordHasherConfiguration {

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }
}
