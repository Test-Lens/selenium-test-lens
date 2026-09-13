package io.github.testlens.hud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HudOptionsTest {
    @TempDir Path temp;

    @Test void compactIsTheDefault() {
        HudOptions options = HudOptions.defaults();
        assertEquals(HudPreset.COMPACT, options.preset());
        assertFalse(options.showPipeline());
        assertFalse(options.showTimestamps());
        assertTrue(options.showEventLog());
        assertEquals(HudBranding.TEST_LENS, options.branding());
        assertEquals(HudPosition.BOTTOM_RIGHT, options.position());
        assertEquals(HudHeaderLayout.AUTO, options.headerLayout());
        assertEquals(10, options.offsetXPx());
        assertEquals(10, options.offsetYPx());
        assertEquals(280, options.maxHeightPx());
        assertEquals(16, options.railWidthPx());
        assertEquals(HudFontPreset.UI_SANS, options.fontPreset());
        assertTrue(options.typography().header().isEmpty());
        assertTrue(options.typography().currentStep().isEmpty());
        assertTrue(options.typography().eventLog().isEmpty());
        assertTrue(options.typography().metadata().isEmpty());
        assertEquals(HudScrollbarStyle.SUBTLE, options.scrollbarStyle());
        assertEquals(6, options.scrollbarWidthPx());
        assertEquals("#111827", options.scrollbarTrackColor());
        assertEquals("#64748b", options.scrollbarThumbColor());
        assertEquals("#94a3b8", options.scrollbarThumbHoverColor());
        assertEquals(HudLogoPlacement.LEFT_RAIL, options.logoPlacement());
    }

    @Test void laterOverridesWinAfterPreset() {
        HudOptions options = HudOptions.builder().preset(HudPreset.DEBUG)
                .showPipeline(false).showNetwork(false).backgroundOpacity(.75).widthPx(480)
                .offsetXPx(16).offsetYPx(24).maxHeightPx(440).railWidthPx(30)
                .fontPreset(HudFontPreset.SYSTEM).baseFontSizePx(12).headerFontSizePx(9)
                .scrollbarStyle(HudScrollbarStyle.SUBTLE).scrollbarWidthPx(7)
                .scrollbarTrackColor("#020617").scrollbarThumbColor("#334155")
                .scrollbarThumbHoverColor("#64748b")
                .logoPlacement(HudLogoPlacement.HEADER_RIGHT).build();
        assertEquals(HudPreset.DEBUG, options.preset());
        assertFalse(options.showPipeline());
        assertFalse(options.showNetwork());
        assertEquals(.75, options.backgroundOpacity());
        assertEquals(480, options.widthPx());
        assertEquals(16, options.offsetXPx());
        assertEquals(24, options.offsetYPx());
        assertEquals(440, options.maxHeightPx());
        assertEquals(30, options.railWidthPx());
        assertEquals(HudFontPreset.SYSTEM, options.fontPreset());
        assertEquals(HudScrollbarStyle.SUBTLE, options.scrollbarStyle());
        assertEquals(7, options.scrollbarWidthPx());
        assertEquals(HudLogoPlacement.HEADER_RIGHT, options.logoPlacement());
    }

    @Test void explicitOverridesWinWhenPresetIsSelectedLater() {
        HudOptions options = HudOptions.builder().showPipeline(true)
                .headerLayout(HudHeaderLayout.STACKED).preset(HudPreset.MINIMAL).build();
        assertTrue(options.showPipeline());
        assertEquals(HudHeaderLayout.STACKED, options.headerLayout());
        assertFalse(options.showEventLog());
        assertEquals(280, options.widthPx());
    }

    @Test void explicitOverridesAreIndependentOfBuilderCallOrderAcrossAllGroups() {
        HudOptions first = customized(true);
        HudOptions last = customized(false);

        assertEquals(snapshot(first), snapshot(last));
        assertEquals(HudPreset.DEBUG, first.preset());
        assertFalse(first.showPipeline());
        assertEquals(HudPosition.TOP_LEFT, first.position());
        assertEquals(HudHeaderLayout.INLINE, first.headerLayout());
        assertEquals(HudFontPreset.SYSTEM, first.fontPreset());
        assertEquals(HudBranding.NONE, first.branding());
    }

    @Test void validatesColorsOpacityAndDimensions() {
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().background("red"));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().accentColor("url(x)"));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().backgroundOpacity(1.01));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().backgroundOpacity(-0.01));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().backgroundOpacity(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().backgroundOpacity(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().widthPx(239));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().maxLogHeightPx(79));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().offsetXPx(501));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().maxHeightPx(119));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().railWidthPx(15));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().baseFontSizePx(8));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().scrollbarWidthPx(3));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().scrollbarWidthPx(15));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().scrollbarThumbColor("rgba(0,0,0,.5)"));
        assertThrows(NullPointerException.class, () -> HudOptions.builder().scrollbarStyle(null));
        assertThrows(NullPointerException.class, () -> HudOptions.builder().headerLayout(null));
    }

    @Test void sectionTypographyOverridesTheGlobalPresetAndIgnoresBuilderOrder() {
        HudTypography typography = HudTypography.builder()
                .header(HudFontPreset.MONOSPACE)
                .eventLog(HudFontPreset.SYSTEM)
                .build();

        HudOptions beforePreset = HudOptions.builder()
                .fontPreset(HudFontPreset.UI_SANS)
                .typography(typography)
                .preset(HudPreset.DEBUG)
                .build();
        HudOptions afterPreset = HudOptions.builder()
                .preset(HudPreset.DEBUG)
                .fontPreset(HudFontPreset.UI_SANS)
                .typography(typography)
                .build();

        assertEquals(snapshot(beforePreset), snapshot(afterPreset));
        assertEquals(HudFontPreset.MONOSPACE, beforePreset.typography().header().orElseThrow());
        assertEquals(HudFontPreset.SYSTEM, beforePreset.typography().eventLog().orElseThrow());
        assertTrue(beforePreset.typography().currentStep().isEmpty());
        assertTrue(beforePreset.typography().metadata().isEmpty());
        assertEquals("{header=MONOSPACE, eventLog=SYSTEM}", beforePreset.typography().toRuntimeMap().toString());
    }

    @Test void scrollbarOverridesWinRegardlessOfPresetCallOrderAndNativeRemainsAvailable() {
        HudOptions beforePreset = HudOptions.builder()
                .scrollbarStyle(HudScrollbarStyle.STANDARD)
                .scrollbarWidthPx(8)
                .scrollbarTrackColor("#010203")
                .scrollbarThumbColor("#040506")
                .scrollbarThumbHoverColor("#070809")
                .preset(HudPreset.COMPACT)
                .build();
        HudOptions afterPreset = HudOptions.builder()
                .preset(HudPreset.COMPACT)
                .scrollbarStyle(HudScrollbarStyle.STANDARD)
                .scrollbarWidthPx(8)
                .scrollbarTrackColor("#010203")
                .scrollbarThumbColor("#040506")
                .scrollbarThumbHoverColor("#070809")
                .build();

        assertEquals(snapshot(beforePreset), snapshot(afterPreset));
        assertEquals(HudScrollbarStyle.STANDARD, beforePreset.scrollbarStyle());
        assertEquals(8, beforePreset.scrollbarWidthPx());
        assertEquals("#010203", beforePreset.scrollbarTrackColor());
        assertEquals("#040506", beforePreset.scrollbarThumbColor());
        assertEquals("#070809", beforePreset.scrollbarThumbHoverColor());
        assertEquals(HudScrollbarStyle.NATIVE,
                HudOptions.builder().scrollbarStyle(HudScrollbarStyle.NATIVE).build().scrollbarStyle());
        assertEquals(HudScrollbarStyle.STANDARD,
                HudOptions.builder().preset(HudPreset.DEBUG).build().scrollbarStyle());
    }

    @Test void acceptsOnlyBoundedRegularPngLogo() throws Exception {
        Path png = temp.resolve("company.png");
        Files.write(png, pngHeader(32, 16));
        HudOptions options = HudOptions.builder().customLogo(png).branding(HudBranding.BOTH).build();
        assertTrue(options.hasCustomLogo());
        assertEquals("company.png", options.customLogoFileName());
        assertTrue(HudOptions.builder().branding(HudBranding.CUSTOM).customLogo(png)
                .preset(HudPreset.DEBUG).build().hasCustomLogo());
        assertTrue(HudOptions.builder().preset(HudPreset.DEBUG).customLogo(png)
                .branding(HudBranding.CUSTOM).build().hasCustomLogo());

        Path text = temp.resolve("logo.svg");
        Files.writeString(text, "<svg onload='alert(1)'></svg>");
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().customLogo(text));
        Path oversized = temp.resolve("oversized.png");
        Files.write(oversized, pngHeader(4097, 1));
        assertThrows(IllegalArgumentException.class, () -> HudOptions.builder().customLogo(oversized));
        assertThrows(IllegalStateException.class, () -> HudOptions.builder().branding(HudBranding.CUSTOM).build());
        assertThrows(IllegalStateException.class, () -> HudOptions.builder().customLogo(png).build());
        assertThrows(IllegalStateException.class, () -> HudOptions.builder().customLogo(png)
                .preset(HudPreset.COMPACT).build());
    }

    private HudOptions customized(boolean presetFirst) {
        HudOptions.Builder builder = HudOptions.builder();
        if (presetFirst) builder.preset(HudPreset.DEBUG);
        builder.showPipeline(false).showNetwork(false).showTestName(false).showTimestamps(false)
                .widthPx(480).maxHeightPx(320).maxLogHeightPx(140).railWidthPx(24)
                .position(HudPosition.TOP_LEFT).headerLayout(HudHeaderLayout.INLINE)
                .offsetXPx(21).offsetYPx(22)
                .fontPreset(HudFontPreset.SYSTEM).baseFontSizePx(12).headerFontSizePx(9)
                .scrollbarStyle(HudScrollbarStyle.STANDARD).scrollbarWidthPx(8)
                .scrollbarTrackColor("#151617").scrollbarThumbColor("#18191a")
                .scrollbarThumbHoverColor("#1b1c1d")
                .background("#010203").backgroundOpacity(.75).accentColor("#040506")
                .primaryTextColor("#070809").mutedTextColor("#0a0b0c")
                .successColor("#0d0e0f").warningColor("#101112").failureColor("#131415")
                .branding(HudBranding.NONE).logoPlacement(HudLogoPlacement.HEADER_RIGHT);
        if (!presetFirst) builder.preset(HudPreset.DEBUG);
        return builder.build();
    }

    private static String snapshot(HudOptions value) {
        return value.toRuntimeMap().toString();
    }

    private static byte[] pngHeader(int width, int height) {
        byte[] bytes = new byte[24];
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        System.arraycopy(signature, 0, bytes, 0, signature.length);
        bytes[12] = 'I'; bytes[13] = 'H'; bytes[14] = 'D'; bytes[15] = 'R';
        bytes[16] = (byte) (width >>> 24); bytes[17] = (byte) (width >>> 16);
        bytes[18] = (byte) (width >>> 8); bytes[19] = (byte) width;
        bytes[20] = (byte) (height >>> 24); bytes[21] = (byte) (height >>> 16);
        bytes[22] = (byte) (height >>> 8); bytes[23] = (byte) height;
        return bytes;
    }
}
