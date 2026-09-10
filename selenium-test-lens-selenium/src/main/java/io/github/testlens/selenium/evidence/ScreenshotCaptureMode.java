package io.github.testlens.selenium.evidence;

/** Selects the area captured by the portable screenshot pipeline. */
public enum ScreenshotCaptureMode {
    /** Captures the currently visible browser viewport. */
    VIEWPORT,
    /**
     * Captures the initial top-level document dimensions by scrolling and stitching viewport images.
     * The mode preserves the current responsive viewport, does not expand frames or nested scroll containers,
     * and does not redact screenshot pixels.
     */
    FULL_PAGE
}
