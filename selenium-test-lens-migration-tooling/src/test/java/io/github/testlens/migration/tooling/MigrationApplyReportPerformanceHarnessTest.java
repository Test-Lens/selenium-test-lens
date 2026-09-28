package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Diagnostic-only bounded report measurements. There are deliberately no timing assertions. */
class MigrationApplyReportPerformanceHarnessTest {
    @Test
    void boundedReportModelJsonAndHtmlProfiles() throws Exception {
        int[][] profiles = {{1, 1, 10}, {10, 10, 100}, {32, 64, 512}};
        MigrationApplyReportJson json = new MigrationApplyReportJson();
        MigrationApplyReportHtml html = new MigrationApplyReportHtml();
        for (int[] profile : profiles) {
            long memoryBefore = used();
            long start = System.nanoTime();
            MigrationApplyReport report = report(profile[0], profile[1], profile[2]);
            long buildNanos = System.nanoTime() - start;
            start = System.nanoTime();
            byte[] jsonBytes = json.write(report);
            MigrationApplyReport reopened = json.read(jsonBytes);
            long jsonNanos = System.nanoTime() - start;
            start = System.nanoTime();
            byte[] htmlBytes = html.render(report);
            long htmlNanos = System.nanoTime() - start;
            if (!report.equals(reopened)) {
                throw new AssertionError("report roundtrip");
            }
            System.out.printf(
                    "MIGRATION_S11B3_REPORT proposals=%d files=%d stages=%d buildMs=%.2f jsonMs=%.2f htmlMs=%.2f jsonBytes=%d htmlBytes=%d memoryDelta=%d%n",
                    profile[0], profile[1], profile[2], buildNanos / 1e6, jsonNanos / 1e6,
                    htmlNanos / 1e6, jsonBytes.length, htmlBytes.length,
                    Math.max(0, used() - memoryBefore));
        }
    }

    private static MigrationApplyReport report(int proposalCount, int fileCount, int stageCount) {
        List<String> selected = new ArrayList<>();
        List<MigrationApplyReport.ProposalSummary> proposals = new ArrayList<>();
        for (int i = 0; i < proposalCount; i++) {
            String proposal = ref("migration-proposal-v1", i + 1);
            selected.add(proposal);
            proposals.add(new MigrationApplyReport.ProposalSummary(proposal, MigrationProposal.Category.LOCATOR,
                    "LOCATOR_REPLACEMENT", ref("migration-semantic-target-v1", i + 1), List.of(),
                    MigrationVerificationResult.ProposalStatus.VERIFIED));
        }
        List<MigrationApplyReport.FileSummary> files = new ArrayList<>();
        for (int i = 0; i < fileCount; i++) {
            files.add(new MigrationApplyReport.FileSummary("src/test/java/F" + i + ".java",
                    MigrationApplyResult.FileStatus.APPLIED,
                    MigrationApplyReport.CurrentFileState.CURRENT_TOOL_APPLIED,
                    MigrationApplyReport.RollbackState.AVAILABLE, sha(i + 1), 1, false, true,
                    List.of(selected.get(i % selected.size())), List.of()));
        }
        List<MigrationApplyReport.StageCoverage> stages = new ArrayList<>();
        MigrationVerificationPlan.StageType[] types = MigrationVerificationPlan.StageType.values();
        for (int i = 0; i < stageCount; i++) {
            stages.add(new MigrationApplyReport.StageCoverage(ref("migration-verification-stage-v1", i + 1),
                    types[i % types.length], MigrationVerificationPlan.Necessity.REQUIRED,
                    MigrationVerificationResult.StageStatus.PASSED, List.of()));
        }
        MigrationApplyReport.VerificationCoverage coverage = new MigrationApplyReport.VerificationCoverage(
                ref("migration-verification-plan-v1", 1), ref("migration-verification-result-v1", 1),
                MigrationVerificationResult.Status.VERIFIED, stageCount, stageCount, stages, List.of(), List.of());
        MigrationRecoverySummary recovery = new MigrationRecoverySummary(
                MigrationRecoverySummary.Situation.NO_RECOVERY_REQUIRED, List.of(), List.of(), List.of());
        MigrationCommitReadiness readiness = new MigrationCommitReadiness(
                MigrationCommitReadiness.State.COMMIT_READY_MANUAL, List.of(),
                List.of("Review the exact owned diff", "Commit manually"), "migrate tests for headless compatibility");
        return MigrationApplyReport.create(ref("migration-repository-binding-v1", 1),
                ref("migration-worktree-binding-v1", 1), ref("migration-proposal-set-v1", 1), selected,
                List.of(), ref("migration-apply-plan-v1", 1), ref("migration-apply-transaction-v1", 1),
                MigrationApplyReport.Status.APPLIED_VERIFIED, MigrationApplyResult.Status.APPLIED, proposals, files,
                List.of(), ref("migration-checkpoint-v1", 1), recovery,
                ref("migration-recovery-action-plan-v1", 1), coverage,
                ref("migration-verification-result-v1", 1), List.of(ref("migration-verification-result-v1", 1)),
                List.of(), "apply/diffs/owned.patch", sha(1), 0,
                new MigrationApplyReport.PreExistingUserState(List.of(), List.of(), List.of(), List.of()),
                new MigrationApplyReport.IndexState(List.of(), List.of(), List.of(), List.of()),
                MigrationApplyReport.CurrentStateRelation.MATCHES_VERIFIED_STATE, readiness,
                List.of(MigrationApplyReport.NextAction.REVIEW_OWNED_DIFF,
                        MigrationApplyReport.NextAction.MANUAL_COMMIT,
                        MigrationApplyReport.NextAction.RUN_RELEASE_HARDENING),
                List.of(), List.of(), List.of(), ref("migration-release-hardening-handoff-v1", 1));
    }

    private static String ref(String domain, int value) {
        return domain + ":sha256:" + Integer.toHexString(value).repeat(64).substring(0, 64);
    }

    private static String sha(int value) {
        return "sha256:" + Integer.toHexString(value).repeat(64).substring(0, 64);
    }

    private static long used() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
