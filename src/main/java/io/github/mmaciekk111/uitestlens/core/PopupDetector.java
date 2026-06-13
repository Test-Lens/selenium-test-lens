package io.github.mmaciekk111.uitestlens.core;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.actions.HighlightActions;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;
public class PopupDetector {
private final WebDriver driver;
private final JavascriptExecutor js;
private final OverlayConfig config;
private final OverlayRootManager rootManager;
private final HighlightActions highlightActions;

public PopupDetector(WebDriver driver,
                     OverlayConfig config,
                     OverlayRootManager rootManager,
                     HighlightActions highlightActions) {
    if (!(driver instanceof JavascriptExecutor)) {
        throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
    }
    this.driver = driver;
    this.js = (JavascriptExecutor) driver;
    this.config = config;
    this.rootManager = rootManager;
    this.highlightActions = highlightActions;
}

/**
 * Szuka potencjalnego popupa na wierzchu i zwraca go jako WebElement (jeśli jest).
 */
public Optional<WebElement> findTopMostPopup() {
    Object result = js.executeScript(
            "var selectors = [" +
                    "  '[role=\"dialog\"]'," +
                    "  '[role=\"alertdialog\"]'," +
                    "  '[aria-modal=\"true\"]'," +
                    "  '.modal'," +
                    "  '.dialog'," +
                    "  '.popup'," +
                    "  '.MuiDialog-root'," +
                    "  '.ant-modal'," +
                    "  '.cdk-overlay-pane'" +
                    "];" +

                    // zbierz kandydatów po selektorach
                    "var candidates = [];" +
                    "selectors.forEach(function(sel) {" +
                    "  try {" +
                    "    var nodes = document.querySelectorAll(sel);" +
                    "    for (var i = 0; i < nodes.length; i++) {" +
                    "      candidates.push(nodes[i]);" +
                    "    }" +
                    "  } catch (e) {}" +
                    "});" +

                    // jeśli nic nie znaleziono po selektorach, zrób fallback:
                    "if (candidates.length === 0) {" +
                    "  candidates = Array.prototype.slice.call(document.body.getElementsByTagName('*'));" +
                    "}" +

                    "var best = null;" +
                    "var bestScore = 0;" +

                    "for (var i = 0; i < candidates.length; i++) {" +
                    "  var el = candidates[i];" +
                    "  if (!el || !el.getBoundingClientRect) continue;" +

                    "  var style = window.getComputedStyle(el);" +
                    "  if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') continue;" +

                    "  if (!(style.position === 'fixed' || style.position === 'absolute')) continue;" +

                    "  var z = parseInt(style.zIndex, 10);" +
                    "  if (isNaN(z)) { z = 0; }" +

                    "  var rect = el.getBoundingClientRect();" +
                    "  if (rect.width <= 0 || rect.height <= 0) continue;" +

                    // minimalny rozmiar: np. >= 30% szerokości i >= 20% wysokości viewportu
                    "  var minWidth = window.innerWidth * 0.3;" +
                    "  var minHeight = window.innerHeight * 0.2;" +
                    "  if (rect.width < minWidth || rect.height < minHeight) continue;" +

                    // musi być przynajmniej częściowo w viewport
                    "  if (rect.right < 0 || rect.bottom < 0 || rect.left > window.innerWidth || rect.top > window.innerHeight) continue;" +

                    // score: rozmiar * z-index (prosto, ale działa)
                    "  var area = rect.width * rect.height;" +
                    "  var score = area + z * 1000;" +

                    "  if (score > bestScore) {" +
                    "    bestScore = score;" +
                    "    best = el;" +
                    "  }" +
                    "}" +

                    "return best;"
    );

    if (result instanceof WebElement) {
        return Optional.of((WebElement) result);
    }
    return Optional.empty();
}

/**
 * Wykrywa potencjalny popup i jeśli jest, podświetla go overlayem.
 *
 * @param label etykieta, np. \"POPUP\" albo \"MODAL\"
 * @return true, jeśli popup został wykryty, false jeśli nie.
 */
public boolean highlightPopupIfPresent(String label) {
    Optional<WebElement> popupOpt = findTopMostPopup();
    if (popupOpt.isPresent()) {
        WebElement popup = popupOpt.get();
        if (config.isEnabled()) {
            rootManager.ensureRootExists();
            highlightActions.highlightClick(popup, label != null ? label : "POPUP");
        }
        return true;
    }
    return false;
}
    /**
     * Próbuje zamknąć popup/overlay:
     * 1) najpierw po globalOverlayCloseButtonSelector (jeśli ustawiony),
     * 2) potem heurystycznie – overlay na środku ekranu + przycisk close/accept.
     *
     * @return true jeśli coś zostało realnie kliknięte, false jeśli nic nie znaleziono.
     */
    public boolean closePopupIfPresent(String overlayLabel, String closeButtonLabel) {
        // 1) najpierw próba globalnego selektora (np. #acceptCookies)
        WebElement button = findGlobalCloseButtonIfVisible();
        WebElement overlayRoot = null;

        if (button == null) {
            // 2) próbujemy heurystycznie znaleźć overlay na środku ekranu
            overlayRoot = findOverlayAtViewportCenter();
            if (overlayRoot != null) {
                button = findCloseButtonInside(overlayRoot);
            }
        }

        if (button == null) {
            return false;
        }

        try {
            if (config.isEnabled() && highlightActions != null) {
                rootManager.ensureRootExists();
                // opcjonalnie podświetlamy cały popup
                if (overlayRoot != null && overlayLabel != null && !overlayLabel.isBlank()) {
                    highlightActions.highlightClick(overlayRoot, overlayLabel);
                }
                // i sam przycisk zamknięcia
                highlightActions.highlightClick(
                        button,
                        (closeButtonLabel != null && !closeButtonLabel.isBlank()) ? closeButtonLabel : "CLOSE"
                );
            } else {
                button.click();
            }

            // dajmy chwilę na zniknięcie popupa
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return true;
        } catch (Exception e) {
            // jeżeli coś poszło nie tak – nie zabijamy testu, tylko mówimy, że się nie udało
            return false;
        }
    }

