package dev.maxaure.notetrail.item.application.port.in;

import dev.maxaure.notetrail.item.application.CreateItemCommand;
import dev.maxaure.notetrail.item.domain.Item;

public interface CreateItemUseCase {

    Item create(CreateItemCommand command);
}
