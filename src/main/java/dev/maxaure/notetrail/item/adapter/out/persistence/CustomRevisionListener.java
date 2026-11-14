package dev.maxaure.notetrail.item.adapter.out.persistence;

import dev.maxaure.notetrail.security.CurrentUser;
import org.hibernate.envers.RevisionListener;

class CustomRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        CustomRevisionEntity revision = (CustomRevisionEntity) revisionEntity;
        CurrentUser.get().ifPresent(user -> revision.setChangedBy(user.login()));
    }
}
