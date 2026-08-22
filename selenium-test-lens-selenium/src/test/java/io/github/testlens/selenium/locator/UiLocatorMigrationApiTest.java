package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class UiLocatorMigrationApiTest {
    @Test
    void readsAttributePropertyValueAndPressesGeneralKeys() {
        List<CharSequence> sent = new ArrayList<>();
        WebElement element = element(sent, new AtomicBoolean(true));
        UiLocator locator = overlay(driver(element, null)).locator(By.id("field"), "Field", options());
        assertEquals("attr-data-kind", locator.attribute("data-kind"));
        assertEquals("property-valueAsNumber", locator.property("valueAsNumber"));
        assertEquals("attr-value", locator.value());
        locator.press(Keys.TAB).press(Keys.CONTROL, "a");
        assertEquals(List.of(Keys.TAB, Keys.CONTROL, "a"), sent);
    }

    @Test
    void waitsAreConditionsNotAssertionsAndEmitNativeEvents() {
        AtomicBoolean visible = new AtomicBoolean(false);
        WebElement element = element(new ArrayList<>(), visible);
        WebDriver driver = driver(element, () -> visible.set(true));
        JsOverlayDebug overlay = overlay(driver);
        UiTestLensSession session = overlay.startSession("wait");
        assertDoesNotThrow(() -> overlay.locator(By.id("field"), "Field", options()).waitUntilVisible());
        assertTrue(session.events().stream().anyMatch(e -> "WAIT".equals(e.attributes().get("uiEventType"))));
        assertTrue(session.events().stream().noneMatch(e -> "ASSERTION_PASSED".equals(e.attributes().get("uiEventType"))));
    }

    @Test
    void hudFailureDoesNotFailClickOrFillAndEventsStillReachTrace() {
        List<CharSequence> sent = new ArrayList<>();
        AtomicBoolean clicked = new AtomicBoolean();
        WebElement element = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "click" -> { clicked.set(true); yield null; }
                    case "clear" -> null;
                    case "sendKeys" -> { for (CharSequence key : (CharSequence[]) args[0]) sent.add(key); yield null; }
                    case "isDisplayed", "isEnabled" -> true;
                    case "getRect" -> new Rectangle(1, 1, 20, 20);
                    default -> null;
                });
        WebDriver driver = driverFailingHud(element);
        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        UiTestLensSession session = overlay.startSession("hud failure");
        UiLocator locator = overlay.locator(By.id("field"), "Field", options());
        assertDoesNotThrow(() -> locator.fill("john"));
        assertDoesNotThrow(locator::click);
        assertTrue(clicked.get());
        assertEquals(List.of("john"), sent);
        assertTrue(session.events().stream().anyMatch(e -> "LOCATOR_ACTION_PASSED".equals(e.attributes().get("uiEventType"))));
    }

    private static JsOverlayDebug overlay(WebDriver driver) {
        return new JsOverlayDebug(driver, OverlayConfig.builder().enabled(false).build());
    }
    private static UiLocatorOptions options() {
        return UiLocatorOptions.builder().timeout(Duration.ofMillis(150)).pollInterval(Duration.ofMillis(5)).build();
    }
    private static WebElement element(List<CharSequence> sent, AtomicBoolean visible) {
        return (WebElement) Proxy.newProxyInstance(UiLocatorMigrationApiTest.class.getClassLoader(), new Class[]{WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendKeys" -> { for (CharSequence key : (CharSequence[]) args[0]) sent.add(key); yield null; }
                    case "isDisplayed" -> visible.get();
                    case "isEnabled" -> true;
                    case "getAttribute" -> "attr-" + args[0];
                    case "getDomProperty" -> "property-" + args[0];
                    case "getText" -> "ready";
                    default -> null;
                });
    }
    private static WebDriver driver(WebElement element, Runnable firstFind) {
        AtomicBoolean called = new AtomicBoolean();
        return (WebDriver) Proxy.newProxyInstance(UiLocatorMigrationApiTest.class.getClassLoader(), new Class[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) { if (called.compareAndSet(false, true) && firstFind != null) firstFind.run(); return element; }
                    if (method.getName().equals("findElements")) return List.of(element);
                    if (method.getName().startsWith("execute")) return null;
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }
    private static WebDriver driverFailingHud(WebElement element) {
        return (WebDriver) Proxy.newProxyInstance(UiLocatorMigrationApiTest.class.getClassLoader(), new Class[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) return element;
                    if (method.getName().equals("findElements")) return List.of(element);
                    if (method.getName().startsWith("execute")) throw new WebDriverException("script injection blocked");
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }
}
