package io.github.testlens.selenium.evidence;

/** Selects how screenshot pixels behind a sensitive element are visually obscured. */
public enum VisualMaskMode {
    /** Covers the complete element rectangle with an opaque color. Recommended for secrets. */
    SOLID,
    /** Visually blurs the rectangle. This is obfuscation, not irreversible redaction. */
    BLUR
}
