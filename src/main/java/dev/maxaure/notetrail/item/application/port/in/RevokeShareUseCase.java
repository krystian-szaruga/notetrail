package dev.maxaure.notetrail.item.application.port.in;

import java.util.UUID;

public interface RevokeShareUseCase {

    void revoke(UUID itemId, UUID requesterId, UUID targetUserId);
}
