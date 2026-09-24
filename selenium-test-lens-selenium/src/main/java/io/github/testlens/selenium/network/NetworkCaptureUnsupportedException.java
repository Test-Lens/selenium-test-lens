package io.github.testlens.selenium.network;

import java.util.Map;

/** Internal signal distinguishing an unavailable BiDi session from a failed subscription. */
final class NetworkCaptureUnsupportedException extends BiDiCaptureException {
    NetworkCaptureUnsupportedException(String message) {
        this(message, Map.of());
    }

    NetworkCaptureUnsupportedException(String message, Map<String, String> diagnostics) {
        super(BiDiFailureCategory.UNSUPPORTED, message, diagnostics, null);
    }
}
