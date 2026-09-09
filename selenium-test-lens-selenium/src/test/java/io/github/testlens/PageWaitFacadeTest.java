package io.github.testlens;

import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageWaitFacadeTest {
    @Test
    void testLensDefaultsUseConfiguredLocatorTimeoutAndPollInterval() {
        WebDriver driver = readyDriver();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(disabledOverlay())
                .locatorOptions(UiLocatorOptions.builder()
                        .timeout(Duration.ofMillis(1234))
                        .pollInterval(Duration.ofMillis(37))
                        .build())
                .build());
        UiTestLensSession session = lens.startSession("page-wait-options");

        lens.waitForPageReady();

        TraceEvent started = waitEvents(session).get(0);
        assertEquals("1234", started.attributes().get("metadata.timeoutMs"));
        assertEquals("37", started.attributes().get("metadata.pollIntervalMs"));
        assertEquals(0, session.retrySummary().totalRetries());
    }

    @Test
    void explicitTimeoutWinsAndBothFacadesEmitOneStartAndOneTerminalEvent() {
        WebDriver testLensDriver = readyDriver();
        TestLens lens = TestLens.attach(testLensDriver, TestLensOptions.builder()
                .overlayConfig(disabledOverlay())
                .locatorOptions(UiLocatorOptions.builder()
                        .timeout(Duration.ofSeconds(3)).pollInterval(Duration.ofMillis(29)).build())
                .build());
        UiTestLensSession lensSession = lens.startSession("test-lens-page-wait");
        lens.waitForInteractiveOrComplete(Duration.ofMillis(222));
        List<TraceEvent> lensEvents = waitEvents(lensSession);
        assertEquals(2, lensEvents.size());
        assertEquals("222", lensEvents.get(0).attributes().get("metadata.timeoutMs"));
        assertEquals("29", lensEvents.get(0).attributes().get("metadata.pollIntervalMs"));

        JsOverlayDebug legacy = new JsOverlayDebug(readyDriver(), disabledOverlay());
        UiTestLensSession legacySession = legacy.startSession("legacy-page-wait");
        legacy.waitForPageReady(Duration.ofMillis(222));
        List<TraceEvent> legacyEvents = waitEvents(legacySession);
        assertEquals(2, legacyEvents.size());
        assertTrue(legacyEvents.get(0).attributes().get("metadata.pollIntervalMs").equals("100"));
    }

    @Test
    void lastWaitHudMessageUsesTextNodesInsteadOfExecutableHtml() {
        List<String> scripts = new ArrayList<>();
        JsOverlayDebug legacy = new JsOverlayDebug(readyDriver(scripts), disabledOverlay());

        legacy.showLastWaitInHud();

        String script = scripts.get(scripts.size() - 1);
        assertTrue(script.contains("textContent"));
        assertTrue(script.contains("createTextNode"));
        assertFalse(script.contains("innerHTML"));
    }

    private static List<TraceEvent> waitEvents(UiTestLensSession session) {
        return session.events().stream()
                .filter(event -> "page.wait".equals(event.attributes().get("action")))
                .toList();
    }

    private static OverlayConfig disabledOverlay() {
        return OverlayConfig.builder().enabled(false).showHudPanel(false).build();
    }

    private static WebDriver readyDriver() {
        return readyDriver(new ArrayList<>());
    }

    private static WebDriver readyDriver(List<String> scripts) {
        return (WebDriver) Proxy.newProxyInstance(PageWaitFacadeTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeScript")) {
                        scripts.add((String) args[0]);
                        return "return document.readyState".equals(args[0]) ? "complete" : null;
                    }
                    if (method.getName().equals("toString")) return "ReadyDriver";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    if (!method.getReturnType().isPrimitive()) return null;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == char.class) return '\0';
                    return 0;
                });
    }
}
