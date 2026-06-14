package io.github.mmaciekk111.uitestlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class NetworkAssertionErrorTest {

    @Test
    void storesSummary() {
        NetworkSummary summary = NetworkSummary.from(List.of(
                NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 500))
        ), 0, 400, NetworkDiagnosticsStatus.STARTED);

        NetworkAssertionError error = new NetworkAssertionError("failed", summary);

        assertSame(summary, error.summary());
    }
}
