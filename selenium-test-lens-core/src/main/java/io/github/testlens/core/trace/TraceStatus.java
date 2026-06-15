package io.github.testlens.core.trace;

/**
 * Status values used by trace events and session metadata.
 */
public enum TraceStatus {
    STARTED,
    PASSED,
    FAILED,
    SKIPPED,
    INFO,
    WARNING,
    ERROR
}
