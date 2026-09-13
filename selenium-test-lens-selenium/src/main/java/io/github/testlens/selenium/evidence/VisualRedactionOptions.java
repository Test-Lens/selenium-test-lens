package io.github.testlens.selenium.evidence;

import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Configures browser-side masking of sensitive element pixels during screenshot capture.
 * This is separate from text-oriented {@code RedactionPolicy}.
 */
public final class VisualRedactionOptions {
    /** Default opaque mask color. */
    public static final String DEFAULT_SOLID_COLOR = "#2B2F36";
    /** Default blur radius in CSS pixels. */
    public static final int DEFAULT_BLUR_RADIUS_PX = 8;
    private final boolean maskPasswordInputs;
    private final List<VisualMaskRule> rules;
    private final VisualRedactionFailurePolicy failurePolicy;
    private final String solidColor;
    private final int blurRadiusPx;
    private final int paddingPx;
    private final String maskLabel;

    private VisualRedactionOptions(Builder builder) {
        maskPasswordInputs = builder.maskPasswordInputs;
        rules = List.copyOf(builder.rules);
        failurePolicy = builder.failurePolicy;
        solidColor = builder.solidColor;
        blurRadiusPx = builder.blurRadiusPx;
        paddingPx = builder.paddingPx;
        maskLabel = builder.maskLabel;
    }

    public static VisualRedactionOptions defaults() { return builder().build(); }
    public static VisualRedactionOptions disabled() { return builder().maskPasswordInputs(false).build(); }
    public static Builder builder() { return new Builder(); }
    public boolean maskPasswordInputs() { return maskPasswordInputs; }
    public List<VisualMaskRule> rules() { return rules; }
    public VisualRedactionFailurePolicy failurePolicy() { return failurePolicy; }
    public String solidColor() { return solidColor; }
    public int blurRadiusPx() { return blurRadiusPx; }
    public int paddingPx() { return paddingPx; }
    /** Returns the optional plain-text label, or {@code null}. */
    public String maskLabel() { return maskLabel; }
    public boolean hasMasks() { return maskPasswordInputs || !rules.isEmpty(); }

    public static final class Builder {
        private boolean maskPasswordInputs = true;
        private final List<VisualMaskRule> rules = new ArrayList<>();
        private VisualRedactionFailurePolicy failurePolicy = VisualRedactionFailurePolicy.STRICT;
        private String solidColor = DEFAULT_SOLID_COLOR;
        private int blurRadiusPx = DEFAULT_BLUR_RADIUS_PX;
        private int paddingPx;
        private String maskLabel;

        private Builder() { }

        public Builder maskPasswordInputs(boolean value) { maskPasswordInputs = value; return this; }
        public Builder mask(By locator, VisualMaskMode mode) {
            rules.add(new VisualMaskRule(locator, mode));
            return this;
        }
        public Builder failurePolicy(VisualRedactionFailurePolicy value) {
            if (value == null) throw new IllegalArgumentException("failurePolicy must not be null");
            failurePolicy = value;
            return this;
        }
        public Builder solidColor(String value) {
            if (value == null || !value.matches("(?i)#[0-9a-f]{6}")) {
                throw new IllegalArgumentException("solidColor must use #RRGGBB format");
            }
            solidColor = value.toUpperCase(Locale.ROOT);
            return this;
        }
        public Builder blurRadiusPx(int value) {
            if (value < 2 || value > 32) throw new IllegalArgumentException("blurRadiusPx must be between 2 and 32");
            blurRadiusPx = value;
            return this;
        }
        public Builder paddingPx(int value) {
            if (value < 0 || value > 32) throw new IllegalArgumentException("paddingPx must be between 0 and 32");
            paddingPx = value;
            return this;
        }
        /** Sets an optional label rendered with {@code textContent}; blank or null disables it. */
        public Builder maskLabel(String value) {
            maskLabel = value == null || value.isBlank() ? null : value;
            return this;
        }
        public VisualRedactionOptions build() { return new VisualRedactionOptions(this); }
    }
}
