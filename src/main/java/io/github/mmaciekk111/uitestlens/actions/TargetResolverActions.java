package io.github.mmaciekk111.uitestlens.actions;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;

/**
 * Szuka "prawdziwego" celu akcji na podstawie podanego elementu:
 * - resolveClickTarget(...) -> coś, co sensownie kliknąć,
 * - resolveFileInputTarget(...) -> input[type=file] powiązany z danym elementem,
 * - + metody zwracające selektor CSS tych celów.
 */
public class TargetResolverActions {

    private final WebDriver driver;
    private final JavascriptExecutor js;
    private final OverlayLogger logger;

    public TargetResolverActions(WebDriver driver) {
        this(driver, OverlayLogger.noop());
    }

    public TargetResolverActions(WebDriver driver, OverlayLogger logger) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.driver = driver;
        this.js = (JavascriptExecutor) driver;
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    /**
     * Znajduje sensowny element do kliknięcia:
     * - jeśli base już jest klikalny -> zwraca base,
     * - jeśli nie -> szuka wewnątrz potomków,
     * - jeśli dalej nic -> idzie po przodkach do góry,
     * - jeśli nadal nic -> base (ostatnia deska ratunku).
     */
    public WebElement resolveClickTarget(WebElement base) {
        if (base == null) {
            emitResolve("resolveClickTarget", null, false);
            return null;
        }

        Object result = js.executeScript(
                "var el = arguments[0];" +
                        "if (!el) return null;" +

                        "function isClickable(e) {" +
                        "  if (!e) return false;" +
                        "  var tag = (e.tagName || '').toLowerCase();" +
                        "  if (tag === 'button') return true;" +
                        "  if (tag === 'a' && e.hasAttribute('href')) return true;" +
                        "  if (tag === 'input') {" +
                        "    var type = (e.type || '').toLowerCase();" +
                        "    if (['button','submit','reset','radio','checkbox','file','image'].indexOf(type) !== -1) return true;" +
                        "  }" +
                        "  var role = (e.getAttribute && e.getAttribute('role')) || '';" +
                        "  if (role.toLowerCase() === 'button') return true;" +
                        "  if (typeof e.onclick === 'function') return true;" +
                        "  return false;" +
                        "}" +

                        "function findClickableInSubtree(root) {" +
                        "  if (!root || !root.querySelectorAll) return null;" +
                        "  var candidates = root.querySelectorAll(" +
                        "    'button, input, a[href], [role=\"button\"], [data-test-click-target]' " +
                        "  );" +
                        "  for (var i = 0; i < candidates.length; i++) {" +
                        "    if (isClickable(candidates[i])) return candidates[i];" +
                        "  }" +
                        "  return null;" +
                        "}" +

                        "// 1) jeśli sam el jest klikalny -> zwróć go" +
                        "if (isClickable(el)) return el;" +

                        "// 2) potomkowie" +
                        "var sub = findClickableInSubtree(el);" +
                        "if (sub) return sub;" +

                        "// 3) jeśli to <label>, spróbuj po atrybucie for" +
                        "var tagName = (el.tagName || '').toLowerCase();" +
                        "if (tagName === 'label') {" +
                        "  var id = el.getAttribute('for');" +
                        "  if (id) {" +
                        "    var forEl = document.getElementById(id);" +
                        "    if (forEl && isClickable(forEl)) return forEl;" +
                        "  }" +
                        "}" +

                        "// 4) przodkowie" +
                        "var parent = el.parentElement;" +
                        "while (parent && parent !== document.body) {" +
                        "  if (isClickable(parent)) return parent;" +
                        "  var inner = findClickableInSubtree(parent);" +
                        "  if (inner) return inner;" +
                        "  parent = parent.parentElement;" +
                        "}" +

                        "// 5) nic lepszego nie znaleźliśmy -> zwróć bazowy" +
                        "return el;",
                base
        );

        if (result instanceof WebElement) {
            emitResolve("resolveClickTarget", null, true);
            return (WebElement) result;
        }
        emitResolve("resolveClickTarget", null, true);
        return base;
    }

