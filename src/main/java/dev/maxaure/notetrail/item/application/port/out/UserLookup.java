package dev.maxaure.notetrail.item.application.port.out;

import java.util.UUID;

public interface UserLookup {

    boolean existsById(UUID userId);
}
