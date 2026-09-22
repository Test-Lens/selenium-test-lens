package io.github.testlens.actions;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmartClickFallbackCascadeTest {

    @Test
    void nativeSuccessStopsTheCascade() {
        Fixture fixture = fixture((click, ignored) -> click.incrementAndGet(), false, List.of());

        fixture.actions.click(fixture.element, true);

        assertEquals(1, fixture.nativeClicks.get());
        assertEquals(0, fixture.pointerSequences.get());
        assertEquals(0, fixture.javascriptClicks.get());
    }

    @Test
    void actionsSuccessStopsBeforePointAndJavascript() {
        Fixture fixture = fixture((click, ignored) -> {
            click.incrementAndGet();
            throw new ElementClickInterceptedException("covered center");
        }, true, List.of());

        fixture.actions.click(fixture.element, true);

        assertEquals(1, fixture.nativeClicks.get());
        assertEquals(2, fixture.pointerSequences.get(), "hover and click are separate, hit-tested sequences");
        assertEquals(0, fixture.javascriptClicks.get());
    }

    @Test
    void pointSuccessStopsBeforeJavascriptWhenCenterIsCovered() {
        Fixture fixture = fixture((click, ignored) -> {
            click.incrementAndGet();
            throw new ElementClickInterceptedException("covered center");
        }, false, List.of(point(true)));

        fixture.actions.click(fixture.element, true);

        assertEquals(2, fixture.pointerSequences.get(), "hover plus one validated point click");
        assertEquals(0, fixture.javascriptClicks.get());
    }

    @Test
    void javascriptNullResultIsOneSuccessfulDispatch() {
        Fixture fixture = fixture((click, ignored) -> {
            click.incrementAndGet();
            throw new ElementClickInterceptedException("covered");
        }, false, List.of());

        fixture.actions.click(fixture.element, true);

        assertEquals(1, fixture.javascriptClicks.get());
        assertEquals(1, fixture.nativeClicks.get());
        assertEquals(1, fixture.pointerSequences.get(), "hover is allowed, but a mismatched hit point must not click");
        assertEquals(1, fixture.actionHighlights.get(), "strategy transitions must share one ACTION highlight");
    }

    @Test
    void javascriptDispatchFailureIsAmbiguousAndNeverRepeated() {
        Fixture fixture = fixture((click, ignored) -> {
            click.incrementAndGet();
            throw new ElementClickInterceptedException("covered");
        }, false, List.of(), true, true);

        assertThrows(WebDriverException.class, () -> fixture.actions.click(fixture.element, true));

        assertEquals(1, fixture.javascriptClicks.get());
        assertEquals(1, fixture.nativeClicks.get());
    }

    @Test
    void disablingJavascriptFailsAfterPhysicalStrategiesWithoutDispatch() {
        Fixture fixture = fixture((click, ignored) -> {
            click.incrementAndGet();
            throw new ElementClickInterceptedException("covered");
        }, false, List.of());

        assertThrows(ElementClickInterceptedException.class,
                () -> fixture.actions.click(fixture.element, false));

        assertEquals(0, fixture.javascriptClicks.get());
    }

    @Test
    void disabledTargetNeverReachesAnyClickStrategy() {
        Fixture fixture = fixture((click, ignored) -> click.incrementAndGet(), false, List.of(), false);

        assertThrows(org.openqa.selenium.ElementNotInteractableException.class,
                () -> fixture.actions.click(fixture.element, true));

        assertEquals(0, fixture.nativeClicks.get());
        assertEquals(0, fixture.pointerSequences.get());
        assertEquals(0, fixture.javascriptClicks.get());
    }

    @Test
    void ambiguousNativeFailureDoesNotRiskASecondClick() {
        Fixture fixture = fixture((click, ignored) -> {
            click.incrementAndGet();
            throw new WebDriverException("transport outcome unknown");
        }, false, List.of());

        assertThrows(WebDriverException.class, () -> fixture.actions.click(fixture.element, true));

        assertEquals(1, fixture.nativeClicks.get());
        assertEquals(0, fixture.pointerSequences.get());
        assertEquals(0, fixture.javascriptClicks.get());
    }

    @Test
    void legacyTwoArgumentMethodDoesNotGainJavascriptFallback() {
        Fixture fixture = fixture((click, ignored) -> {
            click.incrementAndGet();
            throw new ElementClickInterceptedException("covered");
        }, false, List.of());

        assertThrows(ElementClickInterceptedException.class,
                () -> fixture.actions.clickLegacy(fixture.element));

        assertEquals(0, fixture.javascriptClicks.get());
    }

    private static Fixture fixture(NativeClick nativeClick, boolean centerOwned, List<Map<String, Object>> points) {
        return fixture(nativeClick, centerOwned, points, true);
    }

    private static Fixture fixture(NativeClick nativeClick, boolean centerOwned,
                                   List<Map<String, Object>> points, boolean enabled) {
        return fixture(nativeClick, centerOwned, points, enabled, false);
    }

    private static Fixture fixture(NativeClick nativeClick, boolean centerOwned,
                                   List<Map<String, Object>> points, boolean enabled,
                                   boolean javascriptThrowsAfterDispatch) {
        AtomicInteger nativeClicks = new AtomicInteger();
        AtomicInteger javascriptClicks = new AtomicInteger();
        AtomicInteger pointerSequences = new AtomicInteger();
        AtomicInteger actionHighlights = new AtomicInteger();
        WebElement element = (WebElement) Proxy.newProxyInstance(
                SmartClickFallbackCascadeTest.class.getClassLoader(), new Class<?>[]{WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "click" -> {
                        nativeClick.run(nativeClicks, proxy);
                        yield null;
                    }
                    case "isDisplayed" -> true;
                    case "isEnabled" -> enabled;
                    case "getRect" -> new Rectangle(10, 10, 120, 40);
                    case "getDomAttribute", "getAttribute" -> null;
                    case "toString" -> "SmartClickTarget";
                    default -> defaultValue(method.getReturnType());
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                SmartClickFallbackCascadeTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class, Interactive.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "executeScript" -> {
                        String script = String.valueOf(args[0]);
                        Object[] scriptArguments = args.length > 1 && args[1] instanceof Object[] values
                                ? values : new Object[0];
                        if ("arguments[0].click();".equals(script)) {
                            javascriptClicks.incrementAndGet();
                            if (javascriptThrowsAfterDispatch) {
                                throw new WebDriverException("javascript dispatch outcome is ambiguous");
                            }
                            yield null;
                        }
                        if (script.contains("arguments[0].isConnected")) yield true;
                        if (scriptArguments.length > 1 && "CENTER".equals(scriptArguments[1])) yield point(centerOwned);
                        if (scriptArguments.length > 1 && "POINTS".equals(scriptArguments[1])) yield points;
                        yield null;
                    }
                    case "perform" -> {
                        pointerSequences.incrementAndGet();
                        yield null;
                    }
                    case "resetInputState" -> null;
                    case "toString" -> "SmartClickDriver";
                    default -> defaultValue(method.getReturnType());
                });
        OverlayConfig config = OverlayConfig.builder().enabled(true)
                .showHudPanel(false).globalOverlayCloseButtonSelector("").build();
        BrowserScriptExecutor executor = (script, args) -> null;
        OverlayRootManager rootManager = new OverlayRootManager(executor, config);
        HighlightActions highlights = new HighlightActions(driver, rootManager, config, OverlayLogger.noop()) {
            @Override
            public void automaticAction(WebElement target, String label) {
                actionHighlights.incrementAndGet();
            }

            @Override
            boolean automaticActionIfAbsent(WebElement target, String label) {
                actionHighlights.incrementAndGet();
                return true;
            }
        };
        ExposedSmartClickActions actions = new ExposedSmartClickActions(
                driver, config, rootManager, highlights, OverlayLogger.noop());
        return new Fixture(actions, element, nativeClicks, javascriptClicks, pointerSequences, actionHighlights);
    }

    private static Map<String, Object> point(boolean owned) {
        return Map.of("x", 35.0, "y", 30.0, "owned", owned,
                "reason", owned ? "target-owned" : "hit-test-mismatch:span");
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0D;
        if (type == float.class) return 0F;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == char.class) return '\0';
        return null;
    }

    @FunctionalInterface
    private interface NativeClick {
        void run(AtomicInteger clicks, Object proxy);
    }

    private static final class ExposedSmartClickActions extends SmartClickActions {
        private ExposedSmartClickActions(WebDriver driver, OverlayConfig config, OverlayRootManager rootManager,
                                         HighlightActions highlightActions, OverlayLogger logger) {
            super(driver, config, rootManager, highlightActions, logger);
        }

        private void click(WebElement target, boolean javascriptFallback) {
            clickWithOverlayHandling(target, "target", javascriptFallback);
        }

        private void clickLegacy(WebElement target) {
            clickWithOverlayHandling(target, "target");
        }
    }

    private record Fixture(ExposedSmartClickActions actions, WebElement element, AtomicInteger nativeClicks,
                           AtomicInteger javascriptClicks, AtomicInteger pointerSequences,
                           AtomicInteger actionHighlights) {}
}
