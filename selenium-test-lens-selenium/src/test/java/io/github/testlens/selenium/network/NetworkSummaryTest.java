package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkSummaryTest {

    @Test
    void countsRequestsResponsesAndFailures() {
        NetworkSummary summary = NetworkSummary.from(List.of(
                NetworkEvent.request(NetworkRequest.of("GET", "/api/orders")),
                NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 200)),
                NetworkEvent.response(NetworkResponse.of("2", "/api/payments", 503)),
                NetworkEvent.failed(NetworkFailure.of("3", "/api/profile", "net::ERR_FAILED"))
        ), 1, 400, NetworkDiagnosticsStatus.STARTED);

        assertEquals(1, summary.totalRequests());
        assertEquals(2, summary.totalResponses());
        assertEquals(1, summary.failedResponses());
        assertEquals(1, summary.failedRequests());
        assertEquals(1, summary.ignoredEvents());
        assertTrue(summary.hasFailures());
        assertTrue(summary.failureSummary().contains("responses=1"));
    }
}
