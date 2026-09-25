package io.github.testlens;

import io.github.testlens.core.trace.PassedTraceRetention;
import io.github.testlens.core.trace.TraceRetentionOptions;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.SourceNavigationOptions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservabilityModeResolverTest {
    @Test
    void resolvesDefaultAndIndependentInstances() {
        TestLensOptions first = options(null, Map.of(), Map.of());
        TestLensOptions second = options(null, Map.of(), Map.of());
        assertEquals(ObservabilityMode.DEFAULT, first.observabilityMode());
        assertNotSame(first, second);
    }

    @Test
    void explicitJavaValueWinsAndSkipsInvalidLowerSources() {
        assertEquals(ObservabilityMode.DEFAULT, options(ObservabilityMode.DEFAULT,
                Map.of(ObservabilityModeResolver.PROPERTY, "banana"),
                Map.of(ObservabilityModeResolver.ENVIRONMENT, "banana")).observabilityMode());
        assertEquals(ObservabilityMode.FAST, options(ObservabilityMode.FAST,
                Map.of(ObservabilityModeResolver.PROPERTY, "banana"), Map.of()).observabilityMode());
    }

    @Test
    void propertyPrecedesEnvironmentAndEnvironmentIsFallback() {
        assertEquals(ObservabilityMode.FAST, resolve(null,
                Map.of(ObservabilityModeResolver.PROPERTY, " FaSt "),
                Map.of(ObservabilityModeResolver.ENVIRONMENT, "banana")));
        assertEquals(ObservabilityMode.DEFAULT, resolve(null,
                Map.of(ObservabilityModeResolver.PROPERTY, "DEFAULT"),
                Map.of(ObservabilityModeResolver.ENVIRONMENT, "fast")));
        assertEquals(ObservabilityMode.FAST, resolve(null, Map.of(),
                Map.of(ObservabilityModeResolver.ENVIRONMENT, " FAST ")));
    }

    @Test
    void invalidEffectiveSourceFailsClearly() {
        IllegalArgumentException property = assertThrows(IllegalArgumentException.class,
                () -> resolve(null, Map.of(ObservabilityModeResolver.PROPERTY, ""), Map.of()));
        assertTrue(property.getMessage().contains(ObservabilityModeResolver.PROPERTY));
        IllegalArgumentException environment = assertThrows(IllegalArgumentException.class,
                () -> resolve(null, Map.of(), Map.of(ObservabilityModeResolver.ENVIRONMENT, "quick")));
        assertTrue(environment.getMessage().contains(ObservabilityModeResolver.ENVIRONMENT));
    }

    @Test
    void fastDefaultsArePresentationOnlyAndExplicitOverridesWin() {
        TestLensOptions defaults = TestLensOptions.builder().observabilityMode(ObservabilityMode.FAST).build();
        assertFalse(defaults.effectiveObservability().liveHud());
        assertFalse(defaults.effectiveObservability().automaticFeedback());
        assertFalse(defaults.effectiveObservability().liveSourceNavigation());
        assertEquals(PassedTraceRetention.SUMMARY_ONLY,
                defaults.effectiveObservability().traceRetention().passedSessionRetention());
        assertEquals(PassedTraceRetention.RETAIN_TRACE,
                defaults.traceRetention().passedSessionRetention(), "requested options remain unchanged");

        TraceRetentionOptions retained = TraceRetentionOptions.builder()
                .passedSessionRetention(PassedTraceRetention.RETAIN_TRACE).build();
        OverlayConfig explicit = OverlayConfig.builder()
                .showHudPanel(true)
                .highlightOptions(HighlightOptions.builder().automaticFeedback(true).build())
                .hudOptions(HudOptions.builder().sourceNavigation(
                        SourceNavigationOptions.builder().enabled(true).build()).build())
                .build();
        TestLensOptions overridden = TestLensOptions.builder()
                .observabilityMode(ObservabilityMode.FAST)
                .overlayConfig(explicit)
                .traceRetention(retained)
                .build();
        assertTrue(overridden.effectiveObservability().liveHud());
        assertTrue(overridden.effectiveObservability().automaticFeedback());
        assertTrue(overridden.effectiveObservability().liveSourceNavigation());
        assertEquals(PassedTraceRetention.RETAIN_TRACE,
                overridden.effectiveObservability().traceRetention().passedSessionRetention());
    }

    private static TestLensOptions options(ObservabilityMode explicit, Map<String, String> properties,
                                           Map<String, String> environment) {
        ObservabilityMode mode = ObservabilityModeResolver.resolve(explicit, properties::get, environment::get);
        return TestLensOptions.builder().observabilityMode(mode).build();
    }

    private static ObservabilityMode resolve(ObservabilityMode explicit, Map<String, String> properties,
                                             Map<String, String> environment) {
        return ObservabilityModeResolver.resolve(explicit, properties::get, environment::get);
    }
}
