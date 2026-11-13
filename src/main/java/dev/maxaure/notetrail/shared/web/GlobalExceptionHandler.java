package dev.maxaure.notetrail.shared.web;

import dev.maxaure.notetrail.shared.error.ConflictException;
import dev.maxaure.notetrail.shared.error.UnauthorizedException;
import dev.maxaure.notetrail.shared.error.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    ProblemDetail onValidation(ValidationException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalidRequest(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("request is invalid");
        return problem(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail onConflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    ProblemDetail onUnauthorized(UnauthorizedException exception) {
        return problem(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
