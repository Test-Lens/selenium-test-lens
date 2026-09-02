package io.github.testlens.core.trace;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetrySummaryTest {
    @Test
    void zeroRetriesIsNeutralAndReportOnlyByDefault() {
        RetrySummary summary = UiTestLensSession.start("zero").retrySummary();
        assertAll(
                () -> assertEquals(0, summary.totalRetries()),
                () -> assertEquals(Duration.ZERO, summary.timeLost()),
                () -> assertFalse(summary.flakyCandidate()),
                () -> assertEquals(RetryOutcomePolicy.REPORT_ONLY, summary.policy()));
    }

    @Test
    void aggregatesTimeAndDeterministicallyGroupsRetries() {
        UiTestLensSession session = UiTestLensSession.start("aggregate", RetryOutcomePolicy.WARN, 0);
        session.addEvent(retry("fill", "z locator", "ZException", 1, 2, 12));
        session.addEvent(retry("click", "a locator", "AException", 2, 3, 25));
        session.addEvent(retry("click", "a locator", "AException", 3, 4, 0));

        RetrySummary summary = session.retrySummary();

        assertEquals(3, summary.totalRetries());
        assertEquals(Duration.ofMillis(37), summary.timeLost());
        assertTrue(summary.flakyCandidate());
        assertEquals(List.of("click", "fill"), new ArrayList<>(summary.byAction().keySet()));
        assertEquals(Map.of("a locator", 2L, "z locator", 1L), summary.byLocator());
        assertEquals(List.of("AException", "ZException"), new ArrayList<>(summary.byException().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> summary.byAction().put("x", 1L));
    }

    @Test
    void failOnAnyRetryCreatesViolationAndFailedSession() {
        UiTestLensSession session = UiTestLensSession.start("fail any", RetryOutcomePolicy.FAIL_ON_ANY_RETRY, 0);
        session.addEvent(retry("click", "save", "Stale", 1, 2, 4));

        RetryPolicyViolationException failure = assertThrows(RetryPolicyViolationException.class, session::finishPassed);
        assertEquals(TraceStatus.FAILED, session.metadata().status());
        assertEquals(session.retrySummary(), failure.retrySummary());
        assertTrue(failure.retrySummary().policyTriggered());
        assertTrue(failure.getMessage().contains("retries=1"));
        assertDecisionBeforeFinished(session);
    }

    @Test
    void failAfterNAllowsBoundaryAndFailsOnlyAboveIt() {
        UiTestLensSession boundary = UiTestLensSession.start("boundary", RetryOutcomePolicy.FAIL_AFTER_N, 1);
        boundary.addEvent(retry("click", "save", "Stale", 1, 2, 1));
        boundary.finishPassed();
        assertEquals(TraceStatus.PASSED, boundary.metadata().status());

        UiTestLensSession above = UiTestLensSession.start("above", RetryOutcomePolicy.FAIL_AFTER_N, 1);
        above.addEvent(retry("click", "save", "Stale", 1, 2, 1));
        above.addEvent(retry("click", "save", "Stale", 2, 3, 1));
        assertThrows(RetryPolicyViolationException.class, above::finishPassed);
        assertEquals(TraceStatus.FAILED, above.metadata().status());
        assertTrue(above.retrySummary().policyTriggered());
    }

    @Test
    void policyNeverOverridesExplicitFailureOrSkip() {
        UiTestLensSession failed = UiTestLensSession.start("failed", RetryOutcomePolicy.FAIL_ON_ANY_RETRY, 0);
        failed.addEvent(retry("click", "save", "Stale", 1, 2, 1));
        RuntimeException original = new RuntimeException("original");
        failed.finishFailed(original);
        assertEquals(TraceStatus.FAILED, failed.metadata().status());
        assertFalse(failed.retrySummary().policyTriggered());
        assertEquals(original.getClass().getName(), failed.events().get(failed.events().size() - 1).failure().exceptionType());

        UiTestLensSession skipped = UiTestLensSession.start("skipped", RetryOutcomePolicy.FAIL_ON_ANY_RETRY, 0);
        skipped.addEvent(retry("click", "save", "Stale", 1, 2, 1));
        skipped.finishSkipped("not applicable");
        assertEquals(TraceStatus.SKIPPED, skipped.metadata().status());
        assertFalse(skipped.retrySummary().policyTriggered());
    }

    @Test
    void negativeAllowedRetriesIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> UiTestLensSession.start("bad", RetryOutcomePolicy.FAIL_AFTER_N, -1));
    }

    private static TraceEvent retry(String action, String locator, String exception,
                                    int attempt, int nextAttempt, long milliseconds) {
        return TraceEvent.builder(TraceEventType.RETRY, TraceStatus.WARNING, "retry")
                .duration(Duration.ofMillis(milliseconds))
                .attribute("retry.action", action)
                .attribute("retry.locator", locator)
                .attribute("retry.exceptionType", exception)
                .attribute("retry.attempt", String.valueOf(attempt))
                .attribute("retry.nextAttempt", String.valueOf(nextAttempt))
                .build();
    }

    private static void assertDecisionBeforeFinished(UiTestLensSession session) {
        List<TraceEventType> types = session.events().stream().map(TraceEvent::type).toList();
        assertTrue(types.indexOf(TraceEventType.RETRY_SUMMARY) < types.indexOf(TraceEventType.SESSION_FINISHED));
    }
}
