package io.github.testlens.react;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReactSafeExecutorRetryTest {
    @Test
    void recordsOnlyFailedOperationsThatScheduleAnotherPhysicalAttempt() {
        AtomicInteger operationCalls = new AtomicInteger();
        List<String> retries = new ArrayList<>();
        ReactSafeExecutor executor = new ReactSafeExecutor(driver(element()), null, 3,
                Duration.ZERO, Duration.ofMillis(20),
                (action, locator, attempt, nextAttempt, failure, durationNanos) ->
                        retries.add(action + ":" + locator + ":" + attempt + ":" + nextAttempt
                                + ":" + failure.getClass().getName()));

        assertThrows(RuntimeException.class, () -> executor.doWithRetry(By.id("save"), "click", element -> {
            operationCalls.incrementAndGet();
            throw new StaleElementReferenceException("replaced");
        }));

        assertEquals(3, operationCalls.get());
        assertEquals(List.of(
                "click:By.id: save:1:2:" + StaleElementReferenceException.class.getName(),
                "click:By.id: save:2:3:" + StaleElementReferenceException.class.getName()), retries);
    }

    private static WebDriver driver(WebElement element) {
        return (WebDriver) Proxy.newProxyInstance(ReactSafeExecutorRetryTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) return element;
                    if (method.getName().equals("toString")) return "driver";
                    return defaultValue(method.getReturnType());
                });
    }

    private static WebElement element() {
        return (WebElement) Proxy.newProxyInstance(ReactSafeExecutorRetryTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("toString")) return "element";
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        return null;
    }
}
