package dev.maxaure.notetrail.user.adapter.out.persistence;

import dev.maxaure.notetrail.user.domain.PasswordHash;
import dev.maxaure.notetrail.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_login", columnNames = "login"))
class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64)
    private String login;

    @Column(name = "password", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
    }

    private UserJpaEntity(UUID id, String login, String passwordHash, Instant createdAt) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    static UserJpaEntity from(User user) {
        return new UserJpaEntity(user.id(), user.login(), user.passwordHash().value(), user.createdAt());
    }

    User toDomain() {
        return new User(id, login, new PasswordHash(passwordHash), createdAt);
    }
}
