package io.github.mmaciekk111.uitestlens.actions;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;

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
        rootManager.ensureRootExists();
        long duration = config.getDecorationDurationMs();
        String color = config.getHighlightColor();

        // 1) ZAWSZE dekoruj overlay (bez klikania w JS)
        js.executeScript(
                "var target = arguments[0];" +
                        "var label = arguments[1];" +
                        "var duration = arguments[2];" +
                        "var highlightColor = arguments[3];" +
                        "if (!target || !target.getBoundingClientRect) return;" +
                        "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) return;" +

                        "var container = document.createElement('div');" +
                        "container.className = 'selenium-overlay-highlight';" +
                        "container.style.position = 'fixed';" +
                        "container.style.border = '2px solid ' + (highlightColor || '#ffeb3b');" +
                        "container.style.borderRadius = '4px';" +
                        "container.style.boxSizing = 'border-box';" +
                        "container.style.pointerEvents = 'none';" +
                        "container.style.zIndex = '2147483647';" +

                        "var badge = document.createElement('div');" +
                        "badge.textContent = label || '';" +
                        "badge.style.position = 'absolute';" +
                        "badge.style.top = '-18px';" +
                        "badge.style.left = '0';" +
                        "badge.style.padding = '2px 6px';" +
                        "badge.style.fontSize = '10px';" +
                        "badge.style.background = (highlightColor || '#ffeb3b');" +
                        "badge.style.color = '#000';" +
                        "badge.style.borderRadius = '3px';" +
                        "badge.style.whiteSpace = 'nowrap';" +
                        "container.appendChild(badge);" +
                        "shadow.appendChild(container);" +

                        "function update() {" +
                        "  try {" +
                        "    if (!document.body.contains(target)) return;" +
                        "    var rect = target.getBoundingClientRect();" +
                        "    container.style.left = rect.left + 'px';" +
                        "    container.style.top = rect.top + 'px';" +
                        "    container.style.width = rect.width + 'px';" +
                        "    container.style.height = rect.height + 'px';" +
                        "  } catch(e) {}" +
                        "}" +

                        "function onScroll(){ update(); }" +
                        "function cleanup(){" +
                        "  window.removeEventListener('scroll', onScroll, true);" +
                        "  window.removeEventListener('resize', onScroll, true);" +
                        "  if (container && container.parentNode) container.parentNode.removeChild(container);" +
                        "}" +

                        "window.addEventListener('scroll', onScroll, true);" +
                        "window.addEventListener('resize', onScroll, true);" +
                        "update();" +
                        "setTimeout(cleanup, duration);",
                element, label, duration, color
        );
        emitHighlight("highlightClick", label, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null);

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
                "var el = arguments[0];" +
                        "var label = arguments[1];" +
                        "var levels = arguments[2] || 1;" +
                        "var duration = arguments[3];" +
                        "var highlightColor = arguments[4];" +
                        "if (!el) return;" +

                        // szukamy rodzica
                        "while (levels > 0 && el && el.parentElement) {" +
                        "  el = el.parentElement;" +
                        "  levels--;" +
                        "}" +
                        "var target = el;" +
                        "if (!target || !target.getBoundingClientRect) return;" +

                        "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) return;" +

                        "var container = document.createElement('div');" +
                        "container.className = 'selenium-overlay-highlight-parent';" +
                        "container.style.position = 'fixed';" +
                        "container.style.border = '2px solid ' + (highlightColor || '#ffeb3b');" +
                        "container.style.borderRadius = '4px';" +
                        "container.style.boxSizing = 'border-box';" +
                        "container.style.pointerEvents = 'none';" +
                        "container.style.zIndex = '2147483647';" +

                        "var badge = document.createElement('div');" +
                        "badge.textContent = label || '';" +
                        "badge.style.position = 'absolute';" +
                        "badge.style.top = '-18px';" +
                        "badge.style.left = '0';" +
                        "badge.style.padding = '2px 6px';" +
                        "badge.style.fontSize = '10px';" +
                        "badge.style.background = (highlightColor || '#ffeb3b');" +
                        "badge.style.color = '#000';" +
                        "badge.style.borderRadius = '3px';" +
                        "badge.style.whiteSpace = 'nowrap';" +
                        "container.appendChild(badge);" +

                        "shadow.appendChild(container);" +

                        "function update() {" +
                        "  if (!document.body.contains(target)) { return; }" +
                        "  var rect = target.getBoundingClientRect();" +
                        "  container.style.left = rect.left + 'px';" +
                        "  container.style.top = rect.top + 'px';" +
                        "  container.style.width = rect.width + 'px';" +
                        "  container.style.height = rect.height + 'px';" +
                        "}" +

                        "function onScroll() {" +
                        "  update();" +
                        "}" +

                        "function cleanup() {" +
                        "  window.removeEventListener('scroll', onScroll, true);" +
                        "  window.removeEventListener('resize', onScroll, true);" +
                        "  if (container && container.parentNode) {" +
                        "    container.parentNode.removeChild(container);" +
                        "  }" +
                        "}" +

                        "window.addEventListener('scroll', onScroll, true);" +
                        "window.addEventListener('resize', onScroll, true);" +
                        "update();" +
                        "setTimeout(cleanup, duration);",
                element, label, levelsUp, duration, color
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
                "var el = arguments[0];" +
                        "var label = arguments[1];" +
                        "var selector = arguments[2];" +
                        "var duration = arguments[3];" +
                        "var highlightColor = arguments[4];" +
                        "if (!el || !selector) return;" +

                        "var target = null;" +
                        "if (el.closest) {" +
                        "  target = el.closest(selector);" +
                        "} else {" +
                        "  var node = el;" +
                        "  while (node && node !== document.body) {" +
                        "    if (node.matches && node.matches(selector)) { target = node; break; }" +
                        "    node = node.parentElement;" +
                        "  }" +
                        "}" +

                        "if (!target || !target.getBoundingClientRect) return;" +

                        "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) return;" +

                        "var container = document.createElement('div');" +
                        "container.className = 'selenium-overlay-highlight-closest';" +
                        "container.style.position = 'fixed';" +
                        "container.style.border = '2px solid ' + (highlightColor || '#ffeb3b');" +
                        "container.style.borderRadius = '4px';" +
                        "container.style.boxSizing = 'border-box';" +
                        "container.style.pointerEvents = 'none';" +
                        "container.style.zIndex = '2147483647';" +

                        "var badge = document.createElement('div');" +
                        "badge.textContent = label || '';" +
                        "badge.style.position = 'absolute';" +
                        "badge.style.top = '-18px';" +
                        "badge.style.left = '0';" +
                        "badge.style.padding = '2px 6px';" +
                        "badge.style.fontSize = '10px';" +
                        "badge.style.background = (highlightColor || '#ffeb3b');" +
                        "badge.style.color = '#000';" +
                        "badge.style.borderRadius = '3px';" +
                        "badge.style.whiteSpace = 'nowrap';" +
                        "container.appendChild(badge);" +

                        "shadow.appendChild(container);" +

                        "function update() {" +
                        "  if (!document.body.contains(target)) { return; }" +
                        "  var rect = target.getBoundingClientRect();" +
                        "  container.style.left = rect.left + 'px';" +
                        "  container.style.top = rect.top + 'px';" +
                        "  container.style.width = rect.width + 'px';" +
                        "  container.style.height = rect.height + 'px';" +
                        "}" +

                        "function onScroll() {" +
                        "  update();" +
                        "}" +

                        "function cleanup() {" +
                        "  window.removeEventListener('scroll', onScroll, true);" +
                        "  window.removeEventListener('resize', onScroll, true);" +
                        "  if (container && container.parentNode) {" +
                        "    container.parentNode.removeChild(container);" +
                        "  }" +
                        "}" +

                        "window.addEventListener('scroll', onScroll, true);" +
                        "window.addEventListener('resize', onScroll, true);" +
                        "update();" +
                        "setTimeout(cleanup, duration);",
                element, label, cssSelector, duration, color
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
