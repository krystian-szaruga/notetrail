package dev.maxaure.notetrail.user.application.port.in;

import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.domain.User;

public interface RegisterUserUseCase {

    User register(CredentialsCommand command);
}
