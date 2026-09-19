package io.github.testlens.hud;

/**
 * Presentation format for timestamps in the browser HUD event log.
 *
 * <p>This setting affects only the HUD. Trace and JSON timestamps remain canonical UTC instants.
 *
 * @since 0.3.1
 */
public enum HudTimestampFormat {
    /** Canonical UTC representation retained for compatibility with earlier releases. */
    ISO_UTC,
    /** Time in the selected zone, formatted as {@code HH:mm:ss}. */
    TIME_ONLY,
    /** Date and time in the selected zone, formatted as {@code dd.MM.yy HH:mm:ss}. */
    DATE_TIME
}
