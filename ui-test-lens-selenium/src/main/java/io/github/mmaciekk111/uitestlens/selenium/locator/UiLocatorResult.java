package io.github.mmaciekk111.uitestlens.selenium.locator;

import java.time.Duration;

public final class UiLocatorResult {
    private final UiLocatorStatus status;
    private final UiLocatorFailureReason failureReason;
    private final String action;
    private final String description;
    private final int attempts;
    private final Duration elapsed;
    private final String message;

    private UiLocatorResult(Builder builder) {
        this.status = builder.status;
        this.failureReason = builder.failureReason;
        this.action = builder.action == null ? "" : builder.action;
        this.description = builder.description == null ? "" : builder.description;
        this.attempts = builder.attempts;
        this.elapsed = builder.elapsed == null ? Duration.ZERO : builder.elapsed;
        this.message = builder.message == null ? "" : builder.message;
    }

    public static Builder builder(UiLocatorStatus status) {
        return new Builder(status);
    }

    public UiLocatorStatus status() {
        return status;
    }

    public UiLocatorFailureReason failureReason() {
        return failureReason;
    }

    public String action() {
        return action;
    }

    public String description() {
        return description;
    }

    public int attempts() {
        return attempts;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public String message() {
        return message;
    }

    public boolean passed() {
        return status == UiLocatorStatus.PASSED;
    }

    public static final class Builder {
        private final UiLocatorStatus status;
        private UiLocatorFailureReason failureReason;
        private String action;
        private String description;
        private int attempts;
        private Duration elapsed = Duration.ZERO;
        private String message;

        private Builder(UiLocatorStatus status) {
            this.status = status;
        }

        public Builder failureReason(UiLocatorFailureReason failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder attempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public Builder elapsed(Duration elapsed) {
            this.elapsed = elapsed;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public UiLocatorResult build() {
            return new UiLocatorResult(this);
        }
    }
}
