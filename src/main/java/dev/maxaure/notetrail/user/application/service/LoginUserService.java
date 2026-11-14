package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.core.error.UnauthorizedException;
import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.application.IssuedToken;
import dev.maxaure.notetrail.user.application.port.in.LoginUserUseCase;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.TokenIssuer;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;

import java.util.Optional;
import java.util.UUID;

public class LoginUserService implements LoginUserUseCase {

    private static final String INVALID_CREDENTIALS = "invalid login or password";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;
    private final PasswordHash dummyHash;

    public LoginUserService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
        this.dummyHash = passwordHasher.hash(UUID.randomUUID().toString());
    }

    @Override
    public IssuedToken login(CredentialsCommand command) {
        String rawPassword = command.rawPassword();
        if (rawPassword == null || PasswordPolicy.isTooLong(rawPassword)) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        Optional<User> user = userRepository.findByLogin(command.login().trim());
        PasswordHash stored = user.map(User::passwordHash).orElse(dummyHash);
        boolean matches = passwordHasher.matches(rawPassword, stored);
        if (user.isEmpty() || !matches) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        return tokenIssuer.issue(user.get());
    }
}