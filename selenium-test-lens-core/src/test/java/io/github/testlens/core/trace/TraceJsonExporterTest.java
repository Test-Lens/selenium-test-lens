package io.github.testlens.core.trace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertTrue(json.contains("\"flakiness\":{\"flakyCandidate\":false,\"totalRetries\":0,\"timeLostMs\":0,\"policy\":\"REPORT_ONLY\",\"policyTriggered\":false"));
    }

    @Test
    void exportContainsStableFlakinessAggregation() {
        UiTestLensSession session = UiTestLensSession.start("retry", RetryOutcomePolicy.WARN, 0);
        session.addEvent(TraceEvent.builder(TraceEventType.RETRY, TraceStatus.WARNING, "retry")
                .duration(Duration.ofMillis(37))
                .attribute("retry.action", "click")
                .attribute("retry.locator", "save")
                .attribute("retry.exceptionType", "org.openqa.selenium.StaleElementReferenceException")
                .build());
        session.finishPassed();

        String json = session.exportJson();

        assertTrue(json.contains("\"flakyCandidate\":true"));
        assertTrue(json.contains("\"totalRetries\":1"));
        assertTrue(json.contains("\"timeLostMs\":37"));
        assertTrue(json.contains("\"policy\":\"WARN\""));
        assertTrue(json.contains("\"byAction\":{\"click\":1}"));
        assertTrue(json.contains("\"byLocator\":{\"save\":1}"));
        assertTrue(json.contains("\"byException\":{\"org.openqa.selenium.StaleElementReferenceException\":1}"));
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
    void structuredLocatorIsProjectedOnceWithoutLeakingInternalKeys() {
        UiTestLensSession session = UiTestLensSession.start("locator");
        session.addEvent(TraceEvent.builder(TraceEventType.LOCATOR_RESOLVE, TraceStatus.PASSED, "find")
                .attribute("metadata.testlens.selector.schemaVersion", "1")
                .attribute("metadata.testlens.selector.locator.strategy", "id")
                .attribute("metadata.testlens.selector.locator.value", "save")
                .attribute("metadata.testlens.selector.locator.valueState", "KNOWN")
                .attribute("metadata.testlens.selector.locator.display", "By.id: save")
                .attribute("metadata.testlens.selector.locator.supportKind", "STRUCTURED")
                .attribute("metadata.testlens.selector.context.knowledge", "KNOWN")
                .attribute("metadata.testlens.selector.context.segmentCount", "1")
                .attribute("metadata.testlens.selector.context.segment.0.kind", "DRIVER_ROOT")
                .attribute("metadata.testlens.selector.context.segment.0.knowledge", "KNOWN")
                .attribute("metadata.testlens.selector.usageIntent", "FIND_ONE")
                .attribute("metadata.testlens.selector.outcome", "RESOLVED")
                .attribute("metadata.testlens.selector.matchCount.knowledge", "UNKNOWN")
                .attribute("target.selector", "By.id: save")
                .build());

        String json = session.exportJson();

        assertTrue(json.contains("\"schemaVersion\":\"1.0\""));
        assertTrue(json.contains("\"locatorObservation\":{\"schemaVersion\":1"));
        assertTrue(json.contains("\"strategy\":\"id\""));
        assertTrue(json.contains("\"segments\":[{\"kind\":\"DRIVER_ROOT\",\"knowledge\":\"KNOWN\"}]"));
        assertTrue(json.contains("\"declarationSource\":{\"knowledge\":\"UNKNOWN\"}"));
        assertTrue(json.contains("\"target.selector\":\"By.id: save\""));
        assertFalse(json.contains("metadata.testlens.selector"));
        assertEquals(2, occurrences(json, "\"locatorObservation\""),
                "session export intentionally mirrors its event list at the root and inside session");
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        for (int index = 0; (index = value.indexOf(needle, index)) >= 0; index += needle.length()) count++;
        return count;
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
    void unfinishedSuiteIsReportedAsStartedWithoutMutatingTheSession() {
        UiTestLensSession unfinished = UiTestLensSession.start("unfinished");
        List<TraceEvent> eventsBefore = unfinished.events();
        List<TraceArtifact> artifactsBefore = unfinished.artifacts();

        String json = new TraceJsonExporter().exportSuite(List.of(unfinished));

        assertTrue(json.contains("\"status\":\"STARTED\""));
        assertTrue(json.contains("\"summary\":{\"totalSessions\":1,\"passed\":0,\"failed\":0,\"started\":1"));
        assertFalse(json.contains("\"endedAt\""));
        int sessionStart = json.indexOf("\"sessions\":[");
        int sessionSummary = json.indexOf("\"summary\":", sessionStart);
        assertFalse(json.substring(sessionStart, sessionSummary).contains("\"durationMs\""));
        assertEquals(TraceStatus.STARTED, unfinished.metadata().status());
        assertNull(unfinished.metadata().finishedAt());
        assertEquals(eventsBefore, unfinished.events());
        assertEquals(artifactsBefore, unfinished.artifacts());

        unfinished.finishPassed();
        String finishedJson = new TraceJsonExporter().exportSuite(List.of(unfinished));
        assertTrue(finishedJson.contains("\"status\":\"PASSED\""));
        assertTrue(finishedJson.contains("\"started\":0"));
        assertTrue(finishedJson.contains("\"endedAt\""));
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

