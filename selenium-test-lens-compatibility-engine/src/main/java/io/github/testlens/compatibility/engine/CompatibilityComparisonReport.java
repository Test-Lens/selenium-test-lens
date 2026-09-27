package io.github.testlens.compatibility.engine;

import java.util.List;
import java.util.Objects;

/** Sanitized, deterministic result of comparing two supplied manifest sets. */
public record CompatibilityComparisonReport(
        int schemaVersion, int algorithmVersion, AnalysisMode analysisMode,
        ComparisonIntent intent, Coverage coverage, List<TestComparison> testComparisons,
        List<Unmatched> unmatchedBaseline, List<Unmatched> unmatchedVariant,
        List<AmbiguousMatch> ambiguousMatches, List<Finding> findings,
        List<Recommendation> recommendations, List<String> issues, List<String> outputLimitations) {
    public static final int SCHEMA_VERSION = 1;
    public static final int ALGORITHM_VERSION = 1;
    public CompatibilityComparisonReport {
        if (schemaVersion != 1 || algorithmVersion != 1) throw new IllegalArgumentException("unsupported report version");
        analysisMode = Objects.requireNonNull(analysisMode); intent = Objects.requireNonNull(intent); coverage = Objects.requireNonNull(coverage);
        testComparisons = copy(testComparisons, 25_000, "comparisons"); unmatchedBaseline = copy(unmatchedBaseline, 25_000, "unmatched baseline");
        unmatchedVariant = copy(unmatchedVariant, 25_000, "unmatched variant"); ambiguousMatches = copy(ambiguousMatches, 25_000, "ambiguous");
        findings = copy(findings, 250_000, "findings"); recommendations = copy(recommendations, 250_000, "recommendations");
        issues = strings(issues, 1024, 512); outputLimitations = strings(outputLimitations, 1024, 512);
    }
    public enum AnalysisMode { COMPARE }
    public enum Comparability { COMPARABLE, COMPARABLE_WITH_LIMITATIONS, NOT_COMPARABLE, UNKNOWN }
    public enum DimensionState { INTENDED_DIFFERENCE, EQUAL_KNOWN, DIFFERENT, UNKNOWN_LEFT, UNKNOWN_RIGHT, UNKNOWN_BOTH, CONFLICTED, NOT_APPLICABLE }
    public enum BehaviorState { SAME_KNOWN, DIFFERENT, UNKNOWN, PARTIAL, EXPECTED_DIFFERENCE }
    public enum BehaviorChannel { EXECUTION_BEHAVIOR, OBSERVABILITY_BEHAVIOR, EXPECTED_PRESET_DIFFERENCE }
    public enum Outcome { NO_MEANINGFUL_DIFFERENCE, VARIANT_REGRESSION, VARIANT_IMPROVEMENT, PASS_WITH_BEHAVIORAL_DELTA, BOTH_PASS_DIFFERENT_BEHAVIOR, BASELINE_ALREADY_FAILED_SAME_FAILURE, BASELINE_ALREADY_FAILED_DIFFERENT_FAILURE, EXPECTED_OBSERVABILITY_DIFFERENCE, INCONCLUSIVE, NOT_COMPARABLE }
    public enum FailureRelationship { SAME_KNOWN, DIFFERENT_KNOWN, UNKNOWN, PARTIAL, NOT_APPLICABLE }
    public enum FindingCategory { CONFIGURATION, VIEWPORT_RESPONSIVE, LOCATOR, INTERACTION, SCROLL_TIMING, WINDOWS_TABS, DOWNLOAD_UPLOAD, AUTH_SESSION, APPLICATION_BEHAVIOR, TEST_ASSUMPTION, INFRASTRUCTURE, UNKNOWN }
    public enum StatementKind { FACT, CONFIGURATION_DIFFERENCE, OBSERVATION, HYPOTHESIS, CONFIRMED_CAUSE, MISSING_DATA }
    public enum CausalState { OBSERVATION, HYPOTHESIS, CONFIRMED_CAUSE }
    public enum Confidence { LOW, MEDIUM, HIGH }
    public enum Severity { INFO, REVIEW, WARNING, ERROR }
    public enum RecommendationCode { ALIGN_VIEWPORT_AND_RERUN, ALIGN_DATASET_AND_RERUN, ALIGN_BROWSER_VERSION_AND_RERUN, ALIGN_TEST_REVISION, ALIGN_SUT_REVISION, COLLECT_MISSING_METADATA, COLLECT_MORE_EVIDENCE, REVIEW_INTERACTION_ASSUMPTION, REVIEW_LOCATOR_AFTER_CONFIG_ALIGNMENT, LIVE_VALIDATE_LOCATOR, REMOVE_NATIVE_GUI_DEPENDENCY, INVESTIGATE_APPLICATION_DIFFERENCE, REVIEW_INFRASTRUCTURE, KEEP_CURRENT_TEST, INCONCLUSIVE }
    public enum CodeChangeRequired { YES, NO, UNKNOWN }

    public record Coverage(int baselineSupplied, int variantSupplied, int baselineAccepted, int variantAccepted,
                           int duplicateIdsCoalesced, int matched, int unmatchedBaseline, int unmatchedVariant,
                           int ambiguous, int comparable, int comparableWithLimitations, int unknown,
                           int notComparable, int noMeaningfulDifference, int behavioralDeltas,
                           int regressions, int baselineAlreadyFailed, int evidenceLimited) { }
    public record DimensionAssessment(String dimension, DimensionState state, boolean critical, String reasonCode) {
        public DimensionAssessment { dimension=safe(dimension,128); state=Objects.requireNonNull(state); reasonCode=safe(reasonCode,128); }
    }
    public record ComparabilityResult(Comparability status, List<String> reasonCodes, List<DimensionAssessment> dimensions) {
        public ComparabilityResult { status=Objects.requireNonNull(status); reasonCodes=strings(reasonCodes,128,128); dimensions=copy(dimensions,64,"dimensions"); }
    }
    public record BehaviorGroupDiff(String group, BehaviorState state, BehaviorChannel channel, String reasonCode,
                                    String baselineSummary, String variantSummary) {
        public BehaviorGroupDiff { group=safe(group,128); state=Objects.requireNonNull(state); channel=Objects.requireNonNull(channel); reasonCode=safe(reasonCode,128); baselineSummary=safe(baselineSummary,512); variantSummary=safe(variantSummary,512); }
    }
    public record BehaviorDiff(List<BehaviorGroupDiff> groups, boolean executionDelta, boolean expectedPresetDifference, boolean evidenceLimited) {
        public BehaviorDiff { groups=copy(groups,32,"behavior groups"); }
    }
    public record ManifestSide(String manifestId, CompatibilityRunManifest.ResultStatus terminalStatus,
                               int attemptCount, boolean passedAfterRetry, List<CompatibilityRunManifest.ResultStatus> attemptStatuses) {
        public ManifestSide { manifestId=safe(manifestId,128); terminalStatus=Objects.requireNonNull(terminalStatus); attemptStatuses=List.copyOf(attemptStatuses); }
    }
    public record TestComparison(String testIdentityRef, String invocationRef, ManifestSide baseline, ManifestSide variant,
                                 ComparabilityResult comparability, BehaviorDiff behaviorDiff,
                                 FailureRelationship failureRelationship, Outcome outcome, boolean baselineAlreadyFailed,
                                 List<String> findingIds, List<RecommendationCode> recommendationCodes,
                                 CodeChangeRequired codeChangeRequired, List<String> verificationScope, List<String> limitations) {
        public TestComparison { testIdentityRef=safe(testIdentityRef,128); invocationRef=safe(invocationRef,128); Objects.requireNonNull(baseline);Objects.requireNonNull(variant);Objects.requireNonNull(comparability);Objects.requireNonNull(behaviorDiff);Objects.requireNonNull(failureRelationship);Objects.requireNonNull(outcome);findingIds=strings(findingIds,64,128);recommendationCodes=copy(recommendationCodes,32,"test recommendations");Objects.requireNonNull(codeChangeRequired);verificationScope=strings(verificationScope,16,256);limitations=strings(limitations,32,256); }
    }
    public record EvidenceStatement(StatementKind kind, String code, List<String> refs) {
        public EvidenceStatement { kind=Objects.requireNonNull(kind); code=safe(code,128); refs=strings(refs,16,256); }
    }
    public record Finding(String findingId, String testIdentityRef, String invocationRef, FindingCategory category,
                          String code, Severity severity, CausalState causalState, Confidence confidence,
                          List<EvidenceStatement> evidence, List<String> supportingRefs, List<String> conflictingRefs) {
        public Finding { findingId=safe(findingId,128);testIdentityRef=safe(testIdentityRef,128);invocationRef=safe(invocationRef,128);category=Objects.requireNonNull(category);code=safe(code,128);severity=Objects.requireNonNull(severity);causalState=Objects.requireNonNull(causalState);confidence=Objects.requireNonNull(confidence);evidence=copy(evidence,32,"evidence");supportingRefs=strings(supportingRefs,32,256);conflictingRefs=strings(conflictingRefs,32,256); }
    }
    public record Recommendation(RecommendationCode code, String testIdentityRef, String reasonCode, CodeChangeRequired codeChangeRequired) {
        public Recommendation { code=Objects.requireNonNull(code);testIdentityRef=safe(testIdentityRef,128);reasonCode=safe(reasonCode,128);codeChangeRequired=Objects.requireNonNull(codeChangeRequired); }
    }
    public record Unmatched(String testIdentityRef, String manifestId, String reasonCode) { public Unmatched { testIdentityRef=safe(testIdentityRef,128);manifestId=safe(manifestId,128);reasonCode=safe(reasonCode,128); } }
    public record AmbiguousMatch(String matchKey, List<String> baselineManifestIds, List<String> variantManifestIds) { public AmbiguousMatch { matchKey=safe(matchKey,256);baselineManifestIds=strings(baselineManifestIds,64,128);variantManifestIds=strings(variantManifestIds,64,128); } }

    private static <T> List<T> copy(List<T> v,int max,String field){v=v==null?List.of():List.copyOf(v);if(v.size()>max)throw new IllegalArgumentException(field+" exceeds "+max);return v;}
    private static List<String> strings(List<String> v,int max,int len){v=v==null?List.of():v.stream().map(x->safe(x,len)).distinct().sorted().toList();if(v.size()>max)throw new IllegalArgumentException("string collection exceeds "+max);return v;}
    private static String safe(String v,int max){v=v==null?"":v;if(v.codePointCount(0,v.length())>max||v.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("unsafe/bounded report string");return v;}
}
