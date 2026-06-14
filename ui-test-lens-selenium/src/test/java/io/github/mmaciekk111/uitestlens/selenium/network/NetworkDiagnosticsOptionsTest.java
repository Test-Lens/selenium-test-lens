package io.github.mmaciekk111.uitestlens.selenium.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkDiagnosticsOptionsTest {

    @Test
    void defaultsUseAutoWithoutHeaders() {
        NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.defaults();

        assertEquals(NetworkCaptureMode.AUTO, options.captureMode());
        assertFalse(options.includeHeaders());
        assertTrue(options.maskSensitiveHeaders());
        assertEquals(400, options.failedStatusThreshold());
        assertTrue(options.attachToSession());
    }

    @Test
    void builderSupportsIgnoredPatterns() {
        NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.MANUAL)
                .includeHeaders(true)
                .maskSensitiveHeaders(false)
                .failedStatusThreshold(500)
                .ignoreUrlPattern(".*analytics.*")
                .attachToSession(false)
                .build();

        assertEquals(NetworkCaptureMode.MANUAL, options.captureMode());
        assertTrue(options.includeHeaders());
        assertFalse(options.maskSensitiveHeaders());
        assertEquals(500, options.failedStatusThreshold());
        assertTrue(options.isIgnored("https://example.com/analytics/pixel"));
        assertFalse(options.attachToSession());
    }
}
