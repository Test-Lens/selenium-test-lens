package io.github.testlens.actions;

import io.github.testlens.HighlightOptions;
import io.github.testlens.HighlightState;
import io.github.testlens.OverlayConfig;
import io.github.testlens.core.HighlightJs;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Browser decoration helpers. None of these methods performs an application action. */
public class HighlightActions {
    private final JavascriptExecutor js;
    private final OverlayRootManager rootManager;
    private final OverlayConfig config;
    private final OverlayLogger logger;
    private final OperationCoordinator operations = new OperationCoordinator();

    public HighlightActions(WebDriver driver, OverlayRootManager rootManager, OverlayConfig config) {
        this(driver, rootManager, config, OverlayLogger.noop());
    }

    public HighlightActions(WebDriver driver, OverlayRootManager rootManager, OverlayConfig config, OverlayLogger logger) {
        if (driver == null) throw new IllegalArgumentException("driver must not be null");
        if (!(driver instanceof JavascriptExecutor)) throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        this.js = (JavascriptExecutor) driver;
        this.rootManager = rootManager;
        this.config = config == null ? OverlayConfig.builder().build() : config;
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    /** Legacy manual highlight; it is a neutral visual event, never an assertion result. */
    public void highlightClick(WebElement element, String label) { highlight(element, label, HighlightState.ACTION, false); }

    /**
     * Internal-style ACTION feedback for a real operation; honors automatic feedback settings.
     * @since 0.3.1
     */
    public void automaticAction(WebElement element, String label) {
        highlight(element, label, HighlightState.ACTION, true);
    }

    boolean automaticActionIfAbsent(WebElement element, String label) {
        return render(element, label, HighlightState.ACTION, true, true);
    }

    public void highlight(WebElement element, String label, HighlightState state, boolean automatic) {
        render(element, label, state, automatic, false);
    }

    private boolean render(WebElement element, String label, HighlightState state,
                           boolean automatic, boolean onlyIfNoOperation) {
        HighlightOptions options = config.getHighlightOptions();
        if (!config.isEnabled() || !options.enabled() || (automatic && !options.automaticFeedback()) || element == null) return false;
        String safeLabel = logger.redactionPolicy().redact(label == null ? "" : label);
        HighlightState effective = state == null ? HighlightState.ACTION : state;
        Presentation presentation = operations.presentation(effective, automatic, onlyIfNoOperation);
        if (presentation == null) return false;
        try {
            rootManager.ensureRootExists();
            Map<String, Object> runtime = options.toRuntimeMap(effective);
            runtime.put("sessionId", presentation.sessionId());
            runtime.put("operationId", presentation.operationId());
            runtime.put("standalone", presentation.standalone());
            Object rendered = js.executeScript(HighlightJs.INIT
                    + "return window.__uiTestLens.modules.highlight.element(arguments[0], arguments[1], arguments[2]);",
                    element, safeLabel, runtime);
            emitHighlight("highlightElement", safeLabel, effective, Boolean.TRUE.equals(rendered), null,
                    automatic, presentation.operationId());
            return Boolean.TRUE.equals(rendered);
        } catch (RuntimeException decorationFailure) {
            emitHighlight("highlightElement", safeLabel, effective, false, decorationFailure,
                    automatic, presentation.operationId());
            return false;
        }
    }

    public void highlightParent(WebElement element, int levelsUp, String label) {
        decorateRelative("highlightParent", true, element, Math.max(1, levelsUp), null, label);
    }

    public void highlightClosest(WebElement element, String cssSelector, String label) {
        if (cssSelector != null) decorateRelative("highlightClosest", false, element, 0, cssSelector, label);
    }

    private void decorateRelative(String method, boolean parent, WebElement element, int levels, String selector, String label) {
        HighlightOptions options = config.getHighlightOptions();
        if (!config.isEnabled() || !options.enabled() || element == null) return;
        String safeLabel = logger.redactionPolicy().redact(label == null ? "" : label);
        try {
            rootManager.ensureRootExists();
            String script = HighlightJs.INIT + (parent
                    ? "return window.__uiTestLens.modules.highlight.parent(arguments[0], arguments[1], arguments[2], arguments[3]);"
                    : "return window.__uiTestLens.modules.highlight.closest(arguments[0], arguments[1], arguments[2], arguments[3]);");
            Map<String, Object> runtime = options.toRuntimeMap(HighlightState.ACTION);
            Presentation presentation = operations.presentation(HighlightState.ACTION, false, false);
            runtime.put("sessionId", presentation.sessionId());
            runtime.put("operationId", presentation.operationId());
            runtime.put("standalone", true);
            Object rendered = parent
                    ? js.executeScript(script, element, levels, safeLabel, runtime)
                    : js.executeScript(script, element, selector, safeLabel, runtime);
            emitHighlight(method, safeLabel, HighlightState.ACTION, Boolean.TRUE.equals(rendered), null,
                    false, presentation.operationId());
        } catch (RuntimeException failure) {
            emitHighlight(method, safeLabel, HighlightState.ACTION, false, failure, false, "");
        }
    }

    private void emitHighlight(String method, String label, HighlightState state, boolean rendered, Throwable failure,
                               boolean automatic, String operationId) {
        try {
            UiTestLensLogEntry.Builder builder = UiTestLensLogEntry.builder().level(failure == null ? UiTestLensLogLevel.INFO : UiTestLensLogLevel.WARN)
                    .eventType(UiTestLensEventType.HIGHLIGHT).status(failure == null ? UiTestLensStatus.INFO : UiTestLensStatus.WARN)
                    .message("Highlight " + state + (rendered ? " rendered" : " skipped"))
                    .action(method).target(TargetDescriptor.label(label)).metadata("method", method)
                    .metadata("highlightState", state.name()).metadata("rendered", String.valueOf(rendered))
                    .metadata("label", label).metadata("feedbackKind", automatic ? "automatic" : "manual")
                    .metadata("operationId", operationId == null ? "" : operationId).throwable(failure);
            if (!automatic && config.getHudOptions().sourceNavigation().enabled()) {
                builder.metadata("testlens.internal.captureSourceLocation", "true");
            }
            logger.emit(builder.build());
        } catch (Exception ignored) { }
    }

    private record Presentation(String sessionId, String operationId, boolean standalone) {}

    private static final class OperationCoordinator {
        private final String sessionId = UUID.randomUUID().toString();
        private final AtomicLong sequence = new AtomicLong();
        private final Map<Long, Operation> byThread = new ConcurrentHashMap<>();

        synchronized Presentation presentation(HighlightState state, boolean automatic, boolean onlyIfNoOperation) {
            if (!automatic) return new Presentation(sessionId, nextId(), true);
            long thread = Thread.currentThread().getId();
            Operation current = byThread.get(thread);
            boolean startsAction = state == HighlightState.ACTION;
            boolean startsWaiting = state == HighlightState.WAITING && (current == null || current.terminal);
            if (startsAction || startsWaiting) {
                if (onlyIfNoOperation && current != null && !current.terminal) return null;
                current = new Operation(nextId());
                byThread.put(thread, current);
            } else if (current == null) {
                current = new Operation(nextId());
                byThread.put(thread, current);
            }
            if (state == HighlightState.SUCCESS || state == HighlightState.FAILURE) current.terminal = true;
            return new Presentation(sessionId, current.id, false);
        }

        private String nextId() { return sessionId + ":" + sequence.incrementAndGet(); }

        private static final class Operation {
            private final String id;
            private boolean terminal;
            private Operation(String id) { this.id = id; }
        }
    }

}
