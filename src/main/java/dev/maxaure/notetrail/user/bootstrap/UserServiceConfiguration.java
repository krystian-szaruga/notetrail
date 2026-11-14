package dev.maxaure.notetrail.user.bootstrap;

import dev.maxaure.notetrail.user.application.port.in.FindUserUseCase;
import dev.maxaure.notetrail.user.application.port.in.LoginUserUseCase;
import dev.maxaure.notetrail.user.application.port.in.RegisterUserUseCase;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.TokenIssuer;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.application.service.FindUserService;
import dev.maxaure.notetrail.user.application.service.LoginUserService;
import dev.maxaure.notetrail.user.application.service.RegisterUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;


@Configuration
class UserServiceConfiguration {

    @Bean
    FindUserUseCase findUserUseCase(UserRepository userRepository) {
        return new FindUserService(userRepository);
    }

    @Bean
    LoginUserUseCase loginUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        return new LoginUserService(userRepository, passwordHasher, tokenIssuer);
    }

    @Bean
    RegisterUserUseCase registerUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        return new RegisterUserService(userRepository, passwordHasher, clock);
    }
}
