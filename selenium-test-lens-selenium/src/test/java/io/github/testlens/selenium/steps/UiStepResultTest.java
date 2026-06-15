package io.github.testlens.selenium.steps;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiStepResultTest {

    @Test
    void passedResultContainsTiming() {
        Instant started = Instant.parse("2026-06-14T10:00:00Z");
        Instant ended = Instant.parse("2026-06-14T10:00:01Z");

        UiStepResult result = UiStepResult.passed("Verify order summary", started, ended);

        assertTrue(result.isPassed());
        assertEquals(UiStepStatus.PASSED, result.status());
        assertEquals(1000, result.elapsed().toMillis());
        assertNull(result.failure());
        assertTrue(result.summary().contains("Verify order summary"));
    }

    @Test
    void failedResultContainsFailure() {
        UiStepFailure failure = UiStepFailure.from(new IllegalStateException("bad state"), UiStepOptions.defaults());

        UiStepResult result = UiStepResult.failed("Save order", Instant.now(), Instant.now(), failure);

        assertFalse(result.isPassed());
        assertEquals(UiStepStatus.FAILED, result.status());
        assertEquals("bad state", result.failure().message());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> UiStepResult.passed(" ", Instant.now(), Instant.now()));
    }
}

