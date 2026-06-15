package io.github.testlens.actions;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

/**
 * Resolves practical browser targets for clicks and file uploads.
 *
 * <p>The file-input resolver handles direct {@code input[type=file]} elements, descendants,
 * associated {@code <label for="...">} controls, and ancestor containers.
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
     * Resolves the element that should receive a click when the provided element is a wrapper.
     *
     * <p>The resolver checks the element itself, clickable descendants, associated label targets,
     * and then ancestors before falling back to the original element.
     */
    public WebElement resolveClickTarget(WebElement base) {
        if (base == null) {
            emitResolve("resolveClickTarget", null, false);
            return null;
        }

        Object result = js.executeScript(clickTargetResolverScript(), base);

        if (result instanceof WebElement) {
            emitResolve("resolveClickTarget", null, true);
            return (WebElement) result;
        }
        emitResolve("resolveClickTarget", null, true);
        return base;
    }

    /**
     * Resolves an {@code input[type=file]} associated with the provided element.
     *
     * <p>The resolver checks the element itself, descendants, {@code <label for="...">}
     * associations, and ancestor containers.
     */
    public WebElement resolveFileInputTarget(WebElement base) {
        if (base == null) {
            emitResolve("resolveFileInputTarget", null, false);
            return null;
        }

        Object result = js.executeScript(fileInputResolverScript(), base);

        if (result instanceof WebElement) {
            emitResolve("resolveFileInputTarget", null, true);
            return (WebElement) result;
        }
        emitResolve("resolveFileInputTarget", null, false);
        return null;
    }

    static String clickTargetResolverScript() {
        return "var el = arguments[0];" +
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
                "  var candidates = root.querySelectorAll('button, input, a[href], [role=\"button\"], [data-test-click-target]');" +
                "  for (var i = 0; i < candidates.length; i++) {" +
                "    if (isClickable(candidates[i])) return candidates[i];" +
                "  }" +
                "  return null;" +
                "}" +
                "if (isClickable(el)) return el;" +
                "var sub = findClickableInSubtree(el);" +
                "if (sub) return sub;" +
                "var tagName = (el.tagName || '').toLowerCase();" +
                "if (tagName === 'label') {" +
                "  var id = el.getAttribute('for');" +
                "  if (id) {" +
                "    var forEl = document.getElementById(id);" +
                "    if (forEl && isClickable(forEl)) return forEl;" +
                "  }" +
                "}" +
                "var parent = el.parentElement;" +
                "while (parent && parent !== document.body) {" +
                "  if (isClickable(parent)) return parent;" +
                "  var inner = findClickableInSubtree(parent);" +
                "  if (inner) return inner;" +
                "  parent = parent.parentElement;" +
                "}" +
                "return el;";
    }

    static String fileInputResolverScript() {
        return "var el = arguments[0];" +
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
                "if (isFileInput(el)) return el;" +
                "var sub = findFileInputInSubtree(el);" +
                "if (sub) return sub;" +
                "var tagName = (el.tagName || '').toLowerCase();" +
                "if (tagName === 'label') {" +
                "  var id = el.getAttribute('for');" +
                "  if (id) {" +
                "    var forEl = document.getElementById(id);" +
                "    if (isFileInput(forEl)) return forEl;" +
                "  }" +
                "}" +
                "var parent = el.parentElement;" +
                "while (parent && parent !== document.body) {" +
                "  if (isFileInput(parent)) return parent;" +
                "  var inner = findFileInputInSubtree(parent);" +
                "  if (inner) return inner;" +
                "  parent = parent.parentElement;" +
                "}" +
                "return null;";
    }

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

    /** Returns a CSS selector for the resolved click target, or {@code null} when none is resolved. */
    public String resolveClickTargetSelector(WebElement base) {
        WebElement target = resolveClickTarget(base);
        String selector = (target != null) ? buildCssSelector(target) : null;
        emitResolve("resolveClickTargetSelector", selector, selector != null);
        return selector;
    }

    /** Returns a CSS selector for the associated {@code input[type=file]}, or {@code null}. */
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
        } catch (Exception ignored) {
        }
    }
}

