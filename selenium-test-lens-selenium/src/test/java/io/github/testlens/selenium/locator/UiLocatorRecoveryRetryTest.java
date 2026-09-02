package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceLogSink;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

class UiLocatorRecoveryRetryTest {
    @Test
    void firstAttemptSuccessAndTerminalFailureProduceNoRetry() {
        Harness success = harness(3, null, 0, 10);
        assertEquals("ok", success.locator.textContent());
        assertEquals(0, success.session.retrySummary().totalRetries());

        Harness terminal = harness(3, new WebDriverException("terminal"), Integer.MAX_VALUE, 0, 10);
        assertThrows(UiLocatorException.class, terminal.locator::textContent);
        assertEquals(0, terminal.session.retrySummary().totalRetries());
    }

    @Test
    void successAfterOneAndSeveralFailuresRecordsOnlyScheduledRetries() {
        Harness one = harness(3, new StaleElementReferenceException("stale"), 1, 0, 12_000_000, 20_000_000);
        assertEquals("ok", one.locator.textContent());
        assertEquals(1, one.session.retrySummary().totalRetries());
        assertEquals(Duration.ofMillis(12), one.session.retrySummary().timeLost());

        Harness several = harness(3, new StaleElementReferenceException("stale"), 2,
                0, 3_000_000, 4_000_000, 11_000_000, 12_000_000);
        assertEquals("ok", several.locator.textContent());
        assertEquals(2, several.session.retrySummary().totalRetries());
        assertEquals(Duration.ofMillis(10), several.session.retrySummary().timeLost());
    }

    @Test
    void threeFailedAttemptsProduceTwoRetriesWithStableAttemptNumbers() {
        Harness harness = harness(3, new StaleElementReferenceException("stale"), Integer.MAX_VALUE,
                0, 1, 2, 3, 4);
        assertThrows(UiLocatorException.class, harness.locator::textContent);

        List<TraceEvent> retries = retries(harness.session);
        assertEquals(2, retries.size());
        assertEquals("1", retries.get(0).attributes().get("retry.attempt"));
        assertEquals("2", retries.get(0).attributes().get("retry.nextAttempt"));
        assertEquals("2", retries.get(1).attributes().get("retry.attempt"));
        assertEquals("3", retries.get(1).attributes().get("retry.nextAttempt"));
        assertEquals(3, harness.driverCalls.get());
    }

    @Test
    void wrappedRetryableCauseIsGroupedByEffectiveExceptionType() {
        RuntimeException wrapped = new RuntimeException("wrapper", new StaleElementReferenceException("stale"));
        Harness harness = harness(2, wrapped, 1, 0, 5, 6);
        assertEquals("ok", harness.locator.textContent());
        assertEquals(
                java.util.Map.of(StaleElementReferenceException.class.getName(), 1L),
                harness.session.retrySummary().byException());
        assertEquals(StaleElementReferenceException.class.getName(),
                retries(harness.session).get(0).failure().exceptionType());
    }

    @Test
    void waitAndResolverPollingNeverCreateRecoveryRetry() {
        UiTestLensSession session = UiTestLensSession.start("polling");
        new TraceLogSink(session).accept(io.github.testlens.core.logging.UiTestLensLogEntry.builder()
                .eventType(io.github.testlens.core.logging.UiTestLensEventType.LOCATOR_RETRY)
                .metadata("attempt", "1")
                .build());
        assertEquals(0, session.retrySummary().totalRetries());

        AtomicInteger calls = new AtomicInteger();
        WebElement found = element(null, 0);
        WebDriver driver = driver(calls, found, new org.openqa.selenium.NoSuchElementException("missing"), 2);
        assertSame(found, new UiLocatorResolver(driver).resolve(By.id("eventual"), UiLocatorOptions.builder()
                .timeout(Duration.ofMillis(50)).pollInterval(Duration.ofMillis(1)).build()));
        assertEquals(0, session.retrySummary().totalRetries());
    }

    private static Harness harness(int maxAttempts, RuntimeException failure, int failures, long... ticks) {
        AtomicInteger elementCalls = new AtomicInteger();
        WebElement element = element(failure, failures);
        AtomicInteger driverCalls = new AtomicInteger();
        WebDriver driver = driver(driverCalls, element, null, 0);
        UiTestLensSession session = UiTestLensSession.start("retry");
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder().sink(new TraceLogSink(session)).build());
        AtomicInteger tick = new AtomicInteger();
        LongSupplier ticker = () -> ticks[Math.min(tick.getAndIncrement(), ticks.length - 1)];
        UiLocator locator = new UiLocator(driver, By.id("save"), "Save", new JsOverlayDebug(driver),
                UiLocatorOptions.builder().timeout(Duration.ofMillis(20)).pollInterval(Duration.ofMillis(1))
                        .maxRetries(maxAttempts).build(), logger, ticker);
        return new Harness(locator, session, driverCalls);
    }

    private static WebElement element(RuntimeException failure, int failures) {
        AtomicInteger calls = new AtomicInteger();
        return (WebElement) Proxy.newProxyInstance(UiLocatorRecoveryRetryTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getText")) {
                        if (failure != null && calls.getAndIncrement() < failures) throw failure;
                        return "ok";
                    }
                    if (method.getName().equals("toString")) return "element";
                    return defaultValue(method.getReturnType());
                });
    }

    private static WebDriver driver(AtomicInteger calls, WebElement element, RuntimeException failure, int failures) {
        return (WebDriver) Proxy.newProxyInstance(UiLocatorRecoveryRetryTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) {
                        int call = calls.getAndIncrement();
                        if (failure != null && call < failures) throw failure;
                        return element;
                    }
                    if (method.getName().startsWith("execute")) return null;
                    if (method.getName().equals("toString")) return "driver";
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }

    private static List<TraceEvent> retries(UiTestLensSession session) {
        return session.events().stream().filter(event -> event.type() == TraceEventType.RETRY).toList();
    }

    private record Harness(UiLocator locator, UiTestLensSession session, AtomicInteger driverCalls) {}
}
