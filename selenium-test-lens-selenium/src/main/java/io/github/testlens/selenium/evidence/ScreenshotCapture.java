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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Captures viewport or portable full-page PNG evidence without CDP or window resizing.
 * Full-page capture keeps viewport geometry fixed while adapting its bounded vertical extent to document-height
 * changes, scrolls the existing viewport, and stitches standard Selenium screenshots. It does not expand frames,
 * shadow roots, or nested scroll containers, and applies configured browser-side visual masks before pixels are
 * captured.
 */
public final class ScreenshotCapture {
    private static final String STATE_KEY_PREFIX = "__testLensFullPage_";
    private static final String OVERLAY_STATE_KEY_PREFIX = "__testLensOverlaySnapshot_";
    private static final int FULL_PAGE_MAX_ATTEMPTS = 2;
    private static final int MAX_DOCUMENT_HEIGHT_EXPANSIONS = 8;
    private static final int REQUIRED_STABLE_BOTTOM_SAMPLES = 2;
    private static final int GEOMETRY_OBSERVATION_LIMIT_PADDING = 16;
    private static final Logger LOGGER = Logger.getLogger(ScreenshotCapture.class.getName());
    private final WebDriver driver;
    private final VisualRedactionOptions visualRedaction;

    public ScreenshotCapture(WebDriver driver) {
        this(driver, VisualRedactionOptions.defaults());
    }

    /**
     * Creates a capture service with an explicit screenshot-pixel masking policy.
     * @param driver consumer-owned WebDriver
     * @param visualRedaction masking options; null restores password-safe defaults
     * @since 0.3.0
     */
    public ScreenshotCapture(WebDriver driver, VisualRedactionOptions visualRedaction) {
        if (driver == null) throw new IllegalArgumentException("driver must not be null");
        this.driver = driver;
        this.visualRedaction = visualRedaction == null ? VisualRedactionOptions.defaults() : visualRedaction;
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
            VisualRedactionController masks = new VisualRedactionController(driver, visualRedaction);
            CaptureData captured;
            Throwable captureFailure = null;
            try {
                if (requestedMode == ScreenshotCaptureMode.FULL_PAGE) {
                    captured = captureFullPage(takesScreenshot, (JavascriptExecutor) driver, effective, masks);
                } else {
                    masks.apply();
                    captured = captureViewport(takesScreenshot);
                }
            } catch (IOException | RuntimeException failure) {
                captureFailure = failure;
                throw failure;
            } finally {
                masks.remove(captureFailure);
            }

            temporary = Files.createTempFile(output, destination.getFileName().toString(), ".part");
            if (!ImageIO.write(captured.image(), "png", temporary.toFile())) {
                throw new IOException("No PNG writer is available");
            }
            publish(temporary, destination, effective.overwriteExisting());
            temporary = null;

            TraceArtifact artifact = null;
            String message = masks.warnings().isEmpty() ? "Screenshot captured"
                    : "Screenshot captured with visual-redaction warning(s): " + String.join("; ", masks.warnings());
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
                    message = message + "; no UiTestLensSession attached";
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
                                        ScreenshotCaptureOptions options, VisualRedactionController masks) throws IOException {
        LOGGER.fine("Full-page screenshot capture");
        String overlayKey = OVERLAY_STATE_KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        Throwable primary = null;
        try {
            installOverlaySnapshot(javascript, overlayKey);
            for (int attempt = 1; attempt <= FULL_PAGE_MAX_ATTEMPTS; attempt++) {
                try {
                    CaptureData captured = captureFullPageAttempt(screenshots, javascript, options, masks, attempt);
                    LOGGER.fine("Full-page screenshot captured");
                    return captured;
                } catch (ViewportGeometryChangedException changed) {
                    if (attempt == FULL_PAGE_MAX_ATTEMPTS) {
                        throw new ViewportGeometryChangedException(
                                changed.getMessage() + " after " + attempt + " attempts", changed);
                    }
                    LOGGER.log(Level.FINE, "Full-page screenshot retry: hard viewport geometry changed", changed);
                }
            }
            throw new IllegalStateException("Full-page screenshot capture exhausted its retry limit");
        } catch (IOException | RuntimeException failure) {
            primary = failure;
            throw failure;
        } finally {
            restoreOverlay(javascript, overlayKey, primary);
        }
    }

