package com.changeimpact.detector;

import java.util.Objects;

/**
 * Represents a single file change detected via Git or explicitly specified.
 */
public class FileChange {
    private final String filePath;
    private final FileChangeType changeType;
    private final String oldFilePath; // populated for RENAMED

    public FileChange(String filePath, FileChangeType changeType, String oldFilePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath cannot be null").replace('\\', '/').trim();
        this.changeType = changeType != null ? changeType : FileChangeType.MODIFIED;
        this.oldFilePath = oldFilePath != null ? oldFilePath.replace('\\', '/').trim() : null;
    }

    public FileChange(String filePath, FileChangeType changeType) {
        this(filePath, changeType, null);
    }

    public FileChange(String filePath) {
        this(filePath, FileChangeType.MODIFIED, null);
    }

    public String getFilePath() {
        return filePath;
    }

    public FileChangeType getChangeType() {
        return changeType;
    }

    public String getOldFilePath() {
        return oldFilePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileChange that = (FileChange) o;
        return Objects.equals(filePath, that.filePath) && changeType == that.changeType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, changeType);
    }

    @Override
    public String toString() {
        return changeType + ": " + filePath + (oldFilePath != null ? " (from " + oldFilePath + ")" : "");
    }
}
