package io.github.testlens.selenium.business;

public final class BusinessAssertionOptions {
    private final boolean collectFailures;
    private final boolean failFast;
    private final boolean includeStackTrace;
    private final int messagePreviewLimit;

    private BusinessAssertionOptions(Builder builder) {
        this.collectFailures = builder.collectFailures;
        this.failFast = builder.failFast;
        this.includeStackTrace = builder.includeStackTrace;
        if (builder.messagePreviewLimit < 0) {
            throw new IllegalArgumentException("messagePreviewLimit must not be negative");
        }
        this.messagePreviewLimit = builder.messagePreviewLimit;
    }

    public static BusinessAssertionOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean collectFailures() {
        return collectFailures;
    }

    public boolean failFast() {
        return failFast;
    }

    public boolean includeStackTrace() {
        return includeStackTrace;
    }

    public int messagePreviewLimit() {
        return messagePreviewLimit;
    }

    public static final class Builder {
        private boolean collectFailures = true;
        private boolean failFast = false;
        private boolean includeStackTrace = false;
        private int messagePreviewLimit = 500;

        private Builder() {
        }

        public Builder collectFailures(boolean collectFailures) {
            this.collectFailures = collectFailures;
            return this;
        }

        public Builder failFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        public Builder includeStackTrace(boolean includeStackTrace) {
            this.includeStackTrace = includeStackTrace;
            return this;
        }

        public Builder messagePreviewLimit(int messagePreviewLimit) {
            this.messagePreviewLimit = messagePreviewLimit;
            return this;
        }

        public BusinessAssertionOptions build() {
            return new BusinessAssertionOptions(this);
        }
    }
}
