package io.github.testlens.browser;

import io.github.testlens.OverlayConfig;
import io.github.testlens.actions.SmartClickActions;
import io.github.testlens.actions.SmartInputActions;
import io.github.testlens.actions.TypingActions;
import io.github.testlens.core.BlockingOverlayHelper;
import io.github.testlens.core.PopupDetector;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeOverlayBrowserAuditIT {
    @Test
    void immediateGlobalDismissalHandlesRemovedCloseControl() {
        WebDriver driver = BrowserTestHarness.createDriver();
        try {
            page(driver, "<div id='panel'><button id='close' onclick='this.parentElement.remove()'>Close</button></div>");
            OverlayConfig config = config("#close");
            assertTrue(new BlockingOverlayHelper(driver, config, null, null)
                    .handleGlobalOverlayIfPresent(null, null));
            assertTrue(driver.findElements(By.id("panel")).isEmpty());
        } finally {
            driver.quit();
        }
    }

    @Test
    void delayedBlockingDismissalWaitsForOverlayRootRemoval() {
        WebDriver driver = BrowserTestHarness.createDriver();
        try {
            page(driver, """
                    <button id='target' style='position:absolute;left:50px;top:50px'>Target</button>
                    <div id='panel' style='position:fixed;inset:0;z-index:1000;background:white'>
                      <button onclick="setTimeout(()=>document.getElementById('panel').remove(),150)">Close</button>
                    </div>
                    """);
            assertTrue(new BlockingOverlayHelper(driver, config(""), null, null)
                    .handleBlockingOverlayFor(driver.findElement(By.id("target")), null, null));
            assertTrue(driver.findElements(By.id("panel")).isEmpty());
        } finally {
            driver.quit();
        }
    }

    @Test
    void popupDismissalHandlesRemovedPopup() {
        WebDriver driver = BrowserTestHarness.createDriver();
        try {
            page(driver, "<div id='panel'><button id='close' onclick='this.parentElement.remove()'>Close</button></div>");
            assertTrue(new PopupDetector(driver, config("#close"), null, null).closePopupIfPresent());
            assertTrue(driver.findElements(By.id("panel")).isEmpty());
        } finally {
            driver.quit();
        }
    }

    @Test
    void ambiguousClickResponseDoesNotRepeatAlreadyExecutedBrowserAction() {
        WebDriver driver = BrowserTestHarness.createDriver();
        try {
            page(driver, "<button id='target' onclick='window.clicks=(window.clicks||0)+1'>Submit</button>");
            AtomicInteger dispatched = new AtomicInteger();
            WebElement target = failingResponse(driver.findElement(By.id("target")), "click", dispatched,
                    new WebDriverException("response lost", new ElementClickInterceptedException("earlier interception")));
            SmartClickActions actions = new SmartClickActions(driver, config(""), null, null);
            assertThrows(WebDriverException.class, () -> actions.clickWithOverlayHandling(target, "Submit"));
            assertEquals(1, dispatched.get());
            assertEquals(1L, ((Number) ((JavascriptExecutor) driver).executeScript("return window.clicks")).longValue());
        } finally {
            driver.quit();
        }
    }

    @Test
    void ambiguousTypingResponseDoesNotRepeatAlreadyExecutedBrowserInput() {
        WebDriver driver = BrowserTestHarness.createDriver();
        try {
            page(driver, "<input id='target' oninput='window.inputs=(window.inputs||0)+1'>");
            AtomicInteger dispatched = new AtomicInteger();
            WebElement target = failingResponse(driver.findElement(By.id("target")), "sendKeys", dispatched,
                    new WebDriverException("response lost after intercepted input event"));
            OverlayConfig config = config("");
            SmartInputActions actions = new SmartInputActions(driver, config, null,
                    new TypingActions(driver, null, config));
            assertThrows(WebDriverException.class, () -> actions.smartTypeWithHint(target, "X", "Input"));
            assertEquals(1, dispatched.get());
            assertEquals("X", driver.findElement(By.id("target")).getDomProperty("value"));
            assertEquals(1L, ((Number) ((JavascriptExecutor) driver).executeScript("return window.inputs")).longValue());
        } finally {
            driver.quit();
        }
    }

    private static WebElement failingResponse(WebElement real, String command, AtomicInteger dispatched,
                                             WebDriverException failure) {
        return (WebElement) Proxy.newProxyInstance(RuntimeOverlayBrowserAuditIT.class.getClassLoader(),
                new Class<?>[]{WebElement.class, WrapsElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getWrappedElement")) return real;
                    try {
                        Object result = method.invoke(real, args);
                        if (method.getName().equals(command)) {
                            dispatched.incrementAndGet();
                            throw failure;
                        }
                        return result;
                    } catch (InvocationTargetException wrapped) {
                        throw wrapped.getCause();
                    }
                });
    }

    private static OverlayConfig config(String closeSelector) {
        return OverlayConfig.builder().enabled(false).globalOverlayCloseButtonSelector(closeSelector).build();
    }

    private static void page(WebDriver driver, String body) {
        driver.get("data:text/html;base64," + Base64.getEncoder().encodeToString(
                ("<!doctype html><html><body>" + body + "</body></html>").getBytes(StandardCharsets.UTF_8)));
    }
}
