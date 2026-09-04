package io.github.testlens;

import lombok.Getter;
import org.openqa.selenium.*;
import io.github.testlens.actions.*;
import io.github.testlens.api.ApiOverlayJs;
import io.github.testlens.api.ApiOverlayPanel;
import io.github.testlens.core.Guards;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.OverlayRootManager;
import io.github.testlens.core.PageWaits;
import io.github.testlens.core.PopupDetector;
import io.github.testlens.core.UiTestLensRuntimeNames;
import io.github.testlens.core.WaitHudJs;
import io.github.testlens.core.browser.BrowserScriptExecutor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensLogSink;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.logging.UiTestLensStatus;
import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.TraceLogSink;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.export.TraceHtmlExportOptions;
import io.github.testlens.hud.HudPanel;
import io.github.testlens.scroll.ScrollElementEdge;
import io.github.testlens.scroll.ScrollViewportEdge;
import io.github.testlens.selenium.SeleniumOverlayFactory;
import io.github.testlens.selenium.actionability.ActionabilityChecker;
import io.github.testlens.selenium.actionability.ActionabilityOptions;
import io.github.testlens.selenium.actionability.ActionabilityReport;
import io.github.testlens.selenium.assertions.UiAssertionOptions;
import io.github.testlens.selenium.assertions.UiExpect;
import io.github.testlens.selenium.auth.AuthRestoreOptions;
import io.github.testlens.selenium.auth.AuthRestoreResult;
import io.github.testlens.selenium.auth.AuthState;
import io.github.testlens.selenium.auth.AuthStateManager;
import io.github.testlens.selenium.auth.AuthStateOptions;
import io.github.testlens.selenium.business.BusinessAssertionOptions;
import io.github.testlens.selenium.business.BusinessAssertions;
import io.github.testlens.selenium.evidence.ScreenshotCapture;
import io.github.testlens.selenium.evidence.ScreenshotCaptureOptions;
import io.github.testlens.selenium.evidence.ScreenshotCaptureResult;
import io.github.testlens.selenium.evidence.ScreenshotCaptureStatus;
import io.github.testlens.selenium.evidence.VideoEvidence;
import io.github.testlens.selenium.evidence.VideoEvidenceOptions;
import io.github.testlens.selenium.evidence.VideoEvidenceResult;
import io.github.testlens.selenium.evidence.VideoEvidenceStatus;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorSelectors;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import io.github.testlens.selenium.network.NetworkDiagnostics;
import io.github.testlens.selenium.network.NetworkDiagnosticsResult;
import io.github.testlens.selenium.overlay.OverlayPolicy;
import io.github.testlens.selenium.overlay.OverlayPolicyExecutor;
import io.github.testlens.selenium.steps.UiStepError;
import io.github.testlens.selenium.steps.UiStepOptions;
import io.github.testlens.selenium.steps.UiStepResult;
import io.github.testlens.selenium.steps.UiStepScope;
import io.github.testlens.selenium.steps.UiStepStatus;


