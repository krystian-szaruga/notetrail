package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.core.error.ForbiddenException;
import dev.maxaure.notetrail.core.error.NotFoundException;
import dev.maxaure.notetrail.core.error.ValidationException;
import dev.maxaure.notetrail.item.application.ShareItemCommand;
import dev.maxaure.notetrail.item.application.ShareOutcome;
import dev.maxaure.notetrail.item.application.port.in.ShareItemUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.application.port.out.UserLookup;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemPermission;

import java.time.Clock;
import java.util.Optional;

public class ShareItemService implements ShareItemUseCase {

    private final ItemRepository itemRepository;
    private final ItemPermissionRepository permissionRepository;
    private final UserLookup userLookup;
    private final Clock clock;

    public ShareItemService(
            ItemRepository itemRepository,
            ItemPermissionRepository permissionRepository,
            UserLookup userLookup,
            Clock clock
    ) {
        this.itemRepository = itemRepository;
        this.permissionRepository = permissionRepository;
        this.userLookup = userLookup;
        this.clock = clock;
    }

    @Override
    public ShareOutcome share(ShareItemCommand command) {
        Item item = itemRepository.findById(command.itemId())
                .orElseThrow(() -> new NotFoundException("item not found"));
        if (!item.isOwnedBy(command.requesterId())) {
            throw new ForbiddenException("only owner can share item");
        }
        if (command.targetUserId().equals(item.ownerId())) {
            throw new ValidationException("item cannot be shared with its owner");
        }
        if (!userLookup.existsById(command.targetUserId())) {
            throw new NotFoundException("user not found");
        }
        Optional<ItemPermission> existing = permissionRepository.find(item.id(), command.targetUserId());
        if (existing.isPresent()) {
            ItemPermission updated = permissionRepository.updateRole(
                    existing.get().withRole(command.role())
            );
            return new ShareOutcome(updated, false);
        }
        ItemPermission permission = new ItemPermission(
                item.id(),
                command.targetUserId(),
                command.role(),
                clock.instant()
        );
        return new ShareOutcome(permissionRepository.insert(permission), true);
    }
}
