package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.shared.error.ValidationException;
import dev.maxaure.notetrail.user.application.RegisterUserCommand;
import dev.maxaure.notetrail.user.application.port.in.RegisterUserUseCase;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;

import java.nio.charset.StandardCharsets;
import java.time.Clock;

class RegisterUserService implements RegisterUserUseCase {

    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    RegisterUserService(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public User register(RegisterUserCommand command) {
        PasswordHash passwordHash = passwordHasher.hash(requireStrongPassword(command.rawPassword()));
        User user = User.create(command.login(), passwordHash, clock.instant());
        return userRepository.save(user);
    }

    private static String requireStrongPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new ValidationException("password must be at least " + PASSWORD_MIN_LENGTH + " characters");
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new ValidationException("password must be at most " + PASSWORD_MAX_BYTES + " bytes");
        }
        return rawPassword;
    }
}
