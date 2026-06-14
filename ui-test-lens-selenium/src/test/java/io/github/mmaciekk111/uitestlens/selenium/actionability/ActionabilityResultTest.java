package io.github.mmaciekk111.uitestlens.selenium.actionability;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionabilityResultTest {

    @Test
    void resultDetailsAreImmutable() {
        ActionabilityResult result = ActionabilityResult.builder(
                        ActionabilityCheckType.RECEIVES_CLICK_POINT,
                        ActionabilityStatus.NOT_READY)
                .failureReason(ActionabilityFailureReason.ELEMENT_COVERED)
                .message("covered")
                .elapsed(Duration.ofMillis(5))
                .details(Map.of("topElement", "div.modal"))
                .build();

        assertEquals(ActionabilityStatus.NOT_READY, result.status());
        assertEquals(ActionabilityFailureReason.ELEMENT_COVERED, result.failureReason());
        assertEquals("div.modal", result.details().get("topElement"));
        assertThrows(UnsupportedOperationException.class, () -> result.details().put("x", "y"));
    }

    @Test
    void readyFactoryBuildsReadyResult() {
        ActionabilityResult result = ActionabilityResult.ready(
                ActionabilityCheckType.VISIBLE,
                "visible",
                Duration.ZERO);

        assertTrue(result.ready());
        assertEquals(ActionabilityStatus.READY, result.status());
    }
}
