package io.github.testlens.testng;

import io.github.testlens.selenium.execution.BrowserExecutionConfig;
import io.github.testlens.selenium.execution.HeadlessMode;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLensTestNgFactoryExecutionConfigTest {
    @Test
    void legacyFactoryDelegatesExactlyOnceForUnset() {
        AtomicInteger calls = new AtomicInteger();
        WebDriver expected = proxy();
        TestLensTestNgFactory factory = () -> { calls.incrementAndGet(); return expected; };
        assertSame(expected, factory.createDriver(resolve(HeadlessMode.UNSET)));
        assertEquals(1, calls.get());
    }

    @Test
    void legacyFactoryRejectsConfiguredModesBeforeCreation() {
        AtomicInteger calls = new AtomicInteger();
        TestLensTestNgFactory factory = () -> { calls.incrementAndGet(); return proxy(); };
        IllegalStateException headless = assertThrows(IllegalStateException.class,
                () -> factory.createDriver(resolve(HeadlessMode.TRUE)));
        assertTrue(headless.getMessage().contains("createDriver(BrowserExecutionConfig)"));
        assertThrows(IllegalStateException.class, () -> factory.createDriver(resolve(HeadlessMode.FALSE)));
        assertEquals(0, calls.get());
    }

    private static BrowserExecutionConfig resolve(HeadlessMode mode) {
        return BrowserExecutionConfig.resolve(mode);
    }

    private static WebDriver proxy() {
        return (WebDriver) java.lang.reflect.Proxy.newProxyInstance(WebDriver.class.getClassLoader(),
                new Class<?>[]{WebDriver.class}, (instance, method, arguments) -> null);
    }
}
