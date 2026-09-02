package io.github.testlens.actions;

import io.github.testlens.OverlayConfig;
import io.github.testlens.core.HighlightJs;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** Browser decoration helpers. None of these methods performs an application action. */
public class HighlightActions {
    private final JavascriptExecutor js;
    private final OverlayRootManager rootManager;
    private final OverlayConfig config;
    private final OverlayLogger logger;

    public HighlightActions(WebDriver driver, OverlayRootManager rootManager, OverlayConfig config) {
        this(driver, rootManager, config, OverlayLogger.noop());
    }

    public HighlightActions(WebDriver driver, OverlayRootManager rootManager, OverlayConfig config, OverlayLogger logger) {
        if (driver == null) throw new IllegalArgumentException("driver must not be null");
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.rootManager = rootManager;
        this.config = config;
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    /** Draws a border and optional label around an element without clicking it. */
    public void highlightClick(WebElement element, String label) {
        if (!config.isEnabled() || element == null) return;
        emitHighlight("highlightClick", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null);
        try {
            rootManager.ensureRootExists();
            js.executeScript(
                    HighlightJs.INIT
                            + "return window.__uiTestLens.modules.highlight.element(arguments[0], arguments[1], { duration: arguments[2], color: arguments[3] });",
                    element, label, Math.max(0, config.getDecorationDurationMs()), config.getHighlightColor());
            emitHighlight("highlightClick", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);
        } catch (RuntimeException decorationFailure) {
            emitHighlight("highlightClick", label, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN, decorationFailure);
        }
    }

    /** Draws the same decoration around an ancestor a specified number of levels up. */
    public void highlightParent(WebElement element, int levelsUp, String label) {
        if (!config.isEnabled() || element == null) return;
        if (levelsUp < 1) levelsUp = 1;
        emitHighlight("highlightParent", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null);
        rootManager.ensureRootExists();
        js.executeScript(
                HighlightJs.INIT
                        + "return window.__uiTestLens.modules.highlight.parent(arguments[0], arguments[1], arguments[2], { duration: arguments[3], color: arguments[4] });",
                element, levelsUp, label, config.getDecorationDurationMs(), config.getHighlightColor());
        emitHighlight("highlightParent", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);
    }

    /** Draws the same decoration around the closest ancestor matching a CSS selector. */
    public void highlightClosest(WebElement element, String cssSelector, String label) {
        if (!config.isEnabled() || element == null || cssSelector == null) return;
        emitHighlight("highlightClosest", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null);
        rootManager.ensureRootExists();
        js.executeScript(
                HighlightJs.INIT
                        + "return window.__uiTestLens.modules.highlight.closest(arguments[0], arguments[1], arguments[2], { duration: arguments[3], color: arguments[4] });",
                element, cssSelector, label, config.getDecorationDurationMs(), config.getHighlightColor());
        emitHighlight("highlightClosest", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);
    }

    private void emitHighlight(String method, String label, UiTestLensStatus status,
                               UiTestLensLogLevel level, Throwable throwable) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.HIGHLIGHT)
                    .status(status)
                    .message("Highlight " + method + " " + status)
                    .action(method)
                    .target(TargetDescriptor.label(label))
                    .metadata("method", method)
                    .metadata("label", label == null ? "" : label)
                    .throwable(throwable)
                    .build());
        } catch (Exception ignored) {
            // Logging is best effort.
        }
    }
}
