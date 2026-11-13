package dev.maxaure.notetrail.user.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.NonNull;

record RegisterRequest(@NotBlank String login, @NotBlank String password) {

    @Override
    @NonNull
    public String toString() {
        return "RegisterRequest[login=%s, password=***]".formatted(login);
    }
}
