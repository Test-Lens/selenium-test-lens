package io.github.testlens.application.mapper;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class PageScannerTest {
    @Test
    void passesHardDiscoveryBoundToSingleScriptAndPropagatesTruncation() {
        AtomicInteger scripts = new AtomicInteger();
        AtomicReference<Object[]> scriptArguments = new AtomicReference<>();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("executeScript")) {
                        scripts.incrementAndGet();
                        scriptArguments.set(((Object[]) arguments[1]).clone());
                        return Map.of(
                                "elements", List.of(),
                                "regions", List.of(),
                                "discovered", 50,
                                "truncated", true,
                                "closedShadowPossible", 0,
                                "shadowRoots", 0,
                                "shadowDepthReached", 0);
                    }
                    return defaultValue(method.getReturnType());
                });
        ApplicationMapperOptions options = ApplicationMapperOptions.builder("Example")
                .maxDiscoveredNodes(7)
                .maxRegions(3)
                .maxShadowDepth(2)
                .preferredTestAttributes(List.of("data-testid"))
                .build();

        PageScanner.Snapshot snapshot = new PageScanner().scan(driver, options);

        assertEquals(1, scripts.get());
        assertArrayEquals(new Object[]{7, 3, 2, List.of("data-testid")}, scriptArguments.get());
        assertEquals(50, snapshot.discoveredNodes());
        assertTrue(snapshot.truncated());
        assertTrue(snapshot.elements().isEmpty());
    }

    @Test
    void rejectsDriversWithoutJavascriptInsteadOfFallingBackToUnboundedSeleniumCalls() {
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, arguments) -> defaultValue(method.getReturnType()));

        MappingException failure = assertThrows(MappingException.class,
                () -> new PageScanner().scan(driver, ApplicationMapperOptions.builder("Example").build()));

        assertEquals(MappingException.Code.BROWSER_SCRIPT_FAILED, failure.code());
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }
}
