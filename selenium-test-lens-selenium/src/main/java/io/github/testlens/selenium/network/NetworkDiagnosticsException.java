package io.github.testlens.selenium.network;

public final class NetworkDiagnosticsException extends RuntimeException {
    public NetworkDiagnosticsException(String message) {
        super(message);
    }

    public NetworkDiagnosticsException(String message, Throwable cause) {
        super(message, cause);
    }
}

