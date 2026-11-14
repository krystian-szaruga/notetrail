package dev.maxaure.notetrail.security;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String login) {
}
