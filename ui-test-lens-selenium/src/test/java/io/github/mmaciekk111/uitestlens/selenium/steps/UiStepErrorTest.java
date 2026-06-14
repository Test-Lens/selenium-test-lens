package io.github.mmaciekk111.uitestlens.selenium.steps;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiStepErrorTest {

    @Test
    void summaryContainsStepNameCauseAndElapsed() {
        UiStepFailure failure = UiStepFailure.from(new AssertionError("business failure"), UiStepOptions.defaults());
        UiStepResult result = UiStepResult.failed("Verify order summary", Instant.now(), Instant.now(), failure);

        UiStepError error = new UiStepError(result);

        assertSame(result, error.result());
        assertTrue(error.getMessage().contains("Step failed: Verify order summary"));
        assertTrue(error.getMessage().contains("Cause: business failure"));
        assertTrue(error.getMessage().contains("Elapsed:"));
    }
}
