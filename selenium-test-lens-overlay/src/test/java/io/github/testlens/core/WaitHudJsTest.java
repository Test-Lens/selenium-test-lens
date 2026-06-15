package io.github.testlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaitHudJsTest {

    @Test
    void initLoadsWaitHudResource() {
        assertFalse(WaitHudJs.INIT.isBlank());
        assertTrue(WaitHudJs.INIT.contains("__uiTestLens"));
        assertTrue(WaitHudJs.INIT.contains("modules.waitHud"));
        assertTrue(WaitHudJs.INIT.contains("__seleniumWaitHud"));
        assertTrue(WaitHudJs.INIT.contains("__seleniumLastWaitElapsedMs"));
    }

    @Test
    void bridgeScriptKeepsPrimaryAndLegacyNamesConnected() {
        String script = WaitHudJs.bridgeScript();

        assertTrue(script.contains("modules.waitHud"));
        assertTrue(script.contains("__seleniumWaitHud"));
    }
}
