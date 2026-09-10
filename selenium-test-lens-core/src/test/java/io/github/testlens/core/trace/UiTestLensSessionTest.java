package io.github.testlens.core.trace;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class UiTestLensSessionTest {

    @Test
    void startCreatesMetadataAndStartEvent() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        assertFalse(session.id().isBlank());
        assertEquals("Checkout flow", session.metadata().name());
        assertEquals(TraceStatus.STARTED, session.metadata().status());
        assertEquals(TraceEventType.SESSION_STARTED, session.events().get(0).type());
    }

    @Test
    void addEventAndAttachArtifacts() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        session.addEvent(TraceEvent.info("note", "hello"));
        TraceArtifact screenshot = session.attachScreenshot("Save form", Path.of("target/screenshots/save.png"));
        TraceArtifact video = session.attachVideo("Video", Path.of("target/videos/test.mp4"));

        assertEquals(TraceArtifactType.SCREENSHOT, screenshot.type());
        assertEquals(TraceArtifactType.VIDEO, video.type());
        assertEquals(2, session.artifacts().size());
    }

    @Test
    void finishUpdatesStatus() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        session.finishPassed();

        assertEquals(TraceStatus.PASSED, session.metadata().status());
        assertEquals(TraceEventType.SESSION_FINISHED, session.events().get(session.events().size() - 1).type());
    }

    @Test
    void firstTerminalOutcomeCannotBeOverwritten() {
        RuntimeException original = new RuntimeException("original failure");
        UiTestLensSession failed = UiTestLensSession.start("failed");
        failed.finishFailed(original);
        Instant failedAt = failed.metadata().finishedAt();

        failed.finishPassed();
        failed.finishSkipped("late skip");
        failed.finishFailed(new IllegalStateException("late failure"));

        assertTerminal(failed, TraceStatus.FAILED, failedAt);
        TraceEvent failedEvent = terminalEvents(failed).get(0);
        assertEquals(original.getClass().getName(), failedEvent.failure().exceptionType());
        assertEquals(original.getMessage(), failedEvent.failure().message());

        UiTestLensSession passed = UiTestLensSession.start("passed");
        passed.finishPassed();
        Instant passedAt = passed.metadata().finishedAt();
        passed.finishFailed(original);
        passed.finishSkipped("late skip");
        assertTerminal(passed, TraceStatus.PASSED, passedAt);

        UiTestLensSession skipped = UiTestLensSession.start("skipped");
        skipped.finishSkipped("original reason");
        Instant skippedAt = skipped.metadata().finishedAt();
        skipped.finishFailed(original);
        skipped.finishPassed();
        assertTerminal(skipped, TraceStatus.SKIPPED, skippedAt);
        assertEquals("original reason", terminalEvents(skipped).get(0).message());
    }

    @Test
    void finishFailedWithNullIsTerminalAndIdempotent() {
        UiTestLensSession session = UiTestLensSession.start("null failure");

        session.finishFailed(null);
        Instant finishedAt = session.metadata().finishedAt();
        session.finishPassed();

        assertTerminal(session, TraceStatus.FAILED, finishedAt);
        assertEquals(null, terminalEvents(session).get(0).failure());
    }

    @Test
    void concurrentFinalizersProduceOneTerminalDecision() throws Exception {
        UiTestLensSession session = UiTestLensSession.start("concurrent");
        CountDownLatch ready = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            List<Future<?>> futures = List.of(
                    executor.submit(() -> finishWhenReleased(ready, release, session::finishPassed)),
                    executor.submit(() -> finishWhenReleased(ready, release,
                            () -> session.finishFailed(new AssertionError("failure")))),
                    executor.submit(() -> finishWhenReleased(ready, release,
                            () -> session.finishSkipped("skip"))));
            ready.await();
            release.countDown();
            for (Future<?> future : futures) future.get();
        } finally {
            executor.shutdownNow();
        }

        assertTrue(List.of(TraceStatus.PASSED, TraceStatus.FAILED, TraceStatus.SKIPPED)
                .contains(session.metadata().status()));
        assertEquals(1, terminalEvents(session).size());
        assertEquals(1, retrySummaryEvents(session).size());
    }

    @Test
    void retryPolicyIsEvaluatedOnlyByTheFirstSuccessfulFinalizationAttempt() {
        UiTestLensSession session = UiTestLensSession.start(
                "policy", RetryOutcomePolicy.FAIL_ON_ANY_RETRY, 0);
        session.addEvent(TraceEvent.builder(TraceEventType.RETRY, TraceStatus.WARNING, "retry").build());

        RetryPolicyViolationException violation = assertThrows(
                RetryPolicyViolationException.class, session::finishPassed);
        Instant finishedAt = session.metadata().finishedAt();

        session.finishPassed();
        session.finishFailed(new AssertionError("late"));

        assertEquals(1, violation.retrySummary().totalRetries());
        assertTerminal(session, TraceStatus.FAILED, finishedAt);
    }

    @Test
    void attachNullArtifactIsRejected() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");

        assertThrows(IllegalArgumentException.class, () -> session.attachArtifact(null));
    }

    @Test
    void directEventsArtifactsAndExportsAreAlreadyRedacted() {
        String secret = "trace-canary-293a";
        UiTestLensSession session = UiTestLensSession.start("session " + secret,
                RetryOutcomePolicy.REPORT_ONLY, 0, RedactionPolicy.builder().secret(secret).build());
        session.addEvent(TraceEvent.builder(TraceEventType.CUSTOM, TraceStatus.FAILED, "name " + secret)
                .message("token=" + secret).attribute("authorization", secret)
                .failure(new TraceFailure("failure " + secret, "Example", "stack " + secret,
                        java.util.Map.of("password", secret))).build());
        session.attachArtifact(TraceArtifact.url("artifact " + secret, TraceArtifactType.CUSTOM_URL,
                "https://user:" + secret + "@example.test/a?token=" + secret + "#fragment"));

        String memory = session.metadata() + session.events().toString() + session.artifacts();
        assertFalse(memory.contains(secret));
        assertFalse(session.exportJson().contains(secret));
        assertFalse(session.exportHtml().contains(secret));
        assertTrue(session.exportJson().contains("[REDACTED]"));
    }

    @Test
    void directJsonEventIsStructurallyRedactedInMemoryAndInExports() {
        String canary = "o'TRACE_JSON_CANARY";
        UiTestLensSession session = UiTestLensSession.start("json redaction");
        session.addEvent(TraceEvent.info("json", "{\"password\":\"" + canary + "\"}"));

        String memory = session.events().toString();
        String json = session.exportJson();
        String html = session.exportHtml();

        assertFalse(memory.contains(canary));
        assertFalse(json.contains(canary));
        assertFalse(html.contains(canary));
        assertTrue(json.contains("[REDACTED]"));
        assertTrue(html.contains("[REDACTED]"));
    }

    private static void finishWhenReleased(CountDownLatch ready, CountDownLatch release, Runnable finish) {
        ready.countDown();
        try {
            release.await();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError(failure);
        }
        finish.run();
    }

    private static void assertTerminal(UiTestLensSession session, TraceStatus status, Instant finishedAt) {
        assertEquals(status, session.metadata().status());
        assertSame(finishedAt, session.metadata().finishedAt());
        assertEquals(1, terminalEvents(session).size());
        assertEquals(1, retrySummaryEvents(session).size());
    }

    private static List<TraceEvent> terminalEvents(UiTestLensSession session) {
        return session.events().stream()
                .filter(event -> event.type() == TraceEventType.SESSION_FINISHED)
                .toList();
    }

    private static List<TraceEvent> retrySummaryEvents(UiTestLensSession session) {
        return session.events().stream()
                .filter(event -> event.type() == TraceEventType.RETRY_SUMMARY)
                .toList();
    }
}

