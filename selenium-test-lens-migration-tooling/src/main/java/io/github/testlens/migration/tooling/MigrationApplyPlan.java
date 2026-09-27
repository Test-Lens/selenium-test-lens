package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable exact-byte execution unit. Proposal edits are never executed directly. */
public record MigrationApplyPlan(int schemaVersion, int algorithmVersion, String applyPlanId,
        String repositoryBindingRef, String worktreeBindingRef, String proposalSetId,
        List<String> selectedProposalIds, String decisionLedgerDigest, List<String> effectiveDecisionRefs,
        List<String> dependencyResolutionRefs, List<String> evidenceDigests, String proposalCheckpointRef,
        String preparationCheckpointRef, List<FilePlan> filePlans, String verificationRequirementsDigest,
        long backupBytes, long targetBytes, String finalDiffRef, String finalDiffDigest, long finalDiffBytes,
        long finalDiffBudget, List<String> requiredCapabilities,
        List<String> limitations) {
    public static final int MAX_PROPOSALS = 32, MAX_FILES = 64, MAX_EDITS = 512, MAX_ISSUES = 1024;
    public static final long MAX_FILE_BYTES = 64L * 1024 * 1024, MAX_AGGREGATE_BYTES = 512L * 1024 * 1024,
            MAX_DIFF_BYTES = 64L * 1024 * 1024;
    public MigrationApplyPlan {
        if (schemaVersion != 1 || algorithmVersion != 1) throw new IllegalArgumentException("apply plan version");
        require(repositoryBindingRef, "migration-repository-binding-v1");
        require(worktreeBindingRef, "migration-worktree-binding-v1");
        require(proposalSetId, "migration-proposal-set-v1");
        selectedProposalIds = ids(selectedProposalIds, "migration-proposal-v1");
        if (selectedProposalIds.isEmpty() || selectedProposalIds.size() > MAX_PROPOSALS) throw new IllegalArgumentException("proposal bound");
        require(decisionLedgerDigest, "migration-decision-ledger-v1");
        effectiveDecisionRefs = ids(effectiveDecisionRefs, "migration-decision-v1");
        dependencyResolutionRefs = ids(dependencyResolutionRefs, "migration-dependency-resolution-v1");
        evidenceDigests = evidenceDigests == null ? List.of() : evidenceDigests.stream().distinct().sorted().toList();
        evidenceDigests.forEach(x -> require(x, "sha256"));
        require(proposalCheckpointRef, "migration-checkpoint-v1"); require(preparationCheckpointRef, "migration-checkpoint-v1");
        filePlans = filePlans == null ? List.of() : filePlans.stream().sorted(Comparator.comparing(FilePlan::logicalPath)).toList();
        if (filePlans.size() > MAX_FILES || filePlans.stream().mapToInt(x -> x.approvedRanges().size()).sum() > MAX_EDITS)
            throw new IllegalArgumentException("file/edit bound");
        require(verificationRequirementsDigest, "migration-verification-requirements-v1");
        if (backupBytes < 0 || targetBytes < 0 || backupBytes > MAX_AGGREGATE_BYTES || targetBytes > MAX_AGGREGATE_BYTES
                || finalDiffBytes < 0 || finalDiffBytes > MAX_DIFF_BYTES
                || finalDiffBudget < 0 || finalDiffBudget > MAX_DIFF_BYTES) throw new IllegalArgumentException("byte bound");
        finalDiffRef = MigrationRunPlan.logical(finalDiffRef); require(finalDiffDigest, "sha256");
        requiredCapabilities = strings(requiredCapabilities); limitations = strings(limitations);
        if (limitations.size() > MAX_ISSUES) throw new IllegalArgumentException("issue bound");
        String expected = id(repositoryBindingRef, worktreeBindingRef, proposalSetId, selectedProposalIds,
                decisionLedgerDigest, effectiveDecisionRefs, dependencyResolutionRefs, evidenceDigests,
                proposalCheckpointRef, preparationCheckpointRef, filePlans, verificationRequirementsDigest,
                backupBytes, targetBytes, finalDiffRef, finalDiffDigest, finalDiffBytes, finalDiffBudget,
                requiredCapabilities, limitations);
        if (!expected.equals(applyPlanId)) throw new IllegalArgumentException("applyPlanId");
    }
    public static MigrationApplyPlan create(String repository, String worktree, String proposalSet,
            List<String> selected, String decisions, List<String> decisionRefs, List<String> resolutionRefs,
            List<String> evidence, String proposalCheckpoint, String preparationCheckpoint, List<FilePlan> files,
            String verificationDigest, long backups, long targets, String finalDiffRef, String finalDiffDigest,
            long finalDiffBytes, long diffBudget, List<String> limitations) {
        List<String> capabilities = List.of(MigrationTargetedAuthorization.Capability.APPLY_APPROVED_SOURCE_CHANGES.name());
        String id = id(repository, worktree, proposalSet, selected, decisions, decisionRefs, resolutionRefs, evidence,
                proposalCheckpoint, preparationCheckpoint, files, verificationDigest, backups, targets,
                finalDiffRef, finalDiffDigest, finalDiffBytes, diffBudget,
                capabilities, limitations);
        return new MigrationApplyPlan(1, 1, id, repository, worktree, proposalSet, selected, decisions, decisionRefs,
                resolutionRefs, evidence, proposalCheckpoint, preparationCheckpoint, files, verificationDigest,
                backups, targets, finalDiffRef, finalDiffDigest, finalDiffBytes, diffBudget, capabilities, limitations);
    }
    public record FilePlan(String logicalPath, String originalSha256, String proposedSha256,
            SourceArtifact originalArtifact, SourceArtifact proposedArtifact, List<ApprovedRange> approvedRanges,
            List<String> sourcePreconditionDigests, List<String> proposalIds, Metadata metadata, boolean writeRequired) {
        public FilePlan {
            logicalPath = MigrationRunPlan.logical(logicalPath); require(originalSha256, "sha256"); require(proposedSha256, "sha256");
            Objects.requireNonNull(originalArtifact); Objects.requireNonNull(proposedArtifact);
            if (!originalArtifact.sha256().equals(originalSha256) || !proposedArtifact.sha256().equals(proposedSha256))
                throw new IllegalArgumentException("artifact/file digest mismatch");
            approvedRanges = approvedRanges == null ? List.of() : approvedRanges.stream().sorted(Comparator.comparingInt(ApprovedRange::startUtf16)).toList();
            int end = -1; for (ApprovedRange range : approvedRanges) { if (range.startUtf16() < end) throw new IllegalArgumentException("overlapping edits"); end = range.endUtf16Exclusive(); }
            sourcePreconditionDigests = strings(sourcePreconditionDigests); proposalIds = ids(proposalIds, "migration-proposal-v1");
            Objects.requireNonNull(metadata);
            if (originalArtifact.byteCount() > MAX_FILE_BYTES || proposedArtifact.byteCount() > MAX_FILE_BYTES) throw new IllegalArgumentException("file bound");
        }
    }
    public record SourceArtifact(String logicalRef, String sha256, long byteCount) {
        public SourceArtifact { logicalRef = MigrationRunPlan.logical(logicalRef); require(sha256, "sha256"); if (byteCount < 0 || byteCount > MAX_FILE_BYTES) throw new IllegalArgumentException("artifact size"); }
    }
    public record ApprovedRange(int startUtf16, int endUtf16Exclusive, String originalConstructDigest,
            String replacementDigest, String semanticBeforeRef, String semanticAfterRef) {
        public ApprovedRange { if (startUtf16 < 0 || endUtf16Exclusive < startUtf16) throw new IllegalArgumentException("range"); require(originalConstructDigest, "migration-source-construct-v1"); require(replacementDigest, "sha256"); Objects.requireNonNull(semanticBeforeRef); Objects.requireNonNull(semanticAfterRef); }
    }
    public record Metadata(boolean dosReadOnly, List<String> posixPermissions) {
        public Metadata { posixPermissions = posixPermissions == null ? List.of() : posixPermissions.stream().distinct().sorted().toList(); }
    }
    static String id(String repository, String worktree, String proposalSet, List<String> selected, String decisions,
            List<String> decisionRefs, List<String> resolutions, List<String> evidence, String proposalCheckpoint,
            String preparationCheckpoint, List<FilePlan> files, String verification, long backupBytes, long targetBytes,
            String finalDiffRef, String finalDiffDigest, long finalDiffBytes, long diffBudget,
            List<String> capabilities, List<String> limitations) {
        List<String> f = new ArrayList<>(List.of(repository, worktree, proposalSet, decisions, proposalCheckpoint,
                preparationCheckpoint, verification, Long.toString(backupBytes), Long.toString(targetBytes),
                finalDiffRef, finalDiffDigest, Long.toString(finalDiffBytes), Long.toString(diffBudget)));
        selected.stream().sorted().forEach(x -> { f.add("PROPOSAL"); f.add(x); });
        decisionRefs.stream().sorted().forEach(x -> { f.add("DECISION"); f.add(x); });
        resolutions.stream().sorted().forEach(x -> { f.add("RESOLUTION"); f.add(x); });
        evidence.stream().sorted().forEach(x -> { f.add("EVIDENCE"); f.add(x); });
        files.stream().sorted(Comparator.comparing(FilePlan::logicalPath)).forEach(x -> {
            f.add("FILE"); f.add(x.logicalPath()); f.add(x.originalSha256()); f.add(x.proposedSha256());
            f.add(x.originalArtifact().logicalRef()); f.add(Long.toString(x.originalArtifact().byteCount()));
            f.add(x.proposedArtifact().logicalRef()); f.add(Long.toString(x.proposedArtifact().byteCount()));
            f.add(Boolean.toString(x.writeRequired())); f.add(Boolean.toString(x.metadata().dosReadOnly()));
            x.metadata().posixPermissions().forEach(f::add);
            x.approvedRanges().forEach(r -> { f.add(Integer.toString(r.startUtf16())); f.add(Integer.toString(r.endUtf16Exclusive())); f.add(r.originalConstructDigest()); f.add(r.replacementDigest()); f.add(r.semanticBeforeRef()); f.add(r.semanticAfterRef()); });
            x.sourcePreconditionDigests().forEach(f::add); x.proposalIds().forEach(f::add);
        });
        capabilities.stream().sorted().forEach(f::add); limitations.stream().sorted().forEach(f::add);
        return "migration-apply-plan-v1:sha256:" + MigrationDigests.digest("migration-apply-plan-v1", f.toArray(String[]::new));
    }
    static String sourcePreconditionDigest(MigrationSourcePrecondition p) {
        return "migration-source-precondition-v1:sha256:" + MigrationDigests.digest("migration-source-precondition-v1",
                p.logicalPath(), p.fileSha256(), p.encoding().name(), p.newline().name(), p.expectedNodeKind(),
                p.declarationRef(), p.constructIdentityDigest(), Integer.toString(p.exactCharacterRange().startUtf16()),
                Integer.toString(p.exactCharacterRange().endUtf16Exclusive()), p.originalConstructDigest(),
                p.currentSemanticDigest(), p.proposedSemanticDigest(), p.symlinkState().name(), p.repositoryBindingRef(),
                p.worktreeBindingRef(), p.checkpointRef(), p.alreadyAppliedSemanticState());
    }
    private static List<String> ids(List<String> values, String domain) { List<String> out = strings(values); out.forEach(x -> require(x, domain)); return out; }
    private static List<String> strings(List<String> values) { return values == null ? List.of() : values.stream().distinct().sorted().toList(); }
    static void require(String value, String domain) {
        String pattern = "sha256".equals(domain) ? "sha256:[0-9a-f]{64}"
                : java.util.regex.Pattern.quote(domain) + ":sha256:[0-9a-f]{64}";
        if (value == null || !value.matches(pattern)) throw new IllegalArgumentException(domain);
    }
}
