package dev.maxaure.notetrail.item.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ItemPermissionJpaRepository extends JpaRepository<ItemPermissionJpaEntity, UUID> {

    Optional<ItemPermissionJpaEntity> findByItemIdAndUserId(UUID itemId, UUID userId);

    List<ItemPermissionJpaEntity> findAllByUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("delete from ItemPermissionJpaEntity p where p.itemId = :itemId and p.userId = :userId")
    int deleteByItemIdAndUserId(@Param("itemId") UUID itemId, @Param("userId") UUID userId);
}
