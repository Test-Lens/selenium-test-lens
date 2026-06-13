package io.github.mmaciekk111.uitestlens.core.logging;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record UiTestLensLogEntry(
        Instant timestamp,
        UiTestLensLogLevel level,
        UiTestLensEventType eventType,
        UiTestLensStatus status,
        String message,
        String step,
        String action,
        TargetDescriptor target,
        Map<String, String> metadata,
        Throwable throwable
) {
    public UiTestLensLogEntry {
        timestamp = timestamp != null ? timestamp : Instant.now();
        level = level != null ? level : UiTestLensLogLevel.INFO;
        eventType = eventType != null ? eventType : UiTestLensEventType.GENERAL;
        status = status != null ? status : UiTestLensStatus.INFO;
        message = message != null ? message : "";
        target = target != null ? target : TargetDescriptor.none();
        metadata = immutableCopy(metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static UiTestLensLogEntry info(String message) {
        return builder().level(UiTestLensLogLevel.INFO).status(UiTestLensStatus.INFO).message(message).build();
    }

    public static UiTestLensLogEntry warn(String message) {
        return builder().level(UiTestLensLogLevel.WARN).status(UiTestLensStatus.WARN).message(message).build();
    }

    public static UiTestLensLogEntry error(String message, Throwable throwable) {
        return builder()
                .level(UiTestLensLogLevel.ERROR)
                .eventType(UiTestLensEventType.ERROR)
                .status(UiTestLensStatus.FAILED)
                .message(message)
                .throwable(throwable)
                .build();
    }

    public Builder toBuilder() {
        return builder()
                .timestamp(timestamp)
                .level(level)
                .eventType(eventType)
                .status(status)
                .message(message)
                .step(step)
                .action(action)
                .target(target)
                .metadata(metadata)
                .throwable(throwable);
    }

    private static Map<String, String> immutableCopy(Map<String, String> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    public static final class Builder {
        private Instant timestamp;
        private UiTestLensLogLevel level = UiTestLensLogLevel.INFO;
        private UiTestLensEventType eventType = UiTestLensEventType.GENERAL;
        private UiTestLensStatus status = UiTestLensStatus.INFO;
        private String message = "";
        private String step;
        private String action;
        private TargetDescriptor target = TargetDescriptor.none();
        private Map<String, String> metadata = Map.of();
        private Throwable throwable;

        private Builder() {
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder level(UiTestLensLogLevel level) {
            this.level = level;
            return this;
        }

        public Builder eventType(UiTestLensEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder status(UiTestLensStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message != null ? message : "";
            return this;
        }

        public Builder step(String step) {
            this.step = step;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder target(TargetDescriptor target) {
            this.target = target;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = immutableCopy(metadata);
            return this;
        }

        public Builder metadata(String key, String value) {
            if (key == null || key.isBlank() || value == null) {
                return this;
            }
            Map<String, String> copy = new LinkedHashMap<>(this.metadata);
            copy.put(key, value);
            this.metadata = Collections.unmodifiableMap(copy);
            return this;
        }

        public Builder throwable(Throwable throwable) {
            this.throwable = throwable;
            return this;
        }

        public UiTestLensLogEntry build() {
            return new UiTestLensLogEntry(
                    timestamp,
                    level,
                    eventType,
                    status,
                    message,
                    step,
                    action,
                    target,
                    metadata,
                    throwable
            );
        }
    }
}
