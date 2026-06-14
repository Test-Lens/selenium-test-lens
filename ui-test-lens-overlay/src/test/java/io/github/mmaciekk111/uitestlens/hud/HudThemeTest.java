package io.github.mmaciekk111.uitestlens.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                .build();

        assertEquals("rgba(15, 23, 42, 0.92)", theme.background());
        assertEquals("#f8fafc", theme.foreground());
        assertEquals("#38bdf8", theme.accent());
        assertEquals(16, theme.borderRadiusPx());
        assertEquals("Inter, system-ui, sans-serif", theme.fontFamily());
        assertEquals(0.9, theme.opacity());
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
    void toMapContainsOnlyDefinedValues() {
        HudTheme theme = HudTheme.builder()
                .background("#000")
                .foreground("")
                .accent("#fff")
                .build();

        assertEquals("#000", theme.toMap().get("background"));
        assertEquals("#fff", theme.toMap().get("accent"));
        assertTrue(!theme.toMap().containsKey("foreground"));
    }
}
