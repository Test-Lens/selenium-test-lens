package io.github.mmaciekk111.uitestlens.core;

public final class UiTestLensRuntimeNames {
    public static final String GLOBAL_NAMESPACE = "__uiTestLens";

    public static final String LEGACY_OVERLAY_ROOT_GLOBAL = "__seleniumOverlayRoot";
    public static final String LEGACY_WAIT_ELAPSED_GLOBAL = "__seleniumLastWaitElapsedMs";
    public static final String LEGACY_WAIT_MESSAGE_GLOBAL = "__seleniumLastWaitMessage";
    public static final String LEGACY_WAIT_HUD_GLOBAL = "__seleniumWaitHud";
    public static final String LEGACY_API_MODAL_GLOBAL = "__seleniumApiModal";
    public static final String LEGACY_ACTIVE_REQUESTS_GLOBAL = "__seleniumActiveRequests";
    public static final String LEGACY_NETWORK_TRACKER_INSTALLED_GLOBAL = "__seleniumNetworkTrackerInstalled";

    public static final String RUNTIME_RESOURCE_ROOT = "uitestlens/runtime/";
    public static final String LEGACY_SELENIUM_RESOURCE_ROOT = "selenium/";

    public static final String API_OVERLAY_RESOURCE = RUNTIME_RESOURCE_ROOT + "api-overlay.js";
    public static final String LEGACY_API_OVERLAY_RESOURCE = LEGACY_SELENIUM_RESOURCE_ROOT + "api-overlay.js";
    public static final String WAIT_HUD_RESOURCE = RUNTIME_RESOURCE_ROOT + "wait/WaitHud.js";
    public static final String LEGACY_WAIT_HUD_RESOURCE = LEGACY_SELENIUM_RESOURCE_ROOT + "wait/WaitHud.js";

    private UiTestLensRuntimeNames() {}

    public static String ensureNamespaceScript() {
        return "window.__uiTestLens = window.__uiTestLens || { version: '1.0-SNAPSHOT', modules: {}, state: {} };" +
                "window.__uiTestLens.modules = window.__uiTestLens.modules || {};" +
                "window.__uiTestLens.state = window.__uiTestLens.state || {};" +
                "window.__uiTestLens.state.wait = window.__uiTestLens.state.wait || {};" +
                "window.__uiTestLens.state.network = window.__uiTestLens.state.network || {};" +
                "window.__uiTestLens.state.overlay = window.__uiTestLens.state.overlay || {};" +
                "window.__uiTestLens.state.api = window.__uiTestLens.state.api || {};";
    }
}
