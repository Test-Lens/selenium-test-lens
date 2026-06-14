package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.utils.JsResources;

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
