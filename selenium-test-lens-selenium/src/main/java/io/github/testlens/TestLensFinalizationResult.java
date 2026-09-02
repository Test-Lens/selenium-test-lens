package io.github.testlens;

import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.RetrySummary;
import io.github.testlens.core.trace.RetryOutcomePolicy;

import java.nio.file.Path;
import java.util.List;

/**
 * Result of best-effort finalization. Diagnostic failures remain data; an outcome-policy violation is
 * instead thrown after finalization and carries its own {@link RetrySummary}.
 */
public record TestLensFinalizationResult(
        UiTestLensSession session,
        Path outputDirectory,
        Path jsonReport,
        Path htmlReport,
        Path failureScreenshot,
        List<Throwable> diagnosticFailures) {
    public TestLensFinalizationResult {
        diagnosticFailures = diagnosticFailures == null ? List.of() : List.copyOf(diagnosticFailures);
    }
    public boolean fullySuccessful() { return diagnosticFailures.isEmpty(); }
    public RetrySummary retrySummary() {
        return session == null
                ? new RetrySummary(0, java.time.Duration.ZERO, false, RetryOutcomePolicy.REPORT_ONLY,
                false, java.util.Map.of(), java.util.Map.of(), java.util.Map.of())
                : session.retrySummary();
    }
}
