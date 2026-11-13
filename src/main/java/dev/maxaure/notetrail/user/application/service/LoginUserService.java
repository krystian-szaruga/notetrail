package dev.maxaure.notetrail.user.application.service;

import dev.maxaure.notetrail.core.error.UnauthorizedException;
import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.application.IssuedToken;
import dev.maxaure.notetrail.user.application.port.in.LoginUserUseCase;
import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.application.port.out.TokenIssuer;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.domain.User;

public class LoginUserService implements LoginUserUseCase {

    private static final String INVALID_CREDENTIALS = "invalid login or password";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public LoginUserService(UserRepository userRepository, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public IssuedToken login(CredentialsCommand command) {
        User user = userRepository.findByLogin(command.login().trim())
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS));
        if (!passwordHasher.matches(command.rawPassword(), user.passwordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        return tokenIssuer.issue(user);
    }
}
