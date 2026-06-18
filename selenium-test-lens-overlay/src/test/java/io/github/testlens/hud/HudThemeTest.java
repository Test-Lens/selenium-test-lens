package io.github.testlens.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudThemeTest {

    @Test
    void presetsReturnThemes() {
        for (HudThemePreset preset : HudThemePreset.values()) {
            HudTheme theme = HudTheme.fromPreset(preset);

            assertNotNull(theme);
            assertNotNull(theme.background());
            assertNotNull(theme.foreground());
            assertNotNull(theme.accent());
        }
    }

    @Test
    void customThemeKeepsValues() {
        HudTheme theme = HudTheme.builder()
                .background("rgba(15, 23, 42, 0.92)")
                .foreground("#f8fafc")
                .accent("#38bdf8")
                .borderRadiusPx(16)
                .fontFamily("Inter, system-ui, sans-serif")
                .opacity(0.9)
                .maxHeightPx(420)
                .build();

        assertEquals("rgba(15, 23, 42, 0.92)", theme.background());
        assertEquals("#f8fafc", theme.foreground());
        assertEquals("#38bdf8", theme.accent());
        assertEquals(16, theme.borderRadiusPx());
        assertEquals("Inter, system-ui, sans-serif", theme.fontFamily());
        assertEquals(0.9, theme.opacity());
        assertEquals(420, theme.maxHeightPx());
    }

    @Test
    void opacityMustBeBetweenZeroAndOne() {
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().opacity(-0.1));
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().opacity(1.1));
    }

    @Test
    void pixelValuesMustBeNonNegative() {
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().borderRadiusPx(-1));
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().fontSizePx(-1));
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().paddingPx(-1));
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().gapPx(-1));
    }

    @Test
    void maxHeightMustBePositiveWhenDefined() {
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().maxHeightPx(0));
        assertThrows(IllegalArgumentException.class, () -> HudTheme.builder().maxHeightPx(-1));
    }

    @Test
    void presetsDefineMaxHeight() {
        assertEquals(480, HudTheme.defaultTheme().maxHeightPx());
        assertEquals(480, HudTheme.dark().maxHeightPx());
        assertEquals(480, HudTheme.light().maxHeightPx());
        assertEquals(480, HudTheme.glass().maxHeightPx());
        assertEquals(360, HudTheme.compact().maxHeightPx());
        assertEquals(480, HudTheme.highContrast().maxHeightPx());
        assertEquals(480, HudTheme.blackAndColors().maxHeightPx());
        assertEquals(360, HudTheme.minimal().maxHeightPx());
    }

    @Test
    void defaultThemeUsesGraphiteSlatePalette() {
        HudTheme theme = HudTheme.defaultTheme();

        assertEquals("rgba(15, 23, 42, 0.96)", theme.background());
        assertEquals("#f8fafc", theme.foreground());
        assertEquals("#cbd5e1", theme.mutedForeground());
        assertEquals("#38bdf8", theme.accent());
        assertEquals("rgba(148, 163, 184, 0.28)", theme.borderColor());
    }

    @Test
    void darkThemeUsesZincPaletteAndDiffersFromGlass() {
        HudTheme dark = HudTheme.dark();
        HudTheme glass = HudTheme.glass();

        assertEquals("rgba(24, 24, 27, 0.96)", dark.background());
        assertEquals("#fafafa", dark.foreground());
        assertEquals("#a1a1aa", dark.mutedForeground());
        assertEquals("rgba(63, 63, 70, 0.86)", dark.borderColor());
        assertNotEquals(dark.background(), glass.background());
        assertNotEquals(dark.backdropFilter(), glass.backdropFilter());
    }

    @Test
    void lightThemeUsesLightBackgroundAndDarkText() {
        HudTheme theme = HudTheme.light();

        assertEquals("rgba(255, 255, 255, 0.98)", theme.background());
        assertEquals("#0f172a", theme.foreground());
        assertEquals("#64748b", theme.mutedForeground());
        assertEquals("#cbd5e1", theme.borderColor());
        assertEquals("#2563eb", theme.accent());
    }

    @Test
    void glassThemeUsesTranslucencyAndBackdropBlur() {
        HudTheme theme = HudTheme.glass();

        assertTrue(theme.background().startsWith("linear-gradient(135deg"));
        assertTrue(theme.background().contains("rgba(15, 23, 42, 0.62)"));
        assertEquals("blur(18px) saturate(160%)", theme.backdropFilter());
        assertTrue(theme.borderColor().startsWith("rgba("));
        assertTrue(theme.boxShadow().contains("inset 0 1px 0"));
        assertTrue(theme.boxShadow().contains("inset 0 -1px 0"));
        assertEquals("blur(18px) saturate(160%)", theme.toMap().get("backdropFilter"));
        assertEquals(theme.background(), theme.toMap().get("background"));
    }

    @Test
    void highContrastKeepsStrongContrastValues() {
        HudTheme theme = HudTheme.highContrast();

        assertEquals("#020617", theme.background());
        assertEquals("#ffffff", theme.foreground());
        assertEquals("#f8fafc", theme.borderColor());
        assertTrue(theme.boxShadow().contains("0 0 0 2px"));
    }

    @Test
    void blackAndColorsUsesNeonPalette() {
        HudTheme theme = HudTheme.fromPreset(HudThemePreset.BLACK_AND_COLORS);

        assertEquals("#020617", theme.background());
        assertEquals("#00f5ff", theme.accent());
        assertEquals("#39ff14", theme.success());
        assertEquals("#fff200", theme.warning());
        assertEquals("#ff1744", theme.danger());
        assertEquals("#ff00ff", theme.borderColor());
    }

    @Test
    void toMapContainsOnlyDefinedValues() {
        HudTheme theme = HudTheme.builder()
                .background("#000")
                .foreground("")
                .accent("#fff")
                .maxHeightPx(420)
                .build();

        assertEquals("#000", theme.toMap().get("background"));
        assertEquals("#fff", theme.toMap().get("accent"));
        assertEquals(420, theme.toMap().get("maxHeightPx"));
        assertTrue(!theme.toMap().containsKey("foreground"));
    }
}

