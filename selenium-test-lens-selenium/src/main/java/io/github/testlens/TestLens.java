package io.github.testlens;

import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.RetryPolicyViolationException;
import io.github.testlens.core.trace.RetrySummary;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.RetryOutcomePolicy;
import io.github.testlens.selenium.assertions.UiExpect;
import io.github.testlens.selenium.assertions.UiPageExpect;
import io.github.testlens.selenium.assertions.UiAssertionOptions;
import io.github.testlens.selenium.evidence.ScreenshotCaptureOptions;
import io.github.testlens.selenium.evidence.ScreenshotCaptureResult;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.network.NetworkDiagnostics;
import io.github.testlens.selenium.steps.UiStepOptions;
import io.github.testlens.selenium.steps.UiStepResult;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/** Public, runner-agnostic entry point for attaching Test Lens to an existing WebDriver. */
public final class TestLens {
    private final JsOverlayDebug delegate;
    private final TestLensOptions options;
    private final Object finalizationLock = new Object();
    private final Map<UiTestLensSession, FacadeFinalization> finalizations = new IdentityHashMap<>();
    private final FinalizationObserver finalizationObserver;

    private TestLens(WebDriver driver, TestLensOptions options) {
        this(driver, options, ignored -> { });
    }

    TestLens(WebDriver driver, TestLensOptions options, FinalizationObserver finalizationObserver) {
        this.options = options == null ? TestLensOptions.defaults() : options;
        this.finalizationObserver = finalizationObserver == null ? ignored -> { } : finalizationObserver;
        this.delegate = new JsOverlayDebug(driver, this.options.overlayConfig(), this.options.redactionPolicy(),
                this.options.locatorOptions());
    }

    public static TestLens attach(WebDriver driver) { return new TestLens(driver, TestLensOptions.defaults()); }
    public static TestLens attach(WebDriver driver, OverlayConfig config) {
        return new TestLens(driver, TestLensOptions.builder().overlayConfig(config).build());
    }
    public static TestLens attach(WebDriver driver, TestLensOptions options) { return new TestLens(driver, options); }

    public WebDriver driver() { return delegate.getDriver(); }
    public UiTestLensSession startSession(String name) {
        UiTestLensSession session = delegate.startSession(name, options.retryOutcomePolicy(), options.allowedRetries(),
                options.redactionPolicy());
        // Safe even before the first document exists; subsequent native events lazily reinject it.
        try { delegate.initHud(session.metadata().name(), ""); } catch (RuntimeException ignored) {
            // The browser may not have a document yet. Native events will retry lazily.
        }
        return session;
    }
    public Optional<UiTestLensSession> session() { return delegate.session(); }
    public RetrySummary retrySummary() {
        return delegate.session().map(UiTestLensSession::retrySummary)
                .orElseGet(() -> new RetrySummary(0, java.time.Duration.ZERO, false,
                        options.retryOutcomePolicy(), false, java.util.Map.of(), java.util.Map.of(), java.util.Map.of()));
    }

