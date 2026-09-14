package io.github.testlens.core;

import io.github.testlens.utils.JsResources;
import io.github.testlens.core.browser.BrowserScriptExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

final class VisualTypographyJs {
    static final String RUNTIME_RESOURCE = "uitestlens/runtime/visual-typography.js";
    static final String FONT_RESOURCE = "uitestlens/runtime/fonts/Sora-wght.woff2";
    static final String LICENSE_RESOURCE = "META-INF/licenses/OFL-Sora.txt";
    static final String FONT_FAMILY = "Test Lens Sora";
    static final String UI_FONT_STACK = "\"Test Lens Sora\", system-ui, -apple-system, BlinkMacSystemFont, \"Segoe UI\", sans-serif";

    static final String INIT = UiTestLensRuntimeNames.ensureNamespaceScript()
            + JsResources.load(RUNTIME_RESOURCE)
            + "window.__uiTestLens.modules.visualTypography.installBase64('"
            + Base64.getEncoder().encodeToString(readFontBytes())
            + "');";

    private static final String INSTALLED_PROBE = UiTestLensRuntimeNames.ensureNamespaceScript()
            + "return !!(window.__uiTestLens.modules.visualTypography"
            + " && window.__uiTestLens.state.typography"
            + " && window.__uiTestLens.state.typography.loadPromise);";

    private VisualTypographyJs() {}

    static void ensureInstalled(BrowserScriptExecutor executor) {
        Object installed = executor.execute(INSTALLED_PROBE);
        if (!Boolean.TRUE.equals(installed)) executor.execute(INIT);
    }

    private static byte[] readFontBytes() {
        ClassLoader loader = VisualTypographyJs.class.getClassLoader();
        try (InputStream input = loader.getResourceAsStream(FONT_RESOURCE)) {
            if (input == null) throw new IllegalArgumentException("Font resource not found: " + FONT_RESOURCE);
            return input.readAllBytes();
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to load font resource: " + FONT_RESOURCE, failure);
        }
    }
}
