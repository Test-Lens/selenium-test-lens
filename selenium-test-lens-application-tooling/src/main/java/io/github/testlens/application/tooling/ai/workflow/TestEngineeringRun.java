package io.github.testlens.application.tooling.ai.workflow;

import java.util.List;
import java.util.Objects;

/** Immutable state, provenance, metrics and audit trail of one workflow run. @since 0.5.0 */
public record TestEngineeringRun(String runId, TestEngineeringRequest request, State state,
                                 List<ArtifactEnvelope<?>> artifacts, Metrics metrics,
                                 List<AuditEntry> audit, String trustedApplyMarker) {
    public TestEngineeringRun {
        if (runId == null || runId.isBlank()) throw new IllegalArgumentException("runId is required");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(state, "state");
        artifacts = List.copyOf(artifacts == null ? List.of() : artifacts);
        metrics = metrics == null ? Metrics.zero() : metrics;
        audit = List.copyOf(audit == null ? List.of() : audit);
    }

    public static TestEngineeringRun create(String runId, TestEngineeringRequest request) {
        return new TestEngineeringRun(runId, request, State.CREATED, List.of(), Metrics.zero(), List.of(), null);
    }

    public boolean terminal() { return state.terminal; }

    public enum State {
        CREATED, CONTEXT_PREPARED, PLAN_REQUESTED, PLAN_READY, IMPLEMENTATION_REQUESTED,
        IMPLEMENTATION_READY, COMPILE_FAILED, EXECUTION_READY, EXECUTION_FAILED,
        FAILURE_CLASSIFIED, REPAIR_PROPOSED, AWAITING_TRUSTED_APPLY, RERUN_REQUIRED,
        REVIEW_REQUESTED,
        SUCCESS(true), REJECTED(true), BLOCKED(true), FAILED(true), NEEDS_HUMAN_REVIEW(true);

        private final boolean terminal;
        State() { this(false); }
        State(boolean terminal) { this.terminal = terminal; }
        public boolean terminal() { return terminal; }
    }

    public record Metrics(int corrections, int reruns, int repairs, int agentCalls,
                          int compilations, int executions) {
        public Metrics {
            if (corrections < 0 || reruns < 0 || repairs < 0 || agentCalls < 0 || compilations < 0 || executions < 0) {
                throw new IllegalArgumentException("metrics cannot be negative");
            }
        }
        static Metrics zero() { return new Metrics(0, 0, 0, 0, 0, 0); }
        Metrics correction() { return new Metrics(corrections + 1, reruns, repairs, agentCalls, compilations, executions); }
        Metrics rerun() { return new Metrics(corrections, reruns + 1, repairs, agentCalls, compilations, executions); }
        Metrics repair() { return new Metrics(corrections, reruns, repairs + 1, agentCalls, compilations, executions); }
        Metrics agentCall() { return new Metrics(corrections, reruns, repairs, agentCalls + 1, compilations, executions); }
        Metrics compilation() { return new Metrics(corrections, reruns, repairs, agentCalls, compilations + 1, executions); }
        Metrics execution() { return new Metrics(corrections, reruns, repairs, agentCalls, compilations, executions + 1); }
    }

    public record AuditEntry(int sequence, State from, State to, TestEngineeringWorkflow.EventType event,
                             String evidence) {
        public AuditEntry {
            if (sequence < 1 || from == null || to == null || event == null) throw new IllegalArgumentException("invalid audit entry");
            evidence = evidence == null ? "" : evidence;
        }
    }
}
