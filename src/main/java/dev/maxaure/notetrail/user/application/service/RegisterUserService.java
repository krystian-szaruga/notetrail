package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.core.error.ValidationException;
import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.application.port.in.RegisterUserUseCase;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RegisterUserService(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public User register(CredentialsCommand command) {
        String rawPassword = requireValidPassword(command.rawPassword());
        PasswordHash passwordHash = passwordHasher.hash(rawPassword);
        User user = User.create(command.login(), passwordHash, clock.instant());
        return userRepository.save(user);
    }

    private static String requireValidPassword(String rawPassword) {
        if (rawPassword == null || PasswordPolicy.isTooShort(rawPassword)) {
            throw new ValidationException("password must be at least " + PasswordPolicy.MIN_LENGTH + " characters");
        }
        if (PasswordPolicy.isTooLong(rawPassword)) {
            throw new ValidationException("password must be at most " + PasswordPolicy.MAX_BYTES + " bytes");
        }
        return rawPassword;
    }
}
