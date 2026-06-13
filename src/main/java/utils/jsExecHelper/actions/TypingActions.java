package utils.jsExecHelper.actions;

import utils.jsExecHelper.OverlayConfig;
import utils.jsExecHelper.core.OverlayRootManager;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class TypingActions {

    private final JavascriptExecutor js;
    private final OverlayRootManager rootManager;
    private final OverlayConfig config;

    public TypingActions(WebDriver driver,
                         OverlayRootManager rootManager,
                         OverlayConfig config) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.rootManager = rootManager;
        this.config = config;
    }



    /**
     * Wpisuje tekst do elementu (clear + sendKeys) oraz pokazuje obok
     * mały dymek z informacją, jaki tekst został ustawiony.
     */
    public void typeWithHint(WebElement element, String value) {
        if (element == null) {
            return;
        }

        if (!config.isEnabled()) {
            element.clear();
            element.sendKeys(value);
            return;
        }

        rootManager.ensureRootExists();

        long duration = config.getDecorationDurationMs();
        if (duration < 0) {
            duration = 0;
        }

        String hintText = "SET: " + (value == null ? "null" : value);

        js.executeScript(
                "var el = arguments[0];" +
                        "var msg = arguments[1];" +
                        "if (!el) { return; }" +
                        "var rect = el.getBoundingClientRect();" +
                        "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) { return; }" +

                        "var hint = document.createElement('div');" +
                        "hint.textContent = msg;" +
                        "hint.style.position = 'fixed';" +
                        "hint.style.left = (rect.right + 6) + 'px';" +
                        "hint.style.top = rect.top + 'px';" +
                        "hint.style.padding = '2px 6px';" +
                        "hint.style.fontSize = '10px';" +
                        "hint.style.background = 'rgba(0,0,0,0.8)';" +
                        "hint.style.color = '#ffffff';" +
                        "hint.style.borderRadius = '3px';" +
                        "hint.style.maxWidth = '200px';" +
                        "hint.style.zIndex = '2147483647';" +
                        "hint.style.pointerEvents = 'none';" +
                        "hint.style.boxShadow = '0 0 4px rgba(0,0,0,0.4)';" +

                        "shadow.appendChild(hint);" +

                        "window.setTimeout(function () {" +
                        "  if (hint && hint.parentNode) {" +
                        "    hint.parentNode.removeChild(hint);" +
                        "  }" +
                        "}, " + duration + ");",
                element, hintText
        );

        element.clear();
        if (value != null) {
            element.sendKeys(value);
        }
    }

    public void clearAndType(WebElement input, String value) {
        if (input == null) return;
        js.executeScript(
                "arguments[0].scrollIntoView({block:'center',inline:'nearest'});" +
                        "try{arguments[0].focus();}catch(e){}" +
                        "try{arguments[0].click();}catch(e){}",
                input
        );
        input.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
        input.sendKeys(org.openqa.selenium.Keys.BACK_SPACE);
        if (value != null) {
            input.sendKeys(value);
        }
    }
}

