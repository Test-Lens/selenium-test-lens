package io.github.testlens.react.actionability;

import io.github.testlens.selenium.actionability.ActionabilityStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactReadinessResultTest {

    @Test
    void detailsAreImmutable() {
        ReactReadinessResult result = ReactReadinessResult.builder(
                        ReactReadinessCheckType.SPINNER_PRESENT,
                        ActionabilityStatus.NOT_READY)
                .failureReason(ReactReadinessFailureReason.SPINNER_BLOCKING)
                .message("spinner")
                .details(Map.of("blocker", "div.spinner"))
                .build();

        assertEquals(ReactReadinessFailureReason.SPINNER_BLOCKING, result.failureReason());
        assertEquals("div.spinner", result.details().get("blocker"));
        assertThrows(UnsupportedOperationException.class, () -> result.details().put("x", "y"));
    }

    @Test
    void readyFactoryBuildsReadyResult() {
        ReactReadinessResult result = ReactReadinessResult.ready(
                ReactReadinessCheckType.ARIA_BUSY,
                "ready",
                Duration.ZERO);

        assertTrue(result.ready());
        assertEquals(ActionabilityStatus.READY, result.status());
    }
}
