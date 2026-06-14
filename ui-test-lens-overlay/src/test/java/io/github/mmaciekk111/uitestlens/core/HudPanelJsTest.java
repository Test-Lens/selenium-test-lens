package io.github.mmaciekk111.uitestlens.core;

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
    void bridgeScriptUsesPrimaryHudModule() {
        String script = HudPanelJs.bridgeScript();

        assertTrue(script.contains("modules.hud"));
    }
}
