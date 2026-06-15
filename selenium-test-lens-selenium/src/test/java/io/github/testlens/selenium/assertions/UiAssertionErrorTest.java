package io.github.testlens.selenium.assertions;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiAssertionErrorTest {

    @Test
    void exposesAssertionResultAndSummary() {
        UiAssertionResult result = UiAssertionResult.timedOut(
                "toContainText",
                UiAssertionFailureReason.TEXT_MISMATCH,
                "Toast",
                "Saved",
                "Saving",
                3,
                Duration.ofMillis(100),
                "Element text did not contain expected substring");

        UiAssertionError error = new UiAssertionError(result);

        assertSame(result, error.result());
        assertTrue(error.getMessage().contains("toContainText TIMED_OUT"));
        assertTrue(error.getMessage().contains("TEXT_MISMATCH"));
    }
}
