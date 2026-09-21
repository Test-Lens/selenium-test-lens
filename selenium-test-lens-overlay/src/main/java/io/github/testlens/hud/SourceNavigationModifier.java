package io.github.testlens.hud;

/** Keyboard shortcut used to activate source links in the HUD. @since 0.3.1 */
public enum SourceNavigationModifier {
    /** Toggles source navigation; Escape disables it. @since 0.4.0 */
    F8,
    /** Preserves the legacy behavior: source navigation is active only while Ctrl+Alt is held. @since 0.3.1 */
    @Deprecated(since = "0.4.0")
    CTRL_ALT
}
