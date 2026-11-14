package dev.maxaure.notetrail.user.adapter.out.persistence;

import dev.maxaure.notetrail.user.domain.error.LoginAlreadyTakenException;
import dev.maxaure.notetrail.user.application.port.out.UserRepository;
import dev.maxaure.notetrail.user.domain.User;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository repository;

    UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        try {
            return repository.saveAndFlush(UserJpaEntity.from(user)).toDomain();
        } catch (DataIntegrityViolationException exception) {
            throw new LoginAlreadyTakenException(user.login());
        }
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return repository.findByLogin(login).map(UserJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID userId) {
        return repository.existsById(userId);
    }
}
