package io.github.testlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayRootManagerTest {

    @Test
    void ensureRootScriptUsesUiTestLensStateAsPrimaryRoot() {
        String script = OverlayRootManager.ensureRootScript();

        assertTrue(script.contains("window.__uiTestLens"));
        assertTrue(script.contains("state.overlay"));
        assertTrue(script.contains("overlayState.root"));
        assertTrue(script.contains("window.__seleniumOverlayRoot = overlayState.root"));
    }

    @Test
    void ensureRootScriptReadsPrimaryStateBeforeLegacyAlias() {
        String script = OverlayRootManager.ensureRootScript();

        int primaryIndex = script.indexOf("if (overlayState.root)");
        int legacyIndex = script.indexOf("if (window.__seleniumOverlayRoot)");

        assertTrue(primaryIndex >= 0);
        assertTrue(legacyIndex > primaryIndex);
    }

    @Test
    void ensureRootScriptReusesExistingHostBeforeCreatingNewOne() {
        String script = OverlayRootManager.ensureRootScript();

        assertTrue(script.contains("document.getElementById('" + OverlayRootManager.LEGACY_OVERLAY_HOST_ID + "')"));
        assertTrue(script.contains("host && host.shadowRoot"));
        assertTrue(script.contains("document.createElement('div')"));
    }

    @Test
    void clearRootScriptKeepsLegacyAliasSynchronized() {
        String script = OverlayRootManager.clearRootScript();

        assertTrue(script.contains("var overlayState = window.__uiTestLens.state.overlay"));
        assertTrue(script.contains("var shadow = overlayState.root || window.__seleniumOverlayRoot"));
        assertTrue(script.contains("window.__seleniumOverlayRoot = overlayState.root"));
    }
}
