package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Reviewable repair evidence. Tooling never silently applies a proposal. @since 0.5.0 */
public record RepairProposal(
        ContractHeader header,
        String proposalId,
        ApplicationPolicy applicationPolicy,
        String whatChanged,
        String why,
        String oldSelector,
        String newSelector,
        String sourceDeclarationRef,
        String oldCandidateId,
        String newCandidateId,
        String classificationRef,
        String driftRef,
        List<String> sameTargetEvidence,
        List<String> stabilityEvidence,
        List<String> affectedTests,
        List<String> risks,
        List<String> verificationPlan,
        SourceTarget sourceTarget,
        SelectorEvidence replacementEvidence,
        List<String> affectedMethods) {

    /** Compatibility constructor for proposals created before source-apply evidence was modeled. */
    public RepairProposal(ContractHeader header, String proposalId, ApplicationPolicy applicationPolicy,
                          String whatChanged, String why, String oldSelector, String newSelector,
                          String sourceDeclarationRef, String oldCandidateId, String newCandidateId,
                          String classificationRef, String driftRef, List<String> sameTargetEvidence,
                          List<String> stabilityEvidence, List<String> affectedTests, List<String> risks,
                          List<String> verificationPlan) {
        this(header, proposalId, applicationPolicy, whatChanged, why, oldSelector, newSelector,
                sourceDeclarationRef, oldCandidateId, newCandidateId, classificationRef, driftRef,
                sameTargetEvidence, stabilityEvidence, affectedTests, risks, verificationPlan,
                null, null, List.of());
    }

    public RepairProposal(ContractHeader header, String proposalId, ApplicationPolicy applicationPolicy,
                          String whatChanged, String why, String oldSelector, String newSelector,
                          List<String> sameTargetEvidence, List<String> stabilityEvidence,
                          List<String> affectedTests) {
        this(header, proposalId, applicationPolicy, whatChanged, why, oldSelector, newSelector,
                null, null, null, null, null, sameTargetEvidence, stabilityEvidence, affectedTests,
                List.of(), List.of(), null, null, List.of());
    }

    public RepairProposal {
        if (header == null || applicationPolicy != ApplicationPolicy.PROPOSE_ONLY) {
            throw new IllegalArgumentException("Repairs must use PROPOSE_ONLY");
        }
        sameTargetEvidence = canonical(sameTargetEvidence);
        stabilityEvidence = canonical(stabilityEvidence);
        affectedTests = canonical(affectedTests);
        affectedMethods = canonical(affectedMethods);
        risks = canonical(risks);
        verificationPlan = canonical(verificationPlan);
    }

    /** Exact, source-safe preconditions required by the trusted host apply boundary. */
    public record SourceTarget(String sourceElementId, String declarationRef, String logicalPath,
                               SourceRange range, String sourceFileFingerprint,
                               String declarationFingerprint, String correlationState,
                               String oldStrategy, String oldValue) {
        public SourceTarget {
            if (blank(sourceElementId) || blank(declarationRef) || blank(logicalPath) || range == null
                    || blank(sourceFileFingerprint) || blank(declarationFingerprint)
                    || blank(correlationState) || blank(oldStrategy) || oldValue == null) {
                throw new IllegalArgumentException("Complete source repair preconditions are required");
            }
        }
    }

    /** Character offsets are based on decoded Java source, never byte indexes. */
    public record SourceRange(int startLine, int startColumn, int endLine, int endColumn,
                              int startOffset, int endOffsetExclusive) {
        public SourceRange {
            if (startLine < 1 || startColumn < 1 || endLine < startLine || endColumn < 1
                    || startOffset < 0 || endOffsetExclusive <= startOffset) {
                throw new IllegalArgumentException("Invalid source range");
            }
        }
    }

    /** Structured projection of the engine-selected live replacement candidate. */
    public record SelectorEvidence(String strategy, String value, String candidateId,
                                   String validation, String sameTarget, boolean unique,
                                   List<String> stability, String source) {
        public SelectorEvidence {
            if (blank(strategy) || value == null || blank(candidateId) || blank(validation)
                    || blank(sameTarget) || blank(source)) {
                throw new IllegalArgumentException("Complete selector evidence is required");
            }
            stability = canonical(stability);
        }
    }

    public enum ApplicationPolicy { PROPOSE_ONLY }

    private static List<String> canonical(List<String> values) {
        return (values == null ? List.<String>of() : values).stream()
                .filter(java.util.Objects::nonNull).distinct().sorted().toList();
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
