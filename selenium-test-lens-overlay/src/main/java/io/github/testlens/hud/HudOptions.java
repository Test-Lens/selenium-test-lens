package io.github.testlens.hud;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable product-level configuration for the browser HUD.
 *
 * <p>The default is {@link HudPreset#COMPACT}. A preset supplies base values and every explicit
 * builder override wins regardless of call order. Colors accept only six-digit hexadecimal
 * values. Scrollbar styling is limited to product presets, a bounded width, and validated
 * colors; this API does not accept arbitrary CSS or HTML.
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

    public static HudOptions defaults() { return builder().build(); }
    public static Builder builder() { return new Builder(); }
    public Builder toBuilder() { return new Builder(this); }
    public HudPreset preset() { return preset; }
    public HudPosition position() { return position; }
    /** Returns the responsive arrangement of test-name and current-step header items. */
    public HudHeaderLayout headerLayout() { return headerLayout; }
    public int offsetXPx() { return offsetXPx; }
    public int offsetYPx() { return offsetYPx; }
    public int widthPx() { return widthPx; }
    public int maxHeightPx() { return maxHeightPx; }
    public int maxLogHeightPx() { return maxLogHeightPx; }
    public int railWidthPx() { return railWidthPx; }
    public HudFontPreset fontPreset() { return fontPreset; }
    public HudTypography typography() { return typography; }
    /** Returns the event-log scrollbar rendering mode. */
    public HudScrollbarStyle scrollbarStyle() { return scrollbarStyle; }
    /** Returns the configured Chromium scrollbar width in pixels. */
    public int scrollbarWidthPx() { return scrollbarWidthPx; }
    /** Returns the event-log scrollbar track color. */
    public String scrollbarTrackColor() { return scrollbarTrackColor; }
    /** Returns the event-log scrollbar thumb color. */
    public String scrollbarThumbColor() { return scrollbarThumbColor; }
    /** Returns the event-log scrollbar thumb hover color. */
    public String scrollbarThumbHoverColor() { return scrollbarThumbHoverColor; }
    public int baseFontSizePx() { return baseFontSizePx; }
    public int headerFontSizePx() { return headerFontSizePx; }
    public boolean showTestName() { return showTestName; }
    public boolean showCurrentStep() { return showCurrentStep; }
    public boolean showPipeline() { return showPipeline; }
    public boolean showTimestamps() { return showTimestamps; }
    public boolean showEventLog() { return showEventLog; }
    public boolean showNetwork() { return showNetwork; }
    public boolean showRetries() { return showRetries; }
    public boolean showWaits() { return showWaits; }
    public boolean showAssertions() { return showAssertions; }
    public HudBranding branding() { return branding; }
    public HudLogoPlacement logoPlacement() { return logoPlacement; }
    public String background() { return background; }
    public double backgroundOpacity() { return backgroundOpacity; }
    public String accentColor() { return accentColor; }
    public String primaryTextColor() { return primaryTextColor; }
    public String mutedTextColor() { return mutedTextColor; }
    public String successColor() { return successColor; }
    public String warningColor() { return warningColor; }
    public String failureColor() { return failureColor; }
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

    public static final class Builder {
        private enum Field {
            POSITION, HEADER_LAYOUT, OFFSET_X, OFFSET_Y, WIDTH, MAX_HEIGHT, MAX_LOG_HEIGHT, RAIL_WIDTH,
            FONT_PRESET, TYPOGRAPHY, SCROLLBAR_STYLE, SCROLLBAR_WIDTH, SCROLLBAR_TRACK,
            SCROLLBAR_THUMB, SCROLLBAR_THUMB_HOVER, BASE_FONT_SIZE, HEADER_FONT_SIZE,
            SHOW_TEST_NAME, SHOW_CURRENT_STEP,
            SHOW_PIPELINE, SHOW_TIMESTAMPS, SHOW_EVENT_LOG, SHOW_NETWORK, SHOW_RETRIES,
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

        public Builder preset(HudPreset value) { applyPreset(Objects.requireNonNull(value, "preset must not be null")); return this; }
        public Builder position(HudPosition value) { explicit.add(Field.POSITION); position = Objects.requireNonNull(value, "position must not be null"); return this; }
        /**
         * Sets the arrangement of the atomic test-name and current-step header items.
         *
         * @param value responsive, single-row, or stacked layout
         * @return this builder
         */
        public Builder headerLayout(HudHeaderLayout value) {
            explicit.add(Field.HEADER_LAYOUT);
            headerLayout = Objects.requireNonNull(value, "headerLayout must not be null");
            return this;
        }
        public Builder offsetXPx(int value) { explicit.add(Field.OFFSET_X); offsetXPx = bounded(value, 0, 500, "offsetXPx"); return this; }
        public Builder offsetYPx(int value) { explicit.add(Field.OFFSET_Y); offsetYPx = bounded(value, 0, 500, "offsetYPx"); return this; }
        public Builder widthPx(int value) { if (value < 240 || value > 960) throw new IllegalArgumentException("widthPx must be between 240 and 960"); explicit.add(Field.WIDTH); widthPx = value; return this; }
        public Builder maxHeightPx(int value) { explicit.add(Field.MAX_HEIGHT); maxHeightPx = bounded(value, 120, 1000, "maxHeightPx"); return this; }
        public Builder maxLogHeightPx(int value) { if (value < 80 || value > 720) throw new IllegalArgumentException("maxLogHeightPx must be between 80 and 720"); explicit.add(Field.MAX_LOG_HEIGHT); maxLogHeightPx = value; return this; }
        public Builder railWidthPx(int value) { explicit.add(Field.RAIL_WIDTH); railWidthPx = bounded(value, 16, 80, "railWidthPx"); return this; }
        public Builder fontPreset(HudFontPreset value) { explicit.add(Field.FONT_PRESET); fontPreset = Objects.requireNonNull(value, "fontPreset must not be null"); return this; }
        public Builder typography(HudTypography value) { explicit.add(Field.TYPOGRAPHY); typography = Objects.requireNonNull(value, "typography must not be null"); return this; }
        /**
         * Selects custom subtle/standard styling or native browser rendering.
         *
         * @param value scrollbar rendering mode
         * @return this builder
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
         */
        public Builder scrollbarWidthPx(int value) { explicit.add(Field.SCROLLBAR_WIDTH); scrollbarWidthPx = bounded(value, 4, 14, "scrollbarWidthPx"); return this; }
        /**
         * Sets the validated six-digit hexadecimal track color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         */
        public Builder scrollbarTrackColor(String value) { explicit.add(Field.SCROLLBAR_TRACK); scrollbarTrackColor = color(value, "scrollbarTrackColor"); return this; }
        /**
         * Sets the validated six-digit hexadecimal thumb color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         */
        public Builder scrollbarThumbColor(String value) { explicit.add(Field.SCROLLBAR_THUMB); scrollbarThumbColor = color(value, "scrollbarThumbColor"); return this; }
        /**
         * Sets the validated six-digit hexadecimal thumb hover color.
         *
         * @param value color in {@code #RRGGBB} form
         * @return this builder
         */
        public Builder scrollbarThumbHoverColor(String value) { explicit.add(Field.SCROLLBAR_THUMB_HOVER); scrollbarThumbHoverColor = color(value, "scrollbarThumbHoverColor"); return this; }
        public Builder baseFontSizePx(int value) { explicit.add(Field.BASE_FONT_SIZE); baseFontSizePx = bounded(value, 9, 18, "baseFontSizePx"); return this; }
        public Builder headerFontSizePx(int value) { explicit.add(Field.HEADER_FONT_SIZE); headerFontSizePx = bounded(value, 8, 16, "headerFontSizePx"); return this; }
        public Builder showTestName(boolean value) { explicit.add(Field.SHOW_TEST_NAME); showTestName = value; return this; }
        public Builder showCurrentStep(boolean value) { explicit.add(Field.SHOW_CURRENT_STEP); showCurrentStep = value; return this; }
        public Builder showPipeline(boolean value) { explicit.add(Field.SHOW_PIPELINE); showPipeline = value; return this; }
        public Builder showTimestamps(boolean value) { explicit.add(Field.SHOW_TIMESTAMPS); showTimestamps = value; return this; }
        public Builder showEventLog(boolean value) { explicit.add(Field.SHOW_EVENT_LOG); showEventLog = value; return this; }
        public Builder showNetwork(boolean value) { explicit.add(Field.SHOW_NETWORK); showNetwork = value; return this; }
        public Builder showRetries(boolean value) { explicit.add(Field.SHOW_RETRIES); showRetries = value; return this; }
        public Builder showWaits(boolean value) { explicit.add(Field.SHOW_WAITS); showWaits = value; return this; }
        public Builder showAssertions(boolean value) { explicit.add(Field.SHOW_ASSERTIONS); showAssertions = value; return this; }
        public Builder branding(HudBranding value) { explicit.add(Field.BRANDING); branding = Objects.requireNonNull(value, "branding must not be null"); return this; }
        public Builder logoPlacement(HudLogoPlacement value) { explicit.add(Field.LOGO_PLACEMENT); logoPlacement = Objects.requireNonNull(value, "logoPlacement must not be null"); return this; }
        public Builder background(String value) { explicit.add(Field.BACKGROUND); background = color(value, "background"); return this; }
        public Builder backgroundOpacity(double value) { if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("backgroundOpacity must be between 0 and 1"); explicit.add(Field.BACKGROUND_OPACITY); backgroundOpacity = value; return this; }
        public Builder accentColor(String value) { explicit.add(Field.ACCENT); accentColor = color(value, "accentColor"); return this; }
        public Builder primaryTextColor(String value) { explicit.add(Field.PRIMARY_TEXT); primaryTextColor = color(value, "primaryTextColor"); return this; }
        public Builder mutedTextColor(String value) { explicit.add(Field.MUTED_TEXT); mutedTextColor = color(value, "mutedTextColor"); return this; }
        public Builder successColor(String value) { explicit.add(Field.SUCCESS); successColor = color(value, "successColor"); return this; }
        public Builder warningColor(String value) { explicit.add(Field.WARNING); warningColor = color(value, "warningColor"); return this; }
        public Builder failureColor(String value) { explicit.add(Field.FAILURE); failureColor = color(value, "failureColor"); return this; }

        /**
         * Loads a bounded local PNG for browser-side display. SVG is intentionally not accepted.
         *
         * @param path regular, non-symbolic-link PNG path
         * @return this builder
         * @throws IllegalArgumentException if the file is unreadable or violates the PNG limits
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
