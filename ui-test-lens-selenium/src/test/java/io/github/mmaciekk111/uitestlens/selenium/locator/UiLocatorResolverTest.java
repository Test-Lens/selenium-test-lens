package io.github.mmaciekk111.uitestlens.selenium.locator;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UiLocatorResolverTest {

    @Test
    void resolvesElementWithWebDriverWait() {
        WebElement element = element();
        CountingDriver driver = new CountingDriver(element, 1);
        UiLocatorOptions options = UiLocatorOptions.builder()
                .timeout(Duration.ofMillis(200))
                .pollInterval(Duration.ofMillis(10))
                .build();

        WebElement resolved = new UiLocatorResolver(driver.proxy()).resolve(By.id("save"), options);

        assertSame(element, resolved);
    }

    @Test
    void returnsFailedResultWhenLocatorDoesNotResolve() {
        CountingDriver driver = new CountingDriver(element(), Integer.MAX_VALUE);
        UiLocatorOptions options = UiLocatorOptions.builder()
                .timeout(Duration.ofMillis(50))
                .pollInterval(Duration.ofMillis(10))
                .build();

        UiLocatorResult result = new UiLocatorResolver(driver.proxy()).resolveResult(By.id("missing"), options);

        assertEquals(UiLocatorStatus.FAILED, result.status());
        assertEquals(UiLocatorFailureReason.TIMEOUT, result.failureReason());
    }

    private static WebElement element() {
        return (WebElement) Proxy.newProxyInstance(
                UiLocatorResolverTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) {
                        return "FakeWebElement";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class CountingDriver {
        private final WebElement element;
        private final int failuresBeforeSuccess;
        private int calls;

        private CountingDriver(WebElement element, int failuresBeforeSuccess) {
            this.element = element;
            this.failuresBeforeSuccess = failuresBeforeSuccess;
        }

        private WebDriver proxy() {
            return (WebDriver) Proxy.newProxyInstance(
                    UiLocatorResolverTest.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class},
                    (proxy, method, args) -> {
                        if ("findElement".equals(method.getName())) {
                            calls++;
                            if (calls <= failuresBeforeSuccess) {
                                throw new NoSuchElementException("missing");
                            }
                            return element;
                        }
                        if ("toString".equals(method.getName())) {
                            return "FakeWebDriver";
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
