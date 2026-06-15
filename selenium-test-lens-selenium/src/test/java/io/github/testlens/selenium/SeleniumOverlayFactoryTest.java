package io.github.testlens.selenium;

import io.github.testlens.OverlayConfig;
import io.github.testlens.api.ApiOverlayPanel;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.hud.HudPanel;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SeleniumOverlayFactoryTest {

    @Test
    void createsOverlayObjectsFromWebDriver() {
        FakeJavascriptWebDriver driver = new FakeJavascriptWebDriver();
        OverlayConfig config = OverlayConfig.builder().build();

        BrowserScriptExecutor executor = SeleniumOverlayFactory.scriptExecutor(driver);
        OverlayRootManager rootManager = SeleniumOverlayFactory.overlayRoot(driver, config);
        HudPanel hudPanel = SeleniumOverlayFactory.hudPanel(driver, rootManager, config);
        ApiOverlayPanel apiOverlayPanel = SeleniumOverlayFactory.apiOverlayPanel(driver, rootManager, config);

        assertNotNull(executor);
        assertNotNull(rootManager);
        assertNotNull(hudPanel);
        assertNotNull(apiOverlayPanel);
    }

    private static final class FakeJavascriptWebDriver implements WebDriver, JavascriptExecutor {
        @Override
        public Object executeScript(String script, Object... args) {
            return null;
        }

        @Override
        public Object executeAsyncScript(String script, Object... args) {
            return null;
        }

        @Override
        public void get(String url) {}

        @Override
        public String getCurrentUrl() {
            return "";
        }

        @Override
        public String getTitle() {
            return "";
        }

        @Override
        public java.util.List<org.openqa.selenium.WebElement> findElements(org.openqa.selenium.By by) {
            return java.util.List.of();
        }

        @Override
        public org.openqa.selenium.WebElement findElement(org.openqa.selenium.By by) {
            throw new org.openqa.selenium.NoSuchElementException("not implemented");
        }

        @Override
        public String getPageSource() {
            return "";
        }

        @Override
        public void close() {}

        @Override
        public void quit() {}

        @Override
        public Set<String> getWindowHandles() {
            return Set.of();
        }

        @Override
        public String getWindowHandle() {
            return "";
        }

        @Override
        public TargetLocator switchTo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Navigation navigate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Options manage() {
            throw new UnsupportedOperationException();
        }
    }
}
