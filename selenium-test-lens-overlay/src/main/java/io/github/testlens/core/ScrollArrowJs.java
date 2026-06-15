package io.github.testlens.core;

import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.utils.JsResources;

public final class ScrollArrowJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.SCROLL_ARROW_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_SCROLL_ARROW_RESOURCE
                    ) +
                    bridgeScript();

    private ScrollArrowJs() {}

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT);
    }

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var scrollArrow = window.__uiTestLens.modules.scrollArrow;";
    }
}
