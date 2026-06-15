package io.github.testlens.examples;

import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensLogger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingExportExampleTest {

    @Test
    void exportLogsWithoutBrowser() {
        InMemoryLogSink sink = new InMemoryLogSink();

        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(sink)
                .build();

        logger.info("Example started");
        logger.warn("Example warning");

        String text = sink.exportAsText();
        String json = sink.exportAsJson();
        String html = sink.exportAsHtml();

        assertTrue(text.contains("Example started"));
        assertTrue(json.contains("Example warning"));
        assertTrue(html.contains("<html"));
    }
}