    private CaptureData captureFullPageAttempt(TakesScreenshot screenshots, JavascriptExecutor javascript,
                                                ScreenshotCaptureOptions options,
                                                VisualRedactionController masks, int attempt) throws IOException {
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
        LOGGER.fine(() -> "Full-page attempt " + attempt + " baseline=" + page);
        Throwable primary = null;
        try {
            validateCssDimensions(page);
            CaptureExtent extent = new CaptureExtent(page, geometryObservationLimit(options.maxTileCount()));
            validateCaptureExtent(page, extent, options,
                    tileCount(page.documentWidth(), page.viewportWidth()),
                    0, 0, attempt, 0, new Position(0, 0), page);
            List<Long> xs = positions(page.documentWidth(), page.viewportWidth());
            Set<Position> capturedPositions = new LinkedHashSet<>();
            double scaleX = 0;
            double scaleY = 0;
            BufferedImage stitched = null;
            int capturedTileCount = 0;
            int tileIndex = 0;
            long coveredBottom = 0;
            long requestedY = 0;
            boolean bottomStable = false;
            while (!bottomStable) {
                long rowCoveredRight = 0;
                long rowBottom = coveredBottom;
                for (long x : xs) {
                    Position requested = new Position(x, requestedY);
                    PageSnapshot current = scrollAndObserve(javascript, stateKey, x, requestedY);
                    int observedTile = tileIndex;
                    LOGGER.fine(() -> "Full-page attempt " + attempt + " tile=" + observedTile
                            + " geometry=" + current);
                    tileIndex++;
                    requireStableGeometry(page, current, attempt, observedTile, requested, extent);
                    extent.observe(current.documentHeight(), attempt, observedTile, requested, current);
                    validateCaptureExtent(page, extent, options, xs.size(), scaleX, scaleY,
                            attempt, observedTile, requested, current);
                    Position actual = new Position(current.scrollX(), current.scrollY());
                    if (!capturedPositions.add(actual)) continue;

                    masks.refresh();
                    BufferedImage tile;
                    try {
                        tile = decode(screenshots.getScreenshotAs(OutputType.BYTES));
                    } catch (IOException | RuntimeException failure) {
                        throw tileCaptureFailed(page, current, extent, attempt, observedTile, requested, failure);
                    }
                    double currentScaleX = tile.getWidth() / (double) page.viewportWidth();
                    double currentScaleY = tile.getHeight() / (double) page.viewportHeight();
                    if (scaleX == 0) {
                        scaleX = currentScaleX;
                        scaleY = currentScaleY;
                        validateCaptureExtent(page, extent, options, xs.size(), scaleX, scaleY,
                                attempt, observedTile, requested, current);
                    } else if (tile.getWidth() != Math.round(page.viewportWidth() * scaleX)
                            || tile.getHeight() != Math.round(page.viewportHeight() * scaleY)
                            || Math.abs(currentScaleX - scaleX) > 0.000001
                            || Math.abs(currentScaleY - scaleY) > 0.000001) {
                        throw viewportGeometryChanged(page, current, extent, attempt, observedTile, requested,
                                "viewport screenshot pixel dimensions or scale changed from "
                                        + scaleX + "x" + scaleY + " to " + currentScaleX + "x" + currentScaleY);
                    }
                    int requiredWidth = scaledDimension(page.documentWidth(), scaleX, "width");
                    int requiredHeight = scaledDimension(extent.currentHeight(), scaleY, "height");
                    if (stitched == null) {
                        stitched = new BufferedImage(requiredWidth, requiredHeight, BufferedImage.TYPE_INT_ARGB);
                    } else if (requiredHeight > stitched.getHeight()) {
                        stitched = resizeCanvas(stitched, requiredWidth, requiredHeight);
                    }
                    long overlapX = Math.max(0, rowCoveredRight - actual.x());
                    long overlapY = Math.max(0, coveredBottom - actual.y());
                    drawTile(stitched, tile, actual, overlapX, overlapY, scaleX, scaleY);
                    capturedTileCount++;
                    rowCoveredRight = Math.max(rowCoveredRight, actual.x() + page.viewportWidth());
                    rowBottom = Math.max(rowBottom, actual.y() + page.viewportHeight());
                    hideRepeatedElements(javascript, stateKey);
                }
                coveredBottom = rowBottom;
                int stableSamples = 0;
                while (coveredBottom >= extent.currentHeight()
                        && stableSamples < REQUIRED_STABLE_BOTTOM_SAMPLES) {
                    PageSnapshot sample = observe(javascript, stateKey);
                    LOGGER.fine(() -> "Full-page attempt " + attempt + " bottom-sample=" + sample);
                    Position requested = new Position(0, requestedY);
                    requireStableGeometry(page, sample, attempt, tileIndex, requested, extent);
                    boolean changed = extent.observe(sample.documentHeight(), attempt, tileIndex, requested, sample);
                    validateCaptureExtent(page, extent, options, xs.size(), scaleX, scaleY,
                            attempt, tileIndex, requested, sample);
                    if (coveredBottom < extent.currentHeight()) break;
                    stableSamples = changed ? 0 : stableSamples + 1;
                }
                bottomStable = coveredBottom >= extent.currentHeight()
                        && stableSamples >= REQUIRED_STABLE_BOTTOM_SAMPLES;
                if (!bottomStable) {
                    requestedY = Math.min(coveredBottom,
                            Math.max(0, extent.currentHeight() - page.viewportHeight()));
                }
            }
            if (stitched == null || capturedTileCount == 0) {
                throw new IllegalStateException("No screenshot tiles were captured");
            }
            int outputWidth = scaledDimension(page.documentWidth(), scaleX, "width");
            int outputHeight = scaledDimension(extent.currentHeight(), scaleY, "height");
            if (stitched.getWidth() != outputWidth || stitched.getHeight() != outputHeight) {
                stitched = resizeCanvas(stitched, outputWidth, outputHeight);
            }
            return new CaptureData(ScreenshotCaptureMode.FULL_PAGE, stitched, capturedTileCount);
        } catch (RuntimeException failure) {
            primary = failure;
            throw failure;
        } finally {
            restore(javascript, stateKey, primary);
        }
    }

