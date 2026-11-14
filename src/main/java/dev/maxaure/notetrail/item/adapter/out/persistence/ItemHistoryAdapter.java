package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.item.application.port.out.ItemHistoryRepository;
import dev.maxaure.notetrail.item.domain.ItemRevision;
import dev.maxaure.notetrail.item.domain.RevisionType;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class ItemHistoryAdapter implements ItemHistoryRepository {

    private final EntityManager entityManager;

    ItemHistoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemRevision> findRevisions(UUID itemId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<?> rows = reader.createQuery()
                .forRevisionsOfEntity(ItemJpaEntity.class, false, true)
                .add(AuditEntity.id().eq(itemId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        List<ItemRevision> revisions = new ArrayList<>();
        for (Object row : rows) {
            Object[] fields = (Object[]) row;
            ItemJpaEntity item = (ItemJpaEntity) fields[0];
            CustomRevisionEntity revision = (CustomRevisionEntity) fields[1];
            RevisionType type = RevisionType.valueOf(
                    ((org.hibernate.envers.RevisionType) fields[2]).name()
            );
            revisions.add(new ItemRevision(
                    revision.getId(),
                    type,
                    Instant.ofEpochMilli(revision.getTimestamp()),
                    revision.changedBy(),
                    item.title(),
                    item.content()
            ));
        }
        return revisions;
    }
}
