package io.github.testlens.selenium.evidence;

import org.openqa.selenium.By;

/** An immutable Selenium locator and its screenshot mask mode. */
public final class VisualMaskRule {
    private final By locator;
    private final VisualMaskMode mode;

    public VisualMaskRule(By locator, VisualMaskMode mode) {
        if (locator == null) throw new IllegalArgumentException("locator must not be null");
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        this.locator = locator;
        this.mode = mode;
    }

    public By locator() { return locator; }
    public VisualMaskMode mode() { return mode; }
}
