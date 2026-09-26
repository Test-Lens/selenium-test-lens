package io.github.testlens.selenium.locator;

import org.openqa.selenium.By;

import java.util.Objects;

public final class UiLocatorDescription {
    private final By by;
    private final String label;
    private final String locatorDisplay;
    private final String compactLocator;

    private UiLocatorDescription(By by, String label) {
        this.by = Objects.requireNonNull(by, "by must not be null");
        this.label = label == null ? "" : label.trim();
        this.locatorDisplay = LocatorObservationMetadata.display(by);
        this.compactLocator = LocatorObservationMetadata.compact(by, locatorDisplay);
    }

    public static UiLocatorDescription of(By by, String label) {
        return new UiLocatorDescription(by, label);
    }

    public By by() {
        return by;
    }

    public String label() {
        return label;
    }

    public String displayName() {
        String locator = locatorDisplay();
        return label.isBlank() ? locator : label + " (" + locator + ")";
    }

    String locatorDisplay() { return locatorDisplay; }

    String compactLocator() { return compactLocator; }

    @Override
    public String toString() {
        return displayName();
    }
}

