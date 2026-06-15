package io.github.testlens.selenium.locator;

public final class UiLocatorException extends RuntimeException {
    private final String action;
    private final String locatorDescription;
    private final String actionabilitySummary;

    public UiLocatorException(String action, String locatorDescription, String message, Throwable cause, String actionabilitySummary) {
        super(message, cause);
        this.action = action == null ? "" : action;
        this.locatorDescription = locatorDescription == null ? "" : locatorDescription;
        this.actionabilitySummary = actionabilitySummary == null ? "" : actionabilitySummary;
    }

    public String action() {
        return action;
    }

    public String locatorDescription() {
        return locatorDescription;
    }

    public String actionabilitySummary() {
        return actionabilitySummary;
    }
}

