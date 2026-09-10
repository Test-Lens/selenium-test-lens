package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.UiTestLensSession;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Captures viewport or portable full-page PNG evidence without CDP or window resizing.
 * Full-page capture snapshots the current top-level document dimensions, scrolls the existing viewport, and
 * stitches standard Selenium screenshots. It does not expand frames, shadow roots, or nested scroll containers,
 * and screenshot pixels are not redacted.
 */
public final class ScreenshotCapture {
    private static final String STATE_KEY_PREFIX = "__testLensFullPage_";
    private final WebDriver driver;

    public ScreenshotCapture(WebDriver driver) {
        if (driver == null) throw new IllegalArgumentException("driver must not be null");
        this.driver = driver;
    }

    public ScreenshotCaptureResult capture(String name, ScreenshotCaptureOptions options) {
        return capture(name, options, null);
    }

    public ScreenshotCaptureResult capture(String name, ScreenshotCaptureOptions options, UiTestLensSession session) {
        ScreenshotCaptureOptions effective = options == null ? ScreenshotCaptureOptions.defaults() : options;
        ScreenshotCaptureMode requestedMode = effective.captureMode();
        String effectiveName = name == null || name.isBlank() ? "Screenshot" : name.trim();
        if (!(driver instanceof TakesScreenshot takesScreenshot)) {
            return ScreenshotCaptureResult.failed(effectiveName, null,
                    "WebDriver does not implement TakesScreenshot", null, requestedMode);
        }
        if (requestedMode == ScreenshotCaptureMode.FULL_PAGE && !(driver instanceof JavascriptExecutor)) {
            return ScreenshotCaptureResult.failed(effectiveName, null,
                    "Full-page capture requires JavascriptExecutor", null, requestedMode);
        }

        Path destination = null;
        Path temporary = null;
        long started = System.nanoTime();
        try {
            Path output = prepareOutputDirectory(effective.outputDirectory());
            destination = EvidencePathStrategy.screenshotPath(effectiveName, effective).toAbsolutePath().normalize();
            validateDestination(output, destination);
            CaptureData captured = requestedMode == ScreenshotCaptureMode.FULL_PAGE
                    ? captureFullPage(takesScreenshot, (JavascriptExecutor) driver, effective)
                    : captureViewport(takesScreenshot);

            temporary = Files.createTempFile(output, destination.getFileName().toString(), ".part");
            if (!ImageIO.write(captured.image(), "png", temporary.toFile())) {
                throw new IOException("No PNG writer is available");
            }
            publish(temporary, destination, effective.overwriteExisting());
            temporary = null;

            TraceArtifact artifact = null;
            String message = "Screenshot captured";
            if (effective.attachToSession()) {
                if (session != null) {
                    artifact = TraceArtifact.screenshot(effectiveName, destination)
                            .withMetadata("capturedAt", Instant.now().toString())
                            .withMetadata("captureMode", captured.mode().name())
                            .withMetadata("width", String.valueOf(captured.image().getWidth()))
                            .withMetadata("height", String.valueOf(captured.image().getHeight()))
                            .withMetadata("tileCount", String.valueOf(captured.tileCount()))
                            .withMetadata("durationMs", String.valueOf(elapsedMillis(started)));
                    session.attachArtifact(artifact);
                } else {
                    message = "Screenshot captured; no UiTestLensSession attached";
                }
            }
            return ScreenshotCaptureResult.captured(effectiveName, destination, artifact, message,
                    captured.mode(), captured.image().getWidth(), captured.image().getHeight(), captured.tileCount());
        } catch (UnsupportedContextException unsupported) {
            return ScreenshotCaptureResult.skipped(effectiveName, unsupported.getMessage(), requestedMode);
        } catch (IOException | RuntimeException failure) {
            return ScreenshotCaptureResult.failed(effectiveName, destination,
                    "Screenshot capture failed: " + messageFor(failure), failure, requestedMode);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }

    private CaptureData captureViewport(TakesScreenshot screenshots) throws IOException {
        File source = screenshots.getScreenshotAs(OutputType.FILE);
        if (source == null) throw new IOException("Screenshot file is unavailable");
        BufferedImage image = decode(Files.readAllBytes(source.toPath()));
        return new CaptureData(ScreenshotCaptureMode.VIEWPORT, image, 1);
    }

    private CaptureData captureFullPage(TakesScreenshot screenshots, JavascriptExecutor javascript,
                                        ScreenshotCaptureOptions options) throws IOException {
        String stateKey = STATE_KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        PageSnapshot page;
        try {
            page = snapshotAndPrepare(javascript, stateKey);
        } catch (RuntimeException snapshotFailure) {
            restore(javascript, stateKey, snapshotFailure);
            throw snapshotFailure;
        }
        if (!page.topLevel()) {
            restore(javascript, stateKey, null);
            throw new UnsupportedContextException(
                    "Full-page capture is supported only in the current top-level browsing context");
        }
        Throwable primary = null;
        try {
            validateCssDimensions(page, options);
            long horizontalTiles = tileCount(page.documentWidth(), page.viewportWidth());
            long verticalTiles = tileCount(page.documentHeight(), page.viewportHeight());
            long requestedTiles = Math.multiplyExact(horizontalTiles, verticalTiles);
            if (requestedTiles > options.maxTileCount()) {
                throw new IllegalArgumentException("Full-page capture requires " + requestedTiles
                        + " tiles, exceeding maxTileCount=" + options.maxTileCount());
            }
            List<Long> xs = positions(page.documentWidth(), page.viewportWidth());
            List<Long> ys = positions(page.documentHeight(), page.viewportHeight());

            Set<Position> capturedPositions = new LinkedHashSet<>();
            double scaleX = 0;
            double scaleY = 0;
            BufferedImage stitched = null;
            int tileCount = 0;
            long coveredBottom = 0;
            for (long y : ys) {
                long rowCoveredRight = 0;
                long rowBottom = coveredBottom;
                for (long x : xs) {
                    PageSnapshot current = scrollAndObserve(javascript, x, y);
                    requireStablePage(page, current);
                    Position actual = new Position(current.scrollX(), current.scrollY());
                    if (!capturedPositions.add(actual)) continue;

                    BufferedImage tile = decode(screenshots.getScreenshotAs(OutputType.BYTES));
                    tileCount++;
                    double currentScaleX = tile.getWidth() / (double) page.viewportWidth();
                    double currentScaleY = tile.getHeight() / (double) page.viewportHeight();
                    if (scaleX == 0) {
                        scaleX = currentScaleX;
                        scaleY = currentScaleY;
                        int outputWidth = scaledDimension(page.documentWidth(), scaleX, "width");
                        int outputHeight = scaledDimension(page.documentHeight(), scaleY, "height");
                        long pixels = Math.multiplyExact((long) outputWidth, (long) outputHeight);
                        if (pixels > options.maxPixelCount()) {
                            throw new IllegalArgumentException("Full-page image " + outputWidth + "x" + outputHeight
                                    + " exceeds maxPixelCount=" + options.maxPixelCount());
                        }
                        stitched = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB);
                    } else if (tile.getWidth() != Math.round(page.viewportWidth() * scaleX)
                            || tile.getHeight() != Math.round(page.viewportHeight() * scaleY)
                            || Math.abs(currentScaleX - scaleX) > 0.000001
                            || Math.abs(currentScaleY - scaleY) > 0.000001) {
                        throw new IllegalStateException("Viewport screenshot dimensions or scale changed during capture");
                    }
                    long overlapX = Math.max(0, rowCoveredRight - actual.x());
                    long overlapY = Math.max(0, coveredBottom - actual.y());
                    drawTile(stitched, tile, actual, overlapX, overlapY, scaleX, scaleY);
                    rowCoveredRight = Math.max(rowCoveredRight, actual.x() + page.viewportWidth());
                    rowBottom = Math.max(rowBottom, actual.y() + page.viewportHeight());
                    hideRepeatedElements(javascript, stateKey);
                }
                coveredBottom = rowBottom;
            }
            if (stitched == null || tileCount == 0) throw new IllegalStateException("No screenshot tiles were captured");
            return new CaptureData(ScreenshotCaptureMode.FULL_PAGE, stitched, tileCount);
        } catch (IOException | RuntimeException failure) {
            primary = failure;
            throw failure;
        } finally {
            restore(javascript, stateKey, primary);
        }
    }

