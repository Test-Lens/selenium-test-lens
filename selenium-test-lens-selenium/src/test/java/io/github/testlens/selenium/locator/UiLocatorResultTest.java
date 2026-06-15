package io.github.testlens.selenium.locator;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLocatorResultTest {

    @Test
    void passedResultReportsPassed() {
        UiLocatorResult result = UiLocatorResult.builder(UiLocatorStatus.PASSED)
                .action("click")
                .description("Save")
                .attempts(1)
                .elapsed(Duration.ofMillis(5))
                .build();

        assertTrue(result.passed());
        assertEquals("click", result.action());
        assertEquals(1, result.attempts());
    }

    @Test
    void failedResultKeepsReason() {
        UiLocatorResult result = UiLocatorResult.builder(UiLocatorStatus.FAILED)
                .failureReason(UiLocatorFailureReason.TIMEOUT)
                .message("timeout")
                .build();

        assertFalse(result.passed());
        assertEquals(UiLocatorFailureReason.TIMEOUT, result.failureReason());
    }
}
