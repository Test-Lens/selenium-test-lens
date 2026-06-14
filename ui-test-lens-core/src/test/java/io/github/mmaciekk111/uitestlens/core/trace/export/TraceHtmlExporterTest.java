package io.github.mmaciekk111.uitestlens.core.trace.export;

import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifact;
import io.github.mmaciekk111.uitestlens.core.trace.TraceEvent;
import io.github.mmaciekk111.uitestlens.core.trace.TraceEventType;
import io.github.mmaciekk111.uitestlens.core.trace.TraceFailure;
import io.github.mmaciekk111.uitestlens.core.trace.TraceStatus;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

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
        session.attachArtifact(TraceArtifact.url("Video", io.github.mmaciekk111.uitestlens.core.trace.TraceArtifactType.VIDEO, "https://ci.example.com/video.mp4"));
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

        Path written = new TraceHtmlExporter().exportTo(session, output);

        assertEquals(output, written);
        assertTrue(Files.readString(output).contains("Checkout flow"));
    }

    @Test
    void sessionConvenienceExportsHtml() {
        UiTestLensSession session = sampleSession();

        assertTrue(session.exportHtml().contains("Checkout flow"));
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
        session.attachArtifact(TraceArtifact.url("Video", io.github.mmaciekk111.uitestlens.core.trace.TraceArtifactType.VIDEO, "https://ci.example.com/video.mp4"));
        session.finishFailed(new RuntimeException("final failure"));
        return session;
    }
}
