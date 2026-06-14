package io.github.mmaciekk111.uitestlens.selenium.overlay;

import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayPolicyExecutorTest {

    @Test
    void emptyPolicyDoesNotTouchDriver() {
        WebDriver driver = driverThrowingOnFindElements();

        List<OverlayHandlingResult> results = new OverlayPolicyExecutor(driver, OverlayPolicy.none(), OverlayLogger.noop())
                .handleKnownOverlays();

        assertTrue(results.isEmpty());
    }

    @Test
    void optionalNotDetectedDoesNotFail() {
        OverlayPolicy policy = OverlayPolicy.builder()
                .handler(OverlayHandler.builder("Cookie consent")
                        .detect(By.cssSelector("[data-testid='cookie-banner']"))
                        .action(OverlayAction.pressEscape())
                        .build())
                .build();

        List<OverlayHandlingResult> results = new OverlayPolicyExecutor(driverReturning(List.of()), policy, OverlayLogger.noop())
                .handleKnownOverlays();

        assertEquals(1, results.size());
        assertEquals(OverlayHandlingStatus.NOT_DETECTED, results.get(0).status());
    }

    @Test
    void detectedFailActionReturnsFailed() {
        OverlayPolicy policy = OverlayPolicy.builder()
                .handler(OverlayHandler.builder("Session expired")
                        .detect(By.cssSelector("[data-testid='session-expired']"))
                        .action(OverlayAction.fail("Session expired popup detected"))
                        .optional(false)
                        .build())
                .build();

        List<OverlayHandlingResult> results = new OverlayPolicyExecutor(driverReturning(List.of(visibleElement())), policy, OverlayLogger.noop())
                .handleKnownOverlays();

        assertEquals(1, results.size());
        assertEquals(OverlayHandlingStatus.FAILED, results.get(0).status());
        assertFalse(results.get(0).attemptedActions().isEmpty());
    }

    private static WebDriver driverReturning(List<WebElement> elements) {
        return (WebDriver) Proxy.newProxyInstance(
                OverlayPolicyExecutorTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> {
                    if ("findElements".equals(method.getName())) {
                        return elements;
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeWebDriver";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static WebDriver driverThrowingOnFindElements() {
        return (WebDriver) Proxy.newProxyInstance(
                OverlayPolicyExecutorTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> {
                    if ("findElements".equals(method.getName())) {
                        throw new AssertionError("findElements should not be called for empty policy");
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeWebDriver";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static WebElement visibleElement() {
        return (WebElement) Proxy.newProxyInstance(
                OverlayPolicyExecutorTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    if ("isDisplayed".equals(method.getName())) {
                        return true;
                    }
                    if ("toString".equals(method.getName())) {
                        return "VisibleWebElement";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
