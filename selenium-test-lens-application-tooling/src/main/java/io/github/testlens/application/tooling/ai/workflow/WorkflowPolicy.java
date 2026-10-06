package io.github.testlens.application.tooling.ai.workflow;

/** Hard bounds for deterministic workflow correction cycles. @since 0.5.0 */
public record WorkflowPolicy(int maxCorrections, int maxReruns, int maxRepairs,
                             int maxArtifacts, int maxAuditEntries) {
    public WorkflowPolicy {
        bounded("maxCorrections", maxCorrections, 0, 20);
        bounded("maxReruns", maxReruns, 0, 20);
        bounded("maxRepairs", maxRepairs, 0, 20);
        bounded("maxArtifacts", maxArtifacts, 1, 10_000);
        bounded("maxAuditEntries", maxAuditEntries, 1, 100_000);
    }
    public static WorkflowPolicy defaults() { return new WorkflowPolicy(2, 2, 2, 100, 1_000); }
    private static void bounded(String name, int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException(name + " is outside supported bounds");
    }
}
