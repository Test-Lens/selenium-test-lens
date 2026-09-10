package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class FullPageScreenshotCaptureTest {
    @TempDir Path tempDir;

    @Test
    void defaultCaptureRemainsAViewportImageRatherThanExpandingToDocumentHeight() throws Exception {
        StitchDriver driver = new StitchDriver(4, 9, 4, 3, 1.0);
        ScreenshotCaptureOptions options = ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir)
                .includeTimestamp(false)
                .build();

        ScreenshotCaptureResult result = new ScreenshotCapture(driver).capture("viewport", options, null);

        assertTrue(result.isCaptured());
        assertEquals(ScreenshotCaptureMode.VIEWPORT, result.capturedMode());
        assertEquals(3, result.height());
        assertEquals(1, result.tileCount());
        assertEquals(1, driver.screenshotCalls);
        assertEquals(0, driver.restoreCalls);
    }

    @Test
    void stitchesTwoDimensionalGridAtScaleOneWithoutBlankOrDuplicatedSeams() throws Exception {
        StitchDriver driver = new StitchDriver(7, 8, 4, 3, 1.0);
        UiTestLensSession session = UiTestLensSession.start("full-page");

        ScreenshotCaptureResult result = capture(driver, session, 100, 20);

        assertEquals(ScreenshotCaptureStatus.CAPTURED, result.status());
        assertEquals(ScreenshotCaptureMode.FULL_PAGE, result.requestedMode());
        assertEquals(ScreenshotCaptureMode.FULL_PAGE, result.capturedMode());
        assertEquals(7, result.width());
        assertEquals(8, result.height());
        assertEquals(6, result.tileCount());
        BufferedImage image = ImageIO.read(result.path().toFile());
        for (int y = 0; y < 8; y++) for (int x = 0; x < 7; x++) {
            assertEquals(colorAt(x, y), image.getRGB(x, y), "pixel " + x + "," + y);
        }
        assertEquals(0, driver.scrollX);
        assertEquals(0, driver.scrollY);
        assertEquals(1, driver.restoreCalls);
        assertEquals(6, driver.hideCalls);
        assertEquals(1, session.artifacts().size());
        assertEquals("FULL_PAGE", session.artifacts().get(0).metadata().get("captureMode"));
    }

    @Test
    void derivesIndependentImageScalesAndExactPartialTileDimensions() throws Exception {
        StitchDriver driver = new StitchDriver(5, 5, 4, 3, 2.0, 1.5);

        ScreenshotCaptureResult result = capture(driver, null, 200, 20);

        assertTrue(result.isCaptured());
        assertEquals(10, result.width());
        assertEquals(8, result.height());
        assertEquals(4, result.tileCount());
    }

    @Test
    void pageSmallerThanViewportUsesOneTileAndCropsToDocument() throws Exception {
        StitchDriver driver = new StitchDriver(2, 2, 4, 3, 1.0);
        ScreenshotCaptureResult result = capture(driver, null, 20, 2);
        assertTrue(result.isCaptured());
        assertEquals(2, result.width());
        assertEquals(2, result.height());
        assertEquals(1, result.tileCount());
        assertEquals(1, driver.screenshotCalls);
    }

    @Test
    void limitsFailBeforePublishingAndAlwaysRestorePageState() {
        StitchDriver pixels = new StitchDriver(10, 10, 5, 5, 1.0);
        ScreenshotCaptureResult pixelResult = capture(pixels, null, 50, 20);
        assertEquals(ScreenshotCaptureStatus.FAILED, pixelResult.status());
        assertTrue(pixelResult.message().contains("maxPixelCount"));
        assertEquals(0, pixels.screenshotCalls);
        assertEquals(1, pixels.restoreCalls);

        StitchDriver tiles = new StitchDriver(10, 10, 5, 5, 1.0);
        ScreenshotCaptureResult tileResult = capture(tiles, null, 100, 3);
        assertEquals(ScreenshotCaptureStatus.FAILED, tileResult.status());
        assertTrue(tileResult.message().contains("maxTileCount"));
        assertEquals(0, tiles.screenshotCalls);
        assertEquals(1, tiles.restoreCalls);
        assertFalse(Files.exists(tempDir.resolve("shot_page.png")));
    }

    @Test
    void middleTileFailureKeepsPrimaryAndSuppressesRestorationFailure() {
        StitchDriver driver = new StitchDriver(4, 7, 4, 3, 1.0);
        driver.failScreenshotAt = 2;
        driver.failRestore = true;

        ScreenshotCaptureResult result = capture(driver, null, 100, 10);

        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertEquals("tile failure", result.exception().getMessage());
        assertEquals(1, result.exception().getSuppressed().length);
        assertEquals("restore failure", result.exception().getSuppressed()[0].getMessage());
        assertFalse(Files.exists(tempDir.resolve("shot_page.png")));
    }

    @Test
    void changingGeometryOrTileScaleFailsWithoutPublishingPartialImage() {
        StitchDriver geometry = new StitchDriver(4, 7, 4, 3, 1.0);
        geometry.changeGeometry = true;
        ScreenshotCaptureResult geometryResult = capture(geometry, null, 100, 10);
        assertEquals(ScreenshotCaptureStatus.FAILED, geometryResult.status());
        assertTrue(geometryResult.message().contains("dimensions changed"));

        StitchDriver scale = new StitchDriver(4, 7, 4, 3, 1.0);
        scale.changeScale = true;
        ScreenshotCaptureResult scaleResult = capture(scale, null, 100, 10);
        assertEquals(ScreenshotCaptureStatus.FAILED, scaleResult.status());
        assertTrue(scaleResult.message().contains("scale changed"));
        assertFalse(Files.exists(tempDir.resolve("shot_page.png")));
    }

    @Test
    void restoresNonZeroInitialScrollAndRejectsDimensionOverflow() {
        StitchDriver driver = new StitchDriver(7, 8, 4, 3, 1.0);
        driver.scrollX = 2;
        driver.scrollY = 4;
        ScreenshotCaptureResult result = capture(driver, null, 100, 20);
        assertTrue(result.isCaptured());
        assertEquals(2, driver.scrollX);
        assertEquals(4, driver.scrollY);

        StitchDriver overflow = new StitchDriver(Long.MAX_VALUE, 2, 1, 1, 1.0);
        ScreenshotCaptureResult overflowResult = capture(overflow, null, Long.MAX_VALUE, 20);
        assertEquals(ScreenshotCaptureStatus.FAILED, overflowResult.status());
        assertTrue(overflowResult.message().toLowerCase().contains("overflow"));
        assertEquals(0, overflow.screenshotCalls);
    }

    @Test
    void iframeContextIsSkippedWithoutScreenshotAndContextIsNotChanged() {
        StitchDriver driver = new StitchDriver(4, 6, 4, 3, 1.0);
        driver.topLevel = false;
        ScreenshotCaptureResult result = capture(driver, null, 100, 10);
        assertEquals(ScreenshotCaptureStatus.SKIPPED, result.status());
        assertEquals(ScreenshotCaptureMode.FULL_PAGE, result.requestedMode());
        assertNull(result.capturedMode());
        assertEquals(0, driver.screenshotCalls);
        assertEquals(0, driver.switchCalls);
        assertEquals(1, driver.restoreCalls);
    }

    @Test
    void missingJavascriptExecutorDoesNotReturnViewportAsFullPage() throws Exception {
        Path png = tempDir.resolve("viewport.png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", png.toFile());
        ScreenshotCaptureResult result = new ScreenshotCapture(new ScreenshotOnlyDriver(png)).capture("page",
                ScreenshotCaptureOptions.builder().outputDirectory(tempDir)
                        .captureMode(ScreenshotCaptureMode.FULL_PAGE).build());
        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertNull(result.capturedMode());
        assertTrue(result.message().contains("JavascriptExecutor"));
    }

    private ScreenshotCaptureResult capture(StitchDriver driver, UiTestLensSession session,
                                            long maxPixels, int maxTiles) {
        return new ScreenshotCapture(driver).capture("page", ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir).fileNamePrefix("shot").includeTimestamp(false)
                .captureMode(ScreenshotCaptureMode.FULL_PAGE)
                .maxPixelCount(maxPixels).maxTileCount(maxTiles).build(), session);
    }

    private static int colorAt(int x, int y) {
        return new Color((x * 31 + 17) & 255, (y * 29 + 23) & 255, ((x + y) * 19 + 11) & 255, 255).getRGB();
    }

    private static class StitchDriver extends BaseDriver implements TakesScreenshot, JavascriptExecutor {
        final long documentWidth, documentHeight, viewportWidth, viewportHeight;
        final double scaleX, scaleY;
        long scrollX, scrollY;
        boolean topLevel = true;
        boolean failRestore;
        boolean changeGeometry;
        boolean changeScale;
        int failScreenshotAt;
        int screenshotCalls, restoreCalls, hideCalls, switchCalls;
        long savedScrollX, savedScrollY;

        StitchDriver(long documentWidth, long documentHeight, long viewportWidth, long viewportHeight, double scale) {
            this(documentWidth, documentHeight, viewportWidth, viewportHeight, scale, scale);
        }

        StitchDriver(long documentWidth, long documentHeight, long viewportWidth, long viewportHeight,
                     double scaleX, double scaleY) {
            this.documentWidth = documentWidth; this.documentHeight = documentHeight;
            this.viewportWidth = viewportWidth; this.viewportHeight = viewportHeight;
            this.scaleX = scaleX; this.scaleY = scaleY;
        }

        @Override public Object executeScript(String script, Object... args) {
            if (script.contains("window[key] =")) {
                savedScrollX = scrollX;
                savedScrollY = scrollY;
                return geometry();
            }
            if (script.contains("querySelectorAll")) { hideCalls++; return null; }
            if (script.contains("state.hidden.length")) {
                restoreCalls++;
                if (failRestore) throw new IllegalStateException("restore failure");
                scrollX = savedScrollX; scrollY = savedScrollY; return true;
            }
            throw new AssertionError("Unexpected script");
        }

        @Override public Object executeAsyncScript(String script, Object... args) {
            scrollX = Math.min(((Number) args[0]).longValue(), Math.max(0, documentWidth - viewportWidth));
            scrollY = Math.min(((Number) args[1]).longValue(), Math.max(0, documentHeight - viewportHeight));
            Map<String, Object> result = geometry();
            if (changeGeometry) result.put("documentHeight", documentHeight + 1);
            return result;
        }

        @Override public <X> X getScreenshotAs(OutputType<X> target) {
            screenshotCalls++;
            if (failScreenshotAt == screenshotCalls) throw new IllegalStateException("tile failure");
            int width = (int) Math.round(viewportWidth * scaleX);
            int height = (int) Math.round(viewportHeight * scaleY);
            if (changeScale && screenshotCalls > 1) width++;
            BufferedImage tile = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int py = 0; py < height; py++) for (int px = 0; px < width; px++) {
                int cssX = (int) Math.min(documentWidth - 1, scrollX + Math.floor(px / scaleX));
                int cssY = (int) Math.min(documentHeight - 1, scrollY + Math.floor(py / scaleY));
                tile.setRGB(px, py, colorAt(cssX, cssY));
            }
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(tile, "png", out);
                return target.convertFromPngBytes(out.toByteArray());
            } catch (Exception failure) { throw new RuntimeException(failure); }
        }

        private Map<String, Object> geometry() {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("documentWidth", documentWidth); value.put("documentHeight", documentHeight);
            value.put("viewportWidth", viewportWidth); value.put("viewportHeight", viewportHeight);
            value.put("scrollX", scrollX); value.put("scrollY", scrollY); value.put("topLevel", topLevel);
            return value;
        }

        @Override public TargetLocator switchTo() { switchCalls++; return super.switchTo(); }
    }

    private static final class ScreenshotOnlyDriver extends BaseDriver implements TakesScreenshot {
        private final Path png;
        ScreenshotOnlyDriver(Path png) { this.png = png; }
        @Override public <X> X getScreenshotAs(OutputType<X> target) {
            try { return target.convertFromPngBytes(Files.readAllBytes(png)); }
            catch (Exception failure) { throw new RuntimeException(failure); }
        }
    }

    private static class BaseDriver implements WebDriver {
        @Override public void get(String url) { }
        @Override public String getCurrentUrl() { return ""; }
        @Override public String getTitle() { return ""; }
        @Override public List<WebElement> findElements(By by) { return List.of(); }
        @Override public WebElement findElement(By by) { throw new UnsupportedOperationException(); }
        @Override public String getPageSource() { return ""; }
        @Override public void close() { }
        @Override public void quit() { }
        @Override public Set<String> getWindowHandles() { return Set.of(); }
        @Override public String getWindowHandle() { return ""; }
        @Override public TargetLocator switchTo() { throw new UnsupportedOperationException(); }
        @Override public Navigation navigate() { throw new UnsupportedOperationException(); }
        @Override public Options manage() { throw new UnsupportedOperationException(); }
    }
}
