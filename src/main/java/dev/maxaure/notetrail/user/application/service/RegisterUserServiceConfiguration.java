package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.user.application.port.in.RegisterUserUseCase;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class RegisterUserServiceConfiguration {

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        return new RegisterUserService(userRepository, passwordHasher, clock);
    }
}
