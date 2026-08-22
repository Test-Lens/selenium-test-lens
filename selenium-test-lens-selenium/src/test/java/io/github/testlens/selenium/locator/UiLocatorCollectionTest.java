package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UiLocatorCollectionTest {
    private static final By ROWS = By.cssSelector(".row");

    @Test void zeroOneAndManyCollectionsResolveAndCount() {
        assertEquals(0, locator(browser(List.of())).count());
        assertEquals(1, locator(browser(List.of(element("one", null)))).resolveAll().size());
        assertEquals(3, locator(browser(List.of(element("a", null), element("b", null), element("c", null)))).count());
    }

    @Test void nthFirstLastRemainObservableLocators() {
        List<String> clicks = new CopyOnWriteArrayList<>();
        UiLocator rows = locator(browser(List.of(element("first", clicks), element("middle", clicks), element("last", clicks))));
        rows.nth(0).expect().toBeVisible();
        rows.nth(2).click();
        rows.first().click();
        rows.last().click();
        assertEquals(List.of("last", "first", "last"), clicks);
    }

    @Test void invalidIndexHasLensContextAndActualCount() {
        UiLocatorException failure = assertThrows(UiLocatorException.class,
                () -> locator(browser(List.of(element("only", null)))).nth(3).resolve());
        assertTrue(failure.getMessage().contains("Rows"));
        assertTrue(failure.getMessage().contains("requestedIndex=3"));
        assertTrue(failure.getMessage().contains("actualCount=1"));
    }

    @Test void nthResolvesDynamicCollectionAtActionTime() {
        List<String> clicks = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();
        WebElement target = element("dynamic", clicks);
        WebDriver driver = browserDynamic(calls, target);
        UiLocator selected = locator(driver).nth(1);
        selected.click();
        assertEquals(List.of("dynamic"), clicks);
        assertTrue(calls.get() >= 2);
    }

    @Test void collectionOperationsReachTracePipeline() {
        WebDriver driver = browser(List.of(element("one", null)));
        JsOverlayDebug overlay = overlay(driver);
        UiTestLensSession session = overlay.startSession("collections");
        overlay.locator(ROWS, "Rows", options()).count();
        overlay.locator(ROWS, "Rows", options()).first().expect().toBeVisible();
        assertTrue(session.events().stream().anyMatch(e -> "LOCATOR_RESOLVE_STARTED".equals(e.attributes().get("uiEventType"))));
        assertTrue(session.events().stream().anyMatch(e -> "LOCATOR_ACTION_PASSED".equals(e.attributes().get("uiEventType"))));
    }

    @Test void collectionOperationsReachAutomaticHudFeed() {
        List<Object[]> scriptArguments = new CopyOnWriteArrayList<>();
        WebElement row = element("one", null);
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) return row;
                    if (method.getName().equals("findElements")) return List.of(row);
                    if (method.getName().startsWith("execute")) {
                        scriptArguments.add(args == null ? new Object[0] : args.clone());
                        return null;
                    }
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.startSession("collection HUD");

        overlay.locator(ROWS, "Rows", options()).count();

        assertTrue(scriptArguments.stream().flatMap(args -> java.util.Arrays.stream(args))
                .anyMatch(value -> String.valueOf(value).contains("hud.log")));
    }

    @Test void parallelSessionsKeepCollectionEventsIndependent() {
        JsOverlayDebug first = overlay(browser(List.of(element("first", null))));
        JsOverlayDebug second = overlay(browser(List.of(element("a", null), element("b", null))));
        UiTestLensSession firstSession = first.startSession("parallel first");
        UiTestLensSession secondSession = second.startSession("parallel second");

        CompletableFuture<Integer> firstCount = CompletableFuture.supplyAsync(
                () -> first.locator(ROWS, "First rows", options()).count());
        CompletableFuture<Integer> secondCount = CompletableFuture.supplyAsync(
                () -> second.locator(ROWS, "Second rows", options()).count());

        assertEquals(1, firstCount.join());
        assertEquals(2, secondCount.join());
        assertNotEquals(firstSession.id(), secondSession.id());
        assertTrue(firstSession.events().stream().allMatch(e -> !String.valueOf(e.attributes()).contains("Second rows")));
        assertTrue(secondSession.events().stream().allMatch(e -> !String.valueOf(e.attributes()).contains("First rows")));
    }

    private static UiLocator locator(WebDriver driver) { return overlay(driver).locator(ROWS, "Rows", options()); }
    private static JsOverlayDebug overlay(WebDriver driver) {
        return new JsOverlayDebug(driver, OverlayConfig.builder().enabled(false).showHudPanel(false).build());
    }
    private static UiLocatorOptions options() {
        return UiLocatorOptions.builder().timeout(Duration.ofMillis(120)).pollInterval(Duration.ofMillis(5)).build();
    }

    private static WebElement element(String name, List<String> clicks) {
        return (WebElement) Proxy.newProxyInstance(UiLocatorCollectionTest.class.getClassLoader(), new Class[]{WebElement.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "click" -> { if (clicks != null) clicks.add(name); yield null; }
                    case "isDisplayed", "isEnabled" -> true;
                    case "getText", "getAttribute", "getDomProperty" -> name;
                    case "toString" -> name;
                    default -> null;
                });
    }

    private static WebDriver browser(List<WebElement> elements) {
        return browserHandler((method, args) -> method.equals("findElements") ? elements :
                method.equals("findElement") && !elements.isEmpty() ? elements.get(0) : null);
    }

    private static WebDriver browserDynamic(AtomicInteger calls, WebElement target) {
        WebElement initial = element("initial", null);
        return browserHandler((method, args) -> {
            if (!method.equals("findElements")) return method.equals("findElement") ? initial : null;
            return calls.getAndIncrement() == 0 ? List.of(initial) : List.of(initial, target);
        });
    }

    private static WebDriver browserHandler(BrowserCall call) {
        return (WebDriver) Proxy.newProxyInstance(UiLocatorCollectionTest.class.getClassLoader(),
                new Class[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().startsWith("execute")) return null;
                    if (method.getName().equals("toString")) return "collection-driver";
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    if (method.getName().equals("findElement")) throw new NoSuchElementException("missing");
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                });
    }

    @FunctionalInterface private interface BrowserCall { Object invoke(String method, Object[] args); }
}
