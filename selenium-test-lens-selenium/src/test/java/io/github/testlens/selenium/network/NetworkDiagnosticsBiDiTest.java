package io.github.testlens.selenium.network;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.trace.TraceLogSink;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NetworkDiagnosticsBiDiTest {
    @Test
    void bidiAndAutoActivateOnlyAfterFactorySucceeds() {
        for (NetworkCaptureMode requested : List.of(NetworkCaptureMode.BIDI, NetworkCaptureMode.AUTO)) {
            FakeFactory factory = new FakeFactory();
            NetworkDiagnostics diagnostics = diagnostics(factory).start(options(requested));

            assertTrue(diagnostics.isStarted());
            assertEquals(requested, diagnostics.captureMode());
            assertEquals(NetworkCaptureMode.BIDI, diagnostics.activeCaptureMode().orElseThrow());
            assertEquals(NetworkDiagnosticsStatus.STARTED, diagnostics.summary().status());
            assertEquals(1, factory.opens.get());
        }
    }

    @Test
    void callbacksEmitTypedTraceEventsAndLifecycle() {
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(),
                OverlayLogger.from(UiTestLensLogger.builder().sink(entries::add).build()), factory)
                .start(options(NetworkCaptureMode.BIDI));
        factory.latest().fire(request("1", "/request", 0));
        factory.latest().fire(response("1", "/response", 200, 0));
        factory.latest().fire(NetworkEvent.failed(NetworkFailure.of("2", "/failure", "broken")));
        diagnostics.stop();

        assertTrue(entries.stream().anyMatch(entry -> entry.eventType() == UiTestLensEventType.NETWORK_DIAGNOSTICS_STARTED));
        assertTrue(entries.stream().anyMatch(entry -> entry.eventType() == UiTestLensEventType.NETWORK_REQUEST_RECORDED));
        assertTrue(entries.stream().anyMatch(entry -> entry.eventType() == UiTestLensEventType.NETWORK_RESPONSE_RECORDED));
        assertTrue(entries.stream().anyMatch(entry -> entry.eventType() == UiTestLensEventType.NETWORK_FAILURE_RECORDED));
        assertTrue(entries.stream().anyMatch(entry -> entry.eventType() == UiTestLensEventType.NETWORK_DIAGNOSTICS_STOPPED));
    }

    @Test
    void hiddenHudTrafficStillReachesCaptureWaitAssertionTraceAndExternalSink() {
        List<UiTestLensLogEntry> external = new ArrayList<>();
        UiTestLensSession session = UiTestLensSession.start("hidden-network");
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder()
                .sink(external::add).sink(new TraceLogSink(session)).build());
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger, factory)
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI)
                        .hudFilter(NetworkHudFilter.builder().includeUrlPattern("/visible$")
                                .excludeUrlPattern("/hidden.*").build()).build());

        factory.latest().fire(response("hidden", "/hidden", 200, 0));
        factory.latest().fire(response("hidden-failure", "/hidden-failure", 503, 0));

        assertEquals(2, diagnostics.summary().totalResponses());
        assertEquals(NetworkWaitStatus.MATCHED, diagnostics.waitForResponse("/hidden", 200).status());
        assertThrows(NetworkAssertionError.class, diagnostics::assertNoFailedRequests);
        UiTestLensLogEntry raw = external.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.NETWORK_RESPONSE_RECORDED)
                .findFirst().orElseThrow();
        assertEquals("false", raw.metadata().get("hudVisible"));
        assertEquals("RESPONSE", raw.metadata().get("networkEventType"));
        assertEquals("hidden", raw.metadata().get("requestId"));
        assertEquals("/hidden", raw.metadata().get("url"));
        assertEquals("200", raw.metadata().get("status"));
        assertEquals("false", raw.metadata().get("resourceTypeAvailable"));
        assertTrue(session.events().stream().anyMatch(event ->
                "false".equals(event.attributes().get("metadata.hudVisible"))));
        assertTrue(diagnostics.exportJson().contains("/hidden"));
    }

    @Test
    void restartUsesNewHudFilterWithoutChangingHistoryOrCounters() {
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(),
                OverlayLogger.from(UiTestLensLogger.builder().sink(entries::add).build()), factory)
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI)
                        .hudFilter(NetworkHudFilter.none()).build());
        factory.latest().fire(response("one", "/one", 200, 0));

        diagnostics.start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI)
                .hudFilter(NetworkHudFilter.all()).build());
        factory.latest().fire(response("two", "/two", 200, 0));

        List<UiTestLensLogEntry> raw = entries.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.NETWORK_RESPONSE_RECORDED).toList();
        assertEquals(List.of("false", "true"), raw.stream()
                .map(entry -> entry.metadata().get("hudVisible")).toList());
        assertEquals(2, diagnostics.summary().totalResponses());
        assertEquals(0, diagnostics.summary().droppedEvents());
    }

    @Test
    void manualResourceTypeAndSafeMetadataArePreservedWithoutLeakingUrlSecrets() {
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(),
                OverlayLogger.from(UiTestLensLogger.builder().sink(entries::add).build()))
                .start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.MANUAL)
                        .hudFilter(NetworkHudFilter.all()).build());

        diagnostics.addManualEvent(NetworkEvent.request(new NetworkRequest("manual", "POST",
                "https://user:password@example.test/api/orders?token=secret#private", "custom-api",
                Instant.now(), Map.of())));

        UiTestLensLogEntry raw = entries.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.NETWORK_REQUEST_RECORDED)
                .findFirst().orElseThrow();
        assertEquals("custom-api", raw.metadata().get("resourceType"));
        assertEquals("true", raw.metadata().get("resourceTypeAvailable"));
        assertEquals("/api/orders", raw.metadata().get("url"));
        assertFalse(raw.message().contains("secret"));
        assertFalse(raw.message().contains("password"));
        assertEquals("POST /api/orders", raw.message());
        assertEquals("custom-api", diagnostics.events().stream()
                .filter(event -> event.request() != null).findFirst().orElseThrow().request().resourceType());
    }

    @Test
    void unsupportedAndFailedStartRemainInactiveAndWaitWithoutAttempts() {
        FakeFactory unsupportedFactory = new FakeFactory();
        unsupportedFactory.unsupported = true;
        NetworkDiagnostics unsupported = diagnostics(unsupportedFactory).start(options(NetworkCaptureMode.AUTO));
        NetworkWaitResult skipped = unsupported.waitForResponse("/api", 200);
        assertFalse(unsupported.isStarted());
        assertTrue(unsupported.activeCaptureMode().isEmpty());
        assertEquals(NetworkDiagnosticsStatus.UNSUPPORTED, unsupported.summary().status());
        assertEquals(NetworkWaitStatus.SKIPPED, skipped.status());
        assertEquals(NetworkWaitFailureReason.UNSUPPORTED_CAPTURE_MODE, skipped.failureReason());
        assertEquals(0, skipped.attempts());

        FakeFactory failedFactory = new FakeFactory();
        RuntimeException expected = new RuntimeException("subscribe failed");
        failedFactory.failure = expected;
        NetworkDiagnostics failed = diagnostics(failedFactory).start(options(NetworkCaptureMode.BIDI));
        NetworkWaitResult failure = failed.waitForResponse("/api", 200);
        assertEquals(NetworkDiagnosticsStatus.FAILED, failed.summary().status());
        assertEquals(NetworkWaitStatus.FAILED, failure.status());
        assertEquals(NetworkWaitFailureReason.CAPTURE_START_FAILED, failure.failureReason());
        assertNotSame(expected, failure.exception(), "public diagnostics use a safe throwable snapshot");
        assertEquals(expected.toString(), failure.exception().toString());
        assertEquals(0, failure.attempts());
    }

    @Test
    void assertionRejectsEveryGenerationThatNeverBecameActive() {
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(entries::add).build());

        NetworkDiagnostics neverStarted = new NetworkDiagnostics(fakeDriver(), logger, new FakeFactory());
        neverStarted.stop();
        NetworkDiagnostics off = new NetworkDiagnostics(fakeDriver(), logger, new FakeFactory())
                .start(options(NetworkCaptureMode.OFF));
        NetworkDiagnostics performanceLogs = new NetworkDiagnostics(fakeDriver(), logger, new FakeFactory())
                .start(options(NetworkCaptureMode.PERFORMANCE_LOGS));
        FakeFactory bidiUnsupportedFactory = new FakeFactory();
        bidiUnsupportedFactory.unsupported = true;
        NetworkDiagnostics bidiUnsupported = new NetworkDiagnostics(fakeDriver(), logger, bidiUnsupportedFactory)
                .start(options(NetworkCaptureMode.BIDI));
        FakeFactory autoUnsupportedFactory = new FakeFactory();
        autoUnsupportedFactory.unsupported = true;
        NetworkDiagnostics autoUnsupported = new NetworkDiagnostics(fakeDriver(), logger, autoUnsupportedFactory)
                .start(options(NetworkCaptureMode.AUTO));
        FakeFactory openFailedFactory = new FakeFactory();
        openFailedFactory.failure = new IllegalStateException("adapter open failed");
        NetworkDiagnostics openFailed = new NetworkDiagnostics(fakeDriver(), logger, openFailedFactory)
                .start(options(NetworkCaptureMode.BIDI));
        FakeFactory registrationFailedFactory = new FakeFactory();
        registrationFailedFactory.failure = new IllegalStateException("listener registration failed");
        NetworkDiagnostics registrationFailed = new NetworkDiagnostics(fakeDriver(), logger, registrationFailedFactory)
                .start(options(NetworkCaptureMode.BIDI));

        for (NetworkDiagnostics invalid : List.of(neverStarted, off, performanceLogs,
                bidiUnsupported, autoUnsupported, openFailed, registrationFailed)) {
            NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                    invalid::assertNoFailedRequests);
            assertTrue(error.getMessage().startsWith("Cannot assert network failures:"));
            assertFalse(error.getMessage().contains("No failed network requests"));
            assertEquals(invalid.summary().status(), error.summary().status());
        }
        assertEquals(7, entries.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.NETWORK_ASSERTION_FAILED).count());
        assertEquals(0, entries.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.NETWORK_ASSERTION_PASSED).count());
        assertEquals(NetworkDiagnosticsStatus.STOPPED, neverStarted.summary().status());
        assertEquals(NetworkDiagnosticsStatus.STOPPED, off.summary().status());
        assertEquals(NetworkDiagnosticsStatus.UNSUPPORTED, performanceLogs.summary().status());
        assertEquals(NetworkDiagnosticsStatus.UNSUPPORTED, bidiUnsupported.summary().status());
        assertEquals(NetworkDiagnosticsStatus.UNSUPPORTED, autoUnsupported.summary().status());
        assertEquals(NetworkDiagnosticsStatus.FAILED, openFailed.summary().status());
        assertEquals(NetworkDiagnosticsStatus.FAILED, registrationFailed.summary().status());
    }

    @Test
    void validActiveAndStoppedSnapshotsCanPassAcrossManualBidiAndAuto() {
        NetworkDiagnostics manual = diagnostics(new FakeFactory()).start(options(NetworkCaptureMode.MANUAL));
        assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED, manual.assertNoFailedRequests().status());
        manual.addManualEvent(response("manual", "/ok", 200, 0));
        manual.stop();
        assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED, manual.assertNoFailedRequests().status());
        assertEquals(NetworkDiagnosticsStatus.STOPPED, manual.assertNoFailedRequests().summary().status());

        for (NetworkCaptureMode mode : List.of(NetworkCaptureMode.BIDI, NetworkCaptureMode.AUTO)) {
            FakeFactory factory = new FakeFactory();
            NetworkDiagnostics diagnostics = diagnostics(factory).start(options(mode));
            factory.latest().fire(response(mode.name(), "/ok", 200, 0));
            assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED,
                    diagnostics.assertNoFailedRequests().status());
        }
    }

    @Test
    void aNewInvalidGenerationInvalidatesAnOlderValidSnapshotWithoutClearingHistory() {
        for (NetworkCaptureMode invalidMode : List.of(NetworkCaptureMode.OFF,
                NetworkCaptureMode.BIDI, NetworkCaptureMode.AUTO)) {
            FakeFactory factory = new FakeFactory();
            NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
            factory.latest().fire(response("old", "/old", 200, 0));
            diagnostics.stop();
            assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED,
                    diagnostics.assertNoFailedRequests().status());

            if (invalidMode != NetworkCaptureMode.OFF) factory.unsupported = true;
            diagnostics.start(options(invalidMode));

            NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                    diagnostics::assertNoFailedRequests);
            assertEquals(1, error.summary().totalResponses(), "event history remains preserved");
            assertEquals(invalidMode == NetworkCaptureMode.OFF
                            ? NetworkDiagnosticsStatus.STOPPED : NetworkDiagnosticsStatus.UNSUPPORTED,
                    error.summary().status());
        }

        FakeFactory failedFactory = new FakeFactory();
        NetworkDiagnostics failed = diagnostics(failedFactory).start(options(NetworkCaptureMode.BIDI));
        failedFactory.latest().fire(response("old", "/old", 200, 0));
        failedFactory.failure = new IllegalStateException("new generation failed");
        failed.start(options(NetworkCaptureMode.BIDI));
        NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                failed::assertNoFailedRequests);
        assertEquals(NetworkDiagnosticsStatus.FAILED, error.summary().status());
        assertEquals(1, error.summary().totalResponses());
    }

    @Test
    void assertionDuringInitializationFailsImmediatelyAndSuccessfulActivationCanLaterPass() throws Exception {
        FakeFactory factory = new FakeFactory();
        factory.blockOpen = true;
        NetworkDiagnostics diagnostics = diagnostics(factory);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> starting = executor.submit(() -> diagnostics.start(options(NetworkCaptureMode.BIDI)));
            assertTrue(factory.openEntered.await(1, TimeUnit.SECONDS));

            NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                    diagnostics::assertNoFailedRequests);
            assertTrue(error.getMessage().startsWith("Cannot assert network failures:"));
            assertEquals(NetworkDiagnosticsStatus.STOPPED, error.summary().status());

            factory.releaseOpen.countDown();
            starting.get(1, TimeUnit.SECONDS);
            assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED,
                    diagnostics.assertNoFailedRequests().status());
        } finally {
            factory.releaseOpen.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void invalidAssertionRedactsStartFailureAndDoesNotAffectRetryOrLifecycleStatus() {
        String secret = "TL_INVALID_CAPTURE_SECRET";
        RuntimeException cause = new RuntimeException("cause token=" + secret);
        RuntimeException original = new IllegalStateException("listener token=" + secret, cause);
        original.addSuppressed(new RuntimeException("cleanup token=" + secret));
        FakeFactory factory = new FakeFactory();
        factory.failure = original;
        UiTestLensSession session = UiTestLensSession.start("invalid capture assertion");
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder()
                .redactionPolicy(RedactionPolicy.builder().secret(secret).build())
                .sink(entries::add).sink(new TraceLogSink(session)).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger, factory)
                .start(options(NetworkCaptureMode.BIDI));

        NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                diagnostics::assertNoFailedRequests);

        String outward = error.getMessage() + error + error.getCause()
                + error.getCause().getCause() + error.getCause().getSuppressed()[0]
                + entries + session.events();
        assertFalse(outward.contains(secret));
        assertTrue(outward.contains("[REDACTED]"));
        assertEquals(NetworkDiagnosticsStatus.FAILED, error.summary().status());
        assertEquals(0, session.retrySummary().totalRetries());
        assertEquals(1, entries.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.NETWORK_ASSERTION_FAILED).count());
        assertEquals(0, entries.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.NETWORK_ASSERTION_PASSED).count());
        assertTrue(original.getMessage().contains(secret), "the runtime failure remains unchanged");
    }

    @Test
    void stopAndAssertionObserveAConsistentValidSnapshot() throws Exception {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        factory.latest().fire(response("ok", "/ok", 200, 0));
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> stopping = executor.submit(() -> {
                await(go);
                diagnostics.stop();
            });
            Future<NetworkDiagnosticsResult> asserting = executor.submit(() -> {
                await(go);
                return diagnostics.assertNoFailedRequests();
            });
            go.countDown();
            NetworkDiagnosticsResult result = asserting.get(1, TimeUnit.SECONDS);
            assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED, result.status());
            assertTrue(List.of(NetworkDiagnosticsStatus.STARTED, NetworkDiagnosticsStatus.STOPPED)
                    .contains(result.summary().status()));
            stopping.get(1, TimeUnit.SECONDS);
            assertEquals(NetworkDiagnosticsStatus.ASSERTION_PASSED,
                    diagnostics.assertNoFailedRequests().status());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void startFailureAssertionRedactsCauseAndSuppressedDiagnostics() {
        String secret = "TL_NETWORK_START_FAILURE_SECRET";
        RuntimeException cause = new RuntimeException("cause " + secret);
        RuntimeException original = new RuntimeException("subscribe " + secret, cause);
        original.addSuppressed(new IllegalStateException("cleanup " + secret));
        FakeFactory factory = new FakeFactory();
        factory.failure = original;
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder()
                .redactionPolicy(RedactionPolicy.builder().secret(secret).build()).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger, factory)
                .start(options(NetworkCaptureMode.BIDI));

        NetworkAssertionError error = assertThrows(NetworkAssertionError.class,
                () -> diagnostics.expectResponse().urlContains("/api").waitNow());

        String outward = error.getMessage() + error + error.waitResult().message()
                + error.waitResult().exception() + error.getCause() + error.getCause().getCause()
                + error.getCause().getSuppressed()[0];
        assertFalse(outward.contains(secret));
        assertTrue(outward.contains("[REDACTED]"));
        assertSame(error.waitResult().exception(), error.getCause());
        assertTrue(original.getMessage().contains(secret));
    }

    @Test
    void stopIsIdempotentAndLateCallbacksAreIgnored() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        FakeSource source = factory.latest();
        source.fire(response("one", "/api/one", 200, 0));

        diagnostics.stop();
        diagnostics.stop();
        source.fire(response("late", "/api/late", 200, 0));

        assertEquals(1, source.closes.get());
        assertFalse(diagnostics.isStarted());
        assertTrue(diagnostics.activeCaptureMode().isEmpty());
        assertEquals(1, diagnostics.summary().totalResponses());
    }

    @Test
    void stopFailureIsDiagnosticAndNeverClosesDriver() {
        AtomicInteger quitCalls = new AtomicInteger();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    if (method.getName().equals("quit")) quitCalls.incrementAndGet();
                    return null;
                });
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(driver, OverlayLogger.noop(), factory)
                .start(options(NetworkCaptureMode.BIDI));
        factory.latest().closeFailure = new RuntimeException("unsubscribe failed");

        assertDoesNotThrow(diagnostics::stop);

        assertEquals(NetworkDiagnosticsStatus.STOPPED, diagnostics.summary().status());
        assertEquals(1, factory.latest().closes.get());
        assertEquals(0, quitCalls.get());
        assertTrue(diagnostics.events().stream()
                .anyMatch(event -> event.message().contains("unsubscribe failed")));
    }

    @Test
    void cyclicWrappedDriverIsUnsupportedWithoutUnboundedUnwrap() {
        WebDriver cyclic = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, WrapsDriver.class}, (proxy, method, args) ->
                        method.getName().equals("getWrappedDriver") ? proxy : null);

        NetworkDiagnostics diagnostics = new NetworkDiagnostics(cyclic)
                .start(options(NetworkCaptureMode.BIDI));

        assertEquals(NetworkDiagnosticsStatus.UNSUPPORTED, diagnostics.summary().status());
        assertFalse(diagnostics.isStarted());
        assertEquals(0, diagnostics.waitForResponse("/never", 200).attempts());
    }

    @Test
    void restartClosesOldGenerationAndDoesNotDuplicateEvents() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        FakeSource old = factory.latest();

        diagnostics.start(options(NetworkCaptureMode.BIDI));
        FakeSource current = factory.latest();
        old.fire(response("old", "/api/old", 200, 0));
        current.fire(response("new", "/api/new", 200, 0));

        assertEquals(1, old.closes.get());
        assertEquals(2, factory.opens.get());
        assertEquals(1, diagnostics.summary().totalResponses());
        assertTrue(diagnostics.events().stream().anyMatch(event -> "/api/new".equals(event.url())));
        assertFalse(diagnostics.events().stream().anyMatch(event -> "/api/old".equals(event.url())));
    }

    @Test
    void eventLimitDropsCapturedEventsAndWarnsOnlyOnce() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.BIDI).maxCapturedEvents(2).build());
        FakeSource source = factory.latest();

        source.fire(request("1", "/api/one", 0));
        source.fire(response("1", "/api/one", 200, 0));
        source.fire(response("2", "/api/two", 200, 0));
        source.fire(NetworkEvent.failed(NetworkFailure.of("3", "/api/three", "broken")));

        assertEquals(1, diagnostics.summary().totalRequests());
        assertEquals(1, diagnostics.summary().totalResponses());
        assertEquals(2, diagnostics.summary().droppedEvents());
        assertEquals(1, diagnostics.events().stream()
                .filter(event -> event.message().contains("event limit reached")).count());
        assertTrue(diagnostics.findMatchingEvent(NetworkWaitCondition.builder()
                .urlContains("/api/two").status(200).build()).isEmpty());
    }

    @Test
    void asynchronousEventSignalsWaitWithoutWaitingForPollInterval() throws Exception {
        CountDownLatch waitStarted = new CountDownLatch(1);
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(entry -> {
            if (entry.eventType() == UiTestLensEventType.NETWORK_WAIT_STARTED) waitStarted.countDown();
        }).build());
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger, factory)
                .start(options(NetworkCaptureMode.BIDI));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<NetworkWaitResult> result = executor.submit(() -> diagnostics.waitForResponse(
                    NetworkWaitCondition.builder().urlContains("/api/async").status(201)
                            .timeout(Duration.ofSeconds(3)).pollInterval(Duration.ofSeconds(2)).build()));
            assertTrue(waitStarted.await(1, TimeUnit.SECONDS));
            factory.latest().fire(response("async", "/api/async", 201, 0));

            NetworkWaitResult matched = result.get(1, TimeUnit.SECONDS);
            assertEquals(NetworkWaitStatus.MATCHED, matched.status());
            assertTrue(matched.attempts() >= 1);
            assertNotNull(matched.matchedResponse());
            assertEquals(201, matched.matchedResponse().status());
            assertEquals("/api/async", matched.matchedResponse().url());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void stopWakesWaitAndReturnsCaptureNotStarted() throws Exception {
        CountDownLatch waitStarted = new CountDownLatch(1);
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(entry -> {
            if (entry.eventType() == UiTestLensEventType.NETWORK_WAIT_STARTED) waitStarted.countDown();
        }).build());
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger, factory)
                .start(options(NetworkCaptureMode.BIDI));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<NetworkWaitResult> result = executor.submit(() -> diagnostics.waitForResponse(
                    NetworkWaitCondition.builder().urlContains("/never").timeout(Duration.ofSeconds(3))
                            .pollInterval(Duration.ofSeconds(2)).build()));
            assertTrue(waitStarted.await(1, TimeUnit.SECONDS));
            diagnostics.stop();

            NetworkWaitResult stopped = result.get(1, TimeUnit.SECONDS);
            assertEquals(NetworkWaitStatus.SKIPPED, stopped.status());
            assertEquals(NetworkWaitFailureReason.CAPTURE_NOT_STARTED, stopped.failureReason());
            assertFalse(diagnostics.isStarted());
            assertEquals(1, factory.latest().closes.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void concurrentCallbacksAreSafeAndSnapshotsAreImmutable() throws Exception {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.BIDI).maxCapturedEvents(500).build());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch release = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < 8; worker++) {
                int first = worker * 25;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(release.await(2, TimeUnit.SECONDS));
                    for (int value = first; value < first + 25; value++) {
                        factory.latest().fire(response(String.valueOf(value), "/api/" + value, 200, 0));
                    }
                    return null;
                }));
            }
            assertTrue(ready.await(2, TimeUnit.SECONDS));
            release.countDown();
            for (Future<?> future : futures) future.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(200, diagnostics.summary().totalResponses());
        List<NetworkEvent> snapshot = diagnostics.events();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(NetworkEvent.info("no")));
    }

    @Test
    void redirectCorrelationUsesRequestIdAndRedirectCountAndResponseMayStandAlone() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        FakeSource source = factory.latest();
        source.fire(request("shared", "/redirect", 0));
        source.fire(response("shared", "/redirect", 302, 0));
        source.fire(request("shared", "/api/final", 1));
        source.fire(response("shared", "/api/final", 200, 1));
        source.fire(response("standalone", "/api/standalone", 204, 0));

        NetworkWaitResult finalResponse = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/final").method("GET").status(200).build());
        assertEquals(NetworkWaitStatus.MATCHED, finalResponse.status());
        assertEquals("/api/final", finalResponse.matchedRequest().url());
        assertTrue(diagnostics.findMatchingEvent(NetworkWaitCondition.builder()
                .urlContains("/api/standalone").status(204).build()).isPresent());
    }

    @Test
    void embeddedRequestMakesResponseBeforeRequestImmediatelyCorrelatable() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        FakeSource source = factory.latest();
        source.fire(responseWithRequest("race", "POST", "/api/request", "/api/response", 201, 0));

        NetworkWaitResult matched = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/response").method("POST").status(201).build());

        assertEquals(NetworkWaitStatus.MATCHED, matched.status());
        assertNotNull(matched.matchedRequest());
        assertEquals(matched.matchedResponse().requestId(), matched.matchedRequest().id());
        assertEquals("/api/request", matched.matchedRequest().url());
        assertEquals("/api/response", matched.matchedResponse().url());
        assertEquals(0, diagnostics.summary().totalRequests());
        assertEquals(1, diagnostics.summary().totalResponses());

        source.fire(request("race", "/api/request", 0));
        assertEquals(1, diagnostics.summary().totalRequests());
        assertEquals(1, diagnostics.summary().totalResponses());
        assertEquals(1, diagnostics.events().stream()
                .filter(event -> event.type() == NetworkEventType.REQUEST).count());
    }

    @Test
    void requestBeforeResponseStillUsesTheMatchingRequest() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        FakeSource source = factory.latest();
        source.fire(request("ordered", "/api/ordered", 0));
        source.fire(responseWithRequest("ordered", "GET", "/api/ordered", "/api/ordered", 200, 0));

        NetworkWaitResult matched = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/ordered").method("GET").status(200).build());

        assertEquals(NetworkWaitStatus.MATCHED, matched.status());
        assertEquals("ordered", matched.matchedRequest().id());
        assertEquals(1, diagnostics.summary().totalRequests());
        assertEquals(1, diagnostics.summary().totalResponses());
    }

    @Test
    void standaloneResponseWithoutRequestDataRemainsMatchableWithNullRequest() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        factory.latest().fire(response("standalone", "/api/standalone", 204, 0));

        NetworkWaitResult matched = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/standalone").status(204).build());

        assertEquals(NetworkWaitStatus.MATCHED, matched.status());
        assertNotNull(matched.matchedResponse());
        assertNull(matched.matchedRequest());
    }

    @Test
    void embeddedRequestsKeepRedirectsWithTheSameIdSeparatedByRedirectCount() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        FakeSource source = factory.latest();
        source.fire(responseWithRequest("shared", "GET", "/redirect", "/redirect", 302, 0));
        source.fire(responseWithRequest("shared", "POST", "/api/final", "/api/final", 200, 1));

        NetworkWaitResult redirect = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/redirect").method("GET").status(302).includeFailedResponses(true).build());
        NetworkWaitResult target = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/final").method("POST").status(200).build());

        assertEquals("/redirect", redirect.matchedRequest().url());
        assertEquals("/api/final", target.matchedRequest().url());
        assertEquals("shared", redirect.matchedRequest().id());
        assertEquals("shared", target.matchedRequest().id());
        assertEquals(0, diagnostics.summary().totalRequests());
        assertEquals(2, diagnostics.summary().totalResponses());
    }

    @Test
    void responseBeforeRequestAndRedirectSnapshotsUseTheCentralRedactionPolicy() {
        String secret = "TL_BIDI_CORRELATION_SECRET";
        RedactionPolicy policy = RedactionPolicy.builder().sensitiveKey("tenant-key")
                .secret(secret).build();
        FakeFactory factory = new FakeFactory();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder()
                .redactionPolicy(policy).build());
        NetworkDiagnostics diagnostics = new NetworkDiagnostics(fakeDriver(), logger, factory)
                .start(options(NetworkCaptureMode.BIDI));
        FakeSource source = factory.latest();
        String redirectUrl = "https://user:pass@example.test/redirect?token=" + secret + "#private";
        String finalRequestUrl = "https://example.test/api/final?tenant-key=" + secret + "&safe=visible";
        String finalResponseUrl = finalRequestUrl + "#response-fragment";
        source.fire(responseWithRequest("shared", "GET", redirectUrl, redirectUrl, 302, 0));
        source.fire(responseWithRequest("shared", "POST", finalRequestUrl, finalResponseUrl, 503, 1));
        source.fire(request("shared", finalRequestUrl, 1));

        NetworkWaitResult matched = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .exactUrl(finalResponseUrl).method("POST").status(503)
                .includeFailedResponses(true).build());
        NetworkEvent failure = diagnostics.summary().firstFailure().orElseThrow();

        String outward = diagnostics.events() + diagnostics.exportJson() + diagnostics.summary().failureSummary()
                + failure.url() + failure.response().headers() + failure.correlatedRequest().url()
                + matched.message() + matched.conditionSummary() + matched.matchedRequest().url()
                + matched.matchedResponse().url();
        assertFalse(outward.contains(secret));
        assertFalse(outward.contains("user:pass"));
        assertFalse(outward.contains("#private"));
        assertFalse(outward.contains("#response-fragment"));
        assertTrue(failure.url().contains("safe=visible"));
        assertEquals("shared", matched.matchedRequest().id());
        assertEquals(matched.matchedResponse().requestId(), matched.matchedRequest().id());
        assertEquals(1, diagnostics.summary().totalRequests());
        assertEquals(2, diagnostics.summary().totalResponses());
    }

    @Test
    void malformedFailureUrlIsFailClosedAtThePublicBoundary() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        String malformed = "http://[broken?token=TL_MALFORMED_SECRET#TL_MALFORMED_FRAGMENT";
        factory.latest().fire(NetworkEvent.failed(NetworkFailure.of("failed", malformed, "fetch failed")));

        NetworkEvent safe = diagnostics.summary().firstFailure().orElseThrow();

        assertTrue(safe.url().startsWith("url[length="));
        assertFalse(safe.url().contains("TL_MALFORMED_SECRET"));
        assertFalse(safe.url().contains("TL_MALFORMED_FRAGMENT"));
        assertFalse(diagnostics.exportJson().contains("TL_MALFORMED_SECRET"));
    }

    @Test
    void responseWithoutEmbeddedDataNeverBorrowsARequestFromAnotherRedirect() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(options(NetworkCaptureMode.BIDI));
        FakeSource source = factory.latest();
        source.fire(request("shared", "/redirect", 0));
        source.fire(response("shared", "/api/final", 200, 1));

        assertTrue(diagnostics.findMatchingEvent(NetworkWaitCondition.builder()
                .urlContains("/api/final").method("GET").status(200).build()).isEmpty());
    }

    @Test
    void embeddedCorrelationDoesNotConsumeAnExtraEventLimitSlot() {
        FakeFactory factory = new FakeFactory();
        NetworkDiagnostics diagnostics = diagnostics(factory).start(NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.BIDI).maxCapturedEvents(1).build());
        FakeSource source = factory.latest();
        source.fire(responseWithRequest("limited", "POST", "/api/limited", "/api/limited", 201, 0));

        NetworkWaitResult matched = diagnostics.waitForResponse(NetworkWaitCondition.builder()
                .urlContains("/api/limited").method("POST").status(201).build());
        source.fire(request("limited", "/api/limited", 0));

        assertEquals(NetworkWaitStatus.MATCHED, matched.status());
        assertEquals("limited", matched.matchedRequest().id());
        assertEquals(0, diagnostics.summary().totalRequests());
        assertEquals(1, diagnostics.summary().totalResponses());
        assertEquals(1, diagnostics.summary().droppedEvents());
    }

    private NetworkDiagnostics diagnostics(FakeFactory factory) {
        return new NetworkDiagnostics(fakeDriver(), OverlayLogger.noop(), factory);
    }

    private static NetworkDiagnosticsOptions options(NetworkCaptureMode mode) {
        return NetworkDiagnosticsOptions.builder().captureMode(mode).build();
    }

    private static NetworkEvent request(String id, String url, int redirect) {
        return NetworkEvent.request(new NetworkRequest(id, "GET", url, "fetch", Instant.now(), Map.of()),
                Instant.now(), Map.of("redirectCount", String.valueOf(redirect)));
    }

    private static NetworkEvent response(String id, String url, int status, int redirect) {
        return NetworkEvent.response(NetworkResponse.of(id, url, status), Instant.now(),
                Map.of("redirectCount", String.valueOf(redirect)));
    }

    private static NetworkEvent responseWithRequest(String id, String method, String requestUrl,
                                                    String responseUrl, int status, int redirect) {
        NetworkRequest request = new NetworkRequest(id, method, requestUrl, "fetch", Instant.now(), Map.of());
        return NetworkEvent.response(NetworkResponse.of(id, responseUrl, status), request, Instant.now(),
                Map.of("redirectCount", String.valueOf(redirect)));
    }

    private static WebDriver fakeDriver() {
        return (WebDriver) Proxy.newProxyInstance(NetworkDiagnosticsBiDiTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) ->
                        method.getName().equals("toString") ? "network-driver" : null);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) throw new IllegalStateException("latch timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", interrupted);
        }
    }

    private static final class FakeFactory implements NetworkCaptureSourceFactory {
        private final AtomicInteger opens = new AtomicInteger();
        private final List<FakeSource> sources = new ArrayList<>();
        private boolean unsupported;
        private RuntimeException failure;
        private boolean blockOpen;
        private final CountDownLatch openEntered = new CountDownLatch(1);
        private final CountDownLatch releaseOpen = new CountDownLatch(1);

        @Override
        public synchronized NetworkCaptureSource open(WebDriver driver, NetworkDiagnosticsOptions options,
                                                       NetworkCaptureSink sink) {
            opens.incrementAndGet();
            openEntered.countDown();
            if (blockOpen) await(releaseOpen);
            if (unsupported) throw new NetworkCaptureUnsupportedException("BiDi unavailable");
            if (failure != null) throw failure;
            FakeSource source = new FakeSource(sink);
            sources.add(source);
            return source;
        }

        synchronized FakeSource latest() { return sources.get(sources.size() - 1); }
    }

    private static final class FakeSource implements NetworkCaptureSource {
        private final NetworkCaptureSink sink;
        private final AtomicInteger closes = new AtomicInteger();
        private RuntimeException closeFailure;
        private FakeSource(NetworkCaptureSink sink) { this.sink = sink; }
        void fire(NetworkEvent event) { sink.recorded(event); }
        @Override public void close() {
            closes.incrementAndGet();
            if (closeFailure != null) throw closeFailure;
        }
    }
}
