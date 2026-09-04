package io.github.testlens.selenium.assertions;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiPageExpectTest {

    @Test
    void exactAndContainingUrlUseRawCaseSensitiveValues() {
        PageDriver exact = PageDriver.urls("https://example.test/dashboard?token=secret#part");
        UiAssertionResult exactResult = expect(exact).toHaveUrl("https://example.test/dashboard?token=secret#part");
        assertEquals(UiAssertionStatus.PASSED, exactResult.status());
        assertEquals(1, exact.urlReads());

        PageDriver contains = PageDriver.urls("https://example.test/Dashboard?a=1");
        assertEquals(UiAssertionStatus.PASSED, expect(contains).toContainUrl("/Dashboard?a=1").status());
        UiAssertionError mismatch = assertThrows(UiAssertionError.class,
                () -> expect(PageDriver.urls("https://example.test/Dashboard")).toContainUrl("/dashboard"));
        assertEquals(UiAssertionFailureReason.URL_MISMATCH, mismatch.result().failureReason());
    }

    @Test
    void exactAndContainingTitleUseTextOptions() {
        UiAssertionOptions options = options().normalizeWhitespace(true).trimText(true).caseSensitive(false).build();
        PageDriver exact = PageDriver.titles("  Order\n  DASHBOARD  ");
        assertEquals(UiAssertionStatus.PASSED,
                new UiPageExpect(exact.driver(), options, OverlayLogger.noop()).toHaveTitle("order dashboard").status());

        PageDriver contains = PageDriver.titles("Ready For Checkout");
        assertEquals(UiAssertionStatus.PASSED,
                new UiPageExpect(contains.driver(), options, OverlayLogger.noop()).toContainTitle("for check").status());
        assertEquals(1, exact.titleReads());
        assertEquals(1, contains.titleReads());
    }

    @Test
    void textOptionsDoNotNormalizeUrls() {
        UiAssertionOptions textOptions = options().normalizeWhitespace(true).trimText(true).caseSensitive(false).build();
        UiAssertionError mismatch = assertThrows(UiAssertionError.class, () ->
                new UiPageExpect(PageDriver.urls("https://example.test/DashBoard").driver(), textOptions,
                        OverlayLogger.noop()).toHaveUrl("https://example.test/dashboard"));
        assertEquals(UiAssertionFailureReason.URL_MISMATCH, mismatch.result().failureReason());
    }

    @Test
    void pollingFindsDelayedUrlAndCountsExactlyOneReadPerAttempt() {
        PageDriver page = PageDriver.urls("https://example.test/loading", "https://example.test/loading",
                "https://example.test/ready");
        InMemoryLogSink sink = new InMemoryLogSink();
        UiAssertionResult result = new UiPageExpect(page.driver(), options().build(), logger(sink))
                .toHaveUrl("https://example.test/ready");

        assertEquals(3, result.attempts());
        assertEquals(result.attempts(), page.urlReads());
        assertEquals(2, count(sink, UiTestLensEventType.ASSERTION_RETRY));
        assertEquals(1, count(sink, UiTestLensEventType.ASSERTION_STARTED));
        assertEquals(1, count(sink, UiTestLensEventType.ASSERTION_PASSED));
    }

    @Test
    void pollingFindsDelayedTitleAndCountsExactlyOneReadPerAttempt() {
        PageDriver page = PageDriver.titles("Loading", "Loading", "Ready");
        UiAssertionResult result = expect(page).toHaveTitle("Ready");
        assertEquals(3, result.attempts());
        assertEquals(result.attempts(), page.titleReads());
    }

    @Test
    void timeoutRetainsLastMismatchWithoutExtraDiagnosticRead() {
        PageDriver page = PageDriver.urls("https://example.test/one", "https://example.test/two");
        UiAssertionError error = assertThrows(UiAssertionError.class,
                () -> expect(page).toHaveUrl("https://example.test/expected"));

        assertEquals(UiAssertionStatus.TIMED_OUT, error.result().status());
        assertEquals(UiAssertionFailureReason.URL_MISMATCH, error.result().failureReason());
        assertEquals(page.urlReads(), error.result().attempts());
        assertTrue(error.result().actualPreview().contains("/two"));
        assertTrue(error.getCause() instanceof org.openqa.selenium.TimeoutException);
    }

    @Test
    void terminalDriverFailureIsImmediateAndPreserved() {
        NoSuchWindowException original = new NoSuchWindowException("window is gone");
        PageDriver page = PageDriver.failure(original);
        InMemoryLogSink sink = new InMemoryLogSink();

        UiAssertionError error = assertThrows(UiAssertionError.class,
                () -> new UiPageExpect(page.driver(), options().build(), logger(sink)).toHaveTitle("Ready"));

        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertEquals(1, error.result().attempts());
        assertEquals(1, page.titleReads());
        assertSame(original, error.getCause());
        assertEquals(0, count(sink, UiTestLensEventType.ASSERTION_RETRY));
        assertEquals(1, count(sink, UiTestLensEventType.ASSERTION_FAILED));
    }

    @Test
    void failFastMissingElementOptionDoesNotAffectPagePolling() {
        PageDriver page = PageDriver.titles("Loading", "Ready");
        UiAssertionOptions options = options().failFastOnMissingElement(true).build();
        UiAssertionResult result = new UiPageExpect(page.driver(), options, OverlayLogger.noop()).toHaveTitle("Ready");
        assertEquals(2, result.attempts());
    }

    @Test
    void pagePollingDoesNotProduceRecoveryRetryOrFlakySummary() {
        PageDriver page = PageDriver.urls("https://example.test/loading", "https://example.test/ready");
        TestLens lens = TestLens.attach(page.driver(), OverlayConfig.builder().enabled(false).build());
        UiTestLensSession session = lens.startSession("page polling");

        lens.expectPage(options().build()).toContainUrl("/ready");

        assertEquals(0, session.retrySummary().totalRetries());
        assertFalse(session.retrySummary().flakyCandidate());
        assertTrue(session.events().stream().noneMatch(event -> event.type().name().equals("RETRY")));
    }

    @Test
    void urlDiagnosticsRemoveCredentialsQueryAndFragmentEverywhere() {
        String secretUrl = "https://alice:password@example.test/private/path?token=super-secret#hidden";
        InMemoryLogSink sink = new InMemoryLogSink();
        UiAssertionError error = assertThrows(UiAssertionError.class, () ->
                new UiPageExpect(PageDriver.urls(secretUrl).driver(), options().build(), logger(sink))
                        .toContainUrl("token=other-secret"));
        String diagnostics = error.getMessage() + error.result().summary() + sink.entries();

        assertTrue(diagnostics.contains("example.test/private/path"));
        assertFalse(diagnostics.contains("alice"));
        assertFalse(diagnostics.contains("password"));
        assertFalse(diagnostics.contains("token="));
        assertFalse(diagnostics.contains("super-secret"));
        assertFalse(diagnostics.contains("hidden"));
    }

    @Test
    void malformedUrlUsesOnlyLengthFallback() {
        String malformed = "not a url ?token=secret value";
        UiAssertionError error = assertThrows(UiAssertionError.class,
                () -> expect(PageDriver.urls(malformed)).toHaveUrl("different"));
        assertEquals("url[length=" + malformed.length() + "]", error.result().actualPreview());
        assertFalse(error.getMessage().contains(malformed));
        assertFalse(error.getMessage().contains("secret"));
    }

    @Test
    void bothFacadesCreatePageAssertionsWithSessionLoggerAndRequestedOptions() {
        PageDriver testLensPage = PageDriver.titles("Loading", "Ready");
        TestLens lens = TestLens.attach(testLensPage.driver(), OverlayConfig.builder().enabled(false).build());
        UiTestLensSession lensSession = lens.startSession("test lens page");
        UiAssertionResult lensResult = lens.expectPage(options().build()).toHaveTitle("Ready");
        assertEquals(2, lensResult.attempts());
        assertTrue(lensSession.events().stream().anyMatch(event -> event.type().name().equals("ASSERTION_PASSED")));

        PageDriver legacyPage = PageDriver.urls("https://example.test/loading", "https://example.test/ready");
        JsOverlayDebug legacy = new JsOverlayDebug(legacyPage.driver(), OverlayConfig.builder().enabled(false).build());
        UiTestLensSession legacySession = legacy.startSession("legacy page");
        UiAssertionResult legacyResult = legacy.expectPage(options().build()).toContainUrl("/ready");
        assertEquals(2, legacyResult.attempts());
        assertTrue(legacySession.events().stream().anyMatch(event -> event.type().name().equals("ASSERTION_PASSED")));
    }

    private static UiPageExpect expect(PageDriver page) {
        return new UiPageExpect(page.driver(), options().build(), OverlayLogger.noop());
    }

    private static UiAssertionOptions.Builder options() {
        return UiAssertionOptions.builder().timeout(Duration.ofMillis(40)).pollInterval(Duration.ofMillis(2));
    }

    private static OverlayLogger logger(InMemoryLogSink sink) {
        return OverlayLogger.from(UiTestLensLogger.builder().sink(sink).build());
    }

    private static long count(InMemoryLogSink sink, UiTestLensEventType type) {
        return sink.entries().stream().filter(entry -> entry.eventType() == type).count();
    }

    private static final class PageDriver {
        private final List<String> urls;
        private final List<String> titles;
        private final RuntimeException failure;
        private final AtomicInteger urlReads = new AtomicInteger();
        private final AtomicInteger titleReads = new AtomicInteger();
        private final WebDriver driver;

        private PageDriver(List<String> urls, List<String> titles, RuntimeException failure) {
            this.urls = new ArrayList<>(urls);
            this.titles = new ArrayList<>(titles);
            this.failure = failure;
            this.driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getCurrentUrl" -> next(this.urls, urlReads);
                        case "getTitle" -> {
                            titleReads.incrementAndGet();
                            if (failure != null) throw failure;
                            yield valueAt(this.titles, titleReads.get() - 1);
                        }
                        case "executeScript", "executeAsyncScript" -> null;
                        case "toString" -> "PageDriver";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        static PageDriver urls(String... values) {
            return new PageDriver(List.of(values), List.of(""), null);
        }

        static PageDriver titles(String... values) {
            return new PageDriver(List.of(""), List.of(values), null);
        }

        static PageDriver failure(RuntimeException failure) {
            return new PageDriver(List.of(""), List.of(""), failure);
        }

        private String next(List<String> values, AtomicInteger counter) {
            int index = counter.getAndIncrement();
            if (failure != null) throw failure;
            return valueAt(values, index);
        }

        private static String valueAt(List<String> values, int index) {
            return values.get(Math.min(index, values.size() - 1));
        }

        WebDriver driver() { return driver; }
        int urlReads() { return urlReads.get(); }
        int titleReads() { return titleReads.get(); }
    }
}
