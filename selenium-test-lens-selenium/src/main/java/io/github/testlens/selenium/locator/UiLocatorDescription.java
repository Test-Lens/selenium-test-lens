package io.github.testlens.selenium.locator;

import org.openqa.selenium.By;

import java.util.Objects;

public final class UiLocatorDescription {
    private final By by;
    private final String label;

    private UiLocatorDescription(By by, String label) {
        this.by = Objects.requireNonNull(by, "by must not be null");
        this.label = label == null ? "" : label.trim();
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
        return label.isBlank() ? by.toString() : label + " (" + by + ")";
    }

    @Override
    public String toString() {
        return displayName();
    }
}

