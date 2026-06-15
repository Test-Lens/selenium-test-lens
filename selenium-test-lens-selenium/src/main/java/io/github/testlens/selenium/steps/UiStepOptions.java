package io.github.testlens.selenium.steps;

import io.github.testlens.selenium.evidence.ScreenshotCaptureOptions;

public final class UiStepOptions {
    private final boolean failFast;
    private final boolean logToHud;
    private final boolean captureNestedEvents;
    private final boolean includeStackTrace;
    private final boolean captureScreenshotOnFailure;
    private final ScreenshotCaptureOptions screenshotCaptureOptions;
    private final int messagePreviewLimit;

    private UiStepOptions(Builder builder) {
        this.failFast = builder.failFast;
        this.logToHud = builder.logToHud;
        this.captureNestedEvents = builder.captureNestedEvents;
        this.includeStackTrace = builder.includeStackTrace;
        this.captureScreenshotOnFailure = builder.captureScreenshotOnFailure;
        this.screenshotCaptureOptions = builder.screenshotCaptureOptions == null
                ? ScreenshotCaptureOptions.defaults()
                : builder.screenshotCaptureOptions;
        if (builder.messagePreviewLimit < 0) {
            throw new IllegalArgumentException("messagePreviewLimit must not be negative");
        }
        this.messagePreviewLimit = builder.messagePreviewLimit;
    }

    public static UiStepOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean failFast() {
        return failFast;
    }

    public boolean logToHud() {
        return logToHud;
    }

    public boolean captureNestedEvents() {
        return captureNestedEvents;
    }

    public boolean includeStackTrace() {
        return includeStackTrace;
    }

    public boolean captureScreenshotOnFailure() {
        return captureScreenshotOnFailure;
    }

    public ScreenshotCaptureOptions screenshotCaptureOptions() {
        return screenshotCaptureOptions;
    }

    public int messagePreviewLimit() {
        return messagePreviewLimit;
    }

    public static final class Builder {
        private boolean failFast = true;
        private boolean logToHud = true;
        private boolean captureNestedEvents = true;
        private boolean includeStackTrace = false;
        private boolean captureScreenshotOnFailure = false;
        private ScreenshotCaptureOptions screenshotCaptureOptions = ScreenshotCaptureOptions.defaults();
        private int messagePreviewLimit = 500;

        private Builder() {
        }

        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public Builder logToHud(boolean logToHud) {
            this.logToHud = logToHud;
            return this;
        }

        public Builder captureNestedEvents(boolean captureNestedEvents) {
            this.captureNestedEvents = captureNestedEvents;
            return this;
        }

        public Builder includeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
            return this;
        }

        public Builder captureScreenshotOnFailure(boolean captureScreenshotOnFailure) {
            this.captureScreenshotOnFailure = captureScreenshotOnFailure;
            return this;
        }

        public Builder screenshotCaptureOptions(ScreenshotCaptureOptions screenshotCaptureOptions) {
            this.screenshotCaptureOptions = screenshotCaptureOptions;
            return this;
        }

        public Builder messagePreviewLimit(int messagePreviewLimit) {
            this.messagePreviewLimit = messagePreviewLimit;
            return this;
        }

        public UiStepOptions build() {
            return new UiStepOptions(this);
        }
    }
}

