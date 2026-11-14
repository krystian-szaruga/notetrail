package dev.maxaure.notetrail.item.application.port.in;

import dev.maxaure.notetrail.item.application.ShareItemCommand;
import dev.maxaure.notetrail.item.application.ShareOutcome;

public interface ShareItemUseCase {

    ShareOutcome share(ShareItemCommand command);
}
