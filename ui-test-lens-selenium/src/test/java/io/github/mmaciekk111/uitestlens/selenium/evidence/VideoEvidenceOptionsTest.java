package io.github.mmaciekk111.uitestlens.selenium.evidence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoEvidenceOptionsTest {

    @Test
    void defaultsDescribeCustomMp4Attachment() {
        VideoEvidenceOptions options = VideoEvidenceOptions.defaults();

        assertEquals(VideoEvidenceSource.CUSTOM, options.source());
        assertEquals("video/mp4", options.mediaType());
        assertFalse(options.validateLocalFileExists());
        assertTrue(options.attachToSession());
        assertTrue(options.metadata().isEmpty());
    }

    @Test
    void builderOverridesValuesAndMetadata() {
        VideoEvidenceOptions options = VideoEvidenceOptions.builder()
                .source(VideoEvidenceSource.CI_ARTIFACT)
                .mediaType("video/webm")
                .validateLocalFileExists(true)
                .attachToSession(false)
                .metadata("buildId", "123")
                .metadata("provider", "Selenium Grid")
                .build();

        assertEquals(VideoEvidenceSource.CI_ARTIFACT, options.source());
        assertEquals("video/webm", options.mediaType());
        assertTrue(options.validateLocalFileExists());
        assertFalse(options.attachToSession());
        assertEquals("123", options.metadata().get("buildId"));
        assertEquals("Selenium Grid", options.metadata().get("provider"));
    }
}
