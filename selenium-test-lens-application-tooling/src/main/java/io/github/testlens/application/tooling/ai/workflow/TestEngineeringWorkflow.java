package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.RepairProposal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun.State;

/** Pure deterministic reducer for the test-engineering lifecycle. @since 0.5.0 */
public final class TestEngineeringWorkflow {
    private static final Map<State, EnumSet<EventType>> LEGAL = legalTransitions();
    private final WorkflowPolicy policy;

    public TestEngineeringWorkflow(WorkflowPolicy policy) { this.policy = Objects.requireNonNull(policy, "policy"); }

    public TestEngineeringRun reduce(TestEngineeringRun run, Event event) {
        Objects.requireNonNull(run, "run");
        Objects.requireNonNull(event, "event");
        if (run.terminal()) throw new IllegalStateException("terminal run cannot transition from " + run.state());
        if (!LEGAL.getOrDefault(run.state(), EnumSet.noneOf(EventType.class)).contains(event.type())) {
            throw new IllegalStateException("illegal transition: " + run.state() + " + " + event.type());
        }

        validateArtifact(run, event.artifact());
        if (event.type() == EventType.PROPOSE_REPAIR
                && !(event.artifact().payload() instanceof RepairProposal)) {
            throw new IllegalArgumentException("repair artifact must be a PROPOSE_ONLY RepairProposal");
        }
        if (event.type() == EventType.CAPABILITY_MISSING
                && !(event.artifact().payload() instanceof PageObjectExtensionProposal)) {
            throw new IllegalArgumentException("missing capability requires an extension proposal, never a selector");
        }
        State next = target(run, event);
        boolean boundReached = next == State.NEEDS_HUMAN_REVIEW && switch (event.type()) {
            case REQUEST_CORRECTION, PROPOSE_REPAIR, RERUN_REQUESTED -> true;
            default -> false;
        };
        TestEngineeringRun.Metrics metrics = boundReached ? run.metrics() : metrics(run.metrics(), event.type());
        String marker = run.trustedApplyMarker();
        if (event.type() == EventType.TRUSTED_APPLY_RECORDED) {
            if (event.trustedApplyMarker() == null || event.trustedApplyMarker().isBlank()) {
                throw new IllegalArgumentException("trusted apply marker is required");
            }
            marker = event.trustedApplyMarker();
        }

        List<ArtifactEnvelope<?>> artifacts = new ArrayList<>(run.artifacts());
        if (event.artifact() != null) artifacts.add(event.artifact());
        if (artifacts.size() > policy.maxArtifacts()) throw new IllegalStateException("artifact bound exceeded");
        List<TestEngineeringRun.AuditEntry> audit = new ArrayList<>(run.audit());
        audit.add(new TestEngineeringRun.AuditEntry(audit.size() + 1, run.state(), next, event.type(), event.evidence()));
        if (audit.size() > policy.maxAuditEntries()) throw new IllegalStateException("audit bound exceeded");
        return new TestEngineeringRun(run.runId(), run.request(), next, artifacts, metrics, audit, marker);
    }

    private State target(TestEngineeringRun run, Event event) {
        if (event.type() == EventType.REQUEST_CORRECTION && run.metrics().corrections() >= policy.maxCorrections()) return State.NEEDS_HUMAN_REVIEW;
        if (event.type() == EventType.PROPOSE_REPAIR && run.metrics().repairs() >= policy.maxRepairs()) return State.NEEDS_HUMAN_REVIEW;
        if (event.type() == EventType.RERUN_REQUESTED && run.metrics().reruns() >= policy.maxReruns()) return State.NEEDS_HUMAN_REVIEW;
        return switch (event.type()) {
            case PREPARE_CONTEXT -> State.CONTEXT_PREPARED;
            case REQUEST_PLAN -> State.PLAN_REQUESTED;
            case PLAN_PRODUCED -> State.PLAN_READY;
            case REQUEST_IMPLEMENTATION, REQUEST_CORRECTION -> State.IMPLEMENTATION_REQUESTED;
            case IMPLEMENTATION_PRODUCED -> State.IMPLEMENTATION_READY;
            case COMPILE_SUCCEEDED -> State.EXECUTION_READY;
            case COMPILE_FAILED -> State.COMPILE_FAILED;
            case EXECUTION_SUCCEEDED -> State.REVIEW_REQUESTED;
            case EXECUTION_FAILED -> State.EXECUTION_FAILED;
            case CLASSIFY_FAILURE -> State.FAILURE_CLASSIFIED;
            case PROPOSE_REPAIR -> State.REPAIR_PROPOSED;
            case REQUEST_TRUSTED_APPLY -> State.AWAITING_TRUSTED_APPLY;
            case TRUSTED_APPLY_RECORDED -> State.RERUN_REQUIRED;
            case REVIEW_APPROVED -> State.SUCCESS;
            case RERUN_REQUESTED -> State.IMPLEMENTATION_READY;
            case REVIEW_REJECTED, REJECT -> State.REJECTED;
            case CAPABILITY_MISSING -> State.BLOCKED;
            case FAIL -> State.FAILED;
            case REQUIRE_HUMAN_REVIEW -> State.NEEDS_HUMAN_REVIEW;
        };
    }

