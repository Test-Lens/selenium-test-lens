package io.github.testlens.selenium.network;

import java.time.Duration;

public final class NetworkResponseExpectation {
    private final NetworkDiagnostics diagnostics;
    private final NetworkWaitCondition.Builder builder = NetworkWaitCondition.builder();

    NetworkResponseExpectation(NetworkDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public NetworkResponseExpectation urlContains(String value) {
        builder.urlContains(value);
        return this;
    }

    public NetworkResponseExpectation urlRegex(String regex) {
        builder.urlRegex(regex);
        return this;
    }

    public NetworkResponseExpectation exactUrl(String url) {
        builder.exactUrl(url);
        return this;
    }

    public NetworkResponseExpectation method(String method) {
        builder.method(method);
        return this;
    }

    public NetworkResponseExpectation status(int status) {
        builder.status(status);
        return this;
    }

    public NetworkResponseExpectation statusBetween(int min, int max) {
        builder.statusBetween(min, max);
        return this;
    }

    public NetworkWaitResult within(Duration timeout) {
        NetworkWaitResult result = diagnostics.waitForResponse(builder.timeout(timeout).build());
        if (result.status() != NetworkWaitStatus.MATCHED) {
            throw new NetworkAssertionError(result.message(), diagnostics.summary(), result);
        }
        return result;
    }

    public NetworkWaitResult waitNow() {
        NetworkWaitResult result = diagnostics.waitForResponse(builder.build());
        if (result.status() != NetworkWaitStatus.MATCHED) {
            throw new NetworkAssertionError(result.message(), diagnostics.summary(), result);
        }
        return result;
    }
}
