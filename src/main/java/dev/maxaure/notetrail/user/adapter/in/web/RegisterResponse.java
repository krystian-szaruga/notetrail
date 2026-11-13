package dev.maxaure.notetrail.user.adapter.in.web;

import dev.maxaure.notetrail.user.domain.User;

import java.time.Instant;
import java.util.UUID;

record RegisterResponse(UUID id, String login, Instant createdAt) {

    static RegisterResponse from(User user) {
        return new RegisterResponse(user.id(), user.login(), user.createdAt());
    }
}
