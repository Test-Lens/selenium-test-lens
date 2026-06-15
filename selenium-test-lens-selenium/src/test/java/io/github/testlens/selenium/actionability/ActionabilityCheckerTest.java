package io.github.testlens.selenium.actionability;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.selenium.overlay.OverlayAction;
import io.github.testlens.selenium.overlay.OverlayHandler;
import io.github.testlens.selenium.overlay.OverlayPolicy;
import io.github.testlens.selenium.overlay.OverlayPolicyExecutor;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionabilityCheckerTest {

    @Test
    void readyPathPassesAllConfiguredChecks() {
        FakeBrowser browser = FakeBrowser.ready();

        ActionabilityReport report = new ActionabilityChecker(browser.driver(), null, OverlayLogger.noop())
                .check(browser.element(), fastOptions().checkOverlayPolicy(false).build());

        assertTrue(report.isReady());
        assertEquals(ActionabilityStatus.READY, report.status());
    }

    @Test
    void invisibleElementIsNotReady() {
        FakeBrowser browser = FakeBrowser.ready().displayed(false);

        ActionabilityReport report = new ActionabilityChecker(browser.driver(), null, OverlayLogger.noop())
                .check(browser.element(), fastOptions().checkOverlayPolicy(false).build());

        assertFalse(report.isReady());
        assertEquals(ActionabilityFailureReason.ELEMENT_NOT_VISIBLE, report.firstFailure().orElseThrow().failureReason());
    }

    @Test
    void disabledElementIsNotReady() {
        FakeBrowser browser = FakeBrowser.ready().enabled(false);

        ActionabilityReport report = new ActionabilityChecker(browser.driver(), null, OverlayLogger.noop())
                .check(browser.element(), fastOptions().checkOverlayPolicy(false).build());

        assertFalse(report.isReady());
        assertEquals(ActionabilityFailureReason.ELEMENT_NOT_ENABLED, report.firstFailure().orElseThrow().failureReason());
    }

    @Test
    void coveredClickPointIsNotReady() {
        FakeBrowser browser = FakeBrowser.ready().clickPoint(Map.of(
                "receives", false,
                "topElement", "div.modal",
                "x", 10,
                "y", 10
        ));

        ActionabilityReport report = new ActionabilityChecker(browser.driver(), null, OverlayLogger.noop())
                .check(browser.element(), fastOptions().checkOverlayPolicy(false).build());

        assertFalse(report.isReady());
        assertEquals(ActionabilityFailureReason.ELEMENT_COVERED, report.firstFailure().orElseThrow().failureReason());
    }

    @Test
    void unstableBoundsAreNotReady() {
        FakeBrowser browser = FakeBrowser.ready()
                .rects(
                        rect(10, 10, 100, 30),
                        rect(25, 10, 100, 30)
                );

        ActionabilityReport report = new ActionabilityChecker(browser.driver(), null, OverlayLogger.noop())
                .check(browser.element(), fastOptions().checkOverlayPolicy(false).build());

        assertFalse(report.isReady());
        assertEquals(ActionabilityFailureReason.ELEMENT_NOT_STABLE, report.firstFailure().orElseThrow().failureReason());
    }

    @Test
    void fatalOverlayPolicyFailureMarksReportFailed() {
        FakeBrowser browser = FakeBrowser.ready();
        OverlayPolicy policy = OverlayPolicy.builder()
                .handler(OverlayHandler.builder("Session expired")
                        .detect(By.cssSelector("[data-testid='session-expired']"))
                        .action(OverlayAction.fail("Session expired popup detected"))
                        .optional(false)
                        .build())
                .build();
        OverlayPolicyExecutor policyExecutor = new OverlayPolicyExecutor(browser.driver(), policy, OverlayLogger.noop());

        ActionabilityReport report = new ActionabilityChecker(browser.driver(), policyExecutor, OverlayLogger.noop())
                .check(browser.element(), fastOptions().build());

        assertEquals(ActionabilityStatus.FAILED, report.status());
        assertEquals(ActionabilityFailureReason.BLOCKING_OVERLAY_DETECTED, report.firstFailure().orElseThrow().failureReason());
    }

    private static ActionabilityOptions.Builder fastOptions() {
        return ActionabilityOptions.builder()
                .stableBoundsSampleDelay(Duration.ZERO)
                .timeout(Duration.ofMillis(100))
                .pollInterval(Duration.ofMillis(10));
    }

    private static Map<String, Object> rect(double x, double y, double width, double height) {
        return Map.of(
                "x", x,
                "y", y,
                "width", width,
                "height", height,
                "left", x,
                "top", y,
                "right", x + width,
                "bottom", y + height,
                "inViewport", true
        );
    }

    private static final class FakeBrowser {
        private boolean attached = true;
        private boolean displayed = true;
        private boolean enabled = true;
        private Map<String, Object> clickPoint = Map.of("receives", true, "topElement", "", "x", 10, "y", 10);
        private Queue<Map<String, Object>> rects = new ArrayDeque<>(List.of(rect(10, 10, 100, 30), rect(10, 10, 100, 30)));
        private WebElement element;
        private WebDriver driver;

        private static FakeBrowser ready() {
            return new FakeBrowser();
        }

        private FakeBrowser displayed(boolean displayed) {
            this.displayed = displayed;
            return this;
        }

        private FakeBrowser enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        private FakeBrowser clickPoint(Map<String, Object> clickPoint) {
            this.clickPoint = clickPoint;
            return this;
        }

        private FakeBrowser rects(Map<String, Object> first, Map<String, Object> second) {
            this.rects = new ArrayDeque<>(List.of(first, second));
            return this;
        }

        private WebElement element() {
            if (element == null) {
                element = (WebElement) Proxy.newProxyInstance(
                        ActionabilityCheckerTest.class.getClassLoader(),
                        new Class<?>[]{WebElement.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "isDisplayed" -> displayed;
                            case "isEnabled" -> enabled;
                            case "toString" -> "FakeWebElement";
                            default -> throw new UnsupportedOperationException(method.getName());
                        });
            }
            return element;
        }

        private WebDriver driver() {
            WebElement webElement = element();
            if (driver == null) {
                driver = (WebDriver) Proxy.newProxyInstance(
                        ActionabilityCheckerTest.class.getClassLoader(),
                        new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "executeScript" -> executeScript(String.valueOf(args[0]));
                            case "findElements" -> List.of(webElement);
                            case "toString" -> "FakeWebDriver";
                            default -> throw new UnsupportedOperationException(method.getName());
                        });
            }
            return driver;
        }

        private Object executeScript(String script) {
            if (ActionabilityScripts.IS_ATTACHED.equals(script)) {
                return attached;
            }
            if (ActionabilityScripts.SCROLL_INTO_VIEW.equals(script)) {
                return true;
            }
            if (ActionabilityScripts.BOUNDING_RECT.equals(script)) {
                return rects.isEmpty() ? rect(10, 10, 100, 30) : rects.remove();
            }
            if (ActionabilityScripts.CLICK_POINT.equals(script)) {
                return clickPoint;
            }
            throw new UnsupportedOperationException(script);
        }
    }
}

