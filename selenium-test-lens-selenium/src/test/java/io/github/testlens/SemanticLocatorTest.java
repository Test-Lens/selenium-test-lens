package io.github.testlens;

import io.github.testlens.selenium.locator.UiLocator;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SemanticLocatorTest {

    @Test
    void roleUsesBrowserAccessibleNameRatherThanTextOrAriaLabel() {
        ElementModel element = element("button", "Save order", Map.of("aria-label", "Wrong"));
        element.text = "Also wrong";

        assertEquals(List.of(element.proxy), SemanticBy.role("button", "Save order").findElements(context(element)));
        assertTrue(SemanticBy.role("button", "Wrong").findElements(context(element)).isEmpty());
        assertEquals(2, element.accessibleNameCalls.get());
    }

    @Test
    void roleMatchesBrowserComputedNamesFromLabelledbyAndDescendants() {
        ElementModel labelled = element("button", "Save order", Map.of("aria-labelledby", "first second"));
        ElementModel descendantImage = element("button", "Save", Map.of());

        assertEquals(List.of(labelled.proxy), SemanticBy.role("button", "Save\u00a0 \n order").findElements(context(labelled)));
        assertEquals(List.of(descendantImage.proxy), SemanticBy.role("button", "Save").findElements(context(descendantImage)));
        assertTrue(SemanticBy.role("button", "save").findElements(context(descendantImage)).isEmpty());
    }

    @Test
    void roleWithoutNameDoesNotRequireAccessibleName() {
        ElementModel element = element("checkbox", null, Map.of());

        assertEquals(List.of(element.proxy), SemanticBy.role("checkbox", null).findElements(context(element)));
        assertEquals(0, element.accessibleNameCalls.get());
        assertTrue(SemanticBy.role("button", null).findElements(context(element)).isEmpty());
    }

    @Test
    void unsupportedAccessibleNameIsPropagatedWithoutFallback() {
        WebDriverException unsupported = new WebDriverException("accessibility endpoint unavailable");
        ElementModel element = element("button", "ignored", Map.of("aria-label", "Save"));
        element.accessibleNameFailure = unsupported;

        assertSame(unsupported, assertThrows(WebDriverException.class,
                () -> SemanticBy.role("button", "Save").findElements(context(element))));
    }

    @Test
    void labelRequiresRealLabelSourceAndUsesComputedName() {
        ElementModel nativeLabel = element("textbox", "Email address", Map.of("nativeLabel", "true"));
        ElementModel ariaLabel = element("textbox", "Account", Map.of("aria-label", "Account"));
        ElementModel ariaLabelledby = element("textbox", "Billing address", Map.of("aria-labelledby", "billing-label"));
        ElementModel placeholderOnly = element("textbox", "Placeholder name", Map.of("placeholder", "Placeholder name"));
        ElementModel titleOnly = element("textbox", "Title name", Map.of("title", "Title name"));

        SearchContext context = selectiveContext(List.of(nativeLabel, ariaLabel, ariaLabelledby, placeholderOnly, titleOnly));
        assertEquals(List.of(nativeLabel.proxy), SemanticBy.label("Email address").findElements(context));
        assertEquals(List.of(ariaLabel.proxy), SemanticBy.label("Account").findElements(context));
        assertEquals(List.of(ariaLabelledby.proxy), SemanticBy.label("Billing address").findElements(context));
        assertTrue(SemanticBy.label("Placeholder name").findElements(context).isEmpty());
        assertTrue(SemanticBy.label("Title name").findElements(context).isEmpty());
    }

    @Test
    void altTextUsesOnlyNormalizedAltForSupportedElementsAndAllowsEmptyAlt() {
        ElementModel image = alt("img", "Company logo");
        ElementModel area = alt("area", "Office map");
        ElementModel imageInput = alt("input-image", "Submit image");
        ElementModel empty = alt("img", "");
        ElementModel ariaOnly = element("img", "Not alt", Map.of("aria-label", "Company logo"));
        SearchContext context = selectiveContext(List.of(image, area, imageInput, empty, ariaOnly));

        assertEquals(List.of(image.proxy), SemanticBy.altText(" Company\u00a0logo ").findElements(context));
        assertEquals(List.of(area.proxy), SemanticBy.altText("Office map").findElements(context));
        assertEquals(List.of(imageInput.proxy), SemanticBy.altText("Submit image").findElements(context));
        assertEquals(List.of(empty.proxy), SemanticBy.altText("").findElements(context));
        assertThrows(IllegalArgumentException.class, () -> SemanticBy.altText(null));
    }

    @Test
    void factoriesAreLazyAndCollectionsPreserveDomOrder() {
        ElementModel first = element("button", "Duplicate", Map.of());
        ElementModel second = element("button", "Duplicate", Map.of());
        AtomicInteger driverCalls = new AtomicInteger();
        WebDriver driver = driver(List.of(first, second), driverCalls);
        TestLens lens = TestLens.attach(driver, OverlayConfig.builder().enabled(false).showHudPanel(false).build());

        UiLocator matches = lens.getByRole("button", "Duplicate");
        UiLocator placeholder = lens.getByPlaceholder("It's \"quoted\"\nand safe");
        UiLocator label = lens.getByLabel("Duplicate");
        UiLocator alt = lens.getByAltText("Duplicate");
        assertEquals(0, driverCalls.get());
        assertNotNull(placeholder);
        assertNotNull(label);
        assertNotNull(alt);

        assertEquals(2, matches.count());
        assertSame(first.proxy, matches.first().resolve());
        assertSame(second.proxy, matches.nth(1).resolve());
        assertSame(second.proxy, matches.last().resolve());
    }

    @Test
    void accessibleNameUsesReadPipelineRetriesStaleAndDoesNotFallback() {
        ElementModel stale = element("button", "unused", Map.of());
        stale.accessibleNameFailure = new StaleElementReferenceException("rerender");
        ElementModel fresh = element("button", "Browser name", Map.of());
        AtomicInteger calls = new AtomicInteger();
        WebDriver driver = driver(List.of(stale, fresh), calls);
        UiLocator locator = new JsOverlayDebug(driver, OverlayConfig.builder().enabled(false).showHudPanel(false).build())
                .locator(By.id("target"), "target", io.github.testlens.selenium.locator.UiLocatorOptions.builder()
                        .timeout(Duration.ofMillis(200)).pollInterval(Duration.ofMillis(2)).maxRetries(2).build());

        assertEquals("Browser name", locator.accessibleName());
        assertEquals(1, stale.accessibleNameCalls.get());
        assertEquals(1, fresh.accessibleNameCalls.get());
    }

    @Test
    void accessibleNameStopsImmediatelyForTerminalWebDriverFailure() {
        ElementModel broken = element("button", "unused", Map.of());
        WebDriverException original = new WebDriverException("accessible name unavailable");
        broken.accessibleNameFailure = original;
        UiLocator locator = new JsOverlayDebug(driver(List.of(broken), new AtomicInteger()),
                OverlayConfig.builder().enabled(false).showHudPanel(false).build())
                .locator(By.id("target"));

        RuntimeException failure = assertThrows(RuntimeException.class, locator::accessibleName);
        assertTrue(hasCause(failure, original));
        assertEquals(1, broken.accessibleNameCalls.get());
    }

    @Test
    void xpathPlaceholderEscapingHandlesBothQuotesAndNewlines() {
        UiLocator locator = new JsOverlayDebug(driver(List.of(), new AtomicInteger()))
                .getByPlaceholder("It's \"quoted\"\nand safe");
        String selector = locator.by().toString();
        assertTrue(selector.contains("concat("));
        assertTrue(selector.contains("placeholder"));
    }

    private static ElementModel element(String role, String name, Map<String, String> attributes) {
        return new ElementModel(role, name, attributes);
    }

    private static ElementModel alt(String kind, String value) {
        return element("img", value, Map.of("alt", value, "altKind", kind));
    }

    private static SearchContext context(ElementModel... models) {
        return selectiveContext(List.of(models));
    }

    private static SearchContext selectiveContext(List<ElementModel> models) {
        return new SearchContext() {
            @Override public List<WebElement> findElements(By by) {
                String selector = by.toString();
                if (selector.contains("ancestor::label")) {
                    return models.stream().filter(model -> model.attributes.containsKey("nativeLabel")
                                    || model.attributes.containsKey("aria-label") || model.attributes.containsKey("aria-labelledby"))
                            .map(model -> model.proxy).toList();
                }
                if (selector.contains("//img[@alt]")) {
                    return models.stream().filter(model -> model.attributes.containsKey("alt"))
                            .map(model -> model.proxy).toList();
                }
                return models.stream().map(model -> model.proxy).toList();
            }
            @Override public WebElement findElement(By by) { return findElements(by).get(0); }
        };
    }

    private static WebDriver driver(List<ElementModel> models, AtomicInteger calls) {
        AtomicInteger ordinaryResolution = new AtomicInteger();
        return (WebDriver) Proxy.newProxyInstance(SemanticLocatorTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findElements")) {
                        calls.incrementAndGet();
                        By by = (By) args[0];
                        if (by instanceof SemanticBy) return by.findElements((SearchContext) proxy);
                        if (by.toString().contains("semantic") || by.toString().contains("//*[") || by.toString().contains("//img")) {
                            return selectiveContext(models).findElements(by);
                        }
                        int index = Math.min(ordinaryResolution.getAndIncrement(), Math.max(0, models.size() - 1));
                        return models.isEmpty() ? List.of() : List.of(models.get(index).proxy);
                    }
                    if (method.getName().equals("findElement")) {
                        List<WebElement> found = ((WebDriver) proxy).findElements((By) args[0]);
                        if (found.isEmpty()) throw new org.openqa.selenium.NoSuchElementException("missing");
                        return found.get(0);
                    }
                    if (method.getName().startsWith("execute")) return null;
                    if (method.getName().equals("toString")) return "semantic-driver";
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }

    private static boolean hasCause(Throwable failure, Throwable expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current == expected) return true;
        }
        return false;
    }

    private static final class ElementModel {
        private final String role;
        private final String accessibleName;
        private final Map<String, String> attributes;
        private final AtomicInteger accessibleNameCalls = new AtomicInteger();
        private final WebElement proxy;
        private String text = "";
        private RuntimeException accessibleNameFailure;

        private ElementModel(String role, String accessibleName, Map<String, String> attributes) {
            this.role = role;
            this.accessibleName = accessibleName;
            this.attributes = attributes;
            this.proxy = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{WebElement.class},
                    (ignored, method, args) -> switch (method.getName()) {
                        case "getAriaRole" -> role;
                        case "getAccessibleName" -> {
                            accessibleNameCalls.incrementAndGet();
                            if (accessibleNameFailure != null) throw accessibleNameFailure;
                            yield accessibleName;
                        }
                        case "getDomAttribute", "getAttribute" -> attributes.get((String) args[0]);
                        case "getText" -> text;
                        case "isDisplayed", "isEnabled" -> true;
                        case "toString" -> "semantic-element";
                        default -> null;
                    });
        }
    }
}
