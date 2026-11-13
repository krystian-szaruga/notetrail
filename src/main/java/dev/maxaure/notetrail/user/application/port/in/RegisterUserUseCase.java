package dev.maxaure.notetrail.user.application.port.in;

import dev.maxaure.notetrail.user.application.RegisterUserCommand;
import dev.maxaure.notetrail.user.domain.User;

public interface RegisterUserUseCase {

    User register(RegisterUserCommand command);
}
