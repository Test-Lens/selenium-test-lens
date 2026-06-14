package io.github.mmaciekk111.uitestlens.core.trace;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceJsonExporterTest {

    @Test
    void exportContainsMetadataEventsArtifactsAndEscapedText() {
        UiTestLensSession session = UiTestLensSession.start("Checkout \"flow\"");
        session.addEvent(TraceEvent.info("line\nbreak", "message with \"quotes\""));
        session.attachScreenshot("Save form", Path.of("target/screenshots/save.png"));
        session.finishFailed(new RuntimeException("failed \"reason\""));

        String json = session.exportJson();

        assertTrue(json.contains("\"metadata\""));
        assertTrue(json.contains("\"events\""));
        assertTrue(json.contains("\"artifacts\""));
        assertTrue(json.contains("Checkout \\\"flow\\\""));
        assertTrue(json.contains("line\\nbreak"));
        assertTrue(json.contains("failed \\\"reason\\\""));
    }
}
