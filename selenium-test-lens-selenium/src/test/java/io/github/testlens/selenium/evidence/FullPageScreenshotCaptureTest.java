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
        assertEquals(1, driver.guardInstallCalls);
        assertFalse(driver.guardActive);
        assertEquals(1, driver.overlaySnapshotInstallCalls);
        assertEquals(1, driver.overlaySnapshotRestoreCalls);
        assertFalse(driver.overlaySnapshotActive);
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
        assertTrue(result.exception().getMessage().contains("TILE_CAPTURE_FAILED: tile failure"));
        assertEquals(1, result.exception().getSuppressed().length);
        assertEquals("restore failure", result.exception().getSuppressed()[0].getMessage());
        assertTrue(driver.guardActive, "the fake restoration failure deliberately leaves the guard installed");
        assertFalse(Files.exists(tempDir.resolve("shot_page.png")));
    }

    @Test
    void middleTileFailureStillRestoresAndRemovesTheScreenshotGuard() {
        StitchDriver driver = new StitchDriver(4, 7, 4, 3, 1.0);
        driver.failScreenshotAt = 2;

        ScreenshotCaptureResult result = capture(driver, null, 100, 10);

        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertTrue(result.exception().getMessage().contains("TILE_CAPTURE_FAILED: tile failure"));
        assertEquals(1, driver.restoreCalls);
        assertFalse(driver.guardActive);
        assertFalse(driver.overlaySnapshotActive);
        assertFalse(Files.exists(tempDir.resolve("shot_page.png")));
    }

    @Test
    void heightGrowthAfterFirstScrollExtendsCaptureWithoutRetry() {
        StitchDriver driver = new StitchDriver(4, 7, 4, 3, 1.0);
        driver.heightsAfterPositiveScroll = List.of(9L);

        ScreenshotCaptureResult result = capture(driver, null, 100, 10);

        assertTrue(result.isCaptured(), result.message());
        assertEquals(9, result.height());
        assertEquals(3, result.tileCount());
        assertEquals(1, driver.guardInstallCalls, "legitimate height growth must not consume a retry");
        assertEquals(1, driver.restoreCalls);
        assertFalse(driver.guardActive);
        assertEquals(1, driver.overlaySnapshotInstallCalls,
                "the overlay snapshot spans the complete dynamic capture");
        assertEquals(1, driver.overlaySnapshotRestoreCalls);
        assertFalse(driver.overlaySnapshotActive);
        assertEquals(result.tileCount(), driver.screenshotCalls);
        assertTrue(Files.exists(tempDir.resolve("shot_page.png")));
    }

    @Test
    void severalBoundedHeightExpansionsExtendCaptureUntilStable() throws Exception {
        StitchDriver driver = new StitchDriver(4, 7, 4, 3, 1.0);
        driver.heightsAfterPositiveScroll = List.of(8L, 9L, 10L);

        ScreenshotCaptureResult result = capture(driver, null, 100, 10);

        assertTrue(result.isCaptured(), result.message());
        assertEquals(10, result.height());
        assertEquals(1, driver.guardInstallCalls);
        BufferedImage image = ImageIO.read(result.path().toFile());
        for (int y = 0; y < 10; y++) {
            assertEquals(colorAt(1, y), image.getRGB(1, y), "dynamic row " + y);
        }
    }

    @Test
    void heightShrinkClampsBottomScrollAndCropsWithoutDuplicateContent() throws Exception {
        StitchDriver driver = new StitchDriver(4, 8, 4, 3, 1.0);
        driver.heightsAfterPositiveScroll = List.of(5L);

        ScreenshotCaptureResult result = capture(driver, null, 100, 10);

        assertTrue(result.isCaptured(), result.message());
        assertEquals(5, result.height());
        assertEquals(2, result.tileCount());
        assertTrue(driver.clampedScrolls > 0);
        BufferedImage image = ImageIO.read(result.path().toFile());
        for (int y = 0; y < 5; y++) {
            assertEquals(colorAt(1, y), image.getRGB(1, y), "cropped row " + y);
        }
    }

    @Test
    void infiniteHeightGrowthFailsAtExpansionBoundWithoutRetryOrPartialImage() {
        StitchDriver driver = new StitchDriver(4, 7, 4, 3, 1.0);
        driver.growAfterEveryPositiveScroll = true;

        ScreenshotCaptureResult result = capture(driver, null, 1_000, 100);

        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertTrue(result.message().contains("DOCUMENT_HEIGHT_DID_NOT_STABILIZE"), result.message());
        assertTrue(result.message().contains("maxDocumentHeightExpansions=8"), result.message());
        assertTrue(result.message().contains("documentHeightExpansions=9"), result.message());
        assertEquals(1, driver.guardInstallCalls);
        assertEquals(1, driver.restoreCalls);
        assertFalse(driver.guardActive);
        assertFalse(driver.overlaySnapshotActive);
        assertFalse(Files.exists(tempDir.resolve("shot_page.png")));
    }

    @Test
    void viewportWidthAndDprChangesRemainRetryableHardFailures() {
        StitchDriver width = new StitchDriver(4, 7, 4, 3, 1.0);
        width.hardGeometryChangesRemaining = Integer.MAX_VALUE;
        width.hardGeometryChange = HardGeometryChange.VIEWPORT_WIDTH;
        ScreenshotCaptureResult widthResult = capture(width, null, 100, 10);
        assertEquals(ScreenshotCaptureStatus.FAILED, widthResult.status());
        assertTrue(widthResult.message().contains("VIEWPORT_GEOMETRY_CHANGED"), widthResult.message());
        assertTrue(widthResult.message().contains("viewportWidth"), widthResult.message());
        assertTrue(widthResult.message().contains("after 2 attempts"), widthResult.message());
        assertEquals(2, width.guardInstallCalls);

        StitchDriver dpr = new StitchDriver(4, 7, 4, 3, 1.0);
        dpr.hardGeometryChangesRemaining = Integer.MAX_VALUE;
        dpr.hardGeometryChange = HardGeometryChange.DPR;
        ScreenshotCaptureResult dprResult = capture(dpr, null, 100, 10);
        assertEquals(ScreenshotCaptureStatus.FAILED, dprResult.status());
        assertTrue(dprResult.message().contains("VIEWPORT_GEOMETRY_CHANGED"), dprResult.message());
        assertTrue(dprResult.message().contains("devicePixelRatio"), dprResult.message());
        assertEquals(2, dpr.guardInstallCalls);
    }

    @Test
    void tileScaleChangeStillFailsWithoutPublishingPartialImage() {

        StitchDriver scale = new StitchDriver(4, 7, 4, 3, 1.0);
        scale.changeScale = true;
        ScreenshotCaptureResult scaleResult = capture(scale, null, 100, 10);
        assertEquals(ScreenshotCaptureStatus.FAILED, scaleResult.status());
        assertTrue(scaleResult.message().contains("scale changed"));
        assertTrue(scaleResult.message().contains("VIEWPORT_GEOMETRY_CHANGED"));
        assertFalse(scale.guardActive);
        assertFalse(scale.overlaySnapshotActive);
        assertEquals(1, scale.overlaySnapshotRestoreCalls);
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
        final long documentWidth, viewportWidth, viewportHeight;
        long documentHeight;
        final double scaleX, scaleY;
        long scrollX, scrollY;
        boolean topLevel = true;
        boolean failRestore;
        List<Long> heightsAfterPositiveScroll = List.of();
        int positiveScrolls;
        boolean growAfterEveryPositiveScroll;
        int hardGeometryChangesRemaining;
        HardGeometryChange hardGeometryChange;
        boolean changeScale;
        boolean guardActive;
        boolean overlaySnapshotActive;
        int failScreenshotAt;
        int screenshotCalls, screenshotsThisAttempt, restoreCalls, hideCalls, switchCalls, guardInstallCalls, clampedScrolls;
        int overlaySnapshotInstallCalls, overlaySnapshotRestoreCalls;
        long savedScrollX, savedScrollY;
        String activeContextToken = "";

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
            if (script.contains("cloneNode(false)")) {
                overlaySnapshotInstallCalls++;
                overlaySnapshotActive = true;
                return true;
            }
            if (script.contains("state.snapshot && state.snapshot.isConnected")) {
                overlaySnapshotRestoreCalls++;
                overlaySnapshotActive = false;
                return true;
            }
            if (script.contains("querySelectorAll")) { hideCalls++; return null; }
            if (script.contains("state.hidden.length")) {
                restoreCalls++;
                if (failRestore) throw new IllegalStateException("restore failure");
                guardActive = false;
                activeContextToken = "";
                scrollX = savedScrollX; scrollY = savedScrollY; return true;
            }
            throw new AssertionError("Unexpected script");
        }

        @Override public Object executeAsyncScript(String script, Object... args) {
            if (script.contains("data-test-lens-screenshot-guard")) {
                savedScrollX = scrollX;
                savedScrollY = scrollY;
                guardInstallCalls++;
                guardActive = true;
                screenshotsThisAttempt = 0;
                activeContextToken = String.valueOf(args[0]);
                return geometry();
            }
            if (!script.contains("window.scrollTo(x, y)")) return geometry();
            long requestedX = ((Number) args[1]).longValue();
            long requestedY = ((Number) args[2]).longValue();
            if (requestedY > 0) {
                if (positiveScrolls < heightsAfterPositiveScroll.size()) {
                    documentHeight = heightsAfterPositiveScroll.get(positiveScrolls);
                } else if (growAfterEveryPositiveScroll) {
                    documentHeight++;
                }
                positiveScrolls++;
            }
            scrollX = Math.min(requestedX, Math.max(0, documentWidth - viewportWidth));
            scrollY = Math.min(requestedY, Math.max(0, documentHeight - viewportHeight));
            if (scrollX != requestedX || scrollY != requestedY) clampedScrolls++;
            Map<String, Object> result = geometry();
            if (hardGeometryChangesRemaining > 0 && screenshotCalls > 0) {
                hardGeometryChangesRemaining--;
                if (hardGeometryChange == HardGeometryChange.VIEWPORT_WIDTH) {
                    result.put("viewportWidth", viewportWidth + 1);
                } else if (hardGeometryChange == HardGeometryChange.DPR) {
                    result.put("devicePixelRatio", 2.0);
                }
            }
            return result;
        }

        @Override public <X> X getScreenshotAs(OutputType<X> target) {
            assertTrue(guardInstallCalls == 0 || guardActive,
                    "every full-page tile must be captured while the screenshot guard is active");
            assertTrue(guardInstallCalls == 0 || overlaySnapshotActive,
                    "every full-page tile must use the one frozen overlay snapshot");
            screenshotCalls++;
            screenshotsThisAttempt++;
            if (failScreenshotAt == screenshotCalls) throw new IllegalStateException("tile failure");
            int width = (int) Math.round(viewportWidth * scaleX);
            int height = (int) Math.round(viewportHeight * scaleY);
            if (changeScale && screenshotsThisAttempt > 1) width++;
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
            value.put("scrollX", scrollX); value.put("scrollY", scrollY);
            value.put("devicePixelRatio", 1.0); value.put("topLevel", topLevel);
            value.put("contextToken", activeContextToken);
            return value;
        }

        @Override public TargetLocator switchTo() { switchCalls++; return super.switchTo(); }
    }

    private enum HardGeometryChange { VIEWPORT_WIDTH, DPR }

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
