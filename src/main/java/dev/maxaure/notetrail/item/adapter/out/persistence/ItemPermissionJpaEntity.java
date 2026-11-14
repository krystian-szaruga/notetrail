package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.item.domain.ItemPermission;
import dev.maxaure.notetrail.item.domain.ItemRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "item_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_item_permissions_item_user",
                columnNames = {"item_id", "user_id"}
        )
)
class ItemPermissionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ItemRole role;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected ItemPermissionJpaEntity() {
    }

    private ItemPermissionJpaEntity(UUID id, UUID itemId, UUID userId, ItemRole role, Instant grantedAt) {
        this.id = id;
        this.itemId = itemId;
        this.userId = userId;
        this.role = role;
        this.grantedAt = grantedAt;
    }

    static ItemPermissionJpaEntity from(ItemPermission permission) {
        return new ItemPermissionJpaEntity(
                UUID.randomUUID(),
                permission.itemId(),
                permission.userId(),
                permission.role(),
                permission.grantedAt()
        );
    }

    void updateRole(ItemRole newRole) {
        this.role = newRole;
    }

    ItemPermission toDomain() {
        return new ItemPermission(itemId, userId, role, grantedAt);
    }
}
