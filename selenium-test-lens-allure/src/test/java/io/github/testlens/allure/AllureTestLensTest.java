package io.github.testlens.allure;

import io.github.testlens.TestLensFinalizationResult;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.FileSystemResultsWriter;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.TestResult;
import io.qameta.allure.model.TestResultContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class AllureTestLensTest {
    @TempDir Path temp;

    @Test
    void returnsSafeSkipWithoutActiveAllureContext() throws Exception {
        Allure.setLifecycle(new AllureLifecycle(new FileSystemResultsWriter(temp.resolve("results"))));
        var result = AllureTestLens.attach(AllureTestArtifacts.failed(temp.resolve("evidence"), "secret-free"));
        assertEquals(AllureAttachStatus.SKIPPED_NO_ACTIVE_CONTEXT, result.status());
        assertEquals(0, result.attachedCount());
    }

    @Test
    void streamsExpectedFailedArtifactsWithExactMetadataAndIsIdempotent() throws Exception {
        Path results = temp.resolve("results");
        AllureLifecycle lifecycle = activeLifecycle(results, "failed-case");
        TestLensFinalizationResult finalized = AllureTestArtifacts.failed(temp.resolve("evidence"), "FAILED-A");

        AllureAttachResult first = AllureTestLens.attach(finalized);
        AllureAttachResult duplicate = AllureTestLens.attach(finalized);
        finish(lifecycle, "failed-case");

        assertEquals(AllureAttachStatus.ATTACHED, first.status());
        assertEquals(4, first.attachedCount());
        assertEquals(AllureAttachStatus.SKIPPED_ALREADY_ATTACHED, duplicate.status());
        String json = resultJson(results);
        assertAll(
                () -> assertTrue(json.contains("Test Lens — Diagnostic screenshot")),
                () -> assertTrue(json.contains("Test Lens — Clean screenshot")),
                () -> assertTrue(json.contains("Test Lens — Report")),
                () -> assertTrue(json.contains("Test Lens — Failure bundle")),
                () -> assertTrue(json.contains("image/png")),
                () -> assertTrue(json.contains("text/html")),
                () -> assertTrue(json.contains("application/zip")),
                () -> assertFalse(json.contains("Test Lens — Trace")));
        assertAttachmentSignatures(results);
    }

    @Test
    void passedAndSkippedStyleSessionsAreOptInAndFilteringWorks() throws Exception {
        AllureLifecycle lifecycle = activeLifecycle(temp.resolve("results"), "passed-case");
        TestLensFinalizationResult finalized = AllureTestArtifacts.passed(temp.resolve("evidence"), "PASSED-A");
        assertEquals(AllureAttachStatus.SKIPPED_BY_POLICY, AllureTestLens.attach(finalized).status());

        AllureTestLensOptions options = AllureTestLensOptions.builder()
                .attachNonFailedSessions(true)
                .attachDiagnosticScreenshot(false)
                .attachCleanScreenshot(false)
                .attachFailureBundle(false)
                .attachTrace(true)
                .build();
        AllureAttachResult attached = AllureTestLens.attach(finalized, options);
        finish(lifecycle, "passed-case");
        assertEquals(AllureAttachStatus.ATTACHED, attached.status());
        assertEquals(2, attached.attachedCount());
        String json = resultJson(temp.resolve("results"));
        assertTrue(json.contains("application/json"));
        assertTrue(json.contains("text/html"));

        AllureLifecycle skippedLifecycle = activeLifecycle(temp.resolve("skipped-results"), "skipped-case");
        assertEquals(AllureAttachStatus.SKIPPED_BY_POLICY,
                AllureTestLens.attach(AllureTestArtifacts.skipped(temp.resolve("skipped-evidence"), "SKIPPED-A")).status());
        finish(skippedLifecycle, "skipped-case");
    }

    @Test
    void allureWriteFailureIsDiagnosticAndDoesNotReplaceOriginalTestFailure() throws Exception {
        AllureResultsWriter failingWriter = new AllureResultsWriter() {
            @Override public void write(TestResult result) { }
            @Override public void write(TestResultContainer container) { }
            @Override public void write(String source, java.io.InputStream attachment) {
                throw new IllegalStateException("simulated writer failure");
            }
        };
        AllureLifecycle lifecycle = new AllureLifecycle(failingWriter);
        Allure.setLifecycle(lifecycle);
        lifecycle.scheduleTestCase("writer-failure", new TestResult().setUuid("writer-failure").setName("writer-failure"));
        lifecycle.startTestCase("writer-failure");
        TestLensFinalizationResult finalized = AllureTestArtifacts.failed(temp.resolve("writer-evidence"), "WRITER-A");
        AllureAttachResult result = assertDoesNotThrow(() -> AllureTestLens.attach(finalized));
        assertEquals(AllureAttachStatus.FAILED, result.status());
        assertEquals(4, result.failures().size());
        assertEquals(io.github.testlens.core.trace.TraceStatus.FAILED, finalized.session().metadata().status());
    }

    @Test
    void reportsMissingArtifactsWithoutThrowingOrLeakingPaths() throws Exception {
        AllureLifecycle lifecycle = activeLifecycle(temp.resolve("results"), "missing-case");
        TestLensFinalizationResult finalized = AllureTestArtifacts.failed(temp.resolve("evidence"), "MISSING-A");
        Files.delete(finalized.failureScreenshot());
        AllureAttachResult attached = AllureTestLens.attach(finalized);
        finish(lifecycle, "missing-case");
        assertEquals(AllureAttachStatus.PARTIALLY_ATTACHED, attached.status());
        assertEquals(List.of("Test Lens — Diagnostic screenshot"), attached.missingArtifacts());
        assertTrue(attached.failures().isEmpty());
    }

    @Test
    void parallelAllureContextsDoNotMixTestLensResults() throws Exception {
        Path results = temp.resolve("parallel-results");
        AllureLifecycle lifecycle = new AllureLifecycle(new FileSystemResultsWriter(results));
        Allure.setLifecycle(lifecycle);
        var a = AllureTestArtifacts.failed(temp.resolve("evidence-a"), "CONTENT-A");
        var b = AllureTestArtifacts.failed(temp.resolve("evidence-b"), "CONTENT-B");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var fa = executor.submit(() -> attachInContext(lifecycle, "parallel-a", a, ready, go));
            var fb = executor.submit(() -> attachInContext(lifecycle, "parallel-b", b, ready, go));
            ready.await();
            go.countDown();
            assertEquals(AllureAttachStatus.ATTACHED, fa.get().status());
            assertEquals(AllureAttachStatus.ATTACHED, fb.get().status());
        } finally {
            executor.shutdownNow();
        }
        try (var stream = Files.list(results)) {
            List<String> json = stream.filter(path -> path.toString().endsWith("-result.json"))
                    .map(path -> { try { return Files.readString(path); } catch (Exception e) { throw new RuntimeException(e); } })
                    .toList();
            assertEquals(2, json.size());
            assertTrue(json.stream().anyMatch(value -> value.contains("parallel-a") && count(value, "\"source\"") == 4));
            assertTrue(json.stream().anyMatch(value -> value.contains("parallel-b") && count(value, "\"source\"") == 4));
        }
    }

    @Test
    void streamingPerformanceDiagnosticForTwoPngHtmlJsonAndFiveMegabyteZip() throws Exception {
        AllureLifecycle lifecycle = activeLifecycle(temp.resolve("performance-results"), "performance-case");
        TestLensFinalizationResult finalized = AllureTestArtifacts.failed(temp.resolve("performance-evidence"), "PERF");
        byte[] payload = new byte[5 * 1024 * 1024];
        new java.util.Random(7).nextBytes(payload);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(finalized.failureBundleArchive().orElseThrow()))) {
            zip.putNextEntry(new ZipEntry("payload.bin"));
            zip.write(payload);
            zip.closeEntry();
        }
        AllureTestLensOptions options = AllureTestLensOptions.builder().attachTrace(true).build();
        long started = System.nanoTime();
        AllureAttachResult attached = AllureTestLens.attach(finalized, options);
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        finish(lifecycle, "performance-case");
        System.out.println("ALLURE_ATTACH_DIAGNOSTIC_MS=" + elapsedMillis + " ZIP_BYTES="
                + Files.size(finalized.failureBundleArchive().orElseThrow()));
        assertEquals(5, attached.attachedCount());
        assertTrue(elapsedMillis < 5_000, "diagnostic guard against pathological attachment latency");
    }

    private static AllureAttachResult attachInContext(AllureLifecycle lifecycle, String uuid,
                                                       TestLensFinalizationResult result, CountDownLatch ready,
                                                       CountDownLatch go) throws Exception {
        lifecycle.scheduleTestCase(uuid, new TestResult().setUuid(uuid).setName(uuid));
        lifecycle.startTestCase(uuid);
        ready.countDown();
        go.await();
        AllureAttachResult attached = AllureTestLens.attach(result);
        finish(lifecycle, uuid);
        return attached;
    }

    private static int count(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }

    private static AllureLifecycle activeLifecycle(Path results, String uuid) {
        AllureLifecycle lifecycle = new AllureLifecycle(new FileSystemResultsWriter(results));
        Allure.setLifecycle(lifecycle);
        lifecycle.scheduleTestCase(uuid, new TestResult().setUuid(uuid).setName(uuid));
        lifecycle.startTestCase(uuid);
        return lifecycle;
    }

    private static void finish(AllureLifecycle lifecycle, String uuid) {
        lifecycle.stopTestCase(uuid);
        lifecycle.writeTestCase(uuid);
    }

    private static String resultJson(Path results) throws Exception {
        Path file;
        try (var stream = Files.list(results)) {
            file = stream.filter(path -> path.getFileName().toString().endsWith("-result.json")).findFirst().orElseThrow();
        }
        return Files.readString(file);
    }

    private static void assertAttachmentSignatures(Path results) throws Exception {
        try (var stream = Files.list(results)) {
            List<Path> attachments = stream.filter(path -> path.getFileName().toString().contains("-attachment.")).toList();
            assertEquals(4, attachments.size());
            assertTrue(attachments.stream().anyMatch(path -> path.toString().endsWith(".png")));
            assertTrue(attachments.stream().anyMatch(path -> path.toString().endsWith(".html")));
            Path zip = attachments.stream().filter(path -> path.toString().endsWith(".zip")).findFirst().orElseThrow();
            assertArrayEquals(new byte[]{'P', 'K'}, java.util.Arrays.copyOf(Files.readAllBytes(zip), 2));
            Path png = attachments.stream().filter(path -> path.toString().endsWith(".png")).findFirst().orElseThrow();
            assertArrayEquals(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, java.util.Arrays.copyOf(Files.readAllBytes(png), 4));
        }
    }
}
