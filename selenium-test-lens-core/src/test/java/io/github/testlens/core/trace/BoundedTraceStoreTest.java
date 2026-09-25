package io.github.testlens.core.trace;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedTraceStoreTest {
    @Test
    void countBoundRetainsNewestTailAndTerminalFailure() {
        UiTestLensSession session = session(5, 32_000, 4_000, PassedTraceRetention.RETAIN_TRACE);
        for (int i = 0; i < 12; i++) session.addEvent(TraceEvent.info("diagnostic-" + i, "value-" + i));
        session.finishFailed(new IllegalStateException("root cause"));

        assertTrue(session.events().size() <= 5);
        assertTrue(session.events().stream().anyMatch(event -> event.type() == TraceEventType.SESSION_FINISHED
                && event.status() == TraceStatus.FAILED));
        assertTrue(Long.parseLong(session.metadata().labels().get("testlens.retention.evictedEvents")) > 0);
        assertTrue(session.metadata().labels().get("testlens.retention.issues").contains("COUNT_EVICTION"));
    }

    @Test
    void byteBoundWorksBelowCountBoundaryAndUsesUtf8() {
        UiTestLensSession session = session(100, 1_300, 900, PassedTraceRetention.RETAIN_TRACE);
        String heart = "\u2764\uFE0F";
        String family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";
        String rocket = "\uD83D\uDE80";
        session.addEvent(TraceEvent.info("one", heart.repeat(80)));
        session.addEvent(TraceEvent.info("two", family.repeat(80)));
        session.addEvent(TraceEvent.info("three", rocket.repeat(80)));
        session.finishPassed();

        assertTrue(Long.parseLong(session.metadata().labels().get("testlens.retention.byteEvictions")) > 0);
        assertTrue(Long.parseLong(session.metadata().labels().get("testlens.retention.retainedEstimatedBytes")) <= 1_300);
    }

    @Test
    void oversizedEventIsRedactedThenTruncatedAtGraphemeBoundary() {
        RedactionPolicy redaction = RedactionPolicy.defaults();
        UiTestLensSession session = UiTestLensSession.start("oversized", RetryOutcomePolicy.REPORT_ONLY, 0,
                redaction, TraceRetentionOptions.builder().maxEvents(20).maxBytes(8_000).maxEventBytes(700).build());
        String family = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66";
        session.addEvent(TraceEvent.info("user", family.repeat(500) + " token=top-secret"));
        session.finishPassed();

        String retained = session.events().stream().map(TraceEvent::message).reduce("", String::concat);
        assertFalse(retained.contains("top-secret"));
        assertFalse(retained.endsWith("\u200D"));
        assertTrue(Long.parseLong(session.metadata().labels().get("testlens.retention.oversizedEvents")) > 0);
    }

    @Test
    void exactByteBoundaryRetainsTheEvent() {
        TraceEvent event = TraceEvent.info("exact", "żółw");
        long bytes = BoundedTraceStore.TraceSizeEstimator.eventBytes(event);
        BoundedTraceStore store = new BoundedTraceStore(TraceRetentionOptions.builder()
                .maxEvents(1).maxBytes(bytes).maxEventBytes(bytes).build());

        assertEquals(event, store.add(event));
        assertEquals(List.of(event), store.events());
    }

    @Test
    void completedOperationIsEvictedAsAGroup() {
        BoundedTraceStore store = new BoundedTraceStore(TraceRetentionOptions.builder()
                .maxEvents(3).maxBytes(20_000).maxEventBytes(4_000).build());
        store.add(operation("one", TraceStatus.STARTED));
        store.add(operation("one", TraceStatus.PASSED));
        store.add(operation("two", TraceStatus.STARTED));
        store.add(operation("two", TraceStatus.PASSED));

        long firstOperationEvents = store.events().stream()
                .filter(event -> "one".equals(event.attributes().get("metadata.operationId")))
                .count();
        assertEquals(0, firstOperationEvents);
    }

    @Test
    void localFailedActionDoesNotFreezeButFinalFailureDoes() {
        UiTestLensSession session = session(8, 32_000, 4_000, PassedTraceRetention.RETAIN_TRACE);
        session.addEvent(TraceEvent.failed(TraceEventType.ACTION_FAILED, "caught", new RuntimeException("caught"),
                java.time.Duration.ZERO));
        for (int i = 0; i < 12; i++) session.addEvent(TraceEvent.info("after-caught-" + i, "diagnostic"));
        assertEquals(TraceStatus.STARTED, session.metadata().status());
        session.finishFailed(new AssertionError("uncaught"));
        assertEquals(TraceStatus.FAILED, session.metadata().status());
    }

    @Test
    void summaryOnlyReleasesDetailedPassedTail() {
        UiTestLensSession session = session(50, 32_000, 4_000, PassedTraceRetention.SUMMARY_ONLY);
        session.addEvent(TraceEvent.info("detail", "discard after summary"));
        session.finishPassed();
        List<TraceEventType> types = session.events().stream().map(TraceEvent::type).toList();
        assertFalse(types.contains(TraceEventType.CUSTOM));
        assertTrue(types.contains(TraceEventType.SESSION_STARTED));
        assertTrue(types.contains(TraceEventType.SESSION_FINISHED));
    }

    @Test
    void skippedSessionRetainsItsBoundedDiagnosticTail() {
        UiTestLensSession session = session(10, 32_000, 4_000, PassedTraceRetention.RETAIN_TRACE);
        session.addEvent(TraceEvent.info("setup", "useful skip context"));
        session.finishSkipped("dependency unavailable");

        assertEquals(TraceStatus.SKIPPED, session.metadata().status());
        assertTrue(session.events().stream().anyMatch(event -> event.message().contains("useful skip context")));
        assertTrue(session.events().stream().anyMatch(event -> event.type() == TraceEventType.SESSION_FINISHED));
    }

    @Test
    void appendAfterClosedIsDroppedAndNeverReopensSnapshot() {
        UiTestLensSession session = session(20, 32_000, 4_000, PassedTraceRetention.RETAIN_TRACE);
        session.finishPassed();
        List<TraceEvent> snapshot = session.events();
        assertNull(session.addEvent(TraceEvent.info("late", "must not appear")));
        assertEquals(snapshot, session.events());
        assertFalse(session.exportJson().contains("must not appear"));
    }

    @Test
    void jsonAndHtmlDescribeTheSameImmutablePartialSnapshot() {
        UiTestLensSession session = session(4, 8_000, 2_000, PassedTraceRetention.RETAIN_TRACE);
        for (int i = 0; i < 10; i++) session.addEvent(TraceEvent.info("event-" + i, "diagnostic"));
        session.finishPassed();

        String json = session.exportJson();
        String html = session.exportHtml();
        assertTrue(json.contains("\"retention\""));
        assertTrue(json.contains("\"complete\": false") || json.contains("\"complete\":false"));
        assertTrue(json.contains("COUNT_EVICTION"));
        assertTrue(html.contains("Partial trace"));
        assertTrue(html.contains(session.metadata().labels().get("testlens.retention.evictedEvents")));
    }

    @Test
    void deterministicEstimatorDoesNotDependOnHistory() {
        TraceEvent event = TraceEvent.info("name", "żółw ❤️");
        long first = BoundedTraceStore.TraceSizeEstimator.eventBytes(event);
        long second = BoundedTraceStore.TraceSizeEstimator.eventBytes(event);
        assertEquals(first, second);
        assertTrue(first > "name".length() + "żółw ❤️".length());
    }

    private static UiTestLensSession session(int maxEvents, long maxBytes, long maxEventBytes,
                                             PassedTraceRetention passed) {
        return UiTestLensSession.start("bounded", RetryOutcomePolicy.REPORT_ONLY, 0, RedactionPolicy.defaults(),
                TraceRetentionOptions.builder().maxEvents(maxEvents).maxBytes(maxBytes)
                        .maxEventBytes(maxEventBytes).passedSessionRetention(passed).build());
    }

    private static TraceEvent operation(String operationId, TraceStatus status) {
        TraceEventType type = status == TraceStatus.STARTED
                ? TraceEventType.ACTION_STARTED : TraceEventType.ACTION_PASSED;
        return TraceEvent.builder(type, status, operationId)
                .attribute("metadata.operationId", operationId)
                .build();
    }
}
