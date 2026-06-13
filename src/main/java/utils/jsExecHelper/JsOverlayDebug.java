package utils.jsExecHelper;

import lombok.Getter;
import org.openqa.selenium.*;
import utils.jsExecHelper.actions.*;
import utils.jsExecHelper.api.ApiCallActions;
import utils.jsExecHelper.api.ApiOverlayJs;
import utils.jsExecHelper.api.ApiOverlayPanel;
import utils.jsExecHelper.core.Guards;
import utils.jsExecHelper.core.OverlayRootManager;
import utils.jsExecHelper.core.PageWaits;
import utils.jsExecHelper.core.PopupDetector;
import utils.jsExecHelper.hud.HudPanel;
import utils.jsExecHelper.react.ReactSafeExecutor;
import utils.jsExecHelper.scroll.ScrollElementEdge;
import utils.jsExecHelper.scroll.ScrollViewportEdge;


import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;



public final class JsOverlayDebug {

    @Getter
    private final WebDriver driver;
    @Getter
    private final OverlayConfig config;

    private final OverlayRootManager rootManager;
    private final HighlightActions highlightActions;
    private final TypingActions typingActions;
    private final HudPanel hudPanel;
    private final PageWaits pageWaits;
    private final PopupDetector popupDetector;
    private final SmartClickActions smartClickActions;
    private final SmartInputActions smartInputActions;
    private final ScrollActions scrollActions;
    private final AssertActions assertActions;
    private final TargetResolverActions targetResolverActions;
    private ReactSafeExecutor reactSafeExecutor;
    private final ApiOverlayPanel apiPanel;
    private final ApiCallActions apiCalls;
    private boolean waitHudInjected = false;
    private final Guards guards;

    // ======================================================================
    //  CTOR
    // ======================================================================



    public JsOverlayDebug(WebDriver driver, OverlayConfig config, ApiOverlayPanel apiPanel, ApiCallActions apiCalls, Guards guards) {
        this.apiPanel = apiPanel;
        this.apiCalls = apiCalls;
        this.guards = guards;
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.driver = driver;
        this.config = config;

        this.rootManager = new OverlayRootManager(driver, config);
        this.highlightActions = new HighlightActions(driver, rootManager, config);
        this.typingActions = new TypingActions(driver, rootManager, config);
        this.smartClickActions = new SmartClickActions(driver, config, rootManager, highlightActions);
        this.smartInputActions = new SmartInputActions(driver, config, rootManager, typingActions);
        this.hudPanel = new HudPanel(driver, rootManager, config);
        this.pageWaits = new PageWaits(driver, config);
        this.popupDetector = new PopupDetector(driver, config, rootManager, highlightActions);
        this.scrollActions = new ScrollActions(driver, config, rootManager);
        this.assertActions = new AssertActions(driver, rootManager, config, hudPanel);
        this.targetResolverActions = new TargetResolverActions(driver);
    }

    // ======================================================================
    //  HUD
    // ======================================================================

    /** Initializes the HUD with test name and pipeline ID. */
    public void initHud(String testName, String pipelineId) {
        hudPanel.init(testName, pipelineId);
    }

    /** Updates the description of the current step in the HUD. */
    public void setStep(String stepDescription) {
        hudPanel.updateStep(stepDescription);
    }

    public void hudLog(String level, String message, String timestamp) {
        hudPanel.appendLog(level, message, timestamp);
    }

    // ======================================================================
    //  HIGHLIGHT
    // ======================================================================

    /** Draws a click decoration around an element (border + label). */
    public void highlightClick(WebElement element, String label) {
        highlightActions.highlightClick(element, label);
    }

    /** Alias for highlightClick – “highlight the element” without clicking. */
    public WebElement highlightElement(WebElement element, String label) {
        highlightActions.highlightClick(element, label);
        return element;
    }

    /** Draws a border around the direct parent of the given element. */
    public void highlightParent(WebElement element, String label) {
        highlightActions.highlightParent(element, 1, label);
    }

    /** Draws a border around an ancestor N levels up. */
    public void highlightAncestor(WebElement element, int levelsUp, String label) {
        highlightActions.highlightParent(element, levelsUp, label);
    }

    /** Draws a border around the closest ancestor matching a CSS selector. */
    public void highlightClosest(WebElement element, String cssSelector, String label) {
        highlightActions.highlightClosest(element, cssSelector, label);
    }

    /** Common case: decoration + classic click(). */
    public void highlightThenClick(WebElement element, String label) {
        highlightActions.highlightClick(element, label);
    }

    // ======================================================================
    //  INPUT / TYPING
    // ======================================================================

    /** Types text + shows a tooltip with information about the set value. */
    public void typeWithHint(WebElement element, String value) {
        typingActions.typeWithHint(element, value);
    }

    public void clearAndType(WebElement element, String value) {
        typingActions.clearAndType(element, value);
    }

    /** Basic smart type (without highlight). */
    public void smartTypeWithHint(WebElement element, String value) {
        smartInputActions.smartTypeWithHint(element, value, "OVERLAY");
    }

    /** Smart typing + highlight with default label “SET”. */
    public void smartTypeWithHintHighlighted(WebElement element, String value) {
        smartTypeWithHintHighlighted(element, value, "SET");
    }

