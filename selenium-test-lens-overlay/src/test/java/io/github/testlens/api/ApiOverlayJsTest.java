package io.github.testlens.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiOverlayJsTest {

    @Test
    void initModalLoadsApiOverlayResource() {
        assertFalse(ApiOverlayJs.INIT_MODAL.isBlank());
        assertTrue(ApiOverlayJs.INIT_MODAL.contains("__uiTestLens"));
        assertTrue(ApiOverlayJs.INIT_MODAL.contains("__seleniumApiModal"));
        assertTrue(ApiOverlayJs.INIT_MODAL.contains("showRequest"));
    }

    @Test
    void initModalRegistersPrimaryAndLegacyApiOverlayNames() {
        assertTrue(ApiOverlayJs.INIT_MODAL.contains("modules.apiOverlay"));
        assertTrue(ApiOverlayJs.INIT_MODAL.contains("modules.apiModal"));
    }
}
