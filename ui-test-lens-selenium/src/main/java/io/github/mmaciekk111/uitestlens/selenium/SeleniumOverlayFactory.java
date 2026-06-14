package io.github.mmaciekk111.uitestlens.selenium;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.api.ApiOverlayPanel;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.core.browser.SeleniumBrowserScriptExecutor;
import io.github.mmaciekk111.uitestlens.hud.HudPanel;
import org.openqa.selenium.WebDriver;

public final class SeleniumOverlayFactory {
    private SeleniumOverlayFactory() {}

    public static BrowserScriptExecutor scriptExecutor(WebDriver driver) {
        return new SeleniumBrowserScriptExecutor(driver);
    }

    public static OverlayRootManager overlayRoot(WebDriver driver, OverlayConfig config) {
        return new OverlayRootManager(scriptExecutor(driver), config);
    }

    public static HudPanel hudPanel(WebDriver driver, OverlayRootManager rootManager, OverlayConfig config) {
        return new HudPanel(scriptExecutor(driver), rootManager, config);
    }

    public static ApiOverlayPanel apiOverlayPanel(WebDriver driver, OverlayRootManager rootManager, OverlayConfig config) {
        return new ApiOverlayPanel(scriptExecutor(driver), rootManager, config);
    }
}
