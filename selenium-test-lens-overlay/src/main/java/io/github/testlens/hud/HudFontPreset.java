package io.github.testlens.hud;

/**
 * Selects a local font stack used by the HUD without loading remote font resources.
 *
 * @since 0.3.0
 */
public enum HudFontPreset {
    /** Native system UI fonts. */
    SYSTEM,
    /** Native monospace fonts. */
    MONOSPACE,
    /** Test Lens UI-oriented sans-serif stack with local fallbacks only. */
    UI_SANS
}
