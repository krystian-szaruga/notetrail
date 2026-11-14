package dev.maxaure.notetrail.item.application.port.in;

import dev.maxaure.notetrail.item.application.ItemWithRole;

import java.util.List;
import java.util.UUID;

public interface ListItemsUseCase {

    List<ItemWithRole> list(UUID requesterId);
}
