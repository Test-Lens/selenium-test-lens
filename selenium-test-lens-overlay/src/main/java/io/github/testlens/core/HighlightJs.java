package io.github.testlens.core;

import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.utils.JsResources;

public final class HighlightJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.HIGHLIGHT_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_HIGHLIGHT_RESOURCE
                    ) +
                    bridgeScript();

    private HighlightJs() {}

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT);
    }

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var highlight = window.__uiTestLens.modules.highlight;" +
                "if (highlight) { window.__uiTestLens.modules.highlight = highlight; }";
    }
}
