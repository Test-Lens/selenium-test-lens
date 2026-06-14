package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.utils.JsResources;

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
