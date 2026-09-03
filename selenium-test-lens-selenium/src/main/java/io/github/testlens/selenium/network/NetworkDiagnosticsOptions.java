package io.github.testlens.selenium.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/** Options for bounded manual or WebDriver BiDi network diagnostics collection. */
public final class NetworkDiagnosticsOptions {
    /** Default maximum number of captured request, response, and fetch-error events. */
    public static final int DEFAULT_MAX_CAPTURED_EVENTS = 10_000;

    private final NetworkCaptureMode captureMode;
    private final boolean includeHeaders;
    private final boolean maskSensitiveHeaders;
    private final int failedStatusThreshold;
    private final List<Pattern> ignoredUrlPatterns;
    private final int maxCapturedEvents;
    private final NetworkHudFilter hudFilter;

    private NetworkDiagnosticsOptions(Builder builder) {
        this.captureMode = builder.captureMode == null ? NetworkCaptureMode.MANUAL : builder.captureMode;
        this.includeHeaders = builder.includeHeaders;
        this.maskSensitiveHeaders = builder.maskSensitiveHeaders;
        this.failedStatusThreshold = builder.failedStatusThreshold <= 0 ? 400 : builder.failedStatusThreshold;
        this.ignoredUrlPatterns = Collections.unmodifiableList(new ArrayList<>(builder.ignoredUrlPatterns));
        this.maxCapturedEvents = builder.maxCapturedEvents;
        this.hudFilter = builder.hudFilter == null ? NetworkHudFilter.defaults() : builder.hudFilter;
    }

    public static NetworkDiagnosticsOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public NetworkCaptureMode captureMode() {
        return captureMode;
    }

    public boolean includeHeaders() {
        return includeHeaders;
    }

    public boolean maskSensitiveHeaders() {
        return maskSensitiveHeaders;
    }

    public int failedStatusThreshold() {
        return failedStatusThreshold;
    }

    public List<Pattern> ignoredUrlPatterns() {
        return ignoredUrlPatterns;
    }

    /** Maximum captured events retained before later events are counted as dropped. */
    public int maxCapturedEvents() {
        return maxCapturedEvents;
    }

    /** Presentation-only rules for raw network entries shown in the HUD. */
    public NetworkHudFilter hudFilter() { return hudFilter; }

    public boolean isIgnored(String url) {
        String safe = url == null ? "" : url;
        return ignoredUrlPatterns.stream().anyMatch(pattern -> pattern.matcher(safe).matches());
    }

    public static final class Builder {
        private NetworkCaptureMode captureMode = NetworkCaptureMode.MANUAL;
        private boolean includeHeaders;
        private boolean maskSensitiveHeaders = true;
        private int failedStatusThreshold = 400;
        private final List<Pattern> ignoredUrlPatterns = new ArrayList<>();
        private int maxCapturedEvents = DEFAULT_MAX_CAPTURED_EVENTS;
        private NetworkHudFilter hudFilter = NetworkHudFilter.defaults();

        private Builder() {}

        public Builder captureMode(NetworkCaptureMode captureMode) {
            this.captureMode = captureMode;
            return this;
        }

        public Builder includeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
            return this;
        }

        public Builder maskSensitiveHeaders(boolean maskSensitiveHeaders) {
            this.maskSensitiveHeaders = maskSensitiveHeaders;
            return this;
        }

        public Builder failedStatusThreshold(int failedStatusThreshold) {
            this.failedStatusThreshold = failedStatusThreshold;
            return this;
        }

        public Builder ignoreUrlPattern(String pattern) {
            if (pattern != null && !pattern.isBlank()) {
                ignoredUrlPatterns.add(Pattern.compile(pattern));
            }
            return this;
        }

        /** Sets the positive per-diagnostics event retention limit. */
        public Builder maxCapturedEvents(int value) {
            if (value <= 0) {
                throw new IllegalArgumentException("maxCapturedEvents must be positive");
            }
            this.maxCapturedEvents = value;
            return this;
        }

        /** Sets HUD-only filtering; {@code null} restores {@link NetworkHudFilter#defaults()}. */
        public Builder hudFilter(NetworkHudFilter value) {
            this.hudFilter = value == null ? NetworkHudFilter.defaults() : value;
            return this;
        }

        public NetworkDiagnosticsOptions build() {
            return new NetworkDiagnosticsOptions(this);
        }
    }
}

