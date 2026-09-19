package io.github.testlens.hud;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.HudPanelJs;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;

import java.util.Collections;
import java.util.Objects;
import java.time.Instant;

/**
 * Browser HUD panel configured by {@link HudOptions}. HUD rendering is best-effort.
 */
public class HudPanel {

    private final BrowserScriptExecutor scriptExecutor;
    private final OverlayConfig config;
    private final OverlayRootManager rootManager;

    private String lastTestName = "-";
    private String lastPipelineId = "-";

    public HudPanel(BrowserScriptExecutor scriptExecutor,
                    OverlayRootManager rootManager,
                    OverlayConfig config) {
        this.scriptExecutor = Objects.requireNonNull(scriptExecutor, "scriptExecutor must not be null");
        this.rootManager = Objects.requireNonNull(rootManager, "rootManager must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public void init(String testName, String pipelineId) {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        this.lastTestName = (testName == null || testName.isBlank()) ? "-" : testName;
        this.lastPipelineId = (pipelineId == null || pipelineId.isBlank()) ? "-" : pipelineId;

        safely(this::ensureHudPanelExists);
    }

    public void updateStep(String stepDescription) {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        safely(() -> {
        ensureHudPanelExists();
        scriptExecutor.execute(
                HudPanelJs.bridgeScript() +
                        "if (hud) { hud.setStep(arguments[0]); }",
                stepDescription);
        });
    }

    public void appendLog(String level, String message, String timestamp) {
        appendLog(level, message, timestamp, "GENERAL", null, null);
    }

    /** Appends a row with optional logical source label and local-only navigation target. @since 0.3.1 */
    public void appendLog(String level, String message, String timestamp, String eventType,
                          String sourceLabel, String navigationTarget) {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        safely(() -> {
        ensureHudPanelExists();
        Instant eventTimestamp = eventTimestamp(timestamp);
        HudOptions hudOptions = config.getHudOptions();
        scriptExecutor.execute(
                HudPanelJs.bridgeScript() +
                        "if (hud) { hud.log(arguments[1], arguments[0], arguments[2], arguments[3], arguments[4], arguments[5], arguments[6]); }",
                level, message, eventTimestamp.toString(), eventType == null ? "GENERAL" : eventType,
                sourceLabel, navigationTarget, hudOptions.formatHudTimestamp(eventTimestamp));
        });
    }

    private static Instant eventTimestamp(String value) {
        if (value != null) {
            try { return Instant.parse(value.trim()); }
            catch (RuntimeException ignored) { /* assign once below */ }
        }
        return Instant.now();
    }

    private void ensureHudPanelExists() {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        rootManager.ensureRootExists();

        HudPanelJs.inject(scriptExecutor);

        scriptExecutor.execute(
                HudPanelJs.bridgeScript() +
                        "if (hud) { hud.init({" +
                        "testName: arguments[0]," +
                        "pipelineId: arguments[1]," +
                        "position: arguments[2]," +
                        "offsetX: arguments[3]," +
                        "offsetY: arguments[4]," +
                        "maxWidth: arguments[5]," +
                        "theme: arguments[6]," +
                        "themeName: arguments[7]," +
                        "hudOptions: arguments[8]" +
                        "}); }",
                lastTestName,
                lastPipelineId,
                config.getHudPosition().name(),
                config.getHudOffsetX(),
                config.getHudOffsetY(),
                config.getHudMaxWidthPx(),
                config.getHudTheme().toMap(),
                config.getHudThemePreset() == null ? "CUSTOM" : config.getHudThemePreset().name(),
                config.isHudOptionsAuthoritative()
                        ? config.getHudOptions().toBrowserRuntimeMap()
                        : Collections.emptyMap()
        );
    }

    private static void safely(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException ignored) {
            // HUD is best-effort observability and must never alter the test result.
        }
    }
}

