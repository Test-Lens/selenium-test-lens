package io.github.testlens.actions;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.HighlightJs;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

public class HighlightActions {

        private final WebDriver driver;
        private final JavascriptExecutor js;
        private final OverlayRootManager rootManager;
        private final OverlayConfig config;
        private final OverlayLogger logger;

        public HighlightActions(WebDriver driver,
                                OverlayRootManager rootManager,
                                OverlayConfig config) {
            this(driver, rootManager, config, OverlayLogger.noop());
        }

        public HighlightActions(WebDriver driver,
                                OverlayRootManager rootManager,
                                OverlayConfig config,
                                OverlayLogger logger) {
            if (driver == null) throw new IllegalArgumentException("driver must not be null");
            if (!(driver instanceof JavascriptExecutor)) {
                throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
            }
            this.driver = driver;
            this.js = (JavascriptExecutor) driver;
            this.rootManager = rootManager;
            this.config = config;
            this.logger = logger != null ? logger : OverlayLogger.noop();
        }

    /**
     * Rysuje ramkę wokół elementu + opcjonalną etykietę (np. "CLICK").
     * Niczego nie klika – to jest tylko dekoracja.
     * Ramka śledzi element przy scrollowaniu.
     */


    public void highlightClick(WebElement element, String label) {
        if (!config.isEnabled() || element == null) return;

        emitHighlight("highlightClick", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null);
        try {
            rootManager.ensureRootExists();
            long duration = Math.max(0, config.getDecorationDurationMs());
            js.executeScript(
                    HighlightJs.INIT +
                            "return window.__uiTestLens.modules.highlight.element(arguments[0], arguments[1], { duration: arguments[2], color: arguments[3] });",
                    element, label, duration, config.getHighlightColor());
            emitHighlight("highlightClick", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);
        } catch (RuntimeException hudFailure) {
            emitHighlight("highlightClick", label, UiTestLensStatus.WARN, UiTestLensLogLevel.WARN, hudFailure);
            // Decoration failure is not an action failure. Continue with Selenium click.
        }

        // 2) Klik: selenium -> fallback JS
        try {
            element.click();
            emitAction("click", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, null, null);
            return;
        } catch (ElementNotInteractableException | StaleElementReferenceException e) {
            // fallback poniżej
        } catch (WebDriverException e) {
            // czasem Selenium rzuca ogólny WebDriverException na click
            // też warto fallbackować
        }

        WebElement target = element;

        // 3) Spróbuj znaleźć bardziej “klikalny” target (np. nie <p>, tylko rodzic button/div[tabindex])
        try {
            Object resolved = js.executeScript(
                    "var el = arguments[0];" +
                            "if (!el) return null;" +
                            // jeśli kliknięty node jest tekstowy/child, spróbuj najbliższego sensownego
                            "var cand = el.closest && el.closest('button,a,[role=\"button\"],[tabindex]');" +
                            "if (cand) return cand;" +
                            // albo pierwszy klikalny wewnątrz
                            "var inside = el.querySelector && el.querySelector('button,a,[role=\"button\"],[tabindex]');" +
                            "return inside || el;",
                    element
            );
            if (resolved instanceof WebElement) {
                target = (WebElement) resolved;
            }
        } catch (Exception ignored) {}

        // 4) scroll do środka viewportu
        try {
            js.executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", target);
        } catch (Exception ignored) {}

        // 5) jeszcze jedna próba “normalnego” kliku (czasem po scrollu przechodzi)
        try {
            target.click();
            emitAction("click", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, "true", "seleniumClickAfterScroll");
            return;
        } catch (Exception ignored) {}

        // 6) ostateczność: JS click
        try {
            js.executeScript("arguments[0].click();", target);
            emitAction("click", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, "true", "jsClick");
        } catch (Exception ignored) {
            // 7) naprawdę last-last resort: Actions click (czasem pomaga)
            try {
                new Actions(driver).moveToElement(target).click().perform();
                emitAction("click", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, "true", "actionsClick");
            } catch (Exception ex) {
                emitAction("click", label, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, ex, "true", "actionsClick");
                throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
            }
        }
    }




    /**
     * Podświetla rodzica elementu (levelsUp poziomów w górę),
     * z taką samą ramką i badge'em jak highlightClick.
     */
    public void highlightParent(WebElement element, int levelsUp, String label) {
        if (!config.isEnabled() || element == null) return;
        if (levelsUp < 1) levelsUp = 1;

        emitHighlight("highlightParent", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null);
        rootManager.ensureRootExists();
        long duration = config.getDecorationDurationMs();
        String color = config.getHighlightColor();

        js.executeScript(
                HighlightJs.INIT +
                        "return window.__uiTestLens.modules.highlight.parent(arguments[0], arguments[1], arguments[2], { duration: arguments[3], color: arguments[4] });",
                element, levelsUp, label, duration, color
        );
        emitHighlight("highlightParent", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);
    }

    /**
     * Podświetla najbliższego przodka pasującego do selektora CSS,
     * z taką samą ramką i badge'em jak highlightClick.
     */
    public void highlightClosest(WebElement element, String cssSelector, String label) {
        if (!config.isEnabled() || element == null || cssSelector == null) return;

        emitHighlight("highlightClosest", label, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null);
        rootManager.ensureRootExists();
        long duration = config.getDecorationDurationMs();
        String color = config.getHighlightColor();

        js.executeScript(
                HighlightJs.INIT +
                        "return window.__uiTestLens.modules.highlight.closest(arguments[0], arguments[1], arguments[2], { duration: arguments[3], color: arguments[4] });",
                element, cssSelector, label, duration, color
        );
        emitHighlight("highlightClosest", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);
    }

    private void emitHighlight(String method,
                               String label,
                               UiTestLensStatus status,
                               UiTestLensLogLevel level,
                               Throwable throwable) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.HIGHLIGHT)
                    .status(status)
                    .message("Highlight " + method + " " + status)
                    .action(method)
                    .target(TargetDescriptor.label(label))
                    .metadata("method", method)
                    .metadata("label", safe(label))
                    .throwable(throwable)
                    .build());
        } catch (Exception ignored) {}
    }

    private void emitAction(String action,
                            String label,
                            UiTestLensStatus status,
                            UiTestLensLogLevel level,
                            Throwable throwable,
                            String fallback,
                            String fallbackType) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.ACTION)
                    .status(status)
                    .message("Action " + action + " " + status)
                    .action(action)
                    .target(TargetDescriptor.label(label))
                    .metadata("method", action)
                    .metadata("label", safe(label))
                    .throwable(throwable);
            if (fallback != null) {
                builder.metadata("fallback", fallback);
            }
            if (fallbackType != null) {
                builder.metadata("fallbackType", fallbackType);
            }
            logger.emit(builder.build());
        } catch (Exception ignored) {}
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