    private static PageSnapshot snapshotAndPrepare(JavascriptExecutor js, String key) {
        Object result = js.executeScript("""
                const key = arguments[0];
                const root = document.documentElement;
                const body = document.body;
                const remember = (element, property) => ({element, property,
                  value: element.style.getPropertyValue(property), priority: element.style.getPropertyPriority(property)});
                const saved = [];
                for (const element of [root, body]) {
                  if (!element) continue;
                  for (const property of ['scroll-behavior', 'scroll-snap-type']) saved.push(remember(element, property));
                  element.style.setProperty('scroll-behavior', 'auto', 'important');
                  element.style.setProperty('scroll-snap-type', 'none', 'important');
                }
                window[key] = {token: key, saved, hidden: [], scrollX: window.scrollX, scrollY: window.scrollY};
                const width = Math.max(root ? root.scrollWidth : 0, root ? root.offsetWidth : 0,
                  body ? body.scrollWidth : 0, body ? body.offsetWidth : 0, window.innerWidth);
                const height = Math.max(root ? root.scrollHeight : 0, root ? root.offsetHeight : 0,
                  body ? body.scrollHeight : 0, body ? body.offsetHeight : 0, window.innerHeight);
                return {documentWidth: width, documentHeight: height, viewportWidth: window.innerWidth,
                  viewportHeight: window.innerHeight, scrollX: window.scrollX, scrollY: window.scrollY,
                  topLevel: window.top === window};
                """, key);
        return pageSnapshot(result);
    }

