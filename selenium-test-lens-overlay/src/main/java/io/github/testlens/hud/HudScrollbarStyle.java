package io.github.testlens.hud;

/**
 * Selects how the HUD event-log scrollbar is rendered. Chromium uses the configured pixel width,
 * while Firefox maps custom styles to its supported scrollbar-width values and applies the colors.
 *
 * @since 0.3.0
 */
public enum HudScrollbarStyle {
    /** Uses the browser and operating system scrollbar without Test Lens styling. */
    NATIVE,
    /** Uses the compact, low-contrast Test Lens scrollbar. */
    SUBTLE,
    /** Uses a slightly wider and more visible Test Lens scrollbar. */
    STANDARD
}
