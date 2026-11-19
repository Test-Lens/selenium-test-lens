package io.github.testlens.core;

import io.github.testlens.OverlayConfig;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LegacyOverlayRecoveryRegressionTest {
    private final OverlayConfig config = OverlayConfig.builder().enabled(false)
            .globalOverlayCloseButtonSelector("#close").build();

    @Test
    void globalCloseWaitsForDisappearanceInsteadOfBlindDelay() {
        AtomicInteger visibilityChecks = new AtomicInteger();
        WebElement button = button(visibilityChecks);
        BlockingOverlayHelper helper = new BlockingOverlayHelper(driver(button), config, null, null);
        assertTrue(helper.handleGlobalOverlayIfPresent(null, null));
        assertEquals(3, visibilityChecks.get(), "observe the closing control until it disappears");
    }

    @Test
    void popupCloseWaitsForDisappearanceInsteadOfBlindDelay() {
        AtomicInteger visibilityChecks = new AtomicInteger();
        PopupDetector detector = new PopupDetector(driver(button(visibilityChecks)), config, null, null);
        assertTrue(detector.closePopupIfPresent());
        assertEquals(3, visibilityChecks.get());
    }

    @Test
    void canceledCloseMustNotDispatchClick() {
        AtomicInteger clicks = new AtomicInteger();
        WebElement button = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("click")) clicks.incrementAndGet();
                    return null;
                });
        BlockingOverlayHelper helper = new BlockingOverlayHelper(driver(button), config, null, null);
        Thread.currentThread().interrupt();
        try {
            assertThrows(WebDriverException.class, () -> helper.handleGlobalOverlayIfPresent(null, null));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(0, clicks.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void blockingCloseObservesOverlayRootAndAcceptsItsRemovalAsDisappearance() {
        AtomicInteger visibilityChecks = new AtomicInteger();
        AtomicInteger closeClicks = new AtomicInteger();
        WebElement overlay = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isDisplayed")) {
                        if (visibilityChecks.incrementAndGet() == 3) {
                            throw new StaleElementReferenceException("removed after close");
                        }
                        return true;
                    }
                    return null;
                });
        WebElement close = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("click")) closeClicks.incrementAndGet();
                    if (method.getName().equals("isDisplayed")) fail("observe the overlay root, not its control");
                    return null;
                });
        WebDriver browser = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeScript")) {
                        return args[0].equals(BlockingOverlayHelper.closeButtonInsideScript()) ? close : overlay;
                    }
                    return null;
                });
        assertTrue(new BlockingOverlayHelper(browser, config, null, null)
                .handleBlockingOverlayFor(close, null, null));
        assertEquals(1, closeClicks.get());
        assertEquals(3, visibilityChecks.get());
    }
    private WebElement button(AtomicInteger visibilityChecks) {
        return (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isDisplayed")) return visibilityChecks.incrementAndGet() < 3;
                    return null;
                });
    }

    private WebDriver driver(WebElement button) {
        return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) ->
                        method.getName().equals("executeScript") ? button : null);
    }
}
