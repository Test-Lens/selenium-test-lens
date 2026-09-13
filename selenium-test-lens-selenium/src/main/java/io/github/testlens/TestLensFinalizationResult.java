package io.github.testlens;

import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.RetrySummary;
import io.github.testlens.core.trace.RetryOutcomePolicy;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.nio.file.Files;

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
    public Optional<Path> failureBundleDirectory() {
        return existing(outputDirectory == null ? null : outputDirectory.resolve("failure-bundle"));
    }
    public Optional<Path> failureBundleManifest() {
        return existing(outputDirectory == null ? null : outputDirectory.resolve("failure-bundle").resolve("manifest.json"));
    }
    /**
     * Returns the clean, visually redacted failure screenshot when the failure bundle produced it.
     * "Clean" excludes Test Lens diagnostic decorations; it does not bypass visual redaction.
     *
     * @return the existing clean screenshot path, or an empty optional
     * @since 0.3.0
     */
    public Optional<Path> cleanFailureScreenshot() {
        return existing(outputDirectory == null ? null : outputDirectory.resolve("failure-bundle").resolve("failure-clean.png"));
    }
    public Optional<Path> failureBundleArchive() {
        return existing(outputDirectory == null ? null : outputDirectory.resolve("failure-bundle.zip"));
    }
    private static Optional<Path> existing(Path path) {
        return path != null && Files.exists(path) ? Optional.of(path) : Optional.empty();
    }
}