    private static PageSnapshot snapshotAndPrepare(JavascriptExecutor js, String key) {
        Object result = js.executeAsyncScript("""
                const key = arguments[0], done = arguments[arguments.length - 1];
                const root = document.documentElement;
                const css = `
                  *, *::before, *::after {
                    animation-play-state: paused !important;
                    transition-property: none !important;
                    transition-duration: 0s !important;
                    transition-delay: 0s !important;
                    caret-color: transparent !important;
                    scroll-behavior: auto !important;
                  }
                  html, body { scroll-behavior: auto !important; scroll-snap-type: none !important; }
                `;
                const state = {token: key, hidden: [], scrollX: window.scrollX, scrollY: window.scrollY,
                  sheet: null, style: null};
                if (typeof CSSStyleSheet === 'function' && 'adoptedStyleSheets' in document) {
                  const sheet = new CSSStyleSheet();
                  sheet.replaceSync(css);
                  document.adoptedStyleSheets = [...document.adoptedStyleSheets, sheet];
                  state.sheet = sheet;
                } else {
                  const style = document.createElement('style');
                  style.setAttribute('data-test-lens-screenshot-guard', key);
                  style.textContent = css;
                  (document.head || root).appendChild(style);
                  state.style = style;
                }
                window[key] = state;
                requestAnimationFrame(() => requestAnimationFrame(() => {
                  const body = document.body;
                  const width = Math.max(root ? root.scrollWidth : 0, root ? root.offsetWidth : 0,
                    body ? body.scrollWidth : 0, body ? body.offsetWidth : 0, window.innerWidth);
                  const height = Math.max(root ? root.scrollHeight : 0, root ? root.offsetHeight : 0,
                    body ? body.scrollHeight : 0, body ? body.offsetHeight : 0, window.innerHeight);
                  done({documentWidth: width, documentHeight: height, viewportWidth: window.innerWidth,
                    viewportHeight: window.innerHeight, scrollX: window.scrollX, scrollY: window.scrollY,
                    devicePixelRatio: window.devicePixelRatio, topLevel: window.top === window,
                    contextToken: key});
                }));
                """, key);
        return pageSnapshot(result);
    }

