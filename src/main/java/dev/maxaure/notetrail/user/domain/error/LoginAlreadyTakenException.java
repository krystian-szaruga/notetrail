package dev.maxaure.notetrail.user.domain.error;

public class LoginAlreadyTakenException extends RuntimeException {

    public LoginAlreadyTakenException(String login) {
        super("login already taken: " + login);
    }
}
