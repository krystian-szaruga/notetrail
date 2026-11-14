package dev.maxaure.notetrail.item.bootstrap;

import dev.maxaure.notetrail.item.application.port.in.CreateItemUseCase;
import dev.maxaure.notetrail.item.application.port.in.DeleteItemUseCase;
import dev.maxaure.notetrail.item.application.port.in.GetItemHistoryUseCase;
import dev.maxaure.notetrail.item.application.port.in.ListItemsUseCase;
import dev.maxaure.notetrail.item.application.port.in.RevokeShareUseCase;
import dev.maxaure.notetrail.item.application.port.in.ShareItemUseCase;
import dev.maxaure.notetrail.item.application.port.in.UpdateItemUseCase;
import dev.maxaure.notetrail.item.application.port.out.ItemHistoryRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import dev.maxaure.notetrail.item.application.port.out.UserLookup;
import dev.maxaure.notetrail.item.application.service.CreateItemService;
import dev.maxaure.notetrail.item.application.service.DeleteItemService;
import dev.maxaure.notetrail.item.application.service.GetItemHistoryService;
import dev.maxaure.notetrail.item.application.service.ItemAccessPolicy;
import dev.maxaure.notetrail.item.application.service.ListItemsService;
import dev.maxaure.notetrail.item.application.service.RevokeShareService;
import dev.maxaure.notetrail.item.application.service.ShareItemService;
import dev.maxaure.notetrail.item.application.service.UpdateItemService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ItemServiceConfiguration {

    @Bean
    ItemAccessPolicy itemAccessPolicy(ItemPermissionRepository permissionRepository) {
        return new ItemAccessPolicy(permissionRepository);
    }

    @Bean
    CreateItemUseCase createItemUseCase(ItemRepository itemRepository, Clock clock) {
        return new CreateItemService(itemRepository, clock);
    }

    @Bean
    ListItemsUseCase listItemsUseCase(ItemRepository itemRepository, ItemPermissionRepository permissionRepository) {
        return new ListItemsService(itemRepository, permissionRepository);
    }

    @Bean
    UpdateItemUseCase updateItemUseCase(
            ItemRepository itemRepository,
            ItemAccessPolicy accessPolicy,
            Clock clock
    ) {
        return new UpdateItemService(itemRepository, accessPolicy, clock);
    }

    @Bean
    DeleteItemUseCase deleteItemUseCase(ItemRepository itemRepository, Clock clock) {
        return new DeleteItemService(itemRepository, clock);
    }

    @Bean
    GetItemHistoryUseCase getItemHistoryUseCase(
            ItemRepository itemRepository,
            ItemHistoryRepository historyRepository,
            ItemAccessPolicy accessPolicy
    ) {
        return new GetItemHistoryService(itemRepository, historyRepository, accessPolicy);
    }

    @Bean
    ShareItemUseCase shareItemUseCase(
            ItemRepository itemRepository,
            ItemPermissionRepository permissionRepository,
            UserLookup userLookup,
            Clock clock
    ) {
        return new ShareItemService(itemRepository, permissionRepository, userLookup, clock);
    }

    @Bean
    RevokeShareUseCase revokeShareUseCase(
            ItemRepository itemRepository,
            ItemPermissionRepository permissionRepository
    ) {
        return new RevokeShareService(itemRepository, permissionRepository);
    }
}
