package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Side-effect-free draft preparation and policy simulation. */
public final class PolicyDraftService {
    public Preparation prepareExact(PolicyWorkspaceSnapshot workspace, CandidateAnalysis.Candidate candidate,
                                    SelectorSubject subject, SelectorPolicy.Decision decision,
                                    PolicyDraft.ScopeChoice scopeChoice, PolicyWorkspaceSnapshot.Origin destination,
                                    ObservationEvidence evidence) {
        if (candidate == null || candidate.locator() == null || candidate.opaqueOriginal() || !subject.hasTrustedExactValue())
            return Preparation.rejected("EXACT_POLICY_REQUIRES_TRUSTED_SCALAR");
        if (decision == SelectorPolicy.Decision.STABLE && !stableEligible(candidate.validation().state()))
            return Preparation.rejected("STABLE_POLICY_REJECTED_FOR_" + candidate.validation().state());
        SelectorPolicy.Scope scope = scope(subject, scopeChoice);
        if (scope == null) return Preparation.rejected("REQUESTED_SCOPE_UNAVAILABLE");
        SelectorPolicy.Rule rule = SelectorPolicy.Rule.create(decision, 0, scope,
                SelectorPolicy.ExactMatcher.from(subject), new SelectorPolicy.Reason(reason(false, decision), null));
        boolean ambiguous = candidate.validation().state() == CandidateAnalysis.ValidationState.VALID_BUT_AMBIGUOUS;
        return build(workspace, PolicyDraft.Action.ADD, destination, rule, null,
                new PolicyDraft.Source(null, candidate.candidateId(), subject.declarationRef(), null),
                preview(scopeChoice, 1, 0, 1, 0, true), evidence, subject, ambiguous,
                PolicyDraft.SecurityClassification.DIGEST_ONLY);
    }

    public Preparation preparePattern(PolicyWorkspaceSnapshot workspace, PatternProposal.Result proposal,
                                      SelectorSubject subject, SelectorPolicy.Decision decision,
                                      PolicyDraft.ScopeChoice scopeChoice, PolicyWorkspaceSnapshot.Origin destination,
                                      FederatedPatternPreview.Result impact, ObservationEvidence evidence,
                                      String candidateId, String proposalId) {
        if (proposal == null || !proposal.useful() || proposal.pattern() == null) return Preparation.rejected("SAFE_PATTERN_PROPOSAL_REQUIRED");
        if (impact == null) return Preparation.rejected("FULL_SCOPE_PREVIEW_REQUIRED");
        SelectorPolicy.Scope scope = scope(subject, scopeChoice);
        if (scope == null) return Preparation.rejected("REQUESTED_SCOPE_UNAVAILABLE");
        SelectorPolicy.Rule rule = SelectorPolicy.Rule.create(decision, 0, scope, proposal.pattern(),
                new SelectorPolicy.Reason(reason(true, decision), null));
        PolicyDraft.Preview preview = preview(scopeChoice, impact.matchedStaticSubjects().size(), impact.matchedHistorySubjects().size(),
                impact.affectedDeclarationRefs().size(), impact.usageCount(), !impact.incompleteHistory() && impact.unsupportedDigestOnlySubjects().isEmpty());
        PolicyDraft.SecurityClassification classification = proposal.pattern().segments().stream().anyMatch(s -> s.kind() == SelectorPolicy.SegmentKind.LITERAL)
                ? PolicyDraft.SecurityClassification.LOCAL_RECOMMENDED : PolicyDraft.SecurityClassification.REVIEW_LITERAL_CONTENT;
        return build(workspace, PolicyDraft.Action.ADD, destination, rule, null,
                new PolicyDraft.Source(null, candidateId, subject.declarationRef(), proposalId), preview, evidence, subject, false, classification);
    }

    public Preparation prepareRemove(PolicyWorkspaceSnapshot workspace, String knownRuleId) {
        PolicyWorkspaceSnapshot.OriginRule existing = workspace.find(knownRuleId);
        if (existing == null) return Preparation.rejected("UNKNOWN_RULE_ID");
        PolicyDraft.Preview preview = preview(PolicyDraft.ScopeChoice.PROJECT, 0,0,0,0,true);
        return build(workspace, PolicyDraft.Action.REMOVE, existing.origin(), null, knownRuleId,
                new PolicyDraft.Source(null,null,existing.rule().scope().declarationRef(),null), preview,
                ObservationEvidence.unavailable(), null, false, PolicyDraft.SecurityClassification.DIGEST_ONLY);
    }

