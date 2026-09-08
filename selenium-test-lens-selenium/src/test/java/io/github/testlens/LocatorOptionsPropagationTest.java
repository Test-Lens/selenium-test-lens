package io.github.testlens;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocatorOptionsPropagationTest {

    @Test
    void configuredRetryLimitAppliesEquallyToOrdinaryAndSemanticFactories() {
        DriverFixture fixture = new DriverFixture();
        TestLens lens = TestLens.attach(fixture.driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(false).showHudPanel(false).build())
                .locatorOptions(options(1))
                .build());

        assertThrows(RuntimeException.class, () -> lens.locator(By.id("target")).focus());
        assertEquals(1, fixture.scriptCalls.get(), "ordinary locator must use configured maxRetries");

        fixture.scriptCalls.set(0);
        assertThrows(RuntimeException.class, () -> lens.getByTestId("target").focus());
        assertEquals(1, fixture.scriptCalls.get(), "semantic factory must use the same configured maxRetries");
    }

    @Test
    void everyPublicGetByFactoryUsesTheFacadeOptions() {
        for (FacadeFactory factory : testLensFactories()) {
            DriverFixture fixture = new DriverFixture();
            TestLens lens = lens(fixture, options(1));
            assertRetries(factory.name(), factory.create().apply(lens), fixture, 1);
        }

        for (LegacyFactory factory : legacyFactories()) {
            DriverFixture fixture = new DriverFixture();
            JsOverlayDebug legacy = overlay(fixture, options(2));
            assertRetries(factory.name(), factory.create().apply(legacy), fixture, 2);
        }
    }

    @Test
    void inventoryCoversEveryPublicGetByOverload() {
        assertEquals(Set.of(
                        "getByTestId(String)",
                        "getByText(String)",
                        "getByText(String,String)",
                        "getByTextContaining(String)",
                        "getByPlaceholder(String)",
                        "getByLabel(String)",
                        "getByAltText(String)",
                        "getByRole(String)",
                        "getByRole(String,String)"),
                publicGetBySignatures(TestLens.class));
        assertEquals(Set.of(
                        "getByTestId(String)", "getByTestId(String,String)",
                        "getByPlaceholder(String)", "getByPlaceholder(String,String)",
                        "getByText(String)", "getByText(String,String)",
                        "getByTextContaining(String)", "getByTextContaining(String,String)",
                        "getByLabel(String)", "getByLabel(String,String)",
                        "getByRole(String)", "getByRole(String,String)",
                        "getByAltText(String)", "getByAltText(String,String)"),
                publicGetBySignatures(JsOverlayDebug.class));
    }

    @Test
    void explicitLocatorOptionsWinAndFacadeInstancesRemainIsolated() {
        DriverFixture fixture = new DriverFixture();
        JsOverlayDebug configured = overlay(fixture, options(1));
        assertRetries("explicit locator options", configured.locator(By.id("target"), options(2)), fixture, 2);

        TestLens oneAttempt = lens(fixture, options(1));
        TestLens twoAttempts = lens(fixture, options(2));
        assertRetries("first facade", oneAttempt.getByTestId("target"), fixture, 1);
        assertRetries("second facade", twoAttempts.getByTestId("target"), fixture, 2);
        assertRetries("first facade remains isolated", oneAttempt.getByTestId("target"), fixture, 1);
    }

    @Test
    void defaultsRemainThreeAttemptsForBothFacades() {
        DriverFixture lensFixture = new DriverFixture();
        TestLens lens = TestLens.attach(lensFixture.driver, disabledOverlay());
        assertRetries("TestLens defaults", lens.getByTestId("target"), lensFixture, 3);

        DriverFixture legacyFixture = new DriverFixture();
        JsOverlayDebug legacy = new JsOverlayDebug(legacyFixture.driver, disabledOverlay());
        assertRetries("JsOverlayDebug defaults", legacy.getByTestId("target"), legacyFixture, 3);
    }

    @Test
    void everyCompositionStageKeepsSourceOptions() {
        DriverFixture fixture = new DriverFixture();
        TestLens lens = lens(fixture, options(1));
        List<UiLocator> derived = List.of(
                lens.getByTestId("target").nth(0),
                lens.getByTestId("target").first(),
                lens.getByTestId("target").last(),
                lens.getByTestId("target").filterByText("target"),
                lens.getByTestId("target").filterByTextContaining("arg"),
                lens.getByTestId("target").filterByAttribute("data-testid", "target"),
                lens.getByTestId("target").filterHas(By.cssSelector(".child")),
                lens.getByTestId("target").filterHas(lens.getByRole("button", "target")),
                lens.getByTestId("target").locator(By.cssSelector(".child")),
                lens.getByTestId("target").locator(By.cssSelector(".child"), "child"),
                lens.getByTestId("target").locator(lens.getByRole("button", "target")));

        for (int index = 0; index < derived.size(); index++) {
            assertRetries("derived locator " + index, derived.get(index), fixture, 1);
        }
    }

    @Test
    void semanticResolverPollingIsNotARecoveryRetryAndTerminalErrorsStopImmediately() {
        DriverFixture polling = new DriverFixture();
        polling.emptyFinds = 2;
        TestLens lens = lens(polling, options(1));
        UiTestLensSession session = lens.startSession("semantic-polling");
        assertSame(polling.element, lens.getByRole("button", "target").resolve());
        assertEquals(3, polling.findCalls.get());
        assertEquals(0, session.retrySummary().totalRetries());

        DriverFixture terminal = new DriverFixture();
        terminal.findFailure = new org.openqa.selenium.WebDriverException("terminal lookup failure");
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> lens(terminal, options(3)).getByTestId("target").focus());
        assertEquals(1, terminal.findCalls.get());
        assertEquals(0, terminal.scriptCalls.get());
        assertTrue(hasCause(failure, terminal.findFailure));
    }

    private static List<FacadeFactory> testLensFactories() {
        return List.of(
                new FacadeFactory("getByTestId(String)", lens -> lens.getByTestId("target")),
                new FacadeFactory("getByText(String)", lens -> lens.getByText("target")),
                new FacadeFactory("getByText(String,String)", lens -> lens.getByText("target", "label")),
                new FacadeFactory("getByTextContaining(String)", lens -> lens.getByTextContaining("arg")),
                new FacadeFactory("getByPlaceholder(String)", lens -> lens.getByPlaceholder("target")),
                new FacadeFactory("getByLabel(String)", lens -> lens.getByLabel("target")),
                new FacadeFactory("getByAltText(String)", lens -> lens.getByAltText("target")),
                new FacadeFactory("getByRole(String)", lens -> lens.getByRole("button")),
                new FacadeFactory("getByRole(String,String)", lens -> lens.getByRole("button", "target")));
    }

    private static List<LegacyFactory> legacyFactories() {
        return List.of(
                new LegacyFactory("getByTestId(String)", lens -> lens.getByTestId("target")),
                new LegacyFactory("getByTestId(String,String)", lens -> lens.getByTestId("target", "label")),
                new LegacyFactory("getByPlaceholder(String)", lens -> lens.getByPlaceholder("target")),
                new LegacyFactory("getByPlaceholder(String,String)", lens -> lens.getByPlaceholder("target", "label")),
                new LegacyFactory("getByText(String)", lens -> lens.getByText("target")),
                new LegacyFactory("getByText(String,String)", lens -> lens.getByText("target", "label")),
                new LegacyFactory("getByTextContaining(String)", lens -> lens.getByTextContaining("arg")),
                new LegacyFactory("getByTextContaining(String,String)", lens -> lens.getByTextContaining("arg", "label")),
                new LegacyFactory("getByLabel(String)", lens -> lens.getByLabel("target")),
                new LegacyFactory("getByLabel(String,String)", lens -> lens.getByLabel("target", "label")),
                new LegacyFactory("getByRole(String)", lens -> lens.getByRole("button")),
                new LegacyFactory("getByRole(String,String)", lens -> lens.getByRole("button", "target")),
                new LegacyFactory("getByAltText(String)", lens -> lens.getByAltText("target")),
                new LegacyFactory("getByAltText(String,String)", lens -> lens.getByAltText("target", "label")));
    }

    private static Set<String> publicGetBySignatures(Class<?> type) {
        Set<String> signatures = new LinkedHashSet<>();
        Arrays.stream(type.getDeclaredMethods())
                .filter(method -> java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getName().startsWith("getBy"))
                .filter(method -> method.getReturnType() == io.github.testlens.selenium.locator.UiLocator.class)
                .map(method -> method.getName() + "(" + Arrays.stream(method.getParameterTypes())
                        .map(Class::getSimpleName).collect(java.util.stream.Collectors.joining(",")) + ")")
                .forEach(signatures::add);
        return signatures;
    }

    private static TestLens lens(DriverFixture fixture, UiLocatorOptions options) {
        return TestLens.attach(fixture.driver, TestLensOptions.builder()
                .overlayConfig(disabledOverlay()).locatorOptions(options).build());
    }

    private static JsOverlayDebug overlay(DriverFixture fixture, UiLocatorOptions options) {
        return new JsOverlayDebug(fixture.driver, disabledOverlay(), RedactionPolicy.defaults(), options);
    }

    private static OverlayConfig disabledOverlay() {
        return OverlayConfig.builder().enabled(false).showHudPanel(false).build();
    }

    private static void assertRetries(String name, io.github.testlens.selenium.locator.UiLocator locator,
                                      DriverFixture fixture, int expected) {
        fixture.scriptCalls.set(0);
        assertThrows(RuntimeException.class, locator::focus, name);
        assertEquals(expected, fixture.scriptCalls.get(), name);
    }

    private static boolean hasCause(Throwable failure, Throwable expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current == expected) return true;
        }
        return false;
    }

    private static UiLocatorOptions options(int maxRetries) {
        return UiLocatorOptions.builder()
                .timeout(Duration.ofMillis(50))
                .pollInterval(Duration.ofMillis(1))
                .maxRetries(maxRetries)
                .highlightBeforeAction(false)
                .build();
    }

    private static final class DriverFixture {
        private final AtomicInteger scriptCalls = new AtomicInteger();
        private final AtomicInteger findCalls = new AtomicInteger();
        private final WebElement element;
        private final WebDriver driver;
        private int emptyFinds;
        private RuntimeException findFailure;

        private DriverFixture() {
            element = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebElement.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "getText" -> "target";
                        case "getAccessibleName" -> "target";
                        case "getAriaRole" -> "button";
                        case "getDomAttribute", "getAttribute" -> attribute((String) args[0]);
                        case "findElements" -> List.of(proxy);
                        case "toString" -> "target-element";
                        default -> defaultValue(method.getReturnType());
                    });
            driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                        if (method.getName().equals("findElement")) {
                            findCalls.incrementAndGet();
                            if (findFailure != null) throw findFailure;
                            if (emptyFinds-- > 0) throw new org.openqa.selenium.NoSuchElementException("not yet present");
                            return element;
                        }
                        if (method.getName().equals("findElements")) {
                            findCalls.incrementAndGet();
                            if (findFailure != null) throw findFailure;
                            if (emptyFinds-- > 0) return List.of();
                            return List.of(element);
                        }
                        if (method.getName().equals("executeScript")) {
                            scriptCalls.incrementAndGet();
                            throw new StaleElementReferenceException("controlled stale element");
                        }
                        if (method.getName().equals("toString")) return "options-driver";
                        return defaultValue(method.getReturnType());
                    });
        }

        private static String attribute(String name) {
            return switch (name) {
                case "data-testid", "placeholder", "alt", "aria-label" -> "target";
                case "role" -> "button";
                default -> null;
            };
        }
    }

    private record FacadeFactory(String name, Function<TestLens, io.github.testlens.selenium.locator.UiLocator> create) {}
    private record LegacyFactory(String name, Function<JsOverlayDebug, io.github.testlens.selenium.locator.UiLocator> create) {}

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
