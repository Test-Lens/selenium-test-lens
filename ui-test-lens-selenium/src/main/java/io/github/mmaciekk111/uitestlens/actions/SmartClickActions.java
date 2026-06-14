package io.github.mmaciekk111.uitestlens.actions;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.BlockingOverlayHelper;
import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

public class SmartClickActions {

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final HighlightActions highlightActions;
    private final BlockingOverlayHelper blockingHelper;
    private final OverlayLogger logger;

    public SmartClickActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             HighlightActions highlightActions) {
        this(driver, config, rootManager, highlightActions, OverlayLogger.noop());
    }

    public SmartClickActions(WebDriver driver,
                             OverlayConfig config,
                             OverlayRootManager rootManager,
                             HighlightActions highlightActions,
                             OverlayLogger logger) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.highlightActions = highlightActions;
        this.blockingHelper = new BlockingOverlayHelper(driver, config, rootManager, highlightActions);
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    /**
     * Główna implementacja klika z obsługą overlayów/popupów.
     * (wcześniej to było w smartClick).
     */
    public void clickWithOverlayHandling(WebElement target, String label) {
        if (target == null) {
            return;
        }
        emitClick("clickWithOverlayHandling", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null, false, null, false);
        try {

        // ✨ NAJPIERW spróbuj globalnie zamknąć znany overlay (np. cookies)
        blockingHelper.handleGlobalOverlayIfPresent("OVERLAY", "CLOSE");

        try {
            if (config.isEnabled()) {
                highlightActions.highlightClick(target, label);
            } else {
                target.click();
            }
            emitClick("clickWithOverlayHandling", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, false, null, true);
            return;
        } catch (WebDriverException e) {
            if (!isClickInterceptError(e)) {
                throw e;
            }
        }

        // dopiero jeśli mimo wszystko jest błąd, lecimy w logikę "overlay blokuje target"
        boolean handled = blockingHelper.handleBlockingOverlayFor(
                target,
                "BLOCKING OVERLAY",
                "CLOSE"
        );

        if (handled) {
            if (config.isEnabled()) {
                highlightActions.highlightClick(target, label);
            } else {
                target.click();
            }
            emitClick("clickWithOverlayHandling", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, true, "blockingOverlay", true);
        } else {
            target.click();
            emitClick("clickWithOverlayHandling", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, true, "directRetry", false);
        }
        } catch (RuntimeException e) {
            emitClick("clickWithOverlayHandling", label, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, e, false, null, false);
            throw e;
        }
    }

    /**
     * Stara nazwa – zostawiamy dla kompatybilności.
     */
    @Deprecated
    public void smartClick(WebElement target, String label) {
        clickWithOverlayHandling(target, label);
    }

    private boolean isClickInterceptError(Throwable e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("other element would receive the click")
                || msg.contains("is not clickable at point")
                || msg.contains("intercepted");
    }

    private void emitClick(String method,
                           String label,
                           UiTestLensStatus status,
                           UiTestLensLogLevel level,
                           Throwable throwable,
                           boolean fallback,
                           String fallbackType,
                           boolean popupHandled) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.ACTION)
                    .status(status)
                    .message("Click action " + method + " " + status)
                    .action(method)
                    .target(TargetDescriptor.label(label))
                    .metadata("method", method)
                    .metadata("label", label == null ? "" : label)
                    .metadata("fallback", String.valueOf(fallback))
                    .metadata("popupHandled", String.valueOf(popupHandled))
                    .throwable(throwable);
            if (fallbackType != null) {
                builder.metadata("fallbackType", fallbackType);
            }
            logger.emit(builder.build());
        } catch (Exception ignored) {}
    }
}
