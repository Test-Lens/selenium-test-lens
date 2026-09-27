package io.github.testlens.migration.tooling;

import java.util.List;

/** Read-only classification of an interrupted source transaction. */
public record MigrationRecoveryPlan(String transactionId, Situation situation, List<FileState> files,
        String journalHeadDigest, List<String> issues) {
    public MigrationRecoveryPlan { files=files==null?List.of():List.copyOf(files);issues=issues==null?List.of():List.copyOf(issues); }
    public enum TargetState { ORIGINAL, TOOL_APPLIED, UNKNOWN_MODIFIED, MISSING }
    public enum Situation { SAFE_TO_ABANDON, ALL_APPLIED_NEEDS_RECONCILIATION,
        GUARDED_ROLLBACK_AVAILABLE, ROLLBACK_PARTIALLY_BLOCKED, RECOVERY_REQUIRED }
    public record FileState(String logicalPath, TargetState state, String observedDigest) { public FileState { logicalPath=MigrationRunPlan.logical(logicalPath); } }
}
