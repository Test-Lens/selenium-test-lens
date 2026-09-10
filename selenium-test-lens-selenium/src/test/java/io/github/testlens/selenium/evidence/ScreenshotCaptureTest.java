package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifactType;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotCaptureTest {
    @TempDir
    Path tempDir;

    @Test
    void captureCreatesDestinationFileAndAttachesArtifact() throws Exception {
        Path source = tempDir.resolve("source.png");
        writePng(source, 3, 2);
        UiTestLensSession session = UiTestLensSession.start("Checkout");
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir.resolve("screens"))
                .fileNamePrefix("shot")
                .includeTimestamp(false)
                .build();

        FakeScreenshotDriver driver = new FakeScreenshotDriver(source);
        ScreenshotCaptureResult result = new ScreenshotCapture(driver).capture("After save", options, session);

        assertEquals(ScreenshotCaptureStatus.CAPTURED, result.status());
        assertTrue(Files.exists(result.path()));
        assertEquals(3, ImageIO.read(result.path().toFile()).getWidth());
        assertEquals(2, ImageIO.read(result.path().toFile()).getHeight());
        assertEquals(ScreenshotCaptureMode.VIEWPORT, result.capturedMode());
        assertEquals(1, result.tileCount());
        assertEquals(1, driver.screenshotCalls);
        assertNotNull(result.artifact());
        assertEquals(TraceArtifactType.SCREENSHOT, session.artifacts().get(0).type());
    }

    @Test
    void unsupportedDriverReturnsFailedResult() {
        ScreenshotCaptureResult result = new ScreenshotCapture(new UnsupportedDriver())
                .capture("After save", ScreenshotCaptureOptions.builder().outputDirectory(tempDir).build());

        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertFalse(result.isCaptured());
        assertTrue(result.message().contains("TakesScreenshot"));
    }

    @Test
    void captureWithoutSessionStillWritesFile() throws Exception {
        Path source = tempDir.resolve("source.png");
        writePng(source, 2, 2);
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir.resolve("screens"))
                .includeTimestamp(false)
                .build();

        ScreenshotCaptureResult result = new ScreenshotCapture(new FakeScreenshotDriver(source)).capture("After save", options, null);

        assertEquals(ScreenshotCaptureStatus.CAPTURED, result.status());
        assertTrue(Files.exists(result.path()));
        assertEquals(null, result.artifact());
        assertTrue(result.message().contains("no UiTestLensSession"));
    }

    private static void writePng(Path path, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ImageIO.write(image, "png", path.toFile());
    }

    private static final class FakeScreenshotDriver extends UnsupportedDriver implements TakesScreenshot {
        private final Path source;
        private int screenshotCalls;

        private FakeScreenshotDriver(Path source) {
            this.source = source;
        }

        @Override
        public <X> X getScreenshotAs(OutputType<X> target) {
            screenshotCalls++;
            return target.convertFromPngBytes(readBytes(source));
        }

        private static byte[] readBytes(Path path) {
            try {
                return Files.readAllBytes(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class UnsupportedDriver implements WebDriver {
        @Override
        public void get(String url) {}

        @Override
        public String getCurrentUrl() {
            return "";
        }

        @Override
        public String getTitle() {
            return "";
        }

        @Override
        public List<org.openqa.selenium.WebElement> findElements(org.openqa.selenium.By by) {
            return List.of();
        }

        @Override
        public org.openqa.selenium.WebElement findElement(org.openqa.selenium.By by) {
            throw new org.openqa.selenium.NoSuchElementException("not implemented");
        }

        @Override
        public String getPageSource() {
            return "";
        }

        @Override
        public void close() {}

        @Override
        public void quit() {}

        @Override
        public Set<String> getWindowHandles() {
            return Set.of();
        }

        @Override
        public String getWindowHandle() {
            return "";
        }

        @Override
        public TargetLocator switchTo() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Navigation navigate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Options manage() {
            throw new UnsupportedOperationException();
        }
    }
}

