package io.github.testlens.hud;

/** Selects how the HUD event-log scrollbar is rendered. */
public enum HudScrollbarStyle {
    /** Uses the browser and operating system scrollbar without Test Lens styling. */
    NATIVE,
    /** Uses the compact, low-contrast Test Lens scrollbar. */
    SUBTLE,
    /** Uses a slightly wider and more visible Test Lens scrollbar. */
    STANDARD
}
