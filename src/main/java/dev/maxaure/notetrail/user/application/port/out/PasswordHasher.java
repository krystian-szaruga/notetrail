package dev.maxaure.notetrail.user.application.port.out;

import dev.maxaure.notetrail.user.domain.PasswordHash;

public interface PasswordHasher {

    PasswordHash hash(String rawPassword);

    boolean matches(String rawPassword, PasswordHash hash);
}
