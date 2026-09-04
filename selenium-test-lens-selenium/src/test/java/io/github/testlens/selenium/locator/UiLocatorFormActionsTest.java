package io.github.testlens.selenium.locator;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.trace.TraceLogSink;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class UiLocatorFormActionsTest {
    @TempDir Path tempDir;

    @Test
    void nativeCheckboxActionsAreIdempotentAndEmitSafeMetadata() {
        ElementModel checkbox = ElementModel.input("checkbox", false);
        Harness harness = harness(() -> checkbox.element, true);

        assertSame(harness.locator, harness.locator.check());
        assertTrue(harness.locator.isChecked());
        harness.locator.check();
        assertEquals(1, checkbox.clicks.get());
        harness.locator.uncheck().uncheck();

        assertFalse(harness.locator.isChecked());
        assertEquals(2, checkbox.clicks.get());
        UiTestLensLogEntry passed = last(harness.entries, UiTestLensEventType.LOCATOR_ACTION_PASSED, "locator.uncheck");
        assertEquals("NATIVE_CHECKBOX", passed.metadata().get("controlKind"));
        assertEquals("CONTROL", passed.metadata().get("activationKind"));
        assertEquals("false", passed.metadata().get("clickPerformed"));
    }

    @Test
    void radioCanBeCheckedButCannotBeUnchecked() {
        ElementModel radio = ElementModel.input("RaDiO", false);
        Harness harness = harness(() -> radio.element, true);

        harness.locator.check();
        assertTrue(harness.locator.isChecked());
        UiLocatorException failure = assertThrows(UiLocatorException.class, harness.locator::uncheck);

        assertEquals(1, radio.clicks.get());
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("radio controls cannot be unchecked"));
    }

    @Test
    void hiddenInputUsesItsSingleVisibleLabelAndLabelOrChildResolveTheSameControl() {
        ElementModel checkbox = ElementModel.input("checkbox", false).displayed(false);
        ElementModel label = ElementModel.label(checkbox);
        checkbox.labels.add(label.element);
        label.onClick = () -> checkbox.selected.set(!checkbox.selected.get());
        ElementModel child = ElementModel.element("span");
        child.closestLabel = label;

        Harness inputHarness = harness(() -> checkbox.element, true);
        inputHarness.locator.check();
        assertEquals(0, checkbox.clicks.get());
        assertEquals(1, label.clicks.get());

        checkbox.selected.set(false);
        Harness labelHarness = harness(() -> label.element, true);
        labelHarness.locator.check();
        checkbox.selected.set(false);
        Harness childHarness = harness(() -> child.element, true);
        childHarness.locator.check();

        assertEquals(3, label.clicks.get());
        assertEquals("LABEL", last(childHarness.entries, UiTestLensEventType.LOCATOR_ACTION_PASSED,
                "locator.check").metadata().get("activationKind"));
    }

    @Test
    void hiddenInputWithoutUniqueVisibleLabelFailsWithoutClick() {
        ElementModel checkbox = ElementModel.input("checkbox", false).displayed(false);
        Harness harness = harness(() -> checkbox.element, true);

        assertFalse(harness.locator.isChecked());
        UiLocatorException failure = assertThrows(UiLocatorException.class, harness.locator::check);

        assertEquals(0, checkbox.clicks.get());
        assertTrue(failure.getCause().getMessage().contains("exactly one visible associated label"));
    }

    @Test
    void ariaCheckboxSwitchAndRadioUseOnlyAriaChecked() {
        for (String role : List.of("checkbox", "switch", "radio")) {
            ElementModel control = ElementModel.aria(role, "false");
            Harness harness = harness(() -> control.element, true);
            harness.locator.check();
            assertTrue(harness.locator.isChecked());
            assertEquals(1, control.clicks.get());
            if (!"radio".equals(role)) {
                harness.locator.uncheck();
                assertFalse(harness.locator.isChecked());
                assertEquals(2, control.clicks.get());
            } else {
                assertThrows(UiLocatorException.class, harness.locator::uncheck);
            }
        }
    }

    @Test
    void mixedIsNotCheckedAndOneActivationMustReachTheRequestedState() {
        ElementModel control = ElementModel.aria("checkbox", "mixed");
        Harness harness = harness(() -> control.element, true);

        assertFalse(harness.locator.isChecked());
        harness.locator.check();

        assertTrue(harness.locator.isChecked());
        assertEquals(1, control.clicks.get());
    }

    @Test
    void invalidCustomAndDisabledControlsFailTerminallyWithoutClick() {
        for (ElementModel model : List.of(
                ElementModel.element("div").attribute("class", "checkbox"),
                ElementModel.element("div").attribute("data-state", "checked"),
                ElementModel.aria("checkbox", null),
                ElementModel.aria("checkbox", "invalid"),
                ElementModel.input("checkbox", false).enabled(false),
                ElementModel.aria("switch", "false").attribute("aria-disabled", "true"))) {
            Harness harness = harness(() -> model.element, true);
            assertThrows(UiLocatorException.class, harness.locator::check);
            assertEquals(0, model.clicks.get());
        }
    }

    @Test
    void staleBeforeAndAfterAChangedClickNeverCauseDuplicateActivation() {
        ElementModel staleBefore = ElementModel.input("checkbox", false);
        staleBefore.staleSelectedReads.set(1);
        Harness before = harness(() -> staleBefore.element, true);
        before.locator.check();
        assertEquals(1, staleBefore.clicks.get());
        assertEquals(1, before.session.retrySummary().totalRetries());

        ElementModel first = ElementModel.input("checkbox", false);
        ElementModel replacement = ElementModel.input("checkbox", true);
        AtomicInteger resolutions = new AtomicInteger();
        first.onClick = () -> first.staleSelectedReads.set(Integer.MAX_VALUE);
        Harness after = harness(() -> resolutions.getAndIncrement() == 0 ? first.element : replacement.element, true);
        after.locator.check();
        assertEquals(1, first.clicks.get());
        assertEquals(0, replacement.clicks.get());
    }

    @Test
    void asynchronousConfirmationPollsWithoutRecoveryRetry() {
        ElementModel control = ElementModel.aria("checkbox", "false");
        control.onClick = () -> control.ariaReadsUntilChange.set(2);
        Harness harness = harness(() -> control.element, true);

        harness.locator.check();

        assertEquals(1, control.clicks.get());
        assertEquals(0, harness.session.retrySummary().totalRetries());
        assertTrue(Integer.parseInt(last(harness.entries, UiTestLensEventType.LOCATOR_ACTION_PASSED,
                "locator.check").metadata().get("confirmationAttempts")) >= 2);
    }

    @Test
    void nonTransientClickFailureIsTerminalAndOriginalCauseIsPreserved() {
        ElementModel checkbox = ElementModel.input("checkbox", false);
        WebDriverException original = new WebDriverException("terminal");
        checkbox.clickFailure = original;
        Harness harness = harness(() -> checkbox.element, true);

        UiLocatorException failure = assertThrows(UiLocatorException.class, harness.locator::check);

        assertSame(original, failure.getCause());
        assertEquals(1, checkbox.clicks.get());
        assertEquals(0, harness.session.retrySummary().totalRetries());
    }

    @Test
    void uploadUsesOneSendKeysForSingleAndMultipleHiddenFileInputs() throws Exception {
        Path first = Files.writeString(tempDir.resolve("first-secret.txt"), "one");
        Path second = Files.writeString(tempDir.resolve("second-secret.txt"), "two");
        ElementModel single = ElementModel.input("file", false).displayed(false);
        Harness one = harness(() -> single.element, true);
        one.locator.upload(first);
        assertEquals(1, single.sendKeysCalls.get());

        ElementModel multiple = ElementModel.input("file", false).displayed(false).attribute("multiple", "true");
        Harness many = harness(() -> multiple.element, true);
        many.locator.upload(first, second);
        assertEquals(1, multiple.sendKeysCalls.get());
        assertEquals(first.toAbsolutePath().normalize() + "\n" + second.toAbsolutePath().normalize(), multiple.payload);
        assertEquals("2", last(many.entries, UiTestLensEventType.LOCATOR_ACTION_PASSED,
                "locator.upload").metadata().get("fileCount"));
        assertTrue(many.entries.stream().noneMatch(entry -> entry.toString().contains("first-secret")));
    }

    @Test
    void uploadValidatesEverythingBeforeSendKeysAndNeverRetriesSendFailure() throws Exception {
        Path file = Files.writeString(tempDir.resolve("private-name.txt"), "data");
        Path directory = Files.createDirectory(tempDir.resolve("directory"));
        ElementModel noMultiple = ElementModel.input("file", false);
        Harness harness = harness(() -> noMultiple.element, true);

        assertThrows(UiLocatorException.class, () -> harness.locator.upload((Path[]) null));
        assertThrows(UiLocatorException.class, () -> harness.locator.upload());
        assertThrows(UiLocatorException.class, () -> harness.locator.upload(new Path[]{null}));
        assertThrows(UiLocatorException.class, () -> harness.locator.upload(tempDir.resolve("missing.txt")));
        assertThrows(UiLocatorException.class, () -> harness.locator.upload(directory));
        assertThrows(UiLocatorException.class, () -> harness.locator.upload(file, file));
        assertEquals(0, noMultiple.sendKeysCalls.get());

        ElementModel wrong = ElementModel.input("text", false);
        assertThrows(UiLocatorException.class, () -> harness(() -> wrong.element, true).locator.upload(file));
        ElementModel failing = ElementModel.input("file", false);
        WebDriverException original = new WebDriverException("send failed " + file.toAbsolutePath());
        failing.sendKeysFailure = original;
        Harness failingHarness = harness(() -> failing.element, true);
        UiLocatorException failure = assertThrows(UiLocatorException.class, () -> failingHarness.locator.upload(file));
        assertSame(original, failure.getCause());
        assertEquals(1, failing.sendKeysCalls.get());
        assertFalse(failure.getMessage().contains("private-name"));
        assertTrue(failingHarness.entries.stream().noneMatch(entry -> entry.toString().contains("private-name")));
    }

    @Test
    void focusAndScrollUseOneScriptAndNeverClick() {
        ElementModel element = ElementModel.element("input");
        Harness harness = harness(() -> element.element, true);

        harness.locator.focus().scrollIntoView();

        assertEquals(1, harness.driver.focusCalls.get());
        assertEquals(1, harness.driver.scrollCalls.get());
        assertEquals(0, element.clicks.get());
        assertEquals(List.of("locator.focus", "locator.scrollIntoView"), harness.entries.stream()
                .filter(entry -> entry.eventType() == UiTestLensEventType.LOCATOR_ACTION_PASSED)
                .map(UiTestLensLogEntry::action).toList());
    }

    @Test
    void javascriptActionsRetryOnlyStaleAndRejectNonJavascriptDriver() {
        ElementModel first = ElementModel.element("div");
        DriverModel driver = new DriverModel(() -> first.element);
        driver.scriptFailure = new StaleElementReferenceException("stale");
        driver.scriptFailures.set(1);
        Harness retry = harness(driver, true);
        retry.locator.focus();
        assertEquals(2, driver.focusCalls.get());
        assertEquals(1, retry.session.retrySummary().totalRetries());

        DriverModel terminalDriver = new DriverModel(() -> first.element);
        terminalDriver.scriptFailure = new WebDriverException("terminal");
        terminalDriver.scriptFailures.set(3);
        Harness terminal = harness(terminalDriver, true);
        assertThrows(UiLocatorException.class, terminal.locator::scrollIntoView);
        assertEquals(1, terminalDriver.scrollCalls.get());

        WebDriver plain = new DriverModel(() -> first.element).plainDriver;
        UiLocator locator = new UiLocator(plain, By.id("control"), "Control",
                new JsOverlayDebug(new DriverModel(() -> first.element).driver), options(), OverlayLogger.noop());
        assertThrows(UiLocatorException.class, locator::focus);
        assertThrows(UiLocatorException.class, locator::scrollIntoView);
    }

    private Harness harness(Supplier<WebElement> element, boolean javascript) {
        return harness(new DriverModel(element), javascript);
    }

    private Harness harness(DriverModel driver, boolean javascript) {
        UiTestLensSession session = UiTestLensSession.start("form-actions");
        List<UiTestLensLogEntry> entries = new ArrayList<>();
        OverlayLogger logger = OverlayLogger.from(UiTestLensLogger.builder()
                .sink(entries::add).sink(new TraceLogSink(session)).build());
        WebDriver webDriver = javascript ? driver.driver : driver.plainDriver;
        UiLocator locator = new UiLocator(webDriver, By.id("control"), "Control",
                new JsOverlayDebug(driver.driver), options(), logger);
        return new Harness(locator, driver, entries, session);
    }

    private static UiLocatorOptions options() {
        return UiLocatorOptions.builder().timeout(Duration.ofMillis(80)).pollInterval(Duration.ofMillis(1))
                .maxRetries(3).build();
    }

    private static UiTestLensLogEntry last(List<UiTestLensLogEntry> entries,
                                           UiTestLensEventType type, String action) {
        return entries.stream().filter(entry -> entry.eventType() == type && action.equals(entry.action()))
                .reduce((first, second) -> second).orElseThrow();
    }

    private record Harness(UiLocator locator, DriverModel driver, List<UiTestLensLogEntry> entries,
                           UiTestLensSession session) {}

    private static final class DriverModel {
        private final Supplier<WebElement> element;
        private final AtomicInteger focusCalls = new AtomicInteger();
        private final AtomicInteger scrollCalls = new AtomicInteger();
        private final AtomicInteger scriptFailures = new AtomicInteger();
        private RuntimeException scriptFailure;
        private final WebDriver driver;
        private final WebDriver plainDriver;

        private DriverModel(Supplier<WebElement> element) {
            this.element = element;
            this.driver = proxy(true);
            this.plainDriver = proxy(false);
        }

        private WebDriver proxy(boolean javascript) {
            Class<?>[] interfaces = javascript
                    ? new Class<?>[]{WebDriver.class, JavascriptExecutor.class}
                    : new Class<?>[]{WebDriver.class};
            return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, (proxy, method, args) -> {
                switch (method.getName()) {
                    case "findElement" -> { return element.get(); }
                    case "findElements" -> { return List.of(); }
                    case "executeScript" -> {
                        String script = String.valueOf(args[0]);
                        if (script.contains(".labels")) return ((ElementModel) model(scriptArgument(args, 0))).labels;
                        if (script.contains("closest('label')")) {
                            ElementModel origin = (ElementModel) model(scriptArgument(args, 0));
                            ElementModel label = "label".equals(origin.tag) ? origin : origin.closestLabel;
                            return label == null || label.labelControl == null
                                    ? null : List.of(label.labelControl.element, label.element);
                        }
                        if (script.contains("preventScroll")) focusCalls.incrementAndGet();
                        if (script.contains("scrollIntoView")) scrollCalls.incrementAndGet();
                        if (scriptFailures.getAndUpdate(value -> Math.max(0, value - 1)) > 0) throw scriptFailure;
                        return null;
                    }
                    case "executeAsyncScript" -> { return null; }
                    case "toString" -> { return "FormActionsDriver"; }
                    default -> { return defaultValue(method.getReturnType()); }
                }
            });
        }
    }

    private static final Map<WebElement, ElementModel> MODELS = new java.util.WeakHashMap<>();

    private static Object model(Object element) {
        return MODELS.get(element);
    }

    private static Object scriptArgument(Object[] invocationArguments, int index) {
        Object[] arguments = (Object[]) invocationArguments[1];
        return arguments[index];
    }

    private static final class ElementModel {
        private final String tag;
        private final Map<String, String> attributes = new HashMap<>();
        private final AtomicBoolean selected = new AtomicBoolean();
        private final AtomicBoolean displayed = new AtomicBoolean(true);
        private final AtomicBoolean enabled = new AtomicBoolean(true);
        private final AtomicInteger clicks = new AtomicInteger();
        private final AtomicInteger sendKeysCalls = new AtomicInteger();
        private final AtomicInteger staleSelectedReads = new AtomicInteger();
        private final AtomicInteger ariaReadsUntilChange = new AtomicInteger();
        private final List<WebElement> labels = new ArrayList<>();
        private ElementModel labelControl;
        private ElementModel closestLabel;
        private Runnable onClick;
        private RuntimeException clickFailure;
        private RuntimeException sendKeysFailure;
        private String payload;
        private final WebElement element;

        private ElementModel(String tag) {
            this.tag = tag;
            this.element = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "getTagName" -> { return this.tag; }
                            case "getDomAttribute", "getAttribute" -> {
                                String name = String.valueOf(args[0]);
                                if ("aria-checked".equals(name) && ariaReadsUntilChange.get() > 0) {
                                    if (ariaReadsUntilChange.decrementAndGet() == 0) attributes.put(name, "true");
                                    return "false";
                                }
                                return attributes.get(name);
                            }
                            case "getDomProperty" -> { return attributes.get(String.valueOf(args[0])); }
                            case "isSelected" -> {
                                if (staleSelectedReads.getAndUpdate(value -> Math.max(0, value - 1)) > 0) {
                                    throw new StaleElementReferenceException("stale state");
                                }
                                return selected.get();
                            }
                            case "isDisplayed" -> { return displayed.get(); }
                            case "isEnabled" -> { return enabled.get(); }
                            case "getRect" -> { return new Rectangle(0, 0, 20, 20); }
                            case "click" -> {
                                clicks.incrementAndGet();
                                if (clickFailure != null) throw clickFailure;
                                if (onClick != null) onClick.run();
                                else if ("checkbox".equalsIgnoreCase(attributes.get("type"))) selected.set(!selected.get());
                                else if ("radio".equalsIgnoreCase(attributes.get("type"))) selected.set(true);
                                else if (attributes.containsKey("aria-checked")) {
                                    attributes.put("aria-checked", "true".equals(attributes.get("aria-checked"))
                                            ? "false" : "true");
                                }
                                return null;
                            }
                            case "sendKeys" -> {
                                sendKeysCalls.incrementAndGet();
                                CharSequence[] values = (CharSequence[]) args[0];
                                payload = values.length == 0 ? "" : values[0].toString();
                                if (sendKeysFailure != null) throw sendKeysFailure;
                                return null;
                            }
                            case "toString" -> { return "ElementModel[" + this.tag + "]"; }
                            default -> { return defaultValue(method.getReturnType()); }
                        }
                    });
            MODELS.put(element, this);
        }

        private static ElementModel element(String tag) { return new ElementModel(tag); }
        private static ElementModel input(String type, boolean selected) {
            ElementModel model = new ElementModel("input").attribute("type", type);
            model.selected.set(selected);
            return model;
        }
        private static ElementModel aria(String role, String checked) {
            ElementModel model = new ElementModel("div").attribute("role", role);
            if (checked != null) model.attribute("aria-checked", checked);
            return model;
        }
        private static ElementModel label(ElementModel control) {
            ElementModel label = new ElementModel("label");
            label.labelControl = control;
            return label;
        }
        private ElementModel attribute(String name, String value) { attributes.put(name, value); return this; }
        private ElementModel displayed(boolean value) { displayed.set(value); return this; }
        private ElementModel enabled(boolean value) { enabled.set(value); return this; }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
