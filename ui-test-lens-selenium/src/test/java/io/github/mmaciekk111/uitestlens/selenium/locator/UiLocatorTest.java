package io.github.mmaciekk111.uitestlens.selenium.locator;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLocatorTest {

    @Test
    void getByTestIdBuildsCssLocator() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());

        UiLocator locator = overlay.getByTestId("save-button", "Save");

        assertEquals("Save (By.cssSelector: [data-testid='save-button'])", locator.description());
        assertEquals("By.cssSelector: [data-testid='save-button']", locator.by().toString());
    }

    @Test
    void getByTestIdEscapesApostrophe() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());

        UiLocator locator = overlay.getByTestId("owner's-save");

        assertTrue(locator.by().toString().contains("owner\\'s-save"));
    }

    private static WebDriver fakeDriver() {
        return (WebDriver) Proxy.newProxyInstance(
                UiLocatorTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName()) || "executeAsyncScript".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "FakeWebDriver";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
