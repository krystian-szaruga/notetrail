package dev.maxaure.notetrail.shared.error;

public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
