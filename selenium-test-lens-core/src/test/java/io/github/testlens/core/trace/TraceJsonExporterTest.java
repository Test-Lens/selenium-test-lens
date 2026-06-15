package io.github.testlens.core.trace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceJsonExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void exportWorksForEmptySession() {
        UiTestLensSession session = UiTestLensSession.start("Empty");

        String json = session.exportJson();

        assertTrue(json.contains("\"schemaVersion\":\"1.0\""));
        assertTrue(json.contains("\"reportType\":\"session\""));
        assertTrue(json.contains("\"session\""));
        assertTrue(json.contains("\"name\":\"Empty\""));
        assertTrue(json.contains("\"events\""));
    }

    @Test
    void exportWorksForSessionWithEventsAndEscapedText() {
        UiTestLensSession session = UiTestLensSession.start("Checkout \"flow\"");
        session.addEvent(TraceEvent.info("line\nbreak", "message with \"quotes\"")
                .toBuilder()
                .attribute("selector", "[data-testid=\"save\"]")
                .build());
        session.finishFailed(new RuntimeException("failed \"reason\""));

        String json = session.exportJson();

        assertTrue(json.contains("Checkout \\\"flow\\\""));
        assertTrue(json.contains("line\\nbreak"));
        assertTrue(json.contains("message with \\\"quotes\\\""));
        assertTrue(json.contains("failed \\\"reason\\\""));
        assertTrue(json.contains("\"attributes\""));
        assertTrue(json.contains("\"metadata\""));
    }

    @Test
    void suiteJsonContainsMultipleSessionsAndSummaryCounts() {
        UiTestLensSession passed = UiTestLensSession.start("Passed");
        passed.finishPassed();
        UiTestLensSession failed = UiTestLensSession.start("Failed");
        failed.addEvent(TraceEvent.builder(TraceEventType.ACTION_STARTED, TraceStatus.WARNING, "Slow action").build());
        failed.addEvent(TraceEvent.failed(TraceEventType.ACTION_FAILED, "Save", new RuntimeException("boom"), Duration.ofMillis(7)));
        failed.finishFailed(new RuntimeException("final failure"));

        String json = new TraceJsonExporter().exportSuite(List.of(passed, failed));

        assertTrue(json.contains("\"reportType\":\"suite\""));
        assertTrue(json.contains("\"name\":\"Passed\""));
        assertTrue(json.contains("\"name\":\"Failed\""));
        assertTrue(json.contains("\"totalSessions\":2"));
        assertTrue(json.contains("\"passed\":1"));
        assertTrue(json.contains("\"failed\":1"));
        assertTrue(json.contains("\"warnings\":1"));
    }

    @Test
    void artifactFilePathsAreRelativeAndMissingArtifactsAreRepresented() throws Exception {
        Path screenshot = tempDir.resolve("screens").resolve("save.png");
        Files.createDirectories(screenshot.getParent());
        Files.write(screenshot, new byte[] {1, 2, 3});
        UiTestLensSession session = UiTestLensSession.start("Artifacts");
        session.attachScreenshot("Save", screenshot);
        session.attachScreenshot("Missing", tempDir.resolve("screens").resolve("missing.png"));
        Path output = tempDir.resolve("reports").resolve("report.json");

        new TraceJsonExporter().exportTo(session, output);

        String json = Files.readString(output);
        assertTrue(json.contains("\"relativePath\":\"../screens/save.png\""));
        assertTrue(json.contains("\"exists\":true"));
        assertTrue(json.contains("\"sizeBytes\":3"));
        assertTrue(json.contains("missing.png"));
        assertTrue(json.contains("\"exists\":false"));
    }

    @Test
    void missingArtifactsCanBeExcluded() {
        UiTestLensSession session = UiTestLensSession.start("Artifacts");
        session.attachScreenshot("Missing", tempDir.resolve("missing.png"));

        String json = new TraceJsonExporter().export(session, TraceJsonExportOptions.builder()
                .includeMissingArtifacts(false)
                .build());

        assertFalse(json.contains("\"exists\":false"));
    }

    @Test
    void writeToFileCreatesParentDirectoriesAndOverwrites() throws Exception {
        UiTestLensSession session = UiTestLensSession.start("Write");
        Path output = tempDir.resolve("nested").resolve("trace.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, "old");

        Path written = new TraceJsonExporter().exportTo(session, output);

        assertEquals(output, written);
        String json = Files.readString(output);
        assertTrue(json.contains("\"reportType\":\"session\""));
        assertFalse(json.contains("old"));
    }
}

