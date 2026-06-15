package io.github.testlens.core.logging.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlLogExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsFullHtmlDocumentAndTimelineTable() {
        String html = new HtmlLogExporter().export(java.util.List.of());

        assertTrue(html.contains("<html"));
        assertTrue(html.contains("Selenium Test Lens Log Report"));
        assertTrue(html.contains("<table"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    void escapesHtmlInMessageAndMetadata() {
        UiTestLensLogEntry entry = UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.ERROR)
                .eventType(UiTestLensEventType.ERROR)
                .status(UiTestLensStatus.FAILED)
                .message("<script>alert(\"x\")</script>")
                .target(TargetDescriptor.label("Save & continue"))
                .metadata("unsafe", "<b>'quoted'</b>")
                .build();

        String html = new HtmlLogExporter().export(java.util.List.of(entry));

        assertTrue(html.contains("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;"));
        assertTrue(html.contains("Save &amp; continue"));
        assertTrue(html.contains("metadata.unsafe"));
        assertTrue(html.contains("&lt;b&gt;&#39;quoted&#39;&lt;/b&gt;"));
        assertFalse(html.contains("<script>alert"));
    }

    @Test
    void includesEntryFields() {
        UiTestLensLogEntry entry = UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.ACTION)
                .status(UiTestLensStatus.PASSED)
                .message("Done")
                .step("Checkout")
                .action("click")
                .target(TargetDescriptor.selector("#save"))
                .build();

        String html = new HtmlLogExporter().export(java.util.List.of(entry));

        assertTrue(html.contains("INFO"));
        assertTrue(html.contains("ACTION"));
        assertTrue(html.contains("PASS"));
        assertTrue(html.contains("Done"));
        assertTrue(html.contains("Checkout"));
        assertTrue(html.contains("click"));
        assertTrue(html.contains("#save"));
    }

    @Test
    void includesSummaryCountsAndFailureDiagnostics() {
        UiTestLensLogEntry passed = UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.LOCATOR_ACTION_PASSED)
                .status(UiTestLensStatus.PASSED)
                .message("Clicked save")
                .build();
        UiTestLensLogEntry failed = UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.ERROR)
                .eventType(UiTestLensEventType.ASSERTION_FAILED)
                .status(UiTestLensStatus.FAILED)
                .message("Toast missing")
                .throwable(new AssertionError("Expected toast"))
                .build();

        String html = new HtmlLogExporter().export(List.of(passed, failed));

        assertTrue(html.contains("Summary"));
        assertTrue(html.contains("Failed/Error events"));
        assertTrue(html.contains("Failure summary"));
        assertTrue(html.contains("Toast missing"));
        assertTrue(html.contains("Expected toast"));
    }

    @Test
    void exportToCreatesAndOverwritesHtmlFile() throws Exception {
        HtmlLogExporter exporter = new HtmlLogExporter();
        Path output = tempDir.resolve("report.html");
        Files.writeString(output, "old");

        Path written = exporter.exportTo(List.of(UiTestLensLogEntry.info("Saved")), output);

        assertEquals(output, written);
        String html = Files.readString(output);
        assertTrue(html.contains("Saved"));
        assertFalse(html.contains("old"));
    }
}

