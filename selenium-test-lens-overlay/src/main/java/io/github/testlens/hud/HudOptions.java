package io.github.testlens.hud;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Immutable product-level configuration for the browser HUD.
 *
 * <p>The default is {@link HudPreset#COMPACT}. A preset supplies base values and every explicit
 * builder override wins regardless of call order. Colors accept only six-digit hexadecimal
 * values. Scrollbar styling is limited to product presets, a bounded width, and validated
 * colors; this API does not accept arbitrary CSS or HTML.
 *
 * <pre>{@code
 * HudOptions hud = HudOptions.builder()
 *         .preset(HudPreset.COMPACT)
 *         .position(HudPosition.TOP_RIGHT)
 *         .showNetwork(false)
 *         .build();
 * }</pre>
 *
 * @since 0.3.0
 */
public final class HudOptions {
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final int MAX_LOGO_BYTES = 1_048_576;
    private static final int MAX_LOGO_DIMENSION = 4096;
    private static final long MAX_LOGO_PIXELS = 16_777_216L;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

    private final HudPreset preset;
    private final HudPosition position;
    private final HudHeaderLayout headerLayout;
    private final int offsetXPx;
    private final int offsetYPx;
    private final int widthPx;
    private final int maxHeightPx;
    private final int maxLogHeightPx;
    private final int railWidthPx;
    private final HudFontPreset fontPreset;
    private final HudTypography typography;
    private final HudScrollbarStyle scrollbarStyle;
    private final int scrollbarWidthPx;
    private final String scrollbarTrackColor;
    private final String scrollbarThumbColor;
    private final String scrollbarThumbHoverColor;
    private final int baseFontSizePx;
    private final int headerFontSizePx;
    private final boolean showTestName;
    private final boolean showCurrentStep;
    private final boolean showPipeline;
    private final boolean showTimestamps;
    private final HudTimestampFormat timestampFormat;
    private final ZoneId timestampZone;
    private final boolean showEventLog;
    private final boolean showNetwork;
    private final boolean showRetries;
    private final boolean showWaits;
    private final boolean showAssertions;
    private final HudBranding branding;
    private final HudLogoPlacement logoPlacement;
    private final String background;
    private final double backgroundOpacity;
    private final String accentColor;
    private final String primaryTextColor;
    private final String mutedTextColor;
    private final String successColor;
    private final String warningColor;
    private final String failureColor;
    private final String customLogoFileName;
    private final String customLogoDataUri;

    private HudOptions(Builder builder) {
        this.preset = builder.preset;
        this.position = builder.position;
        this.headerLayout = builder.headerLayout;
        this.offsetXPx = builder.offsetXPx;
        this.offsetYPx = builder.offsetYPx;
        this.widthPx = builder.widthPx;
        this.maxHeightPx = builder.maxHeightPx;
        this.maxLogHeightPx = builder.maxLogHeightPx;
        this.railWidthPx = builder.railWidthPx;
        this.fontPreset = builder.fontPreset;
        this.typography = builder.typography;
        this.scrollbarStyle = builder.scrollbarStyle;
        this.scrollbarWidthPx = builder.scrollbarWidthPx;
        this.scrollbarTrackColor = builder.scrollbarTrackColor;
        this.scrollbarThumbColor = builder.scrollbarThumbColor;
        this.scrollbarThumbHoverColor = builder.scrollbarThumbHoverColor;
        this.baseFontSizePx = builder.baseFontSizePx;
        this.headerFontSizePx = builder.headerFontSizePx;
        this.showTestName = builder.showTestName;
        this.showCurrentStep = builder.showCurrentStep;
        this.showPipeline = builder.showPipeline;
        this.showTimestamps = builder.showTimestamps;
        this.timestampFormat = builder.timestampFormat;
        this.timestampZone = builder.timestampZone;
        this.showEventLog = builder.showEventLog;
        this.showNetwork = builder.showNetwork;
        this.showRetries = builder.showRetries;
        this.showWaits = builder.showWaits;
        this.showAssertions = builder.showAssertions;
        this.branding = builder.branding;
        this.logoPlacement = builder.logoPlacement;
        this.background = builder.background;
        this.backgroundOpacity = builder.backgroundOpacity;
        this.accentColor = builder.accentColor;
        this.primaryTextColor = builder.primaryTextColor;
        this.mutedTextColor = builder.mutedTextColor;
        this.successColor = builder.successColor;
        this.warningColor = builder.warningColor;
        this.failureColor = builder.failureColor;
        this.customLogoFileName = builder.customLogoFileName;
        this.customLogoDataUri = builder.customLogoDataUri;
        if ((branding == HudBranding.CUSTOM || branding == HudBranding.BOTH) && customLogoDataUri == null) {
            throw new IllegalStateException("A PNG custom logo is required for " + branding + " branding");
        }
        if ((branding == HudBranding.TEST_LENS || branding == HudBranding.NONE) && customLogoDataUri != null) {
            throw new IllegalStateException("A custom logo requires CUSTOM or BOTH branding");
        }
    }

    /**
     * Returns the product default.
     * @return options based on {@link HudPreset#COMPACT}
     * @since 0.3.0
     */
    public static HudOptions defaults() { return builder().build(); }
    /**
     * Returns a new product-level HUD builder.
     * @return a builder based on {@link HudPreset#COMPACT}
     * @since 0.3.0
     */
    public static Builder builder() { return new Builder(); }
    /**
     * Copies the effective configuration into explicit builder overrides.
     * @return a builder initialized from this instance
     * @since 0.3.0
     */
    public Builder toBuilder() { return new Builder(this); }
    /**
     * Returns the preset used as the base configuration.
     * @return selected preset
     * @since 0.3.0
     */
    public HudPreset preset() { return preset; }
    /**
     * Returns the viewport-corner anchor.
     * @return selected position
     * @since 0.3.0
     */
    public HudPosition position() { return position; }
    /**
     * Returns the responsive arrangement of test-name and current-step header items.
     * @return header layout
     * @since 0.3.0
     */
    public HudHeaderLayout headerLayout() { return headerLayout; }
    /**
     * Returns the horizontal distance from the selected anchor.
     * @return CSS pixels before viewport clamping
     * @since 0.3.0
     */
    public int offsetXPx() { return offsetXPx; }
    /**
     * Returns the vertical distance from the selected anchor.
     * @return CSS pixels before viewport clamping
     * @since 0.3.0
     */
    public int offsetYPx() { return offsetYPx; }
    /**
     * Returns the requested panel width.
     * @return CSS pixels before viewport clamping
     * @since 0.3.0
     */
    public int widthPx() { return widthPx; }
    /**
     * Returns the requested panel maximum height.
     * @return CSS pixels before viewport clamping
     * @since 0.3.0
     */
    public int maxHeightPx() { return maxHeightPx; }
    /**
     * Returns the requested event-log maximum height.
     * @return CSS pixels before panel and viewport limits
     * @since 0.3.0
     */
    public int maxLogHeightPx() { return maxLogHeightPx; }
    /**
     * Returns the branding rail width.
     * @return CSS pixels
     * @since 0.3.0
     */
    public int railWidthPx() { return railWidthPx; }
    /**
     * Returns the global local-font preset inherited by sections without an override.
     * @return global font preset
     * @since 0.3.0
     */
    public HudFontPreset fontPreset() { return fontPreset; }
    /**
     * Returns semantic section font overrides.
     * @return typography configuration
     * @since 0.3.0
     */
    public HudTypography typography() { return typography; }
    /**
     * Returns the event-log scrollbar rendering mode.
     * @return scrollbar style
     * @since 0.3.0
     */
    public HudScrollbarStyle scrollbarStyle() { return scrollbarStyle; }
    /**
     * Returns the configured Chromium scrollbar width.
     * @return CSS pixels; Firefox maps the style to engine-supported widths
     * @since 0.3.0
     */
    public int scrollbarWidthPx() { return scrollbarWidthPx; }
    /**
     * Returns the event-log scrollbar track color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String scrollbarTrackColor() { return scrollbarTrackColor; }
    /**
     * Returns the event-log scrollbar thumb color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String scrollbarThumbColor() { return scrollbarThumbColor; }
    /**
     * Returns the event-log scrollbar thumb hover color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String scrollbarThumbHoverColor() { return scrollbarThumbHoverColor; }
    /**
     * Returns the base font size.
     * @return CSS pixels
     * @since 0.3.0
     */
    public int baseFontSizePx() { return baseFontSizePx; }
    /**
     * Returns the header font size.
     * @return CSS pixels
     * @since 0.3.0
     */
    public int headerFontSizePx() { return headerFontSizePx; }
    /**
     * Reports whether the test-name item is rendered.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showTestName() { return showTestName; }
    /**
     * Reports whether the current-step item is rendered.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showCurrentStep() { return showCurrentStep; }
    /**
     * Reports whether pipeline metadata is rendered.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showPipeline() { return showPipeline; }
    /**
     * Reports whether event timestamps are rendered.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showTimestamps() { return showTimestamps; }
    /**
     * Returns the presentation format used for visible HUD timestamps.
     * @return timestamp format; defaults to {@link HudTimestampFormat#ISO_UTC}
     * @since 0.3.1
     */
    public HudTimestampFormat timestampFormat() { return timestampFormat; }
    /**
     * Returns the explicitly selected timestamp zone.
     *
     * <p>An empty value means that the JVM system zone is resolved by the test process when the
     * HUD runtime configuration is created. It does not mean the remote browser's system zone.
     *
     * @return explicit zone, or empty for the JVM system zone
     * @since 0.3.1
     */
    public Optional<ZoneId> timestampZone() { return Optional.ofNullable(timestampZone); }
    /**
     * Reports whether timestamp presentation follows the test JVM system zone.
     * @return {@code true} for the system-zone mode
     * @since 0.3.1
     */
    public boolean usesSystemTimestampZone() { return timestampZone == null; }
    /**
     * Resolves the zone that will be sent to the browser HUD.
     * @return explicit zone or the current test JVM system zone
     * @since 0.3.1
     */
    public ZoneId effectiveTimestampZone() { return timestampZone != null ? timestampZone : ZoneId.systemDefault(); }
    /**
     * Reports whether the event-log region is rendered.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showEventLog() { return showEventLog; }
    /**
     * Reports whether network event rows are rendered in the HUD.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showNetwork() { return showNetwork; }
    /**
     * Reports whether retry and recovery rows are rendered in the HUD.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showRetries() { return showRetries; }
    /**
     * Reports whether wait rows are rendered in the HUD.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showWaits() { return showWaits; }
    /**
     * Reports whether assertion rows are rendered in the HUD.
     * @return configured visibility
     * @since 0.3.0
     */
    public boolean showAssertions() { return showAssertions; }
    /**
     * Returns the branding assets to render.
     * @return branding mode
     * @since 0.3.0
     */
    public HudBranding branding() { return branding; }
    /**
     * Returns placement for rendered branding assets.
     * @return logo placement
     * @since 0.3.0
     */
    public HudLogoPlacement logoPlacement() { return logoPlacement; }
    /**
     * Returns the panel background color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String background() { return background; }
    /**
     * Returns panel background opacity.
     * @return finite value from {@code 0.0} through {@code 1.0}
     * @since 0.3.0
     */
    public double backgroundOpacity() { return backgroundOpacity; }
    /**
     * Returns the accent color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String accentColor() { return accentColor; }
    /**
     * Returns the primary text color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String primaryTextColor() { return primaryTextColor; }
    /**
     * Returns the muted text color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String mutedTextColor() { return mutedTextColor; }
    /**
     * Returns the success semantic color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String successColor() { return successColor; }
    /**
     * Returns the warning semantic color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String warningColor() { return warningColor; }
    /**
     * Returns the failure semantic color.
     * @return color in {@code #RRGGBB} form
     * @since 0.3.0
     */
    public String failureColor() { return failureColor; }
    /**
     * Reports whether a validated caller PNG is embedded in this configuration.
     * @return whether a custom logo is present
     * @since 0.3.0
     */
    public boolean hasCustomLogo() { return customLogoDataUri != null; }
    String customLogoFileName() { return customLogoFileName; }

    Map<String, Object> toRuntimeMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("preset", preset.name());
        values.put("position", position.name());
        values.put("headerLayout", headerLayout.name());
        values.put("offsetX", offsetXPx);
        values.put("offsetY", offsetYPx);
        values.put("width", widthPx);
        values.put("maxHeight", maxHeightPx);
        values.put("maxLogHeight", maxLogHeightPx);
        values.put("railWidth", railWidthPx);
        values.put("fontPreset", fontPreset.name());
        values.put("typography", typography.toRuntimeMap());
        values.put("scrollbarStyle", scrollbarStyle.name());
        values.put("scrollbarWidth", scrollbarWidthPx);
        values.put("scrollbarTrack", scrollbarTrackColor);
        values.put("scrollbarThumb", scrollbarThumbColor);
        values.put("scrollbarThumbHover", scrollbarThumbHoverColor);
        values.put("baseFontSize", baseFontSizePx);
        values.put("headerFontSize", headerFontSizePx);
        values.put("showTestName", showTestName);
        values.put("showCurrentStep", showCurrentStep);
        values.put("showPipeline", showPipeline);
        values.put("showTimestamps", showTimestamps);
        values.put("timestampFormat", timestampFormat.name());
        values.put("timestampZone", usesSystemTimestampZone() ? "SYSTEM" : timestampZone.getId());
        values.put("timestampZoneSource", usesSystemTimestampZone() ? "SYSTEM" : "EXPLICIT");
        values.put("showEventLog", showEventLog);
        values.put("showNetwork", showNetwork);
        values.put("showRetries", showRetries);
        values.put("showWaits", showWaits);
        values.put("showAssertions", showAssertions);
        values.put("branding", branding.name());
        values.put("logoPlacement", logoPlacement.name());
        values.put("background", background);
        values.put("backgroundOpacity", backgroundOpacity);
        values.put("accent", accentColor);
        values.put("primaryText", primaryTextColor);
        values.put("mutedText", mutedTextColor);
        values.put("success", successColor);
        values.put("warning", warningColor);
        values.put("failure", failureColor);
        if (customLogoDataUri != null) values.put("customLogo", customLogoDataUri);
        return Collections.unmodifiableMap(values);
    }

    Map<String, Object> toBrowserRuntimeMap() {
        Map<String, Object> values = new LinkedHashMap<>(toRuntimeMap());
        if (usesSystemTimestampZone()) values.put("timestampZone", effectiveTimestampZone().getId());
        return Collections.unmodifiableMap(values);
    }

    /**
     * Builds immutable HUD configuration. A preset is always the base; fields explicitly set by
     * this builder take precedence independently of call order.
     *
     * @since 0.3.0
     */
    public static final class Builder {
        private enum Field {
            POSITION, HEADER_LAYOUT, OFFSET_X, OFFSET_Y, WIDTH, MAX_HEIGHT, MAX_LOG_HEIGHT, RAIL_WIDTH,
            FONT_PRESET, TYPOGRAPHY, SCROLLBAR_STYLE, SCROLLBAR_WIDTH, SCROLLBAR_TRACK,
            SCROLLBAR_THUMB, SCROLLBAR_THUMB_HOVER, BASE_FONT_SIZE, HEADER_FONT_SIZE,
            SHOW_TEST_NAME, SHOW_CURRENT_STEP,
            SHOW_PIPELINE, SHOW_TIMESTAMPS, TIMESTAMP_FORMAT, TIMESTAMP_ZONE, SHOW_EVENT_LOG, SHOW_NETWORK, SHOW_RETRIES,
            SHOW_WAITS, SHOW_ASSERTIONS, BRANDING, LOGO_PLACEMENT, BACKGROUND,
            BACKGROUND_OPACITY, ACCENT, PRIMARY_TEXT, MUTED_TEXT, SUCCESS, WARNING, FAILURE
        }

        private final EnumSet<Field> explicit = EnumSet.noneOf(Field.class);
        private HudPreset preset;
        private HudPosition position;
        private HudHeaderLayout headerLayout;
        private int offsetXPx;
        private int offsetYPx;
        private int widthPx;
        private int maxHeightPx;
        private int maxLogHeightPx;
        private int railWidthPx;
        private HudFontPreset fontPreset;
        private HudTypography typography;
        private HudScrollbarStyle scrollbarStyle;
        private int scrollbarWidthPx;
        private String scrollbarTrackColor;
        private String scrollbarThumbColor;
        private String scrollbarThumbHoverColor;
        private int baseFontSizePx;
        private int headerFontSizePx;
        private boolean showTestName;
        private boolean showCurrentStep;
        private boolean showPipeline;
        private boolean showTimestamps;
        private HudTimestampFormat timestampFormat;
        private ZoneId timestampZone;
        private boolean showEventLog;
        private boolean showNetwork;
        private boolean showRetries;
        private boolean showWaits;
        private boolean showAssertions;
        private HudBranding branding;
        private HudLogoPlacement logoPlacement;
        private String background;
        private double backgroundOpacity;
        private String accentColor;
        private String primaryTextColor;
        private String mutedTextColor;
        private String successColor;
        private String warningColor;
        private String failureColor;
        private String customLogoFileName;
        private String customLogoDataUri;

        private Builder() { applyPreset(HudPreset.COMPACT); }
        private Builder(HudOptions source) {
            this.preset = source.preset; this.position = source.position; this.headerLayout = source.headerLayout;
            this.widthPx = source.widthPx;
            this.offsetXPx = source.offsetXPx; this.offsetYPx = source.offsetYPx;
            this.maxHeightPx = source.maxHeightPx; this.maxLogHeightPx = source.maxLogHeightPx;
            this.railWidthPx = source.railWidthPx; this.fontPreset = source.fontPreset;
            this.typography = source.typography;
            this.scrollbarStyle = source.scrollbarStyle;
            this.scrollbarWidthPx = source.scrollbarWidthPx;
            this.scrollbarTrackColor = source.scrollbarTrackColor;
            this.scrollbarThumbColor = source.scrollbarThumbColor;
            this.scrollbarThumbHoverColor = source.scrollbarThumbHoverColor;
            this.baseFontSizePx = source.baseFontSizePx; this.headerFontSizePx = source.headerFontSizePx;
            this.showTestName = source.showTestName;
            this.showCurrentStep = source.showCurrentStep; this.showPipeline = source.showPipeline;
            this.showTimestamps = source.showTimestamps; this.showEventLog = source.showEventLog;
            this.timestampFormat = source.timestampFormat; this.timestampZone = source.timestampZone;
            this.showNetwork = source.showNetwork; this.showRetries = source.showRetries;
            this.showWaits = source.showWaits; this.showAssertions = source.showAssertions;
            this.branding = source.branding; this.logoPlacement = source.logoPlacement;
            this.background = source.background;
            this.backgroundOpacity = source.backgroundOpacity; this.accentColor = source.accentColor;
            this.primaryTextColor = source.primaryTextColor; this.mutedTextColor = source.mutedTextColor;
            this.successColor = source.successColor; this.warningColor = source.warningColor;
            this.failureColor = source.failureColor; this.customLogoFileName = source.customLogoFileName;
            this.customLogoDataUri = source.customLogoDataUri;
            this.explicit.addAll(EnumSet.allOf(Field.class));
        }

        /**
         * Selects the base preset without replacing explicit overrides.
         *
         * @param value preset
         * @return this builder
         * @since 0.3.0
         */
        public Builder preset(HudPreset value) { applyPreset(Objects.requireNonNull(value, "preset must not be null")); return this; }
        /**
         * Selects the viewport-corner anchor.
         *
         * @param value anchor
         * @return this builder
         * @since 0.3.0
         */
        public Builder position(HudPosition value) { explicit.add(Field.POSITION); position = Objects.requireNonNull(value, "position must not be null"); return this; }
        /**
         * Sets the arrangement of the atomic test-name and current-step header items.
         *
         * @param value responsive, single-row, or stacked layout
         * @return this builder
         * @since 0.3.0
         */
        public Builder headerLayout(HudHeaderLayout value) {
            explicit.add(Field.HEADER_LAYOUT);
            headerLayout = Objects.requireNonNull(value, "headerLayout must not be null");
            return this;
        }
        /**
         * Sets the horizontal anchor offset from 0 through 500 px.
         *
         * @param value offset
         * @return this builder
         * @since 0.3.0
         */
        public Builder offsetXPx(int value) { explicit.add(Field.OFFSET_X); offsetXPx = bounded(value, 0, 500, "offsetXPx"); return this; }
        /**
         * Sets the vertical anchor offset from 0 through 500 px.
         *
         * @param value offset
         * @return this builder
         * @since 0.3.0
         */
        public Builder offsetYPx(int value) { explicit.add(Field.OFFSET_Y); offsetYPx = bounded(value, 0, 500, "offsetYPx"); return this; }
        /**
         * Sets the requested width from 240 through 960 px.
         *
         * @param value width
         * @return this builder
         * @since 0.3.0
         */
        public Builder widthPx(int value) { if (value < 240 || value > 960) throw new IllegalArgumentException("widthPx must be between 240 and 960"); explicit.add(Field.WIDTH); widthPx = value; return this; }
        /**
         * Sets the requested panel maximum height from 120 through 1000 px.
         *
         * @param value height
         * @return this builder
         * @since 0.3.0
         */
        public Builder maxHeightPx(int value) { explicit.add(Field.MAX_HEIGHT); maxHeightPx = bounded(value, 120, 1000, "maxHeightPx"); return this; }
        /**
         * Sets the requested event-log maximum height from 80 through 720 px.
         *
         * @param value height
         * @return this builder
         * @since 0.3.0
         */
        public Builder maxLogHeightPx(int value) { if (value < 80 || value > 720) throw new IllegalArgumentException("maxLogHeightPx must be between 80 and 720"); explicit.add(Field.MAX_LOG_HEIGHT); maxLogHeightPx = value; return this; }
        /**
         * Sets the branding rail width from 16 through 80 px.
         *
         * @param value width
         * @return this builder
         * @since 0.3.0
         */
        public Builder railWidthPx(int value) { explicit.add(Field.RAIL_WIDTH); railWidthPx = bounded(value, 16, 80, "railWidthPx"); return this; }
        /**
         * Sets the local font stack inherited by every section.
         *
         * @param value font preset
         * @return this builder
         * @since 0.3.0
         */
        public Builder fontPreset(HudFontPreset value) { explicit.add(Field.FONT_PRESET); fontPreset = Objects.requireNonNull(value, "fontPreset must not be null"); return this; }
        /**
         * Sets optional semantic section font overrides.
         *
         * @param value typography overrides
         * @return this builder
         * @since 0.3.0
         */
        public Builder typography(HudTypography value) { explicit.add(Field.TYPOGRAPHY); typography = Objects.requireNonNull(value, "typography must not be null"); return this; }
        /**
         * Selects custom subtle/standard styling or native browser rendering.
         *
         * @param value scrollbar rendering mode
         * @return this builder
         * @since 0.3.0
         */
        public Builder scrollbarStyle(HudScrollbarStyle value) {
            explicit.add(Field.SCROLLBAR_STYLE);
            scrollbarStyle = Objects.requireNonNull(value, "scrollbarStyle must not be null");
            applyScrollbarStyleDefaults(value);
            return this;
        }
        /**
         * Sets the bounded Chromium scrollbar width; Firefox maps presets to engine-supported widths.
         *
         * @param value width from 4 through 14 pixels
         * @return this builder
         * @since 0.3.0
         */
        public Builder scrollbarWidthPx(int value) { explicit.add(Field.SCROLLBAR_WIDTH); scrollbarWidthPx = bounded(value, 4, 14, "scrollbarWidthPx"); return this; }
        /**
         * Sets the validated six-digit hexadecimal track color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder scrollbarTrackColor(String value) { explicit.add(Field.SCROLLBAR_TRACK); scrollbarTrackColor = color(value, "scrollbarTrackColor"); return this; }
        /**
         * Sets the validated six-digit hexadecimal thumb color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder scrollbarThumbColor(String value) { explicit.add(Field.SCROLLBAR_THUMB); scrollbarThumbColor = color(value, "scrollbarThumbColor"); return this; }
        /**
         * Sets the validated six-digit hexadecimal thumb hover color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder scrollbarThumbHoverColor(String value) { explicit.add(Field.SCROLLBAR_THUMB_HOVER); scrollbarThumbHoverColor = color(value, "scrollbarThumbHoverColor"); return this; }
        /**
         * Sets the base font size from 9 through 18 px.
         *
         * @param value size
         * @return this builder
         * @since 0.3.0
         */
        public Builder baseFontSizePx(int value) { explicit.add(Field.BASE_FONT_SIZE); baseFontSizePx = bounded(value, 9, 18, "baseFontSizePx"); return this; }
        /**
         * Sets the header font size from 8 through 16 px.
         *
         * @param value size
         * @return this builder
         * @since 0.3.0
         */
        public Builder headerFontSizePx(int value) { explicit.add(Field.HEADER_FONT_SIZE); headerFontSizePx = bounded(value, 8, 16, "headerFontSizePx"); return this; }
        /**
         * Controls test-name visibility.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showTestName(boolean value) { explicit.add(Field.SHOW_TEST_NAME); showTestName = value; return this; }
        /**
         * Controls current-step visibility.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showCurrentStep(boolean value) { explicit.add(Field.SHOW_CURRENT_STEP); showCurrentStep = value; return this; }
        /**
         * Controls pipeline metadata visibility.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showPipeline(boolean value) { explicit.add(Field.SHOW_PIPELINE); showPipeline = value; return this; }
        /**
         * Controls HUD timestamp visibility.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showTimestamps(boolean value) { explicit.add(Field.SHOW_TIMESTAMPS); showTimestamps = value; return this; }
        /**
         * Selects the presentation format for visible HUD timestamps.
         *
         * @param value timestamp format
         * @return this builder
         * @since 0.3.1
         */
        public Builder timestampFormat(HudTimestampFormat value) {
            explicit.add(Field.TIMESTAMP_FORMAT);
            timestampFormat = Objects.requireNonNull(value, "timestampFormat must not be null");
            return this;
        }
        /**
         * Selects an explicit zone for readable HUD timestamp formats.
         *
         * <p>{@link HudTimestampFormat#ISO_UTC} remains UTC regardless of this value.
         *
         * @param value IANA or fixed-offset zone
         * @return this builder
         * @since 0.3.1
         */
        public Builder timestampZone(ZoneId value) {
            explicit.add(Field.TIMESTAMP_ZONE);
            timestampZone = Objects.requireNonNull(value, "timestampZone must not be null");
            return this;
        }
        /**
         * Uses the test JVM system zone for readable HUD timestamp formats.
         *
         * @return this builder
         * @since 0.3.1
         */
        public Builder systemTimestampZone() {
            explicit.add(Field.TIMESTAMP_ZONE);
            timestampZone = null;
            return this;
        }
        /**
         * Controls event-log region visibility.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showEventLog(boolean value) { explicit.add(Field.SHOW_EVENT_LOG); showEventLog = value; return this; }
        /**
         * Controls network row visibility in the HUD only.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showNetwork(boolean value) { explicit.add(Field.SHOW_NETWORK); showNetwork = value; return this; }
        /**
         * Controls retry and recovery row visibility in the HUD only.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showRetries(boolean value) { explicit.add(Field.SHOW_RETRIES); showRetries = value; return this; }
        /**
         * Controls wait row visibility in the HUD only.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showWaits(boolean value) { explicit.add(Field.SHOW_WAITS); showWaits = value; return this; }
        /**
         * Controls assertion row visibility in the HUD only.
         *
         * @param value enabled state
         * @return this builder
         * @since 0.3.0
         */
        public Builder showAssertions(boolean value) { explicit.add(Field.SHOW_ASSERTIONS); showAssertions = value; return this; }
        /**
         * Selects the branding assets.
         *
         * @param value branding mode
         * @return this builder
         * @since 0.3.0
         */
        public Builder branding(HudBranding value) { explicit.add(Field.BRANDING); branding = Objects.requireNonNull(value, "branding must not be null"); return this; }
        /**
         * Selects where branding is rendered.
         *
         * @param value placement
         * @return this builder
         * @since 0.3.0
         */
        public Builder logoPlacement(HudLogoPlacement value) { explicit.add(Field.LOGO_PLACEMENT); logoPlacement = Objects.requireNonNull(value, "logoPlacement must not be null"); return this; }
        /**
         * Sets a validated panel background color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder background(String value) { explicit.add(Field.BACKGROUND); background = color(value, "background"); return this; }
        /**
         * Sets finite background opacity from 0 through 1.
         *
         * @param value opacity
         * @return this builder
         * @since 0.3.0
         */
        public Builder backgroundOpacity(double value) { if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("backgroundOpacity must be between 0 and 1"); explicit.add(Field.BACKGROUND_OPACITY); backgroundOpacity = value; return this; }
        /**
         * Sets the accent color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder accentColor(String value) { explicit.add(Field.ACCENT); accentColor = color(value, "accentColor"); return this; }
        /**
         * Sets the primary text color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder primaryTextColor(String value) { explicit.add(Field.PRIMARY_TEXT); primaryTextColor = color(value, "primaryTextColor"); return this; }
        /**
         * Sets the muted and label text color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder mutedTextColor(String value) { explicit.add(Field.MUTED_TEXT); mutedTextColor = color(value, "mutedTextColor"); return this; }
        /**
         * Sets the success semantic color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder successColor(String value) { explicit.add(Field.SUCCESS); successColor = color(value, "successColor"); return this; }
        /**
         * Sets the warning semantic color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder warningColor(String value) { explicit.add(Field.WARNING); warningColor = color(value, "warningColor"); return this; }
        /**
         * Sets the failure semantic color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         * @since 0.3.0
         */
        public Builder failureColor(String value) { explicit.add(Field.FAILURE); failureColor = color(value, "failureColor"); return this; }

        /**
         * Loads a bounded local PNG for browser-side display. SVG is intentionally not accepted.
         *
         * @param path regular, non-symbolic-link PNG path
         * @return this builder
         * @throws IllegalArgumentException if the file is unreadable or violates the PNG limits
         * @since 0.3.0
         */
        public Builder customLogo(Path path) {
            Objects.requireNonNull(path, "path must not be null");
            Path normalized = path.toAbsolutePath().normalize();
            if (Files.isSymbolicLink(normalized) || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) {
                throw new IllegalArgumentException("customLogo must be a regular non-symbolic-link PNG file");
            }
            try {
                long size = Files.size(normalized);
                if (size < 24 || size > MAX_LOGO_BYTES) {
                    throw new IllegalArgumentException("customLogo must be a PNG no larger than 1048576 bytes");
                }
                byte[] bytes = Files.readAllBytes(normalized);
                for (int index = 0; index < PNG_SIGNATURE.length; index++) {
                    if (bytes[index] != PNG_SIGNATURE[index]) throw new IllegalArgumentException("customLogo must be a PNG file");
                }
                if (bytes[12] != 'I' || bytes[13] != 'H' || bytes[14] != 'D' || bytes[15] != 'R') {
                    throw new IllegalArgumentException("customLogo must contain a PNG IHDR chunk");
                }
                long width = unsignedInt(bytes, 16);
                long height = unsignedInt(bytes, 20);
                if (width == 0 || height == 0 || width > MAX_LOGO_DIMENSION || height > MAX_LOGO_DIMENSION
                        || width * height > MAX_LOGO_PIXELS) {
                    throw new IllegalArgumentException("customLogo dimensions must be between 1 and 4096 pixels and no more than 16777216 pixels total");
                }
                customLogoFileName = normalized.getFileName().toString();
                customLogoDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
                return this;
            } catch (IOException failure) {
                throw new IllegalArgumentException("customLogo could not be read", failure);
            }
        }

        /**
         * Materializes the validated immutable configuration.
         *
         * @return validated immutable HUD options
         * @since 0.3.0
         */
        public HudOptions build() { return new HudOptions(this); }

        private void applyPreset(HudPreset value) {
            preset = value;
            if (!explicit.contains(Field.POSITION)) position = HudPosition.BOTTOM_RIGHT;
            if (!explicit.contains(Field.HEADER_LAYOUT)) headerLayout = HudHeaderLayout.AUTO;
            if (!explicit.contains(Field.OFFSET_X)) offsetXPx = 10;
            if (!explicit.contains(Field.OFFSET_Y)) offsetYPx = 10;
            if (!explicit.contains(Field.BRANDING)) branding = HudBranding.TEST_LENS;
            if (!explicit.contains(Field.LOGO_PLACEMENT)) logoPlacement = HudLogoPlacement.LEFT_RAIL;
            if (!explicit.contains(Field.BACKGROUND)) background = "#0f172a";
            if (!explicit.contains(Field.ACCENT)) accentColor = "#38bdf8";
            if (!explicit.contains(Field.PRIMARY_TEXT)) primaryTextColor = "#f8fafc";
            if (!explicit.contains(Field.MUTED_TEXT)) mutedTextColor = "#cbd5e1";
            if (!explicit.contains(Field.SUCCESS)) successColor = "#22c55e";
            if (!explicit.contains(Field.WARNING)) warningColor = "#f59e0b";
            if (!explicit.contains(Field.FAILURE)) failureColor = "#ef4444";
            if (!explicit.contains(Field.BACKGROUND_OPACITY)) backgroundOpacity = 0.96;
            if (!explicit.contains(Field.FONT_PRESET)) fontPreset = value == HudPreset.DEBUG ? HudFontPreset.MONOSPACE : HudFontPreset.UI_SANS;
            if (!explicit.contains(Field.TYPOGRAPHY)) typography = HudTypography.inheritAll();
            if (!explicit.contains(Field.SCROLLBAR_STYLE)) scrollbarStyle = value == HudPreset.DEBUG
                    ? HudScrollbarStyle.STANDARD : HudScrollbarStyle.SUBTLE;
            applyScrollbarStyleDefaults(scrollbarStyle);
            if (!explicit.contains(Field.BASE_FONT_SIZE)) baseFontSizePx = value == HudPreset.MINIMAL ? 9 : 10;
            if (!explicit.contains(Field.HEADER_FONT_SIZE)) headerFontSizePx = value == HudPreset.DEBUG ? 11 : 10;
            if (!explicit.contains(Field.RAIL_WIDTH)) railWidthPx = 16;
            if (!explicit.contains(Field.SHOW_PIPELINE)) showPipeline = value == HudPreset.DEBUG;
            if (!explicit.contains(Field.SHOW_TIMESTAMPS)) showTimestamps = value == HudPreset.STANDARD || value == HudPreset.DEBUG;
            if (!explicit.contains(Field.TIMESTAMP_FORMAT)) timestampFormat = HudTimestampFormat.ISO_UTC;
            if (!explicit.contains(Field.TIMESTAMP_ZONE)) timestampZone = null;
            if (!explicit.contains(Field.SHOW_TEST_NAME)) showTestName = value != HudPreset.MINIMAL;
            if (!explicit.contains(Field.SHOW_CURRENT_STEP)) showCurrentStep = true;
            if (!explicit.contains(Field.SHOW_EVENT_LOG)) showEventLog = value != HudPreset.MINIMAL;
            if (!explicit.contains(Field.SHOW_NETWORK)) showNetwork = value != HudPreset.MINIMAL;
            if (!explicit.contains(Field.SHOW_RETRIES)) showRetries = value != HudPreset.MINIMAL;
            if (!explicit.contains(Field.SHOW_WAITS)) showWaits = true;
            if (!explicit.contains(Field.SHOW_ASSERTIONS)) showAssertions = true;
            if (!explicit.contains(Field.WIDTH)) widthPx = switch (value) { case MINIMAL -> 280; case COMPACT -> 420; case STANDARD -> 520; case DEBUG -> 620; };
            if (!explicit.contains(Field.MAX_HEIGHT)) maxHeightPx = switch (value) { case MINIMAL -> 180; case COMPACT -> 280; case STANDARD -> 380; case DEBUG -> 520; };
            if (!explicit.contains(Field.MAX_LOG_HEIGHT)) maxLogHeightPx = switch (value) { case MINIMAL -> 80; case COMPACT -> 180; case STANDARD -> 260; case DEBUG -> 360; };
        }

        private void applyScrollbarStyleDefaults(HudScrollbarStyle value) {
            boolean standard = value == HudScrollbarStyle.STANDARD;
            if (!explicit.contains(Field.SCROLLBAR_WIDTH)) scrollbarWidthPx = standard ? 10 : 6;
            if (!explicit.contains(Field.SCROLLBAR_TRACK)) scrollbarTrackColor = standard ? "#1e293b" : "#111827";
            if (!explicit.contains(Field.SCROLLBAR_THUMB)) scrollbarThumbColor = standard ? "#94a3b8" : "#64748b";
            if (!explicit.contains(Field.SCROLLBAR_THUMB_HOVER)) scrollbarThumbHoverColor = standard ? "#cbd5e1" : "#94a3b8";
        }

        private static long unsignedInt(byte[] bytes, int offset) {
            return ((long) bytes[offset] & 0xff) << 24
                    | ((long) bytes[offset + 1] & 0xff) << 16
                    | ((long) bytes[offset + 2] & 0xff) << 8
                    | ((long) bytes[offset + 3] & 0xff);
        }

        private static int bounded(int value, int minimum, int maximum, String name) {
            if (value < minimum || value > maximum) {
                throw new IllegalArgumentException(name + " must be between " + minimum + " and " + maximum);
            }
            return value;
        }

        private static String color(String value, String name) {
            if (value == null || !COLOR.matcher(value).matches()) throw new IllegalArgumentException(name + " must be a six-digit hexadecimal color such as #39ff88");
            return value.toLowerCase();
        }
    }
}
