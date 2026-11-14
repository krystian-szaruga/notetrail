package dev.maxaure.notetrail.item.application.port.in;

import dev.maxaure.notetrail.item.domain.ItemRevision;

import java.util.List;
import java.util.UUID;

public interface GetItemHistoryUseCase {

    List<ItemRevision> history(UUID itemId, UUID requesterId);
}
