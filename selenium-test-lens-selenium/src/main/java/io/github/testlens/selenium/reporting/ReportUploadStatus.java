package io.github.testlens.selenium.reporting;

/** Describes whether a report payload reached the configured HTTP endpoint. */
public enum ReportUploadStatus {
    /** The endpoint returned a successful {@code 2xx} response. */
    UPLOADED,
    /** Validation or transport failed. */
    FAILED,
    /** No upload was attempted because no finalized session was available. */
    SKIPPED
}
