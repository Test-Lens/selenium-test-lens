package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.CodeReviewResult;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.nio.charset.StandardCharsets;

/** Host-authored immutable catalog of the exact artifacts available to a reviewer. @since 0.5.0 */
public record ReviewerEvidencePack(String runId, int attempt, String implementationArtifactId,
                                   String implementationDigest, String sourceFingerprint,
                                   List<Evidence> evidence, List<String> requiredReviewedArtifactRefs,
                                   List<String> allowedRuleIds) {
    public ReviewerEvidencePack {
        if (blank(runId) || attempt < 1 || blank(implementationArtifactId) || blank(implementationDigest)
                || blank(sourceFingerprint)) throw new IllegalArgumentException("bounded reviewer evidence identity is required");
        evidence = (evidence == null ? List.<Evidence>of() : evidence).stream()
                .filter(Objects::nonNull).sorted(Comparator.comparing(Evidence::artifactId)).toList();
        if (evidence.isEmpty() || evidence.stream().map(Evidence::artifactId).distinct().count() != evidence.size()) {
            throw new IllegalArgumentException("reviewer evidence catalog must contain unique artifacts");
        }
        requiredReviewedArtifactRefs = canonical(requiredReviewedArtifactRefs);
        allowedRuleIds = canonical(allowedRuleIds);
        if (!CodeReviewResult.allowedRuleIds().containsAll(allowedRuleIds)
                || !evidence.stream().map(Evidence::artifactId).toList().containsAll(requiredReviewedArtifactRefs)) {
            throw new IllegalArgumentException("reviewer evidence constraints must use catalog values");
        }
    }

    public record Evidence(String artifactId, ArtifactType artifactType, String runId, int attempt,
                           String payloadDigest, List<String> parents, Availability availability,
                           boolean requiredForApproval, List<CodeReviewResult.Category> supportedFindingCategories,
                           List<String> allowedRuleIds, List<FindingContract> supportedFindingContracts) {
        public Evidence(String artifactId, ArtifactType artifactType, String runId, int attempt,
                        String payloadDigest, List<String> parents, Availability availability,
                        boolean requiredForApproval, List<CodeReviewResult.Category> supportedFindingCategories,
                        List<String> allowedRuleIds) {
            this(artifactId, artifactType, runId, attempt, payloadDigest, parents, availability,
                    requiredForApproval, supportedFindingCategories, allowedRuleIds,
                    findingContracts(supportedFindingCategories, allowedRuleIds));
        }

        public Evidence {
            if (blank(artifactId) || artifactType == null || blank(runId) || attempt < 1 || blank(payloadDigest)
                    || availability == null) throw new IllegalArgumentException("complete evidence provenance is required");
            parents = canonical(parents);
            supportedFindingCategories = List.copyOf(supportedFindingCategories == null
                    ? List.of() : supportedFindingCategories);
            allowedRuleIds = canonical(allowedRuleIds);
            supportedFindingContracts = List.copyOf(supportedFindingContracts == null
                    ? List.of() : supportedFindingContracts).stream().distinct().toList();
            if (!CodeReviewResult.allowedRuleIds().containsAll(allowedRuleIds)) {
                throw new IllegalArgumentException("evidence contains an unknown review rule");
            }
            for (FindingContract contract : supportedFindingContracts) {
                if (!supportedFindingCategories.contains(contract.category())
                        || !allowedRuleIds.contains(contract.ruleId())) {
                    throw new IllegalArgumentException("finding contracts must use the evidence catalog");
                }
            }
        }
    }

    /** Complete host-authored tuple the reviewer may select; fields are never inferred from model prose. */
    public record FindingContract(CodeReviewResult.Category category, String ruleId,
                                  CodeReviewResult.ProblemCategory problemCategory,
                                  CodeReviewResult.RequiredAction requiredAction,
                                  CodeReviewResult.SafeLocation safeLocation) {
        public FindingContract {
            Objects.requireNonNull(category, "category");
            ruleId = CodeReviewResult.RuleId.valueOf(ruleId).name();
            Objects.requireNonNull(problemCategory, "problemCategory");
            Objects.requireNonNull(requiredAction, "requiredAction");
            Objects.requireNonNull(safeLocation, "safeLocation");
        }

        public boolean matches(CodeReviewResult.Finding finding) {
            return finding.category() == category && finding.ruleId().equals(ruleId)
                    && finding.problemCategory() == problemCategory
                    && finding.requiredAction() == requiredAction
                    && finding.safeLocation() == safeLocation;
        }
    }

    public enum ArtifactType { CONTEXT, PLAN, IMPLEMENTATION, COMPILATION, QUALITY, EXECUTION }
    public enum Availability { AVAILABLE, UNAVAILABLE }
    public enum IntegrityStatus { VALID, EVIDENCE_UNAVAILABLE, EVIDENCE_DIGEST_MISMATCH,
        EVIDENCE_LINEAGE_INVALID, IMPLEMENTATION_FINGERPRINT_MISMATCH }

    /** Independently verifies this catalog against the trusted envelopes used by the workflow. */
    public IntegrityStatus verify(List<ArtifactEnvelope<?>> artifacts,String currentSourceFingerprint){
        if(currentSourceFingerprint==null||!sourceFingerprint.equals(currentSourceFingerprint)){
            return IntegrityStatus.IMPLEMENTATION_FINGERPRINT_MISMATCH;
        }
        List<ArtifactEnvelope<?>> values=List.copyOf(artifacts==null?List.of():artifacts);
        if(evidence.size()!=values.size())return IntegrityStatus.EVIDENCE_UNAVAILABLE;
        Map<String,ArtifactEnvelope<?>> actual;
        try{
            actual=values.stream().collect(java.util.stream.Collectors.toMap(
                    ArtifactEnvelope::artifactId,value->value));
        }catch(IllegalStateException duplicate){return IntegrityStatus.EVIDENCE_LINEAGE_INVALID;}
        ArtifactEnvelope<?> implementation=actual.get(implementationArtifactId);
        if(implementation==null||!(implementation.payload() instanceof io.github.testlens.application.tooling.ai.TestImplementationProposal proposal)
                ||!implementation.runId().equals(runId)||implementation.attempt()!=attempt){
            return IntegrityStatus.EVIDENCE_LINEAGE_INVALID;
        }
        String actualImplementationDigest=ArtifactEnvelope.digest(
                proposal.sourcePatch().getBytes(StandardCharsets.UTF_8));
        if(!implementationDigest.equals(actualImplementationDigest))return IntegrityStatus.EVIDENCE_DIGEST_MISMATCH;
        for(Evidence item:evidence){
            ArtifactEnvelope<?> artifact=actual.get(item.artifactId());
            if(artifact==null||item.availability()!=Availability.AVAILABLE){
                return IntegrityStatus.EVIDENCE_UNAVAILABLE;
            }
            List<String> actualParents=artifact.parents().stream().distinct().sorted().toList();
            if(!artifact.runId().equals(item.runId())||artifact.attempt()!=item.attempt()
                    ||!actualParents.equals(item.parents())
                    ||item.requiredForApproval()!=requiredReviewedArtifactRefs.contains(item.artifactId())){
                return IntegrityStatus.EVIDENCE_LINEAGE_INVALID;
            }
            if(!artifact.payloadDigest().equals(item.payloadDigest())){
                return IntegrityStatus.EVIDENCE_DIGEST_MISMATCH;
            }
        }
        return IntegrityStatus.VALID;
    }

    private static List<String> canonical(List<String> values) {
        return (values == null ? List.<String>of() : values).stream().filter(Objects::nonNull)
                .distinct().sorted().toList();
    }
    private static List<FindingContract> findingContracts(List<CodeReviewResult.Category> categories,
                                                          List<String> rules) {
        List<CodeReviewResult.Category> categoryValues = categories == null ? List.of() : categories;
        List<String> ruleValues = rules == null ? List.of() : rules;
        java.util.ArrayList<FindingContract> result = new java.util.ArrayList<>();
        if (categoryValues.contains(CodeReviewResult.Category.ASSERTION_INSUFFICIENT)
                && ruleValues.contains(CodeReviewResult.RuleId.ASSERTION_REQUIRED.name())) {
            result.add(new FindingContract(CodeReviewResult.Category.ASSERTION_INSUFFICIENT,
                    CodeReviewResult.RuleId.ASSERTION_REQUIRED.name(), CodeReviewResult.ProblemCategory.ASSERTION,
                    CodeReviewResult.RequiredAction.ADD_ASSERTION, CodeReviewResult.SafeLocation.ASSERTION));
        }
        if (categoryValues.contains(CodeReviewResult.Category.REQUIREMENT_NOT_COVERED)
                && ruleValues.contains(CodeReviewResult.RuleId.REQUIREMENT_COVERAGE_REQUIRED.name())) {
            result.add(new FindingContract(CodeReviewResult.Category.REQUIREMENT_NOT_COVERED,
                    CodeReviewResult.RuleId.REQUIREMENT_COVERAGE_REQUIRED.name(),
                    CodeReviewResult.ProblemCategory.REQUIREMENT,
                    CodeReviewResult.RequiredAction.COVER_REQUIREMENT,
                    CodeReviewResult.SafeLocation.GENERATED_TEST));
        }
        if (categoryValues.contains(CodeReviewResult.Category.TEST_LIFECYCLE_PROBLEM)
                && ruleValues.contains(CodeReviewResult.RuleId.LIFECYCLE_INTEGRITY.name())) {
            result.add(new FindingContract(CodeReviewResult.Category.TEST_LIFECYCLE_PROBLEM,
                    CodeReviewResult.RuleId.LIFECYCLE_INTEGRITY.name(), CodeReviewResult.ProblemCategory.LIFECYCLE,
                    CodeReviewResult.RequiredAction.FIX_TEST_LIFECYCLE,
                    CodeReviewResult.SafeLocation.LIFECYCLE));
        }
        return List.copyOf(result);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
