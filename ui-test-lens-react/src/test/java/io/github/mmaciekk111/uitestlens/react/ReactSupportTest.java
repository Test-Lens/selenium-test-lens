package io.github.mmaciekk111.uitestlens.react;

import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.api.ApiCallActions;
import io.github.mmaciekk111.uitestlens.api.ApiOverlayPanel;
import io.github.mmaciekk111.uitestlens.core.Guards;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.browser.BrowserScriptExecutor;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReactSupportTest {

    @Test
    void createsReactSafeExecutorFromSeleniumFacade() {
        FakeJavascriptWebDriver driver = new FakeJavascriptWebDriver();
        OverlayConfig config = OverlayConfig.builder().build();
        BrowserScriptExecutor executor = (script, args) -> null;
        OverlayRootManager rootManager = new OverlayRootManager(executor, config);
        ApiOverlayPanel apiPanel = new ApiOverlayPanel(executor, rootManager, config);

        JsOverlayDebug overlay = new JsOverlayDebug(
                driver,
                config,
                apiPanel,
                new ApiCallActions(apiPanel),
                new Guards(driver)
        );

        assertNotNull(ReactSupport.overlaySupport(overlay));
        assertNotNull(ReactSupport.reactSafe(overlay));
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
