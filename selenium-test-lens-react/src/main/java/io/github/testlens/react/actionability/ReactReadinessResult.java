package io.github.testlens.react.actionability;

import io.github.testlens.selenium.actionability.ActionabilityStatus;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ReactReadinessResult {
    private final ReactReadinessCheckType checkType;
    private final ActionabilityStatus status;
    private final ReactReadinessFailureReason failureReason;
    private final String message;
    private final Duration elapsed;
    private final Map<String, Object> details;

    private ReactReadinessResult(Builder builder) {
        this.checkType = Objects.requireNonNull(builder.checkType, "checkType must not be null");
        this.status = Objects.requireNonNull(builder.status, "status must not be null");
        this.failureReason = builder.failureReason;
        this.message = builder.message == null ? "" : builder.message;
        this.elapsed = builder.elapsed == null ? Duration.ZERO : builder.elapsed;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(builder.details));
    }

    public static ReactReadinessResult ready(ReactReadinessCheckType checkType, String message, Duration elapsed) {
        return builder(checkType, ActionabilityStatus.READY)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static ReactReadinessResult notReady(ReactReadinessCheckType checkType,
                                                ReactReadinessFailureReason failureReason,
                                                String message,
                                                Duration elapsed) {
        return builder(checkType, ActionabilityStatus.NOT_READY)
                .failureReason(failureReason)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static ReactReadinessResult failed(ReactReadinessCheckType checkType,
                                              ReactReadinessFailureReason failureReason,
                                              String message,
                                              Duration elapsed) {
        return builder(checkType, ActionabilityStatus.FAILED)
                .failureReason(failureReason)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static ReactReadinessResult skipped(ReactReadinessCheckType checkType, String message, Duration elapsed) {
        return builder(checkType, ActionabilityStatus.SKIPPED)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static Builder builder(ReactReadinessCheckType checkType, ActionabilityStatus status) {
        return new Builder(checkType, status);
    }

    public ReactReadinessCheckType checkType() {
        return checkType;
    }

    public ActionabilityStatus status() {
        return status;
    }

    public ReactReadinessFailureReason failureReason() {
        return failureReason;
    }

    public String message() {
        return message;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public Map<String, Object> details() {
        return details;
    }

    public boolean ready() {
        return status == ActionabilityStatus.READY;
    }

    public static final class Builder {
        private final ReactReadinessCheckType checkType;
        private final ActionabilityStatus status;
        private ReactReadinessFailureReason failureReason;
        private String message;
        private Duration elapsed = Duration.ZERO;
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(ReactReadinessCheckType checkType, ActionabilityStatus status) {
            this.checkType = checkType;
            this.status = status;
        }

        public Builder failureReason(ReactReadinessFailureReason failureReason) {
            this.failureReason = failureReason;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder elapsed(Duration elapsed) {
            this.elapsed = elapsed;
            return this;
        }

        public Builder detail(String key, Object value) {
            if (key != null && !key.isBlank() && value != null) {
                details.put(key, value);
            }
            return this;
        }

        public Builder details(Map<String, Object> details) {
            if (details != null) {
                details.forEach(this::detail);
            }
            return this;
        }

        public ReactReadinessResult build() {
            return new ReactReadinessResult(this);
        }
    }
}
