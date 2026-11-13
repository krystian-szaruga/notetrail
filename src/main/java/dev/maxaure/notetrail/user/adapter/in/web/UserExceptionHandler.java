package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.domain.error.LoginAlreadyTakenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class UserExceptionHandler {

    @ExceptionHandler(LoginAlreadyTakenException.class)
    ProblemDetail onLoginAlreadyTaken(LoginAlreadyTakenException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
        return problem;
    }
}
