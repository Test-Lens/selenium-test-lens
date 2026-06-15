package io.github.testlens.core.trace.export;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceReportBundleExporterTest {
    @TempDir
    Path tempDir;

    @Test
    void zipBundleContainsHtmlJsonManifestAndArtifactFiles() throws Exception {
        Path screenshot = tempDir.resolve("screens").resolve("save.png");
        Files.createDirectories(screenshot.getParent());
        Files.write(screenshot, new byte[] {1, 2, 3});
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        session.attachScreenshot("Save", screenshot);
        session.finishPassed();
        Path output = tempDir.resolve("report.zip");

        new TraceReportBundleExporter().exportSuiteTo(List.of(session), output);

        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertNotNull(zip.getEntry("index.html"));
            assertNotNull(zip.getEntry("report.json"));
            assertNotNull(zip.getEntry("manifest.json"));
            assertNotNull(zip.getEntry("artifacts/Checkout-flow/save.png"));
        }
    }

    @Test
    void bundledHtmlLinksToCopiedArtifactEntries() throws Exception {
        Path screenshot = tempDir.resolve("screens").resolve("checkout").resolve("save.png");
        Files.createDirectories(screenshot.getParent());
        Files.write(screenshot, new byte[] {1, 2, 3});
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        session.attachScreenshot("Save", screenshot);
        session.finishPassed();
        Path output = tempDir.resolve("report.zip");

        new TraceReportBundleExporter().exportSuiteTo(List.of(session), output);

        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertNotNull(zip.getEntry("artifacts/Checkout-flow/save.png"));
            String html = new String(zip.getInputStream(zip.getEntry("index.html")).readAllBytes());
            assertTrue(html.contains("href=\"artifacts/Checkout-flow/save.png\""));
            assertTrue(html.contains("src=\"artifacts/Checkout-flow/save.png\""));
            assertFalse(html.contains(screenshot.toString()));
            assertFalse(html.contains(screenshot.toAbsolutePath().toString()));
        }
    }

    @Test
    void localHtmlReportStillUsesReportRelativeArtifactLinks() throws Exception {
        Path screenshot = tempDir.resolve("screens").resolve("local.png");
        Files.createDirectories(screenshot.getParent());
        Files.write(screenshot, new byte[] {1, 2, 3});
        UiTestLensSession session = UiTestLensSession.start("Local report");
        session.attachScreenshot("Local", screenshot);
        Path output = tempDir.resolve("reports").resolve("index.html");

        new TraceHtmlExporter().exportSuiteTo(List.of(session), output);

        String html = Files.readString(output);
        assertTrue(html.contains("href=\"../screens/local.png\""));
        assertFalse(html.contains("href=\"artifacts/Local-report/local.png\""));
    }

    @Test
    void manifestListsMissingArtifactsWithoutFailingBundle() throws Exception {
        UiTestLensSession session = UiTestLensSession.start("Missing artifacts");
        session.attachScreenshot("Missing", tempDir.resolve("missing.png"));
        Path output = tempDir.resolve("missing.zip");

        new TraceReportBundleExporter().exportSuiteTo(List.of(session), output);

        try (ZipFile zip = new ZipFile(output.toFile())) {
            String manifest = new String(zip.getInputStream(zip.getEntry("manifest.json")).readAllBytes());
            assertTrue(manifest.contains("\"missingArtifacts\""));
            assertTrue(manifest.contains("Missing"));
            assertTrue(manifest.contains("missing.png"));
        }
    }

    @Test
    void duplicateArtifactNamesAreDeduplicatedInZip() throws Exception {
        Path first = tempDir.resolve("one").resolve("save.png");
        Path second = tempDir.resolve("two").resolve("save.png");
        Files.createDirectories(first.getParent());
        Files.createDirectories(second.getParent());
        Files.write(first, new byte[] {1});
        Files.write(second, new byte[] {2});
        UiTestLensSession session = UiTestLensSession.start("Duplicate");
        session.attachScreenshot("First", first);
        session.attachScreenshot("Second", second);
        Path output = tempDir.resolve("duplicates.zip");

        new TraceReportBundleExporter().exportSuiteTo(List.of(session), output);

        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertNotNull(zip.getEntry("artifacts/Duplicate/save.png"));
            assertNotNull(zip.getEntry("artifacts/Duplicate/save-2.png"));
        }
    }

    @Test
    void unsafeZipEntryNamesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> TraceReportBundleExporter.safeZipEntryName("../secret.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> TraceReportBundleExporter.safeZipEntryName("C:/secret.txt"));
    }

    @Test
    void unsafeArtifactPathsDoNotCreateUnsafeZipEntries() throws Exception {
        UiTestLensSession session = UiTestLensSession.start("Unsafe");
        session.attachArtifact(TraceArtifact.customFile("Unsafe", Path.of("..", "secret.txt"), "text/plain"));
        Path output = tempDir.resolve("unsafe.zip");

        new TraceReportBundleExporter().exportSuiteTo(List.of(session), output);

        try (ZipFile zip = new ZipFile(output.toFile())) {
            assertTrue(zip.stream().noneMatch(entry -> entry.getName().contains("..")));
            assertNotNull(zip.getEntry("manifest.json"));
        }
    }
}
