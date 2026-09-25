package io.github.testlens;

import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.openqa.selenium.support.events.WebDriverListener;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NativeSeleniumObservationTest {
    @Test void driverFacadeIsStableAndRawDriverContractDoesNotChange() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);

        assertSame(fixture.driver, lens.driver());
        assertSame(lens.observeDriver(), lens.observeDriver());
        assertNotSame(fixture.driver, lens.observeDriver());
    }

    @Test void elementObservationIsLazyAndDoesNotDoubleWrapAnObservedElement() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        WebElement raw = fixture.element;

        WebElement first = lens.observe(raw);
        WebElement repeated = lens.observe(first);
        WebElement labelled = lens.observe(first, "Save");

        assertSame(first, repeated);
        assertNotSame(first, labelled);
        assertEquals(0, fixture.clicks.get());
        assertEquals(0, fixture.finds.get());
    }

    @Test void nativeActionsDelegateExactlyOnceAndKeepOneLogicalOperation() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        UiTestLensSession session = lens.startSession("native actions");
        WebElement element = lens.observeDriver().findElement(By.id("save"));

        element.click();
        element.clear();
        element.sendKeys("secret-canary");

        assertEquals(1, fixture.finds.get());
        assertEquals(1, fixture.clicks.get());
        assertEquals(1, fixture.clears.get());
        assertEquals(1, fixture.sendKeys.get());
        assertEquals(6, session.events().stream()
                .filter(event -> event.attributes().getOrDefault("action", "").matches("selenium\\.(click|clear|sendKeys)"))
                .count());
        assertFalse(session.events().toString().contains("secret-canary"));
    }

    @Test void nativeFailureReturnsTheSameSeleniumExceptionWithoutFallbackOrRetry() {
        Fixture fixture = new Fixture();
        fixture.clickFailure = new ElementClickInterceptedException("blocked");
        TestLens lens = lens(fixture.driver);
        UiTestLensSession session = lens.startSession("native failure");
        WebElement element = lens.observe(fixture.element, "Save");

        ElementClickInterceptedException actual = assertThrows(ElementClickInterceptedException.class, element::click);

        assertSame(fixture.clickFailure, actual);
        assertEquals(1, fixture.clicks.get());
        assertTrue(session.events().stream().anyMatch(event -> "selenium.click".equals(event.attributes().get("action"))
                && event.status() == io.github.testlens.core.trace.TraceStatus.FAILED));
    }

    @Test void diagnosticFailureCannotPreventOrRepeatTheNativeAction() {
        Fixture fixture = new Fixture();
        fixture.scriptFailure = new org.openqa.selenium.WebDriverException("diagnostic script unavailable");
        TestLens lens = TestLens.attach(fixture.driver);
        lens.startSession("diagnostic failure");

        lens.observe(fixture.element, "Save").click();

        assertEquals(1, fixture.clicks.get());
    }

    @Test void labelsAndFailureDiagnosticsUseRedactionWithoutChangingTheOriginalException() {
        Fixture fixture = new Fixture();
        fixture.clickFailure = new ElementClickInterceptedException("token=top-secret");
        TestLens lens = TestLens.attach(fixture.driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().enabled(false).showHudPanel(false).build())
                .redactionPolicy(RedactionPolicy.builder().secret("hunter2").build())
                .build());
        UiTestLensSession session = lens.startSession("redaction");

        ElementClickInterceptedException failure = assertThrows(ElementClickInterceptedException.class,
                () -> lens.observe(fixture.element, "password=hunter2").click());

        assertSame(fixture.clickFailure, failure);
        String exported = session.exportJson();
        assertFalse(exported.contains("hunter2"));
        assertFalse(exported.contains("top-secret"));
    }

    @Test void findMetadataAddsNoSecondLookupAndReadsStayTechnical() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        UiTestLensSession session = lens.startSession("metadata");

        WebElement found = lens.observeDriver().findElement(By.cssSelector("button[data-token='secret']"));
        assertEquals("Save", found.getText());
        assertTrue(found.isDisplayed());

        assertEquals(1, fixture.finds.get());
        assertEquals(0, session.events().stream()
                .filter(event -> event.attributes().getOrDefault("action", "").matches("selenium\\.(click|clear|sendKeys)"))
                .count());
        assertTrue(session.events().stream().allMatch(event -> !event.toString().contains("data-token='secret'")));
    }

    @Test void childAndListResultsRemainObservedWithoutAdditionalFinds() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        lens.startSession("children");
        WebDriver driver = lens.observeDriver();

        WebElement parent = driver.findElement(By.id("parent"));
        parent.findElement(By.className("child")).click();
        driver.findElements(By.cssSelector(".item")).get(0).click();

        assertEquals(3, fixture.finds.get());
        assertEquals(2, fixture.clicks.get());
    }

    @Test void explicitJavascriptTargetProducesOneScriptOperationAndOneDispatch() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        UiTestLensSession session = lens.startSession("javascript");
        WebElement target = lens.observe(fixture.element, "Save");

        ((JavascriptExecutor) lens.observeDriver()).executeScript("arguments[0].click();", target);

        assertEquals(1, fixture.userScripts.get());
        assertEquals(2, session.events().stream()
                .filter(event -> "selenium.executeScript".equals(event.attributes().get("action")))
                .count());
    }

    @Test void rawExecutorAcceptsObservedElementWithoutFabricatingAnObservedScriptOperation() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        UiTestLensSession session = lens.startSession("raw javascript");

        ((JavascriptExecutor) fixture.driver).executeScript("return arguments[0];", lens.observe(fixture.element, "Save"));

        assertEquals(1, fixture.userScripts.get());
        assertEquals(0, session.events().stream()
                .filter(event -> "selenium.executeScript".equals(event.attributes().get("action")))
                .count());
    }

    @Test void callsOutsideAnActiveLensSessionOnlyDelegate() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        WebElement element = lens.observe(fixture.element);

        element.click();
        lens.startSession("finished");
        lens.finishPassed();
        element.click();

        assertEquals(2, fixture.clicks.get());
    }

    @Test void observedQuitDelegatesOnce() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        lens.startSession("quit");

        lens.observeDriver().quit();

        assertEquals(1, fixture.quits.get());
    }

    @Test void equalityAndHashingAreLocalAndDoNotCreateOperationsOrRemoteCalls() {
        Fixture fixture = new Fixture();
        TestLens lens = lens(fixture.driver);
        UiTestLensSession session = lens.startSession("identity");
        WebElement first = lens.observe(fixture.element);
        WebElement labelled = lens.observe(first, "Save");

        int eventsBefore = session.events().size();
        int findsBefore = fixture.finds.get();
        assertEquals(first, first);
        assertEquals(first.hashCode(), first.hashCode());
        assertEquals(first, labelled, "views over the same Selenium element retain Selenium equality");
        assertEquals(eventsBefore, session.events().size());
        assertEquals(findsBefore, fixture.finds.get());
    }

    @Test void existingConsumerDecoratorStillReceivesTheSingleNativeAction() {
        Fixture fixture = new Fixture();
        CountingListener listener = new CountingListener();
        WebDriver decorated = new EventFiringDecorator<>(listener).decorate(fixture.driver);
        TestLens lens = lens(decorated);
        lens.startSession("consumer decorator");

        lens.observeDriver().findElement(By.id("save")).click();

        assertEquals(1, listener.clicks.get());
        assertEquals(1, fixture.clicks.get());
        assertEquals(1, fixture.finds.get());
    }

    @Test void finishedLensWrapperCannotReportIntoAnotherLensSession() {
        Fixture fixture = new Fixture();
        TestLens firstLens = lens(fixture.driver);
        UiTestLensSession first = firstLens.startSession("first");
        WebElement firstView = firstLens.observe(fixture.element, "first view");
        firstView.click();
        firstLens.finishPassed();
        int firstEvents = first.events().size();

        TestLens secondLens = lens(fixture.driver);
        UiTestLensSession second = secondLens.startSession("second");
        firstView.click();
        secondLens.observe(fixture.element, "second view").click();

        assertEquals(firstEvents, first.events().size(), "finished Lens must not accept cross-session events");
        assertEquals(2, second.events().stream()
                .filter(event -> "selenium.click".equals(event.attributes().get("action"))).count());
        assertEquals(3, fixture.clicks.get(), "all native calls still delegate exactly once");
    }

    private static TestLens lens(WebDriver driver) {
        return TestLens.attach(driver, OverlayConfig.builder().enabled(false).showHudPanel(false).build());
    }

    public static final class CountingListener implements WebDriverListener {
        private final AtomicInteger clicks = new AtomicInteger();
        @Override public void beforeClick(WebElement element) { clicks.incrementAndGet(); }
    }

    private static final class Fixture {
        final AtomicInteger finds = new AtomicInteger();
        final AtomicInteger clicks = new AtomicInteger();
        final AtomicInteger clears = new AtomicInteger();
        final AtomicInteger sendKeys = new AtomicInteger();
        final AtomicInteger userScripts = new AtomicInteger();
        final AtomicInteger quits = new AtomicInteger();
        RuntimeException clickFailure;
        RuntimeException scriptFailure;
        final WebDriver driver;
        final WebElement element;

        private Fixture() {
            WebDriver[] driverRef = new WebDriver[1];
            element = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{WebElement.class, WrapsDriver.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "click" -> {
                            clicks.incrementAndGet();
                            if (clickFailure != null) throw clickFailure;
                            yield null;
                        }
                        case "clear" -> { clears.incrementAndGet(); yield null; }
                        case "sendKeys" -> { sendKeys.incrementAndGet(); yield null; }
                        case "findElement" -> { finds.incrementAndGet(); yield proxy; }
                        case "findElements" -> { finds.incrementAndGet(); yield List.of(proxy); }
                        case "getText" -> "Save";
                        case "isDisplayed", "isEnabled" -> true;
                        case "getWrappedDriver" -> driverRef[0];
                        case "toString" -> "fixture-element";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> primitiveDefault(method.getReturnType());
                    });
            driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "findElement" -> { finds.incrementAndGet(); yield element; }
                        case "findElements" -> { finds.incrementAndGet(); yield List.of(element); }
                        case "executeScript", "executeAsyncScript" -> {
                            userScripts.incrementAndGet();
                            if (scriptFailure != null) throw scriptFailure;
                            yield null;
                        }
                        case "quit" -> { quits.incrementAndGet(); yield null; }
                        case "toString" -> "fixture-driver";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> primitiveDefault(method.getReturnType());
                    });
            driverRef[0] = driver;
        }

        private static Object primitiveDefault(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0F;
            if (type == double.class) return 0D;
            return null;
        }
    }
}
