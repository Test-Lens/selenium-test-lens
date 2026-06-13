package io.github.mmaciekk111.uitestlens.actions;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class TypingActions {

    private final JavascriptExecutor js;
    private final OverlayRootManager rootManager;
    private final OverlayConfig config;
    private final OverlayLogger logger;

    public TypingActions(WebDriver driver,
                         OverlayRootManager rootManager,
                         OverlayConfig config) {
        this(driver, rootManager, config, OverlayLogger.noop());
    }

    public TypingActions(WebDriver driver,
                         OverlayRootManager rootManager,
                         OverlayConfig config,
                         OverlayLogger logger) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.rootManager = rootManager;
        this.config = config;
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }



    /**
     * Wpisuje tekst do elementu (clear + sendKeys) oraz pokazuje obok
     * mały dymek z informacją, jaki tekst został ustawiony.
     */
    public void typeWithHint(WebElement element, String value) {
        if (element == null) {
            return;
        }
        emitAction("type", UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null, value);

        try {
            if (!config.isEnabled()) {
                element.clear();
                element.sendKeys(value);
                emitAction("type", UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, value);
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
            emitAction("type", UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, value);
        } catch (RuntimeException e) {
            emitAction("type", UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, e, value);
            throw e;
        }
    }

    public void clearAndType(WebElement input, String value) {
        if (input == null) return;
        emitAction("clearAndType", UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null, value);
        try {
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
            emitAction("clearAndType", UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, value);
        } catch (RuntimeException e) {
            emitAction("clearAndType", UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, e, value);
            throw e;
        }
    }

    private void emitAction(String action,
                            UiTestLensStatus status,
                            UiTestLensLogLevel level,
                            Throwable throwable,
                            String value) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.ACTION)
                    .status(status)
                    .message("Input action " + action + " " + status)
                    .action(action)
                    .target(TargetDescriptor.none())
                    .metadata("method", action)
                    .metadata("valueLength", value == null ? "0" : String.valueOf(value.length()))
                    .throwable(throwable)
                    .build());
        } catch (Exception ignored) {}
    }
}

