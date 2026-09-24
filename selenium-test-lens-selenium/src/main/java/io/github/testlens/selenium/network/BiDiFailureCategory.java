package io.github.testlens.selenium.network;

/** Internal classification for BiDi acquisition and capture startup failures. */
enum BiDiFailureCategory {
    UNSUPPORTED,
    INITIALIZATION_FAILED,
    REMOTE_ENDPOINT_UNAVAILABLE,
    SESSION_CLOSED,
    PROTOCOL_ERROR,
    CAPTURE_START_FAILED
}
