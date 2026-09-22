package io.github.testlens.actions;

import io.github.testlens.HighlightOptions;
import io.github.testlens.HighlightState;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighlightActionsLifecycleTest {

    @Test
    void correlatesOneAutomaticLifecycleAndStartsTheNextOperationIndependently() {
        Fixture fixture = fixture(HighlightOptions.defaults());

        fixture.actions.automaticAction(fixture.element, "Save");
        assertFalse(fixture.actions.automaticActionIfAbsent(fixture.element, "internal smart click"));
        fixture.actions.highlight(fixture.element, "Save", HighlightState.SUCCESS, true);

        assertEquals(2, fixture.runtime.size());
        Map<String, Object> action = fixture.runtime.get(0);
        Map<String, Object> success = fixture.runtime.get(1);
        assertEquals(action.get("sessionId"), success.get("sessionId"));
        assertEquals(action.get("operationId"), success.get("operationId"));
        assertEquals("action", action.get("state"));
        assertEquals("success", success.get("state"));
        assertEquals(false, action.get("standalone"));

        fixture.actions.automaticAction(fixture.element, "Save again");
        assertEquals(3, fixture.runtime.size());
        assertNotEquals(action.get("operationId"), fixture.runtime.get(2).get("operationId"));
    }

    @Test
    void manualHighlightsAreStandaloneAndExplicitZeroSuppressionReachesRuntime() {
        HighlightOptions options = HighlightOptions.builder().durationMs(800).failureDurationMs(0).build();
        Fixture fixture = fixture(options);

        fixture.actions.highlight(fixture.element, "First", HighlightState.ACTION, false);
        fixture.actions.highlight(fixture.element, "Second", HighlightState.FAILURE, false);

        assertEquals(2, fixture.runtime.size());
        assertTrue((Boolean) fixture.runtime.get(0).get("standalone"));
        assertTrue((Boolean) fixture.runtime.get(1).get("standalone"));
        assertNotEquals(fixture.runtime.get(0).get("operationId"), fixture.runtime.get(1).get("operationId"));
        assertEquals(800L, fixture.runtime.get(0).get("duration"));
        assertEquals(false, fixture.runtime.get(0).get("suppress"));
        assertEquals(0L, fixture.runtime.get(1).get("duration"));
        assertEquals(true, fixture.runtime.get(1).get("suppress"));
    }

    private static Fixture fixture(HighlightOptions options) {
        List<Map<String, Object>> runtime = new ArrayList<>();
        WebElement element = (WebElement) Proxy.newProxyInstance(
                HighlightActionsLifecycleTest.class.getClassLoader(), new Class<?>[]{WebElement.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                HighlightActionsLifecycleTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("executeScript")) {
                        String script = String.valueOf(args[0]);
                        Object[] values = args.length > 1 && args[1] instanceof Object[] supplied
                                ? supplied : new Object[0];
                        if (script.contains("modules.highlight.element") && values.length == 3
                                && values[2] instanceof Map<?, ?> map) {
                            Map<String, Object> copy = new LinkedHashMap<>();
                            map.forEach((key, value) -> copy.put(String.valueOf(key), value));
                            runtime.add(copy);
                            return true;
                        }
                    }
                    return defaultValue(method.getReturnType());
                });
        OverlayConfig config = OverlayConfig.builder().enabled(true).showHudPanel(false)
                .highlightOptions(options).build();
        BrowserScriptExecutor executor = (script, args) -> null;
        HighlightActions actions = new HighlightActions(driver,
                new OverlayRootManager(executor, config), config, OverlayLogger.noop());
        return new Fixture(actions, element, runtime);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        if (type == char.class) return '\0';
        return null;
    }

    private record Fixture(HighlightActions actions, WebElement element, List<Map<String, Object>> runtime) {}
}
