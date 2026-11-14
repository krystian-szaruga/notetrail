package dev.maxaure.notetrail.security;

import java.util.Optional;

public final class CurrentUser {

    private static final ThreadLocal<AuthenticatedUser> CURRENT = new ThreadLocal<>();

    public static Scope open(AuthenticatedUser user) {
        CURRENT.set(user);
        return new Scope();
    }

    public static Optional<AuthenticatedUser> get() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static final class Scope implements AutoCloseable {

        @Override
        public void close() {
            CURRENT.remove();
        }

        private Scope() {
        }
    }

    private CurrentUser() {
    }
}
