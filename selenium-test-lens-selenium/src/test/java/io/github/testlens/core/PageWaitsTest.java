package io.github.testlens.core;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Sleeper;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PageWaitsTest {
    @Test
    void documentReadyPollsOneFreshReadyStateObservationPerAttempt() {
        ScriptDriver browser = new ScriptDriver(List.of("loading", NullValue.INSTANCE, "complete"), List.of());
        Fixture fixture = fixture(browser, Duration.ofMillis(30), Duration.ofMillis(5));
        fixture.waits.waitForDocumentReady();
        assertEquals(3, browser.readyReads.get());
        assertEquals(10, fixture.clock.elapsedMillis());
        assertTerminalEvents(fixture, UiTestLensStatus.PASSED, 3);
    }

    @Test
    void interactiveWaitAcceptsInteractiveAndRejectsUnknownStateUntilNextPoll() {
        ScriptDriver browser = new ScriptDriver(List.of("prerender", "interactive"), List.of());
        Fixture fixture = fixture(browser, Duration.ofMillis(20), Duration.ofMillis(4));
        fixture.waits.waitForInteractiveOrComplete();
        assertEquals(2, browser.readyReads.get());
        assertEquals(4, fixture.clock.elapsedMillis());
    }

    @Test
    void networkIdleTimeoutThrowsInsteadOfReturningFalseSuccess() {
        ScriptDriver browser = new ScriptDriver(List.of(), List.of(1L));
        Fixture fixture = fixture(browser, Duration.ofMillis(10), Duration.ofMillis(5));
        TimeoutException failure = assertThrows(TimeoutException.class,
                () -> fixture.waits.waitForNetworkIdle(Duration.ofMillis(2), Duration.ofMillis(10)));
        assertTrue(failure.getMessage().contains("XHR/fetch"));
        assertEquals(3, browser.activeReads.get());
        assertTerminalEvents(fixture, UiTestLensStatus.FAILED, 3);
        assertEquals("TIMEOUT", fixture.sink.entries().get(1).metadata().get("reason"));
        assertEquals(0, fixture.sink.entries().stream()
                .filter(entry -> entry.eventType().name().contains("RETRY")).count());
    }

    @Test
    void networkIdleRequiresZeroForTheWholeIdleWindow() {
        ScriptDriver browser = new ScriptDriver(List.of(), List.of(1L, 0L, 0L, 0L));
        Fixture fixture = fixture(browser, Duration.ofMillis(30), Duration.ofMillis(5));
        fixture.waits.waitForNetworkIdle(Duration.ofMillis(10), Duration.ofMillis(30));
        assertEquals(4, browser.activeReads.get());
        assertEquals(15, fixture.clock.elapsedMillis());
        assertEquals(1, browser.trackerInstalls.get());
    }

    @Test
    void terminalJavascriptFailureIsImmediateAndPreserved() {
        WebDriverException canary = new WebDriverException("terminal canary");
        ScriptDriver browser = new ScriptDriver(List.of(canary), List.of());
        Fixture fixture = fixture(browser, Duration.ofSeconds(1), Duration.ofMillis(5));
        WebDriverException actual = assertThrows(WebDriverException.class, fixture.waits::waitForDocumentReady);
        assertSame(canary, actual);
        assertEquals(1, browser.readyReads.get());
        assertEquals(0, fixture.clock.elapsedMillis());
        assertTerminalEvents(fixture, UiTestLensStatus.FAILED, 1);
        assertEquals("TERMINAL_ERROR", fixture.sink.entries().get(1).metadata().get("reason"));
    }

    @Test
    void explicitTimeoutAndValidationUseARealTotalBudget() {
        ScriptDriver browser = new ScriptDriver(List.of("loading"), List.of());
        Fixture fixture = fixture(browser, Duration.ofSeconds(3), Duration.ofMillis(7));
        assertThrows(TimeoutException.class, () -> fixture.waits.waitForDocumentReady(Duration.ofMillis(15)));
        assertEquals(15, fixture.clock.elapsedMillis());
        assertEquals("15", fixture.sink.entries().get(0).metadata().get("timeoutMs"));
        assertEquals("7", fixture.sink.entries().get(0).metadata().get("pollIntervalMs"));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.waits.waitForDocumentReady(Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.waits.waitForNetworkIdle(Duration.ofMillis(-1), Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new PageWaits(browser.driver(), disabledOverlay(),
                Duration.ofSeconds(1), Duration.ZERO, OverlayLogger.noop(), fixture.clock, fixture.sleeper));
    }

    @Test
    void compositeReactWaitSharesOneDeadlineAcrossAllStages() {
        ScriptDriver browser = new ScriptDriver(List.of(), List.of());
        browser.element = element();
        browser.rootMounted = true;
        browser.domAgeMillis = 0L;
        Fixture fixture = fixture(browser, Duration.ofSeconds(2), Duration.ofMillis(5));

        assertThrows(TimeoutException.class,
                () -> fixture.waits.waitForReactComponentVisible(
                        org.openqa.selenium.By.id("root"), org.openqa.selenium.By.id("component"),
                        Duration.ofMillis(20)));

        assertEquals(20, fixture.clock.elapsedMillis());
        assertEquals("20", fixture.sink.entries().get(1).metadata().get("elapsedMs"));
        assertEquals(2, fixture.sink.entries().size());
    }

    @Test
    void hudIsAlwaysCleanedAndUsesTextNodesInsteadOfHtml() {
        ScriptDriver browser = new ScriptDriver(List.of(new WebDriverException("boom")), List.of());
        Fixture fixture = fixture(browser, Duration.ofMillis(10), Duration.ofMillis(2));
        assertThrows(WebDriverException.class, fixture.waits::waitForDocumentReady);
        assertTrue(browser.scripts.stream().anyMatch(script -> script.contains("hideIndicator")));
        assertFalse(browser.scripts.stream().anyMatch(script -> script.contains("innerHTML")));
    }

    @Test
    void trackerIsIdempotentAndBalancesEveryCompletionPathWithoutGoingNegative() {
        String script = PageWaits.networkTrackerScript();
        assertTrue(script.contains("trackerInstalled"));
        assertTrue(script.contains("finishOnce"));
        assertTrue(script.contains("loadend"));
        assertTrue(script.contains("catch (failure) { finishOnce(); throw failure; }"));
        assertTrue(script.contains("catch (failure) { finishRequest(); throw failure; }"));
        assertTrue(script.contains("function(failure) { finishRequest(); throw failure; }"));
        assertTrue(script.contains("origFetch.apply(window, arguments)"));
        assertTrue(script.contains("Math.max(0"));
    }

    @Test
    void namespaceAndStateScriptsKeepCompatibilityAliases() {
        String namespace = UiTestLensRuntimeNames.ensureNamespaceScript();
        assertTrue(namespace.contains("window.__uiTestLens"));
        assertTrue(namespace.contains("state.wait"));
        assertTrue(namespace.contains("state.network"));
        assertTrue(namespace.contains("state.dom"));
        assertTrue(PageWaits.rememberLastWaitMessageScript().contains("__seleniumLastWaitMessage"));
        assertTrue(PageWaits.networkActiveRequestsScript().contains("__seleniumActiveRequests"));
        assertTrue(PageWaits.domStableMutationScript().contains("MutationObserver"));
    }

    private static void assertTerminalEvents(Fixture fixture, UiTestLensStatus terminal, long attempts) {
        assertEquals(2, fixture.sink.entries().size());
        assertEquals(UiTestLensEventType.WAIT, fixture.sink.entries().get(0).eventType());
        assertEquals(UiTestLensStatus.STARTED, fixture.sink.entries().get(0).status());
        assertEquals(terminal, fixture.sink.entries().get(1).status());
        assertEquals(String.valueOf(attempts), fixture.sink.entries().get(1).metadata().get("attempts"));
    }

    private static Fixture fixture(ScriptDriver browser, Duration timeout, Duration poll) {
        ManualClock clock = new ManualClock();
        AdvancingSleeper sleeper = new AdvancingSleeper(clock);
        InMemoryLogSink sink = new InMemoryLogSink();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(sink).build());
        PageWaits waits = new PageWaits(browser.driver(), disabledOverlay(), timeout, poll, logger, clock, sleeper);
        return new Fixture(waits, clock, sleeper, sink);
    }

    private static OverlayConfig disabledOverlay() {
        return OverlayConfig.builder().enabled(false).showHudPanel(false).build();
    }

    private static WebElement element() {
        return (WebElement) Proxy.newProxyInstance(PageWaitsTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isDisplayed")) return true;
                    if (method.getName().equals("toString")) return "Element";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    return ScriptDriver.defaultValue(method.getReturnType());
                });
    }

    private record Fixture(PageWaits waits, ManualClock clock, AdvancingSleeper sleeper, InMemoryLogSink sink) {}

    private static final class ManualClock extends Clock {
        private Instant now = Instant.EPOCH;
        private void advance(Duration duration) { now = now.plus(duration); }
        private long elapsedMillis() { return Duration.between(Instant.EPOCH, now).toMillis(); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private record AdvancingSleeper(ManualClock clock) implements Sleeper {
        @Override public void sleep(Duration duration) { clock.advance(duration); }
    }

    private static final class ScriptDriver {
        private final Deque<Object> readyStates = new ArrayDeque<>();
        private final Deque<Object> activeRequests = new ArrayDeque<>();
        private final AtomicInteger readyReads = new AtomicInteger();
        private final AtomicInteger activeReads = new AtomicInteger();
        private final AtomicInteger trackerInstalls = new AtomicInteger();
        private final List<String> scripts = new ArrayList<>();
        private WebElement element;
        private boolean rootMounted;
        private long domAgeMillis;
        private final WebDriver driver;

        private ScriptDriver(List<?> readyStates, List<?> activeRequests) {
            this.readyStates.addAll(readyStates);
            this.activeRequests.addAll(activeRequests);
            this.driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                        if (method.getName().equals("executeScript")) return execute((String) args[0]);
                        if (method.getName().equals("findElement")) return element;
                        if (method.getName().equals("toString")) return "ScriptDriver";
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        return defaultValue(method.getReturnType());
                    });
        }

        private WebDriver driver() { return driver; }

        private Object execute(String script) {
            scripts.add(script);
            if (script.equals("return document.readyState")) {
                readyReads.incrementAndGet();
                return next(readyStates);
            }
            if (script.equals(PageWaits.networkActiveRequestsScript())) {
                activeReads.incrementAndGet();
                return next(activeRequests);
            }
            if (script.equals(PageWaits.networkTrackerScript())) trackerInstalls.incrementAndGet();
            if (script.contains("arguments[0].children")) return rootMounted;
            if (script.equals(PageWaits.domStableMutationScript())) return domAgeMillis;
            return null;
        }

        private static Object next(Deque<Object> values) {
            Object value = values.size() > 1 ? values.removeFirst() : values.peekFirst();
            if (value == NullValue.INSTANCE) return null;
            if (value instanceof RuntimeException failure) throw failure;
            return value;
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            return 0;
        }
    }

    private enum NullValue { INSTANCE }
}
