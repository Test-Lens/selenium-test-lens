package io.github.testlens.core.trace.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceHtmlExportOptionsTest {

    @Test
    void defaultsMatchReportDefaults() {
        TraceHtmlExportOptions options = TraceHtmlExportOptions.defaults();

        assertEquals("Selenium Test Lens Trace", options.title());
        assertTrue(options.includeJsonPayload());
        assertTrue(options.includeArtifacts());
        assertFalse(options.includeStackTraces());
        assertTrue(options.includeAttributes());
        assertFalse(options.collapsePassedEvents());
        assertTrue(options.groupTimelineByCategory());
        assertTrue(options.includeEventTypeSummary());
        assertTrue(options.includeFailureSummary());
        assertTrue(options.includeArtifactPreview());
        assertTrue(options.includeDurationSummary());
        assertFalse(options.compactTimeline());
        assertEquals(HtmlReportTheme.AUTO, options.theme());
        assertEquals(1000, options.maxMessageLength());
    }

    @Test
    void builderOverridesValues() {
        TraceHtmlExportOptions options = TraceHtmlExportOptions.builder()
                .title("Custom")
                .includeJsonPayload(false)
                .includeArtifacts(false)
                .includeStackTraces(true)
                .includeAttributes(false)
                .collapsePassedEvents(true)
                .groupTimelineByCategory(false)
                .includeEventTypeSummary(false)
                .includeFailureSummary(false)
                .includeArtifactPreview(false)
                .includeDurationSummary(false)
                .compactTimeline(true)
                .theme(HtmlReportTheme.LIGHT)
                .maxMessageLength(50)
                .build();

        assertEquals("Custom", options.title());
        assertFalse(options.includeJsonPayload());
        assertFalse(options.includeArtifacts());
        assertTrue(options.includeStackTraces());
        assertFalse(options.includeAttributes());
        assertTrue(options.collapsePassedEvents());
        assertFalse(options.groupTimelineByCategory());
        assertFalse(options.includeEventTypeSummary());
        assertFalse(options.includeFailureSummary());
        assertFalse(options.includeArtifactPreview());
        assertFalse(options.includeDurationSummary());
        assertTrue(options.compactTimeline());
        assertEquals(HtmlReportTheme.LIGHT, options.theme());
        assertEquals(50, options.maxMessageLength());
    }

    @Test
    void rejectsNegativeMessageLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> TraceHtmlExportOptions.builder().maxMessageLength(-1).build());
    }
}
