package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.selenium.evidence.VideoEvidenceOptions;
import io.github.testlens.selenium.evidence.VideoEvidenceSource;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;

class VideoEvidenceExampleTest {

    @Disabled("Documentation-only example; requires external video artifact.")
    @Test
    void videoEvidenceUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.startSession("Checkout flow");

        overlay.attachVideoFile(
                "Selenium Grid recording",
                Path.of("target/videos/checkout-flow.mp4"),
                VideoEvidenceOptions.builder()
                        .source(VideoEvidenceSource.SELENIUM_GRID)
                        .metadata("provider", "Docker Selenium")
                        .build()
        );

        overlay.attachVideoUrl(
                "CI video artifact",
                "https://ci.example.com/artifacts/checkout-flow.mp4",
                VideoEvidenceOptions.builder()
                        .source(VideoEvidenceSource.CI_ARTIFACT)
                        .metadata("job", "checkout-ui-tests")
                        .build()
        );

        overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
    }
}

