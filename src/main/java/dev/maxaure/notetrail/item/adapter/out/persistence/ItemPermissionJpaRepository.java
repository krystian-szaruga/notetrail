package dev.maxaure.notetrail.item.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ItemPermissionJpaRepository extends JpaRepository<ItemPermissionJpaEntity, UUID> {

    Optional<ItemPermissionJpaEntity> findByItemIdAndUserId(UUID itemId, UUID userId);

    List<ItemPermissionJpaEntity> findAllByUserId(UUID userId);

    @Transactional
    long deleteByItemIdAndUserId(UUID itemId, UUID userId);
}
