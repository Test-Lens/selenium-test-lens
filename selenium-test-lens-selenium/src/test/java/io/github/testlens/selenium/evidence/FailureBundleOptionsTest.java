package io.github.testlens.selenium.evidence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FailureBundleOptionsTest {
    @Test
    void safeDefaultsEnableBundleButExcludeSensitiveCollectors() {
        FailureBundleOptions options = FailureBundleOptions.defaults();
        assertTrue(options.enabled());
        assertTrue(options.diagnosticScreenshot());
        assertTrue(options.cleanScreenshot());
        assertTrue(options.context());
        assertTrue(options.diagnostics());
        assertTrue(options.networkSummary());
        assertTrue(options.runtimeMetadata());
        assertTrue(options.configurationSnapshot());
        assertTrue(options.zipArchive());
        assertFalse(options.pageSource());
        assertFalse(options.browserConsole());
        assertEquals(5L * 1024L * 1024L, options.maxTextArtifactBytes());
        assertEquals(1_000, options.maxConsoleEntries());
    }

    @Test
    void completePresetExplicitlyEnablesSensitiveCollectors() {
        FailureBundleOptions options = FailureBundleOptions.complete();
        assertTrue(options.pageSource());
        assertTrue(options.browserConsole());
    }

    @Test
    void builderValidatesLimitsAndPreservesFlags() {
        assertThrows(IllegalArgumentException.class,
                () -> FailureBundleOptions.builder().maxTextArtifactBytes(0));
        assertThrows(IllegalArgumentException.class,
                () -> FailureBundleOptions.builder().maxConsoleEntries(-1));
        FailureBundleOptions options = FailureBundleOptions.builder()
                .enabled(false).zipArchive(false).maxTextArtifactBytes(19).maxConsoleEntries(2).build();
        assertFalse(options.enabled());
        assertFalse(options.zipArchive());
        assertEquals(19, options.maxTextArtifactBytes());
        assertEquals(2, options.maxConsoleEntries());
    }
}
