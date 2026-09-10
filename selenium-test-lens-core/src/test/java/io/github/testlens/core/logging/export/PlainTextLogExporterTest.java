package io.github.testlens.core.logging.export;

import org.junit.jupiter.api.Test;
import io.github.testlens.core.logging.TargetDescriptor;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensLogger;
import io.github.testlens.core.redaction.RedactionPolicy;

import static org.junit.jupiter.api.Assertions.*;

class PlainTextLogExporterTest {

    @Test
    void exportsEmptyListAsEmptyString() {
        assertEquals("", new PlainTextLogExporter().export(null));
        assertEquals("", new PlainTextLogExporter().export(java.util.List.of()));
    }

    @Test
    void exportsSingleEntryWithCoreFieldsAndMetadata() {
        UiTestLensLogEntry entry = sampleEntry("Saved");

        String text = new PlainTextLogExporter().export(java.util.List.of(entry));

        assertTrue(text.contains("INFO"));
        assertTrue(text.contains("ACTION"));
        assertTrue(text.contains("PASSED"));
        assertTrue(text.contains("Saved"));
        assertTrue(text.contains("step=Checkout"));
        assertTrue(text.contains("action=click"));
        assertTrue(text.contains("target=#save"));
        assertTrue(text.contains("metadata={a=1, b=2}"));
    }

    @Test
    void truncatesLongValues() {
        UiTestLensLogEntry entry = UiTestLensLogEntry.builder()
                .message("abcdef")
                .build();

        String text = new PlainTextLogExporter(new LogExportOptions(true, true, false, 3))
                .export(java.util.List.of(entry));

        assertTrue(text.contains("abc..."));
        assertFalse(text.contains("abcdef"));
    }

    @Test
    void exportsOriginalTypeFromRedactedLoggerEntry() {
        String secret = "plain-type-canary";
        InMemoryLogSink sink = new InMemoryLogSink();
        UiTestLensLogger.builder().redactionPolicy(RedactionPolicy.builder().secret(secret).build())
                .sink(sink).build().error("failed", new IllegalStateException(secret));

        String text = new PlainTextLogExporter(new LogExportOptions(false, true, false, 500))
                .export(sink.entries());

        assertTrue(text.contains("throwable=java.lang.IllegalStateException:[REDACTED]"));
        assertFalse(text.contains("DiagnosticThrowable"));
        assertFalse(text.contains(secret));
    }

    private static UiTestLensLogEntry sampleEntry(String message) {
        return UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.ACTION)
                .status(UiTestLensStatus.PASSED)
                .message(message)
                .step("Checkout")
                .action("click")
                .target(TargetDescriptor.selector("#save"))
                .metadata("b", "2")
                .metadata("a", "1")
                .build();
    }
}

