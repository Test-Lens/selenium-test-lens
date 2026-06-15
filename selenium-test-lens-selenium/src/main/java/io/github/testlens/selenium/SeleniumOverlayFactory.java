package io.github.testlens.selenium;

import io.github.testlens.OverlayConfig;
import io.github.testlens.api.ApiOverlayPanel;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.browser.SeleniumBrowserScriptExecutor;
import io.github.testlens.hud.HudPanel;
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