    /** Smart typing + highlight with a custom label (e.g. “EMAIL”, “PASSWORD”). */
    public void smartTypeWithHintHighlighted(WebElement element, String value, String label) {
        String effectiveLabel = (label == null || label.isBlank()) ? "SET" : label;
        highlightActions.highlightClick(element, effectiveLabel);
        smartInputActions.smartTypeWithHint(element, value, effectiveLabel);
    }

    // ======================================================================
    //  CLICK
    // ======================================================================

    /**
     * Smart click – tries to close global overlays/popups first and then clicks.
     */
    public void smartClickWithOverlayHandler(WebElement element, String label) {
        smartClickActions.clickWithOverlayHandling(element, label);
    }

    public void smartClickReactSafe(By locator, String label) {
        smartClickActions.clickReactSafe(locator, label, reactSafe());
    }

    public WebElement reactSafeFindBySelectorContainingText(String selector,
                                                            String text,
                                                            boolean visibleOnly,
                                                            String description) {
        By by = toBy(selector);

        String needle = text == null ? "" : text.trim();

        return reactSafeFindFirst(by, el -> {
            if (visibleOnly && !el.isDisplayed()) return false;

            // getText() bywa kapryśny w React/Chakra (np. text w spanach), więc bierzemy textContent
            String tc = safeTextContent(el);
            return tc != null && tc.contains(needle);
        }, description + " | selector=" + selector + " | text=" + needle);
    }

    private String textContent(WebElement el) {
        try {
            Object v = ((JavascriptExecutor) driver).executeScript(
                    "return (arguments[0] && (arguments[0].textContent || arguments[0].innerText)) || '';",
                    el
            );
            return v == null ? "" : String.valueOf(v).trim();
        } catch (StaleElementReferenceException e) {
            throw e; // wymuś retry w reactSafe
        }
    }

    private By toBy(String selectorRaw) {
        String s = selectorRaw == null ? "" : selectorRaw.trim();

        // obsłuż prefixy (opcjonalnie)
        if (s.toLowerCase().startsWith("xpath=") || s.toLowerCase().startsWith("xpath:")) {
            s = s.replaceFirst("(?i)^xpath\\s*[:=]\\s*", "");
            return By.xpath(s);
        }
        if (s.toLowerCase().startsWith("css=") || s.toLowerCase().startsWith("css:")) {
            s = s.replaceFirst("(?i)^css\\s*[:=]\\s*", "");
            return By.cssSelector(s);
        }

        // heurystyka: XPath zwykle zaczyna się od /, ./ albo (
        if (s.startsWith("/") || s.startsWith("./") || s.startsWith("(")) {
            return By.xpath(s);
        }
        return By.cssSelector(s);
    }

    private String safeTextContent(WebElement el) {
        try {
            Object v = ((JavascriptExecutor) driver).executeScript(
                    "return (arguments[0] && (arguments[0].textContent || arguments[0].innerText)) || '';", el
            );
            return v == null ? "" : String.valueOf(v).trim();
        } catch (StaleElementReferenceException e) {
            throw e; // wymuś retry w reactSafeFindFirst
        }
    }


