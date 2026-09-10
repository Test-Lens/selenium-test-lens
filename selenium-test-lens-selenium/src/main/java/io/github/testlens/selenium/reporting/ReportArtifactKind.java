package io.github.testlens.selenium.reporting;

/** Identifies the ZIP payload selected from a completed Test Lens finalization. */
public enum ReportArtifactKind {
    /** A temporary ZIP containing the final trace and HTML report. */
    SESSION_REPORT,
    /** The completed failure-bundle ZIP produced by failed finalization. */
    FAILURE_BUNDLE
}
