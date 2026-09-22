package io.github.testlens.selenium.locator;

import io.github.testlens.selenium.actionability.ActionabilityOptions;

import java.time.Duration;

public final class UiLocatorOptions {
    private final ActionabilityOptions actionabilityOptions;
    private final Duration timeout;
    private final Duration pollInterval;
    private final int maxRetries;
    private final boolean retryOnStaleElement;
    private final boolean retryOnClickIntercepted;
    private final boolean retryOnNotInteractable;
    private final boolean highlightBeforeAction;
    private final boolean javascriptClickFallback;

    private UiLocatorOptions(Builder builder) {
        this.actionabilityOptions = builder.actionabilityOptions != null
                ? builder.actionabilityOptions
                : ActionabilityOptions.defaults();
        this.timeout = requirePositive(builder.timeout, "timeout");
        this.pollInterval = requirePositive(builder.pollInterval, "pollInterval");
        if (builder.maxRetries < 1) {
            throw new IllegalArgumentException("maxRetries must be at least 1");
        }
        this.maxRetries = builder.maxRetries;
        this.retryOnStaleElement = builder.retryOnStaleElement;
        this.retryOnClickIntercepted = builder.retryOnClickIntercepted;
        this.retryOnNotInteractable = builder.retryOnNotInteractable;
        this.highlightBeforeAction = builder.highlightBeforeAction;
        this.javascriptClickFallback = builder.javascriptClickFallback;
    }

    public static UiLocatorOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public ActionabilityOptions actionabilityOptions() {
        return actionabilityOptions;
    }

    public Duration timeout() {
        return timeout;
    }

    public Duration pollInterval() {
        return pollInterval;
    }

    public int maxRetries() {
        return maxRetries;
    }

    public boolean retryOnStaleElement() {
        return retryOnStaleElement;
    }

    public boolean retryOnClickIntercepted() {
        return retryOnClickIntercepted;
    }

    public boolean retryOnNotInteractable() {
        return retryOnNotInteractable;
    }

    public boolean highlightBeforeAction() {
        return highlightBeforeAction;
    }

    /**
     * Whether the standard locator click may use {@code HTMLElement.click()} after all bounded physical
     * strategies have proved that they did not dispatch a click.
     *
     * @return {@code true} by default
     * @since 0.4.0
     */
    public boolean javascriptClickFallback() {
        return javascriptClickFallback;
    }

    UiLocatorOptions withTimeout(Duration remainingTimeout) {
        return builder()
                .actionabilityOptions(actionabilityOptions)
                .timeout(remainingTimeout)
                .pollInterval(pollInterval.compareTo(remainingTimeout) > 0 ? remainingTimeout : pollInterval)
                .maxRetries(maxRetries)
                .retryOnStaleElement(retryOnStaleElement)
                .retryOnClickIntercepted(retryOnClickIntercepted)
                .retryOnNotInteractable(retryOnNotInteractable)
                .highlightBeforeAction(highlightBeforeAction)
                .javascriptClickFallback(javascriptClickFallback)
                .build();
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    public static final class Builder {
        private ActionabilityOptions actionabilityOptions = ActionabilityOptions.defaults();
        private Duration timeout = Duration.ofSeconds(3);
        private Duration pollInterval = Duration.ofMillis(100);
        private int maxRetries = 3;
        private boolean retryOnStaleElement = true;
        private boolean retryOnClickIntercepted = true;
        private boolean retryOnNotInteractable = true;
        private boolean highlightBeforeAction = true;
        private boolean javascriptClickFallback = true;

        private Builder() {
        }

        public Builder actionabilityOptions(ActionabilityOptions actionabilityOptions) {
            this.actionabilityOptions = actionabilityOptions;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder pollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder retryOnStaleElement(boolean retryOnStaleElement) {
            this.retryOnStaleElement = retryOnStaleElement;
            return this;
        }

        public Builder retryOnClickIntercepted(boolean retryOnClickIntercepted) {
            this.retryOnClickIntercepted = retryOnClickIntercepted;
            return this;
        }

        public Builder retryOnNotInteractable(boolean retryOnNotInteractable) {
            this.retryOnNotInteractable = retryOnNotInteractable;
            return this;
        }

        public Builder highlightBeforeAction(boolean highlightBeforeAction) {
            this.highlightBeforeAction = highlightBeforeAction;
            return this;
        }

        /**
         * Enables or disables the final JavaScript stage of the standard click cascade.
         * Physical strategies remain enabled when this is {@code false}.
         *
         * @since 0.4.0
         */
        public Builder javascriptClickFallback(boolean javascriptClickFallback) {
            this.javascriptClickFallback = javascriptClickFallback;
            return this;
        }

        public UiLocatorOptions build() {
            return new UiLocatorOptions(this);
        }
    }
}

