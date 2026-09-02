package io.github.testlens;

import io.github.testlens.core.trace.RetryOutcomePolicy;
import io.github.testlens.selenium.locator.UiLocatorOptions;

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

    private TestLensOptions(Builder builder) {
        this.overlayConfig = builder.overlayConfig == null ? OverlayConfig.builder().build() : builder.overlayConfig;
        this.locatorOptions = builder.locatorOptions == null ? UiLocatorOptions.defaults() : builder.locatorOptions;
        this.outputRoot = builder.outputRoot == null ? Path.of("target", "ui-test-lens") : builder.outputRoot;
        this.screenshotOnFailure = builder.screenshotOnFailure;
        this.cleanupHudOnFinish = builder.cleanupHudOnFinish;
        this.retryOutcomePolicy = builder.retryOutcomePolicy;
        this.allowedRetries = builder.allowedRetries;
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

    public static final class Builder {
        private OverlayConfig overlayConfig;
        private UiLocatorOptions locatorOptions;
        private Path outputRoot = Path.of("target", "ui-test-lens");
        private boolean screenshotOnFailure = true;
        private boolean cleanupHudOnFinish = true;
        private RetryOutcomePolicy retryOutcomePolicy = RetryOutcomePolicy.REPORT_ONLY;
        private int allowedRetries;
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
        public TestLensOptions build() { return new TestLensOptions(this); }
    }
}
