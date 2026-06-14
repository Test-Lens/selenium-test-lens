package io.github.mmaciekk111.uitestlens.selenium.network;

public final class NetworkAssertionError extends AssertionError {
    private final NetworkSummary summary;

    public NetworkAssertionError(String message, NetworkSummary summary) {
        super(message);
        this.summary = summary;
    }

    public NetworkSummary summary() {
        return summary;
    }
}
