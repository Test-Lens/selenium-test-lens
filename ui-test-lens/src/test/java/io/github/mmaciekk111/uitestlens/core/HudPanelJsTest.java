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
    }

    @Test
    void bridgeScriptUsesPrimaryHudModule() {
        String script = HudPanelJs.bridgeScript();

        assertTrue(script.contains("modules.hud"));
    }
}
