package dev.maxaure.notetrail.item.application.port.in;

import dev.maxaure.notetrail.item.application.UpdateItemCommand;
import dev.maxaure.notetrail.item.domain.Item;

public interface UpdateItemUseCase {

    Item update(UpdateItemCommand command);
}