    public UiLocator locator(By by) { return delegate.locator(by); }
    public UiLocator locator(By by, String label) { return delegate.locator(by, label); }
    public UiExpect expect(By by) { return locator(by).expect(); }
    public UiExpect expect(By by, String label) { return locator(by, label).expect(); }
    public UiPageExpect expectPage() { return delegate.expectPage(); }
    public UiPageExpect expectPage(UiAssertionOptions options) { return delegate.expectPage(options); }
    /** Waits for {@code document.readyState == "complete"} using configured locator wait options. */
    public void waitForPageReady() { delegate.waitForPageReady(); }
    /** Waits for {@code document.readyState == "complete"} within the supplied total timeout. */
    public void waitForPageReady(Duration timeout) { delegate.waitForPageReady(timeout); }
    /** Waits for an interactive or complete document using configured locator wait options. */
    public void waitForInteractiveOrComplete() { delegate.waitForInteractiveOrComplete(); }
    /** Waits for an interactive or complete document within the supplied total timeout. */
    public void waitForInteractiveOrComplete(Duration timeout) { delegate.waitForInteractiveOrComplete(timeout); }
    /**
     * Waits for the default idle window in the XHR/fetch tracker. This is not complete browser-network-idle
     * detection; see {@link io.github.testlens.core.PageWaits#waitForNetworkIdle()}.
     */
    public void waitForNetworkIdle() { delegate.waitForNetworkIdle(); }
    /** Waits for the supplied XHR/fetch idle window within the supplied total timeout. */
    public void waitForNetworkIdle(Duration idleDuration, Duration timeout) {
        delegate.waitForNetworkIdle(idleDuration, timeout);
    }
    public UiLocator getByTestId(String testId) { return delegate.getByTestId(testId); }
    public UiLocator getByText(String text) { return delegate.getByText(text); }
    public UiLocator getByText(String text, String label) { return delegate.getByText(text, label); }
    public UiLocator getByTextContaining(String text) { return delegate.getByTextContaining(text); }
    public UiLocator getByPlaceholder(String placeholder) { return delegate.getByPlaceholder(placeholder); }
    public UiLocator getByLabel(String label) { return delegate.getByLabel(label); }
    public UiLocator getByAltText(String altText) { return delegate.getByAltText(altText); }
    public UiLocator getByRole(String role) { return delegate.getByRole(role); }
    public UiLocator getByRole(String role, String accessibleName) { return delegate.getByRole(role, accessibleName); }
    public ScreenshotCaptureResult captureScreenshot(String name) { return delegate.captureScreenshot(name); }
    public ScreenshotCaptureResult captureScreenshot(String name, ScreenshotCaptureOptions options) { return delegate.captureScreenshot(name, options); }
    public void scrollToElementWithArrow(WebElement element) { delegate.scrollToElementWithArrow(element); }
    public void smartUploadFile(WebElement element, String absolutePath) { delegate.smartUploadFile(element, absolutePath); }
    public <T> T apiCallWithModal(String title, String method, String url, String payloadPreview,
                                  long timeoutMs, Callable<T> call, Function<T, String> responsePreview) {
        return delegate.apiCallWithModal(title, method, url, payloadPreview, timeoutMs, call, responsePreview);
    }
    public UiStepResult step(String name, Runnable body) { return delegate.step(name, body); }
    public UiStepResult step(String name, UiStepOptions options, Runnable body) { return delegate.step(name, options, body); }

    public TestLens switchToFrame(By frame, String label) { return switchToFrame(locator(frame, label)); }
    public TestLens switchToFrame(UiLocator frame) {
        return contextOperation("context.frame", "Switch to frame: " + frame.description(),
                () -> driver().switchTo().frame(frame.resolve()));
    }
    public TestLens switchToFrame(int index, String label) {
        return contextOperation("context.frame", "Switch to frame " + label + " index=" + index,
                () -> driver().switchTo().frame(index));
    }
    public TestLens switchToParentFrame() {
        return contextOperation("context.parentFrame", "Switch to parent frame", () -> driver().switchTo().parentFrame());
    }
    public TestLens switchToDefaultContent() {
        return contextOperation("context.defaultContent", "Switch to default content", () -> driver().switchTo().defaultContent());
    }

    public String currentWindowHandle() { return driver().getWindowHandle(); }
    public Set<String> windowHandles() { return Set.copyOf(driver().getWindowHandles()); }
    public TestLens switchToWindow(String handle) { return switchToWindow(handle, handle); }
    public TestLens switchToWindow(String handle, String label) {
        return contextOperation("context.window", "Switch to window: " + label + " handle=" + handle,
                () -> driver().switchTo().window(handle));
    }
    public String waitForNewWindow(Set<String> existingHandles) {
        Set<String> before = existingHandles == null ? Set.of() : Set.copyOf(existingHandles);
        emit("context.newWindow", "Waiting for one new window", io.github.testlens.core.logging.UiTestLensStatus.STARTED,
                io.github.testlens.core.logging.UiTestLensLogLevel.INFO, null);
        try {
            Set<String> difference = new WebDriverWait(driver(), options.locatorOptions().timeout())
                    .pollingEvery(options.locatorOptions().pollInterval())
                    .until(webDriver -> {
                        Set<String> found = new LinkedHashSet<>(webDriver.getWindowHandles());
                        found.removeAll(before);
                        return found.isEmpty() ? null : found;
                    });
            if (difference.size() != 1) {
                throw new NoSuchWindowException("Expected exactly one new window; found " + difference.size() + ": " + difference);
            }
            String handle = difference.iterator().next();
            emit("context.newWindow", "New window detected: " + handle,
                    io.github.testlens.core.logging.UiTestLensStatus.PASSED,
                    io.github.testlens.core.logging.UiTestLensLogLevel.INFO, null);
            return handle;
        } catch (RuntimeException failure) {
            emit("context.newWindow", "New window detection failed",
                    io.github.testlens.core.logging.UiTestLensStatus.FAILED,
                    io.github.testlens.core.logging.UiTestLensLogLevel.ERROR, failure);
            throw failure;
        }
    }
    public TestLens switchToNewWindow(Set<String> existingHandles, String label) {
        return switchToWindow(waitForNewWindow(existingHandles), label);
    }

