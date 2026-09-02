package io.github.testlens.selenium.locator;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UiLocatorResolverTest {

    @Test
    void resolveReturnsFirstElementWithoutASecondLookup() {
        WebElement element = element();
        CountingDriver driver = CountingDriver.succeedsAfter(element, 0);

        WebElement resolved = new UiLocatorResolver(driver.proxy()).resolve(By.id("save"), options());

        assertSame(element, resolved);
        assertEquals(1, driver.calls());
    }

    @Test
    void resolveSucceedsAfterSeveralPollsAndReturnsThatCyclesElement() {
        WebElement element = element();
        CountingDriver driver = CountingDriver.succeedsAfter(element, 3);

        WebElement resolved = new UiLocatorResolver(driver.proxy()).resolve(By.id("save"), options());

        assertSame(element, resolved);
        assertEquals(4, driver.calls());
    }

    @Test
    void missingElementIsReportedAsTimeoutWithActualPollCountAndElapsed() {
        CountingDriver driver = CountingDriver.succeedsAfter(element(), Integer.MAX_VALUE);
        UiLocatorOptions timeoutOptions = UiLocatorOptions.builder()
                .timeout(Duration.ofMillis(55))
                .pollInterval(Duration.ofMillis(10))
                .build();

        UiLocatorResult result = new UiLocatorResolver(driver.proxy())
                .resolveResult(By.id("missing"), timeoutOptions);

        assertEquals(UiLocatorStatus.FAILED, result.status());
        assertEquals(UiLocatorFailureReason.TIMEOUT, result.failureReason());
        assertEquals(driver.calls(), result.attempts());
        assertTrue(result.attempts() > 1);
        assertTrue(result.elapsed().compareTo(timeoutOptions.timeout()) >= 0);
    }

    @Test
    void invalidSelectorStopsImmediatelyAndIsNotReportedAsTimeout() {
        InvalidSelectorException original = new InvalidSelectorException("invalid selector");
        CountingDriver driver = CountingDriver.failsWith(original);

        UiLocatorResult result = new UiLocatorResolver(driver.proxy())
                .resolveResult(By.cssSelector("["), options());

        assertEquals(UiLocatorStatus.FAILED, result.status());
        assertEquals(UiLocatorFailureReason.UNKNOWN, result.failureReason());
        assertEquals(1, result.attempts());
        assertEquals(1, driver.calls());
    }

    @Test
    void noSuchSessionStopsImmediatelyAndRemainsTheExceptionCause() {
        NoSuchSessionException original = new NoSuchSessionException("session closed");
        CountingDriver driver = CountingDriver.failsWith(original);

        UiLocatorException thrown = assertThrows(UiLocatorException.class,
                () -> new UiLocatorResolver(driver.proxy()).resolve(By.id("save"), options()));

        assertSame(original, thrown.getCause());
        assertEquals(1, driver.calls());
    }

    @Test
    void resolvePreservesOriginalNonTransientWebDriverExceptionAsCause() {
        WebDriverException original = new WebDriverException("browser failed");
        CountingDriver driver = CountingDriver.failsWith(original);

        UiLocatorException thrown = assertThrows(UiLocatorException.class,
                () -> new UiLocatorResolver(driver.proxy()).resolve(By.id("save"), options()));

        assertSame(original, thrown.getCause());
        assertInstanceOf(WebDriverException.class, thrown.getCause());
        assertEquals(1, driver.calls());
    }

    @Test
    void resultReportsRealAttemptsInsteadOfConstantOne() {
        CountingDriver driver = CountingDriver.succeedsAfter(element(), 4);

        UiLocatorResult result = new UiLocatorResolver(driver.proxy())
                .resolveResult(By.id("save"), options());

        assertEquals(UiLocatorStatus.PASSED, result.status());
        assertEquals(5, result.attempts());
        assertEquals(driver.calls(), result.attempts());
        assertTrue(result.elapsed().toNanos() > 0);
    }

    private static UiLocatorOptions options() {
        return UiLocatorOptions.builder()
                .timeout(Duration.ofMillis(250))
                .pollInterval(Duration.ofMillis(10))
                .build();
    }

    private static WebElement element() {
        return (WebElement) Proxy.newProxyInstance(
                UiLocatorResolverTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) return "FakeWebElement";
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class CountingDriver {
        private final WebElement element;
        private final int failuresBeforeSuccess;
        private final RuntimeException terminalFailure;
        private int calls;

        private CountingDriver(WebElement element, int failuresBeforeSuccess, RuntimeException terminalFailure) {
            this.element = element;
            this.failuresBeforeSuccess = failuresBeforeSuccess;
            this.terminalFailure = terminalFailure;
        }

        static CountingDriver succeedsAfter(WebElement element, int failuresBeforeSuccess) {
            return new CountingDriver(element, failuresBeforeSuccess, null);
        }

        static CountingDriver failsWith(RuntimeException failure) {
            return new CountingDriver(null, 0, failure);
        }

        int calls() {
            return calls;
        }

        WebDriver proxy() {
            return (WebDriver) Proxy.newProxyInstance(
                    UiLocatorResolverTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class},
                    (proxy, method, args) -> {
                        if ("findElement".equals(method.getName())) {
                            calls++;
                            if (terminalFailure != null) throw terminalFailure;
                            if (calls <= failuresBeforeSuccess) throw new NoSuchElementException("missing");
                            return element;
                        }
                        if ("toString".equals(method.getName())) return "FakeWebDriver";
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
