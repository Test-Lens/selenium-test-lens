package io.github.testlens;

import io.github.testlens.core.trace.RetryOutcomePolicy;
import io.github.testlens.core.trace.TraceRetentionOptions;
import io.github.testlens.core.trace.PassedTraceRetention;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import io.github.testlens.selenium.evidence.FailureBundleOptions;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.selenium.evidence.VisualRedactionOptions;

import java.nio.file.Path;

/** Consumer-level configuration. Defaults are suitable for local and CI execution. */
public final class TestLensOptions {
    private final OverlayConfig overlayConfig;
    private final UiLocatorOptions locatorOptions;
    private final Path outputRoot;
    private final boolean screenshotOnFailure;
    private final boolean cleanupHudOnFinish;
    private final RetryOutcomePolicy retryOutcomePolicy;
    private final int allowedRetries;
    private final FailureBundleOptions failureBundleOptions;
    private final RedactionPolicy redactionPolicy;
    private final VisualRedactionOptions visualRedaction;
    private final HighlightOptions highlightOptions;
    private final TraceRetentionOptions traceRetention;
    private final ObservabilityMode observabilityMode;
    private final EffectiveObservabilityPolicy effectiveObservability;

    private TestLensOptions(Builder builder) {
        OverlayConfig configuredOverlay = builder.overlayConfig == null ? OverlayConfig.builder().build() : builder.overlayConfig;
        OverlayConfig withHud = builder.hudOptions == null ? configuredOverlay : configuredOverlay.withHudOptions(builder.hudOptions);
        this.overlayConfig = builder.highlightOptions == null ? withHud : withHud.withHighlightOptions(builder.highlightOptions);
        this.highlightOptions = this.overlayConfig.getHighlightOptions();
        this.locatorOptions = builder.locatorOptions == null ? UiLocatorOptions.defaults() : builder.locatorOptions;
        this.outputRoot = builder.outputRoot == null ? Path.of("target", "ui-test-lens") : builder.outputRoot;
        this.screenshotOnFailure = builder.screenshotOnFailure;
        this.cleanupHudOnFinish = builder.cleanupHudOnFinish;
        this.retryOutcomePolicy = builder.retryOutcomePolicy;
        this.allowedRetries = builder.allowedRetries;
        this.failureBundleOptions = builder.failureBundleOptions == null
                ? FailureBundleOptions.defaults() : builder.failureBundleOptions;
        this.redactionPolicy = builder.redactionPolicy == null ? RedactionPolicy.defaults() : builder.redactionPolicy;
        this.visualRedaction = builder.visualRedaction == null
                ? VisualRedactionOptions.defaults() : builder.visualRedaction;
        this.traceRetention = builder.traceRetention == null
                ? TraceRetentionOptions.defaults() : builder.traceRetention;
        this.observabilityMode = ObservabilityModeResolver.resolve(builder.observabilityMode);
        this.effectiveObservability = EffectiveObservabilityPolicy.resolve(
                observabilityMode, overlayConfig, builder.traceRetentionExplicit, traceRetention);
    }

    public static TestLensOptions defaults() { return builder().build(); }
    public static Builder builder() { return new Builder(); }
    public OverlayConfig overlayConfig() { return overlayConfig; }
    public UiLocatorOptions locatorOptions() { return locatorOptions; }
    public Path outputRoot() { return outputRoot; }
    public boolean screenshotOnFailure() { return screenshotOnFailure; }
    public boolean cleanupHudOnFinish() { return cleanupHudOnFinish; }
    public RetryOutcomePolicy retryOutcomePolicy() { return retryOutcomePolicy; }
    public int allowedRetries() { return allowedRetries; }
    public FailureBundleOptions failureBundleOptions() { return failureBundleOptions; }
    public RedactionPolicy redactionPolicy() { return redactionPolicy; }
    /** Returns bounded trace-retention configuration. @since 0.4.0 */
    public TraceRetentionOptions traceRetention() { return traceRetention; }
    /** Returns the effective observability policy selected for this immutable options instance. @since 0.4.0 */
    public ObservabilityMode observabilityMode() { return observabilityMode; }
    EffectiveObservabilityPolicy effectiveObservability() { return effectiveObservability; }
    /**
     * Returns the HUD configuration used by the overlay.
     * @return effective immutable HUD options
     * @since 0.3.0
     */
    public HudOptions hud() { return overlayConfig.getHudOptions(); }
    /**
     * Returns the effective manual and automatic element-state decoration configuration.
     * @return effective highlight options after applying compatible legacy defaults
     * @since 0.3.1
     */
    public HighlightOptions highlights() { return highlightOptions; }
    /**
     * Returns element-state decoration configuration.
     * @return effective highlight options
     * @deprecated use {@link #highlights()}
     */
    @Deprecated(since = "0.3.1")
    public HighlightOptions highlight() { return highlights(); }
    /**
     * Returns screenshot-pixel redaction configuration.
     * @return visual redaction options; defaults automatically mask password inputs with STRICT handling
     * @since 0.3.0
     */
    public VisualRedactionOptions visualRedaction() { return visualRedaction; }

