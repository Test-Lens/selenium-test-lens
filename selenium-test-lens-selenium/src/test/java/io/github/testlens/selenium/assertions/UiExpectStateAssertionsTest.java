package io.github.testlens.selenium.assertions;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.trace.TraceLogSink;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class UiExpectStateAssertionsTest {
    private static final By ITEMS = By.cssSelector(".item");

    @Test
    void countPassesImmediatelyAtZeroAndPollsOneSnapshotPerAttempt() {
        StateBrowser immediate = browser(List.of(element("one"), element("two")));
        UiAssertionResult first = locator(immediate).expect(options(false)).toHaveCount(2);
        assertEquals(UiAssertionStatus.PASSED, first.status());
        assertEquals(1, first.attempts());
        assertEquals(1, immediate.snapshotCalls.get());

        StateBrowser empty = browser(List.of());
        assertTrue(locator(empty).expect(options(true)).toHaveCount(0).isPassed());

        StateBrowser growing = browser(List.of(element("one")), List.of(element("one"), element("two")));
        UiAssertionResult polled = locator(growing).expect(options(false)).toHaveCount(2);
        assertTrue(polled.attempts() >= 2);
        assertEquals(polled.attempts(), growing.snapshotCalls.get());
    }

    @Test
    void countValidatesFailFastAndKeepsNonzeroMismatchRetryable() {
        StateBrowser unused = browser(List.of());
        assertThrows(IllegalArgumentException.class, () -> locator(unused).expect().toHaveCount(-1));
        assertEquals(0, unused.snapshotCalls.get());

        UiAssertionError missing = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of())).expect(options(true)).toHaveCount(1));
        assertEquals(UiAssertionStatus.FAILED, missing.result().status());
        assertEquals(UiAssertionFailureReason.COUNT_MISMATCH, missing.result().failureReason());
        assertEquals(1, missing.result().attempts());

        StateBrowser changing = browser(List.of(element("one")),
                List.of(element("one"), element("two")),
                List.of(element("one"), element("two"), element("three")));
        UiAssertionResult result = locator(changing).expect(options(true)).toHaveCount(3);
        assertTrue(result.attempts() >= 2, "a nonzero mismatch must remain retryable");

        UiAssertionError timedOut = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of(element("one")))).expect(shortOptions()).toHaveCount(2));
        assertEquals(UiAssertionStatus.TIMED_OUT, timedOut.result().status());
        assertEquals(UiAssertionFailureReason.COUNT_MISMATCH, timedOut.result().failureReason());
        assertInstanceOf(TimeoutException.class, timedOut.getCause());
    }

    @Test
    void countWorksForCompositePipelineAndDoesNotMarkRecoveryFlakiness() {
        Element available = element("available").text("Laptop Pro").attribute("data-status", "available");
        Element sold = element("sold").text("Laptop Basic").attribute("data-status", "sold");
        StateBrowser browser = browser(List.of(available, sold));
        UiTestLensSession session = UiTestLensSession.start("count assertion");
        UiLocator composed = locator(browser, session)
                .filterByTextContaining("Laptop")
                .filterByAttribute("data-status", "available");

        assertTrue(composed.expect(options(false)).toHaveCount(1).isPassed());
        assertEquals(0, session.retrySummary().totalRetries());
        assertFalse(session.retrySummary().flakyCandidate());
    }

    @Test
    void attributeDistinguishesMissingEmptyAndPollsWithoutLeakingValues() {
        Element missing = element("field").attribute("data-secret", (String) null);
        UiAssertionError absent = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of(missing))).expect(shortOptions()).toHaveAttribute("data-secret", ""));
        assertEquals(UiAssertionFailureReason.ATTRIBUTE_MISMATCH, absent.result().failureReason());
        assertTrue(absent.result().actualPreview().contains("missing"));

        Element empty = element("field").attribute("data-secret", "");
        assertTrue(locator(browser(List.of(empty))).expect().toHaveAttribute("data-secret", "").isPassed());

        String expectedSecret = "token-expected-secret";
        String actualSecret = "token-actual-secret";
        Element changing = element("field").attribute("data-secret", actualSecret, expectedSecret);
        InMemoryLogSink sink = new InMemoryLogSink();
        UiAssertionResult result = locator(browser(List.of(changing)), sink)
                .expect(options(false)).toHaveAttribute("data-secret", expectedSecret);
        assertTrue(result.attempts() >= 2);
        String diagnostics = result.summary() + sink.entries();
        assertFalse(diagnostics.contains(expectedSecret));
        assertFalse(diagnostics.contains(actualSecret));
    }

    @Test
    void classMatchesOneCaseSensitiveHtmlTokenAndValidatesInput() {
        Element element = element("button").attribute("class", "button-primary\tactive");
        UiLocator locator = locator(browser(List.of(element)));
        assertTrue(locator.expect().toHaveClass("active").isPassed());
        UiAssertionError prefix = assertThrows(UiAssertionError.class,
                () -> locator.expect(shortOptions()).toHaveClass("button"));
        assertEquals(UiAssertionFailureReason.CLASS_MISMATCH, prefix.result().failureReason());
        assertThrows(IllegalArgumentException.class, () -> locator.expect().toHaveClass("two classes"));
        assertThrows(IllegalArgumentException.class, () -> locator.expect().toHaveClass(" "));
    }

    @Test
    void cssUsesComputedValueFailsTerminallyAndRedactsUrls() {
        Element element = element("panel").css("display", " none ");
        assertTrue(locator(browser(List.of(element))).expect().toHaveCss("display", "none").isPassed());
        assertEquals(1, element.cssReads.get());

        Element broken = element("panel").cssFailure(new InvalidSelectorException("bad css property"));
        UiAssertionError terminal = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of(broken))).expect().toHaveCss("display", "none"));
        assertEquals(UiAssertionStatus.FAILED, terminal.result().status());
        assertEquals(1, terminal.result().attempts());
        assertInstanceOf(InvalidSelectorException.class, terminal.getCause());

        String secretUrl = "https://person:pass@example.test/image.png?token=secret#part";
        Element url = element("image").css("background-image", "url(" + secretUrl + ")");
        UiAssertionError redacted = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of(url))).expect(shortOptions())
                        .toHaveCss("background-image", "url(other)"));
        assertFalse(redacted.result().summary().contains(secretUrl));
        assertTrue(redacted.result().actualPreview().contains("url(***)"));
    }

    @Test
    void selectedSupportsNativeOptionAndTypedAriaRoles() {
        Element option = element("native").tag("option").selected(false, true);
        UiAssertionResult nativeResult = locator(browser(List.of(option))).expect(options(false)).toBeSelected();
        assertTrue(nativeResult.isPassed());
        assertTrue(nativeResult.attempts() >= 2);

        Element tab = element("tab").tag("button").ariaRole("tab").attribute("aria-selected", "true");
        assertTrue(locator(browser(List.of(tab))).expect().toBeSelected().isPassed());

        Element invalid = element("bad").tag("div").ariaRole("option").attribute("aria-selected", "mixed");
        UiAssertionError invalidResult = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of(invalid))).expect().toBeSelected());
        assertEquals(UiAssertionStatus.FAILED, invalidResult.result().status());
        assertEquals(UiAssertionFailureReason.UNSUPPORTED_ELEMENT_STATE, invalidResult.result().failureReason());
        assertEquals(1, invalidResult.result().attempts());

        Element fake = element("fake").tag("div").attribute("class", "selected");
        UiAssertionError fakeResult = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of(fake))).expect().toBeSelected());
        assertEquals(UiAssertionFailureReason.UNSUPPORTED_ELEMENT_STATE, fakeResult.result().failureReason());
    }

    @Test
    void checkedAssertionsSupportNativeAriaRadioAndMixedWithoutActions() {
        Element checkbox = element("checkbox").tag("input").attribute("type", "checkbox").selected(true);
        assertTrue(locator(browser(List.of(checkbox))).expect().toBeChecked().isPassed());
        checkbox.selected(false);
        assertTrue(locator(browser(List.of(checkbox))).expect().toBeUnchecked().isPassed());

        Element radio = element("radio").tag("input").attribute("type", "radio").selected(true);
        assertTrue(locator(browser(List.of(radio))).expect().toBeChecked().isPassed());

        Element aria = element("aria").tag("button").attribute("role", "switch")
                .attribute("aria-checked", "false", "true");
        assertTrue(locator(browser(List.of(aria))).expect(options(false)).toBeChecked().isPassed());

        Element mixed = element("mixed").tag("div").attribute("role", "checkbox")
                .attribute("aria-checked", "mixed");
        assertEquals(UiAssertionFailureReason.ELEMENT_NOT_CHECKED,
                assertThrows(UiAssertionError.class,
                        () -> locator(browser(List.of(mixed))).expect(shortOptions()).toBeChecked())
                        .result().failureReason());
        assertEquals(UiAssertionFailureReason.ELEMENT_STILL_CHECKED,
                assertThrows(UiAssertionError.class,
                        () -> locator(browser(List.of(mixed))).expect(shortOptions()).toBeUnchecked())
                        .result().failureReason());
        assertEquals(0, checkbox.clicks.get() + radio.clicks.get() + aria.clicks.get() + mixed.clicks.get());
    }

    @Test
    void checkedAssertionUsesNativeInputAssociatedWithLocatedLabelChild() {
        Element input = element("terms").tag("input").attribute("type", "checkbox").selected(true);
        Element label = element("label").tag("label");
        Element decoration = element("decoration").tag("span").labeledBy(input, label);
        StateBrowser browser = browser(List.of(decoration));
        assertTrue(locator(browser).expect().toBeChecked().isPassed());
        assertEquals(0, input.clicks.get());
        assertEquals(0, browser.scriptMutations.get());
    }

    @Test
    void unsupportedCheckedControlFailsOnceWithoutClickOrMutation() {
        Element fake = element("fake").tag("div").attribute("class", "checkbox");
        StateBrowser browser = browser(List.of(fake));
        UiAssertionError error = assertThrows(UiAssertionError.class,
                () -> locator(browser).expect().toBeChecked());
        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertEquals(UiAssertionFailureReason.UNSUPPORTED_ELEMENT_STATE, error.result().failureReason());
        assertEquals(1, error.result().attempts());
        assertEquals(0, fake.clicks.get());
        assertEquals(0, browser.scriptMutations.get());
    }

    @Test
    void attachedAndDetachedObserveCurrentQueryRatherThanElementIdentity() {
        StateBrowser appearing = browser(List.of(), List.of(element("new")));
        UiAssertionResult attached = locator(appearing).expect(options(false)).toBeAttached();
        assertTrue(attached.isPassed());
        assertTrue(attached.attempts() >= 2);

        assertTrue(locator(browser(List.of())).expect(options(true)).toBeDetached().isPassed());
        assertTrue(locator(browser(List.of(element("hidden").displayed(false))))
                .expect().toBeAttached().isPassed());

        StateBrowser replacement = browser(List.of(element("old")), List.of(element("replacement")));
        UiAssertionError stillAttached = assertThrows(UiAssertionError.class,
                () -> locator(replacement).expect(shortOptions()).toBeDetached());
        assertEquals(UiAssertionFailureReason.ELEMENT_STILL_ATTACHED, stillAttached.result().failureReason());
        assertTrue(stillAttached.result().attempts() >= 1);
    }

    @Test
    void attachedHonorsMissingFailFastWhileDetachedMissingAlwaysPasses() {
        UiAssertionError attached = assertThrows(UiAssertionError.class,
                () -> locator(browser(List.of())).expect(options(true)).toBeAttached());
        assertEquals(UiAssertionStatus.FAILED, attached.result().status());
        assertEquals(UiAssertionFailureReason.ELEMENT_NOT_ATTACHED, attached.result().failureReason());
        assertEquals(1, attached.result().attempts());

        UiAssertionResult detached = locator(browser(List.of())).expect(options(true)).toBeDetached();
        assertTrue(detached.isPassed());
        assertEquals(1, detached.attempts());
    }

    @Test
    void staleCompositeFilterRetriesAndCannotProduceFalseDetachment() {
        Element stale = element("stale").textFailure(new StaleElementReferenceException("rerender"));
        Element fresh = element("fresh").text("match");
        StateBrowser browser = browser(List.of(stale), List.of(fresh));
        UiLocator filtered = locator(browser).filterByText("match");

        UiAssertionError result = assertThrows(UiAssertionError.class,
                () -> filtered.expect(shortOptions()).toBeDetached());

        assertEquals(UiAssertionFailureReason.ELEMENT_STILL_ATTACHED, result.result().failureReason());
        assertTrue(result.result().attempts() >= 2);
    }

    @Test
    void terminalDriverFailureIsFailedNotTimedOutAndPreservesCause() {
        InvalidSelectorException failure = new InvalidSelectorException("invalid selector");
        StateBrowser browser = browser(List.of()).failure(failure);
        UiAssertionError error = assertThrows(UiAssertionError.class,
                () -> locator(browser).expect().toBeAttached());
        assertEquals(UiAssertionStatus.FAILED, error.result().status());
        assertEquals(1, error.result().attempts());
        assertSame(failure, error.getCause());
    }

    private static UiLocator locator(StateBrowser browser) {
        return locator(browser, new InMemoryLogSink());
    }

    private static UiLocator locator(StateBrowser browser, InMemoryLogSink sink) {
        return new UiLocator(browser.driver, ITEMS, "items", new JsOverlayDebug(browser.driver), locatorOptions(),
                OverlayLogger.from(UiTestLensLogger.builder().sink(sink).build()));
    }

    private static UiLocator locator(StateBrowser browser, UiTestLensSession session) {
        return new UiLocator(browser.driver, ITEMS, "items", new JsOverlayDebug(browser.driver), locatorOptions(),
                OverlayLogger.from(UiTestLensLogger.builder().sink(new TraceLogSink(session)).build()));
    }

    private static UiLocatorOptions locatorOptions() {
        return UiLocatorOptions.builder().timeout(Duration.ofMillis(30)).pollInterval(Duration.ofMillis(1))
                .maxRetries(1).build();
    }

    private static UiAssertionOptions options(boolean failFast) {
        return UiAssertionOptions.builder().timeout(Duration.ofMillis(80)).pollInterval(Duration.ofMillis(1))
                .failFastOnMissingElement(failFast).build();
    }

    private static UiAssertionOptions shortOptions() {
        return UiAssertionOptions.builder().timeout(Duration.ofMillis(12)).pollInterval(Duration.ofMillis(1)).build();
    }

    @SafeVarargs
    private static StateBrowser browser(List<Element>... snapshots) {
        return new StateBrowser(List.of(snapshots));
    }

    private static Element element(String id) {
        return new Element(id);
    }

    private static final class StateBrowser {
        private final Queue<List<Element>> snapshots;
        private final AtomicInteger snapshotCalls = new AtomicInteger();
        private final AtomicInteger scriptMutations = new AtomicInteger();
        private RuntimeException failure;
        private final WebDriver driver;

        private StateBrowser(List<List<Element>> snapshots) {
            this.snapshots = new LinkedList<>(snapshots);
            this.driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                        return switch (method.getName()) {
                            case "findElements" -> proxies(nextSnapshot());
                            case "findElement" -> {
                                List<Element> current = nextSnapshot();
                                if (current.isEmpty()) throw new NoSuchElementException("missing");
                                yield current.get(0).proxy;
                            }
                            case "executeScript", "executeAsyncScript" -> association(args);
                            case "toString" -> "StateBrowser";
                            default -> throw new UnsupportedOperationException(method.getName());
                        };
                    });
        }

        private StateBrowser failure(RuntimeException failure) {
            this.failure = failure;
            return this;
        }

        private List<Element> nextSnapshot() {
            snapshotCalls.incrementAndGet();
            if (failure != null) throw failure;
            if (snapshots.size() > 1) return snapshots.remove();
            return snapshots.isEmpty() ? List.of() : snapshots.peek();
        }

        private Object association(Object[] args) {
            Object[] scriptArguments = args != null && args.length > 1 && args[1] instanceof Object[] values
                    ? values : new Object[0];
            if (scriptArguments.length > 0 && scriptArguments[0] instanceof WebElement origin) {
                Element model = Element.BY_PROXY.get(origin);
                if (model != null && model.labelControl != null) {
                    return List.of(model.labelControl.proxy, model.label.proxy);
                }
            }
            return null;
        }

        private static List<WebElement> proxies(List<Element> elements) {
            return elements.stream().map(element -> element.proxy).toList();
        }
    }

    private static final class Element {
        private static final Map<WebElement, Element> BY_PROXY = new LinkedHashMap<>();
        private final String id;
        private String tag = "div";
        private String ariaRole = "";
        private boolean displayed = true;
        private final Map<String, Queue<String>> attributes = new LinkedHashMap<>();
        private final Map<String, Queue<String>> css = new LinkedHashMap<>();
        private Queue<Boolean> selected = new LinkedList<>(List.of(false));
        private Queue<String> texts = new LinkedList<>(List.of(""));
        private RuntimeException cssFailure;
        private RuntimeException textFailure;
        private Element labelControl;
        private Element label;
        private final AtomicInteger clicks = new AtomicInteger();
        private final AtomicInteger cssReads = new AtomicInteger();
        private final WebElement proxy;

        private Element(String id) {
            this.id = id;
            this.proxy = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebElement.class}, (ignored, method, args) -> switch (method.getName()) {
                        case "getTagName" -> tag;
                        case "getAriaRole" -> ariaRole;
                        case "getDomAttribute" -> next(attributes.get(String.valueOf(args[0])));
                        case "getDomProperty" -> "indeterminate".equals(args[0]) ? "false" : null;
                        case "getCssValue" -> {
                            cssReads.incrementAndGet();
                            if (cssFailure != null) throw cssFailure;
                            yield next(css.get(String.valueOf(args[0])));
                        }
                        case "getText" -> {
                            if (textFailure != null) {
                                RuntimeException current = textFailure;
                                textFailure = null;
                                throw current;
                            }
                            yield next(texts);
                        }
                        case "isSelected" -> next(selected);
                        case "isDisplayed" -> displayed;
                        case "isEnabled" -> true;
                        case "getRect" -> new Rectangle(0, 0, 10, 10);
                        case "getSize" -> new Dimension(10, 10);
                        case "click" -> { clicks.incrementAndGet(); yield null; }
                        case "findElements" -> List.of();
                        case "toString" -> "Element[" + id + "]";
                        case "hashCode" -> System.identityHashCode(ignored);
                        case "equals" -> ignored == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
            BY_PROXY.put(proxy, this);
        }

        private Element tag(String tag) { this.tag = tag; return this; }
        private Element ariaRole(String role) { this.ariaRole = role; return this; }
        private Element displayed(boolean displayed) { this.displayed = displayed; return this; }
        private Element text(String... values) { this.texts = queue(values); return this; }
        private Element textFailure(RuntimeException failure) { this.textFailure = failure; return this; }
        private Element attribute(String name, String... values) { this.attributes.put(name, queue(values)); return this; }
        private Element css(String name, String... values) { this.css.put(name, queue(values)); return this; }
        private Element cssFailure(RuntimeException failure) { this.cssFailure = failure; return this; }
        private Element selected(Boolean... values) { this.selected = new LinkedList<>(List.of(values)); return this; }
        private Element labeledBy(Element control, Element label) { this.labelControl = control; this.label = label; return this; }

        private static <T> T next(Queue<T> values) {
            if (values == null || values.isEmpty()) return null;
            return values.size() > 1 ? values.remove() : values.peek();
        }

        private static Queue<String> queue(String... values) {
            Queue<String> queue = new LinkedList<>();
            for (String value : values) queue.add(value);
            return queue;
        }
    }
}
