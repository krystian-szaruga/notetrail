package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.item.application.port.out.ItemHistoryRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemPermissionRepository;
import dev.maxaure.notetrail.item.application.port.out.ItemRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ItemPersistenceConfiguration {

    @Bean
    ItemRepository itemRepository(ItemJpaRepository repository) {
        return new ItemPersistenceAdapter(repository);
    }

    @Bean
    ItemPermissionRepository itemPermissionRepository(ItemPermissionJpaRepository repository) {
        return new ItemPermissionPersistenceAdapter(repository);
    }

    @Bean
    ItemHistoryRepository itemHistoryRepository(EntityManager entityManager) {
        return new ItemHistoryAdapter(entityManager);
    }
}
