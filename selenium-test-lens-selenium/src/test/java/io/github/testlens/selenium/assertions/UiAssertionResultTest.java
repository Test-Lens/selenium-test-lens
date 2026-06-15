package io.github.testlens.selenium.assertions;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiAssertionResultTest {

    @Test
    void passedFactoryCreatesPassedResult() {
        UiAssertionResult result = UiAssertionResult.passed(
                "toHaveText", "Toast", "Saved", "Saved", 2, Duration.ofMillis(25), "matched");

        assertTrue(result.isPassed());
        assertEquals(UiAssertionStatus.PASSED, result.status());
        assertNull(result.failureReason());
        assertEquals(2, result.attempts());
        assertTrue(result.summary().contains("toHaveText PASSED"));
    }

    @Test
    void timedOutFactoryAddsTimeoutStatusAndSummary() {
        UiAssertionResult result = UiAssertionResult.timedOut(
                "toBeVisible",
                UiAssertionFailureReason.ELEMENT_NOT_VISIBLE,
                "By.id: toast",
                "",
                "hidden",
                4,
                Duration.ofMillis(300),
                "Element is not visible");

        assertEquals(UiAssertionStatus.TIMED_OUT, result.status());
        assertEquals(UiAssertionFailureReason.ELEMENT_NOT_VISIBLE, result.failureReason());
        assertTrue(result.summary().contains("attempts=4"));
        assertTrue(result.summary().contains("elapsedMs=300"));
    }
}
