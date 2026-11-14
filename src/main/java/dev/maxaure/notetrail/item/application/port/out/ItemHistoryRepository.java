package dev.maxaure.notetrail.item.application.port.out;

import dev.maxaure.notetrail.item.domain.ItemRevision;

import java.util.List;
import java.util.UUID;

public interface ItemHistoryRepository {

    List<ItemRevision> findRevisions(UUID itemId);
}
