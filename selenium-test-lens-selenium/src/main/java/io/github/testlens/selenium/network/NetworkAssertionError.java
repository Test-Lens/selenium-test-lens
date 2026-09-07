package io.github.testlens.selenium.network;

import io.github.testlens.core.redaction.RedactionPolicy;

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

    static NetworkAssertionError redacted(String message, NetworkSummary summary, NetworkWaitResult waitResult,
                                           RedactionPolicy policy) {
        RedactionPolicy effective = policy == null ? RedactionPolicy.defaults() : policy;
        NetworkAssertionError error = new NetworkAssertionError(effective.redact(message), summary, waitResult);
        Throwable cause = waitResult == null ? null : waitResult.exception();
        if (cause != null) error.initCause(cause);
        return error;
    }
}

