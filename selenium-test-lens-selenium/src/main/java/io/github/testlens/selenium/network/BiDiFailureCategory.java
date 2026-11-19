package io.github.testlens.selenium.network;

/** Internal classification for BiDi acquisition and capture startup failures. */
enum BiDiFailureCategory {
    UNSUPPORTED,
    INITIALIZATION_FAILED,
    REMOTE_ENDPOINT_UNAVAILABLE,
    SESSION_CLOSED,
    PROTOCOL_ERROR,
    TRANSPORT_ERROR,
    BINARY_INCOMPATIBILITY,
    CAPTURE_CALLBACK_FAILED,
    CAPTURE_START_FAILED
}