    public static final class Builder {
        private OverlayConfig overlayConfig;
        private UiLocatorOptions locatorOptions;
        private Path outputRoot = Path.of("target", "ui-test-lens");
        private boolean screenshotOnFailure = true;
        private boolean cleanupHudOnFinish = true;
        private RetryOutcomePolicy retryOutcomePolicy = RetryOutcomePolicy.REPORT_ONLY;
        private int allowedRetries;
        private FailureBundleOptions failureBundleOptions = FailureBundleOptions.defaults();
        private RedactionPolicy redactionPolicy = RedactionPolicy.defaults();
        private HudOptions hudOptions;
        private VisualRedactionOptions visualRedaction = VisualRedactionOptions.defaults();
        private HighlightOptions highlightOptions;
        private TraceRetentionOptions traceRetention = TraceRetentionOptions.defaults();
        private boolean traceRetentionExplicit;
        private ObservabilityMode observabilityMode;
        private Builder() {}
        public Builder overlayConfig(OverlayConfig value) { overlayConfig = value; return this; }
        public Builder locatorOptions(UiLocatorOptions value) { locatorOptions = value; return this; }
        public Builder outputRoot(Path value) { outputRoot = value; return this; }
        public Builder screenshotOnFailure(boolean value) { screenshotOnFailure = value; return this; }
        public Builder cleanupHudOnFinish(boolean value) { cleanupHudOnFinish = value; return this; }
        public Builder retryOutcomePolicy(RetryOutcomePolicy value) {
            retryOutcomePolicy = value == null ? RetryOutcomePolicy.REPORT_ONLY : value;
            return this;
        }
        public Builder allowedRetries(int value) {
            if (value < 0) throw new IllegalArgumentException("allowedRetries must not be negative");
            allowedRetries = value;
            return this;
        }
        public Builder failureBundleOptions(FailureBundleOptions value) {
            failureBundleOptions = value == null ? FailureBundleOptions.defaults() : value;
            return this;
        }
        public Builder redactionPolicy(RedactionPolicy value) {
            redactionPolicy = value == null ? RedactionPolicy.defaults() : value;
            return this;
        }
        /** Configures bounded trace retention; null restores defaults. @since 0.4.0 */
        public Builder traceRetention(TraceRetentionOptions value) {
            traceRetention = value == null ? TraceRetentionOptions.defaults() : value;
            traceRetentionExplicit = value != null;
            return this;
        }
        /**
         * Selects the observability cost policy. A null value clears the Java override and
         * restores property/environment resolution when {@link #build()} is called.
         * @since 0.4.0
         */
        public Builder observabilityMode(ObservabilityMode value) {
            observabilityMode = value;
            return this;
        }
        /**
         * Configures the browser HUD without exposing CSS internals.
         * @param value options; null restores {@link HudOptions#defaults()}
         * @return this builder
         * @since 0.3.0
         */
        public Builder hud(HudOptions value) { hudOptions = value == null ? HudOptions.defaults() : value; return this; }
        /**
         * Configures manual and automatic state decoration without changing HUD, redaction,
         * screenshot, or retry settings.
         * @param value immutable options; null restores defaults
         * @return this builder
         * @since 0.3.1
         */
        public Builder highlights(HighlightOptions value) {
            highlightOptions = value == null ? HighlightOptions.defaults() : value;
            return this;
        }
        /**
         * Configures manual and automatic state decoration.
         * @param value immutable options; null restores defaults
         * @return this builder
         * @deprecated use {@link #highlights(HighlightOptions)}
         */
        @Deprecated(since = "0.3.1")
        public Builder highlight(HighlightOptions value) { return highlights(value); }
        /**
         * Configures temporary browser-side masks for Test Lens screenshot pixels.
         * @param value options; null restores {@link VisualRedactionOptions#defaults()}
         * @return this builder
         * @since 0.3.0
         */
        public Builder visualRedaction(VisualRedactionOptions value) {
            visualRedaction = value == null ? VisualRedactionOptions.defaults() : value;
            return this;
        }
        public TestLensOptions build() { return new TestLensOptions(this); }
    }

    static final class EffectiveObservabilityPolicy {
        private final ObservabilityMode mode;
        private final boolean liveHud;
        private final boolean automaticFeedback;
        private final boolean liveSourceNavigation;
        private final TraceRetentionOptions traceRetention;

        private EffectiveObservabilityPolicy(ObservabilityMode mode, boolean liveHud,
                                             boolean automaticFeedback, boolean liveSourceNavigation,
                                             TraceRetentionOptions traceRetention) {
            this.mode = mode;
            this.liveHud = liveHud;
            this.automaticFeedback = automaticFeedback;
            this.liveSourceNavigation = liveSourceNavigation;
            this.traceRetention = traceRetention;
        }

        static EffectiveObservabilityPolicy resolve(ObservabilityMode mode, OverlayConfig overlay,
                                                     boolean traceExplicit, TraceRetentionOptions retention) {
            boolean source = overlay.getHudOptions().sourceNavigation().enabled();
            boolean fast = mode == ObservabilityMode.FAST;
            boolean liveHud = overlay.isEnabled() && (source
                    || (!fast && overlay.isShowHudPanel())
                    || (fast && overlay.isShowHudPanelExplicit() && overlay.isShowHudPanel()));
            HighlightOptions highlights = overlay.getHighlightOptions();
            boolean automatic = overlay.isEnabled() && highlights.enabled()
                    && (!fast || highlights.isAutomaticFeedbackExplicit())
                    && highlights.automaticFeedback();
            TraceRetentionOptions effectiveRetention = retention;
            if (fast && !traceExplicit) {
                effectiveRetention = TraceRetentionOptions.builder()
                        .maxEvents(retention.maxEvents())
                        .maxBytes(retention.maxBytes())
                        .maxEventBytes(retention.maxEventBytes())
                        .passedSessionRetention(PassedTraceRetention.SUMMARY_ONLY)
                        .build();
            }
            return new EffectiveObservabilityPolicy(mode, liveHud, automatic, source, effectiveRetention);
        }

        ObservabilityMode mode() { return mode; }
        boolean liveHud() { return liveHud; }
        boolean automaticFeedback() { return automaticFeedback; }
        boolean liveSourceNavigation() { return liveSourceNavigation; }
        TraceRetentionOptions traceRetention() { return traceRetention; }
    }
}
