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
import java.time.ZoneId;

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
                        Arrays.asList(args).contains("Checkout opened") && args.length == 7 &&
                        String.valueOf(args[2]).matches("\\d{4}-\\d{2}-\\d{2}T.*Z")
        ));
    }

    @Test void legacyMissingAndInvalidTimestampsAreAssignedOnceWhenAccepted() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        HudPanel panel = hudPanel(executor);

        panel.appendLog("info", "missing", null);
        panel.appendLog("warn", "invalid", "ui-test-lens");
        Object[] missing = executor.args.stream().filter(args -> Arrays.asList(args).contains("missing"))
                .findFirst().orElseThrow();
        Object[] invalid = executor.args.stream().filter(args -> Arrays.asList(args).contains("invalid"))
                .findFirst().orElseThrow();
        String missingEventTime = String.valueOf(missing[2]);
        String invalidEventTime = String.valueOf(invalid[2]);

        panel.init("rerender", "pipeline");

        assertEquals(1, executor.args.stream().filter(args -> Arrays.asList(args).contains("missing")).count());
        assertEquals(1, executor.args.stream().filter(args -> Arrays.asList(args).contains("invalid")).count());
        assertEquals(missingEventTime, missing[2]);
        assertEquals(invalidEventTime, invalid[2]);
        assertTrue(missingEventTime.matches("\\d{4}-\\d{2}-\\d{2}T.*Z"));
        assertTrue(invalidEventTime.matches("\\d{4}-\\d{2}-\\d{2}T.*Z"));
        assertTrue(String.valueOf(missing[6]).contains("T"));
        assertTrue(String.valueOf(invalid[6]).contains("T"));
    }

    @Test void formatsTheOriginalEventInstantOnceInJavaWithCustomPatternAndZone() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        HudOptions hud = HudOptions.builder().showTimestamps(true)
                .timestampPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS XXX")
                .timestampZone(ZoneId.of("Europe/Warsaw")).build();
        OverlayConfig config = OverlayConfig.builder().hudOptions(hud).build();
        HudPanel panel = new HudPanel(executor, new OverlayRootManager(executor, config), config);

        panel.appendLog("info", "event", "2026-07-15T12:34:56.123456789Z");
        panel.init("rerender", "pipeline");

        Object[] call = executor.args.stream().filter(args -> Arrays.asList(args).contains("event"))
                .findFirst().orElseThrow();
        assertEquals("2026-07-15T12:34:56.123456789Z", call[2]);
        assertEquals("2026-07-15 14:34:56.123456789 +02:00", call[6]);
        assertEquals(1, executor.args.stream().filter(args -> Arrays.asList(args).contains("event")).count());
    }

    @Test void appendLogPassesSourceMetadataAsSeparateRuntimeArguments() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        HudPanel panel = hudPanel(executor);
        panel.appendLog("info", "click: Login", "now", "LOCATOR_ACTION_PASSED",
                "LoginPage.java:53", "idea://open?file=D%3A%5CLoginPage.java&line=53");
        assertTrue(executor.args.stream().anyMatch(args -> Arrays.asList(args).contains("LoginPage.java:53")
                && Arrays.asList(args).contains("idea://open?file=D%3A%5CLoginPage.java&line=53")));
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
        assertTrue(((Map<?, ?>) hudInitArgs[8]).isEmpty(), "legacy theme must not be shadowed by product defaults");
    }

    @Test
    void explicitHudOptionsAreAuthoritativeAtRuntime() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        OverlayConfig config = OverlayConfig.builder().hudTheme(HudThemePreset.GLASS)
                .hudOptions(HudOptions.builder().background("#010203").build()).build();
        new HudPanel(executor, new OverlayRootManager(executor, config), config).init("Explicit", "local");

        Object[] args = executor.args.stream().filter(value -> Arrays.asList(value).contains("Explicit"))
                .findFirst().orElseThrow();
        assertEquals("#010203", ((Map<?, ?>) args[8]).get("background"));
    }

    @Test
    void customLegacyThemeIsNeverMistakenForProductHudOptions() {
        RecordingBrowserScriptExecutor executor = new RecordingBrowserScriptExecutor();
        HudOptions defaults = HudOptions.defaults();
        HudTheme lookalike = HudTheme.builder()
                .background(defaults.background())
                .foreground(defaults.primaryTextColor())
                .mutedForeground(defaults.mutedTextColor())
                .accent(defaults.accentColor())
                .success(defaults.successColor())
                .warning(defaults.warningColor())
                .danger(defaults.failureColor())
                .maxHeightPx(defaults.maxHeightPx())
                .fontSizePx(18)
                .build();
        OverlayConfig config = OverlayConfig.builder().hudTheme(lookalike).build();

        new HudPanel(executor, new OverlayRootManager(executor, config), config).init("Legacy", "local");

        Object[] args = executor.args.stream().filter(value -> Arrays.asList(value).contains("Legacy"))
                .findFirst().orElseThrow();
        assertTrue(((Map<?, ?>) args[8]).isEmpty());
        assertEquals(18, ((Map<?, ?>) args[6]).get("fontSizePx"));
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

