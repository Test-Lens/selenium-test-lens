package io.github.mmaciekk111.uitestlens.api;

import io.github.mmaciekk111.uitestlens.core.UiTestLensRuntimeNames;
import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.core.browser.SeleniumBrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.utils.JsResources;
import org.openqa.selenium.WebDriver;

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

    private ApiOverlayJs() {}

    public static void inject(WebDriver driver) {
        inject(new SeleniumBrowserScriptExecutor(driver));
    }

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT_MODAL);
    }
}
