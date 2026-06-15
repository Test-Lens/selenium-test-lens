package io.github.testlens.api;

import io.github.testlens.core.UiTestLensRuntimeNames;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.utils.JsResources;

/**
 * Loads the API overlay runtime script into a browser page.
 */
public final class ApiOverlayJs {

    public static final String INIT_MODAL =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.API_OVERLAY_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_API_OVERLAY_RESOURCE
                    ) +
                    "if (window.__uiTestLens.modules.apiOverlay) { window.__uiTestLens.modules.apiModal = window.__uiTestLens.modules.apiOverlay; }" +
                    "if (window.__seleniumApiModal) { window.__uiTestLens.modules.apiOverlay = window.__seleniumApiModal; window.__uiTestLens.modules.apiModal = window.__seleniumApiModal; }" +
                    "if (window.__uiTestLens.modules.apiOverlay) { window.__seleniumApiModal = window.__uiTestLens.modules.apiOverlay; }";

    private ApiOverlayJs() {}

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT_MODAL);
    }
}

