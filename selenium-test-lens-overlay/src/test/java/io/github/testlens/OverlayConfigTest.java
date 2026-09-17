package io.github.testlens;

import io.github.testlens.hud.HudTheme;
import io.github.testlens.hud.HudThemePreset;
import io.github.testlens.hud.HudPreset;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPosition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

class OverlayConfigTest {

    @Test
    void defaultsIncludeDefaultHudTheme() {
        OverlayConfig config = OverlayConfig.builder().build();

        assertEquals(HudThemePreset.DEFAULT, config.getHudThemePreset());
        assertNotNull(config.getHudTheme());
        assertEquals("rgba(15, 23, 42, 0.96)", config.getHudTheme().background());
        assertEquals(420, config.getHudMaxWidthPx());
        assertEquals(HudPreset.COMPACT, config.getHudOptions().preset());
        assertTrue(config.isHudOptionsAuthoritative());
    }

    @Test
    void presetSetsHudTheme() {
        OverlayConfig config = OverlayConfig.builder()
                .hudTheme(HudThemePreset.GLASS)
                .build();

        assertEquals(HudThemePreset.GLASS, config.getHudThemePreset());
        assertEquals("#38bdf8", config.getHudTheme().accent());
        assertFalse(config.isHudOptionsAuthoritative());
    }

    @Test
    void userConfiguredHudWidthOverridesDefault() {
        OverlayConfig config = OverlayConfig.builder()
                .hudMaxWidthPx(360)
                .build();

        assertEquals(360, config.getHudMaxWidthPx());
    }

    @Test
    void customThemeClearsPresetMarker() {
        HudTheme custom = HudTheme.builder()
                .background("#111")
                .foreground("#eee")
                .build();

        OverlayConfig config = OverlayConfig.builder()
                .hudTheme(HudThemePreset.DARK)
                .hudTheme(custom)
                .build();

        assertNull(config.getHudThemePreset());
        assertEquals("#111", config.getHudTheme().background());
        assertEquals("#eee", config.getHudTheme().foreground());
        assertFalse(config.isHudOptionsAuthoritative());
    }

    @Test
    void explicitHudOptionsWinOverLegacyHudSettingsRegardlessOfCallOrder() {
        HudOptions hud = HudOptions.builder().preset(HudPreset.DEBUG)
                .position(HudPosition.TOP_LEFT).widthPx(480).build();

        OverlayConfig legacyFirst = OverlayConfig.builder()
                .hudPosition(HudPosition.BOTTOM_RIGHT).hudMaxWidthPx(360)
                .hudTheme(HudThemePreset.GLASS).hudOptions(hud).build();
        OverlayConfig legacyLast = OverlayConfig.builder()
                .hudOptions(hud).hudPosition(HudPosition.BOTTOM_RIGHT).hudMaxWidthPx(360)
                .hudTheme(HudThemePreset.GLASS).build();

        assertEquals(HudPosition.TOP_LEFT, legacyFirst.getHudPosition());
        assertEquals(HudPosition.TOP_LEFT, legacyLast.getHudPosition());
        assertEquals(480, legacyFirst.getHudMaxWidthPx());
        assertEquals(480, legacyLast.getHudMaxWidthPx());
        assertEquals(hud, legacyFirst.getHudOptions());
        assertEquals(hud, legacyLast.getHudOptions());
        assertTrue(legacyFirst.isHudOptionsAuthoritative());
        assertTrue(legacyLast.isHudOptionsAuthoritative());
    }

    @Test
    void typedHighlightOptionsWinOverLegacySettingsRegardlessOfCallOrder() {
        HighlightOptions typed = HighlightOptions.builder().actionColor("#112233")
                .successColor("#224466").failureColor("#662244").durationMs(3210).borderWidthPx(5).build();

        OverlayConfig legacyFirst = OverlayConfig.builder().highlightColor("#ffffff")
                .decorationDurationMs(12).highlightOptions(typed).build();
        OverlayConfig legacyLast = OverlayConfig.builder().highlightOptions(typed)
                .highlightColor("#ffffff").decorationDurationMs(12).build();

        assertEquals("#112233", legacyFirst.getHighlightColor());
        assertEquals("#112233", legacyLast.getHighlightColor());
        assertEquals(12, legacyFirst.getDecorationDurationMs());
        assertEquals(12, legacyLast.getDecorationDurationMs());
        assertEquals(3210, legacyFirst.getHighlightOptions().durationMs());
        assertEquals(3210, legacyLast.getHighlightOptions().durationMs());
        assertEquals(5, legacyFirst.getHighlightOptions().borderWidthPx());
        assertSame(typed, legacyLast.getHighlightOptions());
        assertTrue(legacyFirst.isHighlightOptionsAuthoritative());
        assertTrue(legacyLast.isHighlightOptionsAuthoritative());
    }

    @Test
    void legacyHighlightSettingsPopulateTypedDefaults() {
        OverlayConfig config = OverlayConfig.builder().highlightColor("#abcdef").decorationDurationMs(99).build();

        assertEquals("#abcdef", config.getHighlightOptions().actionColor());
        assertEquals(99, config.getHighlightOptions().durationMs());
        assertFalse(config.isHighlightOptionsAuthoritative());
    }

    @Test
    void unsetTypedFieldsInheritLegacyValuesInEitherCallOrder() {
        HighlightOptions partial = HighlightOptions.builder().successColor("tomato").showLabels(false).build();

        OverlayConfig legacyFirst = OverlayConfig.builder().highlightColor("rebeccapurple")
                .decorationDurationMs(0).highlightOptions(partial).build();
        OverlayConfig legacyLast = OverlayConfig.builder().highlightOptions(partial)
                .highlightColor("rebeccapurple").decorationDurationMs(0).build();

        for (OverlayConfig config : new OverlayConfig[]{legacyFirst, legacyLast}) {
            assertEquals("rebeccapurple", config.getHighlightOptions().actionColor());
            assertEquals(0, config.getHighlightOptions().durationMs());
            assertEquals("tomato", config.getHighlightOptions().successColor());
            assertFalse(config.getHighlightOptions().showLabels());
        }
    }

    @Test
    void explicitTypedLegacyOverlapsWinButDoNotChangeOtherDecorationDuration() {
        HighlightOptions explicit = HighlightOptions.builder().actionColor("color(display-p3 1 0 0)")
                .durationMs(77).build();
        OverlayConfig config = OverlayConfig.builder().highlightOptions(explicit)
                .highlightColor("#000000").decorationDurationMs(222).build();

        assertEquals("color(display-p3 1 0 0)", config.getHighlightColor());
        assertEquals(77, config.getHighlightOptions().durationMs());
        assertEquals(222, config.getDecorationDurationMs());
    }
}

