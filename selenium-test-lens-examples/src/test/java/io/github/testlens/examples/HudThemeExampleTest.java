package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudTheme;
import io.github.testlens.hud.HudThemePreset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

class HudThemeExampleTest {

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void hudThemeUsage() {
        WebDriver driver = null; // replace with a real driver

        OverlayConfig glassConfig = OverlayConfig.builder()
                .hudPosition(HudPosition.TOP_RIGHT)
                .hudTheme(HudThemePreset.GLASS)
                .build();

        JsOverlayDebug overlay = new JsOverlayDebug(driver, glassConfig);

        overlay.setStep("Checkout");
        overlay.hudLog("info", "Using glass HUD theme", "local");
    }

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void blackAndColorsHudThemeUsage() {
        WebDriver driver = null; // replace with a real driver

        OverlayConfig blackAndColorsConfig = OverlayConfig.builder()
                .hudPosition(HudPosition.TOP_RIGHT)
                .hudTheme(HudThemePreset.BLACK_AND_COLORS)
                .build();

        JsOverlayDebug overlay = new JsOverlayDebug(driver, blackAndColorsConfig);

        overlay.setStep("Checkout");
        overlay.hudLog("info", "Using black and colors HUD theme", "local");
    }

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void customHudThemeUsage() {
        WebDriver driver = null; // replace with a real driver

        HudTheme customTheme = HudTheme.builder()
                .background("rgba(15, 23, 42, 0.92)")
                .foreground("#f8fafc")
                .accent("#38bdf8")
                .borderRadiusPx(16)
                .fontFamily("Inter, system-ui, sans-serif")
                .maxHeightPx(420)
                .build();

        OverlayConfig config = OverlayConfig.builder()
                .hudTheme(customTheme)
                .build();

        JsOverlayDebug overlay = new JsOverlayDebug(driver, config);

        overlay.setStep("Checkout");
        overlay.hudLog("info", "Using a custom HUD theme", "local");
    }
}

