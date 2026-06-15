package io.github.testlens.react.actionability;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactActionabilityOptionsTest {

    @Test
    void defaultsEnableReactReadinessChecks() {
        ReactActionabilityOptions options = ReactActionabilityOptions.defaults();

        assertTrue(options.checkAriaDisabled());
        assertTrue(options.checkAriaBusy());
        assertTrue(options.checkDataLoading());
        assertTrue(options.checkDataPending());
        assertTrue(options.checkProgressbar());
        assertTrue(options.checkSpinner());
        assertTrue(options.checkSkeleton());
        assertTrue(options.checkFocusLock());
        assertTrue(options.checkDialogOrModal());
        assertEquals(Duration.ofSeconds(3), options.timeout());
        assertEquals(Duration.ofMillis(100), options.pollInterval());
        assertTrue(options.customBusyIndicators().isEmpty());
        assertTrue(options.customBlockingOverlays().isEmpty());
    }

    @Test
    void customLocatorsAreImmutable() {
        ReactActionabilityOptions options = ReactActionabilityOptions.builder()
                .customBusyIndicator(By.cssSelector(".spinner"))
                .customBlockingOverlay(By.cssSelector(".modal"))
                .build();

        assertEquals(1, options.customBusyIndicators().size());
        assertThrows(UnsupportedOperationException.class,
                () -> options.customBusyIndicators().add(By.cssSelector(".other")));
    }

    @Test
    void validatesTiming() {
        assertThrows(IllegalArgumentException.class, () -> ReactActionabilityOptions.builder()
                .timeout(Duration.ZERO)
                .build());
        assertThrows(IllegalArgumentException.class, () -> ReactActionabilityOptions.builder()
                .pollInterval(Duration.ZERO)
                .build());
    }
}
