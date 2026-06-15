package io.github.testlens.actions;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.TypeHintJs;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
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
                    TypeHintJs.INIT +
                            "return window.__uiTestLens.modules.typeHint.show(arguments[0], arguments[1], { duration: arguments[2] });",
                    element, hintText, duration
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


