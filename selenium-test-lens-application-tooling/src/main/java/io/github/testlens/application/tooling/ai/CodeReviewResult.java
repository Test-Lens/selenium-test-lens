package io.github.testlens.application.tooling.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Structured, bounded test-code review result. @since 0.5.0 */
public record CodeReviewResult(ContractHeader header, Verdict verdict, List<String> reviewedArtifactRefs,
                               List<Finding> findings) {
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    public CodeReviewResult(ContractHeader header, List<Finding> findings) {
        this(header, infer(findings), List.of(), findings);
    }

    public CodeReviewResult {
        if (header == null || verdict == null) throw new IllegalArgumentException("header and verdict are required");
        // Reviewer-authored header prose is neither evidence nor correction guidance. Keep only the
        // closed contract state; bounded findings below are the sole persisted/forwarded feedback.
        header = new ContractHeader(header.schemaVersion(), header.status(), List.of(), List.of(),
                header.confidence());
        reviewedArtifactRefs = (reviewedArtifactRefs == null ? List.<String>of() : reviewedArtifactRefs).stream()
                .map(CodeReviewResult::safeId).distinct().sorted().toList();
        findings = (findings == null ? List.<Finding>of() : findings).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Finding::severity).thenComparing(Finding::ruleId)
                        .thenComparing(Finding::artifactRef).thenComparing(Finding::safeLocation))
                .toList();
        boolean hasError = findings.stream().anyMatch(value -> value.severity() == Severity.ERROR);
        if (verdict == Verdict.APPROVE && hasError) {
            throw new IllegalArgumentException("APPROVE cannot contain ERROR findings");
        }
        if (verdict != Verdict.APPROVE && !hasError) {
            throw new IllegalArgumentException("A non-approval verdict requires an ERROR finding");
        }
    }

    private static Verdict infer(List<Finding> findings) {
        return findings != null && findings.stream().anyMatch(value -> value.severity() == Severity.ERROR)
                ? Verdict.REQUEST_CHANGES : Verdict.APPROVE;
    }

    private static String safeId(String value) {
        if (value == null || !SAFE_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("review references and rule identifiers must be bounded safe identifiers");
        }
        return value;
    }

    /** Closed production registry accepted in {@link Finding#ruleId()}. */
    public enum RuleId {
        ASSERTION_REQUIRED,
        REQUIREMENT_COVERAGE_REQUIRED,
        VERIFIED_PAGE_OBJECT_REQUIRED,
        PAGE_OBJECT_CONTRACT,
        TEST_DATA_CONTRACT,
        LIFECYCLE_INTEGRITY,
        EXECUTION_EVIDENCE_REQUIRED,
        UNSAFE_CODE,
        REVIEW_EVIDENCE_BINDING,
        ARTIFACT_REFERENCE_INVALID,
        EVIDENCE_UNAVAILABLE,
        EVIDENCE_DIGEST_MISMATCH,
        EVIDENCE_LINEAGE_INVALID,
        IMPLEMENTATION_FINGERPRINT_MISMATCH,
        FEEDBACK_UNSUBSTANTIATED,
        DECISION_CONTRADICTORY,
        REVIEW_CONTRACT_INVALID,
        LEGACY_REVIEW_FINDING
    }

    public static List<String> allowedRuleIds() {
        return java.util.Arrays.stream(RuleId.values()).map(Enum::name).toList();
    }

    /** A closed, machine-verifiable review observation; no model-authored prose is retained. */
    public record Finding(Category category, Severity severity, String ruleId, String artifactRef,
                          ProblemCategory problemCategory, RequiredAction requiredAction,
                          SafeLocation safeLocation, EvidenceAvailability evidenceAvailability) {
        public Finding {
            Objects.requireNonNull(category, "category");
            Objects.requireNonNull(severity, "severity");
            ruleId = RuleId.valueOf(safeId(ruleId)).name();
            artifactRef = safeId(artifactRef);
            Objects.requireNonNull(problemCategory, "problemCategory");
            Objects.requireNonNull(requiredAction, "requiredAction");
            Objects.requireNonNull(safeLocation, "safeLocation");
            Objects.requireNonNull(evidenceAvailability, "evidenceAvailability");
        }

        /** @deprecated source-compatible bridge; untrusted detail and recommendation are intentionally discarded. */
        @Deprecated
        public Finding(Severity severity, String code, String location, String detail, String recommendation) {
            this(Category.REVIEW_UNSUBSTANTIATED, severity, RuleId.LEGACY_REVIEW_FINDING.name(), "legacy-review",
                    ProblemCategory.REVIEW_CONTRACT, RequiredAction.HUMAN_REVIEW,
                    SafeLocation.GENERATED_TEST, EvidenceAvailability.UNAVAILABLE);
        }

        /** @deprecated use {@link #ruleId()}. */
        @Deprecated public String code() { return ruleId; }
        /** @deprecated use {@link #safeLocation()}. */
        @Deprecated public String location() { return safeLocation.name(); }
        /** @deprecated free-form review detail is no longer retained. */
        @Deprecated public String detail() { return problemCategory.name(); }
        /** @deprecated free-form recommendation is no longer retained. */
        @Deprecated public String recommendation() { return requiredAction.name(); }
    }

    public enum Category {
        ASSERTION_INSUFFICIENT, REQUIREMENT_NOT_COVERED, PAGE_OBJECT_MISUSE, TEST_DATA_INVALID,
        TEST_LIFECYCLE_PROBLEM, EXECUTION_EVIDENCE_INSUFFICIENT, UNSAFE_IMPLEMENTATION,
        REVIEW_UNSUBSTANTIATED
    }
    public enum ProblemCategory { ASSERTION, REQUIREMENT, PAGE_OBJECT, TEST_DATA, LIFECYCLE, EVIDENCE, SAFETY, REVIEW_CONTRACT }
    public enum RequiredAction { ADD_ASSERTION, COVER_REQUIREMENT, USE_VERIFIED_PAGE_OBJECT, FIX_TEST_DATA,
        FIX_TEST_LIFECYCLE, PROVIDE_EXECUTION_EVIDENCE, REMOVE_UNSAFE_IMPLEMENTATION, HUMAN_REVIEW }
    public enum SafeLocation { GENERATED_TEST, TEST_METHOD, ASSERTION, PAGE_OBJECT_CALL, TEST_DATA, LIFECYCLE, EXECUTION_EVIDENCE }
    public enum EvidenceAvailability { VERIFIED, PARTIAL, UNAVAILABLE }
    public enum Severity { ERROR, WARNING, INFO }
    public enum Verdict { APPROVE, REQUEST_CHANGES, BLOCK }
}
