package io.github.mmaciekk111.uitestlens.core.logging.export;

import org.junit.jupiter.api.Test;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;

import static org.junit.jupiter.api.Assertions.*;

class JsonLogExporterTest {

    @Test
    void exportsEmptyListAsJsonArray() {
        assertEquals("[]", new JsonLogExporter().export(null));
        assertEquals("[]", new JsonLogExporter().export(java.util.List.of()));
    }

    @Test
    void exportsRequiredFieldsMetadataAndTarget() {
        UiTestLensLogEntry entry = sampleEntry("Saved");

        String json = new JsonLogExporter(new LogExportOptions(true, true, false, 500))
                .export(java.util.List.of(entry));

        assertTrue(json.startsWith("[{"));
        assertTrue(json.endsWith("}]"));
        assertTrue(json.contains("\"timestamp\":"));
        assertTrue(json.contains("\"level\":\"INFO\""));
        assertTrue(json.contains("\"eventType\":\"ACTION\""));
        assertTrue(json.contains("\"status\":\"PASSED\""));
        assertTrue(json.contains("\"message\":\"Saved\""));
        assertTrue(json.contains("\"target\":{\"selector\":\"#save\""));
        assertTrue(json.contains("\"metadata\":{\"a\":\"1\",\"b\":\"2\"}"));
        assertTrue(json.contains("\"throwable\":null"));
    }

    @Test
    void escapesJsonStrings() {
        UiTestLensLogEntry entry = UiTestLensLogEntry.builder()
                .message("quote \" slash \\ newline\n tab\t carriage\r control " + (char) 1)
                .build();

        String json = new JsonLogExporter(new LogExportOptions(true, true, false, 500))
                .export(java.util.List.of(entry));

        assertTrue(json.contains("\\\""));
        assertTrue(json.contains("\\\\"));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\r"));
        assertTrue(json.contains("\\u0001"));
        assertFalse(json.contains("newline\n tab"));
    }

    @Test
    void prettyPrintAddsNewlinesAndIndentation() {
        UiTestLensLogEntry entry = sampleEntry("Saved");

        String json = new JsonLogExporter(LogExportOptions.defaults()).export(java.util.List.of(entry));

        assertTrue(json.contains("\n  {"));
        assertTrue(json.contains("\n    \"level\""));
    }

    private static UiTestLensLogEntry sampleEntry(String message) {
        return UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.ACTION)
                .status(UiTestLensStatus.PASSED)
                .message(message)
                .step("Checkout")
                .action("click")
                .target(TargetDescriptor.selector("#save").withMetadata("role", "button"))
                .metadata("b", "2")
                .metadata("a", "1")
                .build();
    }
}
