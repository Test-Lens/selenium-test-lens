package io.github.testlens.core;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.browser.BrowserScriptExecutor;

public class OverlayRootManager {

    static final String LEGACY_OVERLAY_HOST_ID = "selenium-overlay-host";

    private final BrowserScriptExecutor executor;
    private final OverlayConfig config;

    public OverlayRootManager(BrowserScriptExecutor executor, OverlayConfig config) {
        if (executor == null) {
            throw new IllegalArgumentException("BrowserScriptExecutor must not be null.");
        }
        this.executor = executor;
        this.config = config;
    }

    public void ensureRootExists() {
        if (!config.isEnabled()) {
            return;
        }

        executor.execute(ensureRootScript());
    }

    public void clearAll() {
        executor.execute(clearRootScript());
    }

    static String ensureRootScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var overlayState = window.__uiTestLens.state.overlay;" +
                "if (overlayState.root) {" +
                "  window.__seleniumOverlayRoot = overlayState.root;" +
                "  return;" +
                "}" +
                "if (window.__seleniumOverlayRoot) {" +
                "  overlayState.root = window.__seleniumOverlayRoot;" +
                "  return;" +
                "}" +
                "var host = document.getElementById('" + LEGACY_OVERLAY_HOST_ID + "');" +
                "if (host && host.shadowRoot) {" +
                "  overlayState.root = host.shadowRoot;" +
                "  window.__seleniumOverlayRoot = overlayState.root;" +
                "  return;" +
                "}" +
                "host = document.createElement('div');" +
                "host.id = '" + LEGACY_OVERLAY_HOST_ID + "';" +
                "host.style.position = 'fixed';" +
                "host.style.left = '0';" +
                "host.style.top = '0';" +
                "host.style.width = '0';" +
                "host.style.height = '0';" +
                "host.style.zIndex = '2147483647';" +
                "host.style.pointerEvents = 'none';" +
                "var shadow = host.attachShadow({ mode: 'open' });" +
                "document.body.appendChild(host);" +
                "overlayState.root = shadow;" +
                "window.__seleniumOverlayRoot = overlayState.root;";
    }

    static String clearRootScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var overlayState = window.__uiTestLens.state.overlay;" +
                "var shadow = overlayState.root || window.__seleniumOverlayRoot;" +
                "if (shadow) {" +
                "  overlayState.root = shadow;" +
                "  window.__seleniumOverlayRoot = overlayState.root;" +
                "}" +
                "if (!shadow) { return; }" +
                "while (shadow.firstChild) {" +
                "  shadow.removeChild(shadow.firstChild);" +
                "}";
    }
}
