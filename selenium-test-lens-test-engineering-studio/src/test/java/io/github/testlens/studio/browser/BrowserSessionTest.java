package io.github.testlens.studio.browser;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BrowserSessionTest {
    @Test
    void studioOwnedSessionQuitsExactlyOnce() {
        AtomicInteger quits = new AtomicInteger();
        BrowserSession session = new BrowserSession(driver(quits), Ownership.STUDIO_OWNED);

        session.close();
        session.close();

        assertEquals(1, quits.get());
        assertTrue(session.closed());
    }

    @Test
    void callerOwnedSessionRemainsOpenWhenStudioClosesItsHandle() {
        AtomicInteger quits = new AtomicInteger();
        BrowserSession session = new BrowserSession(driver(quits), Ownership.CALLER_OWNED);

        session.close();

        assertEquals(0, quits.get());
        assertFalse(session.closed());
    }

    private static WebDriver driver(AtomicInteger quits) {
        return (WebDriver) Proxy.newProxyInstance(
                BrowserSessionTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("quit")) quits.incrementAndGet();
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
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
