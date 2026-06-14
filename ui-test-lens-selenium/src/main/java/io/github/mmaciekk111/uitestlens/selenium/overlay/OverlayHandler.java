package io.github.mmaciekk111.uitestlens.selenium.overlay;

import org.openqa.selenium.By;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OverlayHandler {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);

    private final String name;
    private final By detect;
    private final List<OverlayAction> actions;
    private final boolean optional;
    private final Duration timeout;
    private final boolean failIfStillVisible;

    private OverlayHandler(Builder builder) {
        this.name = requireName(builder.name);
        this.detect = Objects.requireNonNull(builder.detect, "detect must not be null");
        this.actions = List.copyOf(builder.actions);
        if (actions.isEmpty()) {
            throw new IllegalArgumentException("actions must not be empty");
        }
        this.optional = builder.optional;
        this.timeout = requirePositive(builder.timeout);
        this.failIfStillVisible = builder.failIfStillVisible;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public By detect() {
        return detect;
    }

    public List<OverlayAction> actions() {
        return actions;
    }

    public boolean optional() {
        return optional;
    }

    public Duration timeout() {
        return timeout;
    }

    public boolean failIfStillVisible() {
        return failIfStillVisible;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return name;
    }

    private static Duration requirePositive(Duration timeout) {
        Duration value = timeout != null ? timeout : DEFAULT_TIMEOUT;
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        return value;
    }

    public static final class Builder {
        private final String name;
        private By detect;
        private final List<OverlayAction> actions = new ArrayList<>();
        private boolean optional = true;
        private Duration timeout = DEFAULT_TIMEOUT;
        private boolean failIfStillVisible = false;

        private Builder(String name) {
            this.name = name;
        }

        public Builder detect(By detect) {
            this.detect = detect;
            return this;
        }

        public Builder action(OverlayAction action) {
            this.actions.add(Objects.requireNonNull(action, "action must not be null"));
            return this;
        }

        public Builder actions(List<OverlayAction> actions) {
            this.actions.clear();
            if (actions != null) {
                actions.forEach(this::action);
            }
            return this;
        }

        public Builder optional(boolean optional) {
            this.optional = optional;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder failIfStillVisible(boolean failIfStillVisible) {
            this.failIfStillVisible = failIfStillVisible;
            return this;
        }

        public OverlayHandler build() {
            return new OverlayHandler(this);
        }
    }
}
