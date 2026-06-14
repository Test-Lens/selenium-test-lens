package io.github.mmaciekk111.uitestlens.selenium.network;

import java.time.Duration;

public final class NetworkDiagnosticsResult {
    private final NetworkDiagnosticsStatus status;
    private final String message;
    private final NetworkSummary summary;
    private final Throwable exception;
    private final Duration elapsed;

    public NetworkDiagnosticsResult(NetworkDiagnosticsStatus status, String message, NetworkSummary summary, Throwable exception, Duration elapsed) {
        this.status = status == null ? NetworkDiagnosticsStatus.FAILED : status;
        this.message = message == null ? "" : message;
        this.summary = summary;
        this.exception = exception;
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
    }

    public static NetworkDiagnosticsResult of(NetworkDiagnosticsStatus status, String message, NetworkSummary summary, Duration elapsed) {
        return new NetworkDiagnosticsResult(status, message, summary, null, elapsed);
    }

    public static NetworkDiagnosticsResult failed(String message, NetworkSummary summary, Throwable exception, Duration elapsed) {
        return new NetworkDiagnosticsResult(NetworkDiagnosticsStatus.FAILED, message, summary, exception, elapsed);
    }

    public NetworkDiagnosticsStatus status() { return status; }
    public String message() { return message; }
    public NetworkSummary summary() { return summary; }
    public Throwable exception() { return exception; }
    public Duration elapsed() { return elapsed; }
}
