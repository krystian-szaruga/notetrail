package dev.maxaure.notetrail.shared.error;

public class LoginAlreadyTakenException extends ConflictException {

    public LoginAlreadyTakenException(String login) {
        super("login already taken: " + login);
    }
}
