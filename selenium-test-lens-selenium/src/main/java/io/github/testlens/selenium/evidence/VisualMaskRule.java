package io.github.testlens.selenium.evidence;

import org.openqa.selenium.By;

/**
 * An immutable Selenium locator and its screenshot mask mode. The locator is resolved in the
 * current Selenium browsing context immediately before screenshot capture.
 *
 * @since 0.3.0
 */
public final class VisualMaskRule {
    private final By locator;
    private final VisualMaskMode mode;

    /**
     * Creates a required visual mask rule.
     * @param locator locator resolved for each capture or full-page tile
     * @param mode pixel masking mode
     * @since 0.3.0
     */
    public VisualMaskRule(By locator, VisualMaskMode mode) {
        if (locator == null) throw new IllegalArgumentException("locator must not be null");
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        this.locator = locator;
        this.mode = mode;
    }

    /**
     * Returns the Selenium locator whose element bounds are masked.
     *
     * @return Selenium locator
     * @since 0.3.0
     */
    public By locator() { return locator; }
    /**
     * Returns the requested mask mode.
     *
     * @return mask mode
     * @since 0.3.0
     */
    public VisualMaskMode mode() { return mode; }
}