    public WebElement reactSafeFindFirst(By listLocator,
                                         java.util.function.Predicate<WebElement> predicate,
                                         String description) {
        // retry wrapper na samą listę (By) – dzięki temu nie trzymasz starej listy WebElementów
        return reactSafe().doWithRetry(listLocator, "FIND_FIRST: " + description, anyElFromList -> {
            // anyElFromList jest tylko “kotwicą” żeby mieć presence – realnie bierzemy świeżą listę
            List<WebElement> list = driver.findElements(listLocator);

            for (WebElement el : list) {
                try {
                    if (predicate.test(el)) {
                        // opcjonalnie: highlight znalezioną sekcję
                        if (config.isEnabled() && config.isShowHudPanel()) {
                            highlightElement(el, "FOUND");
                        }
                        return el;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // element “umarł” w trakcie predicate -> pomiń i szukaj dalej w tej próbie
                }
            }

            throw new NoSuchElementException("No match in list for: " + description + " | locator=" + listLocator);
        });
    }
    public List<WebElement> reactSafeFindChildren(By parentListLocator,
                                                  java.util.function.Predicate<WebElement> parentPredicate,
                                                  By childLocator,
                                                  String description) {
        return reactSafe().doWithRetry(parentListLocator, "FIND_CHILDREN: " + description, anyElFromList -> {
            WebElement parent = reactSafeFindFirst(parentListLocator, parentPredicate, description + " (parent)");
            try {
                return parent.findElements(childLocator);
            } catch (StaleElementReferenceException e) {
                // parent się zestarzał pomiędzy znalezieniem a findElements -> wymuś retry
                throw e;
            }
        });
    }
    public WebElement reactSafeFindChildByText(By parentListLocator,
                                               java.util.function.Predicate<WebElement> parentPredicate,
                                               By childLocator,
                                               java.util.function.Predicate<WebElement> childPredicate,
                                               String description) {

        return reactSafe().doWithRetry(parentListLocator, description, ignored -> {
            WebElement parent = reactSafeFindFirst(
                    parentListLocator,
                    parentPredicate,
                    description + " (parent)"
            );

            List<WebElement> children = parent.findElements(childLocator);
            for (WebElement child : children) {
                try {
                    if (childPredicate.test(child)) {
                        highlightElement(child, "FOUND");
                        return child;
                    }
                } catch (StaleElementReferenceException ignored2) {
                    // React zdążył – retry całej operacji
                    throw ignored2;
                }
            }

            throw new NoSuchElementException("Child not found: " + description);
        });
    }

    public WebElement reactSafeFindChildByTextThenFind(By parentListLocator,
                                                       java.util.function.Predicate<WebElement> parentPredicate,
                                                       By childLocator,
                                                       java.util.function.Predicate<WebElement> childPredicate,
                                                       By innerLocator,
                                                       String description) {

        return reactSafe().doWithRetry(parentListLocator, description, ignored -> {
            WebElement parent = reactSafeFindFirst(parentListLocator, parentPredicate, description + " (parent)");

            List<WebElement> children = parent.findElements(childLocator);
            for (WebElement child : children) {
                try {
                    if (childPredicate.test(child)) {
                        WebElement inner = child.findElement(innerLocator);
                        highlightElement(inner, "FOUND");
                        return inner;
                    }
                } catch (org.openqa.selenium.StaleElementReferenceException e) {
                    throw e; // retry całej operacji
                } catch (org.openqa.selenium.NoSuchElementException e) {
                    // inner nie ma – szukamy dalej / finalnie rzucimy NoSuchElementException
                }
            }

            throw new org.openqa.selenium.NoSuchElementException("Inner element not found: " + description);
        });
    }

    // ======================================================================
    //  CLEANUP
    // ======================================================================

    /** Clears all overlay elements (HUD, borders, tooltips). */
    public void clearDebugArtifacts() {
        rootManager.clearAll();
    }

    // ======================================================================
    //  PAGE WAITS + HUD
    // ======================================================================

    /** Waits until document.readyState == 'complete' and logs the info into HUD. */
    public void waitForPageReady() {
        pageWaits.waitForDocumentReady();
        showLastWaitInHud();
    }

    public void waitForPageReady(Duration timeout) {
        pageWaits.waitForDocumentReady(timeout);
        showLastWaitInHud();
    }

    /** Waits for "network idle" and logs the info into HUD. */
    public void waitForNetworkIdle() {
        pageWaits.waitForNetworkIdle();
        showLastWaitInHud();
    }

    public void waitForNetworkIdle(Duration idleDuration, Duration timeout) {
        pageWaits.waitForNetworkIdle(idleDuration, timeout);
        showLastWaitInHud();
    }

    /** Waits until document.readyState is 'interactive' or 'complete' and logs the info into HUD. */
    public void waitForInteractiveOrComplete() {
        pageWaits.waitForInteractiveOrComplete();
        showLastWaitInHud();
    }

    public void waitForInteractiveOrComplete(Duration timeout) {
        pageWaits.waitForInteractiveOrComplete(timeout);
        showLastWaitInHud();
    }

    // 1) wstrzyknięcie dwóch plików JS (raz na sesję/stronę)

    public void ensureWaitHudInjected() {
        try {
            Object present = ((JavascriptExecutor) driver).executeScript(
                    "return !!(window.__seleniumWaitHud && window.__seleniumWaitHud.start && window.__seleniumWaitHud.stop);"
            );
            if (present instanceof Boolean && (Boolean) present) {
                return;
            }
        } catch (Exception ignored) {}

        String jsCode = loadResource("/selenium/wait/WaitHud.js");
        ((JavascriptExecutor) driver).executeScript(jsCode);
    }


    public void waitHudStart(String label) {
        try {
            ensureWaitHudInjected();
            ((JavascriptExecutor) driver).executeScript(
                    "window.__seleniumWaitHud && window.__seleniumWaitHud.start && window.__seleniumWaitHud.start(arguments[0]);",
                    label
            );
        } catch (Exception ignored) {}
    }

    public void waitHudStop(String prefix, long elapsedMs) {
        try {
            ensureWaitHudInjected();
            ((JavascriptExecutor) driver).executeScript(
                    "return window.__seleniumWaitHud && window.__seleniumWaitHud.stop"
                            + " ? window.__seleniumWaitHud.stop(arguments[0], arguments[1]) : null;",
                    prefix, elapsedMs
            );
            Object v = ((JavascriptExecutor) driver).executeScript("return window.__seleniumLastWaitElapsedMs;");
            logWraper.infoLog("Last wait elapsed (ms) = " + v);
        } catch (Exception ignored) {}
    }

    public void forceHideWaitHud() {
        try {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    "try {" +
                            "  var shadow = window.__seleniumOverlayRoot;" +
                            "  if (shadow) {" +
                            "    var el = shadow.querySelector('#selenium-wait-indicator, #selenium-wait-hud, [data-selenium-wait=\"1\"]');" +
                            "    if (el && el.parentNode) el.parentNode.removeChild(el);" +
                            "  }" +
                            "  var el2 = document.querySelector('#selenium-wait-indicator, #selenium-wait-hud, [data-selenium-wait=\"1\"]');" +
                            "  if (el2 && el2.parentNode) el2.parentNode.removeChild(el2);" +
                            "} catch(e) {}"
            );
        } catch (Exception ignored) {}
    }

    private String loadResource(String path) {
        try (var is = getClass().getResourceAsStream(path)) {
            if (is == null) throw new IllegalStateException("Missing resource: " + path);
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource " + path, e);
        }
    }







    // ======================================================================
    //  WAIT INDICATOR (klepsydra)
    // ======================================================================

