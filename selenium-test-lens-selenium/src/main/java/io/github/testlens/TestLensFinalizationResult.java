package io.github.testlens;

import io.github.testlens.core.trace.UiTestLensSession;

import java.nio.file.Path;
import java.util.List;

/** Result of best-effort finalization. Diagnostic failures are data, never thrown. */
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
}
