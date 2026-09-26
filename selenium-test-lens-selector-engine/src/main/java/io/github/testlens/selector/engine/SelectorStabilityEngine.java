package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Shared offline classifier/policy evaluator. No Selenium or filesystem work occurs here. */
public final class SelectorStabilityEngine {
    public static final String ENGINE_NAME="Test Lens selector stability engine";
    public static final String ENGINE_VERSION="0.4.0";
    public static final int ANALYSIS_SCHEMA_VERSION=1;
    public static final int MAX_PREVIEW_SUBJECTS=100_000;
    private final AppearanceClassifier classifier;
    public SelectorStabilityEngine(){this(new AppearanceClassifier());}
    SelectorStabilityEngine(AppearanceClassifier classifier){this.classifier=Objects.requireNonNull(classifier);}

    public StabilityAssessment analyze(SelectorSubject subject, ObservationEvidence evidence, CompiledPolicySet policies){
        Objects.requireNonNull(subject);evidence=evidence==null?ObservationEvidence.unavailable():evidence;policies=policies==null?CompiledPolicySet.empty():policies;
        List<AppearanceSignal> signals=classifier.classify(subject);
        return assess(subject,signals,subject.hasTrustedExactValue()?CanonicalDigests.exactValueDigest(subject):null,null,evidence,policies);
    }

