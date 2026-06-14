package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.core.browser.OverlayBrowserScriptExecutors;
import io.github.mmaciekk111.uitestlens.utils.JsResources;
import org.openqa.selenium.WebDriver;

public final class AssertionBadgesJs {

    public static final String INIT =
            UiTestLensRuntimeNames.ensureNamespaceScript() +
                    JsResources.readFirstExisting(
                            UiTestLensRuntimeNames.ASSERTION_BADGES_RESOURCE,
                            UiTestLensRuntimeNames.LEGACY_ASSERTION_BADGES_RESOURCE
                    ) +
                    bridgeScript();

    private AssertionBadgesJs() {}

    public static void inject(WebDriver driver) {
        inject(OverlayBrowserScriptExecutors.from(driver));
    }

    public static void inject(BrowserScriptExecutor executor) {
        executor.execute(INIT);
    }

    public static String bridgeScript() {
        return UiTestLensRuntimeNames.ensureNamespaceScript() +
                "var assertionBadges = window.__uiTestLens.modules.assertionBadges;";
    }
}
