package io.github.mmaciekk111.uitestlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockingOverlayHelperTest {

    @Test
    void globalOverlayCloseButtonScriptKeepsConfiguredSelectorAndVisibilityChecks() {
        String script = BlockingOverlayHelper.globalOverlayCloseButtonScript();

        assertTrue(script.contains("document.querySelector(sel)"));
        assertTrue(script.contains("getBoundingClientRect"));
        assertTrue(script.contains("style.display === 'none'"));
        assertTrue(script.contains("parseFloat(style.opacity || '1') < 0.05"));
    }

    @Test
    void blockingOverlayForTargetScriptKeepsElementFromPointHeuristic() {
        String script = BlockingOverlayHelper.blockingOverlayForTargetScript();

        assertTrue(script.contains("var el = arguments[0]"));
        assertTrue(script.contains("document.elementFromPoint(cx, cy)"));
        assertTrue(script.contains("isOverlayCandidate"));
        assertTrue(script.contains("pos === 'fixed'"));
        assertTrue(script.contains("pos === 'absolute'"));
        assertTrue(script.contains("pos === 'sticky'"));
        assertTrue(script.contains("rect.width < 50"));
        assertTrue(script.contains("rect.height < 40"));
        assertTrue(script.contains("z < 10"));
    }

    @Test
    void closeButtonInsideScriptKeepsButtonSelectorsAndTextKeywords() {
        String script = BlockingOverlayHelper.closeButtonInsideScript();

        assertTrue(script.contains("[role=\"button\"]"));
        assertTrue(script.contains("button[id*=\"close\" i]"));
        assertTrue(script.contains("[aria-label*=\"zamknij\" i]"));
        assertTrue(script.contains("text.indexOf('akceptuj')"));
        assertTrue(script.contains("text.indexOf('accept')"));
        assertTrue(script.contains("text.indexOf('zamknij')"));
        assertTrue(script.contains("text.indexOf('close')"));
    }

    @Test
    void blockingOverlayScriptsDoNotIntroduceRuntimeGlobals() {
        String combined = BlockingOverlayHelper.globalOverlayCloseButtonScript()
                + BlockingOverlayHelper.blockingOverlayForTargetScript()
                + BlockingOverlayHelper.closeButtonInsideScript();

        assertFalse(combined.contains("__selenium"));
        assertFalse(combined.contains("__uiTestLens"));
    }
}
