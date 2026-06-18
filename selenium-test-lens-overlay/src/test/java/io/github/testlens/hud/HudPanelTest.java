package io.github.testlens.hud;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.HudPanelJs;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPanelTest {

    @Test
    void initInjectsHudRuntimeAndCallsHudInit() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        HudPanel panel = hudPanel(executor);

        panel.init("Checkout test", "local");

        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains(HudPanelJs.INIT)));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("modules.hud")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("hud.init")));
        assertTrue(executor.args.stream().anyMatch(args -> Arrays.asList(args).contains("Checkout test")));
    }

    @Test
    void setStepCallsHudSetStep() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        HudPanel panel = hudPanel(executor);

        panel.updateStep("Open checkout");

        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("modules.hud")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("hud.setStep")));
        assertTrue(executor.args.stream().anyMatch(args -> Arrays.asList(args).contains("Open checkout")));
    }

    @Test
    void appendLogCallsHudLog() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        HudPanel panel = hudPanel(executor);

        panel.appendLog("info", "Checkout opened", "now");

        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("modules.hud")));
        assertTrue(executor.scripts.stream().anyMatch(script -> script.contains("hud.log")));
        assertTrue(executor.args.stream().anyMatch(args ->
                Arrays.asList(args).contains("info") &&
                        Arrays.asList(args).contains("Checkout opened") &&
                        Arrays.asList(args).contains("now")
        ));
    }

    @Test
    void disabledHudDoesNotExecuteScripts() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        OverlayConfig config = OverlayConfig.builder()
                .showHudPanel(false)
                .build();
        HudPanel panel = new HudPanel(executor, new OverlayRootManager(executor, config), config);

        panel.init("Checkout test", "local");
        panel.updateStep("Open checkout");
        panel.appendLog("info", "Checkout opened", "now");

        assertTrue(executor.scripts.isEmpty());
    }

    @Test
    void initPassesHudThemeToRuntime() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        OverlayConfig config = OverlayConfig.builder()
                .hudTheme(HudThemePreset.GLASS)
                .build();
        HudPanel panel = new HudPanel(executor, new OverlayRootManager(executor, config), config);

        panel.init("Checkout test", "local");

        Object[] hudInitArgs = executor.args.stream()
                .filter(args -> Arrays.asList(args).contains("Checkout test"))
                .findFirst()
                .orElseThrow();

        assertTrue(hudInitArgs[6] instanceof Map<?, ?>);
        assertEquals("#38bdf8", ((Map<?, ?>) hudInitArgs[6]).get("accent"));
        assertTrue(((String) ((Map<?, ?>) hudInitArgs[6]).get("background")).startsWith("linear-gradient(135deg"));
        assertEquals("blur(18px) saturate(160%)", ((Map<?, ?>) hudInitArgs[6]).get("backdropFilter"));
        assertEquals(480, ((Map<?, ?>) hudInitArgs[6]).get("maxHeightPx"));
        assertEquals("GLASS", hudInitArgs[7]);
    }

    private static HudPanel hudPanel(RecordingBrowserScriptExecutor executor) {
        OverlayConfig config = OverlayConfig.builder().build();
        return new HudPanel(executor, new OverlayRootManager(executor, config), config);
    }

    private static final class RecordingBrowserScriptExecutor implements BrowserScriptExecutor {
        private final List<String> scripts = new ArrayList<>();
        private final List<Object[]> args = new ArrayList<>();

        @Override
        public Object execute(String script, Object... args) {
            scripts.add(script);
            this.args.add(Arrays.copyOf(args, args.length));
            return null;
        }
    }
}

