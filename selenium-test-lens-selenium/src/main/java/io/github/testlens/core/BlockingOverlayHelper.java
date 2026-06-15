package io.github.testlens.core;

import io.github.testlens.OverlayConfig;
import io.github.testlens.actions.HighlightActions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class BlockingOverlayHelper {

    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final OverlayRootManager rootManager;
    private final HighlightActions highlightActions;

    public BlockingOverlayHelper(WebDriver driver,
                                 OverlayConfig config,
                                 OverlayRootManager rootManager,
                                 HighlightActions highlightActions) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.rootManager = rootManager;
        this.highlightActions = highlightActions;
    }
    /**
     * Próbuje globalnie zamknąć overlay/popup na podstawie
     * globalOverlayCloseButtonSelector z configu.
     * Nie patrzy na konkretny target – po prostu szuka tego przycisku.
     */
    public boolean handleGlobalOverlayIfPresent(String overlayLabel, String closeButtonLabel) {
        String selector = config.getGlobalOverlayCloseButtonSelector();
        if (selector == null || selector.isBlank()) {
            return false;
        }

        Object result = js.executeScript(globalOverlayCloseButtonScript(), selector);

        if (!(result instanceof WebElement btn)) {
            return false;
        }

        // optional dekoracja overlaya
        if (config.isEnabled() && highlightActions != null) {
            rootManager.ensureRootExists();
            highlightActions.highlightClick(btn,
                    overlayLabel != null ? overlayLabel : "OVERLAY");
        }

        // kliknięcie przycisku zamknięcia
        if (config.isEnabled() && highlightActions != null) {
            highlightActions.highlightClick(btn,
                    closeButtonLabel != null ? closeButtonLabel : "CLOSE");
        } else {
            btn.click();
        }

        try {
            Thread.sleep(500); // daj czas, żeby popup zniknął
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return true;
    }

    /**
     * Próbuje zamknąć overlay zasłaniający target.
     *
     * @param target          element, na którym chcieliśmy działać (click / input)
     * @param overlayLabel    label do narysowania na overlayu (np. "OVERLAY")
     * @param closeButtonLabel label na przycisku zamykającym (np. "CLOSE")
     * @return true jeśli coś realnie zamknęliśmy, false jeśli nie znaleźliśmy nic sensownego
     */
    public boolean handleBlockingOverlayFor(WebElement target,
                                            String overlayLabel,
                                            String closeButtonLabel) {
        WebElement overlay = findBlockingOverlay(target);
        if (overlay == null) {
            return false;
        }

        if (config.isEnabled()) {
            rootManager.ensureRootExists();
            highlightActions.highlightClick(overlay,
                    overlayLabel != null ? overlayLabel : "OVERLAY");
        }

        WebElement closeButton = findCloseButtonInside(overlay);
        if (closeButton == null) {
            return false;
        }

        if (config.isEnabled()) {
            highlightActions.highlightClick(closeButton,
                    closeButtonLabel != null ? closeButtonLabel : "CLOSE");
        } else {
            closeButton.click();
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return true;
    }

    static String globalOverlayCloseButtonScript() {
        return "var sel = arguments[0];" +
                "var btn = document.querySelector(sel);" +
                "if (!btn) return null;" +
                "function isVisible(el) {" +
                "  if (!el || !el.getBoundingClientRect) return false;" +
                "  var style = window.getComputedStyle(el);" +
                "  if (style.display === 'none' || style.visibility === 'hidden') return false;" +
                "  if (parseFloat(style.opacity || '1') < 0.05) return false;" +
                "  var rect = el.getBoundingClientRect();" +
                "  if (rect.width <= 0 || rect.height <= 0) return false;" +
                "  return true;" +
                "}" +
                "return isVisible(btn) ? btn : null;";
    }

    static String blockingOverlayForTargetScript() {
        return "var el = arguments[0];" +
                "if (!el || !el.getBoundingClientRect) return null;" +
                "var rect = el.getBoundingClientRect();" +
                "var cx = rect.left + rect.width / 2;" +
                "var cy = rect.top + rect.height / 2;" +
                "var topEl = document.elementFromPoint(cx, cy);" +
                "if (!topEl) return null;" +
                "function isOverlayCandidate(node) {" +
                "  if (!node || !node.getBoundingClientRect) return false;" +
                "  var style = window.getComputedStyle(node);" +
                "  var pos = style.position;" +
                "  if (!(pos === 'fixed' || pos === 'absolute' || pos === 'sticky')) return false;" +
                "  var rect = node.getBoundingClientRect();" +
                "  if (rect.width < 50 || rect.height < 40) return false;" +
                "  var z = parseInt(style.zIndex, 10);" +
                "  if (isNaN(z)) z = 0;" +
                "  if (z < 10) return false;" +
                "  return true;" +
                "}" +
                "var overlay = topEl;" +
                "while (overlay && overlay !== document.body) {" +
                "  if (isOverlayCandidate(overlay)) {" +
                "    return overlay;" +
                "  }" +
                "  overlay = overlay.parentElement;" +
                "}" +
                "return null;";
    }

    static String closeButtonInsideScript() {
        return "var root = arguments[0];" +
                "if (!root) return null;" +
                "var selectors = [" +
                "  'button', 'a', '[role=\"button\"]'," +
                "  'button[id*=\"close\" i]'," +
                "  'button[id*=\"accept\" i]'," +
                "  'button[id*=\"agree\" i]'," +
                "  'button[id*=\"ok\" i]'," +
                "  '[class*=\"close\" i]'," +
                "  '[class*=\"accept\" i]'," +
                "  '[class*=\"consent\" i]'," +
                "  '[data-test*=\"close\" i]'," +
                "  '[data-testid*=\"close\" i]'," +
                "  '[aria-label*=\"close\" i]'," +
                "  '[aria-label*=\"zamknij\" i]'" +
                "];" +
                "var candidates = [];" +
                "selectors.forEach(function(sel) {" +
                "  try {" +
                "    var nodes = root.querySelectorAll(sel);" +
                "    for (var i = 0; i < nodes.length; i++) {" +
                "      candidates.push(nodes[i]);" +
                "    }" +
                "  } catch (e) {}" +
                "});" +
                "function isVisible(el) {" +
                "  if (!el || !el.getBoundingClientRect) return false;" +
                "  var style = window.getComputedStyle(el);" +
                "  if (style.display === 'none' || style.visibility === 'hidden') return false;" +
                "  if (parseFloat(style.opacity || '1') < 0.1) return false;" +
                "  var rect = el.getBoundingClientRect();" +
                "  if (rect.width <= 0 || rect.height <= 0) return false;" +
                "  return true;" +
                "}" +
                "var best = null;" +
                "var bestScore = 0;" +
                "for (var i = 0; i < candidates.length; i++) {" +
                "  var el = candidates[i];" +
                "  if (!isVisible(el)) continue;" +
                "  var text = (el.textContent || '').toLowerCase();" +
                "  var score = 0;" +
                "  if (text.indexOf('akceptuj') !== -1 || text.indexOf('akceptuję') !== -1 ||" +
                "      text.indexOf('accept') !== -1 || text.indexOf('agree') !== -1) {" +
                "    score += 50;" +
                "  }" +
                "  if (text.indexOf('zamknij') !== -1 || text.indexOf('close') !== -1 ||" +
                "      text === 'ok' || text.indexOf('ok') !== -1) {" +
                "    score += 40;" +
                "  }" +
                "  if (score === 0) {" +
                "    score += 10;" +
                "  }" +
                "  var rect = el.getBoundingClientRect();" +
                "  score += rect.width * rect.height / 1000;" +
                "  if (score > bestScore) {" +
                "    bestScore = score;" +
                "    best = el;" +
                "  }" +
                "}" +
                "return best;";
    }

    /**
     * JS:
     * - bierze środek targetu,
     * - odpala document.elementFromPoint(x,y),
     * - idzie po parentach w górę i szuka kandydata na overlay:
     *   fixed/absolute/sticky, większy element, z sensownym z-index.
     */
    private WebElement findBlockingOverlay(WebElement target) {
        Object result = js.executeScript(blockingOverlayForTargetScript(), target);

        if (result instanceof WebElement) {
            return (WebElement) result;
        }
        return null;
    }

    /**
     * Szuka przycisku zamknięcia/akceptacji w środku overlayu:
     * - po selektorach id/class,
     * - po tekście (PL/EN).
     */
    private WebElement findCloseButtonInside(WebElement overlay) {
        Object result = js.executeScript(closeButtonInsideScript(), overlay);

        if (result instanceof WebElement) {
            return (WebElement) result;
        }
        return null;
    }
}

