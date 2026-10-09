package io.github.testlens.studio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestEngineeringStudioStartNavigationTest {
    @Test void configuredStartReplacesFreshBrowserInternalPagesOnEveryPlatform() {
        assertTrue(TestEngineeringStudioService.requiresConfiguredStartNavigation(null));
        assertTrue(TestEngineeringStudioService.requiresConfiguredStartNavigation("about:blank"));
        assertTrue(TestEngineeringStudioService.requiresConfiguredStartNavigation("about:newtab"));
        assertTrue(TestEngineeringStudioService.requiresConfiguredStartNavigation("data:,"));
        assertTrue(TestEngineeringStudioService.requiresConfiguredStartNavigation("chrome://newtab/"));
        assertTrue(TestEngineeringStudioService.requiresConfiguredStartNavigation("edge://newtab/"));
    }

    @Test void configuredStartDoesNotReplaceAnAlreadyNavigatedApplicationPage() {
        assertFalse(TestEngineeringStudioService.requiresConfiguredStartNavigation("http://127.0.0.1:18181/login"));
        assertFalse(TestEngineeringStudioService.requiresConfiguredStartNavigation("https://example.test/login"));
    }
}
