package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class OverlayRootManager {

    private final JavascriptExecutor js;
    private final OverlayConfig config;

    public OverlayRootManager(WebDriver driver, OverlayConfig config) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.config = config;
    }

    /**
     * Upewnia się, że root overlaya istnieje (window.__seleniumOverlayRoot).
     * Jeśli nie istnieje, tworzy go.
     */
    public void ensureRootExists() {
        if (!config.isEnabled()) {
            return;
        }

        js.executeScript(
                UiTestLensRuntimeNames.ensureNamespaceScript() +
                        "if (window.__uiTestLens.state.overlay.root) {" +
                        "  window.__seleniumOverlayRoot = window.__uiTestLens.state.overlay.root;" +
                        "  return;" +
                        "}" +
                        "if (window.__seleniumOverlayRoot) {" +
                        "  window.__uiTestLens.state.overlay.root = window.__seleniumOverlayRoot;" +
                        "  return;" +
                        "}" +
                        "var host = document.createElement('div');" +
                        "host.id = 'selenium-overlay-host';" +
                        "host.style.position = 'fixed';" +
                        "host.style.left = '0';" +
                        "host.style.top = '0';" +
                        "host.style.width = '0';" +
                        "host.style.height = '0';" +
                        "host.style.zIndex = '2147483647';" +
                        "host.style.pointerEvents = 'none';" +
                        "var shadow = host.attachShadow({ mode: 'open' });" +
                        "document.body.appendChild(host);" +
                        "window.__uiTestLens.state.overlay.root = shadow;" +
                        "window.__seleniumOverlayRoot = shadow;"
        );
    }

    public void clearAll() {
        js.executeScript(
                UiTestLensRuntimeNames.ensureNamespaceScript() +
                        "var shadow = window.__uiTestLens.state.overlay.root || window.__seleniumOverlayRoot;" +
                        "if (shadow) { window.__uiTestLens.state.overlay.root = shadow; window.__seleniumOverlayRoot = shadow; }" +
                        "if (!shadow) { return; }" +
                        "while (shadow.firstChild) {" +
                        "  shadow.removeChild(shadow.firstChild);" +
                        "}"
        );
    }
}

