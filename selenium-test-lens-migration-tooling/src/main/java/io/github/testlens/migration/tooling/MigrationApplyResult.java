package io.github.testlens.migration.tooling;

import java.util.List;

/** Structured source-apply result; APPLIED is deliberately not VERIFIED. */
public record MigrationApplyResult(String applyPlanId, String transactionId, Status status,
        List<FileResult> files, String beforeCheckpointRef, String postApplyCheckpointRef,
        boolean rollbackAvailable, String finalDiffRef, String finalDiffDigest,
        String journalHeadDigest, List<String> issues) {
    public MigrationApplyResult { files=files==null?List.of():List.copyOf(files);issues=issues==null?List.of():issues.stream().distinct().sorted().limit(MigrationApplyPlan.MAX_ISSUES).toList(); }
    public enum Status { APPLIED, ALREADY_APPLIED, ABORTED, STALE_PRECONDITION, CONFLICTING_PROPOSAL,
        DEPENDENCY_UNSATISFIED, APPROVAL_REQUIRED, APPLY_LOCK_UNAVAILABLE, WRITE_FAILED,
        VERIFY_DIGEST_FAILED, PARTIAL_APPLY, COMPENSATED, PARTIAL_COMPENSATION, RECOVERY_REQUIRED }
    public enum FileStatus { APPLIED, ALREADY_APPLIED, NOT_ATTEMPTED, STALE_PRECONDITION, WRITE_FAILED,
        VERIFY_DIGEST_FAILED, COMPENSATED, COMPENSATION_BLOCKED }
    public record FileResult(String logicalPath, FileStatus status, String observedDigest, List<String> issues) {
        public FileResult { logicalPath=MigrationRunPlan.logical(logicalPath);issues=issues==null?List.of():List.copyOf(issues); }
    }
}
