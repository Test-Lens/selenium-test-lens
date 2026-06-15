package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NetworkAssertionErrorTest {

    @Test
    void storesSummary() {
        NetworkSummary summary = NetworkSummary.from(List.of(
                NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 500))
        ), 0, 400, NetworkDiagnosticsStatus.STARTED);

        NetworkAssertionError error = new NetworkAssertionError("failed", summary);

        assertSame(summary, error.summary());
    }

    @Test
    void storesWaitResult() {
        NetworkSummary summary = NetworkSummary.from(List.of(), 0, 400, NetworkDiagnosticsStatus.STARTED);
        NetworkWaitResult waitResult = NetworkWaitResult.timedOut(
                NetworkWaitCondition.builder().urlContains("/api/orders").build(),
                1,
                java.time.Duration.ofMillis(10),
                summary
        );

        NetworkAssertionError error = new NetworkAssertionError("timeout", summary, waitResult);

        assertSame(summary, error.summary());
        assertEquals(NetworkWaitStatus.TIMED_OUT, error.waitResult().status());
    }
}