    /**
     * Wygodna wersja z domyślnymi labelkami.
     */
    public boolean closePopupIfPresent() {
        return closePopupIfPresent("POPUP", "CLOSE");
    }
    /**
     * Próbuje znaleźć globalny przycisk zamykający popup na podstawie
     * config.getGlobalOverlayCloseButtonSelector().
     */
    private WebElement findGlobalCloseButtonIfVisible() {
        String selector = config.getGlobalOverlayCloseButtonSelector();
        if (selector == null || selector.isBlank()) {
            return null;
        }

        Object result = js.executeScript(
                "var sel = arguments[0];" +
                        "function isVisible(el) {" +
                        "  if (!el || !el.getBoundingClientRect) return false;" +
                        "  var style = window.getComputedStyle(el);" +
                        "  if (style.display === 'none' || style.visibility === 'hidden') return false;" +
                        "  if (parseFloat(style.opacity || '1') < 0.05) return false;" +
                        "  var rect = el.getBoundingClientRect();" +
                        "  if (rect.width <= 0 || rect.height <= 0) return false;" +
                        "  return true;" +
                        "}" +
                        "try {" +
                        "  var btn = document.querySelector(sel);" +
                        "  if (!btn) return null;" +
                        "  return isVisible(btn) ? btn : null;" +
                        "} catch (e) {" +
                        "  return null;" +
                        "}",
                selector
        );

        if (result instanceof WebElement) {
            return (WebElement) result;
        }
        return null;
    }

    /**
     * Szuka overlaya na środku viewportu – podobnie jak przy klikach:
     * bierzemy element spod środka ekranu i idziemy po parentach w górę.
     */
    private WebElement findOverlayAtViewportCenter() {
        Object result = js.executeScript(
                "var cx = (window.innerWidth || document.documentElement.clientWidth || 800) / 2;" +
                        "var cy = (window.innerHeight || document.documentElement.clientHeight || 600) / 2;" +
                        "var topEl = document.elementFromPoint(cx, cy);" +
                        "if (!topEl) return null;" +

                        "function isOverlayCandidate(node) {" +
                        "  if (!node || !node.getBoundingClientRect) return false;" +
                        "  var style = window.getComputedStyle(node);" +
                        "  var pos = style.position;" +
                        "  if (!(pos === 'fixed' || pos === 'absolute' || pos === 'sticky')) return false;" +
                        "  var rect = node.getBoundingClientRect();" +
                        "  if (rect.width < 80 || rect.height < 50) return false;" +
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
                        "return null;"
        );

        if (result instanceof WebElement) {
            return (WebElement) result;
        }
        return null;
    }

    /**
     * Szuka przycisku zamykającego/akceptującego w środku overlaya
     * (close / accept / akceptuję / ok itp.).
     */
    private WebElement findCloseButtonInside(WebElement overlayRoot) {
        if (overlayRoot == null) {
            return null;
        }

        Object result = js.executeScript(
                "var root = arguments[0];" +
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
                        "for (var s = 0; s < selectors.length; s++) {" +
                        "  try {" +
                        "    var nodes = root.querySelectorAll(selectors[s]);" +
                        "    for (var i = 0; i < nodes.length; i++) {" +
                        "      candidates.push(nodes[i]);" +
                        "    }" +
                        "  } catch (e) {}" +
                        "}" +

                        "function isVisible(el) {" +
                        "  if (!el || !el.getBoundingClientRect) return false;" +
                        "  var style = window.getComputedStyle(el);" +
                        "  if (style.display === 'none' || style.visibility === 'hidden') return false;" +
                        "  if (parseFloat(style.opacity || '1') < 0.05) return false;" +
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
                        "return best;"
                ,
                overlayRoot
        );

        if (result instanceof WebElement) {
            return (WebElement) result;
        }
        return null;
    }

}