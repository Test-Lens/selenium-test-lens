package io.github.testlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudPanelJsTest {

    @Test
    void initLoadsHudPanelResource() {
        assertFalse(HudPanelJs.INIT.isBlank());
        assertTrue(HudPanelJs.INIT.contains("__uiTestLens"));
        assertTrue(HudPanelJs.INIT.contains("modules.hud"));
        assertTrue(HudPanelJs.INIT.contains("init: init"));
        assertTrue(HudPanelJs.INIT.contains("setStep: setStep"));
        assertTrue(HudPanelJs.INIT.contains("log: log"));
        assertTrue(HudPanelJs.INIT.contains("clear: clear"));
        assertTrue(HudPanelJs.INIT.contains("remove: remove"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-bg"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-fg"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-accent"));
        assertTrue(HudPanelJs.INIT.contains("--ui-test-lens-hud-max-height"));
        assertTrue(HudPanelJs.INIT.contains("maxHeightPx"));
        assertTrue(HudPanelJs.INIT.contains("updateScrollableRegions"));
    }

    @Test
    void initContainsMinimalBrandingShell() {
        assertTrue(HudPanelJs.INIT.contains("stl-hud-shell"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-header"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-brand-icon"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-side-rail"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-side-rail-text"));
        assertTrue(HudPanelJs.INIT.contains("stl-hud-main"));
        assertTrue(HudPanelJs.INIT.contains("TEST LENS"));
        assertTrue(HudPanelJs.INIT.contains("<svg class=\"stl-hud-brand-icon-svg\""));
        assertFalse(HudPanelJs.INIT.contains("Selenium/WebDriver"));
        assertFalse(HudPanelJs.INIT.contains("Test Lens"));
    }

    @Test
    void bridgeScriptUsesPrimaryHudModule() {
        String script = HudPanelJs.bridgeScript();

        assertTrue(script.contains("modules.hud"));
    }
}

