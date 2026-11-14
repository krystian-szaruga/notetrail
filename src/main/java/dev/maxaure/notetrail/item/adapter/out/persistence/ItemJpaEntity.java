package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.item.domain.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.envers.Audited;

import java.time.Instant;
import java.util.UUID;

@Entity
@Audited
@Table(name = "items")
class ItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ItemJpaEntity() {
    }

    private ItemJpaEntity(
            UUID id,
            UUID ownerId,
            String title,
            String content,
            int version,
            boolean deleted,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.title = title;
        this.content = content;
        this.version = version;
        this.deleted = deleted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    static ItemJpaEntity from(Item item) {
        return new ItemJpaEntity(
                item.id(),
                item.ownerId(),
                item.title(),
                item.content(),
                item.version(),
                item.deleted(),
                item.createdAt(),
                item.updatedAt()
        );
    }

    String title() {
        return title;
    }

    String content() {
        return content;
    }

    Item toDomain() {
        return new Item(
                id,
                ownerId,
                title,
                content,
                version,
                deleted,
                createdAt,
                updatedAt
        );
    }
}
