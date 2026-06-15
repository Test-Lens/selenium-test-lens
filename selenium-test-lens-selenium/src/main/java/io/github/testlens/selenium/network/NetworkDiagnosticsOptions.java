package io.github.testlens.selenium.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public final class NetworkDiagnosticsOptions {
    private final NetworkCaptureMode captureMode;
    private final boolean includeHeaders;
    private final boolean maskSensitiveHeaders;
    private final int failedStatusThreshold;
    private final List<Pattern> ignoredUrlPatterns;
    private final boolean attachToSession;

    private NetworkDiagnosticsOptions(Builder builder) {
        this.captureMode = builder.captureMode == null ? NetworkCaptureMode.AUTO : builder.captureMode;
        this.includeHeaders = builder.includeHeaders;
        this.maskSensitiveHeaders = builder.maskSensitiveHeaders;
        this.failedStatusThreshold = builder.failedStatusThreshold <= 0 ? 400 : builder.failedStatusThreshold;
        this.ignoredUrlPatterns = Collections.unmodifiableList(new ArrayList<>(builder.ignoredUrlPatterns));
        this.attachToSession = builder.attachToSession;
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

    public boolean attachToSession() {
        return attachToSession;
    }

    public boolean isIgnored(String url) {
        String safe = url == null ? "" : url;
        return ignoredUrlPatterns.stream().anyMatch(pattern -> pattern.matcher(safe).matches());
    }

    public static final class Builder {
        private NetworkCaptureMode captureMode = NetworkCaptureMode.AUTO;
        private boolean includeHeaders;
        private boolean maskSensitiveHeaders = true;
        private int failedStatusThreshold = 400;
        private final List<Pattern> ignoredUrlPatterns = new ArrayList<>();
        private boolean attachToSession = true;

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

        public Builder attachToSession(boolean attachToSession) {
            this.attachToSession = attachToSession;
            return this;
        }

        public NetworkDiagnosticsOptions build() {
            return new NetworkDiagnosticsOptions(this);
        }
    }
}