    /** Evaluates privacy-preserving, precomputed trusted history metadata without reconstructing a raw value. */
    public StabilityAssessment analyzeIndexed(SelectorSubject subject,List<AppearanceSignal> signals,String exactDigest,
                                              StructuralFamily structuralFamily,ObservationEvidence evidence,CompiledPolicySet policies){
        Objects.requireNonNull(subject);signals=signals==null?List.of():List.copyOf(signals);evidence=evidence==null?ObservationEvidence.unavailable():evidence;policies=policies==null?CompiledPolicySet.empty():policies;
        if(exactDigest!=null&&!exactDigest.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("exactDigest must be 64 lowercase hex characters");
        return assess(subject,signals,exactDigest,structuralFamily,evidence,policies);
    }

    private StabilityAssessment assess(SelectorSubject subject,List<AppearanceSignal> signals,String exactDigest,
                                       StructuralFamily structuralFamily,ObservationEvidence evidence,CompiledPolicySet policies){
        List<SelectorPolicy.Rule> matches=policies.candidates(subject,exactDigest).stream().filter(r->r.scope().matches(subject)&&matches(r.matcher(),subject,exactDigest,structuralFamily)).toList();
        StabilityAssessment.PolicyEvaluation policy=evaluatePolicies(matches);
        StabilityAssessment.EffectiveDisposition disposition=disposition(subject,signals,evidence,policy);
        List<StabilityAssessment.Explanation> explanations=new ArrayList<>();
        explanations.add(new StabilityAssessment.Explanation(disposition.name(),signals.stream().map(AppearanceSignal::detector).distinct().toList(),policy.matchedRuleIds(),
                evidence.comparisons().stream().map(ObservationEvidence.Comparison::evidenceRef).toList(),message(disposition)));
        return new StabilityAssessment(ANALYSIS_SCHEMA_VERSION,new StabilityAssessment.EngineMetadata(ENGINE_NAME,ENGINE_VERSION,AppearanceClassifier.DETECTOR_CATALOG_VERSION,CanonicalDigests.CANONICALIZATION_VERSION),
                subject,signals,evidence,policy,StabilityAssessment.Validation.NOT_EVALUATED,disposition,List.of(),explanations);
    }
    private static boolean matches(SelectorPolicy.Matcher matcher,SelectorSubject subject,String exactDigest,StructuralFamily family){
        if(matcher instanceof SelectorPolicy.ExactMatcher exact)return exactDigest!=null&&exact.strategy().equals(subject.strategy())&&exact.valueDigest().equals(exactDigest);
        if(subject.hasTrustedExactValue())return matcher.matches(subject);
        return family!=null&&family.matchDigestOnly((SelectorPolicy.StructuralPattern)matcher)==StructuralFamily.Match.MATCH;
    }

    public Preview preview(List<SelectorSubject> subjects, ObservationEvidence evidence, CompiledPolicySet policies){
        if(subjects.size()>MAX_PREVIEW_SUBJECTS)throw new IllegalArgumentException("Preview subject limit exceeded");
        ObservationEvidence suppliedEvidence=evidence==null?ObservationEvidence.unavailable():evidence;
        List<StabilityAssessment> assessments=subjects.stream().map(s->analyze(s,suppliedEvidence,policies)).toList();
        return new Preview(assessments,assessments.stream().filter(a->!a.policyEvaluation().matchedRuleIds().isEmpty()).map(StabilityAssessment::subject).toList(),
                assessments.stream().filter(a->a.effectiveDisposition()==StabilityAssessment.EffectiveDisposition.POLICY_POLICY_CONFLICT||a.effectiveDisposition()==StabilityAssessment.EffectiveDisposition.POLICY_EVIDENCE_CONFLICT).toList(),
                assessments.stream().filter(a->a.subject().valueState()==SelectorSubject.ValueState.UNAVAILABLE||a.subject().inputTrust()==SelectorSubject.InputTrust.RUNTIME_REDACTED).map(StabilityAssessment::subject).toList(),
                assessments.stream().map(a->a.subject().declarationRef()).filter(Objects::nonNull).distinct().toList(),
                suppliedEvidence.comparisons().stream().map(ObservationEvidence.Comparison::domain).distinct().toList());
    }

    private static StabilityAssessment.PolicyEvaluation evaluatePolicies(List<SelectorPolicy.Rule> matches){
        if(matches.isEmpty())return StabilityAssessment.PolicyEvaluation.none();
        Comparator<SelectorPolicy.Rule> comparator=Comparator.comparingInt((SelectorPolicy.Rule r)->r.scope().specificity())
                .thenComparingInt(SelectorPolicy.Rule::priority).thenComparingInt(r->r.matcher().matcherSpecificity())
                .thenComparingInt(r->r.matcher().literalCoverage()).thenComparingInt(r->-r.matcher().placeholderCount());
        SelectorPolicy.Rule best=matches.stream().max(comparator).orElseThrow();
        List<SelectorPolicy.Rule> top=matches.stream().filter(r->comparator.compare(r,best)==0).toList();
        boolean conflict=top.stream().map(SelectorPolicy.Rule::decision).distinct().count()>1;
        return new StabilityAssessment.PolicyEvaluation(matches.stream().map(SelectorPolicy.Rule::ruleId).sorted().toList(),conflict?null:best.ruleId(),conflict?null:best.decision(),conflict,
                conflict?top.stream().map(SelectorPolicy.Rule::ruleId).sorted().toList():List.of());
    }

    private static StabilityAssessment.EffectiveDisposition disposition(SelectorSubject s,List<AppearanceSignal> signals,ObservationEvidence e,StabilityAssessment.PolicyEvaluation p){
        if(p.conflict())return StabilityAssessment.EffectiveDisposition.POLICY_POLICY_CONFLICT;
        if(p.selectedDecision()==SelectorPolicy.Decision.STABLE&&e.state()==ObservationEvidence.State.CHANGED)return StabilityAssessment.EffectiveDisposition.POLICY_EVIDENCE_CONFLICT;
        if(e.state()==ObservationEvidence.State.CHANGED)return StabilityAssessment.EffectiveDisposition.OBSERVED_VARIABLE;
        if(p.selectedDecision()==SelectorPolicy.Decision.STABLE)return StabilityAssessment.EffectiveDisposition.DECLARED_STABLE;
        if(p.selectedDecision()==SelectorPolicy.Decision.UNSTABLE)return StabilityAssessment.EffectiveDisposition.DECLARED_UNSTABLE;
        if(!signals.isEmpty())return StabilityAssessment.EffectiveDisposition.REVIEW_GENERATED_LOOKING;
        if(s.valueState()==SelectorSubject.ValueState.UNAVAILABLE||s.inputTrust()==SelectorSubject.InputTrust.RUNTIME_REDACTED)return StabilityAssessment.EffectiveDisposition.INSUFFICIENT_DATA;
        return StabilityAssessment.EffectiveDisposition.NO_APPEARANCE_SIGNAL;
    }
    private static String message(StabilityAssessment.EffectiveDisposition d){return switch(d){
        case INSUFFICIENT_DATA->"Insufficient canonical data for stability assessment";
        case NO_APPEARANCE_SIGNAL->"No generated-looking appearance signal was detected; stability is not verified";
        case REVIEW_GENERATED_LOOKING->"Generated-looking appearance signals require review";
        case DECLARED_STABLE->"Project policy declares this subject stable; validation remains not evaluated";
        case DECLARED_UNSTABLE->"Project policy declares this subject unstable";
        case OBSERVED_VARIABLE->"Comparable supplied evidence reports a changed value";
        case POLICY_POLICY_CONFLICT->"Equal-precedence project policies have opposite decisions";
        case POLICY_EVIDENCE_CONFLICT->"A stable project policy conflicts with comparable changed evidence";};}
    public record Preview(List<StabilityAssessment> assessments,List<SelectorSubject> matchedSubjects,List<StabilityAssessment> conflicts,List<SelectorSubject> incompleteSubjects,List<String> affectedDeclarationRefs,List<ObservationEvidence.Domain> evidenceDomains){
        public Preview{assessments=List.copyOf(assessments);matchedSubjects=List.copyOf(matchedSubjects);conflicts=List.copyOf(conflicts);incompleteSubjects=List.copyOf(incompleteSubjects);affectedDeclarationRefs=List.copyOf(affectedDeclarationRefs);evidenceDomains=List.copyOf(evidenceDomains);}
    }
}
