package io.github.testlens;

import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TestLensBrowserContextTest {
    @Test void framesByLocatorIndexParentAndDefaultPreserveSeleniumSemantics() {
        Browser browser = new Browser();
        TestLens lens = lens(browser.driver());
        UiTestLensSession session = lens.startSession("frames");

        lens.switchToFrame(By.id("payment"), "Payment frame")
                .switchToFrame(0, "Nested frame")
                .switchToParentFrame()
                .switchToDefaultContent();

        assertEquals(List.of("frame-element", "frame-0", "parent", "default"), browser.contextCalls);
        assertTrue(session.events().stream().anyMatch(e -> "context.frame".equals(e.attributes().get("action"))));
        assertTrue(browser.hudScripts.get() > 0, "context events should feed the lazy HUD");
    }

    @Test void frameFailureRemainsOriginalSeleniumFailureAndIsTraced() {
        Browser browser = new Browser();
        browser.frameFailure = new NoSuchFrameException("missing payment frame");
        TestLens lens = lens(browser.driver());
        UiTestLensSession session = lens.startSession("missing frame");

        NoSuchFrameException failure = assertThrows(NoSuchFrameException.class,
                () -> lens.switchToFrame(3, "Payment frame"));

        assertSame(browser.frameFailure, failure);
        assertTrue(session.events().stream().anyMatch(e -> e.failure() != null && e.failure().message().contains("missing payment")));
    }

    @Test void windowsUseDeterministicSetDifferenceAndSwitchBack() {
        Browser browser = new Browser();
        browser.handles.set(new LinkedHashSet<>(Set.of("main")));
        TestLens lens = lens(browser.driver());
        lens.startSession("windows");
        Set<String> before = lens.windowHandles();
        browser.handles.set(new LinkedHashSet<>(Set.of("main", "payment")));

        assertEquals("payment", lens.waitForNewWindow(before));
        lens.switchToNewWindow(before, "Payment").switchToWindow("main", "Main");
        assertEquals(List.of("window-payment", "window-main"), browser.contextCalls);
    }

    @Test void ambiguousNewWindowsFailInsteadOfChoosingSetOrder() {
        Browser browser = new Browser();
        browser.handles.set(new LinkedHashSet<>(Set.of("main", "a", "b")));
        TestLens lens = lens(browser.driver());
        lens.startSession("ambiguous windows");
        assertThrows(NoSuchWindowException.class, () -> lens.waitForNewWindow(Set.of("main")));
    }

    @Test void independentDriversKeepContextStateAndEventsSeparate() {
        Browser one = new Browser(); Browser two = new Browser();
        TestLens first = lens(one.driver()); TestLens second = lens(two.driver());
        UiTestLensSession firstSession = first.startSession("first context");
        UiTestLensSession secondSession = second.startSession("second context");
        CompletableFuture.allOf(
                CompletableFuture.runAsync(() -> first.switchToFrame(0, "first")),
                CompletableFuture.runAsync(() -> second.switchToDefaultContent())).join();
        assertEquals(List.of("frame-0"), one.contextCalls);
        assertEquals(List.of("default"), two.contextCalls);
        assertNotEquals(firstSession.id(), secondSession.id());
    }

    private static TestLens lens(WebDriver driver) {
        return TestLens.attach(driver, TestLensOptions.builder()
                .locatorOptions(UiLocatorOptions.builder().timeout(Duration.ofMillis(80)).pollInterval(Duration.ofMillis(5)).build())
                .build());
    }

    private static final class Browser {
        final java.util.List<String> contextCalls = new java.util.concurrent.CopyOnWriteArrayList<>();
        final AtomicReference<Set<String>> handles = new AtomicReference<>(Set.of("main"));
        final AtomicInteger hudScripts = new AtomicInteger();
        RuntimeException frameFailure;

        WebDriver driver() {
            WebElement frame = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{WebElement.class},
                    (p, m, a) -> switch (m.getName()) { case "isDisplayed", "isEnabled" -> true; default -> null; });
            WebDriver.TargetLocator target = (WebDriver.TargetLocator) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{WebDriver.TargetLocator.class}, (p, m, a) -> {
                        switch (m.getName()) {
                            case "frame" -> {
                                if (frameFailure != null) throw frameFailure;
                                contextCalls.add(a[0] instanceof Integer ? "frame-" + a[0] : "frame-element");
                            }
                            case "parentFrame" -> contextCalls.add("parent");
                            case "defaultContent" -> contextCalls.add("default");
                            case "window" -> contextCalls.add("window-" + a[0]);
                        }
                        return null;
                    });
            return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{WebDriver.class, JavascriptExecutor.class}, (p, m, a) -> switch (m.getName()) {
                        case "switchTo" -> target;
                        case "findElement" -> frame;
                        case "findElements" -> List.of(frame);
                        case "getWindowHandle" -> "main";
                        case "getWindowHandles" -> handles.get();
                        case "executeScript", "executeAsyncScript" -> { hudScripts.incrementAndGet(); yield null; }
                        default -> m.getReturnType() == boolean.class ? false : null;
                    });
        }
    }
}
