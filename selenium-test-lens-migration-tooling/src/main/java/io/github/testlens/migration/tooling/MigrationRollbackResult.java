package io.github.testlens.migration.tooling;

import java.util.List;

/** Guarded rollback result for one tool-owned transaction. */
public record MigrationRollbackResult(String transactionId, Status status, List<FileResult> files,
        String journalHeadDigest, List<String> issues) {
    public MigrationRollbackResult { files=files==null?List.of():List.copyOf(files);issues=issues==null?List.of():List.copyOf(issues); }
    public enum Status { ROLLED_BACK, PARTIAL_ROLLBACK, ROLLBACK_BLOCKED, RECOVERY_REQUIRED, APPROVAL_REQUIRED }
    public enum FileStatus { RESTORED, NOT_APPLIED, ALREADY_ORIGINAL, USER_MODIFIED, MISSING, WRITE_FAILED }
    public record FileResult(String logicalPath, FileStatus status, List<String> issues) {
        public FileResult { logicalPath=MigrationRunPlan.logical(logicalPath);issues=issues==null?List.of():List.copyOf(issues); }
    }
}
