package io.github.mmaciekk111.uitestlens.selenium.actionability;

import java.util.Objects;

public final class ActionabilityCheck {
    private final ActionabilityCheckType type;
    private final boolean enabled;

    private ActionabilityCheck(ActionabilityCheckType type, boolean enabled) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.enabled = enabled;
    }

    public static ActionabilityCheck enabled(ActionabilityCheckType type) {
        return new ActionabilityCheck(type, true);
    }

    public static ActionabilityCheck skipped(ActionabilityCheckType type) {
        return new ActionabilityCheck(type, false);
    }

    public ActionabilityCheckType type() {
        return type;
    }

    public boolean enabled() {
        return enabled;
    }
}
