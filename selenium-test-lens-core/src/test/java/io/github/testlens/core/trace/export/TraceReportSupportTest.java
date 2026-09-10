package io.github.testlens.core.trace.export;

import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TraceReportSupportTest {
    @Test
    void suiteStatusPreservesIncompleteSessionsWithDocumentedPrecedence() {
        UiTestLensSession started = started("started");
        UiTestLensSession passed = passed("passed");
        UiTestLensSession skipped = skipped("skipped");
        UiTestLensSession warning = warning("warning", true);
        UiTestLensSession startedWarning = warning("started warning", false);
        UiTestLensSession failed = failed("failed");
        UiTestLensSession startedFailure = startedWithEvent("started failure", TraceStatus.FAILED);
        UiTestLensSession startedError = startedWithEvent("started error", TraceStatus.ERROR);

        List<StatusCase> cases = List.of(
                new StatusCase("empty", List.of(), TraceStatus.INFO),
                new StatusCase("started", List.of(started), TraceStatus.STARTED),
                new StatusCase("started and passed", List.of(started, passed), TraceStatus.STARTED),
                new StatusCase("started and skipped", List.of(started, skipped), TraceStatus.STARTED),
                new StatusCase("passed", List.of(passed), TraceStatus.PASSED),
                new StatusCase("passed and skipped", List.of(passed, skipped), TraceStatus.PASSED),
                new StatusCase("only skipped", List.of(skipped, skipped("another skipped")), TraceStatus.SKIPPED),
                new StatusCase("completed warning", List.of(warning), TraceStatus.WARNING),
                new StatusCase("started warning", List.of(startedWarning), TraceStatus.WARNING),
                new StatusCase("failed and started", List.of(failed, started), TraceStatus.FAILED),
                new StatusCase("started failed event", List.of(startedFailure), TraceStatus.FAILED),
                new StatusCase("started error event", List.of(startedError), TraceStatus.FAILED));

        assertAll(cases.stream().map(testCase -> () -> assertEquals(
                testCase.expected(), TraceReportSupport.suiteStatus(testCase.sessions()), testCase.name())));
    }

    @Test
    void nullSessionsAreIgnoredAndInputOrderDoesNotChangeStatus() {
        UiTestLensSession started = started("started");
        UiTestLensSession passed = passed("passed");
        List<UiTestLensSession> withNull = Arrays.asList(null, passed, started, null);
        List<UiTestLensSession> reversed = new ArrayList<>(withNull);
        Collections.reverse(reversed);

        assertEquals(TraceStatus.STARTED, TraceReportSupport.suiteStatus(withNull));
        assertEquals(TraceReportSupport.suiteStatus(withNull), TraceReportSupport.suiteStatus(reversed));
        assertEquals(List.of(passed, started), TraceReportSupport.safeSessions(withNull));
    }

    private static UiTestLensSession started(String name) {
        return UiTestLensSession.start(name);
    }

    private static UiTestLensSession passed(String name) {
        UiTestLensSession session = started(name);
        session.finishPassed();
        return session;
    }

    private static UiTestLensSession skipped(String name) {
        UiTestLensSession session = started(name);
        session.finishSkipped("not applicable");
        return session;
    }

    private static UiTestLensSession failed(String name) {
        UiTestLensSession session = started(name);
        session.finishFailed(new AssertionError("failure"));
        return session;
    }

    private static UiTestLensSession warning(String name, boolean finish) {
        UiTestLensSession session = startedWithEvent(name, TraceStatus.WARNING);
        if (finish) session.finishPassed();
        return session;
    }

    private static UiTestLensSession startedWithEvent(String name, TraceStatus status) {
        UiTestLensSession session = started(name);
        session.addEvent(TraceEvent.builder(TraceEventType.ACTION_FAILED, status, "diagnostic").build());
        return session;
    }

    private record StatusCase(String name, List<UiTestLensSession> sessions, TraceStatus expected) { }
}
