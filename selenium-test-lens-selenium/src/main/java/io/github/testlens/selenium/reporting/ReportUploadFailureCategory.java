package io.github.testlens.selenium.reporting;

/** Stable classification of report upload failures. */
public enum ReportUploadFailureCategory {
    /** No failure occurred. */
    NONE,
    /** Required local evidence was missing, incomplete, unsafe, or unreadable. */
    INVALID_ARTIFACT,
    /** The completed payload exceeded the configured limit or the receiver returned {@code 413}. */
    PAYLOAD_TOO_LARGE,
    /** The receiver returned {@code 401} or {@code 403}. */
    AUTHENTICATION,
    /** The receiver rejected the request with another terminal HTTP response. */
    REJECTED_REQUEST,
    /** The receiver returned {@code 429}. */
    RATE_LIMITED,
    /** The receiver returned a {@code 5xx} response. */
    SERVER_ERROR,
    /** The request timed out or the receiver returned {@code 408}. */
    TIMEOUT,
    /** Connection, TLS, I/O, or interruption prevented completion. */
    TRANSPORT,
    /** A response did not match a recognized HTTP outcome. */
    INVALID_RESPONSE,
    /** Proxy options could not be applied. */
    PROXY_CONFIGURATION
}