    public TestLensAlert alert() { return new TestLensAlert(driver(), options.locatorOptions(), delegate); }

    /** Returns the network diagnostics owned by this Lens facade. */
    public NetworkDiagnostics network() { return delegate.network(); }

    /**
     * Runs the facade finalization pipeline once and requests a passed terminal outcome.
     * The first finalizer wins, concurrent callers share the same result, and the WebDriver is never quit.
     */
    public TestLensFinalizationResult finishPassed() {
        return finish(FinalizationOutcome.PASSED, null, null);
    }

    /** Runs the facade finalization pipeline once and requests a failed terminal outcome. */
    public TestLensFinalizationResult finishFailed(Throwable originalFailure) {
        return finish(FinalizationOutcome.FAILED, originalFailure, null);
    }

    /** Runs the facade finalization pipeline once and requests a skipped terminal outcome. */
    public TestLensFinalizationResult finishSkipped(String reason) {
        return finish(FinalizationOutcome.SKIPPED, null, reason);
    }

    private TestLensFinalizationResult finish(FinalizationOutcome outcome,
                                              Throwable originalFailure,
                                              String skipReason) {
        UiTestLensSession session = delegate.session().orElse(null);
        if (session == null) {
            List<Throwable> diagnostics = new ArrayList<>();
            diagnostics.add(new IllegalStateException("No Test Lens session was started"));
            return new TestLensFinalizationResult(null, null, null, null, null, safeDiagnostics(diagnostics));
        }

        FinalizationRequest request = requestFor(session, outcome, originalFailure, skipReason);
        FacadeFinalization finalization;
        boolean owner;
        synchronized (finalizationLock) {
            finalization = finalizations.computeIfAbsent(session, ignored -> new FacadeFinalization(request));
            owner = finalization.claim();
        }
        if (owner) {
            try {
                finalization.complete(runFinalization(session, finalization.request()));
            } catch (Throwable failure) {
                finalization.complete(new FinalizationCompletion(null, failure));
            }
        }
        return finalization.await();
    }

    private FinalizationRequest requestFor(UiTestLensSession session, FinalizationOutcome requested,
                                           Throwable originalFailure, String skipReason) {
        TraceStatus current = session.metadata().status();
        if (current == TraceStatus.STARTED) {
            return new FinalizationRequest(requested, originalFailure, skipReason, true);
        }
        FinalizationOutcome effective = switch (current) {
            case PASSED -> FinalizationOutcome.PASSED;
            case FAILED -> FinalizationOutcome.FAILED;
            case SKIPPED -> FinalizationOutcome.SKIPPED;
            default -> requested;
        };
        return new FinalizationRequest(effective, null, null, false);
    }

