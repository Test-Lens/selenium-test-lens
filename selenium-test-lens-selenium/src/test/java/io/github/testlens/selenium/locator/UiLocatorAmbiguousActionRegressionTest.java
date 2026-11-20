package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.OverlayLogger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UiLocatorAmbiguousActionRegressionTest {
    @Test
    void genericClickFailureWithRetryableCauseDoesNotRedispatch() {
        for (WebDriverException nested : retryableCauses()) {
            WebDriverException failure = new WebDriverException("click response lost", nested);
            Fixture fixture = fixture("click", failure, false);
            UiLocatorException actual = assertThrows(UiLocatorException.class, fixture.locator::click);
            assertSame(failure, actual.getCause());
            assertEquals(1, fixture.dispatched.get());
        }
    }

    @Test
    void genericInputFailureWithRetryableCauseDoesNotRedispatch() {
        for (WebDriverException nested : retryableCauses()) {
            WebDriverException failure = new WebDriverException("input response lost", nested);
            Fixture fixture = fixture("sendKeys", failure, false);
            UiLocatorException actual = assertThrows(UiLocatorException.class, () -> fixture.locator.fill("X"));
            assertSame(failure, actual.getCause());
            assertEquals(1, fixture.dispatched.get());
        }
    }

    @Test
    void successfulCheckboxClickIsNotRepeatedAfterConfirmationStalesAndTimesOut() {
        Fixture fixture = fixture("click", null, true);
        assertThrows(UiLocatorException.class, fixture.locator::check);
        assertEquals(1, fixture.dispatched.get());
    }
    private static List<WebDriverException> retryableCauses() {
        return List.of(new ElementClickInterceptedException("earlier intercepted command"),
                new StaleElementReferenceException("earlier stale command"));
    }

    private static Fixture fixture(String command, WebDriverException failure, boolean checkbox) {
        AtomicInteger dispatched = new AtomicInteger();
        java.util.concurrent.atomic.AtomicLong staleUntil = new java.util.concurrent.atomic.AtomicLong();
        WebElement element = (WebElement) Proxy.newProxyInstance(UiLocatorAmbiguousActionRegressionTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals(command)) {
                        dispatched.incrementAndGet();
                        if (checkbox) staleUntil.set(System.nanoTime() + Duration.ofMillis(200).toNanos());
                        if (failure != null) throw failure;
                        return null;
                    }
                    return switch (method.getName()) {
                        case "isDisplayed", "isEnabled" -> true;
                        case "getTagName" -> checkbox ? "input" : "button";
                        case "getDomAttribute" -> checkbox && "type".equals(args[0]) ? "checkbox" : null;
                        case "isSelected" -> false;
                        case "getRect" -> new Rectangle(10, 10, 100, 30);
                        case "toString" -> "action-target";
                        default -> null;
                    };
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(UiLocatorAmbiguousActionRegressionTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) {
                        if (System.nanoTime() < staleUntil.get()) throw new StaleElementReferenceException("confirmation lookup stale");
                        return element;
                    }
                    if (method.getName().equals("findElements")) return List.of(element);
                    if (method.getName().equals("executeScript")) {
                        if (String.valueOf(args[0]).contains("arguments[0].isConnected")) return true;
                        return null;
                    }
                    if (method.getName().equals("toString")) return "action-driver";
                    return null;
                });
        OverlayConfig config = OverlayConfig.builder().enabled(false).showHudPanel(false)
                .globalOverlayCloseButtonSelector("").build();
        JsOverlayDebug overlay = new JsOverlayDebug(driver, config);
        UiLocator locator = new UiLocator(driver, By.id("target"), "Target", overlay,
                UiLocatorOptions.builder().timeout(Duration.ofMillis(80)).pollInterval(Duration.ofMillis(1))
                        .maxRetries(3).build(), OverlayLogger.noop(), System::nanoTime);
        return new Fixture(locator, dispatched);
    }

    private record Fixture(UiLocator locator, AtomicInteger dispatched) {}
}
