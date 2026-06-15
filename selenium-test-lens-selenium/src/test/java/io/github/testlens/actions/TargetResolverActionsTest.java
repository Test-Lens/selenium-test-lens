package io.github.testlens.actions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetResolverActionsTest {

    @Test
    void clickTargetResolverScriptKeepsExistingClickableFallbacks() {
        String script = TargetResolverActions.clickTargetResolverScript();

        assertTrue(script.contains("function isClickable"));
        assertTrue(script.contains("querySelectorAll"));
        assertTrue(script.contains("button, input, a[href]"));
        assertTrue(script.contains("[role=\"button\"]"));
        assertTrue(script.contains("[data-test-click-target]"));
        assertTrue(script.contains("document.getElementById"));
        assertTrue(script.contains("parentElement"));
        assertTrue(script.contains("return el"));
    }

    @Test
    void fileInputResolverScriptKeepsExistingFileInputFallbacks() {
        String script = TargetResolverActions.fileInputResolverScript();

        assertTrue(script.contains("function isFileInput"));
        assertTrue(script.contains("querySelectorAll"));
        assertTrue(script.contains("input[type=\"file\"]"));
        assertTrue(script.contains("document.getElementById"));
        assertTrue(script.contains("parentElement"));
        assertTrue(script.contains("return null"));
    }

    @Test
    void resolverScriptsDoNotIntroduceRuntimeGlobals() {
        String clickScript = TargetResolverActions.clickTargetResolverScript();
        String fileScript = TargetResolverActions.fileInputResolverScript();

        assertFalse(clickScript.contains("__selenium"));
        assertFalse(clickScript.contains("__uiTestLens"));
        assertFalse(fileScript.contains("__selenium"));
        assertFalse(fileScript.contains("__uiTestLens"));
    }
}

