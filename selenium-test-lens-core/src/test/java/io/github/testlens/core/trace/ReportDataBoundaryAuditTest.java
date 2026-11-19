package io.github.testlens.core.trace;

import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.core.trace.export.TraceReportBundleExporter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ReportDataBoundaryAuditTest {
    @TempDir Path tempDir;

    @Test void customHeaderPolicyProtectsTraceAndPortableReports() throws Exception {
        String canary = "audit-custom-header-value";
        var policy = RedactionPolicy.builder().sensitiveKey("X-Company-Secret").build();
        var session = UiTestLensSession.start("custom headers", RetryOutcomePolicy.REPORT_ONLY, 0, policy);
        new TraceLogSink(session).accept(UiTestLensLogEntry.builder()
                .message("X-Company-Secret: " + canary)
                .metadata("X-Company-Secret", canary).metadata("Content-Type", "application/json").build());
        assertFalse(session.exportJson().contains(canary));
        assertFalse(session.exportHtml().contains(canary));
        assertTrue(session.exportJson().contains("application/json"));
        Path bundle = new TraceReportBundleExporter().exportTo(session, tempDir.resolve("headers.zip"));
        try (var zip = new ZipFile(bundle.toFile())) {
            for (String name : new String[] {"index.html", "report.json", "manifest.json"}) {
                assertFalse(new String(zip.getInputStream(zip.getEntry(name)).readAllBytes(), StandardCharsets.UTF_8)
                        .contains(canary), name);
            }
        }
    }

    @Test void unicodeAndMissingAttachmentsRemainPortableWithoutMarkupInjection() throws Exception {
        String unicode = "Za\u017c\u00f3\u0142\u0107 \uD83D\uDC69\u200D\uD83D\uDCBB";
        Path screenshot = tempDir.resolve(unicode + ".png");
        Files.write(screenshot, new byte[] {1, 2, 3});
        var session = UiTestLensSession.start(unicode + " <script>alert(1)</script>");
        session.attachScreenshot(unicode, screenshot);
        session.attachScreenshot("missing", tempDir.resolve("missing.png"));
        session.addEvent(TraceEvent.builder(TraceEventType.CUSTOM, TraceStatus.INFO, unicode)
                .message("<img src=x onerror=alert(1)>\n\t\"quoted\"\\path").build());
        assertTrue(session.exportJson().contains(unicode));
        assertFalse(session.exportHtml().contains("<img src=x onerror=alert(1)>"));
        Path bundle = new TraceReportBundleExporter().exportTo(session, tempDir.resolve("unicode.zip"));
        try (var zip = new ZipFile(bundle.toFile())) {
            assertTrue(zip.stream().noneMatch(entry -> entry.getName().startsWith("/") || entry.getName().contains("..")));
            assertEquals(1, zip.stream().filter(entry -> entry.getName().startsWith("artifacts/")).count());
            String manifest = new String(zip.getInputStream(zip.getEntry("manifest.json")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(manifest.contains("Missing artifact: missing"));
            String html = new String(zip.getInputStream(zip.getEntry("index.html")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(html.contains(unicode));
            assertFalse(html.contains("<script>alert(1)</script>"));
        }
    }

    @Test void oversizedUnicodeEventRemainsBounded() {
        var bounds = TraceRetentionOptions.builder().maxEventBytes(4096).build();
        var session = UiTestLensSession.start("large event", RetryOutcomePolicy.REPORT_ONLY, 0,
                RedactionPolicy.defaults(), bounds);
        session.addEvent(TraceEvent.builder(TraceEventType.CUSTOM, TraceStatus.INFO, "large")
                .message("\uD83D\uDC69\u200D\uD83D\uDCBB".repeat(100_000)).build());
        assertTrue(session.events().stream().allMatch(event ->
                event.message().getBytes(StandardCharsets.UTF_8).length <= bounds.maxEventBytes()));
        assertTrue(session.exportJson().length() < 100_000);
        assertTrue(session.exportHtml().length() < 200_000);
    }
}
