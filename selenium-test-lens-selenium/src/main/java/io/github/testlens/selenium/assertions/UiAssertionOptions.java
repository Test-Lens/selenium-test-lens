package io.github.testlens.selenium.assertions;

import java.time.Duration;

public final class UiAssertionOptions {
    private final Duration timeout;
    private final Duration pollInterval;
    private final boolean normalizeWhitespace;
    private final boolean caseSensitive;
    private final int actualTextPreviewLimit;
    private final boolean trimText;
    private final boolean failFastOnMissingElement;

    private UiAssertionOptions(Builder builder) {
        this.timeout = requirePositive(builder.timeout, "timeout");
        this.pollInterval = requirePositive(builder.pollInterval, "pollInterval");
        this.normalizeWhitespace = builder.normalizeWhitespace;
        this.caseSensitive = builder.caseSensitive;
        if (builder.actualTextPreviewLimit < 0) {
            throw new IllegalArgumentException("actualTextPreviewLimit must not be negative");
        }
        this.actualTextPreviewLimit = builder.actualTextPreviewLimit;
        this.trimText = builder.trimText;
        this.failFastOnMissingElement = builder.failFastOnMissingElement;
    }

    public static UiAssertionOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Duration timeout() {
        return timeout;
    }

    public Duration pollInterval() {
        return pollInterval;
    }

    public boolean normalizeWhitespace() {
        return normalizeWhitespace;
    }

    public boolean caseSensitive() {
        return caseSensitive;
    }

    public int actualTextPreviewLimit() {
        return actualTextPreviewLimit;
    }

    public boolean trimText() {
        return trimText;
    }

    public boolean failFastOnMissingElement() {
        return failFastOnMissingElement;
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public static final class Builder {
        private Duration timeout = Duration.ofSeconds(3);
        private Duration pollInterval = Duration.ofMillis(100);
        private boolean normalizeWhitespace = true;
        private boolean caseSensitive = true;
        private int actualTextPreviewLimit = 300;
        private boolean trimText = true;
        private boolean failFastOnMissingElement = false;

        private Builder() {
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder pollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        public Builder normalizeWhitespace(boolean normalizeWhitespace) {
            this.normalizeWhitespace = normalizeWhitespace;
            return this;
        }

        public Builder caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        public Builder actualTextPreviewLimit(int actualTextPreviewLimit) {
            this.actualTextPreviewLimit = actualTextPreviewLimit;
            return this;
        }

        public Builder trimText(boolean trimText) {
            this.trimText = trimText;
            return this;
        }

        public Builder failFastOnMissingElement(boolean failFastOnMissingElement) {
            this.failFastOnMissingElement = failFastOnMissingElement;
            return this;
        }

        public UiAssertionOptions build() {
            return new UiAssertionOptions(this);
        }
    }
}

