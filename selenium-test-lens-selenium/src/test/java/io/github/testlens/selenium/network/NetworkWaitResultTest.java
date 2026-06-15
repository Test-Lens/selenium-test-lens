package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkWaitResultTest {

    @Test
    void matchedResultKeepsResponse() {
        NetworkWaitCondition condition = NetworkWaitCondition.builder().urlContains("/api").build();
        NetworkEvent event = NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 200));

        NetworkWaitResult result = NetworkWaitResult.matched(condition, event, null, 2, Duration.ofMillis(20));

        assertEquals(NetworkWaitStatus.MATCHED, result.status());
        assertEquals(event.response(), result.matchedResponse());
        assertEquals(2, result.attempts());
    }

    @Test
    void timedOutResultHasReadableSummary() {
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .method("POST")
                .urlContains("/api/orders")
                .status(201)
                .build();
        NetworkSummary summary = NetworkSummary.from(List.of(
                NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 500))
        ), 0, 400, NetworkDiagnosticsStatus.STARTED);

        NetworkWaitResult result = NetworkWaitResult.timedOut(condition, 3, Duration.ofMillis(100), summary);

        assertEquals(NetworkWaitStatus.TIMED_OUT, result.status());
        assertEquals(NetworkWaitFailureReason.NO_MATCHING_RESPONSE, result.failureReason());
        assertTrue(result.message().contains("/api/orders"));
        assertTrue(result.message().contains("Seen responses: 1"));
    }
}