    private static TestEngineeringRun.Metrics metrics(TestEngineeringRun.Metrics metrics, EventType event) {
        return switch (event) {
            case REQUEST_PLAN, REQUEST_IMPLEMENTATION, CLASSIFY_FAILURE -> metrics.agentCall();
            case PROPOSE_REPAIR -> metrics.repair().agentCall();
            case REQUEST_CORRECTION -> metrics.correction().agentCall();
            case COMPILE_SUCCEEDED, COMPILE_FAILED -> metrics.compilation();
            case EXECUTION_SUCCEEDED, EXECUTION_FAILED -> metrics.execution();
            case RERUN_REQUESTED -> metrics.rerun();
            default -> metrics;
        };
    }

    private static void validateArtifact(TestEngineeringRun run, ArtifactEnvelope<?> artifact) {
        if (artifact == null) return;
        if (!run.runId().equals(artifact.runId())) throw new IllegalArgumentException("artifact belongs to another run");
        if (run.artifacts().stream().anyMatch(existing -> existing.artifactId().equals(artifact.artifactId()))) {
            throw new IllegalArgumentException("duplicate artifactId: " + artifact.artifactId());
        }
        for (String parent : artifact.parents()) {
            if (run.artifacts().stream().noneMatch(existing -> existing.artifactId().equals(parent))) {
                throw new IllegalArgumentException("unknown parent artifact: " + parent);
            }
        }
    }

    private static Map<State, EnumSet<EventType>> legalTransitions() {
        Map<State, EnumSet<EventType>> map = new EnumMap<>(State.class);
        allow(map, State.CREATED, EventType.PREPARE_CONTEXT);
        allow(map, State.CONTEXT_PREPARED, EventType.REQUEST_PLAN);
        allow(map, State.PLAN_REQUESTED, EventType.PLAN_PRODUCED);
        allow(map, State.PLAN_READY, EventType.REQUEST_IMPLEMENTATION);
        allow(map, State.IMPLEMENTATION_REQUESTED, EventType.IMPLEMENTATION_PRODUCED, EventType.CAPABILITY_MISSING);
        allow(map, State.IMPLEMENTATION_READY, EventType.COMPILE_SUCCEEDED, EventType.COMPILE_FAILED);
        allow(map, State.COMPILE_FAILED, EventType.REQUEST_CORRECTION, EventType.REJECT);
        allow(map, State.EXECUTION_READY, EventType.EXECUTION_SUCCEEDED, EventType.EXECUTION_FAILED);
        allow(map, State.EXECUTION_FAILED, EventType.CLASSIFY_FAILURE);
        allow(map, State.FAILURE_CLASSIFIED, EventType.PROPOSE_REPAIR, EventType.REJECT);
        allow(map, State.REPAIR_PROPOSED, EventType.REQUEST_TRUSTED_APPLY, EventType.REJECT);
        allow(map, State.AWAITING_TRUSTED_APPLY, EventType.TRUSTED_APPLY_RECORDED, EventType.REJECT);
        allow(map, State.RERUN_REQUIRED, EventType.RERUN_REQUESTED);
        allow(map, State.REVIEW_REQUESTED, EventType.REVIEW_APPROVED, EventType.REVIEW_REJECTED);
        for (State state : State.values()) if (!state.terminal()) {
            map.computeIfAbsent(state, ignored -> EnumSet.noneOf(EventType.class))
                    .addAll(EnumSet.of(EventType.FAIL, EventType.REQUIRE_HUMAN_REVIEW));
        }
        return Map.copyOf(map);
    }

    private static void allow(Map<State, EnumSet<EventType>> map, State state, EventType... events) {
        map.put(state, EnumSet.of(events[0], events));
    }

    public record Event(EventType type, ArtifactEnvelope<?> artifact, String evidence,
                        String trustedApplyMarker) {
        public Event {
            Objects.requireNonNull(type, "type");
            evidence = evidence == null ? "" : evidence;
            if (type.requiresArtifact && artifact == null) throw new IllegalArgumentException(type + " requires an artifact");
        }
        public static Event of(EventType type) { return new Event(type, null, "", null); }
        public static Event withArtifact(EventType type, ArtifactEnvelope<?> artifact) { return new Event(type, artifact, "", null); }
        public static Event trustedApply(String marker) { return new Event(EventType.TRUSTED_APPLY_RECORDED, null, "trusted apply", marker); }
    }

    public enum EventType {
        PREPARE_CONTEXT, REQUEST_PLAN, PLAN_PRODUCED(true), REQUEST_IMPLEMENTATION,
        IMPLEMENTATION_PRODUCED(true), COMPILE_SUCCEEDED, COMPILE_FAILED(true), REQUEST_CORRECTION,
        EXECUTION_SUCCEEDED(true), EXECUTION_FAILED(true), CLASSIFY_FAILURE(true), PROPOSE_REPAIR(true),
        REQUEST_TRUSTED_APPLY, TRUSTED_APPLY_RECORDED, RERUN_REQUESTED,
        REVIEW_APPROVED(true), REVIEW_REJECTED(true), CAPABILITY_MISSING(true), REJECT, FAIL,
        REQUIRE_HUMAN_REVIEW;

        private final boolean requiresArtifact;
        EventType() { this(false); }
        EventType(boolean requiresArtifact) { this.requiresArtifact = requiresArtifact; }
    }
}
