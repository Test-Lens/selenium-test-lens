package io.github.testlens.actions;

import io.github.testlens.OverlayConfig;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeInputRegressionTest {
    private final OverlayConfig config = OverlayConfig.builder().enabled(false).build();
    private final WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> null);
    private final WebElement element = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[]{WebElement.class}, (proxy, method, args) -> null);

    @Test
    void ambiguousFailureMentioningInterceptionMustNotRepeatTyping() {
        AtomicInteger attempts = new AtomicInteger();
        WebDriverException failure = new WebDriverException("response lost after intercepted input event");
        TypingActions typing = new TypingActions(driver, null, config) {
            @Override public void typeWithHint(WebElement target, String value) {
                if (attempts.incrementAndGet() == 1) throw failure;
            }
        };
        SmartInputActions actions = new SmartInputActions(driver, config, null, typing);

        assertSame(failure, assertThrows(WebDriverException.class,
                () -> actions.smartTypeWithHint(element, "value", "input")));
        assertEquals(1, attempts.get());
    }

    @Test
    void typedBeforeDispatchFailureCanRecoverOnceWithoutMessageMatching() {
        AtomicInteger attempts = new AtomicInteger();
        TypingActions typing = new TypingActions(driver, null, config) {
            @Override public void typeWithHint(WebElement target, String value) {
                if (attempts.incrementAndGet() == 1) throw new ElementNotInteractableException("blocked");
            }
        };
        new SmartInputActions(driver, config, null, typing).smartTypeWithHint(element, "value", "input");
        assertEquals(2, attempts.get());
    }

    @Test
    void canceledSmartInputMustNotDispatchTyping() {
        AtomicInteger attempts = new AtomicInteger();
        TypingActions typing = new TypingActions(driver, null, config) {
            @Override public void typeWithHint(WebElement target, String value) {
                attempts.incrementAndGet();
            }
        };
        SmartInputActions actions = new SmartInputActions(driver, config, null, typing);
        Thread.currentThread().interrupt();
        try {
            assertThrows(WebDriverException.class, () -> actions.smartTypeWithHint(element, "value", "input"));
            assertEquals(0, attempts.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void canceledStandaloneTypingMustNotClearInput() {
        AtomicInteger clears = new AtomicInteger();
        WebElement input = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("clear")) clears.incrementAndGet();
                    return null;
                });
        Thread.currentThread().interrupt();
        try {
            assertThrows(WebDriverException.class, () -> new TypingActions(driver, null, config)
                    .typeWithHint(input, "value"));
            assertEquals(0, clears.get());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
    @Test
    void nullValueClearsWithoutSendingNullWhenDecorationsAreDisabled() {
        AtomicInteger clears = new AtomicInteger();
        AtomicInteger sends = new AtomicInteger();
        WebElement input = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("clear")) clears.incrementAndGet();
                    if (method.getName().equals("sendKeys")) sends.incrementAndGet();
                    return null;
                });
        new TypingActions(driver, null, config).typeWithHint(input, null);
        assertEquals(1, clears.get());
        assertEquals(0, sends.get());
    }
}
