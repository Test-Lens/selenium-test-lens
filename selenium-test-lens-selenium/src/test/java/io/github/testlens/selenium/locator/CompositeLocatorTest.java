package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class CompositeLocatorTest {
    private static final By PARENTS = By.cssSelector(".parent");
    private static final By CHILDREN = By.cssSelector(".child");

    @Test
    void scopedQueryFlattensParentsInOrderDeduplicatesAndNeverSearchesGlobally() {
        Element shared = element("shared", "Shared", Map.of());
        Element firstChild = element("first", "First", Map.of());
        Element lastChild = element("last", "Last", Map.of());
        Element first = element("parent-1", "", Map.of()).children(CHILDREN, firstChild, shared);
        Element second = element("parent-2", "", Map.of()).children(CHILDREN, shared, lastChild);
        DriverModel browser = driver(() -> List.of(first, second));

        UiLocator scoped = locator(browser, PARENTS).locator(CHILDREN);
        assertEquals(0, browser.calls.get(), "composition must stay lazy");
        assertEquals(List.of(firstChild.proxy, shared.proxy, lastChild.proxy), scoped.resolveAll());
        assertEquals(0, browser.globalChildQueries.get());
        assertEquals(List.of(), locator(driver(List::of), PARENTS).locator(CHILDREN).resolveAll());
        assertEquals(List.of(), locator(driver(() -> List.of(element("empty", "", Map.of()))), PARENTS)
                .locator(CHILDREN).resolveAll());
    }

    @Test
    void selectionOrderIsPartOfThePipeline() {
        Element laptop = element("laptop", "Laptop", Map.of()).children(CHILDREN,
                element("laptop-buy", "Buy", Map.of()));
        Element phone = element("phone", "Phone", Map.of()).children(CHILDREN,
                element("phone-buy-1", "Buy", Map.of()), element("phone-buy-2", "Buy", Map.of()));
        DriverModel browser = driver(() -> List.of(laptop, phone));
        UiLocator parents = locator(browser, PARENTS);

        assertEquals(List.of("phone-buy-1", "phone-buy-2"), ids(parents.nth(1).locator(CHILDREN).resolveAll()));
        assertEquals("phone-buy-2", parents.locator(CHILDREN).nth(2).resolve().getDomAttribute("id"));
        assertEquals("laptop", parents.filterByTextContaining("Laptop").first().resolve().getDomAttribute("id"));
        assertThrows(UiLocatorException.class, () -> parents.first().filterByTextContaining("Phone").resolve());
    }

    @Test
    void textAndAttributeFiltersComposeAndUseTypedElementReads() {
        Element matching = element("one", "  Fast\u00a0 Laptop\n", Map.of("data-status", "available"));
        Element wrongCase = element("two", "fast laptop", Map.of("data-status", "available"));
        Element missing = element("three", "Fast Laptop", Map.of());
        DriverModel browser = driver(() -> List.of(matching, wrongCase, missing));
        UiLocator parents = locator(browser, PARENTS);

        assertEquals(List.of("one", "three"), ids(parents.filterByText("Fast Laptop").resolveAll()));
        assertEquals(List.of("one"), ids(parents.filterByTextContaining("Fast")
                .filterByAttribute("data-status", "available").resolveAll()));
        assertEquals(0, matching.getAttributeCalls.get());
        assertTrue(matching.getDomAttributeCalls.get() > 0);
        assertThrows(NullPointerException.class, () -> parents.filterByText(null));
        assertThrows(IllegalArgumentException.class, () -> parents.filterByAttribute(" ", "x"));
    }

    @Test
    void hasFiltersKeepParentsAndSupportSemanticDescendants() {
        Element buy = element("buy", "", Map.of()).role("button", "Buy");
        Element available = element("available", "Card", Map.of()).children(CHILDREN, buy);
        Element unavailable = element("unavailable", "Card", Map.of());
        DriverModel browser = driver(() -> List.of(available, unavailable));
        JsOverlayDebug lens = overlay(browser.driver);
        UiLocator parents = lens.locator(PARENTS, "Cards", options());

        assertEquals(List.of("available"), ids(parents.filterHas(CHILDREN).resolveAll()));
        assertEquals(List.of("available"), ids(parents.filterHas(lens.getByRole("button", "Buy")).resolveAll()));
        assertEquals("buy", parents.locator(lens.getByRole("button", "Buy")).resolve().getDomAttribute("id"));
    }

    @Test
    void differentDriversAndInvalidArgumentsFailBeforeAnyLookup() {
        DriverModel first = driver(List::of);
        DriverModel second = driver(List::of);
        UiLocator parent = locator(first, PARENTS);
        UiLocator foreign = locator(second, CHILDREN);

        assertThrows(IllegalArgumentException.class, () -> parent.locator(foreign));
        assertThrows(IllegalArgumentException.class, () -> parent.filterHas(foreign));
        assertThrows(NullPointerException.class, () -> parent.locator((By) null));
        assertThrows(NullPointerException.class, () -> parent.filterHas((By) null));
        assertEquals(0, first.calls.get());
        assertEquals(0, second.calls.get());
    }

    @Test
    void staleRestartsTheWholeSnapshotAndTerminalFailureDoesNotBecomeEmpty() {
        Element first = element("first", "Match", Map.of());
        Element stale = element("stale", "Match", Map.of());
        stale.textFailures = 1;
        DriverModel browser = driver(() -> List.of(first, stale));

        assertEquals(List.of("first", "stale"), ids(locator(browser, PARENTS).filterByText("Match").resolveAll()));
        assertTrue(first.textCalls.get() >= 2, "the partial first snapshot must be discarded");

        Element invalid = element("invalid", "", Map.of());
        invalid.textFailure = new InvalidSelectorException("terminal");
        UiLocatorException failure = assertThrows(UiLocatorException.class,
                () -> locator(driver(() -> List.of(invalid)), PARENTS).filterByText("").resolveAll());
        assertTrue(hasCause(failure, InvalidSelectorException.class));
        assertEquals(1, invalid.textCalls.get());
    }

    @Test
    void countWaitsObserveFreshSnapshotsAndDoNotCreateFlakyRetries() {
        AtomicInteger growing = new AtomicInteger();
        DriverModel browser = driver(() -> elements(Math.min(3, growing.getAndIncrement())));
        JsOverlayDebug lens = overlay(browser.driver);
        UiTestLensSession session = lens.startSession("count waits");
        UiLocator items = lens.locator(PARENTS, "Items", options());

        assertSame(items, items.waitUntilCountAtLeast(2));
        assertTrue(browser.calls.get() >= 3);
        assertEquals(0, session.retrySummary().totalRetries());
        assertFalse(session.retrySummary().flakyCandidate());

        AtomicInteger shrinking = new AtomicInteger(3);
        UiLocator decreasing = locator(driver(() -> elements(Math.max(0, shrinking.getAndDecrement()))), PARENTS);
        decreasing.waitUntilCountAtMost(1).waitUntilCount(0);
        locator(driver(List::of), PARENTS).waitUntilCount(0);
    }

    @Test
    void countValidationFirstPollAndTimeoutContractsAreDeterministic() {
        DriverModel exact = driver(() -> elements(2));
        UiLocator items = locator(exact, PARENTS);
        assertSame(items, items.waitUntilCount(2));
        assertEquals(1, exact.calls.get());
        assertThrows(IllegalArgumentException.class, () -> items.waitUntilCount(-1));

        DriverModel missing = driver(List::of);
        UiLocatorException timeout = assertThrows(UiLocatorException.class,
                () -> locator(missing, PARENTS).waitUntilCountAtLeast(1));
        assertTrue(timeout.getMessage().contains("count >= 1"));
        assertTrue(timeout.actionabilitySummary().contains("lastCount=0"));
        assertTrue(timeout.actionabilitySummary().contains("attempts="));
        assertTrue(hasCause(timeout, TimeoutException.class));
        assertTrue(missing.calls.get() > 1);
    }

    private static List<Element> elements(int count) {
        List<Element> elements = new ArrayList<>();
        for (int index = 0; index < count; index++) elements.add(element("item-" + index, "", Map.of()));
        return elements;
    }

    private static List<String> ids(List<WebElement> elements) {
        return elements.stream().map(element -> element.getDomAttribute("id")).toList();
    }

    private static UiLocator locator(DriverModel browser, By by) {
        return overlay(browser.driver).locator(by, "Parents", options());
    }

    private static JsOverlayDebug overlay(WebDriver driver) {
        return new JsOverlayDebug(driver, OverlayConfig.builder().enabled(false).showHudPanel(false).build());
    }

    private static UiLocatorOptions options() {
        return UiLocatorOptions.builder().timeout(Duration.ofMillis(80)).pollInterval(Duration.ofMillis(2))
                .maxRetries(3).build();
    }

    private static DriverModel driver(Supplier<List<Element>> elements) {
        return new DriverModel(elements);
    }

    private static Element element(String id, String text, Map<String, String> attributes) {
        return new Element(id, text, attributes);
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private static final class DriverModel {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger globalChildQueries = new AtomicInteger();
        private final Supplier<List<Element>> elements;
        private final WebDriver driver;

        private DriverModel(Supplier<List<Element>> elements) {
            this.elements = elements;
            driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                        if (method.getName().equals("findElements")) {
                            calls.incrementAndGet();
                            By by = (By) args[0];
                            if (!(by instanceof By.Remotable)) return by.findElements((WebDriver) proxy);
                            if (by.toString().equals(CHILDREN.toString())) globalChildQueries.incrementAndGet();
                            return elements.get().stream().map(element -> element.proxy).toList();
                        }
                        if (method.getName().equals("findElement")) {
                            List<WebElement> found = ((WebDriver) proxy).findElements((By) args[0]);
                            if (found.isEmpty()) throw new NoSuchElementException("missing");
                            return found.get(0);
                        }
                        if (method.getName().startsWith("execute")) return null;
                        if (method.getReturnType() == boolean.class) return false;
                        return null;
                    });
        }
    }

    private static final class Element {
        private final String id;
        private final String text;
        private final Map<String, String> attributes;
        private final Map<String, List<Element>> children = new java.util.HashMap<>();
        private final AtomicInteger textCalls = new AtomicInteger();
        private final AtomicInteger getAttributeCalls = new AtomicInteger();
        private final AtomicInteger getDomAttributeCalls = new AtomicInteger();
        private final WebElement proxy;
        private String role = "";
        private String accessibleName = "";
        private int textFailures;
        private RuntimeException textFailure;

        private Element(String id, String text, Map<String, String> attributes) {
            this.id = id; this.text = text; this.attributes = attributes;
            proxy = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebElement.class},
                    (self, method, args) -> switch (method.getName()) {
                        case "findElements" -> {
                            By by = (By) args[0];
                            if (!(by instanceof By.Remotable)) yield by.findElements((WebElement) self);
                            List<Element> matches = children.get(by.toString());
                            if (matches == null && by.toString().startsWith("By.xpath:")) {
                                matches = children.values().stream().flatMap(List::stream).toList();
                            }
                            yield (matches == null ? List.<Element>of() : matches).stream()
                                    .map(element -> element.proxy).toList();
                        }
                        case "findElement" -> {
                            List<WebElement> found = ((WebElement) self).findElements((By) args[0]);
                            if (found.isEmpty()) throw new NoSuchElementException("missing child");
                            yield found.get(0);
                        }
                        case "getText" -> {
                            textCalls.incrementAndGet();
                            if (textFailure != null) throw textFailure;
                            if (textFailures-- > 0) throw new StaleElementReferenceException("rerender");
                            yield text;
                        }
                        case "getDomAttribute" -> {
                            getDomAttributeCalls.incrementAndGet();
                            String name = (String) args[0];
                            yield name.equals("id") ? id : attributes.get(name);
                        }
                        case "getAttribute" -> { getAttributeCalls.incrementAndGet(); yield attributes.get((String) args[0]); }
                        case "getAriaRole" -> role;
                        case "getAccessibleName" -> accessibleName;
                        case "isDisplayed", "isEnabled" -> true;
                        case "hashCode" -> System.identityHashCode(self);
                        case "equals" -> self == args[0];
                        case "toString" -> id;
                        default -> null;
                    });
        }

        private Element children(By by, Element... values) { children.put(by.toString(), List.of(values)); return this; }
        private Element role(String role, String name) { this.role = role; this.accessibleName = name; return this; }
    }
}