    private static PageSnapshot scrollAndObserve(JavascriptExecutor js, long x, long y) {
        Object result = js.executeAsyncScript("""
                const x = arguments[0], y = arguments[1], done = arguments[arguments.length - 1];
                window.scrollTo(x, y);
                requestAnimationFrame(() => requestAnimationFrame(() => {
                  const root = document.documentElement, body = document.body;
                  done({documentWidth: Math.max(root ? root.scrollWidth : 0, root ? root.offsetWidth : 0,
                    body ? body.scrollWidth : 0, body ? body.offsetWidth : 0, window.innerWidth),
                    documentHeight: Math.max(root ? root.scrollHeight : 0, root ? root.offsetHeight : 0,
                    body ? body.scrollHeight : 0, body ? body.offsetHeight : 0, window.innerHeight),
                    viewportWidth: window.innerWidth, viewportHeight: window.innerHeight,
                    scrollX: window.scrollX, scrollY: window.scrollY, topLevel: window.top === window});
                }));
                """, x, y);
        return pageSnapshot(result);
    }

    private static void hideRepeatedElements(JavascriptExecutor js, String key) {
        js.executeScript("""
                const state = window[arguments[0]];
                if (!state || state.token !== arguments[0]) throw new Error('Full-page capture state is unavailable');
                for (const element of document.querySelectorAll('*')) {
                  if (element === document.documentElement || element === document.body) continue;
                  const style = getComputedStyle(element);
                  if (style.position !== 'fixed' && style.position !== 'sticky') continue;
                  const rect = element.getBoundingClientRect();
                  if (rect.bottom <= 0 || rect.right <= 0 || rect.top >= innerHeight || rect.left >= innerWidth) continue;
                  if (style.visibility === 'hidden' || style.display === 'none') continue;
                  state.hidden.push({element, value: element.style.getPropertyValue('visibility'),
                    priority: element.style.getPropertyPriority('visibility')});
                  element.style.setProperty('visibility', 'hidden', 'important');
                }
                """, key);
    }

    private static void restore(JavascriptExecutor js, String key, Throwable primary) {
        try {
            Object restored = js.executeScript("""
                    const state = window[arguments[0]];
                    if (!state || state.token !== arguments[0]) return false;
                    for (let i = state.hidden.length - 1; i >= 0; i--) {
                      const item = state.hidden[i];
                      if (!item.element || !item.element.style) continue;
                      if (item.value) item.element.style.setProperty('visibility', item.value, item.priority || '');
                      else item.element.style.removeProperty('visibility');
                    }
                    for (let i = state.saved.length - 1; i >= 0; i--) {
                      const item = state.saved[i];
                      if (item.value) item.element.style.setProperty(item.property, item.value, item.priority || '');
                      else item.element.style.removeProperty(item.property);
                    }
                    window.scrollTo(state.scrollX, state.scrollY);
                    delete window[arguments[0]];
                    return true;
                    """, key);
            if (!Boolean.TRUE.equals(restored)) throw new IllegalStateException("Full-page capture state could not be restored");
        } catch (RuntimeException restorationFailure) {
            if (primary != null) primary.addSuppressed(restorationFailure);
            else throw restorationFailure;
        }
    }

