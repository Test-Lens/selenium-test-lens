package io.github.testlens.hud;

/**
 * Selects which trusted image assets the HUD renders as branding.
 *
 * @since 0.3.0
 */
public enum HudBranding {
    /** Render only the bundled Test Lens mark. */
    TEST_LENS,
    /** Render only the caller-supplied bounded PNG. */
    CUSTOM,
    /** Render the Test Lens mark and caller-supplied bounded PNG as distinct assets. */
    BOTH,
    /** Render no branding. */
    NONE
}
