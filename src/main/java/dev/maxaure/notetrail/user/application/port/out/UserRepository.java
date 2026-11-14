package dev.maxaure.notetrail.user.application.port.out;

import dev.maxaure.notetrail.user.domain.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    User save(User user);

    Optional<User> findByLogin(String login);

    boolean existsById(UUID userId);
}
