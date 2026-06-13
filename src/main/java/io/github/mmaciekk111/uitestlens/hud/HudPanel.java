package io.github.mmaciekk111.uitestlens.hud;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * Mały panel w rogu ekranu z informacją o teście/pipeline/aktualnym kroku.
 * Położenie i rozmiar sterowane przez OverlayConfig (HudPosition + offset + maxWidth).
 */
public class HudPanel {

    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final OverlayRootManager rootManager;

    // zapamiętujemy ostatnie dane, żeby po przeładowaniu strony móc odtworzyć panel
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

        // cała logika budowy panelu jest w helperze
        ensureHudPanelExists();
    }

    public void updateStep(String stepDescription) {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        ensureHudPanelExists();

        js.executeScript(
                "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) { return; }" +
                        "var step = shadow.querySelector('#selenium-hud-step');" +
                        "if (!step) { return; }" +
                        "step.innerHTML = '<b>Step:</b> ' + (arguments[0] || '-');",
                stepDescription
        );
    }

    /**
     * Dodaje linię logu do HUD-a (sekcja #selenium-hud-logs).
     */
    public void appendLog(String level, String message, String timestamp) {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        ensureHudPanelExists();

        js.executeScript(
                "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) { return; }" +
                        "var panel = shadow.querySelector('#selenium-hud-panel');" +
                        "if (!panel) { return; }" +
                        "var logs = panel.querySelector('#selenium-hud-logs');" +
                        "if (!logs) {" +
                        "  logs = document.createElement('div');" +
                        "  logs.id = 'selenium-hud-logs';" +
                        "  logs.style.marginTop = '6px';" +
                        "  logs.style.maxHeight = '160px';" +
                        "  logs.style.overflowY = 'auto';" +
                        "  logs.style.borderTop = '1px solid rgba(255,255,255,0.2)';" +
                        "  logs.style.paddingTop = '4px';" +
                        "  panel.appendChild(logs);" +
                        "}" +

                        "var row = document.createElement('div');" +
                        "row.style.fontFamily = 'monospace';" +
                        "row.style.fontSize = '10px';" +
                        "row.style.marginBottom = '2px';" +
                        "row.style.whiteSpace = 'pre-wrap';" +
                        "row.style.wordBreak = 'break-word';" +

                        "var lvl = (arguments[0] || '').toLowerCase();" +

                        // === MAPOWANIE KOLORÓW ===
                        // info      -> biały
                        // warn      -> żółty
                        // error     -> czerwony
                        // success   -> zielony
                        "var color = '#ffffff';" + // domyślnie biały (info)
                        "if (lvl === 'warn')  color = '#ffd93b';" +
                        "if (lvl === 'error' || lvl === 'failed') color = '#ff4c4c';" +
                        "if (lvl === 'success') color = '#00ff7f';" +
                        "if (lvl === 'royal')   color = '#4ca3ff';" +
                        "row.style.color = color;" +

                        "var text = '[' + (arguments[2] || '') + '][' + (arguments[0] || '').toUpperCase() + '] ' + (arguments[1] || '');" +
                        "row.textContent = text;" +

                        "logs.appendChild(row);" +
                        "logs.scrollTop = logs.scrollHeight;",
                level, message, timestamp
        );
    }



    /**
     * Odbudowuje (lub aktualizuje) HUD w aktualnym DOM-ie, używając lastTestName/lastPipelineId.
     * Wywołuj wszędzie tam, gdzie chcesz mieć pewność, że panel istnieje
     * (init, updateStep, appendLog itd.).
     */
    private void ensureHudPanelExists() {
        if (!config.isEnabled() || !config.isShowHudPanel()) {
            return;
        }

        // zapewniamy shadow root w aktualnym dokumencie (po każdej nawigacji!)
        rootManager.ensureRootExists();

        HudPosition pos = config.getHudPosition();
        int offsetX = config.getHudOffsetX();
        int offsetY = config.getHudOffsetY();
        int maxWidth = config.getHudMaxWidthPx();

        String positionJs =
                "panel.style.top = 'auto';" +
                        "panel.style.right = 'auto';" +
                        "panel.style.bottom = 'auto';" +
                        "panel.style.left = 'auto';";

        switch (pos) {
            case TOP_LEFT:
                positionJs +=
                        "panel.style.top = '" + offsetY + "px';" +
                                "panel.style.left = '" + offsetX + "px';";
                break;
            case TOP_RIGHT:
                positionJs +=
                        "panel.style.top = '" + offsetY + "px';" +
                                "panel.style.right = '" + offsetX + "px';";
                break;
            case BOTTOM_LEFT:
                positionJs +=
                        "panel.style.bottom = '" + offsetY + "px';" +
                                "panel.style.left = '" + offsetX + "px';";
                break;
            case BOTTOM_RIGHT:
            default:
                positionJs +=
                        "panel.style.bottom = '" + offsetY + "px';" +
                                "panel.style.right = '" + offsetX + "px';";
                break;
        }

        js.executeScript(
                "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) { return; }" +
                        "var panel = shadow.querySelector('#selenium-hud-panel');" +
                        "if (!panel) {" +
                        "  panel = document.createElement('div');" +
                        "  panel.id = 'selenium-hud-panel';" +
                        "  panel.style.position = 'fixed';" +
                        "  " + positionJs +
                        "  panel.style.background = 'rgba(0, 0, 0, 0.75)';" +
                        "  panel.style.color = '#ffffff';" +
                        "  panel.style.fontSize = '11px';" +
                        "  panel.style.padding = '8px 10px';" +
                        "  panel.style.borderRadius = '4px';" +
                        "  panel.style.maxWidth = '" + maxWidth + "px';" +
                        "  panel.style.boxShadow = '0 2px 6px rgba(0,0,0,0.4)';" +
                        "  panel.style.pointerEvents = 'auto';" +
                        "  panel.style.lineHeight = '1.4';" +
                        "  shadow.appendChild(panel);" +
                        "} else {" +
                        "  " + positionJs +
                        "  panel.style.maxWidth = '" + maxWidth + "px';" +
                        "}" +

                        // TEST
                        "var title = panel.querySelector('#selenium-hud-test');" +
                        "if (!title) {" +
                        "  title = document.createElement('div');" +
                        "  title.id = 'selenium-hud-test';" +
                        "  panel.appendChild(title);" +
                        "}" +
                        "title.innerHTML = '<b>Test:</b> ' + (arguments[0] || '-');" +

                        // PIPELINE
                        "var pipeline = panel.querySelector('#selenium-hud-pipeline');" +
                        "if (!pipeline) {" +
                        "  pipeline = document.createElement('div');" +
                        "  pipeline.id = 'selenium-hud-pipeline';" +
                        "  panel.appendChild(pipeline);" +
                        "}" +
                        "pipeline.innerHTML = '<b>Pipeline:</b> ' + (arguments[1] || '-');" +

                        // STEP
                        "var step = panel.querySelector('#selenium-hud-step');" +
                        "if (!step) {" +
                        "  step = document.createElement('div');" +
                        "  step.id = 'selenium-hud-step';" +
                        "  step.style.marginTop = '4px';" +
                        "  panel.appendChild(step);" +
                        "}" +
                        "if (!step.innerHTML) {" +
                        "  step.innerHTML = '<b>Step:</b> -';" +
                        "}" +

                        // LOGS – kontener na logi, jeżeli go nie ma
                        "var logs = panel.querySelector('#selenium-hud-logs');" +
                        "if (!logs) {" +
                        "  logs = document.createElement('div');" +
                        "  logs.id = 'selenium-hud-logs';" +
                        "  logs.style.marginTop = '6px';" +
                        "  logs.style.maxHeight = '160px';" +
                        "  logs.style.overflowY = 'auto';" +
                        "  logs.style.borderTop = '1px solid rgba(255,255,255,0.2)';" +
                        "  logs.style.paddingTop = '4px';" +
                        "  panel.appendChild(logs);" +
                        "}",
                lastTestName, lastPipelineId
        );
    }
}
