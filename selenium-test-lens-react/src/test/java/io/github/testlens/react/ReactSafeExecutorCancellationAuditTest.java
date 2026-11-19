package io.github.testlens.react;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReactSafeExecutorCancellationAuditTest {
    @Test void interruptionAtRetryDelayBoundaryIsPropagatedWithoutAnotherAttempt() {
        AtomicInteger lookups = new AtomicInteger();
        AtomicInteger actions = new AtomicInteger();
        ReactSafeExecutor executor = executor(lookups, (action, locator, attempt, next, failure, nanos) ->
                Thread.currentThread().interrupt());
        try {
            WebDriverException failure = assertThrows(WebDriverException.class,
                    () -> executor.doWithRetry(By.id("target"), "cancelled retry delay", element -> {
                        actions.incrementAndGet();
                        throw new StaleElementReferenceException("rerender");
                    }));
            assertInstanceOf(InterruptedException.class, failure.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, lookups.get());
            assertEquals(1, actions.get());
        } finally { Thread.interrupted(); }
    }

    @Test void retryInterruptionStopsBeforeAnotherLookupOrAction() {
        AtomicInteger lookups = new AtomicInteger();
        AtomicInteger actions = new AtomicInteger();
        ReactSafeExecutor executor = executor(lookups);
        try {
            assertThrows(WebDriverException.class, () -> executor.doWithRetry(By.id("target"), "cancelled input", element -> {
                actions.incrementAndGet();
                Thread.currentThread().interrupt();
                throw new StaleElementReferenceException("rerender then cancellation");
            }));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, lookups.get(), "Cancellation must prevent the next Selenium lookup");
            assertEquals(1, actions.get(), "Cancellation must prevent another operation attempt");
        } finally { Thread.interrupted(); }
    }

    @Test void alreadyInterruptedThreadNeverStartsSeleniumWork() {
        AtomicInteger lookups = new AtomicInteger();
        AtomicInteger actions = new AtomicInteger();
        ReactSafeExecutor executor = executor(lookups);
        try {
            Thread.currentThread().interrupt();
            assertThrows(WebDriverException.class, () -> executor.doWithRetry(By.id("target"), "cancelled action", element -> actions.incrementAndGet()));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(0, lookups.get());
            assertEquals(0, actions.get());
        } finally { Thread.interrupted(); }
    }

    private static ReactSafeExecutor executor(AtomicInteger lookups) {
        return executor(lookups, ReactSafeExecutor.RetryTraceSink.noop());
    }

    private static ReactSafeExecutor executor(AtomicInteger lookups, ReactSafeExecutor.RetryTraceSink sink) {
        WebElement element = (WebElement) Proxy.newProxyInstance(ReactSafeExecutorCancellationAuditTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "isDisplayed", "isEnabled" -> true;
                    case "toString" -> "react cancellation fixture";
                    default -> null;
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(ReactSafeExecutorCancellationAuditTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) { lookups.incrementAndGet(); return element; }
                    return null;
                });
        return new ReactSafeExecutor(driver, null, 3, Duration.ofMillis(100), Duration.ofSeconds(1), sink);
    }
}
