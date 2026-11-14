package dev.maxaure.notetrail.user.application.port.out;

import dev.maxaure.notetrail.user.application.IssuedToken;
import dev.maxaure.notetrail.user.domain.User;

public interface TokenIssuer {

    IssuedToken issue(User user);
}
