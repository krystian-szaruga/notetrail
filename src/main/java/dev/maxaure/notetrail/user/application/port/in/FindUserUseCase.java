package dev.maxaure.notetrail.user.application.port.in;

import java.util.UUID;

public interface FindUserUseCase {

    boolean existsById(UUID userId);
}
