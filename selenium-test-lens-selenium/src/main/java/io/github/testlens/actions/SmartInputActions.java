package io.github.testlens.actions;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.BlockingOverlayHelper;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * "Sprytny" input:
 * - próbuje wpisać tekst (typeWithHint),
 * - jeśli coś zasłania input (popup/overlay), próbuje to zamknąć,
 * - ponawia wpisanie.
 */
public class SmartInputActions {

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final TypingActions typingActions;
    private final BlockingOverlayHelper blockingHelper;
    private final OverlayLogger logger;

    public SmartInputActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             TypingActions typingActions) {
        this(driver, config, rootManager, typingActions, OverlayLogger.noop());
    }

    public SmartInputActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             TypingActions typingActions,
                             OverlayLogger logger) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.typingActions = typingActions;
        this.blockingHelper = new BlockingOverlayHelper(driver, config, rootManager, null);
        this.logger = logger != null ? logger : OverlayLogger.noop();
        // Uwaga: tu możesz pominąć highlightActions w helperze,
        // jeśli nie chcesz highlightować overlaya przy wpisywaniu.
    }

    public void smartTypeWithHint(WebElement element, String value, String labelForOverlay) {
        if (element == null) return;
        emitSmartInput(labelForOverlay, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null, value, false);

        // ✨ najpierw spróbuj globalnie zamknąć overlay (cookies itp.)
        blockingHelper.handleGlobalOverlayIfPresent(
                labelForOverlay != null ? labelForOverlay : "OVERLAY",
                "CLOSE"
        );

        try {
            typingActions.typeWithHint(element, value);
            emitSmartInput(labelForOverlay, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, value, false);
            return;
        } catch (WebDriverException e) {
            if (!isInputInterceptError(e)) {
                emitSmartInput(labelForOverlay, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, e, value, false);
                throw e;
            }
        }

        // fallback: coś blokuje konkretny input – próbujemy zamknąć overlay nad nim
        boolean handled = blockingHelper.handleBlockingOverlayFor(
                element,
                labelForOverlay != null ? labelForOverlay : "OVERLAY",
                "CLOSE"
        );

        // w obu przypadkach ponów wpisanie
        try {
            typingActions.typeWithHint(element, value);
            emitSmartInput(labelForOverlay, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, value, handled);
        } catch (WebDriverException e) {
            emitSmartInput(labelForOverlay, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, e, value, handled);
            throw e;
        }
    }


    private boolean isInputInterceptError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("other element would receive the click")
                || msg.contains("is not clickable at point")
                || msg.contains("not interactable")
                || msg.contains("is not clickable at point")
                || msg.contains("intercepted");
    }

    private void emitSmartInput(String label,
                                UiTestLensStatus status,
                                UiTestLensLogLevel level,
                                Throwable throwable,
                                String value,
                                boolean fallback) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.ACTION)
                    .status(status)
                    .message("Input action smartType " + status)
                    .action("smartType")
                    .target(TargetDescriptor.label(label))
                    .metadata("method", "smartTypeWithHint")
                    .metadata("label", label == null ? "" : label)
                    .metadata("valueLength", value == null ? "0" : String.valueOf(value.length()))
                    .metadata("fallback", String.valueOf(fallback))
                    .throwable(throwable)
                    .build());
        } catch (Exception ignored) {}
    }
}

