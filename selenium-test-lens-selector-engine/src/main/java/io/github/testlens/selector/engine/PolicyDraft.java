package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Neutral immutable policy change. It carries no filesystem or browser capability. */
public record PolicyDraft(int schemaVersion, String draftId, Action action, PolicyWorkspaceSnapshot.Origin destination,
                          SelectorPolicy.Rule proposedRule, String oldRuleId, Source source, Preview preview,
                          List<Warning> warnings, List<BlockingIssue> blockingIssues,
                          Set<HostAcknowledgement> requiredHostAcknowledgements,
                          SecurityClassification securityClassification, WorkspacePreconditions workspacePreconditions) {
    public static final int SCHEMA_VERSION = 1;

    public PolicyDraft {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported draft schemaVersion");
        Objects.requireNonNull(action); Objects.requireNonNull(destination); Objects.requireNonNull(source);
        Objects.requireNonNull(preview); Objects.requireNonNull(securityClassification); Objects.requireNonNull(workspacePreconditions);
        warnings = List.copyOf(warnings); blockingIssues = List.copyOf(blockingIssues);
        requiredHostAcknowledgements = Set.copyOf(requiredHostAcknowledgements);
        if (action == Action.ADD && (proposedRule == null || oldRuleId != null)) throw new IllegalArgumentException("ADD requires only proposedRule");
        if (action == Action.REMOVE && (proposedRule != null || oldRuleId == null)) throw new IllegalArgumentException("REMOVE requires only oldRuleId");
        if (action == Action.REPLACE && (proposedRule == null || oldRuleId == null)) throw new IllegalArgumentException("REPLACE requires old and proposed rules");
        String expected = calculateId(action, destination, oldRuleId, proposedRule, workspacePreconditions, preview);
        if (!expected.equals(draftId)) throw new IllegalArgumentException("draftId does not match semantic content");
    }

    public static PolicyDraft create(Action action, PolicyWorkspaceSnapshot.Origin destination,
                                     SelectorPolicy.Rule proposedRule, String oldRuleId, Source source, Preview preview,
                                     List<Warning> warnings, List<BlockingIssue> blockingIssues,
                                     Set<HostAcknowledgement> acknowledgements, SecurityClassification classification,
                                     WorkspacePreconditions preconditions) {
        return new PolicyDraft(SCHEMA_VERSION, calculateId(action, destination, oldRuleId, proposedRule, preconditions, preview),
                action, destination, proposedRule, oldRuleId, source, preview, warnings, blockingIssues,
                acknowledgements, classification, preconditions);
    }

    public boolean transferable() { return blockingIssues.isEmpty(); }

    public enum Action { ADD, REMOVE, REPLACE }
    public enum ScopeChoice { DECLARATION, FILE, MODULE, PROJECT, RUNTIME_NARROW }
    public enum Warning { PREVIEW_INCOMPLETE, POLICY_EVIDENCE_CONFLICT, AMBIGUOUS_LIVE_VALIDATION, DIGEST_DICTIONARY_RISK, PATTERN_LITERAL_CONTENT }
    public enum BlockingIssue { POLICY_POLICY_CONFLICT, ALREADY_EXISTS, UNSAFE_SCOPE, PREVIEW_REQUIRED, INVALID_CANDIDATE, UNSUPPORTED_MATCHER, MALFORMED_WORKSPACE, CROSS_ORIGIN_REPLACE }
    public enum HostAcknowledgement { POLICY_EVIDENCE_CONFLICT, PREVIEW_INCOMPLETE, AMBIGUOUS_LIVE_VALIDATION }
    public enum SecurityClassification { DIGEST_ONLY, REVIEW_LITERAL_CONTENT, LOCAL_RECOMMENDED }
    public enum SimulationDisposition { WOULD_BE_SELECTED, ALREADY_EXISTS, SHADOWED_RULE, WOULD_SHADOW_RULE, POLICY_POLICY_CONFLICT, POLICY_EVIDENCE_CONFLICT, PREVIEW_INCOMPLETE }

    public record Source(String analysisId, String candidateId, String declarationRef, String patternProposalId) { }
    public record Preview(ScopeChoice scopeChoice, List<SimulationDisposition> dispositions,
                          List<String> selectedRuleIds, List<String> shadowedRuleIds,
                          int staticMatches, int historyMatches, int affectedDeclarations,
                          int usageCount, boolean complete, List<String> reasonCodes) {
        public Preview { dispositions=List.copyOf(dispositions); selectedRuleIds=List.copyOf(selectedRuleIds); shadowedRuleIds=List.copyOf(shadowedRuleIds); reasonCodes=List.copyOf(reasonCodes); }
    }

    public record OriginPrecondition(PolicyWorkspaceSnapshot.FileState expectedState, String fileDigest, String ruleSetDigest) {
        public OriginPrecondition { Objects.requireNonNull(expectedState); Objects.requireNonNull(ruleSetDigest); }
    }
    public record WorkspacePreconditions(String workspaceDigest, OriginPrecondition tracked, OriginPrecondition local,
                                         Set<String> expectedRuleIdsPresent, Set<String> expectedRuleIdsAbsent,
                                         String projectFingerprint) {
        public WorkspacePreconditions { Objects.requireNonNull(workspaceDigest); Objects.requireNonNull(tracked); Objects.requireNonNull(local); expectedRuleIdsPresent=Set.copyOf(expectedRuleIdsPresent); expectedRuleIdsAbsent=Set.copyOf(expectedRuleIdsAbsent); }
        public static WorkspacePreconditions from(PolicyWorkspaceSnapshot workspace, Set<String> present, Set<String> absent) {
            return new WorkspacePreconditions(workspace.effectiveWorkspaceDigest(), origin(workspace.tracked()), origin(workspace.local()), present, absent, workspace.projectFingerprint());
        }
        private static OriginPrecondition origin(PolicyWorkspaceSnapshot.OriginDocument value) { return new OriginPrecondition(value.fileState(), value.fileDigest(), value.semanticRuleSetDigest()); }
    }

    public record PendingPolicyChange(PolicyDraft draft, String sanitizedSummary,
                                      Set<HostAcknowledgement> requiredHostAcknowledgements,
                                      boolean hostApprovalRequired) {
        public PendingPolicyChange { Objects.requireNonNull(draft); if(!draft.transferable())throw new IllegalArgumentException("Blocked draft cannot be transferred"); requiredHostAcknowledgements=Set.copyOf(requiredHostAcknowledgements); if(!hostApprovalRequired)throw new IllegalArgumentException("Host approval is mandatory"); }
        public static PendingPolicyChange transfer(PolicyDraft draft, String summary) { return new PendingPolicyChange(draft, summary, draft.requiredHostAcknowledgements(), true); }
    }

    private static String calculateId(Action action, PolicyWorkspaceSnapshot.Origin destination, String oldRuleId,
                                      SelectorPolicy.Rule proposed, WorkspacePreconditions preconditions, Preview preview) {
        List<String> fields = new ArrayList<>();
        fields.add(action.name()); fields.add(destination.name()); fields.add(oldRuleId == null ? "" : oldRuleId);
        fields.add(proposed == null ? "" : proposed.ruleId()); fields.add(preconditions.workspaceDigest());
        fields.add(preview.scopeChoice().name()); fields.add(Integer.toString(preview.staticMatches()));
        fields.add(Integer.toString(preview.historyMatches())); fields.add(Boolean.toString(preview.complete()));
        return "selector-policy-draft-v1:sha256:" + CanonicalDigests.digest("selector-policy-draft-v1", fields.toArray(String[]::new));
    }
}
