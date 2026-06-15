package io.github.testlens.selenium.network;

public final class NetworkAssertionError extends AssertionError {
    private final NetworkSummary summary;
    private final NetworkWaitResult waitResult;

    public NetworkAssertionError(String message, NetworkSummary summary) {
        super(message);
        this.summary = summary;
        this.waitResult = null;
    }

    public NetworkAssertionError(String message, NetworkSummary summary, NetworkWaitResult waitResult) {
        super(message);
        this.summary = summary;
        this.waitResult = waitResult;
    }

    public NetworkSummary summary() {
        return summary;
    }

    public NetworkWaitResult waitResult() {
        return waitResult;
    }
}