    private FinalizationCompletion runFinalization(UiTestLensSession session, FinalizationRequest request) {
        List<Throwable> diagnostics = new ArrayList<>();
        FinalizationOutcome outcome = request.outcome();
        Throwable originalFailure = request.originalFailure();
        String skipReason = request.skipReason();

        Path directory = sessionOutputDirectory(session);
        RetryPolicyViolationException policyViolation = null;
        RetryPolicyViolationException predictedPolicyViolation = request.finishSession()
                ? policyViolationFor(outcome, session) : null;
        boolean failedOutcome = outcome == FinalizationOutcome.FAILED || predictedPolicyViolation != null;
        FailureBundleCapture bundle = failedOutcome && options.failureBundleOptions().enabled()
                ? new FailureBundleCapture(driver(), delegate, session, options, directory) : null;
        Path screenshotPath = null;

        if (failedOutcome) {
            observe(FinalizationStage.FAILURE_EVIDENCE);
            Throwable effectiveFailure = outcome == FinalizationOutcome.FAILED
                    ? originalFailure : predictedPolicyViolation;
            if (bundle != null) {
                screenshotPath = bundle.captureDiagnosticScreenshot(options.screenshotOnFailure());
                bundle.captureCleanScreenshot(options.screenshotOnFailure());
                bundle.captureRemaining(effectiveFailure, predictedPolicyViolation != null);
            } else if (options.screenshotOnFailure()) {
                screenshotPath = captureLegacyFailureScreenshot(directory, diagnostics);
            }
        }

        try {
            observe(FinalizationStage.NETWORK_STOP);
            delegate.stopNetworkDiagnostics();
        } catch (RuntimeException failure) {
            diagnostics.add(failure);
        }

        if (request.finishSession()) {
            try {
                observe(FinalizationStage.SESSION_FINISH);
                switch (outcome) {
                    case PASSED -> session.finishPassed();
                    case FAILED -> session.finishFailed(originalFailure);
                    case SKIPPED -> session.finishSkipped(skipReason);
                }
            } catch (RetryPolicyViolationException failure) {
                policyViolation = failure;
            } catch (RuntimeException failure) {
                diagnostics.add(failure);
            }
        }

        Path json = directory.resolve("trace.json");
        Path html = directory.resolve("report.html");
        try {
            observe(FinalizationStage.JSON_EXPORT);
            session.exportJson(json);
        } catch (RuntimeException failure) { diagnostics.add(failure); json = null; }
        try {
            observe(FinalizationStage.HTML_EXPORT);
            session.exportHtml(html);
        } catch (RuntimeException failure) { diagnostics.add(failure); html = null; }
        if (options.cleanupHudOnFinish()) {
            try {
                observe(FinalizationStage.HUD_CLEANUP);
                delegate.clearDebugArtifacts();
            } catch (RuntimeException failure) { diagnostics.add(failure); }
        }
        if (bundle != null) {
            observe(FinalizationStage.BUNDLE_COMPLETE);
            bundle.complete(json, html);
            diagnostics.addAll(bundle.failures());
        }
        List<Throwable> safeDiagnostics = safeDiagnostics(diagnostics);
        TestLensFinalizationResult result = new TestLensFinalizationResult(
                session, directory, json, html, screenshotPath, safeDiagnostics);
        if (policyViolation != null) {
            RetryPolicyViolationException finalPolicyViolation = policyViolation;
            safeDiagnostics.forEach(failure -> {
                if (failure != finalPolicyViolation) finalPolicyViolation.addSuppressed(failure);
            });
            return new FinalizationCompletion(result, finalPolicyViolation);
        }
        return new FinalizationCompletion(result, null);
    }

    private void observe(FinalizationStage stage) {
        finalizationObserver.before(stage);
    }

    private RetryPolicyViolationException policyViolationFor(FinalizationOutcome outcome, UiTestLensSession session) {
        if (outcome != FinalizationOutcome.PASSED) return null;
        long retries = session.retrySummary().totalRetries();
        boolean triggered = switch (options.retryOutcomePolicy()) {
            case REPORT_ONLY, WARN -> false;
            case FAIL_ON_ANY_RETRY -> retries >= 1;
            case FAIL_AFTER_N -> retries > options.allowedRetries();
        };
        return triggered ? new RetryPolicyViolationException(options.retryOutcomePolicy(), session.retrySummary()) : null;
    }

    private List<Throwable> safeDiagnostics(List<Throwable> failures) {
        return failures.stream().map(failure -> diagnosticCopy(failure, 0)).toList();
    }

    private Throwable diagnosticCopy(Throwable failure, int depth) {
        if (failure == null || depth >= 16) return null;
        DiagnosticFailure safe = new DiagnosticFailure(failure.getClass().getName(),
                options.redactionPolicy().redact(failure.getMessage()), diagnosticCopy(failure.getCause(), depth + 1));
        safe.setStackTrace(failure.getStackTrace());
        for (Throwable suppressed : failure.getSuppressed()) {
            Throwable copy = diagnosticCopy(suppressed, depth + 1);
            if (copy != null) safe.addSuppressed(copy);
        }
        return safe;
    }

