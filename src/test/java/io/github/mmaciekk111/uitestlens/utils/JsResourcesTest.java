package io.github.mmaciekk111.uitestlens.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsResourcesTest {

    @Test
    void readFirstExistingUsesPreferredPathWhenPresent() {
        assertDoesNotThrow(() -> JsResources.readFirstExisting(
                "uitestlens/runtime/api-overlay.js",
                "missing/legacy.js"
        ));
    }

    @Test
    void readFirstExistingReturnsApiOverlayResourceContent() {
        String script = JsResources.readFirstExisting(
                "uitestlens/runtime/api-overlay.js",
                "missing/legacy.js"
        );

        assertTrue(script.contains("__uiTestLens"));
        assertTrue(script.contains("__seleniumApiModal"));
        assertTrue(script.contains("showRequest"));
    }

    @Test
    void readFirstExistingReturnsWaitHudResourceContent() {
        String script = JsResources.readFirstExisting(
                "uitestlens/runtime/wait-hud.js",
                "missing/legacy.js"
        );

        assertTrue(script.contains("__uiTestLens"));
        assertTrue(script.contains("modules.waitHud"));
        assertTrue(script.contains("__seleniumWaitHud"));
    }

    @Test
    void readFirstExistingReturnsHighlightResourceContent() {
        String script = JsResources.readFirstExisting(
                "uitestlens/runtime/highlight.js",
                "missing/legacy.js"
        );

        assertTrue(script.contains("__uiTestLens"));
        assertTrue(script.contains("modules.highlight"));
        assertTrue(script.contains("element: element"));
    }

    @Test
    void readFirstExistingReturnsTypeHintResourceContent() {
        String script = JsResources.readFirstExisting(
                "uitestlens/runtime/type-hint.js",
                "missing/legacy.js"
        );

        assertTrue(script.contains("__uiTestLens"));
        assertTrue(script.contains("modules.typeHint"));
        assertTrue(script.contains("show: show"));
    }

    @Test
    void readFirstExistingReturnsScrollArrowResourceContent() {
        String script = JsResources.readFirstExisting(
                "uitestlens/runtime/scroll-arrow.js",
                "missing/legacy.js"
        );

        assertTrue(script.contains("__uiTestLens"));
        assertTrue(script.contains("modules.scrollArrow"));
        assertTrue(script.contains("scrollToElementWithArrow"));
    }

    @Test
    void readFirstExistingReturnsHudPanelResourceContent() {
        String script = JsResources.readFirstExisting(
                "uitestlens/runtime/hud-panel.js",
                "missing/legacy.js"
        );

        assertTrue(script.contains("__uiTestLens"));
        assertTrue(script.contains("modules.hud"));
        assertTrue(script.contains("setStep"));
    }

    @Test
    void readFirstExistingFallsBackToLegacyPath() {
        assertDoesNotThrow(() -> JsResources.readFirstExisting(
                "missing/preferred.js",
                "uitestlens/runtime/.gitkeep"
        ));
    }

    @Test
    void readFirstExistingFailsWhenBothPathsAreMissing() {
        assertThrows(IllegalArgumentException.class, () -> JsResources.readFirstExisting(
                "missing/preferred.js",
                "missing/legacy.js"
        ));
    }
}
