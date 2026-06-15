package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifactType;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoEvidenceTest {
    @TempDir
    Path tempDir;

    @Test
    void attachLocalFileWithoutValidationCreatesVideoArtifact() {
        UiTestLensSession session = UiTestLensSession.start("Checkout");
        Path video = tempDir.resolve("missing-but-referenced.mp4");

        VideoEvidenceResult result = new VideoEvidence().attachFile(
                "Grid recording",
                video,
                VideoEvidenceOptions.builder()
                        .source(VideoEvidenceSource.SELENIUM_GRID)
                        .metadata("provider", "Docker Selenium")
                        .build(),
                session
        );

        assertEquals(VideoEvidenceStatus.ATTACHED, result.status());
        assertEquals(TraceArtifactType.VIDEO, result.artifact().type());
        assertEquals(video.toString(), result.artifact().path());
        assertEquals("SELENIUM_GRID", result.artifact().metadata().get("video.source"));
        assertEquals("Docker Selenium", result.artifact().metadata().get("provider"));
        assertEquals(1, session.artifacts().size());
    }

    @Test
    void attachLocalFileWithValidationFailsWhenFileIsMissing() {
        Path missing = tempDir.resolve("missing.mp4");

        VideoEvidenceResult result = new VideoEvidence().attachFile(
                "Missing video",
                missing,
                VideoEvidenceOptions.builder().validateLocalFileExists(true).build(),
                UiTestLensSession.start("Checkout")
        );

        assertEquals(VideoEvidenceStatus.FAILED, result.status());
        assertTrue(result.message().contains("does not exist"));
    }

    @Test
    void attachLocalFileWithValidationPassesWhenFileExists() throws Exception {
        UiTestLensSession session = UiTestLensSession.start("Checkout");
        Path video = tempDir.resolve("checkout.mp4");
        Files.writeString(video, "video");

        VideoEvidenceResult result = new VideoEvidence().attachFile(
                "Checkout video",
                video,
                VideoEvidenceOptions.builder().validateLocalFileExists(true).build(),
                session
        );

        assertEquals(VideoEvidenceStatus.ATTACHED, result.status());
        assertEquals(video.toString(), session.artifacts().get(0).path());
    }

    @Test
    void attachUrlCreatesVideoArtifactWithoutCallingRemoteUrl() {
        UiTestLensSession session = UiTestLensSession.start("Checkout");

        VideoEvidenceResult result = new VideoEvidence().attachUrl(
                "CI video",
                "https://ci.example.com/artifacts/checkout.mp4?signed=sample",
                VideoEvidenceOptions.builder()
                        .source(VideoEvidenceSource.CI_ARTIFACT)
                        .metadata("job", "checkout-ui-tests")
                        .build(),
                session
        );

        assertEquals(VideoEvidenceStatus.ATTACHED, result.status());
        assertEquals(TraceArtifactType.VIDEO, result.artifact().type());
        assertEquals("https://ci.example.com/artifacts/checkout.mp4?signed=sample", result.artifact().url());
        assertEquals("checkout-ui-tests", result.artifact().metadata().get("job"));
    }

    @Test
    void blankNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new VideoEvidence().attachUrl(" ", "https://ci.example.com/video.mp4", VideoEvidenceOptions.defaults(), null));
    }

    @Test
    void blankUrlFailsWithReadableResult() {
        VideoEvidenceResult result = new VideoEvidence().attachUrl("CI video", " ", VideoEvidenceOptions.defaults(), null);

        assertEquals(VideoEvidenceStatus.FAILED, result.status());
        assertTrue(result.message().contains("must not be blank"));
    }

    @Test
    void nullSessionWithAttachToSessionSkipsAttach() {
        VideoEvidenceResult result = new VideoEvidence().attachUrl(
                "CI video",
                "https://ci.example.com/video.mp4",
                VideoEvidenceOptions.defaults(),
                null
        );

        assertEquals(VideoEvidenceStatus.SKIPPED, result.status());
        assertTrue(result.message().contains("No UiTestLensSession"));
    }

    @Test
    void attachToSessionFalseReturnsReferenceWithoutSession() {
        VideoEvidenceResult result = new VideoEvidence().attachUrl(
                "CI video",
                "https://ci.example.com/video.mp4",
                VideoEvidenceOptions.builder().attachToSession(false).build(),
                null
        );

        assertEquals(VideoEvidenceStatus.ATTACHED, result.status());
        assertEquals("https://ci.example.com/video.mp4", result.url());
    }
}

