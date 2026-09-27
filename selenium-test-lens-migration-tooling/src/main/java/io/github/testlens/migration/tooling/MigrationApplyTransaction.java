package io.github.testlens.migration.tooling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Durable apply ownership and append-only journal event model. */
public final class MigrationApplyTransaction {
    private MigrationApplyTransaction() { }
    public static String id(MigrationApplyPlan plan) {
        return "migration-apply-transaction-v1:sha256:" + MigrationDigests.digest("migration-apply-transaction-v1",
                plan.applyPlanId(), plan.preparationCheckpointRef(), "attempt-v1");
    }
    public enum State { PREPARED, WRITING, APPLIED, PARTIAL_APPLY, COMPENSATED, PARTIAL_COMPENSATION,
        VERIFICATION_PENDING, ROLLED_BACK, PARTIAL_ROLLBACK, ROLLBACK_BLOCKED, RECOVERY_REQUIRED }
    public enum EventType { PREPARED, WRITING, REPLACE_STARTED, FILE_REPLACED, APPLY_COMPLETED, PARTIAL_APPLY,
        COMPENSATION_STARTED, FILE_COMPENSATED, COMPENSATION_COMPLETED, VERIFICATION_PENDING,
        ROLLBACK_STARTED, FILE_ROLLED_BACK, ROLLBACK_COMPLETED, RECOVERY_RECONCILED, RECOVERY_REQUIRED }
    public record Event(int schemaVersion, int algorithmVersion, String eventDigest, String previousEventDigest,
            String transactionId, long sequence, EventType type, String logicalPath, String observedDigest,
            String issue, Instant operationalTimestamp) {
        public Event {
            if (schemaVersion != 1 || algorithmVersion != 1 || sequence < 0) throw new IllegalArgumentException("event version/sequence");
            MigrationApplyPlan.require(transactionId, "migration-apply-transaction-v1"); Objects.requireNonNull(type);
            if (previousEventDigest != null) MigrationApplyPlan.require(previousEventDigest, "migration-apply-event-v1");
            if (logicalPath != null) logicalPath = MigrationRunPlan.logical(logicalPath);
            if (observedDigest != null) MigrationApplyPlan.require(observedDigest, "sha256");
            if (issue != null && (issue.length() > 8192 || issue.indexOf('\0') >= 0)) throw new IllegalArgumentException("issue");
            String expected = digest(previousEventDigest, transactionId, sequence, type, logicalPath, observedDigest, issue);
            if (!expected.equals(eventDigest)) throw new IllegalArgumentException("eventDigest");
            Objects.requireNonNull(operationalTimestamp);
        }
        public static Event create(String previous, String transaction, long sequence, EventType type,
                String path, String observed, String issue) {
            return new Event(1, 1, digest(previous, transaction, sequence, type, path, observed, issue), previous,
                    transaction, sequence, type, path, observed, issue, Instant.now());
        }
        private static String digest(String previous, String transaction, long sequence, EventType type,
                String path, String observed, String issue) {
            return "migration-apply-event-v1:sha256:" + MigrationDigests.digest("migration-apply-event-v1",
                    previous == null ? "" : previous, transaction, Long.toString(sequence), type.name(),
                    path == null ? "" : path, observed == null ? "" : observed, issue == null ? "" : issue);
        }
    }
    public record OwnershipFile(String logicalPath, String originalSha256, String proposedSha256,
            String originalArtifactRef, String proposedArtifactRef, List<String> proposalIds,
            List<MigrationApplyPlan.ApprovedRange> approvedRanges, MigrationApplyPlan.Metadata metadata) {
        public OwnershipFile { logicalPath = MigrationRunPlan.logical(logicalPath); MigrationApplyPlan.require(originalSha256,"sha256"); MigrationApplyPlan.require(proposedSha256,"sha256"); originalArtifactRef=MigrationRunPlan.logical(originalArtifactRef); proposedArtifactRef=MigrationRunPlan.logical(proposedArtifactRef); proposalIds=proposalIds.stream().distinct().sorted().toList(); approvedRanges=List.copyOf(approvedRanges); Objects.requireNonNull(metadata); }
    }
    public record OwnershipRegistry(int schemaVersion, int algorithmVersion, String registryDigest,
            String transactionId, String applyPlanId, List<OwnershipFile> files) {
        public OwnershipRegistry { if(schemaVersion!=1||algorithmVersion!=1)throw new IllegalArgumentException("registry version");MigrationApplyPlan.require(transactionId,"migration-apply-transaction-v1");MigrationApplyPlan.require(applyPlanId,"migration-apply-plan-v1");files=files.stream().sorted(Comparator.comparing(OwnershipFile::logicalPath)).toList();String expected=digest(transactionId,applyPlanId,files);if(!expected.equals(registryDigest))throw new IllegalArgumentException("registryDigest"); }
        public static OwnershipRegistry create(String transaction,String plan,List<OwnershipFile>files){return new OwnershipRegistry(1,1,digest(transaction,plan,files),transaction,plan,files);}
        private static String digest(String transaction,String plan,List<OwnershipFile>files){List<String>f=new ArrayList<>(List.of(transaction,plan));files.stream().sorted(Comparator.comparing(OwnershipFile::logicalPath)).forEach(x->{f.add(x.logicalPath());f.add(x.originalSha256());f.add(x.proposedSha256());f.add(x.originalArtifactRef());f.add(x.proposedArtifactRef());x.proposalIds().forEach(f::add);x.approvedRanges().forEach(r->{f.add(Integer.toString(r.startUtf16()));f.add(Integer.toString(r.endUtf16Exclusive()));f.add(r.originalConstructDigest());f.add(r.replacementDigest());f.add(r.semanticBeforeRef());f.add(r.semanticAfterRef());});f.add(Boolean.toString(x.metadata().dosReadOnly()));x.metadata().posixPermissions().forEach(f::add);});return"migration-apply-ownership-v1:sha256:"+MigrationDigests.digest("migration-apply-ownership-v1",f.toArray(String[]::new));}
    }
}