    public Preparation prepareReplace(PolicyWorkspaceSnapshot workspace, String knownRuleId,
                                      SelectorPolicy.Decision decision, SelectorPolicy.Scope replacementScope) {
        PolicyWorkspaceSnapshot.OriginRule existing = workspace.find(knownRuleId);
        if (existing == null) return Preparation.rejected("UNKNOWN_RULE_ID");
        SelectorPolicy.Rule rule = SelectorPolicy.Rule.create(decision, existing.rule().priority(), replacementScope,
                existing.rule().matcher(), new SelectorPolicy.Reason(reason(existing.rule().matcher() instanceof SelectorPolicy.StructuralPattern, decision), null));
        PolicyDraft.Preview preview = preview(choice(replacementScope),0,0,0,0,true);
        return build(workspace, PolicyDraft.Action.REPLACE, existing.origin(), rule, knownRuleId,
                new PolicyDraft.Source(null,null,replacementScope.declarationRef(),null), preview,
                ObservationEvidence.unavailable(), null, false,
                rule.matcher() instanceof SelectorPolicy.ExactMatcher ? PolicyDraft.SecurityClassification.DIGEST_ONLY : PolicyDraft.SecurityClassification.LOCAL_RECOMMENDED);
    }

    private Preparation build(PolicyWorkspaceSnapshot workspace, PolicyDraft.Action action,
                              PolicyWorkspaceSnapshot.Origin destination, SelectorPolicy.Rule proposed, String oldRuleId,
                              PolicyDraft.Source source, PolicyDraft.Preview basePreview, ObservationEvidence evidence,
                              SelectorSubject subject, boolean ambiguous, PolicyDraft.SecurityClassification classification) {
        List<PolicyDraft.Warning> warnings = new ArrayList<>();
        List<PolicyDraft.BlockingIssue> blocking = new ArrayList<>();
        EnumSet<PolicyDraft.HostAcknowledgement> acknowledgements = EnumSet.noneOf(PolicyDraft.HostAcknowledgement.class);
        List<PolicyDraft.SimulationDisposition> dispositions = new ArrayList<>(basePreview.dispositions());
        if (proposed != null) {
            PolicyWorkspaceSnapshot.OriginRule duplicate = workspace.find(proposed.ruleId());
            if (duplicate != null && !proposed.ruleId().equals(oldRuleId)) { blocking.add(PolicyDraft.BlockingIssue.ALREADY_EXISTS); dispositions.add(PolicyDraft.SimulationDisposition.ALREADY_EXISTS); }
        }
        if (!basePreview.complete()) { warnings.add(PolicyDraft.Warning.PREVIEW_INCOMPLETE); acknowledgements.add(PolicyDraft.HostAcknowledgement.PREVIEW_INCOMPLETE); dispositions.add(PolicyDraft.SimulationDisposition.PREVIEW_INCOMPLETE); }
        if (ambiguous) { warnings.add(PolicyDraft.Warning.AMBIGUOUS_LIVE_VALIDATION); acknowledgements.add(PolicyDraft.HostAcknowledgement.AMBIGUOUS_LIVE_VALIDATION); }
        if (classification == PolicyDraft.SecurityClassification.DIGEST_ONLY) warnings.add(PolicyDraft.Warning.DIGEST_DICTIONARY_RISK);
        else warnings.add(PolicyDraft.Warning.PATTERN_LITERAL_CONTENT);
        List<SelectorPolicy.Rule> simulated = new ArrayList<>(workspace.compiled().rules());
        if (oldRuleId != null) simulated.removeIf(rule -> rule.ruleId().equals(oldRuleId));
        if (proposed != null && simulated.stream().noneMatch(rule -> rule.ruleId().equals(proposed.ruleId()))) simulated.add(proposed);
        CompiledPolicySet policies = CompiledPolicySet.compile(new SelectorPolicy.Document(SelectorPolicy.SCHEMA_VERSION, CanonicalDigests.CANONICALIZATION_VERSION, simulated));
        if (subject != null && proposed != null) {
            StabilityAssessment assessment = new SelectorStabilityEngine().analyze(subject, evidence, policies);
            if (assessment.effectiveDisposition() == StabilityAssessment.EffectiveDisposition.POLICY_POLICY_CONFLICT) {
                blocking.add(PolicyDraft.BlockingIssue.POLICY_POLICY_CONFLICT); dispositions.add(PolicyDraft.SimulationDisposition.POLICY_POLICY_CONFLICT);
            } else if (assessment.effectiveDisposition() == StabilityAssessment.EffectiveDisposition.POLICY_EVIDENCE_CONFLICT) {
                warnings.add(PolicyDraft.Warning.POLICY_EVIDENCE_CONFLICT); acknowledgements.add(PolicyDraft.HostAcknowledgement.POLICY_EVIDENCE_CONFLICT); dispositions.add(PolicyDraft.SimulationDisposition.POLICY_EVIDENCE_CONFLICT);
            } else if (proposed.ruleId().equals(assessment.policyEvaluation().selectedRuleId())) dispositions.add(PolicyDraft.SimulationDisposition.WOULD_BE_SELECTED);
            else dispositions.add(PolicyDraft.SimulationDisposition.SHADOWED_RULE);
        }
        PolicyDraft.Preview preview = new PolicyDraft.Preview(basePreview.scopeChoice(), dispositions,
                basePreview.selectedRuleIds(), basePreview.shadowedRuleIds(), basePreview.staticMatches(), basePreview.historyMatches(),
                basePreview.affectedDeclarations(), basePreview.usageCount(), basePreview.complete(), basePreview.reasonCodes());
        Set<String> present = oldRuleId == null ? Set.of() : Set.of(oldRuleId);
        Set<String> absent = proposed == null || proposed.ruleId().equals(oldRuleId) ? Set.of() : Set.of(proposed.ruleId());
        PolicyDraft draft = PolicyDraft.create(action, destination, proposed, oldRuleId, source, preview, warnings, blocking,
                acknowledgements, classification, PolicyDraft.WorkspacePreconditions.from(workspace, present, absent));
        return new Preparation(draft, List.of());
    }

