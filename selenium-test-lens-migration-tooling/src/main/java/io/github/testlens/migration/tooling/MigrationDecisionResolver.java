package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Resolves active decisions exclusively through explicit decision-ref supersession. */
public final class MigrationDecisionResolver {
    public Result resolve(MigrationDecisionLedger ledger, MigrationProposal proposal, String checkpoint,
            List<MigrationEvidenceRef> evidence) {
        Map<String, MigrationDecisionLedger.DecisionRecord> byRef = new HashMap<>();
        for (var decision : ledger.decisions()) {
            if (!valid(decision)) return invalid("DECISION_RECORD_INTEGRITY");
            if (byRef.put(decision.decisionRef(), decision) != null) return invalid("DUPLICATE_DECISION_REF");
        }
        Set<String> superseded = new HashSet<>();
        for (var decision : ledger.decisions()) {
            String prior = decision.supersedesDecisionRef();
            if (prior == null) continue;
            var referenced = byRef.get(prior);
            if (referenced == null) return invalid("MISSING_SUPERSEDED_DECISION");
            if (!referenced.proposalId().equals(decision.proposalId())) return invalid("CROSS_PROPOSAL_SUPERSESSION");
            superseded.add(prior);
        }
        for (String ref : byRef.keySet()) if (cycle(ref, byRef)) return invalid("DECISION_SUPERSESSION_CYCLE");
        List<MigrationDecisionLedger.DecisionRecord> active = ledger.decisions().stream()
                .filter(x -> x.proposalId().equals(proposal.proposalId()) && !superseded.contains(x.decisionRef())).toList();
        if (active.size() != 1) return new Result(Status.DECISION_AMBIGUOUS, null, List.of("ACTIVE_DECISION_COUNT:" + active.size()));
        var decision = active.get(0);
        var validation = MigrationDecisionLedger.create(List.of(decision)).validateApproval(proposal, checkpoint, evidence);
        if (decision.decision() != MigrationDecisionLedger.Decision.APPROVE_FOR_S11)
            return new Result(Status.NOT_APPROVED, decision, List.of("ACTIVE_DECISION:" + decision.decision()));
        if (!validation.valid()) return new Result(Status.INVALID, decision, validation.reasons());
        return new Result(Status.APPROVED, decision, List.of());
    }
    private static boolean cycle(String start, Map<String, MigrationDecisionLedger.DecisionRecord> byRef) {
        Set<String> seen = new HashSet<>(); String current = start;
        while (current != null) {
            if (!seen.add(current)) return true;
            var value = byRef.get(current); current = value == null ? null : value.supersedesDecisionRef();
        }
        return false;
    }
    private static Result invalid(String issue) { return new Result(Status.LEDGER_INVALID, null, List.of(issue)); }
    private static boolean valid(MigrationDecisionLedger.DecisionRecord d) {
        if (d.decisionRef()==null||d.proposalId()==null||d.proposalSemanticDigest()==null
                ||d.sourceCheckpointRef()==null||d.sourcePreconditionDigest()==null) return false;
        String expected="migration-decision-v1:sha256:"+MigrationDigests.digest("migration-decision-v1",
                d.proposalId(),d.sourceCheckpointRef(),d.sourcePreconditionDigest(),d.decision().name(),
                d.decisionProvenance(),d.supersedesDecisionRef()==null?"":d.supersedesDecisionRef());
        return expected.equals(d.decisionRef())
                &&d.proposalId().matches("migration-proposal-v1:sha256:[0-9a-f]{64}")
                &&d.proposalSemanticDigest().matches("migration-proposal-v1:sha256:[0-9a-f]{64}")
                &&d.sourceCheckpointRef().matches("migration-checkpoint-v1:sha256:[0-9a-f]{64}")
                &&d.sourcePreconditionDigest().matches("migration-source-preconditions-v1:sha256:[0-9a-f]{64}")
                &&d.evidenceDigests().stream().allMatch(x->x.matches("sha256:[0-9a-f]{64}"));
    }
    public enum Status { APPROVED, NOT_APPROVED, DECISION_AMBIGUOUS, LEDGER_INVALID, INVALID }
    public record Result(Status status, MigrationDecisionLedger.DecisionRecord effectiveDecision, List<String> issues) {
        public Result { issues = List.copyOf(issues); }
    }
}
