package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("removal")
class NetworkDiagnosticsOptionsTest {

    @Test
    void defaultsUseManualWithoutHeaders() {
        NetworkDiagnosticsOptions options = NetworkDiagnosticsOptions.defaults();

        assertEquals(NetworkCaptureMode.MANUAL, options.captureMode());
        assertFalse(options.includeHeaders());
        assertTrue(options.maskSensitiveHeaders());
        assertEquals(400, options.failedStatusThreshold());
        assertEquals(10_000, options.maxCapturedEvents());
        assertTrue(options.attachToSession());
    }

    @Test
    void attachToSessionOptionIsDeprecatedForRemovalWithoutAutomaticSemantics() throws Exception {
        Method accessor = NetworkDiagnosticsOptions.class.getMethod("attachToSession");
        Method builderMethod = NetworkDiagnosticsOptions.Builder.class.getMethod("attachToSession", boolean.class);

        Deprecated accessorDeprecated = accessor.getAnnotation(Deprecated.class);
        Deprecated builderDeprecated = builderMethod.getAnnotation(Deprecated.class);
        assertTrue(accessorDeprecated.forRemoval());
        assertEquals("0.1.1", accessorDeprecated.since());
        assertTrue(builderDeprecated.forRemoval());
        assertEquals("0.1.1", builderDeprecated.since());
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
                .attachToSession(false)
                .build();

        assertEquals(NetworkCaptureMode.MANUAL, options.captureMode());
        assertTrue(options.includeHeaders());
        assertFalse(options.maskSensitiveHeaders());
        assertEquals(500, options.failedStatusThreshold());
        assertEquals(37, options.maxCapturedEvents());
        assertTrue(options.isIgnored("https://example.com/analytics/pixel"));
        assertFalse(options.attachToSession());
    }

    @Test
    void eventLimitMustBePositive() {
        assertThrows(IllegalArgumentException.class,
                () -> NetworkDiagnosticsOptions.builder().maxCapturedEvents(0));
        assertThrows(IllegalArgumentException.class,
                () -> NetworkDiagnosticsOptions.builder().maxCapturedEvents(-1));
    }
}

