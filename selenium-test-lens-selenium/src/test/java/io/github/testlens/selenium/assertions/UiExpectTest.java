package io.github.testlens.selenium.assertions;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorException;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
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
    void textPassesWhenElementAppearsDuringRetryWithoutFailureEvents() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.visibilitySequenceWithTexts(new Boolean[]{null, null, Boolean.TRUE}, "Hello World!");
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionResult result = locator.expect().toHaveText("Hello World!");

        assertTrue(result.isPassed());
        assertTrue(result.attempts() >= 3);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void textPassesWhenTextChangesDuringRetryWithoutFailureEvents() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.withTexts("Loading...", "Loading...", "Hello World!");
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionResult result = locator.expect().toHaveText("Hello World!");

        assertTrue(result.isPassed());
        assertTrue(result.attempts() >= 3);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void visiblePassesWhenElementBecomesVisibleWithoutFailureEvents() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.visibilitySequence(Boolean.FALSE, Boolean.FALSE, Boolean.TRUE);
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionResult result = locator.expect().toBeVisible();

        assertTrue(result.isPassed());
        assertTrue(result.attempts() >= 3);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void missingElementTimeoutEmitsSingleAssertionFailureWithoutLocatorFailures() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.missing().driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> locator.expect().toBeVisible());

        assertEquals(UiAssertionStatus.TIMED_OUT, error.result().status());
        assertEquals(UiAssertionFailureReason.ELEMENT_NOT_FOUND, error.result().failureReason());
        assertTrue(error.result().attempts() > 1);
        assertTrue(error.getMessage().contains("toBeVisible TIMED_OUT"));
        assertTrue(error.getMessage().contains("modal"));
        assertEquals(1, countEvents(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT));
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void missingElementFailsImmediatelyWhenFailFastIsEnabled() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.missing().driver(), sink);
        UiAssertionOptions options = assertionOptions(true);

        UiAssertionError error = assertThrows(UiAssertionError.class,
                () -> locator.expect(options).toBeVisible());

        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertEquals(UiAssertionFailureReason.ELEMENT_NOT_FOUND, error.result().failureReason());
        assertEquals(1, error.result().attempts());
        assertEquals(1, countEvents(sink.entries(), UiTestLensEventType.ASSERTION_FAILED));
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void textTimeoutReportsLastObservedValueWithoutActionFailure() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.withTexts("Loading...", "Loading...", "Loading...");
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> locator.expect().toHaveText("Hello World!"));

        assertEquals(UiAssertionStatus.TIMED_OUT, error.result().status());
        assertEquals(UiAssertionFailureReason.TEXT_MISMATCH, error.result().failureReason());
        assertEquals("Loading...", error.result().actualPreview());
        assertTrue(error.getMessage().contains("toHaveText TIMED_OUT"));
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void invalidSelectorIsNotTreatedAsRetryableMiss() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.webDriverFailure(new InvalidSelectorException("invalid selector")).driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> locator.expect().toHaveText("Hello World!"));

        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertTrue(error.getMessage().contains("invalid selector"));
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
    }

    @Test
    void closedSessionIsNotTreatedAsRetryableMiss() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.webDriverFailure(new NoSuchSessionException("session closed")).driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> locator.expect().toBeVisible());

        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertTrue(error.getMessage().contains("session closed"));
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hiddenPassesWhenElementIsMissingRegardlessOfFailFast(boolean failFast) {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.missing().driver(), sink);

        UiAssertionResult result = locator.expect(assertionOptions(failFast)).toBeHidden();

        assertTrue(result.isPassed());
        assertEquals("missing", result.actualPreview());
        assertEquals(1, result.attempts());
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
    }

    @Test
    void hiddenMissingElementDoesNotEmitLocatorOrActionFailure() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.missing();
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionResult result = locator.expect().toBeHidden();

        assertTrue(result.isPassed());
        assertEquals("missing", result.actualPreview());
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void hiddenInvisibleElementDoesNotEmitLocatorOrActionFailure() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.displayed(false);
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionResult result = locator.expect().toBeHidden();

        assertTrue(result.isPassed());
        assertEquals("hidden", result.actualPreview());
        assertEquals(1, result.attempts());
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void hiddenPassesWhenElementDisappearsDuringRetryWithoutFailureEvents() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.visibilitySequence(Boolean.TRUE, null);
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionResult result = locator.expect().toBeHidden();

        assertTrue(result.isPassed());
        assertEquals("missing", result.actualPreview());
        assertTrue(result.attempts() >= 2);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void hiddenFailsWhenElementRemainsVisibleWithoutLocatorOrActionFailure() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.displayed(true);
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> locator.expect().toBeHidden());

        assertEquals(UiAssertionStatus.TIMED_OUT, error.result().status());
        assertEquals(UiAssertionFailureReason.ELEMENT_STILL_VISIBLE, error.result().failureReason());
        assertTrue(error.getMessage().contains("toBeHidden TIMED_OUT"));
        assertTrue(error.getMessage().contains("modal"));
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void hiddenDoesNotTreatWebDriverFailureAsSuccess() {
        InMemoryLogSink sink = new InMemoryLogSink();
        FakeBrowser browser = FakeBrowser.webDriverFailure("browser session closed");
        UiLocator locator = loggedLocator(browser.driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> locator.expect().toBeHidden());

        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertEquals(1, error.result().attempts());
        assertTrue(error.getMessage().contains("browser session closed"));
        assertEquals(1, countEvents(sink.entries(), UiTestLensEventType.ASSERTION_FAILED));
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
    }

    @Test
    void staleElementRetriesAndCanPassWithMissingFailFastEnabled() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.staleThenDisplayed().driver(), sink);

        UiAssertionResult result = locator.expect(assertionOptions(true)).toBeVisible();

        assertEquals(UiAssertionStatus.PASSED, result.status());
        assertTrue(result.attempts() > 1);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_FAILED);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
    }

    @Test
    void persistentStaleElementTimesOutAsStaleEvenWithMissingFailFastEnabled() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.alwaysStale().driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class,
                () -> locator.expect(assertionOptions(true)).toBeVisible());

        assertEquals(UiAssertionStatus.TIMED_OUT, error.result().status());
        assertEquals(UiAssertionFailureReason.STALE_ELEMENT, error.result().failureReason());
        assertTrue(error.result().attempts() > 1);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_RETRY);
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_FAILED);
    }

    @Test
    void requiredResolveStillReportsMissingElementFailure() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.missing().driver(), sink);

        assertThrows(RuntimeException.class, locator::resolve);

        assertEventPresent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
    }

    @Test
    void clickStillReportsMissingElementFailure() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.missing().driver(), sink);

        assertThrows(UiLocatorException.class, locator::click);

        assertEventPresent(sink.entries(), UiTestLensEventType.LOCATOR_RESOLVE_FAILED);
        assertEventPresent(sink.entries(), UiTestLensEventType.LOCATOR_ACTION_FAILED);
    }

    @Test
    void visibleAssertionStillFailsForMissingElement() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiLocator locator = loggedLocator(FakeBrowser.missing().driver(), sink);

        UiAssertionError error = assertThrows(UiAssertionError.class, () -> locator.expect().toBeVisible());

        assertEquals(UiAssertionStatus.TIMED_OUT, error.result().status());
        assertEventPresent(sink.entries(), UiTestLensEventType.ASSERTION_TIMED_OUT);
        assertEventAbsent(sink.entries(), UiTestLensEventType.ASSERTION_PASSED);
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

    private static UiAssertionOptions assertionOptions(boolean failFast) {
        return UiAssertionOptions.builder()
                .timeout(Duration.ofMillis(60))
                .pollInterval(Duration.ofMillis(5))
                .failFastOnMissingElement(failFast)
                .build();
    }

    private static UiLocator loggedLocator(WebDriver driver, InMemoryLogSink sink) {
        return new UiLocator(driver, By.id("modal"), "modal", fastOverlay(driver), fastLocatorOptions(),
                OverlayLogger.from(UiTestLensLogger.builder().sink(sink).build()));
    }

    private static void assertEventPresent(List<UiTestLensLogEntry> entries, UiTestLensEventType eventType) {
        assertTrue(entries.stream().anyMatch(entry -> entry.eventType() == eventType),
                () -> "Expected event " + eventType + " in " + eventTypes(entries));
    }

    private static void assertEventAbsent(List<UiTestLensLogEntry> entries, UiTestLensEventType eventType) {
        assertTrue(entries.stream().noneMatch(entry -> entry.eventType() == eventType),
                () -> "Did not expect event " + eventType + " in " + eventTypes(entries));
    }

    private static long countEvents(List<UiTestLensLogEntry> entries, UiTestLensEventType eventType) {
        return entries.stream().filter(entry -> entry.eventType() == eventType).count();
    }

    private static List<UiTestLensEventType> eventTypes(List<UiTestLensLogEntry> entries) {
        return entries.stream().map(UiTestLensLogEntry::eventType).toList();
    }

    private static final class FakeBrowser {
        private final boolean missing;
        private final Queue<Boolean> displayed;
        private final Queue<String> texts;
        private final Queue<RuntimeException> displayFailures;
        private final String value;
        private final RuntimeException webDriverFailure;

        private FakeBrowser(boolean missing, Queue<Boolean> displayed, Queue<String> texts,
                            Queue<RuntimeException> displayFailures, String value, RuntimeException webDriverFailure) {
            this.missing = missing;
            this.displayed = displayed;
            this.texts = texts;
            this.displayFailures = displayFailures;
            this.value = value;
            this.webDriverFailure = webDriverFailure;
        }

        private static FakeBrowser withTexts(String... texts) {
            return new FakeBrowser(false, queue(Boolean.TRUE), new ArrayDeque<>(java.util.List.of(texts)),
                    new ArrayDeque<>(), "", null);
        }

        private static FakeBrowser withValue(String value) {
            return new FakeBrowser(false, queue(Boolean.TRUE), new ArrayDeque<>(java.util.List.of("")),
                    new ArrayDeque<>(), value, null);
        }

        private static FakeBrowser missing() {
            return new FakeBrowser(true, new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>(), "", null);
        }

        private static FakeBrowser displayed(boolean displayed) {
            return new FakeBrowser(false, queue(displayed), new ArrayDeque<>(java.util.List.of("")),
                    new ArrayDeque<>(), "", null);
        }

        private static FakeBrowser visibilitySequence(Boolean... displayedStates) {
            return new FakeBrowser(false, new LinkedList<>(java.util.Arrays.asList(displayedStates)),
                    new ArrayDeque<>(java.util.List.of("")), new ArrayDeque<>(), "", null);
        }

        private static FakeBrowser visibilitySequenceWithTexts(Boolean[] displayedStates, String... texts) {
            return new FakeBrowser(false, new LinkedList<>(java.util.Arrays.asList(displayedStates)),
                    new ArrayDeque<>(java.util.List.of(texts)), new ArrayDeque<>(), "", null);
        }

        private static FakeBrowser staleThenDisplayed() {
            return new FakeBrowser(false, queue(Boolean.TRUE), new ArrayDeque<>(java.util.List.of("")),
                    new LinkedList<>(java.util.Arrays.asList(
                            new StaleElementReferenceException("stale once"), null)), "", null);
        }

        private static FakeBrowser alwaysStale() {
            return new FakeBrowser(false, queue(Boolean.TRUE), new ArrayDeque<>(java.util.List.of("")),
                    new ArrayDeque<>(java.util.List.of(new StaleElementReferenceException("always stale"))), "", null);
        }

        private static FakeBrowser webDriverFailure(String message) {
            return webDriverFailure(new WebDriverException(message));
        }

        private static FakeBrowser webDriverFailure(RuntimeException failure) {
            return new FakeBrowser(false, new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>(), "", failure);
        }

        private static Queue<Boolean> queue(Boolean value) {
            return new ArrayDeque<>(java.util.List.of(value));
        }

        private WebDriver driver() {
            WebElement element = element();
            return (WebDriver) Proxy.newProxyInstance(
                    UiExpectTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findElement" -> {
                            if (webDriverFailure != null) {
                                throw webDriverFailure;
                            }
                            if (missing) {
                                throw new NoSuchElementException("missing");
                            }
                            if (!displayed.isEmpty() && displayed.peek() == null) {
                                displayed.remove();
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
                        case "isDisplayed" -> nextDisplayed();
                        case "isEnabled" -> true;
                        case "getAttribute" -> "value".equals(args[0]) ? value : "";
                        case "toString" -> "FakeWebElement";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private boolean nextDisplayed() {
            if (!displayFailures.isEmpty()) {
                RuntimeException failure = displayFailures.size() == 1 ? displayFailures.peek() : displayFailures.remove();
                if (failure != null) {
                    throw failure;
                }
            }
            if (displayed.isEmpty()) {
                return true;
            }
            if (displayed.size() == 1) {
                Boolean value = displayed.peek();
                return value != null && value;
            }
            Boolean value = displayed.remove();
            return value != null && value;
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