    private static boolean stableEligible(CandidateAnalysis.ValidationState state) {
        return state == CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE || state == CandidateAnalysis.ValidationState.VALID_FOR_INTENT || state == CandidateAnalysis.ValidationState.VALID_BUT_AMBIGUOUS;
    }
    private static String reason(boolean pattern, SelectorPolicy.Decision decision) { return "USER_MARKED_" + (pattern ? "PATTERN_" : "EXACT_") + decision.name(); }
    private static PolicyDraft.Preview preview(PolicyDraft.ScopeChoice scope, int statics, int history, int declarations, int usages, boolean complete) { return new PolicyDraft.Preview(scope,List.of(),List.of(),List.of(),statics,history,declarations,usages,complete,List.of()); }
    private static PolicyDraft.ScopeChoice choice(SelectorPolicy.Scope scope) { if(scope.declarationRef()!=null)return PolicyDraft.ScopeChoice.DECLARATION;if(scope.logicalPath()!=null)return PolicyDraft.ScopeChoice.FILE;if(scope.modulePath()!=null)return PolicyDraft.ScopeChoice.MODULE;if(scope.usageClass()!=null||scope.usageMethod()!=null||scope.contextFingerprint()!=null)return PolicyDraft.ScopeChoice.RUNTIME_NARROW;return PolicyDraft.ScopeChoice.PROJECT; }
    private static SelectorPolicy.Scope scope(SelectorSubject s, PolicyDraft.ScopeChoice choice) { return switch(choice) {
        case DECLARATION -> s.declarationRef()==null?null:new SelectorPolicy.Scope(null,null,s.declarationRef(),null,null,null,null);
        case FILE -> s.logicalPath()==null?null:new SelectorPolicy.Scope(null,s.logicalPath(),null,null,null,null,null);
        case MODULE -> s.modulePath()==null?null:new SelectorPolicy.Scope(s.modulePath(),null,null,null,null,null,null);
        case PROJECT -> SelectorPolicy.Scope.project();
        case RUNTIME_NARROW -> (s.usageClass()==null&&s.usageMethod()==null&&s.contextFingerprint()==null)?null:new SelectorPolicy.Scope(null,null,null,null,s.usageClass(),s.usageMethod(),s.contextFingerprint());
    }; }

    public record Preparation(PolicyDraft draft, List<String> issues) {
        public Preparation { issues=List.copyOf(issues); }
        public static Preparation rejected(String issue) { return new Preparation(null,List.of(issue)); }
        public boolean prepared() { return draft != null; }
    }
}
