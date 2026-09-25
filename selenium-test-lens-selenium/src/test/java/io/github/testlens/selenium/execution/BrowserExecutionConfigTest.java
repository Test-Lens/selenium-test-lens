package io.github.testlens.selenium.execution;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserExecutionConfigTest {
    @Test
    void allUnsetResolvesUnsetAndInstancesAreIndependent() {
        BrowserExecutionConfig first = resolve(HeadlessMode.UNSET, Map.of(), Map.of());
        BrowserExecutionConfig second = resolve(HeadlessMode.UNSET, Map.of(), Map.of());
        assertEquals(HeadlessMode.UNSET, first.headless());
        assertNotSame(first, second);
    }

    @Test
    void explicitModesWinAndSkipInvalidLowerSources() {
        assertEquals(HeadlessMode.TRUE, resolve(HeadlessMode.TRUE,
                Map.of("testLens.headless", "ture"), Map.of("TEST_LENS_HEADLESS", "yes")).headless());
        assertEquals(HeadlessMode.FALSE, resolve(HeadlessMode.FALSE,
                Map.of("testLens.headless", "true"), Map.of("TEST_LENS_HEADLESS", "true")).headless());
    }

    @Test
    void propertyWinsOverEnvironmentAndSkipsInvalidEnvironment() {
        assertEquals(HeadlessMode.TRUE, resolve(HeadlessMode.UNSET,
                Map.of("testLens.headless", " TrUe "), Map.of("TEST_LENS_HEADLESS", "yes")).headless());
        assertEquals(HeadlessMode.FALSE, resolve(HeadlessMode.UNSET,
                Map.of("testLens.headless", "false"), Map.of("TEST_LENS_HEADLESS", "true")).headless());
    }

    @Test
    void environmentIsUsedOnlyWhenPropertyIsAbsent() {
        assertEquals(HeadlessMode.TRUE, resolve(HeadlessMode.UNSET,
                Map.of(), Map.of("TEST_LENS_HEADLESS", "TRUE")).headless());
        assertEquals(HeadlessMode.FALSE, resolve(HeadlessMode.UNSET,
                Map.of(), Map.of("TEST_LENS_HEADLESS", " false ")).headless());
    }

    @Test
    void invalidPropertyAndEnvironmentFailClearly() {
        IllegalArgumentException property = assertThrows(IllegalArgumentException.class,
                () -> resolve(HeadlessMode.UNSET, Map.of("testLens.headless", "ture"), Map.of()));
        assertTrue(property.getMessage().contains("system property testLens.headless"));
        IllegalArgumentException environment = assertThrows(IllegalArgumentException.class,
                () -> resolve(HeadlessMode.UNSET, Map.of(), Map.of("TEST_LENS_HEADLESS", "yes")));
        assertTrue(environment.getMessage().contains("environment variable TEST_LENS_HEADLESS"));
        assertThrows(IllegalArgumentException.class,
                () -> resolve(HeadlessMode.UNSET, Map.of("testLens.headless", " "), Map.of()));
    }

    @Test
    void nullExplicitModeIsRejected() {
        assertThrows(NullPointerException.class,
                () -> BrowserExecutionConfig.resolve(null, ignored -> null, ignored -> null));
    }

    private static BrowserExecutionConfig resolve(HeadlessMode explicit, Map<String, String> properties,
                                                    Map<String, String> environment) {
        Function<String, String> propertySource = properties::get;
        Function<String, String> environmentSource = environment::get;
        return BrowserExecutionConfig.resolve(explicit, propertySource, environmentSource);
    }
}
