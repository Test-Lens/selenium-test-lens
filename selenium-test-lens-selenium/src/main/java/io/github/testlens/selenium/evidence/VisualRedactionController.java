package io.github.testlens.selenium.evidence;

import io.github.testlens.utils.JsResources;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class VisualRedactionController {
    private static final By PASSWORDS = By.cssSelector("input[type='password']");
    private static final int MAX_BATCH_ATTEMPTS = 2;
    private static final double GEOMETRY_TOLERANCE_CSS_PX = 1.5d;
    private final WebDriver driver;
    private final VisualRedactionOptions options;
    private final String batchId = "test-lens-mask-" + UUID.randomUUID();
    private final List<String> warnings = new ArrayList<>();
    private boolean installed;

    VisualRedactionController(WebDriver driver, VisualRedactionOptions options) {
        this.driver = driver;
        this.options = options == null ? VisualRedactionOptions.defaults() : options;
    }

    boolean enabled() { return options.hasMasks(); }
    List<String> warnings() { return List.copyOf(warnings); }
    void apply() { if (enabled()) refresh(); }

    void refresh() {
        if (!enabled()) return;
        if (!(driver instanceof JavascriptExecutor js)) {
            failOrWarn(summary(0, 0, 0, Map.of(BatchStatus.INSTALL_FAILED, 1)));
            return;
        }
        BatchResult last = null;
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_BATCH_ATTEMPTS; attempt++) {
            if (installed) cleanupBatch(js, null, false);
            ResolvedBatch resolved = resolveAll();
            if (resolved.requested() == 0) return;
            if (resolved.elements().isEmpty()) {
                last = new BatchResult(resolved.requested(), 0, 0,
                        new LinkedHashMap<>(resolved.failures()), false);
                continue;
            }
            try {
                last = installAndVerify(js, resolved, attempt);
                lastFailure = null;
            } catch (StaleElementReferenceException failure) {
                installed = true;
                lastFailure = failure;
                last = resolved.failed(BatchStatus.TARGET_DISCONNECTED);
            } catch (RuntimeException failure) {
                installed = true;
                lastFailure = failure;
                last = resolved.failed(BatchStatus.INSTALL_FAILED);
            }
            if (last.blurFallback()) addWarning("BLUR was not supported and safely fell back to SOLID");
            if (last.allVerified()) return;
        }
        String diagnostic = summary(last.requested(), last.installed(), last.verified(), last.failures());
        if (options.failurePolicy() == VisualRedactionFailurePolicy.STRICT) {
            throw lastFailure == null ? new VisualRedactionException(diagnostic)
                    : new VisualRedactionException(diagnostic, lastFailure);
        }
        addWarning(diagnostic);
    }

    void remove(Throwable primary) {
        if (installed && driver instanceof JavascriptExecutor js) cleanupBatch(js, primary, true);
    }

    private ResolvedBatch resolveAll() {
        List<WebElement> elements = new ArrayList<>();
        List<String> modes = new ArrayList<>();
        Map<BatchStatus, Integer> failures = new LinkedHashMap<>();
        int requested = 0;
        if (options.maskPasswordInputs()) {
            try {
                List<WebElement> found = driver.findElements(PASSWORDS);
                requested += found.size();
                for (WebElement element : found) {
                    elements.add(element);
                    modes.add(VisualMaskMode.SOLID.name());
                }
            } catch (RuntimeException failure) {
                requested++;
                increment(failures, BatchStatus.TARGET_MISSING);
            }
        }
        for (VisualMaskRule rule : options.rules()) {
            try {
                List<WebElement> found = driver.findElements(rule.locator());
                if (found.isEmpty()) {
                    requested++;
                    increment(failures, BatchStatus.TARGET_MISSING);
                } else {
                    requested += found.size();
                    for (WebElement element : found) {
                        elements.add(element);
                        modes.add(rule.mode().name());
                    }
                }
            } catch (RuntimeException failure) {
                requested++;
                increment(failures, BatchStatus.TARGET_MISSING);
            }
        }
        return new ResolvedBatch(requested, elements, modes, failures);
    }

    private BatchResult installAndVerify(JavascriptExecutor js, ResolvedBatch resolved, int attempt) {
        // One command installs the complete batch, crosses the paint barrier, and verifies every mask.
        installed = true;
        ensureVisualTypography(js);
        Object value = js.executeAsyncScript(APPLY_AND_VERIFY_SCRIPT, batchId, attempt,
                resolved.elements(), resolved.modes(), options.solidColor(), options.blurRadiusPx(),
                options.paddingPx(), options.maskLabel(), GEOMETRY_TOLERANCE_CSS_PX);
        if (!(value instanceof Map<?, ?> result)) return resolved.failed(BatchStatus.INSTALL_FAILED);
        int installedCount = number(result.get("installed"));
        int verifiedCount = number(result.get("verified"));
        Map<BatchStatus, Integer> failures = new LinkedHashMap<>(resolved.failures());
        if (!batchId.equals(result.get("batchId")) || installedCount < 0 || installedCount > resolved.elements().size()
                || verifiedCount < 0 || verifiedCount > installedCount) {
            increment(failures, BatchStatus.INSTALL_FAILED);
        }
        Object rawStatuses = result.get("statuses");
        if (rawStatuses instanceof List<?> statuses) {
            int verifiedStatuses = 0;
            for (Object raw : statuses) {
                if (!(raw instanceof Map<?, ?> status)) {
                    increment(failures, BatchStatus.INSTALL_FAILED);
                    continue;
                }
                BatchStatus category = BatchStatus.from(status.get("status"));
                if (category == BatchStatus.VERIFIED) verifiedStatuses++;
                if (category != BatchStatus.VERIFIED && category != BatchStatus.UNSUPPORTED_BLUR_FALLBACK) {
                    increment(failures, category);
                }
            }
            if (statuses.size() != resolved.elements().size() || verifiedStatuses != verifiedCount) {
                increment(failures, BatchStatus.INSTALL_FAILED);
            }
        } else {
            increment(failures, BatchStatus.INSTALL_FAILED);
        }
        return new BatchResult(resolved.requested(), installedCount, verifiedCount, failures,
                Boolean.TRUE.equals(result.get("blurFallback")));
    }

    private void cleanupBatch(JavascriptExecutor js, Throwable primary, boolean finalCleanup) {
        try {
            Object removed = js.executeScript(REMOVE_SCRIPT, batchId);
            if (!Boolean.TRUE.equals(removed)) throw new IllegalStateException("Browser did not confirm mask cleanup");
            installed = false;
        } catch (RuntimeException failure) {
            if (options.failurePolicy() == VisualRedactionFailurePolicy.STRICT) {
                if (primary != null) primary.addSuppressed(failure);
                else throw failure;
            } else if (finalCleanup) {
                addWarning("Browser did not confirm visual mask cleanup");
            }
        }
    }

    private void failOrWarn(String message) {
        if (options.failurePolicy() == VisualRedactionFailurePolicy.STRICT) throw new VisualRedactionException(message);
        addWarning(message);
    }

    private void addWarning(String message) { if (!warnings.contains(message)) warnings.add(message); }
    private static int number(Object value) { return value instanceof Number number ? number.intValue() : 0; }
    private static void increment(Map<BatchStatus, Integer> failures, BatchStatus status) {
        failures.merge(status, 1, Integer::sum);
    }
    private static String summary(int requested, int installed, int verified, Map<BatchStatus, Integer> failures) {
        int failed = failures.values().stream().mapToInt(Integer::intValue).sum();
        return "Visual redaction batch not fully verified: requested=" + requested + ", installed=" + installed
                + ", verified=" + verified + ", failed=" + failed + ", reasons=" + failures;
    }

    private enum BatchStatus {
        VERIFIED, TARGET_MISSING, TARGET_DISCONNECTED, INVALID_TARGET_RECT, MASK_MISSING, RECT_MISMATCH,
        INSTALL_FAILED, UNSUPPORTED_BLUR_FALLBACK;
        private static BatchStatus from(Object value) {
            if (value instanceof String name) {
                try { return valueOf(name); } catch (IllegalArgumentException ignored) { }
            }
            return INSTALL_FAILED;
        }
    }

    private record ResolvedBatch(int requested, List<WebElement> elements, List<String> modes,
                                 Map<BatchStatus, Integer> failures) {
        private BatchResult failed(BatchStatus status) {
            Map<BatchStatus, Integer> combined = new LinkedHashMap<>(failures);
            combined.merge(status, Math.max(1, elements.size()), Integer::sum);
            return new BatchResult(requested, 0, 0, combined, false);
        }
    }

    private record BatchResult(int requested, int installed, int verified,
                               Map<BatchStatus, Integer> failures, boolean blurFallback) {
        private boolean allVerified() { return requested == installed && installed == verified && failures.isEmpty(); }
    }

    private static final String APPLY_AND_VERIFY_SCRIPT = """
            const batchId = arguments[0], attempt = arguments[1], elements = arguments[2], modes = arguments[3];
            const color = arguments[4], blur = arguments[5], padding = arguments[6], label = arguments[7];
            const tolerance = arguments[8], done = arguments[arguments.length - 1];
            const typography = window.__uiTestLens && window.__uiTestLens.modules.visualTypography;
            const uiFont = typography ? typography.uiStack
              : '"Test Lens Sora", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
            const selector = '[data-test-lens-visual-mask-batch="' + CSS.escape(batchId) + '"]';
            document.querySelectorAll(selector).forEach(node => node.remove());
            const host = document.body || document.documentElement;
            if (!host) {
              done({installed: 0, verified: 0, statuses: elements.map((_, i) => ({index: i, status: 'INSTALL_FAILED'}))});
              return;
            }
            const container = document.createElement('div');
            container.setAttribute('data-test-lens-visual-mask-batch', batchId);
            container.setAttribute('data-test-lens-visual-mask-attempt', String(attempt));
            container.setAttribute('aria-hidden', 'true');
            container.style.setProperty('pointer-events', 'none', 'important');
            container.style.setProperty('position', 'static', 'important');
            const supportsBlur = !!(window.CSS && (CSS.supports('backdrop-filter', `blur(${blur}px)`)
              || CSS.supports('-webkit-backdrop-filter', `blur(${blur}px)`)));
            const records = [], statuses = [];
            let blurFallback = false;
            for (let i = 0; i < elements.length; i++) {
              const target = elements[i];
              if (!target || !target.isConnected) { statuses[i] = {index: i, status: 'TARGET_DISCONNECTED'}; continue; }
              const rect = target.getBoundingClientRect();
              if (![rect.left, rect.top, rect.width, rect.height].every(Number.isFinite)
                  || rect.width <= 0 || rect.height <= 0) {
                statuses[i] = {index: i, status: 'INVALID_TARGET_RECT'}; continue;
              }
              const mask = document.createElement('div');
              mask.setAttribute('data-test-lens-visual-mask', batchId);
              mask.setAttribute('data-test-lens-visual-mask-index', String(i));
              mask.setAttribute('aria-hidden', 'true');
              const style = mask.style;
              style.setProperty('position', 'absolute', 'important');
              style.setProperty('left', `${rect.left + scrollX - padding}px`, 'important');
              style.setProperty('top', `${rect.top + scrollY - padding}px`, 'important');
              style.setProperty('width', `${rect.width + padding * 2}px`, 'important');
              style.setProperty('height', `${rect.height + padding * 2}px`, 'important');
              style.setProperty('box-sizing', 'border-box', 'important');
              style.setProperty('pointer-events', 'none', 'important');
              style.setProperty('user-select', 'none', 'important');
              style.setProperty('z-index', '2147483647', 'important');
              style.setProperty('display', 'flex', 'important');
              style.setProperty('align-items', 'center', 'important');
              style.setProperty('justify-content', 'center', 'important');
              style.setProperty('overflow', 'hidden', 'important');
              style.setProperty('margin', '0', 'important');
              style.setProperty('padding', '0', 'important');
              const wantsBlur = modes[i] === 'BLUR';
              if (wantsBlur && supportsBlur) {
                mask.dataset.testLensMaskMode = 'BLUR';
                style.setProperty('backdrop-filter', `blur(${blur}px)`, 'important');
                style.setProperty('-webkit-backdrop-filter', `blur(${blur}px)`, 'important');
                style.setProperty('background', 'rgba(255,255,255,0.08)', 'important');
              } else {
                mask.dataset.testLensMaskMode = 'SOLID';
                if (wantsBlur) blurFallback = true;
                style.setProperty('background', color, 'important');
              }
              if (label && (!wantsBlur || !supportsBlur)) {
                mask.textContent = label;
                style.setProperty('color', '#FFFFFF', 'important');
                style.setProperty('font-family', uiFont, 'important');
                style.setProperty('font-size', '11px', 'important');
                style.setProperty('font-weight', '600', 'important');
                style.setProperty('line-height', '1', 'important');
                style.setProperty('letter-spacing', '0.08em', 'important');
              }
              container.appendChild(mask);
              records.push({index: i, target, mask});
            }
            host.appendChild(container);
            requestAnimationFrame(() => requestAnimationFrame(() => {
              let verified = 0;
              for (const record of records) {
                const {index, target, mask} = record;
                if (!target || !target.isConnected) {
                  statuses[index] = {index, status: 'TARGET_DISCONNECTED'}; continue;
                }
                const targetRect = target.getBoundingClientRect();
                if (![targetRect.left, targetRect.top, targetRect.width, targetRect.height].every(Number.isFinite)
                    || targetRect.width <= 0 || targetRect.height <= 0) {
                  statuses[index] = {index, status: 'INVALID_TARGET_RECT'}; continue;
                }
                if (!mask || !mask.isConnected || mask.getAttribute('data-test-lens-visual-mask') !== batchId
                    || mask.parentElement !== container || !container.isConnected) {
                  statuses[index] = {index, status: 'MASK_MISSING'}; continue;
                }
                const maskRect = mask.getBoundingClientRect();
                const expected = {left: targetRect.left - padding, top: targetRect.top - padding,
                  right: targetRect.right + padding, bottom: targetRect.bottom + padding};
                const geometryMatches = Math.abs(maskRect.left - expected.left) <= tolerance
                  && Math.abs(maskRect.top - expected.top) <= tolerance
                  && Math.abs(maskRect.right - expected.right) <= tolerance
                  && Math.abs(maskRect.bottom - expected.bottom) <= tolerance;
                const fullyCovers = maskRect.left <= expected.left + tolerance && maskRect.top <= expected.top + tolerance
                  && maskRect.right >= expected.right - tolerance && maskRect.bottom >= expected.bottom - tolerance;
                if (!geometryMatches || !fullyCovers) {
                  statuses[index] = {index, status: 'RECT_MISMATCH', expectedRect: expected,
                    actualMaskRect: {left: maskRect.left, top: maskRect.top, right: maskRect.right, bottom: maskRect.bottom}};
                  continue;
                }
                const computed = getComputedStyle(mask);
                const rendered = computed.pointerEvents === 'none'
                  && (mask.dataset.testLensMaskMode === 'SOLID'
                    ? computed.backgroundColor !== 'rgba(0, 0, 0, 0)'
                    : computed.backdropFilter !== 'none' || computed.webkitBackdropFilter !== 'none');
                if (!rendered) { statuses[index] = {index, status: 'INSTALL_FAILED'}; continue; }
                statuses[index] = {index, status: 'VERIFIED', expectedRect: expected,
                  actualMaskRect: {left: maskRect.left, top: maskRect.top, right: maskRect.right, bottom: maskRect.bottom}};
                verified++;
              }
              done({batchId, installed: records.length, verified, statuses, blurFallback});
            }));
            """;

    private static final String TYPOGRAPHY_PROBE = """
            return !!(window.__uiTestLens && window.__uiTestLens.modules
              && window.__uiTestLens.modules.visualTypography
              && window.__uiTestLens.state.typography.loadPromise);
            """;

    private static void ensureVisualTypography(JavascriptExecutor js) {
        try {
            if (!Boolean.TRUE.equals(js.executeScript(TYPOGRAPHY_PROBE))) {
                js.executeScript(visualTypographyInit());
            }
        } catch (RuntimeException ignored) {
            // Typography is presentation-only; masking and STRICT verification must still run.
        }
    }

    private static String visualTypographyInit() {
        String fontPath = "uitestlens/runtime/fonts/Sora-wght.woff2";
        try (InputStream input = VisualRedactionController.class.getClassLoader().getResourceAsStream(fontPath)) {
            if (input == null) throw new IllegalArgumentException("Font resource not found: " + fontPath);
            return JsResources.load("uitestlens/runtime/visual-typography.js")
                    + "window.__uiTestLens.modules.visualTypography.installBase64('"
                    + Base64.getEncoder().encodeToString(input.readAllBytes()) + "');";
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to load font resource: " + fontPath, failure);
        }
    }

    private static final String REMOVE_SCRIPT = """
            const batchId = arguments[0];
            const escaped = CSS.escape(batchId);
            document.querySelectorAll('[data-test-lens-visual-mask-batch="' + escaped + '"]').forEach(node => node.remove());
            document.querySelectorAll('[data-test-lens-visual-mask="' + escaped + '"]').forEach(node => node.remove());
            return !document.querySelector('[data-test-lens-visual-mask-batch="' + escaped + '"]')
              && !document.querySelector('[data-test-lens-visual-mask="' + escaped + '"]');
            """;
}
