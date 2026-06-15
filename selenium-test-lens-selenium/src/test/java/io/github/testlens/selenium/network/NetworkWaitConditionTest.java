package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkWaitConditionTest {

    @Test
    void matchesResponseByUrlContainsAndStatus() {
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .status(201)
                .build();

        assertTrue(condition.matches(
                NetworkEvent.response(NetworkResponse.of("1", "https://app.test/api/orders", 201)),
                List.of()
        ));
    }

    @Test
    void matchesResponseMethodThroughRequestId() {
        NetworkRequest request = new NetworkRequest("req-1", "POST", "/api/orders", "", null, null);
        NetworkEvent requestEvent = NetworkEvent.request(request);
        NetworkEvent responseEvent = NetworkEvent.response(NetworkResponse.of("req-1", "/api/orders", 201));
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .method("POST")
                .status(201)
                .build();

        assertTrue(condition.matches(responseEvent, List.of(requestEvent, responseEvent)));
    }

    @Test
    void exactUrlRegexAndStatusRangeAreSupported() {
        NetworkWaitCondition exact = NetworkWaitCondition.builder().exactUrl("https://app.test/api/orders").build();
        NetworkWaitCondition regex = NetworkWaitCondition.builder().urlRegex(".*/api/order[s]?").build();
        NetworkWaitCondition range = NetworkWaitCondition.builder().statusBetween(200, 299).build();
        NetworkEvent response = NetworkEvent.response(NetworkResponse.of("1", "https://app.test/api/orders", 204));

        assertTrue(exact.matches(response, List.of(response)));
        assertTrue(regex.matches(response, List.of(response)));
        assertTrue(range.matches(response, List.of(response)));
    }

    @Test
    void requestOnlyMatchesRequestEvent() {
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .method("POST")
                .matchRequestOnly(true)
                .build();

        assertTrue(condition.matches(NetworkEvent.request(NetworkRequest.of("POST", "/api/orders")), List.of()));
        assertFalse(condition.matches(NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 200)), List.of()));
    }

    @Test
    void failedResponsesCanBeExcluded() {
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .urlContains("/api/orders")
                .status(500)
                .includeFailedResponses(false)
                .build();
        NetworkEvent event = NetworkEvent.response(NetworkResponse.of("1", "/api/orders", 500));

        assertFalse(condition.matches(event, List.of(event)));
        assertTrue(condition.matchesFailedResponse(event, List.of(event)));
    }

    @Test
    void rejectsInvalidRegex() {
        assertThrows(IllegalArgumentException.class, () -> NetworkWaitCondition.builder()
                .urlRegex("[")
                .build());
    }

    @Test
    void invalidDurationsFallBackToDefaults() {
        NetworkWaitCondition condition = NetworkWaitCondition.builder()
                .timeout(Duration.ZERO)
                .pollInterval(Duration.ZERO)
                .build();

        assertTrue(!condition.timeout().isZero() && !condition.timeout().isNegative());
        assertTrue(!condition.pollInterval().isZero() && !condition.pollInterval().isNegative());
    }
}
