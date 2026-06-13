package io.github.mmaciekk111.uitestlens.api;

import io.github.mmaciekk111.uitestlens.core.UiTestLensRuntimeNames;
import io.github.mmaciekk111.uitestlens.utils.JsResources;

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
}
