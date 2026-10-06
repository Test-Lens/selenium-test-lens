package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.json.StrictJson;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Auditable machine- and human-readable projection of one bounded workflow run. @since 0.5.0 */
public record WorkflowReport(int schemaVersion, String runId, String requirement,
                             TestEngineeringRun.State finalState, List<Step> steps,
                             Metrics metrics, List<String> limitations) {
    public static final int SCHEMA_VERSION = 1;

    public WorkflowReport {
        if (schemaVersion != SCHEMA_VERSION || runId == null || runId.isBlank()
                || requirement == null || finalState == null || metrics == null) {
            throw new IllegalArgumentException("complete workflow report identity is required");
        }
        steps = (steps == null ? List.<Step>of() : steps).stream()
                .sorted(Comparator.comparingInt(Step::sequence)).toList();
        limitations = canonical(limitations);
    }

    public static WorkflowReport from(TestEngineeringRun run, List<Step> steps, Metrics metrics,
                                      List<String> limitations) {
        Objects.requireNonNull(run, "run");
        return new WorkflowReport(SCHEMA_VERSION, run.runId(), run.request().requirement().summary(),
                run.state(), steps, metrics, limitations);
    }

    public byte[] json() { return StrictJson.write(this); }

    public String text() {
        StringBuilder out = new StringBuilder("Test Lens workflow ").append(runId).append('\n')
                .append("Requirement: ").append(requirement).append('\n')
                .append("Result: ").append(finalState).append('\n');
        for (Step step : steps) {
            out.append(step.sequence()).append(". ").append(step.name()).append(": ")
                    .append(step.status()).append(" (").append(step.durationMillis()).append(" ms)");
            if (!step.summary().isBlank()) out.append(" - ").append(step.summary());
            out.append('\n');
        }
        out.append("Context: ").append(metrics.contextBytes()).append(" bytes; agent I/O: ")
                .append(metrics.agentInputBytes()).append('/').append(metrics.agentOutputBytes())
                .append(" bytes; attempts: ").append(metrics.agentAttempts()).append('\n')
                .append("Deterministic: ").append(metrics.deterministicDurationMillis())
                .append(" ms; external agents: ").append(metrics.externalAgentDurationMillis())
                .append(" ms; total: ").append(metrics.totalDurationMillis()).append(" ms\n");
        if (!limitations.isEmpty()) out.append("Limitations: ").append(String.join(", ", limitations)).append('\n');
        return out.toString();
    }

    public String jsonText() { return new String(json(), StandardCharsets.UTF_8); }

    public record Step(int sequence, String name, Status status, long durationMillis,
                       long inputBytes, long outputBytes, String summary, List<String> evidenceRefs) {
        public Step {
            if (sequence < 1 || name == null || name.isBlank() || status == null
                    || durationMillis < 0 || inputBytes < 0 || outputBytes < 0) {
                throw new IllegalArgumentException("invalid workflow report step");
            }
            summary = summary == null ? "" : summary;
            evidenceRefs = canonical(evidenceRefs);
        }
    }

    public record Metrics(long contextBytes, long agentInputBytes, long agentOutputBytes,
                          long externalAgentDurationMillis, long deterministicDurationMillis,
                          long compileDurationMillis, long executionDurationMillis,
                          long totalDurationMillis, int agentAttempts, int compileAttempts,
                          int executionAttempts, int changedSourceFiles, int repairProposals) {
        public Metrics {
            if (contextBytes < 0 || agentInputBytes < 0 || agentOutputBytes < 0
                    || externalAgentDurationMillis < 0 || deterministicDurationMillis < 0
                    || compileDurationMillis < 0 || executionDurationMillis < 0 || totalDurationMillis < 0
                    || agentAttempts < 0 || compileAttempts < 0 || executionAttempts < 0
                    || changedSourceFiles < 0 || repairProposals < 0) {
                throw new IllegalArgumentException("workflow metrics cannot be negative");
            }
        }
    }

    public enum Status { PASS, FAIL, BLOCKED, NOT_RUN, PROPOSE_ONLY, APPROVE, REQUEST_CHANGES }

    private static List<String> canonical(List<String> values) {
        return (values == null ? List.<String>of() : values).stream().filter(Objects::nonNull)
                .distinct().sorted().toList();
    }
}
