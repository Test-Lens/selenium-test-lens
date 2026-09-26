package io.github.testlens.selector.engine;

import java.util.List;

public record StabilityAssessment(int schemaVersion, EngineMetadata engine, SelectorSubject subject,
                                  List<AppearanceSignal> appearanceSignals, ObservationEvidence evidenceSummary,
                                  PolicyEvaluation policyEvaluation, Validation validation,
                                  EffectiveDisposition effectiveDisposition, List<Issue> issues,
                                  List<Explanation> explanations) {
    public StabilityAssessment {
        appearanceSignals=List.copyOf(appearanceSignals); issues=List.copyOf(issues); explanations=List.copyOf(explanations);
    }
    public record EngineMetadata(String name,String version,int detectorCatalogVersion,int canonicalizationVersion) { }
    public record PolicyEvaluation(List<String> matchedRuleIds,String selectedRuleId,SelectorPolicy.Decision selectedDecision,
                                   boolean conflict,List<String> conflictingRuleIds) {
        public PolicyEvaluation { matchedRuleIds=List.copyOf(matchedRuleIds); conflictingRuleIds=List.copyOf(conflictingRuleIds); }
        public static PolicyEvaluation none(){return new PolicyEvaluation(List.of(),null,null,false,List.of());}
    }
    public enum Validation { NOT_EVALUATED }
    public enum EffectiveDisposition { INSUFFICIENT_DATA, NO_APPEARANCE_SIGNAL, REVIEW_GENERATED_LOOKING,
        DECLARED_STABLE, DECLARED_UNSTABLE, OBSERVED_VARIABLE, POLICY_POLICY_CONFLICT, POLICY_EVIDENCE_CONFLICT }
    public record Issue(String code,String detail) { }
    public record Explanation(String reasonCode,List<String> detectorIds,List<String> ruleIds,List<String> evidenceRefs,String message) {
        public Explanation { detectorIds=List.copyOf(detectorIds);ruleIds=List.copyOf(ruleIds);evidenceRefs=List.copyOf(evidenceRefs); }
    }
}
