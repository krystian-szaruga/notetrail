package dev.maxaure.notetrail.item.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ItemJpaRepository extends JpaRepository<ItemJpaEntity, UUID> {

    @Query("select i from ItemJpaEntity i where i.id = :id and i.deleted = false")
    Optional<ItemJpaEntity> findByIdAndDeletedFalse(@Param("id") UUID id);

    @Query("select i from ItemJpaEntity i where i.ownerId = :ownerId and i.deleted = false")
    List<ItemJpaEntity> findAllByOwnerIdAndDeletedFalse(@Param("ownerId") UUID ownerId);

    @Query("select i from ItemJpaEntity i where i.id in :ids and i.deleted = false")
    List<ItemJpaEntity> findAllByIdInAndDeletedFalse(@Param("ids") Collection<UUID> ids);
}
