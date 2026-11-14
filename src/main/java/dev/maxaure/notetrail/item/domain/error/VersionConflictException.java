package dev.maxaure.notetrail.item.domain.error;

public class VersionConflictException extends RuntimeException {

    private final int currentVersion;

    public VersionConflictException(int currentVersion) {
        super("item version conflict, current version is " + currentVersion);
        this.currentVersion = currentVersion;
    }

    public int currentVersion() {
        return currentVersion;
    }
}