    /**
     * Znajduje input[type=file] powiązany z podanym elementem:
     * - jeśli base jest input[type=file] -> zwraca base,
     * - jeśli w potomkach jest input[type=file] -> pierwszy,
     * - jeśli base jest <label for="..."> -> skojarzony input,
     * - jeśli dalej nic -> szuka po przodkach.
     */
    public WebElement resolveFileInputTarget(WebElement base) {
        if (base == null) {
            emitResolve("resolveFileInputTarget", null, false);
            return null;
        }

        Object result = js.executeScript(
                "var el = arguments[0];" +
                        "if (!el) return null;" +

                        "function isFileInput(e) {" +
                        "  if (!e) return false;" +
                        "  var tag = (e.tagName || '').toLowerCase();" +
                        "  if (tag !== 'input') return false;" +
                        "  var type = (e.type || '').toLowerCase();" +
                        "  return type === 'file';" +
                        "}" +

                        "function findFileInputInSubtree(root) {" +
                        "  if (!root || !root.querySelectorAll) return null;" +
                        "  var inputs = root.querySelectorAll('input[type=\"file\"]');" +
                        "  if (inputs.length > 0) return inputs[0];" +
                        "  return null;" +
                        "}" +

                        "// 1) sam element" +
                        "if (isFileInput(el)) return el;" +

                        "// 2) potomkowie" +
                        "var sub = findFileInputInSubtree(el);" +
                        "if (sub) return sub;" +

                        "// 3) label for=..." +
                        "var tagName = (el.tagName || '').toLowerCase();" +
                        "if (tagName === 'label') {" +
                        "  var id = el.getAttribute('for');" +
                        "  if (id) {" +
                        "    var forEl = document.getElementById(id);" +
                        "    if (isFileInput(forEl)) return forEl;" +
                        "  }" +
                        "}" +

                        "// 4) przodkowie" +
                        "var parent = el.parentElement;" +
                        "while (parent && parent !== document.body) {" +
                        "  if (isFileInput(parent)) return parent;" +
                        "  var inner = findFileInputInSubtree(parent);" +
                        "  if (inner) return inner;" +
                        "  parent = parent.parentElement;" +
                        "}" +

                        "return null;",
                base
        );

        if (result instanceof WebElement) {
            emitResolve("resolveFileInputTarget", null, true);
            return (WebElement) result;
        }
        emitResolve("resolveFileInputTarget", null, false);
        return null;
    }

    // === WERSJE ZWRACAJĄCE SELEKTOR CSS ===

    private String buildCssSelector(WebElement el) {
        if (el == null) {
            return null;
        }
        String tag = el.getTagName();
        if (tag == null || tag.isBlank()) {
            tag = "*";
        }
        StringBuilder sb = new StringBuilder(tag.toLowerCase());

        String id = el.getAttribute("id");
        if (id != null && !id.isBlank()) {
            sb.append("#").append(id);
        }

        String classAttr = el.getAttribute("class");
        if (classAttr != null && !classAttr.isBlank()) {
            String[] classes = classAttr.trim().split("\\s+");
            for (String c : classes) {
                if (!c.isBlank()) {
                    sb.append(".").append(c);
                }
            }
        }

        return sb.toString();
    }

    /** Selektor CSS dla targetu kliknięcia (albo null). */
    public String resolveClickTargetSelector(WebElement base) {
        WebElement target = resolveClickTarget(base);
        String selector = (target != null) ? buildCssSelector(target) : null;
        emitResolve("resolveClickTargetSelector", selector, selector != null);
        return selector;
    }

    /** Selektor CSS dla input[type=file] powiązanego z base (albo null). */
    public String resolveFileInputSelector(WebElement base) {
        WebElement target = resolveFileInputTarget(base);
        String selector = (target != null) ? buildCssSelector(target) : null;
        emitResolve("resolveFileInputSelector", selector, selector != null);
        return selector;
    }

    private void emitResolve(String method, String selector, boolean resolved) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(resolved ? UiTestLensLogLevel.DEBUG : UiTestLensLogLevel.WARN)
                    .eventType(resolved ? UiTestLensEventType.ACTION : UiTestLensEventType.ERROR)
                    .status(resolved ? UiTestLensStatus.PASSED : UiTestLensStatus.FAILED)
                    .message("Target resolver " + method + " " + (resolved ? "resolved" : "not resolved"))
                    .action(method)
                    .target(selector == null ? TargetDescriptor.none() : TargetDescriptor.selector(selector))
                    .metadata("method", method)
                    .metadata("selector", selector == null ? "" : selector)
                    .metadata("resolved", String.valueOf(resolved))
                    .build());
        } catch (Exception ignored) {}
    }
}