    private static PageSnapshot scrollAndObserve(JavascriptExecutor js, String key, long x, long y) {
        Object result = js.executeAsyncScript("""
                const key = arguments[0], x = arguments[1], y = arguments[2];
                const done = arguments[arguments.length - 1];
                window.scrollTo(x, y);
                requestAnimationFrame(() => requestAnimationFrame(() => {
                  const root = document.documentElement, body = document.body;
                  const result = {documentWidth: Math.max(root ? root.scrollWidth : 0, root ? root.offsetWidth : 0,
                    body ? body.scrollWidth : 0, body ? body.offsetWidth : 0, window.innerWidth),
                    documentHeight: Math.max(root ? root.scrollHeight : 0, root ? root.offsetHeight : 0,
                    body ? body.scrollHeight : 0, body ? body.offsetHeight : 0, window.innerHeight),
                    viewportWidth: window.innerWidth, viewportHeight: window.innerHeight,
                    scrollX: window.scrollX, scrollY: window.scrollY,
                    devicePixelRatio: window.devicePixelRatio, topLevel: window.top === window,
                    contextToken: window[key] && window[key].token === key ? key : ''};
                  done(result);
                }));
                """, key, x, y);
        return pageSnapshot(result);
    }

    private static PageSnapshot observe(JavascriptExecutor js, String key) {
        Object result = js.executeAsyncScript("""
                const key = arguments[0];
                const done = arguments[arguments.length - 1];
                requestAnimationFrame(() => requestAnimationFrame(() => {
                  const root = document.documentElement, body = document.body;
                  const result = {documentWidth: Math.max(root ? root.scrollWidth : 0, root ? root.offsetWidth : 0,
                    body ? body.scrollWidth : 0, body ? body.offsetWidth : 0, window.innerWidth),
                    documentHeight: Math.max(root ? root.scrollHeight : 0, root ? root.offsetHeight : 0,
                    body ? body.scrollHeight : 0, body ? body.offsetHeight : 0, window.innerHeight),
                    viewportWidth: window.innerWidth, viewportHeight: window.innerHeight,
                    scrollX: window.scrollX, scrollY: window.scrollY,
                    devicePixelRatio: window.devicePixelRatio, topLevel: window.top === window,
                    contextToken: window[key] && window[key].token === key ? key : ''};
                  done(result);
                }));
                """, key);
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
                    if (!state) return true;
                    if (state.token !== arguments[0]) return false;
                    for (let i = state.hidden.length - 1; i >= 0; i--) {
                      const item = state.hidden[i];
                      if (!item.element || !item.element.style) continue;
                      if (item.value) item.element.style.setProperty('visibility', item.value, item.priority || '');
                      else item.element.style.removeProperty('visibility');
                    }
                    if (state.sheet && 'adoptedStyleSheets' in document) {
                      document.adoptedStyleSheets = document.adoptedStyleSheets.filter(sheet => sheet !== state.sheet);
                    }
                    if (state.style && state.style.isConnected) state.style.remove();
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

    private static void installOverlaySnapshot(JavascriptExecutor js, String key) {
        js.executeScript("""
                const key = arguments[0];
                const host = document.getElementById('selenium-overlay-host');
                const state = {token: key, host, snapshot: null, visibility: '', visibilityPriority: ''};
                window[key] = state;
                if (!host || !host.shadowRoot) return false;

                state.visibility = host.style.getPropertyValue('visibility');
                state.visibilityPriority = host.style.getPropertyPriority('visibility');
                const hostRect = host.getBoundingClientRect();
                const hostStyle = getComputedStyle(host);
                const snapshot = host.cloneNode(false);
                snapshot.removeAttribute('id');
                snapshot.setAttribute('data-test-lens-overlay-snapshot', key);
                snapshot.setAttribute('aria-hidden', 'true');
                snapshot.setAttribute('inert', '');
                const style = snapshot.style;
                style.setProperty('position', 'absolute', 'important');
                style.setProperty('left', `${window.scrollX + hostRect.left}px`, 'important');
                style.setProperty('top', `${window.scrollY + hostRect.top}px`, 'important');
                style.setProperty('width', `${window.innerWidth}px`, 'important');
                style.setProperty('height', `${window.innerHeight}px`, 'important');
                style.setProperty('box-sizing', 'border-box', 'important');
                style.setProperty('transform', 'translateZ(0)', 'important');
                style.setProperty('transform-origin', '0 0', 'important');
                style.setProperty('pointer-events', 'none', 'important');
                style.setProperty('overflow', 'hidden', 'important');
                style.setProperty('contain', 'layout paint style', 'important');
                style.setProperty('display', hostStyle.display, 'important');
                style.setProperty('visibility', hostStyle.visibility, 'important');

                const root = snapshot.attachShadow({mode: 'open'});
                for (const child of host.shadowRoot.childNodes) root.appendChild(child.cloneNode(true));
                const freeze = document.createElement('style');
                freeze.setAttribute('data-test-lens-overlay-snapshot-freeze', key);
                freeze.textContent = `*, *::before, *::after {
                  animation-play-state: paused !important; transition: none !important;
                  caret-color: transparent !important; scroll-behavior: auto !important;
                }`;
                root.appendChild(freeze);
                (document.body || document.documentElement).appendChild(snapshot);
                state.snapshot = snapshot;
                host.style.setProperty('visibility', 'hidden', 'important');
                return true;
                """, key);
    }

    private static void restoreOverlay(JavascriptExecutor js, String key, Throwable primary) {
        try {
            Object restored = js.executeScript("""
                    const state = window[arguments[0]];
                    if (!state) return true;
                    if (state.token !== arguments[0]) return false;
                    if (state.snapshot && state.snapshot.isConnected) state.snapshot.remove();
                    if (state.host && state.host.style) {
                      if (state.visibility) state.host.style.setProperty(
                        'visibility', state.visibility, state.visibilityPriority || '');
                      else state.host.style.removeProperty('visibility');
                    }
                    delete window[arguments[0]];
                    return true;
                    """, key);
            if (!Boolean.TRUE.equals(restored)) {
                throw new IllegalStateException("Test Lens overlay snapshot state could not be restored");
            }
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

    private static int geometryObservationLimit(int maxTileCount) {
        int largestSafe = (Integer.MAX_VALUE - GEOMETRY_OBSERVATION_LIMIT_PADDING) / 2;
        return maxTileCount > largestSafe ? Integer.MAX_VALUE
                : maxTileCount * 2 + GEOMETRY_OBSERVATION_LIMIT_PADDING;
    }

    private static void validateCssDimensions(PageSnapshot page) {
        if (page.documentWidth() < 1 || page.documentHeight() < 1
                || page.viewportWidth() < 1 || page.viewportHeight() < 1) {
            throw new IllegalArgumentException("Document and viewport dimensions must be positive");
        }
    }

    private static BufferedImage resizeCanvas(BufferedImage source, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = resized.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            int copyWidth = Math.min(source.getWidth(), width);
            int copyHeight = Math.min(source.getHeight(), height);
            graphics.drawImage(source, 0, 0, copyWidth, copyHeight,
                    0, 0, copyWidth, copyHeight, null);
        } finally {
            graphics.dispose();
        }
        return resized;
    }

    private static void requireStableGeometry(PageSnapshot initial, PageSnapshot current, int attempt, int tile,
                                              Position requested, CaptureExtent extent) {
        List<String> changes = new ArrayList<>();
        if (!current.topLevel() || !initial.contextToken().equals(current.contextToken())) {
            changes.add("top-level browsing context identity");
        }
        if (initial.documentWidth() != current.documentWidth()) changes.add("documentWidth");
        if (initial.viewportWidth() != current.viewportWidth()) changes.add("viewportWidth");
        if (initial.viewportHeight() != current.viewportHeight()) changes.add("viewportHeight");
        if (Math.abs(initial.devicePixelRatio() - current.devicePixelRatio()) > 0.000001) {
            changes.add("devicePixelRatio");
        }
        if (!changes.isEmpty()) {
            throw viewportGeometryChanged(initial, current, extent, attempt, tile, requested,
                    "hard invariant(s) changed: " + String.join(", ", changes));
        }
    }

    private static void validateCaptureExtent(PageSnapshot initial, CaptureExtent extent,
                                              ScreenshotCaptureOptions options, long horizontalTiles,
                                              double scaleX, double scaleY, int attempt, int tile,
                                              Position requested, PageSnapshot latest) {
        long requestedTiles;
        long cssPixels;
        try {
            requestedTiles = Math.multiplyExact((long) horizontalTiles,
                    tileCount(extent.currentHeight(), initial.viewportHeight()));
            cssPixels = Math.multiplyExact(initial.documentWidth(), extent.currentHeight());
        } catch (ArithmeticException overflow) {
            throw captureHeightLimitExceeded(initial, latest, extent, attempt, tile, requested,
                    "geometry arithmetic overflow", overflow);
        }
        if (requestedTiles > options.maxTileCount()) {
            throw captureHeightLimitExceeded(initial, latest, extent, attempt, tile, requested,
                    "requiredTileCount=" + requestedTiles + " exceeds maxTileCount=" + options.maxTileCount(), null);
        }
        if (cssPixels > options.maxPixelCount()) {
            throw captureHeightLimitExceeded(initial, latest, extent, attempt, tile, requested,
                    "CSS pixel count=" + cssPixels + " exceeds maxPixelCount=" + options.maxPixelCount(), null);
        }
        if (scaleX > 0 && scaleY > 0) {
            int outputWidth = scaledDimension(initial.documentWidth(), scaleX, "width");
            int outputHeight = scaledDimension(extent.currentHeight(), scaleY, "height");
            long outputPixels = Math.multiplyExact((long) outputWidth, outputHeight);
            if (outputPixels > options.maxPixelCount()) {
                throw captureHeightLimitExceeded(initial, latest, extent, attempt, tile, requested,
                        "stitched pixel count=" + outputPixels + " exceeds maxPixelCount="
                                + options.maxPixelCount(), null);
            }
        }
    }

    private static ViewportGeometryChangedException viewportGeometryChanged(
            PageSnapshot initial, PageSnapshot latest, CaptureExtent extent, int attempt, int tile,
            Position requested, String reason) {
        return new ViewportGeometryChangedException("VIEWPORT_GEOMETRY_CHANGED: " + reason + "; "
                + diagnostics(initial, latest, extent, attempt, tile, requested));
    }

    private static DocumentHeightDidNotStabilizeException documentHeightDidNotStabilize(
            PageSnapshot initial, PageSnapshot latest, CaptureExtent extent, int attempt, int tile,
            Position requested, String reason) {
        return new DocumentHeightDidNotStabilizeException("DOCUMENT_HEIGHT_DID_NOT_STABILIZE: " + reason
                + "; maxDocumentHeightExpansions=" + MAX_DOCUMENT_HEIGHT_EXPANSIONS + "; "
                + diagnostics(initial, latest, extent, attempt, tile, requested));
    }

    private static CaptureHeightLimitException captureHeightLimitExceeded(
            PageSnapshot initial, PageSnapshot latest, CaptureExtent extent, int attempt, int tile,
            Position requested, String reason, Throwable cause) {
        return new CaptureHeightLimitException("CAPTURE_HEIGHT_LIMIT_EXCEEDED: " + reason + "; "
                + diagnostics(initial, latest, extent, attempt, tile, requested), cause);
    }

    private static TileCaptureException tileCaptureFailed(
            PageSnapshot initial, PageSnapshot latest, CaptureExtent extent, int attempt, int tile,
            Position requested, Throwable cause) {
        return new TileCaptureException("TILE_CAPTURE_FAILED: " + messageFor(cause) + "; "
                + diagnostics(initial, latest, extent, attempt, tile, requested), cause);
    }

    private static String diagnostics(PageSnapshot initial, PageSnapshot latest, CaptureExtent extent,
                                      int attempt, int tile, Position requested) {
        return "attempt=" + attempt + ", tile=" + tile + ", requestedScroll=" + requested
                + ", actualScroll=" + new Position(latest.scrollX(), latest.scrollY())
                + ", documentHeightExpansions=" + extent.expansions()
                + ", geometryObservations=" + extent.observations()
                + ", initialDocumentHeight=" + extent.initialHeight()
                + ", maxObservedDocumentHeight=" + extent.maxObservedHeight()
                + ", initialGeometry=" + initial.geometryDescription()
                + ", latestGeometry=" + latest.geometryDescription();
    }

    private static PageSnapshot pageSnapshot(Object value) {
        if (!(value instanceof Map<?, ?> map)) throw new IllegalStateException("Invalid page geometry response");
        return new PageSnapshot(number(map, "documentWidth"), number(map, "documentHeight"),
                number(map, "viewportWidth"), number(map, "viewportHeight"),
                number(map, "scrollX"), number(map, "scrollY"), decimal(map, "devicePixelRatio"),
                Boolean.TRUE.equals(map.get("topLevel")), string(map, "contextToken"));
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

    private static double decimal(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue() <= 0) {
            throw new IllegalStateException("Invalid page geometry: " + key);
        }
        return number.doubleValue();
    }

    private static String string(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof String string)) throw new IllegalStateException("Missing page geometry: " + key);
        return string;
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
                                long scrollX, long scrollY, double devicePixelRatio, boolean topLevel,
                                String contextToken) {
        private String geometryDescription() {
            return "{documentWidth=" + documentWidth + ", documentHeight=" + documentHeight
                    + ", viewportWidth=" + viewportWidth + ", viewportHeight=" + viewportHeight
                    + ", scrollX=" + scrollX + ", scrollY=" + scrollY
                    + ", devicePixelRatio=" + devicePixelRatio + ", topLevel=" + topLevel + "}";
        }
    }
    private record Position(long x, long y) { }
    private static final class CaptureExtent {
        private final PageSnapshot initialSnapshot;
        private final long initialHeight;
        private long currentHeight;
        private long maxObservedHeight;
        private int expansions;
        private final int maxObservations;
        private int observations;

