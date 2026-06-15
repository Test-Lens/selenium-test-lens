package io.github.testlens.selenium.assertions;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiExpectTest {

    @Test
    void retriesTextUntilItMatches() {
        FakeBrowser browser = FakeBrowser.withTexts("Saving", "Saved");
        UiLocator locator = fastOverlay(browser.driver()).locator(By.id("toast"), fastLocatorOptions());
        UiExpect expect = new UiExpect(locator, fastAssertionOptions(), null);

        UiAssertionResult result = expect.toHaveText("Saved");

        assertEquals(UiAssertionStatus.PASSED, result.status());
        assertTrue(result.attempts() >= 2);
    }

    @Test
    void hiddenPassesWhenElementIsMissing() {
        FakeBrowser browser = FakeBrowser.missing();
        UiLocator locator = fastOverlay(browser.driver()).locator(By.id("modal"), fastLocatorOptions());
        UiExpect expect = new UiExpect(locator, fastAssertionOptions(), null);

        UiAssertionResult result = expect.toBeHidden();

        assertTrue(result.isPassed());
        assertEquals("missing", result.actualPreview());
    }

    @Test
    void timeoutFailureContainsSummary() {
        FakeBrowser browser = FakeBrowser.withTexts("Saving", "Still saving");
        UiLocator locator = fastOverlay(browser.driver()).locator(By.id("toast"), fastLocatorOptions());
        UiExpect expect = new UiExpect(locator, fastAssertionOptions(), null);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> expect.toContainText("Saved"));

        assertEquals(UiAssertionStatus.TIMED_OUT, error.result().status());
        assertEquals(UiAssertionFailureReason.TEXT_MISMATCH, error.result().failureReason());
        assertTrue(error.getMessage().contains("toContainText TIMED_OUT"));
    }

    @Test
    void valuePreviewDoesNotExposeInputValue() {
        FakeBrowser browser = FakeBrowser.withValue("masked-input");
        UiLocator locator = fastOverlay(browser.driver()).locator(By.id("masked-field"), fastLocatorOptions());
        UiExpect expect = new UiExpect(locator, fastAssertionOptions(), null);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> expect.toHaveValue("different-sample"));

        assertEquals("length=16", error.result().expectedPreview());
        assertEquals("length=12", error.result().actualPreview());
    }

    private static JsOverlayDebug fastOverlay(WebDriver driver) {
        return new JsOverlayDebug(driver);
    }

    private static UiLocatorOptions fastLocatorOptions() {
        return UiLocatorOptions.builder()
                .timeout(Duration.ofMillis(10))
                .pollInterval(Duration.ofMillis(5))
                .maxRetries(1)
                .build();
    }

    private static UiAssertionOptions fastAssertionOptions() {
        return UiAssertionOptions.builder()
                .timeout(Duration.ofMillis(60))
                .pollInterval(Duration.ofMillis(5))
                .build();
    }

    private static final class FakeBrowser {
        private final boolean missing;
        private final Queue<String> texts;
        private final String value;

        private FakeBrowser(boolean missing, Queue<String> texts, String value) {
            this.missing = missing;
            this.texts = texts;
            this.value = value;
        }

        private static FakeBrowser withTexts(String... texts) {
            return new FakeBrowser(false, new ArrayDeque<>(java.util.List.of(texts)), "");
        }

        private static FakeBrowser withValue(String value) {
            return new FakeBrowser(false, new ArrayDeque<>(java.util.List.of("")), value);
        }

        private static FakeBrowser missing() {
            return new FakeBrowser(true, new ArrayDeque<>(), "");
        }

        private WebDriver driver() {
            WebElement element = element();
            return (WebDriver) Proxy.newProxyInstance(
                    UiExpectTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findElement" -> {
                            if (missing) {
                                throw new NoSuchElementException("missing");
                            }
                            yield element;
                        }
                        case "executeScript", "executeAsyncScript" -> null;
                        case "toString" -> "FakeWebDriver";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private WebElement element() {
            return (WebElement) Proxy.newProxyInstance(
                    UiExpectTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getText" -> nextText();
                        case "isDisplayed", "isEnabled" -> true;
                        case "getAttribute" -> "value".equals(args[0]) ? value : "";
                        case "toString" -> "FakeWebElement";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private String nextText() {
            if (texts.isEmpty()) {
                return "";
            }
            if (texts.size() == 1) {
                return texts.peek();
            }
            return texts.remove();
        }
    }
}
