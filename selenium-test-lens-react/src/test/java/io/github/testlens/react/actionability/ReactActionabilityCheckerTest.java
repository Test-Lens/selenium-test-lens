package io.github.testlens.react.actionability;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.api.ApiCallActions;
import io.github.testlens.api.ApiOverlayPanel;
import io.github.testlens.core.Guards;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.selenium.actionability.ActionabilityOptions;
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

class ReactActionabilityCheckerTest {

    @Test
    void reportsBaseActionabilityFailureWhenElementIsMissing() {
        FakeBrowser browser = FakeBrowser.ready().elements(List.of());

        ReactActionabilityReport report = new ReactActionabilityChecker(overlay(browser.driver()))
                .check(By.cssSelector("[data-testid='save']"), fastOptions());

        assertFalse(report.isReady());
        assertEquals(ReactReadinessFailureReason.BASE_ACTIONABILITY_NOT_READY,
                report.firstReactFailure().orElseThrow().failureReason());
    }

    @Test
    void reportsAriaBusyAsNotReady() {
        FakeBrowser browser = FakeBrowser.ready()
                .reactSignals(Map.of(
                        "ariaDisabled", inactive(),
                        "ariaBusy", Map.of("active", true, "element", "section[aria-busy=true]"),
                        "dataLoading", inactive(),
                        "dataPending", inactive(),
                        "progressbar", "",
                        "spinner", "",
                        "skeleton", "",
                        "focusLock", "",
                        "dialogOrModal", ""
                ));

        ReactActionabilityReport report = new ReactActionabilityChecker(overlay(browser.driver()))
                .check(By.cssSelector("[data-testid='save']"), fastOptions());

        assertFalse(report.isReady());
        assertEquals(ReactReadinessFailureReason.ARIA_BUSY_TRUE,
                report.firstReactFailure().orElseThrow().failureReason());
    }

    @Test
    void readyWhenBaseAndReactSignalsAreReady() {
        FakeBrowser browser = FakeBrowser.ready();

        ReactActionabilityReport report = new ReactActionabilityChecker(overlay(browser.driver()))
                .check(By.cssSelector("[data-testid='save']"), fastOptions());

        assertTrue(report.isReady());
    }

    private static ReactActionabilityOptions fastOptions() {
        return ReactActionabilityOptions.builder()
                .baseOptions(ActionabilityOptions.builder()
                        .stableBoundsSampleDelay(Duration.ZERO)
                        .checkOverlayPolicy(false)
                        .build())
                .timeout(Duration.ofMillis(100))
                .pollInterval(Duration.ofMillis(10))
                .build();
    }

    private static JsOverlayDebug overlay(WebDriver driver) {
        OverlayConfig config = OverlayConfig.builder().build();
        BrowserScriptExecutor executor = (script, args) -> null;
        OverlayRootManager rootManager = new OverlayRootManager(executor, config);
        ApiOverlayPanel apiPanel = new ApiOverlayPanel(executor, rootManager, config);
        return new JsOverlayDebug(
                driver,
                config,
                apiPanel,
                new ApiCallActions(apiPanel),
                new Guards(driver)
        );
    }

    private static Map<String, Object> inactive() {
        return Map.of("active", false);
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
        private final WebElement element;
        private List<WebElement> elements;
        private Queue<Map<String, Object>> rects = new ArrayDeque<>(List.of(rect(10, 10, 100, 30), rect(10, 10, 100, 30)));
        private Map<String, Object> reactSignals = Map.of(
                "ariaDisabled", inactive(),
                "ariaBusy", inactive(),
                "dataLoading", inactive(),
                "dataPending", inactive(),
                "progressbar", "",
                "spinner", "",
                "skeleton", "",
                "focusLock", "",
                "dialogOrModal", ""
        );

        private FakeBrowser() {
            this.element = (WebElement) Proxy.newProxyInstance(
                    ReactActionabilityCheckerTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "isDisplayed", "isEnabled" -> true;
                        case "toString" -> "FakeWebElement";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            this.elements = List.of(element);
        }

        private static FakeBrowser ready() {
            return new FakeBrowser();
        }

        private FakeBrowser elements(List<WebElement> elements) {
            this.elements = elements;
            return this;
        }

        private FakeBrowser reactSignals(Map<String, Object> reactSignals) {
            this.reactSignals = reactSignals;
            return this;
        }

        private WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(
                    ReactActionabilityCheckerTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findElements" -> elements;
                        case "executeScript" -> executeScript(String.valueOf(args[0]));
                        case "toString" -> "FakeWebDriver";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private Object executeScript(String script) {
            if (script.contains("isConnected")) {
                return true;
            }
            if (script.contains("scrollIntoView")) {
                return true;
            }
            if (script.contains("elementFromPoint")) {
                return Map.of("receives", true, "topElement", "", "x", 10, "y", 10);
            }
            if (script.contains("aria-disabled")) {
                return reactSignals;
            }
            if (script.contains("getBoundingClientRect")) {
                return rects.isEmpty() ? rect(10, 10, 100, 30) : rects.remove();
            }
            throw new UnsupportedOperationException(script);
        }
    }
}

