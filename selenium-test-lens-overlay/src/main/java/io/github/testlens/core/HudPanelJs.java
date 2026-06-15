package io.github.testlens.core;

import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.utils.JsResources;

public final class HudPanelJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.HUD_PANEL_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_HUD_PANEL_RESOURCE
                    ) +
                    bridgeScript();

    private HudPanelJs() {}

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT);
    }

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var hud = window.__uiTestLens.modules.hud;";
    }
}

