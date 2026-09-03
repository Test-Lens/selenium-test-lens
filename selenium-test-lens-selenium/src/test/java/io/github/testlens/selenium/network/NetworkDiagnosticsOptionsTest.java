package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkDiagnosticsOptionsTest {

    @Test
    void defaultsUseManualWithoutHeaders() {
        NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.defaults();

        assertEquals(NetworkCaptureMode.MANUAL, options.captureMode());
        assertFalse(options.includeHeaders());
        assertTrue(options.maskSensitiveHeaders());
        assertEquals(400, options.failedStatusThreshold());
        assertEquals(10_000, options.maxCapturedEvents());
        assertFalse(options.hudFilter().showRequests());
        assertTrue(options.hudFilter().showResponses());
    }

    @Test
    void builderSupportsIgnoredPatterns() {
        NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.MANUAL)
                .includeHeaders(true)
                .maskSensitiveHeaders(false)
                .failedStatusThreshold(500)
                .maxCapturedEvents(37)
                .ignoreUrlPattern(".*analytics.*")
                .build();

        assertEquals(NetworkCaptureMode.MANUAL, options.captureMode());
        assertTrue(options.includeHeaders());
        assertFalse(options.maskSensitiveHeaders());
        assertEquals(500, options.failedStatusThreshold());
        assertEquals(37, options.maxCapturedEvents());
        assertTrue(options.isIgnored("https://example.com/analytics/pixel"));
    }

    @Test
    void eventLimitMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> NetworkDiagnosticsOptions.builder().maxCapturedEvents(0));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkDiagnosticsOptions.builder().maxCapturedEvents(-1));
    }

    @Test
    void customHudFilterIsRetainedAndNullRestoresDefaults() {
        NetworkHudFilter none = NetworkHudFilter.none();
        assertSame(none, NetworkDiagnosticsOptions.builder().hudFilter(none).build().hudFilter());

        NetworkHudFilter normalized = NetworkDiagnosticsOptions.builder().hudFilter(null).build().hudFilter();
        assertFalse(normalized.showRequests());
        assertTrue(normalized.showResponses());
        assertTrue(normalized.showFailures());
    }
}

