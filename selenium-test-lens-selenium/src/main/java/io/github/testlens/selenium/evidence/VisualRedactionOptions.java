package io.github.testlens.selenium.evidence;

import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Configures browser-side masking of sensitive element pixels during screenshot capture.
 * This is separate from text-oriented {@code RedactionPolicy}.
 *
 * <p>Defaults mask password inputs with {@link VisualMaskMode#SOLID} and use the fail-closed
 * {@link VisualRedactionFailurePolicy#STRICT} policy. Masks apply to Test Lens screenshot pixels;
 * they do not sanitize page source, network data, video, or native browser UI.
 *
 * <pre>{@code
 * VisualRedactionOptions options = VisualRedactionOptions.builder()
 *         .mask(By.id("account-number"), VisualMaskMode.SOLID)
 *         .failurePolicy(VisualRedactionFailurePolicy.STRICT)
 *         .build();
 * }</pre>
 *
 * @since 0.3.0
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

    /**
     * Returns password-masking, fail-closed defaults.
     *
     * @return password-safe defaults
     * @since 0.3.0
     */
    public static VisualRedactionOptions defaults() { return builder().build(); }
    /**
     * Returns options with no automatic or explicit masks.
     *
     * @return disabled masking options
     * @since 0.3.0
     */
    public static VisualRedactionOptions disabled() { return builder().maskPasswordInputs(false).build(); }
    /**
     * Creates a password-masking builder using strict failure handling.
     *
     * @return new builder
     * @since 0.3.0
     */
    public static Builder builder() { return new Builder(); }
    /**
     * Reports whether password inputs are discovered and solid-masked.
     *
     * @return whether automatic password masking is enabled
     * @since 0.3.0
     */
    public boolean maskPasswordInputs() { return maskPasswordInputs; }
    /**
     * Returns the explicit locator rules.
     *
     * @return immutable explicit locator rules
     * @since 0.3.0
     */
    public List<VisualMaskRule> rules() { return rules; }
    /**
     * Returns the policy used when a required mask cannot be verified.
     *
     * @return mask failure policy
     * @since 0.3.0
     */
    public VisualRedactionFailurePolicy failurePolicy() { return failurePolicy; }
    /**
     * Returns the solid mask color.
     *
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String solidColor() { return solidColor; }
    /**
     * Returns the blur radius.
     *
     * @return radius in CSS pixels
     * @since 0.3.0
     */
    public int blurRadiusPx() { return blurRadiusPx; }
    /**
     * Returns the mask rectangle expansion.
     *
     * @return padding in CSS pixels
     * @since 0.3.0
     */
    public int paddingPx() { return paddingPx; }
    /**
     * Returns the optional plain-text mask label.
     *
     * @return label, or {@code null}
     * @since 0.3.0
     */
    public String maskLabel() { return maskLabel; }
    /**
     * Reports whether automatic or explicit masks are configured.
     *
     * @return whether masking is active
     * @since 0.3.0
     */
    public boolean hasMasks() { return maskPasswordInputs || !rules.isEmpty(); }

    /**
     * Builds validated screenshot-pixel masking options.
     *
     * @since 0.3.0
     */
    public static final class Builder {
        private boolean maskPasswordInputs = true;
        private final List<VisualMaskRule> rules = new ArrayList<>();
        private VisualRedactionFailurePolicy failurePolicy = VisualRedactionFailurePolicy.STRICT;
        private String solidColor = DEFAULT_SOLID_COLOR;
        private int blurRadiusPx = DEFAULT_BLUR_RADIUS_PX;
        private int paddingPx;
        private String maskLabel;

        private Builder() { }

        /**
         * Controls automatic solid masking of password inputs.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder maskPasswordInputs(boolean value) { maskPasswordInputs = value; return this; }
        /**
         * Adds an explicit required mask.
         *
         * @param locator Selenium locator
         * @param mode mask mode
         * @return this builder
         * @since 0.3.0
         */
        public Builder mask(By locator, VisualMaskMode mode) {
            rules.add(new VisualMaskRule(locator, mode));
            return this;
        }
        /**
         * Sets fail-closed or best-effort publication behavior.
         *
         * @param value policy
         * @return this builder
         * @since 0.3.0
         */
        public Builder failurePolicy(VisualRedactionFailurePolicy value) {
            if (value == null) throw new IllegalArgumentException("failurePolicy must not be null");
            failurePolicy = value;
            return this;
        }
        /**
         * Sets the solid mask color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder solidColor(String value) {
            if (value == null || !value.matches("(?i)#[0-9a-f]{6}")) {
                throw new IllegalArgumentException("solidColor must use #RRGGBB format");
            }
            solidColor = value.toUpperCase(Locale.ROOT);
            return this;
        }
        /**
         * Sets the blur radius from 2 through 32 CSS pixels.
         *
         * @param value radius
         * @return this builder
         * @since 0.3.0
         */
        public Builder blurRadiusPx(int value) {
            if (value < 2 || value > 32) throw new IllegalArgumentException("blurRadiusPx must be between 2 and 32");
            blurRadiusPx = value;
            return this;
        }
        /**
         * Expands mask rectangles by 0 through 32 CSS pixels.
         *
         * @param value padding
         * @return this builder
         * @since 0.3.0
         */
        public Builder paddingPx(int value) {
            if (value < 0 || value > 32) throw new IllegalArgumentException("paddingPx must be between 0 and 32");
            paddingPx = value;
            return this;
        }
        /**
         * Sets an optional label rendered with {@code textContent}; blank or null disables it.
         * @param value untrusted plain text, never HTML
         * @return this builder
         * @since 0.3.0
         */
        public Builder maskLabel(String value) {
            maskLabel = value == null || value.isBlank() ? null : value;
            return this;
        }
        /**
         * Builds immutable visual-redaction options.
         *
         * @return immutable options
         * @since 0.3.0
         */
        public VisualRedactionOptions build() { return new VisualRedactionOptions(this); }
    }
}
