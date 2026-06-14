package io.github.mmaciekk111.uitestlens.hud;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.HudPanelJs;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.core.browser.SeleniumBrowserScriptExecutor;
import org.openqa.selenium.WebDriver;

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

    public HudPanel(WebDriver driver,
                    OverlayRootManager rootManager,
                    OverlayConfig config) {
        this(new SeleniumBrowserScriptExecutor(driver), rootManager, config);
    }

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
                        "maxWidth: arguments[5]" +
                        "}); }",
                lastTestName,
                lastPipelineId,
                config.getHudPosition().name(),
                config.getHudOffsetX(),
                config.getHudOffsetY(),
                config.getHudMaxWidthPx()
        );
    }
}