        private CaptureExtent(PageSnapshot initialSnapshot, int maxObservations) {
            this.initialSnapshot = initialSnapshot;
            this.initialHeight = initialSnapshot.documentHeight();
            this.currentHeight = initialHeight;
            this.maxObservedHeight = initialHeight;
            this.maxObservations = maxObservations;
        }

        private boolean observe(long height, int attempt, int tile, Position requested, PageSnapshot latest) {
            observations++;
            if (observations > maxObservations) {
                throw documentHeightDidNotStabilize(initialSnapshot, latest, this, attempt, tile, requested,
                        "geometry did not stabilize within maxGeometryObservations=" + maxObservations);
            }
            boolean changed = height != currentHeight;
            currentHeight = height;
            if (height > maxObservedHeight) {
                maxObservedHeight = height;
                expansions++;
                if (expansions > MAX_DOCUMENT_HEIGHT_EXPANSIONS) {
                    throw documentHeightDidNotStabilize(initialSnapshot, latest, this, attempt, tile, requested,
                            "document height kept growing from " + initialHeight + " to " + maxObservedHeight
                                    + " across " + expansions + " expansions");
                }
            }
            return changed;
        }

        private long initialHeight() { return initialHeight; }
        private long currentHeight() { return currentHeight; }
        private long maxObservedHeight() { return maxObservedHeight; }
        private int expansions() { return expansions; }
        private int observations() { return observations; }
    }
    private static final class UnsupportedContextException extends RuntimeException {
        private UnsupportedContextException(String message) { super(message); }
    }
    private static final class ViewportGeometryChangedException extends IllegalStateException {
        private ViewportGeometryChangedException(String message) { super(message); }
        private ViewportGeometryChangedException(String message, Throwable cause) { super(message, cause); }
    }
    private static final class DocumentHeightDidNotStabilizeException extends IllegalStateException {
        private DocumentHeightDidNotStabilizeException(String message) { super(message); }
    }
    private static final class CaptureHeightLimitException extends IllegalArgumentException {
        private CaptureHeightLimitException(String message, Throwable cause) { super(message, cause); }
    }
    private static final class TileCaptureException extends IllegalStateException {
        private TileCaptureException(String message, Throwable cause) { super(message, cause); }
    }
}
