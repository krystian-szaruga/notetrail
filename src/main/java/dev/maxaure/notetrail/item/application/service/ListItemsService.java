package dev.maxaure.notetrail.item.application.service;

import dev.maxaure.notetrail.item.application.ItemWithRole;
import dev.maxaure.notetrail.item.application.port.in.ListItemsUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.domain.Item;
import dev.maxaure.notetrail.item.domain.ItemPermission;
import dev.maxaure.notetrail.item.domain.ItemRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ListItemsService implements ListItemsUseCase {

    private final ItemRepository itemRepository;
    private final ItemPermissionRepository permissionRepository;

    public ListItemsService(ItemRepository itemRepository, ItemPermissionRepository permissionRepository) {
        this.itemRepository = itemRepository;
        this.permissionRepository = permissionRepository;
    }

    @Override
    public List<ItemWithRole> list(UUID requesterId) {
        List<Item> owned = itemRepository.findAllByOwnerId(requesterId);
        Map<UUID, ItemRole> sharedRoles = permissionRepository.findAllByUserId(requesterId).stream()
                .collect(Collectors.toMap(ItemPermission::itemId, ItemPermission::role));
        List<Item> shared = itemRepository.findAllByIds(sharedRoles.keySet());
        List<ItemWithRole> items = new ArrayList<>();
        owned.forEach(item -> items.add(new ItemWithRole(item, ItemRole.OWNER)));
        shared.forEach(item -> items.add(new ItemWithRole(item, sharedRoles.get(item.id()))));
        items.sort(Comparator.comparing((ItemWithRole item) -> item.item().updatedAt())
                .thenComparing(item -> item.item().id())
                .reversed());
        return items;
    }
}
