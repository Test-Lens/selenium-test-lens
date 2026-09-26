package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.PolicyDraft;
import io.github.testlens.selector.engine.PolicyWorkspaceSnapshot;
import io.github.testlens.selector.engine.SelectorPolicy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Trusted host-only policy mutation boundary. It has no Selenium or Lab dependency. */
public final class SelectorPolicyDraftApplier {
    private final SelectorPolicyWorkspace loader = new SelectorPolicyWorkspace();

    public ApplyResult apply(TrustedPolicyWorkspace trusted, PolicyDraft.PendingPolicyChange pending,
                             Set<PolicyDraft.HostAcknowledgement> hostAcknowledgements) {
        Objects.requireNonNull(trusted, "trusted"); Objects.requireNonNull(pending, "pending");
        Set<PolicyDraft.HostAcknowledgement> supplied = hostAcknowledgements == null || hostAcknowledgements.isEmpty()
                ? Set.of() : EnumSet.copyOf(hostAcknowledgements);
        if (!supplied.containsAll(pending.requiredHostAcknowledgements()))
            return result(Status.ACKNOWLEDGEMENT_REQUIRED, pending.draft(), null, "Required host acknowledgement is missing");
        PolicyDraft draft = pending.draft();
        if (!draft.blockingIssues().isEmpty()) return result(Status.BLOCKED_CONFLICT, draft, null, "Draft has blocking issues");
        try {
            Path root = SelectorPolicyWorkspace.canonicalRoot(trusted.projectRoot());
            PolicyWorkspaceSnapshot current = loader.load(root, trusted.projectFingerprint());
            if (knownMismatch(draft.workspacePreconditions().projectFingerprint(), trusted.projectFingerprint()))
                return result(Status.PROJECT_MISMATCH, draft, current.effectiveWorkspaceDigest(), "Project fingerprint differs");
            if (!preconditionsMatch(draft.workspacePreconditions(), current))
                return result(Status.STALE_DRAFT, draft, current.effectiveWorkspaceDigest(), "Tracked or local policy workspace changed");
            PolicyWorkspaceSnapshot.OriginRule duplicate = draft.proposedRule() == null ? null : current.find(draft.proposedRule().ruleId());
            if (duplicate != null && !(draft.action() == PolicyDraft.Action.REPLACE && duplicate.rule().ruleId().equals(draft.oldRuleId())))
                return new ApplyResult(Status.ALREADY_EXISTS, draft.oldRuleId(), draft.proposedRule().ruleId(), duplicate.origin(), current.effectiveWorkspaceDigest(), "Identical rule already exists in " + duplicate.origin());

            PolicyWorkspaceSnapshot.OriginDocument destination = draft.destination() == PolicyWorkspaceSnapshot.Origin.TRACKED ? current.tracked() : current.local();
            List<SelectorPolicy.Rule> rules = new ArrayList<>(destination.rules());
            int oldIndex = indexOf(rules, draft.oldRuleId());
            switch (draft.action()) {
                case ADD -> rules.add(draft.proposedRule());
                case REMOVE -> { if (oldIndex < 0) return result(Status.STALE_DRAFT, draft, current.effectiveWorkspaceDigest(), "Rule to remove is absent"); rules.remove(oldIndex); }
                case REPLACE -> { if (oldIndex < 0) return result(Status.STALE_DRAFT, draft, current.effectiveWorkspaceDigest(), "Rule to replace is absent"); rules.set(oldIndex, draft.proposedRule()); }
            }
            rules.sort(Comparator.comparing(SelectorPolicy.Rule::ruleId));
            SelectorPolicy.Document updated = new SelectorPolicy.Document(SelectorPolicy.SCHEMA_VERSION,
                    current.canonicalizationVersion(), rules);
            Path target = SelectorPolicyWorkspace.destination(root, draft.destination());
            SelectorPolicyWorkspace.rejectLinkEscape(root, target);
            SelectorPolicyJson.writeAtomic(updated, root, target);
            PolicyWorkspaceSnapshot after = loader.load(root, trusted.projectFingerprint());
            if (draft.proposedRule() != null && after.find(draft.proposedRule().ruleId()) == null)
                return result(Status.WRITE_FAILED, draft, after.effectiveWorkspaceDigest(), "Post-write verification did not find proposed rule");
            if (draft.oldRuleId() != null && !draft.oldRuleId().equals(draft.proposedRule() == null ? null : draft.proposedRule().ruleId()) && after.find(draft.oldRuleId()) != null)
                return result(Status.WRITE_FAILED, draft, after.effectiveWorkspaceDigest(), "Post-write verification found removed rule");
            return result(Status.APPLIED, draft, after.effectiveWorkspaceDigest(), "Policy workspace updated by trusted host");
        } catch (SelectorPolicyJson.PolicyFormatException malformed) {
            return result(Status.POLICY_FILE_INVALID, pending.draft(), null, malformed.getMessage());
        } catch (IllegalArgumentException path) {
            return result(Status.PATH_REJECTED, pending.draft(), null, path.getMessage());
        } catch (IOException failure) {
            return result(Status.WRITE_FAILED, pending.draft(), null, failure.getClass().getSimpleName());
        }
    }

    private static boolean preconditionsMatch(PolicyDraft.WorkspacePreconditions expected, PolicyWorkspaceSnapshot actual) {
        if (!expected.workspaceDigest().equals(actual.effectiveWorkspaceDigest())) return false;
        if (!originMatches(expected.tracked(), actual.tracked()) || !originMatches(expected.local(), actual.local())) return false;
        Set<String> ids = actual.originRules().stream().map(value -> value.rule().ruleId()).collect(java.util.stream.Collectors.toSet());
        return ids.containsAll(expected.expectedRuleIdsPresent()) && java.util.Collections.disjoint(ids, expected.expectedRuleIdsAbsent());
    }

    private static boolean originMatches(PolicyDraft.OriginPrecondition expected, PolicyWorkspaceSnapshot.OriginDocument actual) {
        return expected.expectedState() == actual.fileState()
                && Objects.equals(expected.fileDigest(), actual.fileDigest())
                && expected.ruleSetDigest().equals(actual.semanticRuleSetDigest());
    }

    private static boolean knownMismatch(String expected, String supplied) { return expected != null && supplied != null && !expected.equals(supplied); }
    private static int indexOf(List<SelectorPolicy.Rule> rules, String id) { if(id==null)return -1; for(int i=0;i<rules.size();i++)if(rules.get(i).ruleId().equals(id))return i;return -1; }
    private static ApplyResult result(Status status, PolicyDraft draft, String digest, String message) { return new ApplyResult(status, draft.oldRuleId(), draft.proposedRule()==null?null:draft.proposedRule().ruleId(), draft.destination(), digest, message); }

    public record TrustedPolicyWorkspace(Path projectRoot, String projectFingerprint) {
        public TrustedPolicyWorkspace { Objects.requireNonNull(projectRoot, "projectRoot"); }
    }
    public enum Status { APPLIED, ALREADY_EXISTS, ALREADY_REMOVED, STALE_DRAFT, ACKNOWLEDGEMENT_REQUIRED, POLICY_FILE_INVALID, PROJECT_MISMATCH, PATH_REJECTED, WRITE_FAILED, BLOCKED_CONFLICT }
    public record ApplyResult(Status status, String oldRuleId, String newRuleId,
                              PolicyWorkspaceSnapshot.Origin destination, String resultingWorkspaceDigest, String message) { }
}
