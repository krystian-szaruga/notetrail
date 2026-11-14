package dev.maxaure.notetrail.item.domain;

public enum ItemRole {
    OWNER,
    EDITOR,
    VIEWER;

    public boolean canEdit() {
        return this != VIEWER;
    }

    public boolean canDelete() {
        return this == OWNER;
    }

    public boolean canManagePermissions() {
        return this == OWNER;
    }
}
