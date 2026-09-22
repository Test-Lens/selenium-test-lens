package io.github.testlens;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HighlightClickContractTest {

    @Test
    void decorativeHighlightMethodsNeverClickWithOverlayEnabledOrDisabled() {
        for (boolean enabled : new boolean[]{true, false}) {
            Fixture fixture = fixture(enabled);

            fixture.overlay.highlightClick(fixture.element, "CLICK");
            fixture.overlay.highlightElement(fixture.element, "ELEMENT");
            fixture.overlay.highlightParent(fixture.element, "PARENT");
            fixture.overlay.highlightAncestor(fixture.element, 2, "ANCESTOR");
            fixture.overlay.highlightClosest(fixture.element, "button", "CLOSEST");

            assertEquals(0, fixture.clicks.get(), "enabled=" + enabled);
        }
    }

    @Test
    void highlightThenClickClicksExactlyOnceWithOverlayEnabledOrDisabled() {
        for (boolean enabled : new boolean[]{true, false}) {
            Fixture fixture = fixture(enabled);

            fixture.overlay.highlightThenClick(fixture.element, "SAVE");

            assertEquals(1, fixture.clicks.get(), "enabled=" + enabled);
        }
    }

    @Test
    void uiLocatorClickClicksExactlyOnceWithOverlayEnabledOrDisabled() {
        for (boolean enabled : new boolean[]{true, false}) {
            Fixture fixture = fixture(enabled);

            fixture.overlay.getByTestId("save", "Save").click();

            assertEquals(1, fixture.clicks.get(), "enabled=" + enabled);
        }
    }

    @Test
    void publicSmartClickBridgePreservesLegacyPhysicalMethodAndOptsInPerInvocation() {
        SmartClickFixture legacy = smartClickFixture();

        assertThrows(ElementClickInterceptedException.class,
                () -> legacy.overlay.smartClickWithOverlayHandler(legacy.element, "Covered"));
        assertEquals(0, legacy.javascriptClicks.get(), "the existing two-argument method must remain physical-only");

        SmartClickFixture configured = smartClickFixture();
        configured.overlay.smartClickWithOverlayHandler(configured.element, "Covered", true);
        assertEquals(1, configured.javascriptClicks.get(), "the approved overload must dispatch JS exactly once");
    }

    private static Fixture fixture(boolean enabled) {
        AtomicInteger clicks = new AtomicInteger();
        WebElement element = (WebElement) Proxy.newProxyInstance(
                HighlightClickContractTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "click" -> {
                        clicks.incrementAndGet();
                        yield null;
                    }
                    case "isDisplayed", "isEnabled" -> true;
                    case "getRect" -> new Rectangle(0, 0, 100, 30);
                    case "getTagName" -> "button";
                    case "getText", "getAttribute", "getDomAttribute", "getDomProperty", "getCssValue" -> "";
                    case "toString" -> "CountingWebElement";
                    default -> defaultValue(method.getReturnType());
                });

        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                HighlightClickContractTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findElement" -> element;
                    case "findElements" -> List.of(element);
                    case "executeScript", "executeAsyncScript" -> null;
                    case "toString" -> "FakeWebDriver";
                    default -> defaultValue(method.getReturnType());
                });

        OverlayConfig config = OverlayConfig.builder()
                .enabled(enabled)
                .showHudPanel(false)
                .globalOverlayCloseButtonSelector("")
                .build();
        return new Fixture(new JsOverlayDebug(driver, config), element, clicks);
    }

    private static SmartClickFixture smartClickFixture() {
        AtomicInteger javascriptClicks = new AtomicInteger();
        WebElement element = (WebElement) Proxy.newProxyInstance(
                HighlightClickContractTest.class.getClassLoader(), new Class<?>[]{WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "click" -> throw new ElementClickInterceptedException("covered");
                    case "isDisplayed", "isEnabled" -> true;
                    case "getRect" -> new Rectangle(10, 10, 100, 30);
                    case "getText", "getAttribute", "getDomAttribute", "getDomProperty", "getCssValue" -> null;
                    case "toString" -> "CoveredWebElement";
                    default -> defaultValue(method.getReturnType());
                });
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                HighlightClickContractTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class, Interactive.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("perform") || method.getName().equals("resetInputState")) return null;
                    if (method.getName().equals("executeScript")) {
                        String script = String.valueOf(args[0]);
                        Object[] scriptArguments = args.length > 1 && args[1] instanceof Object[] values
                                ? values : new Object[0];
                        if ("arguments[0].click();".equals(script)) {
                            javascriptClicks.incrementAndGet();
                            return null;
                        }
                        if (script.contains("arguments[0].isConnected")) return true;
                        if (scriptArguments.length > 1 && "CENTER".equals(scriptArguments[1])) {
                            return Map.of("x", 20D, "y", 20D, "owned", false,
                                    "reason", "hit-test-mismatch:div");
                        }
                        if (scriptArguments.length > 1 && "POINTS".equals(scriptArguments[1])) return List.of();
                        return null;
                    }
                    if (method.getName().equals("toString")) return "SmartClickBridgeDriver";
                    return defaultValue(method.getReturnType());
                });
        OverlayConfig config = OverlayConfig.builder().enabled(false).showHudPanel(false)
                .globalOverlayCloseButtonSelector("").build();
        return new SmartClickFixture(new JsOverlayDebug(driver, config), element, javascriptClicks);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }

    private record Fixture(JsOverlayDebug overlay, WebElement element, AtomicInteger clicks) {}

    private record SmartClickFixture(JsOverlayDebug overlay, WebElement element, AtomicInteger javascriptClicks) {}
}
