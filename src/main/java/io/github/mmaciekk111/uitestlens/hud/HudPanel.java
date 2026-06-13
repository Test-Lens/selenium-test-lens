package io.github.mmaciekk111.uitestlens.hud;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.HudPanelJs;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Panel HUD in the browser overlay with test, pipeline, step and log information.
 */
public class HudPanel {

    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final OverlayRootManager rootManager;

    private String lastTestName = "-";
    private String lastPipelineId = "-";

    public HudPanel(WebDriver driver,
                    OverlayRootManager rootManager,
                    OverlayConfig config) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.rootManager = rootManager;
        this.config = config;
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

        js.executeScript(
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

        js.executeScript(
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

        js.executeScript(
                HudPanelJs.INIT +
                        "window.__uiTestLens.modules.hud.init({" +
                        "testName: arguments[0]," +
                        "pipelineId: arguments[1]," +
                        "position: arguments[2]," +
                        "offsetX: arguments[3]," +
                        "offsetY: arguments[4]," +
                        "maxWidth: arguments[5]" +
                        "});",
                lastTestName,
                lastPipelineId,
                config.getHudPosition().name(),
                config.getHudOffsetX(),
                config.getHudOffsetY(),
                config.getHudMaxWidthPx()
        );
    }
}
