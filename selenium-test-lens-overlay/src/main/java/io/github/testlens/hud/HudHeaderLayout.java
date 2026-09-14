package io.github.testlens.hud;

/**
 * Controls how the atomic test-name and current-step items are arranged in the HUD header.
 *
 * @since 0.3.0
 */
public enum HudHeaderLayout {
    /** Keeps both items on one row when they fit and moves the complete step item to a second row otherwise. */
    AUTO,
    /** Keeps both items on one row and truncates values when necessary. */
    INLINE,
    /** Places the test name and current step on separate rows. */
    STACKED
}
