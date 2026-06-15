package io.github.testlens.core.trace.export;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceFailure;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceHtmlExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void exportContainsMetadataEventsArtifactsFailuresAndRawJson() {
        UiTestLensSession session = sampleSession();

        String html = new TraceHtmlExporter().export(session);

        assertTrue(html.contains("<html"));
        assertTrue(html.contains("Checkout flow"));
        assertTrue(html.contains("STEP_PASSED"));
        assertTrue(html.contains("save.png"));
        assertTrue(html.contains("https://ci.example.com/video.mp4"));
        assertTrue(html.contains("Assertion failed"));
        assertTrue(html.contains("Event type summary"));
        assertTrue(html.contains("Failure summary"));
        assertTrue(html.contains("Raw JSON"));
        assertTrue(html.contains(">PASS<"));
        assertTrue(html.contains(">WARN<"));
    }

    @Test
    void canHideRawJsonAndArtifacts() {
        UiTestLensSession session = sampleSession();

        String html = new TraceHtmlExporter().export(session, TraceHtmlExportOptions.builder()
                .includeJsonPayload(false)
                .includeArtifacts(false)
                .build());

        assertFalse(html.contains("Raw JSON"));
        assertFalse(html.contains("<h2>Artifacts</h2>"));
    }

    @Test
    void lightThemeCssVariablesArePresent() {
        String html = new TraceHtmlExporter().export(UiTestLensSession.start("Light"), TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.LIGHT)
                .build());

        assertTrue(html.contains("color-scheme:light"));
        assertTrue(html.contains("--bg:#f5f7fb"));
        assertTrue(html.contains("--panel:#ffffff"));
    }

    @Test
    void darkThemeCssVariablesArePresent() {
        String html = new TraceHtmlExporter().export(UiTestLensSession.start("Dark"), TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.DARK)
                .build());

        assertTrue(html.contains("color-scheme:dark"));
        assertTrue(html.contains("--bg:#070b12"));
        assertTrue(html.contains("--panel:#101722"));
    }

    @Test
    void autoThemeContainsSystemColorSchemeMediaQuery() {
        String html = new TraceHtmlExporter().export(UiTestLensSession.start("Auto"), TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.AUTO)
                .build());

        assertTrue(html.contains("@media (prefers-color-scheme: dark)"));
        assertTrue(html.contains("--bg:#f5f7fb"));
        assertTrue(html.contains("--bg:#070b12"));
    }

    @Test
    void stackTraceVisibilityIsControlledByOptions() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        TraceFailure failure = TraceFailure.from(new IllegalStateException("bad state"), true);
        session.addEvent(TraceEvent.builder(TraceEventType.ACTION_FAILED, TraceStatus.FAILED, "Click save")
                .failure(failure)
                .message("failed")
                .build());

        String hidden = new TraceHtmlExporter().export(session, TraceHtmlExportOptions.builder()
                .includeStackTraces(false)
                .build());
        String shown = new TraceHtmlExporter().export(session, TraceHtmlExportOptions.builder()
                .includeStackTraces(true)
                .build());

        assertFalse(hidden.contains("IllegalStateException: bad state"));
        assertTrue(shown.contains("IllegalStateException: bad state"));
    }

    @Test
    void escapesSessionData() {
        UiTestLensSession session = UiTestLensSession.start("<script>alert('x')</script>");

        String html = new TraceHtmlExporter().export(session);

        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt;"));
    }

    @Test
    void reportContainsCategorizedTimelineAndAttributesDetails() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        session.addEvent(TraceEvent.builder(TraceEventType.LOCATOR_RESOLVE, TraceStatus.PASSED, "Resolve save")
                .message("resolved")
                .attribute("selector", "[data-testid='save']")
                .build());
        session.addEvent(TraceEvent.builder(TraceEventType.NETWORK_WAIT, TraceStatus.FAILED, "Wait order")
                .message("timeout")
                .build());

        String html = new TraceHtmlExporter().export(session);

        assertTrue(html.contains("Locators"));
        assertTrue(html.contains("Network"));
        assertTrue(html.contains("<details class=\"details\"><summary>Attributes</summary>"));
        assertTrue(html.contains("selector"));
    }

    @Test
    void optionsCanHideEventAndFailureSummaries() {
        UiTestLensSession session = sampleSession();

        String html = new TraceHtmlExporter().export(session, TraceHtmlExportOptions.builder()
                .includeEventTypeSummary(false)
                .includeFailureSummary(false)
                .build());

        assertFalse(html.contains("Event type summary"));
        assertFalse(html.contains("Failure summary"));
    }

    @Test
    void artifactPreviewShowsBadgesForScreenshotVideoAndNetworkLog() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        session.attachScreenshot("Save form", Path.of("target/screenshots/save.png"));
        session.attachArtifact(TraceArtifact.url("Video", io.github.testlens.core.trace.TraceArtifactType.VIDEO, "https://ci.example.com/video.mp4"));
        session.attachArtifact(TraceArtifact.networkLog("Network", Path.of("target", "network.json")));

        String html = new TraceHtmlExporter().export(session);

        assertTrue(html.contains("artifact-screenshot"));
        assertTrue(html.contains("artifact-video"));
        assertTrue(html.contains("artifact-network_log"));
        assertTrue(html.contains("network.json"));
    }

    @Test
    void compactTimelineHidesAttributeDetailsAndShortensMessage() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        session.addEvent(TraceEvent.builder(TraceEventType.ACTION_STARTED, TraceStatus.STARTED, "Long action")
                .message("x".repeat(300))
                .attribute("selector", "save")
                .build());

        String html = new TraceHtmlExporter().export(session, TraceHtmlExportOptions.builder()
                .compactTimeline(true)
                .build());

        assertFalse(html.contains("<summary>Attributes</summary>"));
        assertTrue(html.contains("x".repeat(160) + "..."));
    }

    @Test
    void exportToWritesHtmlFile() throws Exception {
        UiTestLensSession session = sampleSession();
        Path output = tempDir.resolve("trace.html");
        Files.writeString(output, "old report");

        Path written = new TraceHtmlExporter().exportTo(session, output);

        assertEquals(output, written);
        String html = Files.readString(output);
        assertTrue(html.contains("Checkout flow"));
        assertFalse(html.contains("old report"));
    }

    @Test
    void sessionConvenienceExportsHtml() {
        UiTestLensSession session = sampleSession();

        assertTrue(session.exportHtml().contains("Checkout flow"));
    }

    @Test
    void exportToCanUseDefaultReportPath() {
        UiTestLensSession session = UiTestLensSession.start("Default report");
        session.finishPassed();

        Path written = new TraceHtmlExporter().exportToDefault(session);

        assertEquals(TraceHtmlExporter.DEFAULT_OUTPUT_PATH, written);
        assertTrue(Files.exists(written));
    }

    @Test
    void artifactLinksAreRelativeAndExistingImagesGetThumbnails() throws Exception {
        Path screenshot = tempDir.resolve("screens").resolve("save.png");
        Files.createDirectories(screenshot.getParent());
        Files.write(screenshot, new byte[] {1, 2, 3});
        UiTestLensSession session = UiTestLensSession.start("Artifacts");
        session.attachScreenshot("Save", screenshot);
        Path output = tempDir.resolve("reports").resolve("index.html");

        new TraceHtmlExporter().exportTo(session, output);

        String html = Files.readString(output);
        assertTrue(html.contains("href=\"../screens/save.png\""));
        assertTrue(html.contains("<img class=\"artifact-thumb\" src=\"../screens/save.png\""));
    }

    @Test
    void missingArtifactPathIsRenderedAsWarning() {
        UiTestLensSession session = UiTestLensSession.start("Artifacts");
        session.attachScreenshot("Missing", tempDir.resolve("missing.png"));

        String html = new TraceHtmlExporter().export(session);

        assertTrue(html.contains("missing file"));
        assertTrue(html.contains("missing.png"));
    }

    @Test
    void suiteReportContainsMultipleSessionsAndSummaryCounts() {
        UiTestLensSession passed = UiTestLensSession.start("Checkout passed");
        passed.finishPassed();
        UiTestLensSession failed = UiTestLensSession.start("Checkout failed");
        failed.addEvent(TraceEvent.failed(TraceEventType.ACTION_FAILED, "Save", new RuntimeException("boom"), Duration.ofMillis(5)));
        failed.finishFailed(new RuntimeException("final failure"));

        String html = new TraceHtmlExporter().exportSuite(List.of(passed, failed));

        assertTrue(html.contains("Checkout passed"));
        assertTrue(html.contains("Checkout failed"));
        assertTrue(html.contains("Suite summary"));
        assertTrue(html.contains("Total tests"));
        assertTrue(html.contains("Passed"));
        assertTrue(html.contains("Failed"));
        assertTrue(html.contains("Suite failures"));
    }

    @Test
    void suiteReportContainsAnchorsToSessionDetails() {
        UiTestLensSession session = UiTestLensSession.start("Anchored test");
        session.finishPassed();

        String html = new TraceHtmlExporter().exportSuite(List.of(session));

        String anchor = "session-" + session.id();
        assertTrue(html.contains("href=\"#" + anchor + "\""));
        assertTrue(html.contains("id=\"" + anchor + "\""));
    }

    @Test
    void suiteReportEscapesUserControlledText() {
        UiTestLensSession session = UiTestLensSession.start("<script>alert('suite')</script>");
        session.finishPassed();

        String html = new TraceHtmlExporter().exportSuite(List.of(session));

        assertFalse(html.contains("<script>alert"));
        assertTrue(html.contains("&lt;script&gt;alert(&#39;suite&#39;)&lt;/script&gt;"));
    }

    @Test
    void suiteReportArtifactLinksAreRelative() throws Exception {
        Path screenshot = tempDir.resolve("screens").resolve("suite.png");
        Files.createDirectories(screenshot.getParent());
        Files.write(screenshot, new byte[] {1, 2, 3});
        UiTestLensSession session = UiTestLensSession.start("Suite artifacts");
        session.attachScreenshot("Suite screenshot", screenshot);
        Path output = tempDir.resolve("reports").resolve("index.html");

        new TraceHtmlExporter().exportSuiteTo(List.of(session), output);

        String html = Files.readString(output);
        assertTrue(html.contains("href=\"../screens/suite.png\""));
    }

    @Test
    void emptySuiteReportDoesNotFail() {
        String html = new TraceHtmlExporter().exportSuite(List.of());

        assertTrue(html.contains("No Selenium Test Lens sessions recorded."));
        assertTrue(html.contains("No session details available."));
    }

    private UiTestLensSession sampleSession() {
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        session.addEvent(TraceEvent.passed(TraceEventType.STEP_PASSED, "Save form", Duration.ofMillis(42))
                .toBuilder()
                .message("Step passed")
                .attribute("attempt", "1")
                .build());
        session.addEvent(TraceEvent.failed(
                TraceEventType.ASSERTION_FAILED,
                "Toast",
                new AssertionError("Assertion failed"),
                Duration.ofMillis(15)
        ));
        session.attachScreenshot("Save form", Path.of("target/screenshots/save.png"));
        session.attachArtifact(TraceArtifact.url("Video", io.github.testlens.core.trace.TraceArtifactType.VIDEO, "https://ci.example.com/video.mp4"));
        session.finishFailed(new RuntimeException("final failure"));
        return session;
    }
}
