package dev.maxaure.notetrail.user.application.port.out;

import dev.maxaure.notetrail.user.domain.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findByLogin(String login);
}
