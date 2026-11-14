package dev.maxaure.notetrail.item.adapter.in.web;

import dev.maxaure.notetrail.item.domain.error.VersionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ItemExceptionHandler {

    @ExceptionHandler(VersionConflictException.class)
    ProblemDetail onVersionConflict(VersionConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle(HttpStatus.CONFLICT.getReasonPhrase());
        problem.setProperty("version", exception.currentVersion());
        return problem;
    }
}
