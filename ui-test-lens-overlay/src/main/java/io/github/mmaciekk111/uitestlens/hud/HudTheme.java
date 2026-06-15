package io.github.mmaciekk111.uitestlens.hud;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Theme values for the browser HUD panel.
 *
 * <p>Values are serialized to runtime CSS variables. Missing custom values fall back to the default theme.
 */
public final class HudTheme {

    private final String background;
    private final String foreground;
    private final String mutedForeground;
    private final String accent;
    private final String success;
    private final String warning;
    private final String danger;
    private final String borderColor;
    private final Integer borderRadiusPx;
    private final Integer fontSizePx;
    private final String fontFamily;
    private final String boxShadow;
    private final Double opacity;
    private final Integer zIndex;
    private final String backdropFilter;
    private final Integer paddingPx;
    private final Integer gapPx;
    private final Integer maxHeightPx;

    private HudTheme(Builder builder) {
        this.background = builder.background;
        this.foreground = builder.foreground;
        this.mutedForeground = builder.mutedForeground;
        this.accent = builder.accent;
        this.success = builder.success;
        this.warning = builder.warning;
        this.danger = builder.danger;
        this.borderColor = builder.borderColor;
        this.borderRadiusPx = builder.borderRadiusPx;
        this.fontSizePx = builder.fontSizePx;
        this.fontFamily = builder.fontFamily;
        this.boxShadow = builder.boxShadow;
        this.opacity = builder.opacity;
        this.zIndex = builder.zIndex;
        this.backdropFilter = builder.backdropFilter;
        this.paddingPx = builder.paddingPx;
        this.gapPx = builder.gapPx;
        this.maxHeightPx = builder.maxHeightPx;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static HudTheme defaultTheme() {
        return builder()
                .background("rgba(0, 0, 0, 0.75)")
                .foreground("#ffffff")
                .mutedForeground("rgba(255,255,255,0.78)")
                .accent("#4ca3ff")
                .success("#00ff7f")
                .warning("#ffd93b")
                .danger("#ff4c4c")
                .borderColor("rgba(255,255,255,0.2)")
                .borderRadiusPx(4)
                .fontSizePx(11)
                .fontFamily("Arial, sans-serif")
                .boxShadow("0 2px 6px rgba(0,0,0,0.4)")
                .opacity(1.0)
                .paddingPx(8)
                .gapPx(6)
                .maxHeightPx(480)
                .build();
    }

    public static HudTheme dark() {
        return builder()
                .background("rgba(17, 24, 39, 0.94)")
                .foreground("#f9fafb")
                .mutedForeground("#cbd5e1")
                .accent("#60a5fa")
                .success("#34d399")
                .warning("#fbbf24")
                .danger("#f87171")
                .borderColor("rgba(148, 163, 184, 0.35)")
                .borderRadiusPx(10)
                .fontSizePx(12)
                .fontFamily("Inter, system-ui, sans-serif")
                .boxShadow("0 14px 36px rgba(15, 23, 42, 0.32)")
                .opacity(1.0)
                .paddingPx(10)
                .gapPx(6)
                .maxHeightPx(480)
                .build();
    }

    public static HudTheme light() {
        return builder()
                .background("rgba(255, 255, 255, 0.96)")
                .foreground("#111827")
                .mutedForeground("#64748b")
                .accent("#2563eb")
                .success("#16a34a")
                .warning("#ca8a04")
                .danger("#dc2626")
                .borderColor("rgba(148, 163, 184, 0.45)")
                .borderRadiusPx(10)
                .fontSizePx(12)
                .fontFamily("Inter, system-ui, sans-serif")
                .boxShadow("0 12px 30px rgba(15, 23, 42, 0.16)")
                .opacity(1.0)
                .paddingPx(10)
                .gapPx(6)
                .maxHeightPx(480)
                .build();
    }

    public static HudTheme glass() {
        return builder()
                .background("rgba(15, 23, 42, 0.78)")
                .foreground("#f8fafc")
                .mutedForeground("#cbd5e1")
                .accent("#38bdf8")
                .success("#22c55e")
                .warning("#facc15")
                .danger("#fb7185")
                .borderColor("rgba(148, 163, 184, 0.35)")
                .borderRadiusPx(16)
                .fontSizePx(13)
                .fontFamily("Inter, system-ui, sans-serif")
                .boxShadow("0 18px 45px rgba(15, 23, 42, 0.35)")
                .opacity(1.0)
                .backdropFilter("blur(14px)")
                .paddingPx(12)
                .gapPx(8)
                .maxHeightPx(480)
                .build();
    }

    public static HudTheme compact() {
        return builder()
                .background("rgba(0, 0, 0, 0.82)")
                .foreground("#ffffff")
                .mutedForeground("rgba(255,255,255,0.72)")
                .accent("#4ca3ff")
                .success("#00ff7f")
                .warning("#ffd93b")
                .danger("#ff4c4c")
                .borderColor("rgba(255,255,255,0.18)")
                .borderRadiusPx(4)
                .fontSizePx(10)
                .fontFamily("Arial, sans-serif")
                .boxShadow("0 2px 8px rgba(0,0,0,0.35)")
                .opacity(1.0)
                .paddingPx(6)
                .gapPx(4)
                .maxHeightPx(360)
                .build();
    }

    public static HudTheme highContrast() {
        return builder()
                .background("#000000")
                .foreground("#ffffff")
                .mutedForeground("#ffffff")
                .accent("#00e5ff")
                .success("#00ff00")
                .warning("#ffff00")
                .danger("#ff3b30")
                .borderColor("#ffffff")
                .borderRadiusPx(2)
                .fontSizePx(13)
                .fontFamily("Arial, sans-serif")
                .boxShadow("0 0 0 2px #000000")
                .opacity(1.0)
                .paddingPx(10)
                .gapPx(6)
                .maxHeightPx(480)
                .build();
    }

    public static HudTheme blackAndColors() {
        return builder()
                .background("#000000")
                .foreground("#f8fff8")
                .mutedForeground("#7dffcc")
                .accent("#00f5ff")
                .success("#39ff14")
                .warning("#fff200")
                .danger("#ff1744")
                .borderColor("#ff00ff")
                .borderRadiusPx(10)
                .fontSizePx(12)
                .fontFamily("Inter, system-ui, sans-serif")
                .boxShadow("0 0 18px rgba(57, 255, 20, 0.45), 0 0 32px rgba(255, 0, 255, 0.28)")
                .opacity(1.0)
                .paddingPx(10)
                .gapPx(6)
                .maxHeightPx(480)
                .build();
    }

    public static HudTheme minimal() {
        return builder()
                .background("rgba(255, 255, 255, 0.92)")
                .foreground("#1f2937")
                .mutedForeground("#6b7280")
                .accent("#111827")
                .success("#15803d")
                .warning("#a16207")
                .danger("#b91c1c")
                .borderColor("rgba(31, 41, 55, 0.14)")
                .borderRadiusPx(6)
                .fontSizePx(11)
                .fontFamily("system-ui, sans-serif")
                .boxShadow("0 6px 18px rgba(31, 41, 55, 0.12)")
                .opacity(1.0)
                .paddingPx(8)
                .gapPx(5)
                .maxHeightPx(360)
                .build();
    }

    public static HudTheme fromPreset(HudThemePreset preset) {
        HudThemePreset effective = preset == null ? HudThemePreset.DEFAULT : preset;
        return switch (effective) {
            case DARK -> dark();
            case LIGHT -> light();
            case GLASS -> glass();
            case COMPACT -> compact();
            case HIGH_CONTRAST -> highContrast();
            case BLACK_AND_COLORS -> blackAndColors();
            case MINIMAL -> minimal();
            case DEFAULT -> defaultTheme();
        };
    }

    public String background() { return background; }
    public String foreground() { return foreground; }
    public String mutedForeground() { return mutedForeground; }
    public String accent() { return accent; }
    public String success() { return success; }
    public String warning() { return warning; }
    public String danger() { return danger; }
    public String borderColor() { return borderColor; }
    public Integer borderRadiusPx() { return borderRadiusPx; }
    public Integer fontSizePx() { return fontSizePx; }
    public String fontFamily() { return fontFamily; }
    public String boxShadow() { return boxShadow; }
    public Double opacity() { return opacity; }
    public Integer zIndex() { return zIndex; }
    public String backdropFilter() { return backdropFilter; }
    public Integer paddingPx() { return paddingPx; }
    public Integer gapPx() { return gapPx; }
    public Integer maxHeightPx() { return maxHeightPx; }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "background", background);
        put(values, "foreground", foreground);
        put(values, "mutedForeground", mutedForeground);
        put(values, "accent", accent);
        put(values, "success", success);
        put(values, "warning", warning);
        put(values, "danger", danger);
        put(values, "borderColor", borderColor);
        put(values, "borderRadiusPx", borderRadiusPx);
        put(values, "fontSizePx", fontSizePx);
        put(values, "fontFamily", fontFamily);
        put(values, "boxShadow", boxShadow);
        put(values, "opacity", opacity);
        put(values, "zIndex", zIndex);
        put(values, "backdropFilter", backdropFilter);
        put(values, "paddingPx", paddingPx);
        put(values, "gapPx", gapPx);
        put(values, "maxHeightPx", maxHeightPx);
        return Collections.unmodifiableMap(values);
    }

    private static void put(Map<String, Object> values, String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    public static final class Builder {
        private String background;
        private String foreground;
        private String mutedForeground;
        private String accent;
        private String success;
        private String warning;
        private String danger;
        private String borderColor;
        private Integer borderRadiusPx;
        private Integer fontSizePx;
        private String fontFamily;
        private String boxShadow;
        private Double opacity;
        private Integer zIndex;
        private String backdropFilter;
        private Integer paddingPx;
        private Integer gapPx;
        private Integer maxHeightPx;

        public Builder background(String background) { this.background = clean(background); return this; }
        public Builder foreground(String foreground) { this.foreground = clean(foreground); return this; }
        public Builder mutedForeground(String mutedForeground) { this.mutedForeground = clean(mutedForeground); return this; }
        public Builder accent(String accent) { this.accent = clean(accent); return this; }
        public Builder success(String success) { this.success = clean(success); return this; }
        public Builder warning(String warning) { this.warning = clean(warning); return this; }
        public Builder danger(String danger) { this.danger = clean(danger); return this; }
        public Builder borderColor(String borderColor) { this.borderColor = clean(borderColor); return this; }
        public Builder fontFamily(String fontFamily) { this.fontFamily = clean(fontFamily); return this; }
        public Builder boxShadow(String boxShadow) { this.boxShadow = clean(boxShadow); return this; }
        public Builder backdropFilter(String backdropFilter) { this.backdropFilter = clean(backdropFilter); return this; }

        public Builder borderRadiusPx(Integer borderRadiusPx) {
            this.borderRadiusPx = nonNegative(borderRadiusPx, "borderRadiusPx");
            return this;
        }

        public Builder fontSizePx(Integer fontSizePx) {
            this.fontSizePx = nonNegative(fontSizePx, "fontSizePx");
            return this;
        }

        public Builder opacity(Double opacity) {
            if (opacity != null && (opacity < 0.0 || opacity > 1.0)) {
                throw new IllegalArgumentException("opacity must be between 0 and 1");
            }
            this.opacity = opacity;
            return this;
        }

        public Builder zIndex(Integer zIndex) {
            this.zIndex = nonNegative(zIndex, "zIndex");
            return this;
        }

        public Builder paddingPx(Integer paddingPx) {
            this.paddingPx = nonNegative(paddingPx, "paddingPx");
            return this;
        }

        public Builder gapPx(Integer gapPx) {
            this.gapPx = nonNegative(gapPx, "gapPx");
            return this;
        }

        public Builder maxHeightPx(Integer maxHeightPx) {
            this.maxHeightPx = positive(maxHeightPx, "maxHeightPx");
            return this;
        }

        public HudTheme build() {
            return new HudTheme(this);
        }

        private static String clean(String value) {
            return value == null || value.isBlank() ? null : value;
        }

        private static Integer nonNegative(Integer value, String name) {
            if (value != null && value < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
            return value;
        }

        private static Integer positive(Integer value, String name) {
            if (value != null && value <= 0) {
                throw new IllegalArgumentException(name + " must be > 0");
            }
            return value;
        }
    }
}
