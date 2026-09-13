package io.github.testlens.allure;

import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.core.trace.UiTestLensSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class AllureTestArtifacts {
    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    private AllureTestArtifacts() { }

    static TestLensFinalizationResult failed(Path root, String marker) throws IOException {
        Path bundle = Files.createDirectories(root.resolve("failure-bundle"));
        Path diagnostic = Files.write(root.resolve("failure-diagnostic.png"), PNG);
        Files.write(bundle.resolve("failure-clean.png"), PNG);
        Path html = Files.writeString(root.resolve("report.html"), "<html>" + marker + "</html>");
        Path json = Files.writeString(root.resolve("trace.json"), "{\"marker\":\"" + marker + "\"}");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(root.resolve("failure-bundle.zip")))) {
            zip.putNextEntry(new ZipEntry("marker.txt"));
            zip.write(marker.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        UiTestLensSession session = UiTestLensSession.start(marker);
        session.finishFailed(new AssertionError("fixture failure"));
        return new TestLensFinalizationResult(session, root, json, html, diagnostic, List.of());
    }

    static TestLensFinalizationResult passed(Path root, String marker) throws IOException {
        Files.createDirectories(root);
        Path html = Files.writeString(root.resolve("report.html"), "<html>" + marker + "</html>");
        Path json = Files.writeString(root.resolve("trace.json"), "{\"marker\":\"" + marker + "\"}");
        UiTestLensSession session = UiTestLensSession.start(marker);
        session.finishPassed();
        return new TestLensFinalizationResult(session, root, json, html, null, List.of());
    }

    static TestLensFinalizationResult skipped(Path root, String marker) throws IOException {
        TestLensFinalizationResult passed = passed(root, marker);
        UiTestLensSession session = UiTestLensSession.start(marker);
        session.finishSkipped("fixture skip");
        return new TestLensFinalizationResult(session, root, passed.jsonReport(), passed.htmlReport(), null, List.of());
    }
}
