package io.github.mmaciekk111.uitestlens.actions;

import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.ScrollArrowJs;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;
import io.github.mmaciekk111.uitestlens.scroll.ScrollElementEdge;
import io.github.mmaciekk111.uitestlens.scroll.ScrollViewportEdge;
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
    private final OverlayLogger logger;

    public ScrollActions(WebDriver driver,
                         OverlayConfig config,
                         OverlayRootManager rootManager) {
        this(driver, config, rootManager, OverlayLogger.noop());
    }

    public ScrollActions(WebDriver driver,
                         OverlayConfig config,
                         OverlayRootManager rootManager,
                         OverlayLogger logger) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.config = config;
        this.rootManager = rootManager;
        this.logger = logger != null ? logger : OverlayLogger.noop();
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
        emitScroll(UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO, null, durationMs, elementEdge, viewportEdge, config.isEnabled());
        try {

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
            emitScroll(UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, durationMs, elementEdge, viewportEdge, false);
            return;
        }

        rootManager.ensureRootExists();

        js.executeAsyncScript(
                ScrollArrowJs.INIT +
                        "var done = arguments[arguments.length - 1];" +
                        "window.__uiTestLens.modules.scrollArrow.scrollToElementWithArrow(arguments[0], arguments[1], arguments[2], arguments[3], done);",
                element, durationMs, elementEdge.name(), viewportEdge.name()
        );
        emitScroll(UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, null, durationMs, elementEdge, viewportEdge, true);
        } catch (RuntimeException e) {
            emitScroll(UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR, e, durationMs, elementEdge, viewportEdge, config.isEnabled());
            throw e;
        }
    }

    private void emitScroll(UiTestLensStatus status,
                            UiTestLensLogLevel level,
                            Throwable throwable,
                            long durationMs,
                            ScrollElementEdge elementEdge,
                            ScrollViewportEdge viewportEdge,
                            boolean withArrow) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(status == UiTestLensStatus.FAILED ? UiTestLensEventType.ERROR : UiTestLensEventType.ACTION)
                    .status(status)
                    .message("Scroll action " + status)
                    .action("scroll")
                    .target(TargetDescriptor.none())
                    .metadata("method", "scrollToElementWithArrow")
                    .metadata("durationMs", String.valueOf(durationMs))
                    .metadata("elementEdge", elementEdge == null ? "" : elementEdge.name())
                    .metadata("viewportEdge", viewportEdge == null ? "" : viewportEdge.name())
                    .metadata("withArrow", String.valueOf(withArrow))
                    .throwable(throwable)
                    .build());
        } catch (Exception ignored) {}
    }
}
