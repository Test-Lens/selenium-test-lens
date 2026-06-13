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
