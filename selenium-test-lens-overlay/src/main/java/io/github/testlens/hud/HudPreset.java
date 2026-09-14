package io.github.testlens.hud;

/**
 * Product-level HUD presets that combine content visibility and panel density.
 * Explicit {@link HudOptions.Builder} overrides take precedence over the selected preset.
 *
 * @since 0.3.0
 */
public enum HudPreset {
    /** Current-step context without an event log. */
    MINIMAL,
    /** Product default with compact context and an event log. */
    COMPACT,
    /** Fuller timestamped event view. */
    STANDARD,
    /** Expanded diagnostic view including pipeline and timestamps. */
    DEBUG
}