    /** Pokazuje prosty indykator "czekam" w overlay (klepsydra). */
    public void showWaitIndicator(String label) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var shadow = window.__seleniumOverlayRoot;" +
                            "if (!shadow) { return; }" +
                            "var existing = shadow.querySelector('#selenium-wait-indicator');" +
                            "if (!existing) {" +
                            "  existing = document.createElement('div');" +
                            "  existing.id = 'selenium-wait-indicator';" +
                            "  existing.style.position = 'fixed';" +
                            "  existing.style.bottom = '8px';" +
                            "  existing.style.left = '50%';" +
                            "  existing.style.transform = 'translateX(-50%)';" +
                            "  existing.style.padding = '4px 10px';" +
                            "  existing.style.background = 'rgba(0,0,0,0.8)';" +
                            "  existing.style.color = '#ffffff';" +
                            "  existing.style.fontSize = '11px';" +
                            "  existing.style.borderRadius = '12px';" +
                            "  existing.style.zIndex = '2147483647';" +
                            "  existing.style.pointerEvents = 'none';" +
                            "  shadow.appendChild(existing);" +
                            "}" +
                            "existing.textContent = '⏳ ' + (arguments[0] || 'Waiting...');",
                    label
            );
        } catch (Exception ignored) {
            // HUD nie powinien wysadzać testów
        }
    }

    /** Chowa indykator "czekam". */
    public void hideWaitIndicator() {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var shadow = window.__seleniumOverlayRoot;" +
                            "if (!shadow) { return; }" +
                            "var existing = shadow.querySelector('#selenium-wait-indicator');" +
                            "if (existing && existing.parentNode) {" +
                            "  existing.parentNode.removeChild(existing);" +
                            "}"
            );
        } catch (Exception ignored) {
            // HUD nie powinien wysadzać testów
        }
    }

    // ======================================================================
    //  REACT / SPA HELPERS
    // ======================================================================

    public void waitForReactRootMounted(By rootLocator) {
        pageWaits.waitForReactRootMounted(rootLocator);
        showLastWaitInHud();
    }

    public void waitForReactRootMounted(By rootLocator, Duration timeout) {
        pageWaits.waitForReactRootMounted(rootLocator, timeout);
        showLastWaitInHud();
    }

    public void waitForSpaDomStableUnder(By rootLocator) {
        pageWaits.waitForSpaDomStableUnder(rootLocator);
        showLastWaitInHud();
    }

    public void waitForSpaDomStableUnder(By rootLocator,
                                         Duration idleDuration,
                                         Duration timeout) {
        pageWaits.waitForSpaDomStableUnder(rootLocator, idleDuration, timeout);
        showLastWaitInHud();
    }

    public WebElement waitForReactComponentVisible(By rootLocator, By componentLocator) {
        WebElement el = pageWaits.waitForReactComponentVisible(rootLocator, componentLocator);
        showLastWaitInHud();
        return el;
    }

    public WebElement waitForReactComponentVisible(By rootLocator,
                                                   By componentLocator,
                                                   Duration timeout) {
        WebElement el = pageWaits.waitForReactComponentVisible(rootLocator, componentLocator, timeout);
        showLastWaitInHud();
        return el;
    }

    public void waitForReactAndNetworkIdle(By rootLocator) {
        pageWaits.waitForReactAndNetworkIdle(rootLocator);
        showLastWaitInHud();
    }

    public void waitForReactAndNetworkIdle(By rootLocator, Duration timeout) {
        pageWaits.waitForReactAndNetworkIdle(rootLocator, timeout);
        showLastWaitInHud();
    }

    /** Wpisuje ostatni komunikat z PageWaits do HUD. */
    public void showLastWaitInHud() {
        // 1) HUD – best effort (nie może wysadzić testu)
        try {
            ((JavascriptExecutor) driver).executeScript(
                    "var shadow = window.__seleniumOverlayRoot;" +
                            "if (!shadow) { return; }" +
                            "var step = shadow.querySelector('#selenium-hud-step');" +
                            "if (!step) { return; }" +
                            "var msg = window.__seleniumLastWaitMessage || '';" +
                            "if (!msg) {" +
                            "  step.innerHTML = '<b>Step:</b> -';" +
                            "} else {" +
                            "  step.innerHTML = '<b>Step:</b> ' + msg;" +
                            "}"
            );
        } catch (Exception ignored) {
            // HUD ma być “best effort”
        }

        // 2) Guards – niezależnie od HUD
        try {
            guards.checkpoint("showLastWaitInHud");
        } catch (AssertionError ae) {
            // guard ma prawo przerwać test
            throw ae;
        } catch (Exception ignored) {
            // jeśli guards ma jakieś wyjątki runtime/selenium, nie wysadzaj z tego miejsca
            // (checkpoint i tak ma failFast => AssertionError, reszta to noise)
        }
    }

    // ======================================================================
    //  POPUPS
    // ======================================================================

    public java.util.Optional<WebElement> detectPopup() {
        return popupDetector.findTopMostPopup();
    }

    /** Detects a popup and, if present, highlights it with an overlay. */
    public boolean highlightPopupIfPresent(String label) {
        return popupDetector.highlightPopupIfPresent(label);
    }

    /** Shorthand version with default label "POPUP". */
    public boolean highlightPopupIfPresent() {
        return popupDetector.highlightPopupIfPresent("POPUP");
    }

    /**
     * Attempts to close a popup/overlay:
     * - first using globalOverlayCloseButtonSelector,
     * - then heuristically.
     */
    public boolean closePopupIfPresent(String overlayLabel, String closeButtonLabel) {
        return popupDetector.closePopupIfPresent(overlayLabel, closeButtonLabel);
    }

    public boolean closePopupIfPresent() {
        return popupDetector.closePopupIfPresent();
    }

    // ======================================================================
    //  SCROLL – scrolling with visual arrow
    // ======================================================================

    /** Smoothly scrolls to the element (CENTER to CENTER) with an arrow. */
    public void scrollToElementWithArrow(WebElement element) {
        scrollActions.scrollToElementWithArrow(element);
    }

    /** Smoothly scrolls to the element in given time with default alignment and an arrow. */
    public void scrollToElementWithArrow(WebElement element, long durationMs) {
        scrollActions.scrollToElementWithArrow(element, durationMs);
    }

    /** Smooth scrolling with full control (time, element edge, viewport edge) + arrow. */
    public void scrollToElementWithArrow(WebElement element,
                                         long durationMs,
                                         ScrollElementEdge elementEdge,
                                         ScrollViewportEdge viewportEdge) {
        scrollActions.scrollToElementWithArrow(element, durationMs, elementEdge, viewportEdge);
    }

    /** Smooth scrolling with custom edges, duration taken from config. */
    public void scrollToElementWithArrow(WebElement element,
                                         ScrollElementEdge elementEdge,
                                         ScrollViewportEdge viewportEdge) {
        scrollActions.scrollToElementWithArrow(
                element,
                config.getDecorationDurationMs(),
                elementEdge,
                viewportEdge
        );
    }

    // ======================================================================
    //  ASSERTIONS – SINGLE (top-level helpers)
    // ======================================================================

    /** Text equals on element. */
    public boolean assertTextEquals(WebElement element,
                                    String expected,
                                    String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertTextEquals(element, expected, contextLabel);
        return r.isSuccess();
    }

    /** Text contains on element (getText().contains). */
    public boolean assertTextContains(WebElement element,
                                      String expectedSubstring,
                                      String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertTextContains(element, expectedSubstring, contextLabel);
        return r.isSuccess();
    }

    /** HTML attribute equals. */
    public boolean assertAttributeEquals(WebElement element,
                                         String attributeName,
                                         String expected,
                                         String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertAttributeEquals(element, attributeName, expected, contextLabel);
        return r.isSuccess();
    }

    /** CSS property equals. */
    public boolean assertCssEquals(WebElement element,
                                   String cssProperty,
                                   String expected,
                                   String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertCssEquals(element, cssProperty, expected, contextLabel);
        return r.isSuccess();
    }

    /** Color equals (normalized to #rrggbb). */
    public boolean assertColorEquals(WebElement element,
                                     String cssProperty,
                                     String expectedColor,
                                     String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertColorEquals(element, cssProperty, expectedColor, contextLabel);
        return r.isSuccess();
    }

    /** Class presence. */
    public boolean assertHasClass(WebElement element,
                                  String className,
                                  boolean expectedPresent,
                                  String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertHasClass(element, className, expectedPresent, contextLabel);
        return r.isSuccess();
    }

    /** Visibility. */
    public boolean assertVisible(WebElement element,
                                 boolean expectedVisible,
                                 String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertVisible(element, expectedVisible, contextLabel);
        return r.isSuccess();
    }

    /** Enabled state. */
    public boolean assertEnabled(WebElement element,
                                 boolean expectedEnabled,
                                 String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertEnabled(element, expectedEnabled, contextLabel);
        return r.isSuccess();
    }

    /** Selected state. */
    public boolean assertSelected(WebElement element,
                                  boolean expectedSelected,
                                  String contextLabel) {
        AssertActions.OverlayAssertionResult r =
                assertActions.assertSelected(element, expectedSelected, contextLabel);
        return r.isSuccess();
    }

    // ======================================================================
    //  ASSERTIONS – GROUPED (SoftAssertions)
    // ======================================================================

    public static final class AssertionSummary {

        public enum AssertionTextFormat {
            MESSAGE, // human readable string (toMessage())
            JSON     // JSON string (toJson())
        }

        private final String groupName;
        private final List<AssertActions.OverlayAssertionResult> results = new ArrayList<>();

        public AssertionSummary(String groupName) {
            this.groupName = groupName;
        }

        void addResult(AssertActions.OverlayAssertionResult result) {
            if (result != null) {
                results.add(result);
            }
        }

        /** All assertions (OK + FAIL) as objects. */
        public List<AssertActions.OverlayAssertionResult> getAllResults() {
            return Collections.unmodifiableList(results);
        }

        /** Only failed assertions as objects. */
        public List<AssertActions.OverlayAssertionResult> getFailuresObjects() {
            List<AssertActions.OverlayAssertionResult> fails = new ArrayList<>();
            for (AssertActions.OverlayAssertionResult r : results) {
                if (!r.isSuccess()) {
                    fails.add(r);
                }
            }
            return Collections.unmodifiableList(fails);
        }

        public boolean hasFailures() {
            for (AssertActions.OverlayAssertionResult r : results) {
                if (!r.isSuccess()) {
                    return true;
                }
            }
            return false;
        }

        public String getGroupName() {
            return groupName;
        }

        /** All assertions as strings (MESSAGE or JSON). */
        public List<String> getAll(AssertionTextFormat format) {
            List<String> out = new ArrayList<>();
            for (AssertActions.OverlayAssertionResult r : results) {
                out.add(format == AssertionTextFormat.JSON ? r.toJson() : r.toMessage());
            }
            return Collections.unmodifiableList(out);
        }

        /** Only failures as strings (MESSAGE or JSON). */
        public List<String> getFailures(AssertionTextFormat format) {
            List<String> out = new ArrayList<>();
            for (AssertActions.OverlayAssertionResult r : results) {
                if (!r.isSuccess()) {
                    out.add(format == AssertionTextFormat.JSON ? r.toJson() : r.toMessage());
                }
            }
            return Collections.unmodifiableList(out);
        }

        /** Text for AssertionError – uses only failures in MESSAGE format. */
        public String formatForException() {
            StringBuilder sb = new StringBuilder();
            sb.append("Overlay assertions failed");
            if (groupName != null && !groupName.isBlank()) {
                sb.append(" (").append(groupName).append(")");
            }
            sb.append(":\n");
            for (String msg : getFailures(AssertionTextFormat.MESSAGE)) {
                sb.append(" - ").append(msg).append("\n");
            }
            return sb.toString();
        }
    }

    public static final class SoftAssertions {
        private final AssertActions actions;
        private final AssertionSummary summary;
        private final ReactSafeExecutor reactSafe; // może być null

        private SoftAssertions(AssertActions actions,
                               AssertionSummary summary,
                               ReactSafeExecutor reactSafe) {
            this.actions = actions;
            this.summary = summary;
            this.reactSafe = reactSafe;
        }

        // ===== helper =====

        private void ensureReactSafe(String what) {
            if (reactSafe == null) {
                throw new IllegalStateException(
                        "SoftAssertions: " + what + " wymaga ReactSafeExecutor. " +
                                "Użyj JsOverlayDebug.assertGroupReactSafe(...) zamiast assertGroup(...)."
                );
            }
        }

        // =================================================================
        //  ELEMENT-BASED ASSERTIONS (WebElement)
        // =================================================================

        /** BACKWARD-COMPAT: stara nazwa, alias do equals(element, ...) */
        public boolean textEquals(WebElement element,
                                  String expected,
                                  String contextLabel) {
            return equals(element, expected, contextLabel);
        }

        /** Tekst elementu (getText) == expected. */
        public boolean equals(WebElement element,
                              String expected,
                              java.util.function.Function<String, String> modifier,
                              String contextLabel) {
            var r = actions.assertTextEqualsModified(element, expected, modifier, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean contains(WebElement element,
                                String expectedSubstring,
                                java.util.function.Function<String, String> modifier,
                                String contextLabel) {
            var r = actions.assertTextContainsModified(element, expectedSubstring, modifier, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }


        public boolean attributeEquals(WebElement element,
                                       String attributeName,
                                       String expected,
                                       String contextLabel) {
            var r = actions.assertAttributeEquals(element, attributeName, expected, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean cssEquals(WebElement element,
                                 String cssProperty,
                                 String expected,
                                 String contextLabel) {
            var r = actions.assertCssEquals(element, cssProperty, expected, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean colorEquals(WebElement element,
                                   String cssProperty,
                                   String expectedColor,
                                   String contextLabel) {
            var r = actions.assertColorEquals(element, cssProperty, expectedColor, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean hasClass(WebElement element,
                                String className,
                                boolean expectedPresent,
                                String contextLabel) {
            var r = actions.assertHasClass(element, className, expectedPresent, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean isVisible(WebElement element,
                                 boolean expectedVisible,
                                 String contextLabel) {
            var r = actions.assertVisible(element, expectedVisible, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean isEnabled(WebElement element,
                                 boolean expectedEnabled,
                                 String contextLabel) {
            var r = actions.assertEnabled(element, expectedEnabled, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean isSelected(WebElement element,
                                  boolean expectedSelected,
                                  String contextLabel) {
            var r = actions.assertSelected(element, expectedSelected, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        /**
         * Element-based CONTAINS:
         * - pobiera element.getText()
         * - AssertActions.assertTextContains(...) robi HUD log + RAMKA + BADGE (stackowane)
         */
        public boolean contains(WebElement element,
                                String expectedSubstring,
                                String contextLabel) {
            var r = actions.assertTextContains(element, expectedSubstring, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        // =================================================================
        //  REACT-SAFE ASSERTIONS (By + ReactSafeExecutor)
        // =================================================================

        /** Tekst elementu znalezionego przez locator == expected. */
        public boolean equals(By locator,
                              String expected,
                              String contextLabel) {
            ensureReactSafe("equals(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT TEXT_EQUALS: " + contextLabel,
                    el -> {
                        var r = actions.assertTextEquals(el, expected, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        public boolean attributeEquals(By locator,
                                       String attributeName,
                                       String expected,
                                       String contextLabel) {
            ensureReactSafe("attributeEquals(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT ATTR_EQUALS(" + attributeName + "): " + contextLabel,
                    el -> {
                        var r = actions.assertAttributeEquals(el, attributeName, expected, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        public boolean cssEquals(By locator,
                                 String cssProperty,
                                 String expected,
                                 String contextLabel) {
            ensureReactSafe("cssEquals(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT CSS_EQUALS(" + cssProperty + "): " + contextLabel,
                    el -> {
                        var r = actions.assertCssEquals(el, cssProperty, expected, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        public boolean colorEquals(By locator,
                                   String cssProperty,
                                   String expectedColor,
                                   String contextLabel) {
            ensureReactSafe("colorEquals(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT COLOR_EQUALS(" + cssProperty + "): " + contextLabel,
                    el -> {
                        var r = actions.assertColorEquals(el, cssProperty, expectedColor, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        public boolean hasClass(By locator,
                                String className,
                                boolean expectedPresent,
                                String contextLabel) {
            ensureReactSafe("hasClass(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT HAS_CLASS(" + className + "): " + contextLabel,
                    el -> {
                        var r = actions.assertHasClass(el, className, expectedPresent, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        public boolean isVisible(By locator,
                                 boolean expectedVisible,
                                 String contextLabel) {
            ensureReactSafe("isVisible(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT VISIBLE: " + contextLabel,
                    el -> {
                        var r = actions.assertVisible(el, expectedVisible, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        public boolean isEnabled(By locator,
                                 boolean expectedEnabled,
                                 String contextLabel) {
            ensureReactSafe("isEnabled(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT ENABLED: " + contextLabel,
                    el -> {
                        var r = actions.assertEnabled(el, expectedEnabled, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        public boolean isSelected(By locator,
                                  boolean expectedSelected,
                                  String contextLabel) {
            ensureReactSafe("isSelected(By, ...)");
            Boolean result = reactSafe.doWithRetry(
                    locator,
                    "ASSERT SELECTED: " + contextLabel,
                    el -> {
                        var r = actions.assertSelected(el, expectedSelected, contextLabel);
                        summary.addResult(r);
                        return r.isSuccess();
                    }
            );
            return result != null && result;
        }

        // =================================================================
        //  GENERIC / VALUE-BASED ASSERTIONS (bez WebElement, bez locatora)
        // =================================================================

        /** Generic equals (bez elementu). */
        public boolean equals(Object actual,
                              Object expected,
                              String contextLabel) {
            var r = actions.assertEquals(expected, actual, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean notEquals(Object actual,
                                 Object expected,
                                 String contextLabel) {
            var r = actions.assertNotEquals(expected, actual, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean contains(String actual,
                                String expectedSubstring,
                                String contextLabel) {
            var r = actions.assertContains(actual, expectedSubstring, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean notContains(String actual,
                                   String expectedSubstring,
                                   String contextLabel) {
            var r = actions.assertNotContains(actual, expectedSubstring, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean isTrue(boolean condition,
                              String contextLabel) {
            var r = actions.assertTrue(condition, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }

        public boolean isFalse(boolean condition,
                               String contextLabel) {
            var r = actions.assertFalse(condition, contextLabel);
            summary.addResult(r);
            return r.isSuccess();
        }
    }

    /**
     * Creates a group of soft assertions:
     * - consumer receives SoftAssertions and can perform many assert* calls,
     * - results are written to AssertionSummary,
     * - if failTestOnErrors == true and there are failures -> throws AssertionError.
     */
    public AssertionSummary assertGroup(String groupName,
                                        Consumer<SoftAssertions> consumer,
                                        boolean failTestOnErrors) {
        AssertionSummary summary = new AssertionSummary(groupName);
        SoftAssertions soft = new SoftAssertions(assertActions, summary, null);

        consumer.accept(soft);

        if (failTestOnErrors && summary.hasFailures()) {
            throw new AssertionError(summary.formatForException());
        }

        return summary;
    }

    /**
     * Creates a group of soft assertions with ReactSafeExecutor:
     * - asercje na By będą wykonywane z retry (StaleElement, NoSuchElement itp.),
     * - asercje na WebElement działają normalnie.
     */
    public AssertionSummary assertGroupReactSafe(String groupName,
                                                 ReactSafeExecutor reactSafeExecutor,
                                                 Consumer<SoftAssertions> consumer,
                                                 boolean failTestOnErrors) {
        AssertionSummary summary = new AssertionSummary(groupName);
        SoftAssertions soft = new SoftAssertions(assertActions, summary, reactSafeExecutor);

        consumer.accept(soft);

        if (failTestOnErrors && summary.hasFailures()) {
            throw new AssertionError(summary.formatForException());
        }

        return summary;
    }

    // ======================================================================
    //  TARGET RESOLVER – RETURNS WEBELEMENT OR SELECTOR
    // ======================================================================

    /** Returns a “reasonable” click target for the given element. */
    public WebElement resolveClickTarget(WebElement element) {
        return targetResolverActions.resolveClickTarget(element);
    }

    /** Returns input[type=file] associated with the given element (may return null). */
    public WebElement resolveFileInputTarget(WebElement element) {
        return targetResolverActions.resolveFileInputTarget(element);
    }

    /** Returns CSS selector of the click target (e.g. button#save.btn.btn-primary). */
    public String resolveClickTargetCssSelector(WebElement element) {
        return targetResolverActions.resolveClickTargetSelector(element);
    }

    /** Returns CSS selector of the associated input[type=file]. */
    public String resolveFileInputCssSelector(WebElement element) {
        return targetResolverActions.resolveFileInputSelector(element);
    }

    /**
     * Convenience action:
     * - find the click target based on container/label,
     * - highlight the target element,
     * - smartClick it (with popup handling, etc.).
     */
    public void smartClickResolved(WebElement containerOrLabel, String label) {
        WebElement target = resolveClickTarget(containerOrLabel);
        if (target == null) {
            if (config.isShowHudPanel()) {
                hudPanel.updateStep("smartClickResolved: no clickable target found");
            }
            return;
        }
        highlightActions.highlightClick(target, label);
        smartClickActions.smartClick(target, label);
    }

    /**
     * Convenience action for file upload:
     * - find input[type=file] associated with the given element,
     * - highlight it,
     * - send file path via sendKeys(path).
     */
    public void smartUploadFile(WebElement containerOrLabel, String absoluteFilePath) {
        WebElement fileInput = resolveFileInputTarget(containerOrLabel);
        if (fileInput == null) {
            if (config.isShowHudPanel()) {
                hudPanel.updateStep("smartUploadFile: file input not found for given element");
            }
            return;
        }
        highlightActions.highlightClick(fileInput, "UPLOAD");
        fileInput.sendKeys(absoluteFilePath);
        if (config.isShowHudPanel()) {
            hudPanel.updateStep("File sent to <input type='file'>");
        }
    }

    // ======================================================================
    //  API shooter
    // ======================================================================
    public void showApiCall(String title, String method, String url, String payloadPreview, long timeoutMs) {
        String id = apiPanel.showRequest(title, method, url, payloadPreview);
        apiPanel.setPending(id, timeoutMs);
    }

    public void showApiResponse(String requestId, int status, long durationMs, String headersPreview, String bodyPreview) {
        apiPanel.setResponse(requestId, status, durationMs, headersPreview, bodyPreview);
    }

    public void hideApiModal() {
        apiPanel.hide();
    }
    public <T> T apiCallWithModal(String title,
                                  String method,
                                  String url,
                                  String payloadPreview,
                                  long timeoutMs,
                                  java.util.concurrent.Callable<T> call,
                                  java.util.function.Function<T, String> responsePreview) {
        return apiCalls.callWithModal(title, method, url, payloadPreview, timeoutMs, call, responsePreview);
    }
    public String apiShowRequest(String title, String method, String url, String payloadPreview) {
        try {
            return (String) ((JavascriptExecutor) driver).executeScript(
                    ApiOverlayJs.INIT_MODAL +
                            "return window.__seleniumApiModal.showRequest(arguments[0], arguments[1], arguments[2], arguments[3]);",
                    title, method, url, payloadPreview
            );
        } catch (Exception e) {
            return null;
        }
    }

    public void apiSetPending(String reqId, long timeoutMs) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    ApiOverlayJs.INIT_MODAL +
                            "window.__seleniumApiModal.setPending(arguments[0], arguments[1]);",
                    reqId, timeoutMs
            );
        } catch (Exception ignored) {}
    }

    public void apiSetResponse(String reqId, int status, long durationMs, String headersPreview, String bodyPreview) {
        try {
            ((JavascriptExecutor) driver).executeScript(
                    ApiOverlayJs.INIT_MODAL +
                            "window.__seleniumApiModal.setResponse(arguments[0], arguments[1], arguments[2], arguments[3], arguments[4]);",
                    reqId, status, durationMs, headersPreview, bodyPreview
            );
        } catch (Exception ignored) {}
    }

    public boolean apiHighlightJsonPath(String path) {
        try {
            Object r = ((JavascriptExecutor) driver).executeScript(
                    ApiOverlayJs.INIT_MODAL +
                            "return window.__seleniumApiModal.highlightPath(arguments[0]);",
                    path
            );
            return r instanceof Boolean && (Boolean) r;
        } catch (Exception ignored) {
            return false;
        }
    }

    public int apiHighlightKeyAnimated(String key, long delayMs, int maxHits) {
        try {
            Object r = ((JavascriptExecutor) driver).executeScript(
                    ApiOverlayJs.INIT_MODAL +
                            "return window.__seleniumApiModal.highlightKeyAnimated(arguments[0], arguments[1], arguments[2]);",
                    key, delayMs, maxHits
            );
            return (r instanceof Number) ? ((Number) r).intValue() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    public void highlightPathAnimated(String path, int stepDelayMs) {
        ((JavascriptExecutor) driver).executeScript(
                "return window.__seleniumApiModal && window.__seleniumApiModal.highlightPathAnimated"
                        + " ? window.__seleniumApiModal.highlightPathAnimated(arguments[0], arguments[1]) : false;",
                path, stepDelayMs
        );
    }
    public void apiHighlightJsonPathsAnimated(List<String> paths, long delayMs) {
        if (paths == null || paths.isEmpty()) return;

        try {
            ((JavascriptExecutor) driver).executeScript(
                    ApiOverlayJs.INIT_MODAL +
                            "window.__seleniumApiModal.highlightManyPaths(arguments[0], arguments[1]);",
                    paths, delayMs
            );
        } catch (Exception ignored) {
            // overlay nie może wysadzać testów
        }
    }


    // ======================================================================
    //  REACT SAFE EXECUTOR
    // ======================================================================

    public ReactSafeExecutor reactSafe() {
        if (reactSafeExecutor == null) {
            reactSafeExecutor = new ReactSafeExecutor(driver, this);
        }
        return reactSafeExecutor;
    }
}
