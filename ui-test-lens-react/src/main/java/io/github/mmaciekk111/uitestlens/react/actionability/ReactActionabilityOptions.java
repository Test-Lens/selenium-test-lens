package io.github.mmaciekk111.uitestlens.react.actionability;

import io.github.mmaciekk111.uitestlens.selenium.actionability.ActionabilityOptions;
import org.openqa.selenium.By;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReactActionabilityOptions {
    private final ActionabilityOptions baseOptions;
    private final boolean checkAriaDisabled;
    private final boolean checkAriaBusy;
    private final boolean checkDataLoading;
    private final boolean checkDataPending;
    private final boolean checkProgressbar;
    private final boolean checkSpinner;
    private final boolean checkSkeleton;
    private final boolean checkFocusLock;
    private final boolean checkDialogOrModal;
    private final Duration timeout;
    private final Duration pollInterval;
    private final List<By> customBusyIndicators;
    private final List<By> customBlockingOverlays;

    private ReactActionabilityOptions(Builder builder) {
        this.baseOptions = builder.baseOptions != null ? builder.baseOptions : ActionabilityOptions.defaults();
        this.checkAriaDisabled = builder.checkAriaDisabled;
        this.checkAriaBusy = builder.checkAriaBusy;
        this.checkDataLoading = builder.checkDataLoading;
        this.checkDataPending = builder.checkDataPending;
        this.checkProgressbar = builder.checkProgressbar;
        this.checkSpinner = builder.checkSpinner;
        this.checkSkeleton = builder.checkSkeleton;
        this.checkFocusLock = builder.checkFocusLock;
        this.checkDialogOrModal = builder.checkDialogOrModal;
        this.timeout = requirePositive(builder.timeout, "timeout");
        this.pollInterval = requirePositive(builder.pollInterval, "pollInterval");
        this.customBusyIndicators = immutableCopy(builder.customBusyIndicators);
        this.customBlockingOverlays = immutableCopy(builder.customBlockingOverlays);
    }

    public static ReactActionabilityOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public ActionabilityOptions baseOptions() {
        return baseOptions;
    }

    public boolean checkAriaDisabled() {
        return checkAriaDisabled;
    }

    public boolean checkAriaBusy() {
        return checkAriaBusy;
    }

    public boolean checkDataLoading() {
        return checkDataLoading;
    }

    public boolean checkDataPending() {
        return checkDataPending;
    }

    public boolean checkProgressbar() {
        return checkProgressbar;
    }

    public boolean checkSpinner() {
        return checkSpinner;
    }

    public boolean checkSkeleton() {
        return checkSkeleton;
    }

    public boolean checkFocusLock() {
        return checkFocusLock;
    }

    public boolean checkDialogOrModal() {
        return checkDialogOrModal;
    }

    public Duration timeout() {
        return timeout;
    }

    public Duration pollInterval() {
        return pollInterval;
    }

    public List<By> customBusyIndicators() {
        return customBusyIndicators;
    }

    public List<By> customBlockingOverlays() {
        return customBlockingOverlays;
    }

    private static Duration requirePositive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static List<By> immutableCopy(List<By> values) {
        return Collections.unmodifiableList(new ArrayList<>(values == null ? List.of() : values));
    }

    public static final class Builder {
        private ActionabilityOptions baseOptions = ActionabilityOptions.defaults();
        private boolean checkAriaDisabled = true;
        private boolean checkAriaBusy = true;
        private boolean checkDataLoading = true;
        private boolean checkDataPending = true;
        private boolean checkProgressbar = true;
        private boolean checkSpinner = true;
        private boolean checkSkeleton = true;
        private boolean checkFocusLock = true;
        private boolean checkDialogOrModal = true;
        private Duration timeout = Duration.ofSeconds(3);
        private Duration pollInterval = Duration.ofMillis(100);
        private final List<By> customBusyIndicators = new ArrayList<>();
        private final List<By> customBlockingOverlays = new ArrayList<>();

        private Builder() {
        }

        public Builder baseOptions(ActionabilityOptions baseOptions) {
            this.baseOptions = baseOptions;
            return this;
        }

        public Builder checkAriaDisabled(boolean checkAriaDisabled) {
            this.checkAriaDisabled = checkAriaDisabled;
            return this;
        }

        public Builder checkAriaBusy(boolean checkAriaBusy) {
            this.checkAriaBusy = checkAriaBusy;
            return this;
        }

        public Builder checkDataLoading(boolean checkDataLoading) {
            this.checkDataLoading = checkDataLoading;
            return this;
        }

        public Builder checkDataPending(boolean checkDataPending) {
            this.checkDataPending = checkDataPending;
            return this;
        }

        public Builder checkProgressbar(boolean checkProgressbar) {
            this.checkProgressbar = checkProgressbar;
            return this;
        }

        public Builder checkSpinner(boolean checkSpinner) {
            this.checkSpinner = checkSpinner;
            return this;
        }

        public Builder checkSkeleton(boolean checkSkeleton) {
            this.checkSkeleton = checkSkeleton;
            return this;
        }

        public Builder checkFocusLock(boolean checkFocusLock) {
            this.checkFocusLock = checkFocusLock;
            return this;
        }

        public Builder checkDialogOrModal(boolean checkDialogOrModal) {
            this.checkDialogOrModal = checkDialogOrModal;
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

        public Builder customBusyIndicator(By locator) {
            if (locator != null) {
                customBusyIndicators.add(locator);
            }
            return this;
        }

        public Builder customBlockingOverlay(By locator) {
            if (locator != null) {
                customBlockingOverlays.add(locator);
            }
            return this;
        }

        public ReactActionabilityOptions build() {
            return new ReactActionabilityOptions(this);
        }
    }
}
