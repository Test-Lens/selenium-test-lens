package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.utils.JsResources;

public final class WaitHudJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.WAIT_HUD_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_WAIT_HUD_RESOURCE
                    ) +
                    bridgeScript();

    private WaitHudJs() {}

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT);
    }

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var waitHud = window.__uiTestLens.modules.waitHud || window.__seleniumWaitHud;" +
                "if (waitHud) { window.__uiTestLens.modules.waitHud = waitHud; window.__seleniumWaitHud = waitHud; }";
    }
}
