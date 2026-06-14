package io.github.mmaciekk111.uitestlens.core.logging.export;

import org.junit.jupiter.api.Test;
import io.github.mmaciekk111.uitestlens.core.logging.TargetDescriptor;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;

import static org.junit.jupiter.api.Assertions.*;

class HtmlLogExporterTest {

    @Test
    void exportsFullHtmlDocumentAndTable() {
        String html = new HtmlLogExporter().export(java.util.List.of());

        assertTrue(html.contains("<html"));
        assertTrue(html.contains("<table>"));
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
        assertTrue(html.contains("unsafe=&lt;b&gt;&#39;quoted&#39;&lt;/b&gt;"));
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
        assertTrue(html.contains("PASSED"));
        assertTrue(html.contains("Done"));
        assertTrue(html.contains("Checkout"));
        assertTrue(html.contains("click"));
        assertTrue(html.contains("#save"));
    }
}
