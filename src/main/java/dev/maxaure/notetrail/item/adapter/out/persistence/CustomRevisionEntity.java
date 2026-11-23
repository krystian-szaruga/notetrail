package dev.maxaure.notetrail.item.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionMapping;

@Entity
@Table(name = "revinfo")
@RevisionEntity(CustomRevisionListener.class)
class CustomRevisionEntity extends RevisionMapping {

    @Column(name = "changed_by", length = 64)
    private String changedBy;

    String changedBy() {
        return changedBy;
    }

    void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }
}
