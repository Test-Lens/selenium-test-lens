package io.github.mmaciekk111.uitestlens.selenium.actionability;

import java.time.Duration;

public final class ActionabilityOptions {
    private final Duration timeout;
    private final Duration pollInterval;
    private final boolean checkAttached;
    private final boolean checkVisible;
    private final boolean checkEnabled;
    private final boolean checkStableBounds;
    private final boolean scrollIntoView;
    private final boolean checkReceivesClickPoint;
    private final boolean checkOverlayPolicy;
    private final int stableBoundsSamples;
    private final Duration stableBoundsSampleDelay;

    private ActionabilityOptions(Builder builder) {
        this.timeout = requirePositive(builder.timeout, "timeout");
        this.pollInterval = requirePositive(builder.pollInterval, "pollInterval");
        this.checkAttached = builder.checkAttached;
        this.checkVisible = builder.checkVisible;
        this.checkEnabled = builder.checkEnabled;
        this.checkStableBounds = builder.checkStableBounds;
        this.scrollIntoView = builder.scrollIntoView;
        this.checkReceivesClickPoint = builder.checkReceivesClickPoint;
        this.checkOverlayPolicy = builder.checkOverlayPolicy;
        if (builder.stableBoundsSamples < 2) {
            throw new IllegalArgumentException("stableBoundsSamples must be at least 2");
        }
        this.stableBoundsSamples = builder.stableBoundsSamples;
        if (builder.stableBoundsSampleDelay == null || builder.stableBoundsSampleDelay.isNegative()) {
            throw new IllegalArgumentException("stableBoundsSampleDelay must not be negative");
        }
        this.stableBoundsSampleDelay = builder.stableBoundsSampleDelay;
    }

    public static ActionabilityOptions defaults() {
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

    public boolean checkAttached() {
        return checkAttached;
    }

    public boolean checkVisible() {
        return checkVisible;
    }

    public boolean checkEnabled() {
        return checkEnabled;
    }

    public boolean checkStableBounds() {
        return checkStableBounds;
    }

    public boolean scrollIntoView() {
        return scrollIntoView;
    }

    public boolean checkReceivesClickPoint() {
        return checkReceivesClickPoint;
    }

    public boolean checkOverlayPolicy() {
        return checkOverlayPolicy;
    }

    public int stableBoundsSamples() {
        return stableBoundsSamples;
    }

    public Duration stableBoundsSampleDelay() {
        return stableBoundsSampleDelay;
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
        private boolean checkAttached = true;
        private boolean checkVisible = true;
        private boolean checkEnabled = true;
        private boolean checkStableBounds = true;
        private boolean scrollIntoView = true;
        private boolean checkReceivesClickPoint = true;
        private boolean checkOverlayPolicy = true;
        private int stableBoundsSamples = 2;
        private Duration stableBoundsSampleDelay = Duration.ofMillis(100);

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

        public Builder checkAttached(boolean checkAttached) {
            this.checkAttached = checkAttached;
            return this;
        }

        public Builder checkVisible(boolean checkVisible) {
            this.checkVisible = checkVisible;
            return this;
        }

        public Builder checkEnabled(boolean checkEnabled) {
            this.checkEnabled = checkEnabled;
            return this;
        }

        public Builder checkStableBounds(boolean checkStableBounds) {
            this.checkStableBounds = checkStableBounds;
            return this;
        }

        public Builder scrollIntoView(boolean scrollIntoView) {
            this.scrollIntoView = scrollIntoView;
            return this;
        }

        public Builder checkReceivesClickPoint(boolean checkReceivesClickPoint) {
            this.checkReceivesClickPoint = checkReceivesClickPoint;
            return this;
        }

        public Builder checkOverlayPolicy(boolean checkOverlayPolicy) {
            this.checkOverlayPolicy = checkOverlayPolicy;
            return this;
        }

        public Builder stableBoundsSamples(int stableBoundsSamples) {
            this.stableBoundsSamples = stableBoundsSamples;
            return this;
        }

        public Builder stableBoundsSampleDelay(Duration stableBoundsSampleDelay) {
            this.stableBoundsSampleDelay = stableBoundsSampleDelay;
            return this;
        }

        public ActionabilityOptions build() {
            return new ActionabilityOptions(this);
        }
    }
}
