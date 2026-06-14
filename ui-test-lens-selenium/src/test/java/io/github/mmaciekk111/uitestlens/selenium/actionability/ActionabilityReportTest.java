package io.github.mmaciekk111.uitestlens.selenium.actionability;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionabilityReportTest {

    @Test
    void readyWhenAllChecksReadyOrSkipped() {
        ActionabilityReport report = ActionabilityReport.of(List.of(
                ActionabilityResult.ready(ActionabilityCheckType.VISIBLE, "visible", Duration.ZERO),
                ActionabilityResult.skipped(ActionabilityCheckType.OVERLAY_POLICY, "no policy", Duration.ZERO)
        ));

        assertTrue(report.isReady());
        assertTrue(report.firstFailure().isEmpty());
    }

    @Test
    void firstFailureReturnsFirstNotReadyOrFailedResult() {
        ActionabilityReport report = ActionabilityReport.of(List.of(
                ActionabilityResult.ready(ActionabilityCheckType.VISIBLE, "visible", Duration.ZERO),
                ActionabilityResult.notReady(
                        ActionabilityCheckType.ENABLED,
                        ActionabilityFailureReason.ELEMENT_NOT_ENABLED,
                        "disabled",
                        Duration.ZERO)
        ));

        assertFalse(report.isReady());
        assertEquals(ActionabilityFailureReason.ELEMENT_NOT_ENABLED, report.firstFailure().orElseThrow().failureReason());
        assertTrue(report.summary().contains("ELEMENT_NOT_ENABLED"));
    }
}
