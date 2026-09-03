package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkHudFilterTest {
    @Test
    void defaultsHideRequestsAndShowResponsesFailuresAndFailedResponses() {
        NetworkHudFilter filter = NetworkHudFilter.defaults();

        assertFalse(filter.isVisible(request("/api/users"), 400));
        assertTrue(filter.isVisible(response("/api/users", 200), 400));
        assertTrue(filter.isVisible(response("/api/users", 503), 400));
        assertTrue(filter.isVisible(failure("/api/users"), 400));
        assertFalse(filter.showRequests());
        assertTrue(filter.showResponses());
        assertTrue(filter.showFailures());
        assertTrue(filter.showFailedResponses());
    }

    @Test
    void allShowsAndNoneHidesAllRawTypes() {
        for (NetworkEvent event : List.of(request("/one"), response("/one", 200), failure("/one"))) {
            assertTrue(NetworkHudFilter.all().isVisible(event, 400));
            assertFalse(NetworkHudFilter.none().isVisible(event, 400));
        }
    }

    @Test
    void includeRestrictsNormalTrafficButEnabledFailuresBypassIt() {
        NetworkHudFilter filter = NetworkHudFilter.builder()
                .showRequests(true)
                .includeUrlPattern("/api/.*")
                .build();

        assertTrue(filter.isVisible(request("https://example.test/api/orders"), 400));
        assertFalse(filter.isVisible(request("https://example.test/assets/app.js"), 400));
        assertFalse(filter.isVisible(response("https://example.test/assets/app.js", 200), 400));
        assertTrue(filter.isVisible(response("https://example.test/assets/app.js", 500), 400));
        assertTrue(filter.isVisible(failure("https://example.test/assets/app.js"), 400));
    }

    @Test
    void explicitExcludeWinsForFailuresAndFailedResponses() {
        NetworkHudFilter filter = NetworkHudFilter.builder()
                .excludeUrlPattern(".*/private(?:/.*)?")
                .build();

        assertFalse(filter.isVisible(failure("https://example.test/private/reset"), 400));
        assertFalse(filter.isVisible(response("https://example.test/private/error", 503), 400));
        assertTrue(filter.isVisible(response("https://example.test/public/error", 503), 400));
    }

    @Test
    void disabledFailureFlagsDoNotBypassInclude() {
        NetworkHudFilter filter = NetworkHudFilter.builder()
                .showFailures(false).showFailedResponses(false)
                .includeUrlPattern("/wanted$").build();

        assertFalse(filter.isVisible(failure("/other"), 400));
        assertFalse(filter.isVisible(response("/other", 503), 400));
        assertFalse(filter.isVisible(response("/wanted", 503), 400));
    }

    @Test
    void patternsAreValidatedImmutableAndDeterministicallyOrdered() {
        NetworkHudFilter filter = NetworkHudFilter.builder()
                .includeUrlPattern("one").includeUrlPattern("two")
                .excludeUrlPattern("three").excludeUrlPattern("four").build();

        assertEquals(List.of("one", "two"), filter.includeUrlPatterns());
        assertEquals(List.of("three", "four"), filter.excludeUrlPatterns());
        assertThrows(UnsupportedOperationException.class, () -> filter.includeUrlPatterns().add("five"));
        assertThrows(IllegalArgumentException.class, () -> NetworkHudFilter.builder().includeUrlPattern("["));
        assertThrows(IllegalArgumentException.class, () -> NetworkHudFilter.builder().excludeUrlPattern("*"));
    }

    @Test
    void nullAndBlankPatternsAreIgnoredAndPartialEventsAreSafe() {
        NetworkHudFilter filter = NetworkHudFilter.builder()
                .includeUrlPattern(null).includeUrlPattern(" ")
                .excludeUrlPattern(null).excludeUrlPattern("").build();

        assertTrue(filter.includeUrlPatterns().isEmpty());
        assertTrue(filter.excludeUrlPatterns().isEmpty());
        assertFalse(filter.isVisible(null, 400));
        assertTrue(filter.isVisible(NetworkEvent.response(NetworkResponse.of("", "", 200)), 400));
    }

    private static NetworkEvent request(String url) {
        return NetworkEvent.request(new NetworkRequest("id", "GET", url, "",
                Instant.EPOCH, Map.of()), Instant.EPOCH, Map.of());
    }

    private static NetworkEvent response(String url, int status) {
        return NetworkEvent.response(NetworkResponse.of("id", url, status), Instant.EPOCH, Map.of());
    }

    private static NetworkEvent failure(String url) {
        return NetworkEvent.failed(NetworkFailure.of("id", url, "reset"));
    }
}
