package utils.jsExecHelper.actions;

import utils.jsExecHelper.OverlayConfig;
import utils.jsExecHelper.core.OverlayRootManager;
import utils.jsExecHelper.scroll.ScrollElementEdge;
import utils.jsExecHelper.scroll.ScrollViewportEdge;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Akcje związane z przewijaniem:
 * - płynne przewijanie do elementu,
 * - strzałka (w górę / w dół) pokazana na środku krawędzi ekranu,
 * - po dojechaniu do elementu strzałka przeskakuje nad/obok elementu.
 * Metody są blokujące (executeAsyncScript) – kolejne kroki testu
 * są wykonywane dopiero po zakończeniu animacji.
 */
public class ScrollActions {

    private final JavascriptExecutor js;
    private final OverlayConfig config;
    private final OverlayRootManager rootManager;

    public ScrollActions(WebDriver driver,
                         OverlayConfig config,
                         OverlayRootManager rootManager) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.rootManager = rootManager;
    }

    /**
     * Płynnie przewija do elementu z domyślnym czasem i domyślnym wyrównaniem:
     * CENTER elementu do CENTER viewportu.
     */
    public void scrollToElementWithArrow(WebElement element) {
        scrollToElementWithArrow(
                element,
                config.getDecorationDurationMs(),
                ScrollElementEdge.CENTER,
                ScrollViewportEdge.CENTER
        );
    }

    /**
     * Płynnie przewija do elementu z zadanym czasem (ms) i domyślnym wyrównaniem:
     * CENTER elementu do CENTER viewportu.
     */
    public void scrollToElementWithArrow(WebElement element, long durationMs) {
        scrollToElementWithArrow(
                element,
                durationMs,
                ScrollElementEdge.CENTER,
                ScrollViewportEdge.CENTER
        );
    }

    /**
     * Płynny scroll z pełną kontrolą:
     * - którą "krawędź" elementu bierzemy (TOP/CENTER/BOTTOM),
     * - do której części viewportu ją wyrównujemy (TOP/CENTER/BOTTOM),
     * - plus strzałka w dół/górę podczas scrolla, przeskakująca nad element po dojechaniu.
     * Metoda blokuje wykonanie do czasu końca animacji (executeAsyncScript + done()).
     */
    public void scrollToElementWithArrow(WebElement element,
                                         long durationMs,
                                         ScrollElementEdge elementEdge,
                                         ScrollViewportEdge viewportEdge) {
        if (element == null) {
            return;
        }

        if (durationMs <= 0) {
            durationMs = 800L;
        }
        if (elementEdge == null) {
            elementEdge = ScrollElementEdge.CENTER;
        }
        if (viewportEdge == null) {
            viewportEdge = ScrollViewportEdge.CENTER;
        }

        // jeśli overlay wyłączony – prosty scroll z wyrównaniem, ale nadal blokujący
        if (!config.isEnabled()) {
            js.executeAsyncScript(
                    "var el = arguments[0];" +
                            "var elemEdge = arguments[1];" +
                            "var viewEdge = arguments[2];" +
                            "var done = arguments[arguments.length - 1];" +
                            "if (!el || !el.getBoundingClientRect) { done(); return; }" +
                            "var rect = el.getBoundingClientRect();" +
                            "var startY = window.scrollY || window.pageYOffset || 0;" +
                            "var vh = window.innerHeight || document.documentElement.clientHeight || 800;" +

                            "var elemAnchor;" +
                            "if (elemEdge === 'TOP') {" +
                            "  elemAnchor = rect.top + startY;" +
                            "} else if (elemEdge === 'BOTTOM') {" +
                            "  elemAnchor = rect.bottom + startY;" +
                            "} else {" +
                            "  elemAnchor = rect.top + startY + rect.height / 2;" +
                            "}" +

                            "var viewportOffset;" +
                            "if (viewEdge === 'TOP') {" +
                            "  viewportOffset = 0;" +
                            "} else if (viewEdge === 'BOTTOM') {" +
                            "  viewportOffset = vh;" +
                            "} else {" +
                            "  viewportOffset = vh / 2;" +
                            "}" +

                            "var targetY = elemAnchor - viewportOffset;" +
                            "window.scrollTo(0, targetY);" +
                            "done();",
                    element, elementEdge.name(), viewportEdge.name()
            );
            return;
        }

        rootManager.ensureRootExists();

        js.executeAsyncScript(
                "var target = arguments[0];" +
                        "var duration = arguments[1] || 800;" +
                        "var elemEdge = arguments[2];" +
                        "var viewEdge = arguments[3];" +
                        "var done = arguments[arguments.length - 1];" +
                        "if (!target || !target.getBoundingClientRect) { done(); return; }" +
                        "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) { done(); return; }" +

                        "var existing = shadow.querySelector('#selenium-scroll-indicator');" +
                        "if (existing && existing.parentNode) { existing.parentNode.removeChild(existing); }" +

                        "var rect = target.getBoundingClientRect();" +
                        "var startY = window.scrollY || window.pageYOffset || 0;" +
                        "var vh = window.innerHeight || document.documentElement.clientHeight || 800;" +

                        "var elemAnchor;" +
                        "if (elemEdge === 'TOP') {" +
                        "  elemAnchor = rect.top + startY;" +
                        "} else if (elemEdge === 'BOTTOM') {" +
                        "  elemAnchor = rect.bottom + startY;" +
                        "} else {" +
                        "  elemAnchor = rect.top + startY + rect.height / 2;" +
                        "}" +

                        "var viewportOffset;" +
                        "if (viewEdge === 'TOP') {" +
                        "  viewportOffset = 0;" +
                        "} else if (viewEdge === 'BOTTOM') {" +
                        "  viewportOffset = vh;" +
                        "} else {" +
                        "  viewportOffset = vh / 2;" +
                        "}" +

                        "var targetY = elemAnchor - viewportOffset;" +
                        "var dirDown = targetY > startY;" +

                        "var arrow = document.createElement('div');" +
                        "arrow.id = 'selenium-scroll-indicator';" +
                        "arrow.style.position = 'fixed';" +
                        "arrow.style.width = '0';" +
                        "arrow.style.height = '0';" +
                        "arrow.style.zIndex = '2147483647';" +
                        "arrow.style.pointerEvents = 'none';" +
                        "arrow.style.borderLeft = '10px solid transparent';" +
                        "arrow.style.borderRight = '10px solid transparent';" +
                        "if (dirDown) {" +
                        "  arrow.style.borderTop = '14px solid #ffeb3b';" +
                        "  arrow.style.top = 'calc(100vh - 40px)';" +
                        "} else {" +
                        "  arrow.style.borderBottom = '14px solid #ffeb3b';" +
                        "  arrow.style.top = '40px';" +
                        "}" +
                        "arrow.style.left = '50%';" +
                        "arrow.style.transform = 'translateX(-50%)';" +
                        "shadow.appendChild(arrow);" +

                        "var startTime = null;" +
                        "function step(ts) {" +
                        "  if (!startTime) { startTime = ts; }" +
                        "  var progress = (ts - startTime) / duration;" +
                        "  if (progress > 1) { progress = 1; }" +
                        "  var y = startY + (targetY - startY) * progress;" +
                        "  window.scrollTo(0, y);" +
                        "  var current = window.scrollY || window.pageYOffset || 0;" +
                        "  if (progress >= 1 || Math.abs(current - targetY) < 2) {" +
                        "    var r = target.getBoundingClientRect();" +
                        "    var arrowY;" +
                        "    if (dirDown) {" +
                        "      arrowY = r.top - 18;" +
                        "    } else {" +
                        "      arrowY = r.bottom + 4;" +
                        "    }" +
                        "    arrow.style.top = arrowY + 'px';" +
                        "    arrow.style.left = (r.left + r.width / 2) + 'px';" +
                        "    arrow.style.transform = 'translateX(-50%)';" +
                        "    window.setTimeout(function() {" +
                        "      if (arrow && arrow.parentNode) { arrow.parentNode.removeChild(arrow); }" +
                        "      done();" +
                        "    }, 1000);" +
                        "    return;" +
                        "  }" +
                        "  window.requestAnimationFrame(step);" +
                        "}" +
                        "window.requestAnimationFrame(step);",
                element, durationMs, elementEdge.name(), viewportEdge.name()
        );
    }

}
