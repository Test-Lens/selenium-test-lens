package io.github.testlens.selenium.network;

/**
 * Network capture modes retained by the public API.
 * Only {@link #MANUAL} is implemented in 0.1.x; {@link #AUTO}, {@link #BIDI}, and
 * {@link #PERFORMANCE_LOGS} report {@code UNSUPPORTED} instead of falling back.
 */
public enum NetworkCaptureMode {
    OFF,
    MANUAL,
    PERFORMANCE_LOGS,
    BIDI,
    AUTO
}

