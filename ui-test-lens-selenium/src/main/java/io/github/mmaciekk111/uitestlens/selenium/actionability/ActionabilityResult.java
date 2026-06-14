package io.github.mmaciekk111.uitestlens.selenium.actionability;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ActionabilityResult {
    private final ActionabilityCheckType checkType;
    private final ActionabilityStatus status;
    private final ActionabilityFailureReason failureReason;
    private final String message;
    private final Duration elapsed;
    private final String selectorDescription;
    private final String elementDescription;
    private final Map<String, Object> details;

    private ActionabilityResult(Builder builder) {
        this.checkType = Objects.requireNonNull(builder.checkType, "checkType must not be null");
        this.status = Objects.requireNonNull(builder.status, "status must not be null");
        this.failureReason = builder.failureReason;
        this.message = builder.message == null ? "" : builder.message;
        this.elapsed = builder.elapsed == null ? Duration.ZERO : builder.elapsed;
        this.selectorDescription = builder.selectorDescription;
        this.elementDescription = builder.elementDescription;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(builder.details));
    }

    public static ActionabilityResult ready(ActionabilityCheckType checkType, String message, Duration elapsed) {
        return builder(checkType, ActionabilityStatus.READY)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static ActionabilityResult notReady(ActionabilityCheckType checkType,
                                               ActionabilityFailureReason failureReason,
                                               String message,
                                               Duration elapsed) {
        return builder(checkType, ActionabilityStatus.NOT_READY)
                .failureReason(failureReason)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static ActionabilityResult failed(ActionabilityCheckType checkType,
                                             ActionabilityFailureReason failureReason,
                                             String message,
                                             Duration elapsed) {
        return builder(checkType, ActionabilityStatus.FAILED)
                .failureReason(failureReason)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static ActionabilityResult skipped(ActionabilityCheckType checkType, String message, Duration elapsed) {
        return builder(checkType, ActionabilityStatus.SKIPPED)
                .message(message)
                .elapsed(elapsed)
                .build();
    }

    public static Builder builder(ActionabilityCheckType checkType, ActionabilityStatus status) {
        return new Builder(checkType, status);
    }

    public ActionabilityCheckType checkType() {
        return checkType;
    }

    public ActionabilityStatus status() {
        return status;
    }

    public ActionabilityFailureReason failureReason() {
        return failureReason;
    }

    public String message() {
        return message;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public String selectorDescription() {
        return selectorDescription;
    }

    public String elementDescription() {
        return elementDescription;
    }

    public Map<String, Object> details() {
        return details;
    }

    public boolean ready() {
        return status == ActionabilityStatus.READY;
    }

    public static final class Builder {
        private final ActionabilityCheckType checkType;
        private final ActionabilityStatus status;
        private ActionabilityFailureReason failureReason;
        private String message;
        private Duration elapsed = Duration.ZERO;
        private String selectorDescription;
        private String elementDescription;
        private final Map<String, Object> details = new LinkedHashMap<>();

        private Builder(ActionabilityCheckType checkType, ActionabilityStatus status) {
            this.checkType = checkType;
            this.status = status;
        }

        public Builder failureReason(ActionabilityFailureReason failureReason) {
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

        public Builder selectorDescription(String selectorDescription) {
            this.selectorDescription = selectorDescription;
            return this;
        }

        public Builder elementDescription(String elementDescription) {
            this.elementDescription = elementDescription;
            return this;
        }

        public Builder detail(String key, Object value) {
            if (key != null && !key.isBlank() && value != null) {
                this.details.put(key, value);
            }
            return this;
        }

        public Builder details(Map<String, Object> details) {
            if (details != null) {
                details.forEach(this::detail);
            }
            return this;
        }

        public ActionabilityResult build() {
            return new ActionabilityResult(this);
        }
    }
}
