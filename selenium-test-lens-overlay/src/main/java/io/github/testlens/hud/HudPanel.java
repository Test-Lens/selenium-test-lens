package io.github.testlens.hud;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.HudPanelJs;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;

import java.util.Objects;

/**
 * Panel HUD in the browser overlay with test, pipeline, step and log information.
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

        ensureHudPanelExists();
    }

    public void updateStep(String stepDescription) {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        ensureHudPanelExists();

        scriptExecutor.execute(
                HudPanelJs.bridgeScript() +
                        "if (hud) { hud.setStep(arguments[0]); }",
                stepDescription
        );
    }

    public void appendLog(String level, String message, String timestamp) {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        ensureHudPanelExists();

        scriptExecutor.execute(
                HudPanelJs.bridgeScript() +
                        "if (hud) { hud.log(arguments[1], arguments[0], arguments[2]); }",
                level, message, timestamp
        );
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
                        "themeName: arguments[7]" +
                        "}); }",
                lastTestName,
                lastPipelineId,
                config.getHudPosition().name(),
                config.getHudOffsetX(),
                config.getHudOffsetY(),
                config.getHudMaxWidthPx(),
                config.getHudTheme().toMap(),
                config.getHudThemePreset() == null ? "CUSTOM" : config.getHudThemePreset().name()
        );
    }
}
