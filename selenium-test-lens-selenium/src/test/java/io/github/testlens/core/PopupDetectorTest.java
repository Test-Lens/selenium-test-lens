package io.github.testlens.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopupDetectorTest {

    @Test
    void detectPopupScriptKeepsKnownPopupSelectorsAndScoring() {
        String script = PopupDetector.detectPopupScript();

        assertTrue(script.contains("[role=\"dialog\"]"));
        assertTrue(script.contains("[role=\"alertdialog\"]"));
        assertTrue(script.contains("[aria-modal=\"true\"]"));
        assertTrue(script.contains(".MuiDialog-root"));
        assertTrue(script.contains(".ant-modal"));
        assertTrue(script.contains(".cdk-overlay-pane"));
        assertTrue(script.contains("querySelectorAll"));
        assertTrue(script.contains("window.innerWidth * 0.3"));
        assertTrue(script.contains("window.innerHeight * 0.2"));
        assertTrue(script.contains("score = area + z * 1000"));
    }

    @Test
    void globalCloseButtonScriptKeepsVisibilityChecks() {
        String script = PopupDetector.globalCloseButtonScript();

        assertTrue(script.contains("document.querySelector(sel)"));
        assertTrue(script.contains("getBoundingClientRect"));
        assertTrue(script.contains("style.display === 'none'"));
        assertTrue(script.contains("parseFloat(style.opacity || '1') < 0.05"));
    }

    @Test
    void overlayAtViewportCenterScriptKeepsCenterPointHeuristic() {
        String script = PopupDetector.overlayAtViewportCenterScript();

        assertTrue(script.contains("document.elementFromPoint(cx, cy)"));
        assertTrue(script.contains("isOverlayCandidate"));
        assertTrue(script.contains("pos === 'fixed'"));
        assertTrue(script.contains("pos === 'absolute'"));
        assertTrue(script.contains("pos === 'sticky'"));
        assertTrue(script.contains("z < 10"));
        assertTrue(script.contains("parentElement"));
    }

    @Test
    void closeButtonInsideScriptKeepsButtonSelectorsAndTextKeywords() {
        String script = PopupDetector.closeButtonInsideScript();

        assertTrue(script.contains("[role=\"button\"]"));
        assertTrue(script.contains("button[id*=\"close\" i]"));
        assertTrue(script.contains("[aria-label*=\"zamknij\" i]"));
        assertTrue(script.contains("text.indexOf('akceptuj')"));
        assertTrue(script.contains("text.indexOf('accept')"));
        assertTrue(script.contains("text.indexOf('zamknij')"));
        assertTrue(script.contains("text.indexOf('close')"));
    }

    @Test
    void popupScriptsDoNotIntroduceRuntimeGlobals() {
        String combined = PopupDetector.detectPopupScript()
                + PopupDetector.globalCloseButtonScript()
                + PopupDetector.overlayAtViewportCenterScript()
                + PopupDetector.closeButtonInsideScript();

        assertFalse(combined.contains("__selenium"));
        assertFalse(combined.contains("__uiTestLens"));
    }
}