    private Path captureLegacyFailureScreenshot(Path directory, List<Throwable> diagnostics) {
        try {
            ScreenshotCaptureResult screenshot = delegate.captureScreenshot("failure", ScreenshotCaptureOptions.builder()
                    .outputDirectory(directory.resolve("screenshots"))
                    .fileNamePrefix("failure")
                    .includeTimestamp(false)
                    .overwriteExisting(false)
                    .attachToSession(true)
                    .build());
            if (!screenshot.isCaptured() && screenshot.exception() != null) diagnostics.add(screenshot.exception());
            return screenshot.path();
        } catch (RuntimeException failure) {
            diagnostics.add(failure);
            return null;
        }
    }

    private enum FinalizationOutcome {
        PASSED,
        FAILED,
        SKIPPED
    }

    enum FinalizationStage {
        FAILURE_EVIDENCE,
        NETWORK_STOP,
        SESSION_FINISH,
        JSON_EXPORT,
        HTML_EXPORT,
        HUD_CLEANUP,
        BUNDLE_COMPLETE
    }

    @FunctionalInterface
    interface FinalizationObserver {
        void before(FinalizationStage stage);
    }

    private record FinalizationRequest(FinalizationOutcome outcome, Throwable originalFailure,
                                       String skipReason, boolean finishSession) { }

    private record FinalizationCompletion(TestLensFinalizationResult result, Throwable terminalFailure) { }

    private static final class FacadeFinalization {
        private final FinalizationRequest request;
        private final CompletableFuture<FinalizationCompletion> completion = new CompletableFuture<>();
        private boolean claimed;

        private FacadeFinalization(FinalizationRequest request) {
            this.request = request;
        }

        private boolean claim() {
            if (claimed) return false;
            claimed = true;
            return true;
        }

        private FinalizationRequest request() {
            return request;
        }

        private void complete(FinalizationCompletion value) {
            completion.complete(value);
        }

        private TestLensFinalizationResult await() {
            FinalizationCompletion value = completion.join();
            Throwable failure = value.terminalFailure();
            if (failure instanceof RuntimeException runtimeException) throw runtimeException;
            if (failure instanceof Error error) throw error;
            if (failure != null) throw new RuntimeException(failure);
            return value.result();
        }
    }

    private static final class DiagnosticFailure extends RuntimeException {
        private final String originalType;
        private DiagnosticFailure(String originalType, String message, Throwable cause) {
            super(message, cause, true, true);
            this.originalType = originalType;
        }
        @Override public String toString() {
            return originalType + (getMessage() == null ? "" : ": " + getMessage());
        }
    }

    private TestLens contextOperation(String action, String description, Runnable operation) {
        emit(action, description, io.github.testlens.core.logging.UiTestLensStatus.STARTED,
                io.github.testlens.core.logging.UiTestLensLogLevel.INFO, null);
        try {
            operation.run();
            emit(action, description, io.github.testlens.core.logging.UiTestLensStatus.PASSED,
                    io.github.testlens.core.logging.UiTestLensLogLevel.INFO, null);
            return this;
        } catch (RuntimeException failure) {
            emit(action, description, io.github.testlens.core.logging.UiTestLensStatus.FAILED,
                    io.github.testlens.core.logging.UiTestLensLogLevel.ERROR, failure);
            throw failure;
        }
    }

    private void emit(String action, String description,
                      io.github.testlens.core.logging.UiTestLensStatus status,
                      io.github.testlens.core.logging.UiTestLensLogLevel level,
                      Throwable failure) {
        delegate.emitConsumerOperation(action, description, status, level, failure);
    }

    private Path sessionOutputDirectory(UiTestLensSession session) {
        return options.outputRoot().resolve(sanitize(session.metadata().name())).resolve(session.id());
    }

    private static String sanitize(String value) {
        String safe = value == null ? "session" : value.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-").replaceAll("-+", "-")
                .replaceAll("(^[-.]+|[-.]+$)", "");
        return safe.isBlank() ? "session" : safe;
    }
}
