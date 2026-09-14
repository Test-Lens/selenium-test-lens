package io.github.testlens.core;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void repeatedRootEnsureTransfersFontPayloadOnlyOncePerDocument() {
        List<String> scripts = new ArrayList<>();
        BrowserScriptExecutor executor = (script, args) -> {
            scripts.add(script);
            if (script.contains("return !!(window.__uiTestLens.modules.visualTypography")) {
                return scripts.stream().anyMatch(value -> value.contains("installBase64"));
            }
            return null;
        };
        OverlayRootManager manager = new OverlayRootManager(executor, OverlayConfig.builder().build());

        manager.ensureRootExists();
        manager.ensureRootExists();

        assertEquals(1, scripts.stream().filter(value -> value.contains("installBase64")).count());
        assertEquals(2, scripts.stream().filter(value -> value.contains("return !!(window.__uiTestLens.modules.visualTypography")).count());
    }
}

