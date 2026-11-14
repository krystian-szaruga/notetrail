package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.ForbiddenException;
import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.item.application.port.in.RevokeShareUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;

import java.util.UUID;

public class RevokeShareService implements RevokeShareUseCase {

    private final ItemRepository itemRepository;
    private final ItemPermissionRepository permissionRepository;

    public RevokeShareService(ItemRepository itemRepository, ItemPermissionRepository permissionRepository) {
        this.itemRepository = itemRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void revoke(UUID itemId, UUID requesterId, UUID targetUserId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("item not found"));
        if (!item.isOwnedBy(requesterId)) {
            throw new ForbiddenException("only owner can revoke item access");
        }
        permissionRepository.find(item.id(), targetUserId)
                .orElseThrow(() -> new NotFoundException("permission not found"));
        permissionRepository.delete(item.id(), targetUserId);
    }
}
