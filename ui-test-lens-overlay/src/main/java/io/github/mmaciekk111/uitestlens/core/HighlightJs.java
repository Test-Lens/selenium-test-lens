package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.core.browser.OverlayBrowserScriptExecutors;
import io.github.mmaciekk111.uitestlens.utils.JsResources;
import org.openqa.selenium.WebDriver;

public final class HighlightJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.HIGHLIGHT_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_HIGHLIGHT_RESOURCE
                    ) +
                    bridgeScript();

    private HighlightJs() {}

    public static void inject(WebDriver driver) {
        inject(OverlayBrowserScriptExecutors.from(driver));
    }

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT);
    }

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var highlight = window.__uiTestLens.modules.highlight;" +
                "if (highlight) { window.__uiTestLens.modules.highlight = highlight; }";
    }
}
