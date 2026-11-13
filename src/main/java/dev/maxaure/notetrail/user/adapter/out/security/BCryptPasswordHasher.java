package dev.maxaure.notetrail.user.adapter.out.security;

import dev.maxaure.notetrail.user.application.port.out.PasswordHasher;
import dev.maxaure.notetrail.user.domain.PasswordHash;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(String rawPassword) {
        return new PasswordHash(encoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash hash) {
        return encoder.matches(rawPassword, hash.value());
    }
}
