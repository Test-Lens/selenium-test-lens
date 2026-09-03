package io.github.testlens.selenium.network;

/** Internal signal distinguishing an unavailable BiDi session from a failed subscription. */
final class NetworkCaptureUnsupportedException extends RuntimeException {
    NetworkCaptureUnsupportedException(String message) {
        super(message);
    }
}
