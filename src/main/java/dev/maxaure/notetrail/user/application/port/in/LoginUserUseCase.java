package dev.maxaure.notetrail.user.application.port.in;

import dev.maxaure.notetrail.user.application.CredentialsCommand;
import dev.maxaure.notetrail.user.application.IssuedToken;

public interface LoginUserUseCase {

    IssuedToken login(CredentialsCommand command);
}