    private static void drawTile(BufferedImage output, BufferedImage tile, Position position,
                                 long overlapX, long overlapY, double scaleX, double scaleY) {
        int sourceX = Math.min(tile.getWidth(), scaledOffset(overlapX, scaleX));
        int sourceY = Math.min(tile.getHeight(), scaledOffset(overlapY, scaleY));
        int destinationX = scaledOffset(position.x(), scaleX) + sourceX;
        int destinationY = scaledOffset(position.y(), scaleY) + sourceY;
        int width = Math.min(tile.getWidth() - sourceX, output.getWidth() - destinationX);
        int height = Math.min(tile.getHeight() - sourceY, output.getHeight() - destinationY);
        if (width <= 0 || height <= 0) return;
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(tile, destinationX, destinationY, destinationX + width, destinationY + height,
                    sourceX, sourceY, sourceX + width, sourceY + height, null);
        } finally {
            graphics.dispose();
        }
    }

    private static BufferedImage decode(byte[] png) throws IOException {
        if (png == null || png.length == 0) throw new IOException("Screenshot PNG is empty");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
            throw new IOException("Screenshot data is not a valid PNG image");
        }
        return image;
    }

    private static List<Long> positions(long document, long viewport) {
        long maximum = Math.max(0, document - viewport);
        List<Long> positions = new ArrayList<>();
        for (long value = 0; value < maximum; value = Math.addExact(value, viewport)) positions.add(value);
        positions.add(maximum);
        return List.copyOf(new LinkedHashSet<>(positions));
    }

    private static long tileCount(long document, long viewport) {
        return Math.addExact(Math.floorDiv(document - 1, viewport), 1);
    }

    private static void validateCssDimensions(PageSnapshot page, ScreenshotCaptureOptions options) {
        if (page.documentWidth() < 1 || page.documentHeight() < 1
                || page.viewportWidth() < 1 || page.viewportHeight() < 1) {
            throw new IllegalArgumentException("Document and viewport dimensions must be positive");
        }
        long cssPixels = Math.multiplyExact(page.documentWidth(), page.documentHeight());
        if (cssPixels > options.maxPixelCount()) {
            throw new IllegalArgumentException("Full-page CSS dimensions " + page.documentWidth() + "x"
                    + page.documentHeight() + " exceed maxPixelCount=" + options.maxPixelCount());
        }
    }

    private static void requireStablePage(PageSnapshot initial, PageSnapshot current) {
        if (!current.topLevel()) throw new UnsupportedContextException("Browsing context changed during full-page capture");
        if (initial.documentWidth() != current.documentWidth() || initial.documentHeight() != current.documentHeight()
                || initial.viewportWidth() != current.viewportWidth() || initial.viewportHeight() != current.viewportHeight()) {
            throw new IllegalStateException("Document or viewport dimensions changed during full-page capture");
        }
    }

    private static PageSnapshot pageSnapshot(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalStateException("Invalid page geometry response");
        return new PageSnapshot(number(map, "documentWidth"), number(map, "documentHeight"),
                number(map, "viewportWidth"), number(map, "viewportHeight"),
                number(map, "scrollX"), number(map, "scrollY"), Boolean.TRUE.equals(map.get("topLevel")));
    }

    private static long number(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number)) throw new IllegalStateException("Missing page geometry: " + key);
        double numeric = number.doubleValue();
        if (!Double.isFinite(numeric) || numeric < 0 || numeric > Long.MAX_VALUE) {
            throw new IllegalStateException("Invalid page geometry: " + key);
        }
        return Math.round(numeric);
    }

    private static int scaledDimension(long css, double scale, String label) {
        double result = css * scale;
        if (!Double.isFinite(result) || result < 1 || result > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Full-page " + label + " exceeds image limits");
        }
        return Math.toIntExact(Math.round(result));
    }

    private static int scaledOffset(long css, double scale) { return Math.toIntExact(Math.round(css * scale)); }

    private static Path prepareOutputDirectory(Path configured) throws IOException {
        Path output = configured.toAbsolutePath().normalize();
        Files.createDirectories(output);
        if (Files.isSymbolicLink(output)) throw new IOException("Screenshot output directory must not be a symbolic link");
        return output;
    }

    private static void validateDestination(Path output, Path destination) throws IOException {
        if (!destination.getParent().equals(output)) {
            throw new IOException("Screenshot destination is outside the configured output directory");
        }
        if (Files.isSymbolicLink(destination)) throw new IOException("Screenshot destination must not be a symbolic link");
    }

    private static void publish(Path source, Path destination, boolean overwrite) throws IOException {
        if (!overwrite && Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Screenshot destination already exists");
        }
        try {
            if (overwrite) Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            else Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException unsupported) {
            if (overwrite) Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            else Files.move(source, destination);
        }
    }

    private static long elapsedMillis(long started) { return Math.max(0, (System.nanoTime() - started) / 1_000_000L); }

    private static String messageFor(Throwable throwable) {
        if (throwable == null) return "";
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record CaptureData(ScreenshotCaptureMode mode, BufferedImage image, int tileCount) { }
    private record PageSnapshot(long documentWidth, long documentHeight, long viewportWidth, long viewportHeight,
                                long scrollX, long scrollY, boolean topLevel) { }
    private record Position(long x, long y) { }
    private static final class UnsupportedContextException extends RuntimeException {
        private UnsupportedContextException(String message) { super(message); }
    }
}
