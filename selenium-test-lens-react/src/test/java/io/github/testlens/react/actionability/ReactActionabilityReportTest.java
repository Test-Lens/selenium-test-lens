package io.github.testlens.react.actionability;

import io.github.testlens.selenium.actionability.ActionabilityCheckType;
import io.github.testlens.selenium.actionability.ActionabilityFailureReason;
import io.github.testlens.selenium.actionability.ActionabilityReport;
import io.github.testlens.selenium.actionability.ActionabilityResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactActionabilityReportTest {

    @Test
    void notReadyWhenBaseReportFails() {
        ActionabilityReport base = ActionabilityReport.of(List.of(ActionabilityResult.notReady(
                ActionabilityCheckType.VISIBLE,
                ActionabilityFailureReason.ELEMENT_NOT_VISIBLE,
                "hidden",
                Duration.ZERO
        )));

        ReactActionabilityReport report = ReactActionabilityReport.of(base, List.of());

        assertFalse(report.isReady());
        assertTrue(report.summary().contains("base actionability failed"));
    }

    @Test
    void firstReactFailureReturnsFirstReactFailure() {
        ReactActionabilityReport report = ReactActionabilityReport.of(null, List.of(
                ReactReadinessResult.ready(ReactReadinessCheckType.ARIA_BUSY, "ready", Duration.ZERO),
                ReactReadinessResult.notReady(
                        ReactReadinessCheckType.DATA_LOADING,
                        ReactReadinessFailureReason.DATA_LOADING_ACTIVE,
                        "loading",
                        Duration.ZERO)
        ));

        assertFalse(report.isReady());
        assertEquals(ReactReadinessFailureReason.DATA_LOADING_ACTIVE,
                report.firstReactFailure().orElseThrow().failureReason());
    }
}

