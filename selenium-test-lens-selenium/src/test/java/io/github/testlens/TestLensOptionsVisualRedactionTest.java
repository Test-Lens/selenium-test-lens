package io.github.testlens;

import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPreset;
import io.github.testlens.selenium.evidence.VisualRedactionOptions;
import io.github.testlens.selenium.evidence.VisualRedactionFailurePolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLensOptionsVisualRedactionTest {
    @Test
    void defaultsEnableAutomaticPasswordMaskingAndFailClosedPolicy() {
        assertTrue(TestLensOptions.defaults().visualRedaction().maskPasswordInputs());
        assertEquals(VisualRedactionFailurePolicy.STRICT,
                TestLensOptions.defaults().visualRedaction().failurePolicy());
    }

    @Test
    void explicitVisualRedactionIsPreserved() {
        VisualRedactionOptions disabled = VisualRedactionOptions.disabled();
        assertFalse(TestLensOptions.builder().visualRedaction(disabled).build()
                .visualRedaction().hasMasks());
    }

    @Test
    void hudAndVisualRedactionRemainIndependentBuilderOptions() {
        HudOptions hud = HudOptions.builder().preset(HudPreset.DEBUG).showPipeline(false).build();
        VisualRedactionOptions visual = VisualRedactionOptions.disabled();

        TestLensOptions options = TestLensOptions.builder().visualRedaction(visual).hud(hud).build();

        assertSame(hud, options.hud());
        assertSame(hud, options.overlayConfig().getHudOptions());
        assertSame(visual, options.visualRedaction());
    }

    @Test
    void highlightsRemainIndependentFromHudRedactionAndScreenshotPolicy() {
        HudOptions hud = HudOptions.builder().preset(HudPreset.DEBUG).build();
        VisualRedactionOptions visual = VisualRedactionOptions.disabled();
        HighlightOptions highlights = HighlightOptions.builder().automaticFeedback(false)
                .successColor("rebeccapurple").build();

        TestLensOptions options = TestLensOptions.builder()
                .hud(hud)
                .visualRedaction(visual)
                .screenshotOnFailure(false)
                .highlights(highlights)
                .build();

        assertSame(hud, options.hud());
        assertSame(visual, options.visualRedaction());
        assertFalse(options.screenshotOnFailure());
        assertEquals("rebeccapurple", options.highlights().successColor());
        assertFalse(options.highlights().automaticFeedback());
        assertSame(options.highlights(), options.highlight());
    }
}
