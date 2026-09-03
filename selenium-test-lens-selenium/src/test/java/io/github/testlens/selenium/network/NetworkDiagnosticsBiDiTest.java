package io.github.testlens.selenium.network;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.logging.UiTestLensLogEntry;
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
        assertSame(expected, failure.exception());
        assertEquals(0, failure.attempts());
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
            assertTrue(matched.attempts() >= 2);
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
            assertTrue(stopped.attempts() >= 1);
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

    private static WebDriver fakeDriver() {
        return (WebDriver) Proxy.newProxyInstance(NetworkDiagnosticsBiDiTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) ->
                        method.getName().equals("toString") ? "network-driver" : null);
    }

    private static final class FakeFactory implements NetworkCaptureSourceFactory {
        private final AtomicInteger opens = new AtomicInteger();
        private final List<FakeSource> sources = new ArrayList<>();
        private boolean unsupported;
        private RuntimeException failure;

        @Override
        public synchronized NetworkCaptureSource open(WebDriver driver, NetworkDiagnosticsOptions options,
                                                       NetworkCaptureSink sink) {
            opens.incrementAndGet();
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
