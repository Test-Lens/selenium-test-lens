package io.github.testlens.core;

import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.utils.JsResources;

public final class AssertionBadgesJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.ASSERTION_BADGES_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_ASSERTION_BADGES_RESOURCE
                    ) +
                    bridgeScript();

    private AssertionBadgesJs() {}

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT);
    }

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var assertionBadges = window.__uiTestLens.modules.assertionBadges;";
    }
}