import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    private final ApiOverlayPanel apiPanel;
    private boolean waitHudInjected = false;
    private final Guards guards;
    private final OverlayLogger logger;
    private final SessionTraceLogSink sessionTraceLogSink = new SessionTraceLogSink();
    private final HudLogSink hudLogSink = new HudLogSink();
    private OverlayPolicy overlayPolicy = OverlayPolicy.none();
    private UiTestLensSession session;
    private NetworkDiagnostics networkDiagnostics;

    // ======================================================================
    //  CTOR
    // ======================================================================



    public JsOverlayDebug(WebDriver driver) {
        this(driver, OverlayConfig.builder().build());
    }

    public JsOverlayDebug(WebDriver driver, OverlayConfig config) {
        this(driver, config, createDefaultComponents(driver, config, OverlayLogger.noop()));
    }

    private JsOverlayDebug(WebDriver driver, OverlayConfig config, DefaultComponents components) {
        this(driver, config, components.apiPanel(), components.guards(), components.logger());
    }

    private JsOverlayDebug(WebDriver driver,
                           OverlayConfig config,
                           ApiOverlayPanel apiPanel,
                           Guards guards,
                           OverlayLogger logger) {
        this.apiPanel = apiPanel;
        this.guards = guards;
        OverlayLogger baseLogger = logger != null ? logger : OverlayLogger.noop();
        this.logger = baseLogger.withSink(sessionTraceLogSink).withSink(hudLogSink);
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.driver = driver;
        this.config = config;

        BrowserScriptExecutor scriptExecutor = SeleniumOverlayFactory.scriptExecutor(driver);

        this.rootManager = new OverlayRootManager(scriptExecutor, config);
        this.highlightActions = new HighlightActions(driver, rootManager, config, this.logger);
        this.typingActions = new TypingActions(driver, rootManager, config, this.logger);
        this.smartClickActions = new SmartClickActions(driver, config, rootManager, highlightActions, this.logger);
        this.smartInputActions = new SmartInputActions(driver, config, rootManager, typingActions, this.logger);
        this.hudPanel = new HudPanel(scriptExecutor, rootManager, config);
        this.hudLogSink.attach(this.hudPanel, driver);
        this.pageWaits = new PageWaits(driver, config);
        this.popupDetector = new PopupDetector(driver, config, rootManager, highlightActions);
        this.scrollActions = new ScrollActions(driver, config, rootManager, this.logger);
        this.assertActions = new AssertActions(driver, rootManager, config, hudPanel, this.logger);
        this.targetResolverActions = new TargetResolverActions(driver, this.logger);
    }

    private static DefaultComponents createDefaultComponents(WebDriver driver, OverlayConfig config, OverlayLogger logger) {
        OverlayRootManager rootManager = SeleniumOverlayFactory.overlayRoot(driver, config);
        ApiOverlayPanel apiPanel = SeleniumOverlayFactory.apiOverlayPanel(driver, rootManager, config);
        return new DefaultComponents(apiPanel, new Guards(driver, logger), logger);
    }

    private record DefaultComponents(ApiOverlayPanel apiPanel, Guards guards, OverlayLogger logger) {}

    public void setOverlayPolicy(OverlayPolicy overlayPolicy) {
        this.overlayPolicy = overlayPolicy != null ? overlayPolicy : OverlayPolicy.none();
        this.smartClickActions.setOverlayPolicy(this.overlayPolicy);
    }

    public ActionabilityReport checkActionability(By locator, ActionabilityOptions options) {
        return actionabilityChecker().check(locator, options);
    }

    public ActionabilityReport checkActionability(WebElement element, ActionabilityOptions options) {
        return actionabilityChecker().check(element, options);
    }

    public UiLocator locator(By by) {
        return locator(by, "", UiLocatorOptions.defaults());
    }

    public UiLocator locator(By by, String label) {
        return locator(by, label, UiLocatorOptions.defaults());
    }

    public UiLocator locator(By by, UiLocatorOptions options) {
        return locator(by, "", options);
    }

    public UiLocator locator(By by, String label, UiLocatorOptions options) {
        return new UiLocator(driver, by, label, this, options, logger);
    }


    public UiLocator getByTestId(String testId) {
        return getByTestId(testId, "");
    }

    public UiLocator getByTestId(String testId, String label) {
        return locator(By.cssSelector(UiLocatorSelectors.cssAttributeEquals("data-testid", testId)), label);
    }

    public UiLocator getByPlaceholder(String placeholder) {
        return getByPlaceholder(placeholder, "placeholder: " + safeLabelValue(placeholder));
    }

    public UiLocator getByPlaceholder(String placeholder, String label) {
        return locator(By.xpath("//*[@placeholder = " + UiLocatorSelectors.xpathLiteral(placeholder) + "]"), label);
    }

    public UiLocator getByText(String text) {
        return getByText(text, "text: " + safeLabelValue(text));
    }

    public UiLocator getByText(String text, String label) {
        String literal = UiLocatorSelectors.xpathLiteral(text);
        return locator(By.xpath("//*[" + UiLocatorSelectors.normalizeSpaceExpression(".") + " = " + literal + "]"), label);
    }

    public UiLocator getByTextContaining(String text) {
        return getByTextContaining(text, "text contains: " + safeLabelValue(text));
    }

    public UiLocator getByTextContaining(String text, String label) {
        String literal = UiLocatorSelectors.xpathLiteral(text);
        return locator(By.xpath("//*[contains(" + UiLocatorSelectors.normalizeSpaceExpression(".") + ", " + literal + ")]"), label);
    }

    public UiLocator getByLabel(String labelText) {
        return getByLabel(labelText, "label: " + safeLabelValue(labelText));
    }

    public UiLocator getByLabel(String labelText, String label) {
        return locator(SemanticBy.label(labelText), label);
    }

    public UiLocator getByRole(String role) {
        return getByRole(role, "");
    }

    public UiLocator getByRole(String role, String accessibleName) {
        String label = accessibleName == null || accessibleName.isBlank()
                ? "role: " + safeLabelValue(role)
                : "role: " + safeLabelValue(role) + ", name: " + safeLabelValue(accessibleName);
        return locator(SemanticBy.role(role, accessibleName), label);
    }

    public UiLocator getByAltText(String altText) {
        return getByAltText(altText, "alt: " + safeLabelValue(altText));
    }

    public UiLocator getByAltText(String altText, String label) {
        return locator(SemanticBy.altText(altText), label);
    }

    public UiExpect expect(By by) {
        return expect(locator(by));
    }

    public UiExpect expect(By by, String label) {
        return expect(locator(by, label));
    }

    public UiExpect expect(UiLocator locator) {
        return expect(locator, UiAssertionOptions.defaults());
    }

    public UiExpect expect(UiLocator locator, UiAssertionOptions options) {
        return locator.expect(options);
    }

    public BusinessAssertions business(String subject) {
        return business(subject, BusinessAssertionOptions.defaults());
    }

    public BusinessAssertions business(String subject, BusinessAssertionOptions options) {
        return new BusinessAssertions(subject, options, logger);
    }

    public AuthStateManager auth() {
        return new AuthStateManager(driver, logger);
    }

    public AuthState captureAuthState(AuthStateOptions options) {
        return auth().captureState(options);
    }

    public AuthRestoreResult restoreAuthState(AuthState state, AuthRestoreOptions options) {
        return auth().restoreState(state, options);
    }

    public AuthRestoreResult restoreAuthState(Path path, AuthRestoreOptions options) {
        return auth().restoreState(path, options);
    }

    public NetworkDiagnostics network() {
        if (networkDiagnostics == null) {
            networkDiagnostics = new NetworkDiagnostics(driver, logger);
        }
        return networkDiagnostics;
    }

    public NetworkDiagnosticsResult attachNetworkLog(Path outputPath) {
        return network().attachToSession(requireSession(), outputPath);
    }

    public void attachSession(UiTestLensSession session) {
        this.session = session;
        this.sessionTraceLogSink.attach(session);
    }

    public Optional<UiTestLensSession> session() {
        return Optional.ofNullable(session);
    }

    public UiTestLensSession startSession(String name) {
        UiTestLensSession started = UiTestLensSession.start(name);
        attachSession(started);
        return started;
    }

    Optional<NetworkDiagnostics> networkDiagnosticsSnapshot() {
        return Optional.ofNullable(networkDiagnostics);
    }

    void stopNetworkDiagnostics() {
        NetworkDiagnostics current = networkDiagnostics;
        if (current != null && current.isStarted()) {
            current.stop();
        }
    }

    UiTestLensSession startSession(String name,
                                   io.github.testlens.core.trace.RetryOutcomePolicy policy,
                                   int allowedRetries) {
        UiTestLensSession started = UiTestLensSession.start(name, policy, allowedRetries);
        attachSession(started);
        return started;
    }

    public TraceArtifact attachScreenshot(String name, Path path) {
        return requireSession().attachScreenshot(name, path);
    }

    public TraceArtifact attachVideo(String name, Path path) {
        VideoEvidenceResult result = attachVideoFile(name, path);
        if (result.artifact() == null) {
            throw new IllegalStateException(result.message());
        }
        return result.artifact();
    }

    public TraceArtifact attachArtifact(TraceArtifact artifact) {
        return requireSession().attachArtifact(artifact);
    }

    public String exportTraceHtml() {
        return requireSession().exportHtml();
    }

    public String exportTraceHtml(TraceHtmlExportOptions options) {
        return requireSession().exportHtml(options);
    }

    public Path exportTraceHtml(Path outputPath) {
        return requireSession().exportHtml(outputPath);
    }

    public Path exportTraceHtml(Path outputPath, TraceHtmlExportOptions options) {
        return requireSession().exportHtml(outputPath, options);
    }

    public ScreenshotCaptureResult captureScreenshot(String name) {
        return captureScreenshot(name, ScreenshotCaptureOptions.defaults());
    }

    public ScreenshotCaptureResult captureScreenshot(String name, ScreenshotCaptureOptions options) {
        ScreenshotCaptureOptions effectiveOptions = options == null ? ScreenshotCaptureOptions.defaults() : options;
        emitScreenshotCaptureStarted(name, effectiveOptions);
        ScreenshotCaptureResult result = new ScreenshotCapture(driver).capture(name, effectiveOptions, session);
        emitScreenshotCaptureFinished(result);
        return result;
    }

    public VideoEvidenceResult attachVideoFile(String name, Path path) {
        return attachVideoFile(name, path, VideoEvidenceOptions.defaults());
    }

    public VideoEvidenceResult attachVideoFile(String name, Path path, VideoEvidenceOptions options) {
        VideoEvidenceOptions effectiveOptions = options == null ? VideoEvidenceOptions.defaults() : options;
        VideoEvidenceResult result = new VideoEvidence().attachFile(name, path, effectiveOptions, session);
        emitVideoEvidenceResult(result);
        return result;
    }

    public VideoEvidenceResult attachVideoUrl(String name, String url) {
        return attachVideoUrl(name, url, VideoEvidenceOptions.defaults());
    }

    public VideoEvidenceResult attachVideoUrl(String name, String url, VideoEvidenceOptions options) {
        VideoEvidenceOptions effectiveOptions = options == null ? VideoEvidenceOptions.defaults() : options;
        VideoEvidenceResult result = new VideoEvidence().attachUrl(name, url, effectiveOptions, session);
        emitVideoEvidenceResult(result);
        return result;
    }

    public UiStepResult step(String name, Runnable body) {
        return step(name, UiStepOptions.defaults(), body);
    }

    public UiStepResult step(String name, UiStepOptions options, Runnable body) {
        UiStepScope scope = new UiStepScope(
                logger,
                this::setStep,
                message -> hudLog("info", message, "ui-test-lens")
        );
        try {
            UiStepResult result = scope.run(name, options, body);
            captureFailedStepScreenshotIfNeeded(name, options, result);
            return result;
        } catch (RuntimeException | Error originalFailure) {
            captureFailedStepScreenshotBestEffort(name, options, originalFailure);
            throw originalFailure;
        }
    }

    private void captureFailedStepScreenshotBestEffort(String stepName, UiStepOptions options, Throwable originalFailure) {
        UiStepOptions effectiveOptions = options == null ? UiStepOptions.defaults() : options;
        if (!effectiveOptions.captureScreenshotOnFailure()) return;
        try {
            ScreenshotCaptureResult result = captureScreenshot("failed-step-" + safeString(stepName), effectiveOptions.screenshotCaptureOptions());
            if (!result.isCaptured() && result.exception() != null) originalFailure.addSuppressed(result.exception());
        } catch (RuntimeException diagnosticFailure) {
            originalFailure.addSuppressed(diagnosticFailure);
        }
    }

    private void captureFailedStepScreenshotIfNeeded(String stepName, UiStepOptions options, UiStepResult result) {
        UiStepOptions effectiveOptions = options == null ? UiStepOptions.defaults() : options;
        if (!effectiveOptions.captureScreenshotOnFailure() || result == null || result.status() != UiStepStatus.FAILED) {
            return;
        }
        try {
            ScreenshotCaptureResult screenshot = captureScreenshot("failed-step-" + safeString(stepName), effectiveOptions.screenshotCaptureOptions());
            if (!screenshot.isCaptured() && screenshot.exception() != null && result.failure() != null && result.failure().cause() != null) {
                result.failure().cause().addSuppressed(screenshot.exception());
            }
        } catch (RuntimeException screenshotFailure) {
            if (result.failure() != null && result.failure().cause() != null) {
                result.failure().cause().addSuppressed(screenshotFailure);
            }
        }
    }

    private UiTestLensSession requireSession() {
        if (session == null) {
            throw new IllegalStateException("No UiTestLensSession attached");
        }
        return session;
    }

    void emitConsumerOperation(String action,
                               String description,
                               UiTestLensStatus status,
                               UiTestLensLogLevel level,
                               Throwable failure) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(UiTestLensEventType.ACTION)
                    .status(status)
                    .message(description == null ? action : description)
                    .action(action)
                    .metadata("description", description == null ? "" : description)
                    .throwable(failure)
                    .build());
        } catch (RuntimeException ignored) {
            // Trace/HUD presentation is observability and cannot alter the browser operation.
        }
    }

    private ActionabilityChecker actionabilityChecker() {
        OverlayPolicyExecutor policyExecutor = overlayPolicy == null || overlayPolicy.isEmpty()
                ? null
                : new OverlayPolicyExecutor(driver, overlayPolicy, logger);
        return new ActionabilityChecker(driver, policyExecutor, logger);
    }

    private static String safeLabelValue(String value) {
        String input = SemanticBy.normalizeText(value);
        return input.length() <= 80 ? input : input.substring(0, 77) + "...";
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
        emit(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.STEP)
                .status(UiTestLensStatus.INFO)
                .message(stepDescription)
                .step(stepDescription)
                .action("hud.setStep")
                .build());
    }

    public void hudLog(String level, String message, String timestamp) {
        hudPanel.appendLog(level, message, timestamp);
        emit(UiTestLensLogEntry.builder()
                .level(toLogLevel(level))
                .eventType(UiTestLensEventType.HUD)
                .status(toStatus(level))
                .message(message)
                .action("hud.log")
                .metadata("hudLevel", safeString(level))
                .metadata("timestamp", safeString(timestamp))
                .build());
    }

    private void emit(UiTestLensLogEntry entry) {
        try {
            logger.emit(entry);
        } catch (Exception ignored) {}
    }

    private void emitScreenshotCaptureStarted(String name, ScreenshotCaptureOptions options) {
        emit(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.SCREENSHOT_CAPTURE_STARTED)
                .status(UiTestLensStatus.STARTED)
                .message("Screenshot capture started")
                .action("screenshot.capture")
                .metadata("name", safeString(name))
                .metadata("outputDirectory", options == null ? "" : options.outputDirectory().toString())
                .build());
    }

    private void emitScreenshotCaptureFinished(ScreenshotCaptureResult result) {
        if (result == null) {
            return;
        }
        boolean captured = result.status() == ScreenshotCaptureStatus.CAPTURED;
        emit(UiTestLensLogEntry.builder()
                .level(captured ? UiTestLensLogLevel.INFO : UiTestLensLogLevel.ERROR)
                .eventType(captured ? UiTestLensEventType.SCREENSHOT_CAPTURE_PASSED : UiTestLensEventType.SCREENSHOT_CAPTURE_FAILED)
                .status(captured ? UiTestLensStatus.PASSED : UiTestLensStatus.FAILED)
                .message(result.message())
                .action("screenshot.capture")
                .metadata("name", result.name())
                .metadata("path", result.path() == null ? "" : result.path().toString())
                .throwable(result.exception())
                .build());
    }

    private void emitVideoEvidenceResult(VideoEvidenceResult result) {
        if (result == null) {
            return;
        }
        boolean attached = result.status() == VideoEvidenceStatus.ATTACHED;
        boolean skipped = result.status() == VideoEvidenceStatus.SKIPPED;
        UiTestLensEventType eventType = attached
                ? UiTestLensEventType.VIDEO_ATTACHED
                : skipped ? UiTestLensEventType.VIDEO_ATTACH_SKIPPED : UiTestLensEventType.VIDEO_ATTACH_FAILED;
        UiTestLensStatus status = attached
                ? UiTestLensStatus.PASSED
                : skipped ? UiTestLensStatus.SKIPPED : UiTestLensStatus.FAILED;
        UiTestLensLogLevel level = attached
                ? UiTestLensLogLevel.INFO
                : skipped ? UiTestLensLogLevel.WARN : UiTestLensLogLevel.ERROR;
        emit(UiTestLensLogEntry.builder()
                .level(level)
                .eventType(eventType)
                .status(status)
                .message(result.message())
                .action("video.attach")
                .metadata("name", result.name())
                .metadata("source", result.source().name())
                .metadata("path", result.path() == null ? "" : result.path().toString())
                .metadata("url", videoUrlPreview(result.url()))
                .throwable(result.exception())
                .build());
    }

    private static String videoUrlPreview(String url) {
        String safe = safeString(url);
        int query = safe.indexOf('?');
        String withoutQuery = query >= 0 ? safe.substring(0, query) : safe;
        return withoutQuery.length() <= 160 ? withoutQuery : withoutQuery.substring(0, 157) + "...";
    }

    private static UiTestLensLogLevel toLogLevel(String level) {
        String normalized = safeString(level).toLowerCase();
        return switch (normalized) {
            case "trace" -> UiTestLensLogLevel.TRACE;
            case "debug" -> UiTestLensLogLevel.DEBUG;
            case "warn", "warning" -> UiTestLensLogLevel.WARN;
            case "error", "failed", "fail" -> UiTestLensLogLevel.ERROR;
            default -> UiTestLensLogLevel.INFO;
        };
    }

    private static UiTestLensStatus toStatus(String level) {
        String normalized = safeString(level).toLowerCase();
        return switch (normalized) {
            case "success", "done", "passed", "pass" -> UiTestLensStatus.PASSED;
            case "warn", "warning" -> UiTestLensStatus.WARN;
            case "error", "failed", "fail" -> UiTestLensStatus.FAILED;
            default -> UiTestLensStatus.INFO;
        };
    }

    private static String safeString(String value) {
        return value == null ? "" : value;
    }

    private static final class SessionTraceLogSink implements UiTestLensLogSink {
        private volatile TraceLogSink delegate;

        void attach(UiTestLensSession session) {
            this.delegate = session == null ? null : new TraceLogSink(session);
        }

        @Override
        public void accept(UiTestLensLogEntry entry) {
            TraceLogSink current = delegate;
            if (current != null) {
                current.accept(entry);
            }
        }
    }

    static final class HudLogSink implements UiTestLensLogSink {
        private volatile HudPanel hud;
        private volatile WebDriver driver;
        private final java.util.Queue<UiTestLensLogEntry> deferredDuringAlert = new java.util.concurrent.ConcurrentLinkedQueue<>();

        void attach(HudPanel hud, WebDriver driver) { this.hud = hud; this.driver = driver; }

        @Override
        public void accept(UiTestLensLogEntry entry) {
            HudPanel current = hud;
            if (current == null || entry == null || entry.eventType() == UiTestLensEventType.HUD) return;
            if (isRawNetworkEntry(entry.eventType())
                    && "false".equalsIgnoreCase(entry.metadata().get("hudVisible"))) return;
            WebDriver currentDriver = driver;
            if (currentDriver != null) {
                try {
                    WebDriver.TargetLocator target = currentDriver.switchTo();
                    if (target != null && target.alert() != null) {
                        deferredDuringAlert.add(entry);
                        return; // Executing HUD JavaScript could dismiss an active browser dialog.
                    }
                } catch (org.openqa.selenium.NoAlertPresentException ignored) {
                    // Normal document context: HUD updates are safe.
                } catch (RuntimeException ignored) {
                    return; // A diagnostic probe must never alter or fail the browser operation.
                }
            }
            UiTestLensLogEntry deferred;
            while ((deferred = deferredDuringAlert.poll()) != null) append(current, deferred);
            append(current, entry);
        }

        private static boolean isRawNetworkEntry(UiTestLensEventType eventType) {
            return eventType == UiTestLensEventType.NETWORK_REQUEST_RECORDED
                    || eventType == UiTestLensEventType.NETWORK_RESPONSE_RECORDED
                    || eventType == UiTestLensEventType.NETWORK_FAILURE_RECORDED;
        }

        private static void append(HudPanel hud, UiTestLensLogEntry entry) {
            String description = entry.metadata().getOrDefault("description", "");
            String action = entry.action() == null ? "" : entry.action();
            String message = description.isBlank() ? entry.message() : action + ": " + description;
            hud.appendLog(entry.level().name().toLowerCase(), message, entry.timestamp().toString());
        }
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
        if (element != null) {
            element.click();
        }
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

    /** Smart typing and highlight with a custom label. */
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

    // ======================================================================
    //  CLEANUP
    // ======================================================================

    /** Clears all overlay elements (HUD, borders, tooltips). */
    public void clearDebugArtifacts() {
        rootManager.clearAll();
    }

    Object hideDebugArtifactsTemporarily() {
        if (!(driver instanceof JavascriptExecutor executor)) {
            throw new UnsupportedOperationException("WebDriver does not implement JavascriptExecutor");
        }
        return executor.executeScript(
                "var h=document.getElementById('selenium-overlay-host');"
                        + "if(!h){return {present:false};}"
                        + "var v=h.style.visibility;h.style.visibility='hidden';"
                        + "return {present:true,visibility:v};");
    }

    void restoreDebugArtifacts(Object token) {
        if (!(driver instanceof JavascriptExecutor executor)) {
            throw new UnsupportedOperationException("WebDriver does not implement JavascriptExecutor");
        }
        executor.executeScript(
                "var t=arguments[0],h=document.getElementById('selenium-overlay-host');"
                        + "if(t&&t.present&&h){h.style.visibility=t.visibility||'';}", token);
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
                    WaitHudJs.bridgeScript() +
                            "return !!(waitHud && waitHud.start && waitHud.stop);"
            );
            if (present instanceof Boolean && (Boolean) present) {
                return;
            }
        } catch (Exception ignored) {}

        ((JavascriptExecutor) driver).executeScript(WaitHudJs.INIT);
    }


    public void waitHudStart(String label) {
        try {
            ensureWaitHudInjected();
            ((JavascriptExecutor) driver).executeScript(
                    WaitHudJs.bridgeScript() +
                            "if (waitHud && waitHud.start) { waitHud.start(arguments[0]); }",
                    label
            );
        } catch (Exception ignored) {}
        emit(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.WAIT)
                .status(UiTestLensStatus.STARTED)
                .message("Wait HUD started: " + safeString(label))
                .action("waitHud.start")
                .metadata("label", safeString(label))
                .build());
    }

    public void waitHudStop(String prefix, long elapsedMs) {
        try {
            ensureWaitHudInjected();
            ((JavascriptExecutor) driver).executeScript(
                    WaitHudJs.bridgeScript() +
                            "window.__uiTestLens.state.wait.lastElapsedMs = arguments[1];" +
                            "window.__seleniumLastWaitElapsedMs = window.__uiTestLens.state.wait.lastElapsedMs;" +
                            "return waitHud && waitHud.stop ? waitHud.stop(arguments[0], arguments[1]) : null;",
                    prefix, elapsedMs
            );
        } catch (Exception ignored) {}
        emit(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.WAIT)
                .status(UiTestLensStatus.PASSED)
                .message("Wait HUD stopped: " + safeString(prefix))
                .action("waitHud.stop")
                .metadata("prefix", safeString(prefix))
                .metadata("elapsedMs", String.valueOf(elapsedMs))
                .build());
    }

    public void forceHideWaitHud() {
        try {
            ensureWaitHudInjected();
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                    WaitHudJs.bridgeScript() +
                            "if (waitHud && waitHud.forceHide) { waitHud.forceHide(); }"
            );
        } catch (Exception ignored) {}
    }


    // ======================================================================
    //  WAIT INDICATOR (klepsydra)
    // ======================================================================

    /** Pokazuje prosty indykator "czekam" w overlay (klepsydra). */
    public void showWaitIndicator(String label) {
        try {
            ensureWaitHudInjected();
            ((JavascriptExecutor) driver).executeScript(
                    WaitHudJs.bridgeScript() +
                            "if (waitHud && waitHud.showIndicator) { waitHud.showIndicator(arguments[0]); }",
                    label
            );
        } catch (Exception ignored) {
            // HUD nie powinien wysadzac testow
        }
        emit(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.WAIT)
                .status(UiTestLensStatus.STARTED)
                .message("Wait indicator shown: " + safeString(label))
                .action("waitIndicator.show")
                .metadata("label", safeString(label))
                .build());
    }

    /** Chowa indykator "czekam". */
    public void hideWaitIndicator() {
        try {
            ensureWaitHudInjected();
            ((JavascriptExecutor) driver).executeScript(
                    WaitHudJs.bridgeScript() +
                            "if (waitHud && waitHud.hideIndicator) { waitHud.hideIndicator(); }"
            );
        } catch (Exception ignored) {
            // HUD nie powinien wysadzac testow
        }
        emit(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.WAIT)
                .status(UiTestLensStatus.INFO)
                .message("Wait indicator hidden")
                .action("waitIndicator.hide")
                .build());
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
                    UiTestLensRuntimeNames.ensureNamespaceScript() +
                            "var shadow = window.__uiTestLens.state.overlay.root || window.__seleniumOverlayRoot;" +
                            "if (shadow) { window.__uiTestLens.state.overlay.root = shadow; window.__seleniumOverlayRoot = shadow; }" +
                            "if (!shadow) { return; }" +
                            "var step = shadow.querySelector('#selenium-hud-step');" +
                            "if (!step) { return; }" +
                            "var msg = (window.__uiTestLens.state.wait && window.__uiTestLens.state.wait.lastMessage) || window.__seleniumLastWaitMessage || '';" +
                            "window.__seleniumLastWaitMessage = msg;" +
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

        private SoftAssertions(AssertActions actions,
                               AssertionSummary summary) {
            this.actions = actions;
            this.summary = summary;
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
        SoftAssertions soft = new SoftAssertions(assertActions, summary);

        consumer.accept(soft);
        emitAssertionSummary(summary);

        if (failTestOnErrors && summary.hasFailures()) {
            throw new AssertionError(summary.formatForException());
        }

        return summary;
    }

    private void emitAssertionSummary(AssertionSummary summary) {
        if (summary == null) return;
        int total = summary.getAllResults().size();
        int failed = summary.getFailuresObjects().size();
        int passed = Math.max(0, total - failed);
        emit(UiTestLensLogEntry.builder()
                .level(failed > 0 ? UiTestLensLogLevel.WARN : UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.ASSERTION)
                .status(failed > 0 ? UiTestLensStatus.FAILED : UiTestLensStatus.PASSED)
                .message("Assertion group " + safeString(summary.getGroupName()) + " total=" + total + " failed=" + failed)
                .action("assertGroup")
                .metadata("groupName", safeString(summary.getGroupName()))
                .metadata("total", String.valueOf(total))
                .metadata("passed", String.valueOf(passed))
                .metadata("failed", String.valueOf(failed))
                .metadata("soft", "true")
                .build());
    }

    // ======================================================================
    //  TARGET RESOLVER – RETURNS WEBELEMENT OR SELECTOR
    // ======================================================================

    /** Returns a “reasonable” click target for the given element. */
    public WebElement resolveClickTarget(WebElement element) {
        return targetResolverActions.resolveClickTarget(element);
    }

    /** Returns an {@code input[type=file]} associated with the given element, or {@code null}. */
    public WebElement resolveFileInputTarget(WebElement element) {
        return targetResolverActions.resolveFileInputTarget(element);
    }

    /** Returns CSS selector of the click target (e.g. button#save.btn.btn-primary). */
    public String resolveClickTargetCssSelector(WebElement element) {
        return targetResolverActions.resolveClickTargetSelector(element);
    }

    /** Returns a CSS selector for the associated {@code input[type=file]}. */
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
     * - find {@code input[type=file]} associated with the given element,
     * - highlight it,
     * - send file path via sendKeys(path).
     */
    public void smartUploadFile(WebElement containerOrLabel, String absoluteFilePath) {
        emit(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.ACTION)
                .status(UiTestLensStatus.STARTED)
                .message("Upload file action started")
                .action("upload")
                .metadata("method", "smartUploadFile")
                .metadata("pathLength", absoluteFilePath == null ? "0" : String.valueOf(absoluteFilePath.length()))
                .build());
        WebElement fileInput = resolveFileInputTarget(containerOrLabel);
        if (fileInput == null) {
            if (config.isShowHudPanel()) {
                hudPanel.updateStep("smartUploadFile: file input not found for given element");
            }
            emit(UiTestLensLogEntry.builder()
                    .level(UiTestLensLogLevel.WARN)
                    .eventType(UiTestLensEventType.ERROR)
                    .status(UiTestLensStatus.FAILED)
                    .message("Upload file input not found")
                    .action("upload")
                    .metadata("method", "smartUploadFile")
                    .build());
            return;
        }
        try {
            highlightActions.highlightClick(fileInput, "UPLOAD");
            fileInput.sendKeys(absoluteFilePath);
            if (config.isShowHudPanel()) {
                hudPanel.updateStep("File sent to <input type='file'>");
            }
            emit(UiTestLensLogEntry.builder()
                    .level(UiTestLensLogLevel.INFO)
                    .eventType(UiTestLensEventType.ACTION)
                    .status(UiTestLensStatus.PASSED)
                    .message("Upload file action passed")
                    .action("upload")
                    .metadata("method", "smartUploadFile")
                    .metadata("pathLength", absoluteFilePath == null ? "0" : String.valueOf(absoluteFilePath.length()))
                    .build());
        } catch (RuntimeException e) {
            emit(UiTestLensLogEntry.builder()
                    .level(UiTestLensLogLevel.ERROR)
                    .eventType(UiTestLensEventType.ERROR)
                    .status(UiTestLensStatus.FAILED)
                    .message("Upload file action failed")
                    .action("upload")
                    .metadata("method", "smartUploadFile")
                    .metadata("pathLength", absoluteFilePath == null ? "0" : String.valueOf(absoluteFilePath.length()))
                    .throwable(e)
                    .build());
            throw e;
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
        apiPanel.ensureOpen();
        String requestId = apiPanel.showRequest(title, method, url, payloadPreview);
        if (requestId == null) {
            throw new IllegalStateException("API modal requestId is null - modal not initialized correctly");
        }
        apiPanel.setPending(requestId, timeoutMs);

        long started = System.currentTimeMillis();
        try {
            T result = call.call();
            long elapsed = System.currentTimeMillis() - started;
            String body = responsePreview != null ? responsePreview.apply(result) : String.valueOf(result);
            apiPanel.setResponse(requestId, 200, elapsed, "", trimApiPreview(body));
            return result;
        } catch (Exception failure) {
            long elapsed = System.currentTimeMillis() - started;
            apiPanel.setError(requestId,
                    failure.getClass().getSimpleName() + " after " + elapsed + "ms",
                    trimApiPreview(stackTrace(failure)));
            throw new RuntimeException(failure);
        }
    }

    private static String trimApiPreview(String value) {
        if (value == null) {
            return "";
        }
        int maxLength = 7000;
        return value.length() > maxLength ? value.substring(0, maxLength) + "\n...(trimmed)" : value;
    }

    private static String stackTrace(Throwable failure) {
        java.io.StringWriter output = new java.io.StringWriter();
        failure.printStackTrace(new java.io.PrintWriter(output));
        return output.toString();
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
}

