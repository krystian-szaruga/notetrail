package dev.maxaure.notetrail.item.application.port.in;

import java.util.UUID;

public interface DeleteItemUseCase {

    void delete(UUID itemId, UUID requesterId);
}
