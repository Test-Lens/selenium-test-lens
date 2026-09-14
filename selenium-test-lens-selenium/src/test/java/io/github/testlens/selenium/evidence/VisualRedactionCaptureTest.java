package io.github.testlens.selenium.evidence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualRedactionCaptureTest {
    @TempDir Path tempDir;

    @Test
    void strictMissingLocatorRefusesScreenshotPublication() {
        MaskDriver driver = new MaskDriver(false);
        VisualRedactionOptions masks = VisualRedactionOptions.builder()
                .maskPasswordInputs(false).mask(By.id("missing"), VisualMaskMode.SOLID)
                .failurePolicy(VisualRedactionFailurePolicy.STRICT).build();

        ScreenshotCaptureResult result = capture(driver, masks, "strict");

        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertTrue(result.exception() instanceof VisualRedactionException);
        assertEquals(0, driver.screenshotCalls);
        assertFalse(Files.exists(tempDir.resolve("shot_strict.png")));
    }

    @Test
    void bestEffortMissingLocatorCapturesAndReportsWarning() {
        MaskDriver driver = new MaskDriver(false);
        VisualRedactionOptions masks = VisualRedactionOptions.builder()
                .maskPasswordInputs(false).mask(By.id("missing"), VisualMaskMode.SOLID)
                .failurePolicy(VisualRedactionFailurePolicy.BEST_EFFORT).build();

        ScreenshotCaptureResult result = capture(driver, masks, "best-effort");

        assertTrue(result.isCaptured(), result.message());
        assertTrue(result.message().contains("requested=1"));
        assertTrue(result.message().contains("TARGET_MISSING=1"));
        assertEquals(1, driver.screenshotCalls);
        assertEquals(0, driver.applyCalls);
        assertEquals(0, driver.removeCalls);
    }

    @Test
    void maskCleanupRunsAfterSuccessfulAndFailedScreenshot() {
        MaskDriver success = new MaskDriver(false, false, true);
        assertTrue(capture(success, VisualRedactionOptions.defaults(), "success").isCaptured());
        assertEquals(1, success.applyCalls);
        assertEquals(1, success.removeCalls);
        assertEquals(1, success.typographyInstallCalls);

        MaskDriver failure = new MaskDriver(true, false, true);
        ScreenshotCaptureResult result = capture(failure, VisualRedactionOptions.defaults(), "failure");
        assertEquals(ScreenshotCaptureStatus.FAILED, result.status());
        assertEquals(1, failure.applyCalls);
        assertEquals(1, failure.removeCalls);
    }

    @Test
    void staleDynamicTargetIsReportedInBestEffortAndBlocksStrict() {
        MaskDriver bestEffort = new MaskDriver(false, true);
        VisualRedactionOptions bestEffortMasks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.id("dynamic"), VisualMaskMode.SOLID)
                .failurePolicy(VisualRedactionFailurePolicy.BEST_EFFORT).build();
        ScreenshotCaptureResult captured = capture(bestEffort, bestEffortMasks, "stale-best-effort");
        assertTrue(captured.isCaptured());
        assertTrue(captured.message().contains("TARGET_DISCONNECTED"));

        MaskDriver strict = new MaskDriver(false, true);
        VisualRedactionOptions strictMasks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.id("dynamic"), VisualMaskMode.SOLID)
                .failurePolicy(VisualRedactionFailurePolicy.STRICT).build();
        ScreenshotCaptureResult blocked = capture(strict, strictMasks, "stale-strict");
        assertFalse(blocked.isCaptured());
        assertEquals(0, strict.screenshotCalls);
    }

    @Test
    void unsupportedBlurUsesDocumentedSolidFallback() {
        MaskDriver driver = new MaskDriver(false, false, true);
        VisualRedactionOptions masks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.id("email"), VisualMaskMode.BLUR).build();
        ScreenshotCaptureResult result = capture(driver, masks, "blur-fallback");
        assertTrue(result.isCaptured());
        assertTrue(result.message().contains("safely fell back to SOLID"));
    }

    @Test
    void disabledVisualRedactionLeavesCapturePathUnchanged() {
        MaskDriver driver = new MaskDriver(false);
        ScreenshotCaptureResult result = capture(driver, VisualRedactionOptions.disabled(), "unchanged");
        assertTrue(result.isCaptured());
        assertEquals(0, driver.applyCalls);
        assertEquals(0, driver.removeCalls);
        assertEquals(1, driver.screenshotCalls);
    }

    @Test
    void failedBrowserConfirmationStillCleansUpAndStrictBlocksPublication() {
        MaskDriver driver = new MaskDriver(false, false, true, false);
        VisualRedactionOptions masks = VisualRedactionOptions.builder()
                .failurePolicy(VisualRedactionFailurePolicy.STRICT).build();
        ScreenshotCaptureResult result = capture(driver, masks, "unconfirmed");
        assertFalse(result.isCaptured());
        assertEquals(0, driver.screenshotCalls);
        assertEquals(2, driver.applyCalls);
        assertEquals(2, driver.removeCalls);
    }

    @Test
    void installsMultipleMasksInOneBatchAndOneAsyncRoundtrip() {
        BatchDriver driver = new BatchDriver(50, List.of(replies(50, "VERIFIED")));
        VisualRedactionOptions masks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.cssSelector(".secret"), VisualMaskMode.SOLID).build();

        ScreenshotCaptureResult result = capture(driver, masks, "batch-50");

        assertTrue(result.isCaptured(), result.message());
        assertEquals(1, driver.applyCalls);
        assertEquals(List.of(50), driver.batchSizes);
        assertEquals(1, driver.removeCalls);
        assertEquals(0, driver.remainingMaskNodes);
    }

    @Test
    void typographyInstallFailureDoesNotBlockVerifiedRedaction() {
        BatchDriver driver = new BatchDriver(1, List.of(replies(1, "VERIFIED")));
        driver.failTypographyInstall = true;
        VisualRedactionOptions masks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.id("secret"), VisualMaskMode.SOLID).maskLabel("REDACTED").build();

        ScreenshotCaptureResult result = capture(driver, masks, "font-failure");

        assertTrue(result.isCaptured(), result.message());
        assertEquals(1, driver.applyCalls);
        assertEquals(1, driver.removeCalls);
    }

    @Test
    void geometryMismatchRetriesTheWholeBatchAndCleansOldBatch() {
        BatchDriver driver = new BatchDriver(2, List.of(
                replies("VERIFIED", "RECT_MISMATCH"), replies(2, "VERIFIED")));
        VisualRedactionOptions masks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.cssSelector(".secret"), VisualMaskMode.SOLID)
                .failurePolicy(VisualRedactionFailurePolicy.STRICT).build();

        ScreenshotCaptureResult result = capture(driver, masks, "retry-success");

        assertTrue(result.isCaptured(), result.message());
        assertEquals(2, driver.applyCalls);
        assertEquals(2, driver.resolveCalls);
        assertEquals(List.of(2, 2), driver.batchSizes);
        assertEquals(2, driver.removeCalls);
        assertEquals(0, driver.remainingMaskNodes);
        assertEquals(driver.batchIds.get(0), driver.batchIds.get(1));
    }

    @Test
    void retryExhaustionIsDiagnosticInBestEffortAndFailClosedInStrict() {
        BatchDriver best = new BatchDriver(1, List.of(replies(1, "RECT_MISMATCH"), replies(1, "RECT_MISMATCH")));
        VisualRedactionOptions bestMasks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.id("secret"), VisualMaskMode.SOLID)
                .failurePolicy(VisualRedactionFailurePolicy.BEST_EFFORT).build();
        ScreenshotCaptureResult captured = capture(best, bestMasks, "retry-best");
        assertTrue(captured.isCaptured(), captured.message());
        assertTrue(captured.message().contains("requested=1, installed=1, verified=0, failed=1"));
        assertTrue(captured.message().contains("RECT_MISMATCH=1"));
        assertEquals(2, best.applyCalls);

        BatchDriver strict = new BatchDriver(1, List.of(
                replies(1, "TARGET_DISCONNECTED"), replies(1, "TARGET_DISCONNECTED")));
        VisualRedactionOptions strictMasks = VisualRedactionOptions.builder().maskPasswordInputs(false)
                .mask(By.id("secret"), VisualMaskMode.SOLID)
                .failurePolicy(VisualRedactionFailurePolicy.STRICT).build();
        ScreenshotCaptureResult blocked = capture(strict, strictMasks, "retry-strict");
        assertFalse(blocked.isCaptured());
        assertEquals(0, strict.screenshotCalls);
        assertEquals(2, strict.applyCalls);
        assertEquals(2, strict.removeCalls);
        assertEquals(0, strict.remainingMaskNodes);
    }

    private static Map<String, Object> replies(int count, String status) {
        String[] statuses = new String[count];
        java.util.Arrays.fill(statuses, status);
        return replies(statuses);
    }

    private static Map<String, Object> replies(String... categories) {
        List<Map<String, Object>> statuses = new ArrayList<>();
        int verified = 0;
        for (int index = 0; index < categories.length; index++) {
            statuses.add(Map.of("index", index, "status", categories[index]));
            if ("VERIFIED".equals(categories[index])) verified++;
        }
        return Map.of("installed", categories.length, "verified", verified,
                "statuses", statuses, "blurFallback", false);
    }

    private ScreenshotCaptureResult capture(WebDriver driver, VisualRedactionOptions masks, String name) {
        return new ScreenshotCapture(driver, masks).capture(name, ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir).fileNamePrefix("shot").includeTimestamp(false).build());
    }

    private static final class MaskDriver implements WebDriver, JavascriptExecutor, TakesScreenshot {
        private final boolean failScreenshot;
        private final boolean staleApply;
        private final boolean blurFallback;
        private final boolean confirmed;
        int applyCalls, removeCalls, screenshotCalls, typographyInstallCalls;
        boolean typographyInstalled, failTypographyInstall;
        MaskDriver(boolean failScreenshot) { this(failScreenshot, false, false, true); }
        MaskDriver(boolean failScreenshot, boolean staleApply) {
            this(failScreenshot, staleApply, false, true);
        }
        MaskDriver(boolean failScreenshot, boolean staleApply, boolean blurFallback) {
            this(failScreenshot, staleApply, blurFallback, true);
        }
        MaskDriver(boolean failScreenshot, boolean staleApply, boolean blurFallback, boolean confirmed) {
            this.failScreenshot = failScreenshot;
            this.staleApply = staleApply;
            this.blurFallback = blurFallback;
            this.confirmed = confirmed;
        }
        @Override public List<WebElement> findElements(By by) {
            if (!staleApply && !blurFallback) return List.of();
            WebElement element = (WebElement) Proxy.newProxyInstance(WebElement.class.getClassLoader(),
                    new Class<?>[]{WebElement.class}, (proxy, method, args) -> null);
            return List.of(element);
        }
        @Override public Object executeAsyncScript(String script, Object... args) {
            applyCalls++;
            assertTrue(script.contains("pointer-events"));
            assertTrue(script.contains("mask.textContent = label"));
            assertTrue(script.contains("getBoundingClientRect"));
            assertTrue(script.contains("RECT_MISMATCH"));
            assertFalse(script.contains(".value ="));
            assertFalse(script.contains(".focus("));
            if (staleApply) throw new StaleElementReferenceException("dynamic target replaced");
            int count = ((List<?>) args[2]).size();
            String status = confirmed ? "VERIFIED" : "INSTALL_FAILED";
            List<Map<String, Object>> statuses = java.util.stream.IntStream.range(0, count)
                    .mapToObj(index -> Map.<String, Object>of("index", index, "status", status)).toList();
            return Map.of("batchId", args[0], "installed", confirmed ? count : 0,
                    "verified", confirmed ? count : 0, "statuses", statuses, "blurFallback", blurFallback);
        }
        @Override public Object executeScript(String script, Object... args) {
            if (script.contains("modules.visualTypography") && script.contains("return !!")) {
                return typographyInstalled;
            }
            if (script.contains("installBase64")) {
                if (failTypographyInstall) throw new IllegalStateException("controlled font install failure");
                typographyInstalled = true;
                typographyInstallCalls++;
                return null;
            }
            removeCalls++;
            return true;
        }
        @Override public <X> X getScreenshotAs(OutputType<X> target) {
            screenshotCalls++;
            if (failScreenshot) throw new IllegalStateException("controlled screenshot failure");
            try {
                BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(image, "png", bytes);
                return target.convertFromPngBytes(bytes.toByteArray());
            } catch (Exception failure) { throw new RuntimeException(failure); }
        }
        @Override public void get(String url) { }
        @Override public String getCurrentUrl() { return ""; }
        @Override public String getTitle() { return ""; }
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

    private static final class BatchDriver implements WebDriver, JavascriptExecutor, TakesScreenshot {
        private final int elementCount;
        private final Deque<Map<String, Object>> replies;
        private final List<WebElement> elements;
        final List<Integer> batchSizes = new ArrayList<>();
        final List<String> batchIds = new ArrayList<>();
        int applyCalls, removeCalls, resolveCalls, screenshotCalls, remainingMaskNodes, typographyInstallCalls;
        boolean typographyInstalled, failTypographyInstall;

        BatchDriver(int elementCount, List<Map<String, Object>> replies) {
            this.elementCount = elementCount;
            this.replies = new ArrayDeque<>(replies);
            List<WebElement> targets = new ArrayList<>();
            for (int i = 0; i < elementCount; i++) {
                targets.add((WebElement) Proxy.newProxyInstance(WebElement.class.getClassLoader(),
                        new Class<?>[]{WebElement.class}, (proxy, method, args) -> null));
            }
            elements = List.copyOf(targets);
        }

        @Override public List<WebElement> findElements(By by) { resolveCalls++; return elements; }
        @Override public Object executeAsyncScript(String script, Object... args) {
            applyCalls++;
            List<?> targets = (List<?>) args[2];
            assertEquals(elementCount, targets.size());
            assertEquals(elementCount, ((List<?>) args[3]).size());
            batchSizes.add(targets.size());
            batchIds.add(String.valueOf(args[0]));
            remainingMaskNodes = elementCount;
            Map<String, Object> reply = new java.util.HashMap<>(replies.removeFirst());
            reply.put("batchId", args[0]);
            return reply;
        }
        @Override public Object executeScript(String script, Object... args) {
            if (script.contains("modules.visualTypography") && script.contains("return !!")) {
                return typographyInstalled;
            }
            if (script.contains("installBase64")) {
                if (failTypographyInstall) throw new IllegalStateException("controlled font install failure");
                typographyInstalled = true;
                typographyInstallCalls++;
                return null;
            }
            removeCalls++;
            remainingMaskNodes = 0;
            return true;
        }
        @Override public <X> X getScreenshotAs(OutputType<X> target) {
            screenshotCalls++;
            try {
                BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageIO.write(image, "png", bytes);
                return target.convertFromPngBytes(bytes.toByteArray());
            } catch (Exception failure) { throw new RuntimeException(failure); }
        }
        @Override public void get(String url) { }
        @Override public String getCurrentUrl() { return ""; }
        @Override public String getTitle() { return ""; }
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
