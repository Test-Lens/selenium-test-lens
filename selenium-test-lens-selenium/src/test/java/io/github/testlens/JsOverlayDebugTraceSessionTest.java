package io.github.testlens;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.TraceArtifactType;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.evidence.ScreenshotCaptureOptions;
import io.github.testlens.selenium.evidence.ScreenshotCaptureResult;
import io.github.testlens.selenium.evidence.ScreenshotCaptureStatus;
import io.github.testlens.selenium.evidence.VideoEvidenceOptions;
import io.github.testlens.selenium.evidence.VideoEvidenceResult;
import io.github.testlens.selenium.evidence.VideoEvidenceSource;
import io.github.testlens.selenium.evidence.VideoEvidenceStatus;
import io.github.testlens.selenium.steps.UiStepOptions;
import io.github.testlens.selenium.steps.UiStepResult;
import io.github.testlens.selenium.steps.UiStepStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsOverlayDebugTraceSessionTest {

    @Test
    void directlyCreatedFacadeUsesSafeDefaultRedaction() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        UiTestLensSession session = overlay.startSession("token=direct-canary");

        assertFalse(session.metadata().name().contains("direct-canary"));
        assertTrue(session.metadata().name().contains("[REDACTED]"));
    }

    @Test
    void apiOverlayReceivesOnlyRedactedArguments() {
        String secret = "api-overlay-canary-91f4";
        List<Object[]> calls = new ArrayList<>();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName())) {
                        calls.add(args == null ? new Object[0] : args.clone());
                        return "request-id";
                    }
                    if ("toString".equals(method.getName())) return "api-redaction-driver";
                    return null;
                });
        JsOverlayDebug overlay = new JsOverlayDebug(driver);

        overlay.apiShowRequest("token=" + secret, "POST",
                "https://user:" + secret + "@example.test/orders?access_token=" + secret + "#fragment",
                "{\"client_secret\":\"" + secret + "\"}");
        overlay.apiSetResponse("request-id", 200, 1,
                "Authorization: Bearer " + secret, "refresh_token=" + secret);
        overlay.setStep("token=" + secret);
        overlay.hudLog("info", "password=" + secret, "now");

        String diagnosticArguments = java.util.Arrays.deepToString(calls.toArray());
        assertFalse(diagnosticArguments.contains(secret));
        assertTrue(diagnosticArguments.contains("[REDACTED]"));
    }
    @TempDir
    Path tempDir;

    @Test
    void canAttachAndStartSession() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        UiTestLensSession attached = UiTestLensSession.start("Attached");

        overlay.attachSession(attached);

        assertSame(attached, overlay.session().orElseThrow());

        UiTestLensSession started = overlay.startSession("Started");

        assertSame(started, overlay.session().orElseThrow());
        assertEquals("Started", started.metadata().name());
    }

    @Test
    void stepAddsTraceEventsWhenSessionIsAttached() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        UiTestLensSession session = overlay.startSession("Checkout flow");

        overlay.step("Save form", () -> {});

        assertEquals(1, countEvents(session, TraceEventType.STEP_STARTED));
        assertEquals(1, countEvents(session, TraceEventType.STEP_PASSED));
    }

    @Test
    void loggerEventsAreForwardedToAttachedSession() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        UiTestLensSession session = UiTestLensSession.start("Checkout flow");
        overlay.attachSession(session);

        overlay.setStep("Open checkout");

        assertTrue(session.events().stream().anyMatch(event ->
                "STEP".equals(event.attributes().get("uiEventType"))
                        && "Open checkout".equals(event.attributes().get("step"))));
    }

    @Test
    void attachScreenshotDelegatesToSession() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        UiTestLensSession session = overlay.startSession("Checkout flow");

        TraceArtifact artifact = overlay.attachScreenshot("Save form", Path.of("target/screenshots/save.png"));

        assertEquals(TraceArtifactType.SCREENSHOT, artifact.type());
        assertEquals(1, session.artifacts().size());
    }

    @Test
    void attachArtifactRequiresSession() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());

        assertThrows(IllegalStateException.class,
                () -> overlay.attachVideo("Video", Path.of("target/videos/test.mp4")));
    }

    @Test
    void attachVideoDelegatesToVideoEvidenceAndSession() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        UiTestLensSession session = overlay.startSession("Checkout flow");

        TraceArtifact artifact = overlay.attachVideo("Video", Path.of("target/videos/test.mp4"));

        assertEquals(TraceArtifactType.VIDEO, artifact.type());
        assertEquals(1, session.artifacts().size());
        assertEquals("CUSTOM", artifact.metadata().get("video.source"));
    }

    @Test
    void attachVideoUrlAddsCiArtifactMetadata() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        UiTestLensSession session = overlay.startSession("Checkout flow");

        VideoEvidenceResult result = overlay.attachVideoUrl(
                "CI video",
                "https://ci.example.com/artifacts/checkout.mp4?signed=sample",
                VideoEvidenceOptions.builder()
                        .source(VideoEvidenceSource.CI_ARTIFACT)
                        .metadata("job", "checkout-ui-tests")
                        .build()
        );

        assertEquals(VideoEvidenceStatus.ATTACHED, result.status());
        assertEquals(1, session.artifacts().size());
        assertEquals("checkout-ui-tests", session.artifacts().get(0).metadata().get("job"));
    }

    @Test
    void attachVideoFileWithoutSessionReturnsSkippedResult() {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());

        VideoEvidenceResult result = overlay.attachVideoFile("Video", Path.of("target/videos/test.mp4"));

        assertEquals(VideoEvidenceStatus.SKIPPED, result.status());
    }

    @Test
    void exportsTraceHtmlWhenSessionIsAttached() throws Exception {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        overlay.startSession("Checkout flow");
        overlay.step("Save form", () -> {});

        String html = overlay.exportTraceHtml();
        Path report = overlay.exportTraceHtml(tempDir.resolve("trace.html"));

        assertTrue(html.contains("Checkout flow"));
        assertTrue(Files.readString(report).contains("Checkout flow"));
    }

    @Test
    void captureScreenshotWritesFileAndAttachesToSession() throws Exception {
        Path source = tempDir.resolve("source.png");
        Files.writeString(source, "png");
        JsOverlayDebug overlay = new JsOverlayDebug(fakeScreenshotDriver(source));
        UiTestLensSession session = overlay.startSession("Checkout flow");

        ScreenshotCaptureResult result = overlay.captureScreenshot("After save", ScreenshotCaptureOptions.builder()
                .outputDirectory(tempDir.resolve("screens"))
                .includeTimestamp(false)
                .build());

        assertEquals(ScreenshotCaptureStatus.CAPTURED, result.status());
        assertTrue(Files.exists(result.path()));
        assertEquals(1, session.artifacts().size());
    }

    @Test
    void failedStepCanCaptureScreenshotWhenEnabled() throws Exception {
        Path source = tempDir.resolve("source.png");
        Files.writeString(source, "png");
        JsOverlayDebug overlay = new JsOverlayDebug(fakeScreenshotDriver(source));
        UiTestLensSession session = overlay.startSession("Checkout flow");
        UiStepOptions options = UiStepOptions.builder()
                .failFast(false)
                .captureScreenshotOnFailure(true)
                .screenshotCaptureOptions(ScreenshotCaptureOptions.builder()
                        .outputDirectory(tempDir.resolve("failed"))
                        .includeTimestamp(false)
                        .build())
                .build();

        UiStepResult result = overlay.step("Verify order summary", options, () -> {
            throw new AssertionError("bad total");
        });

        assertEquals(UiStepStatus.FAILED, result.status());
        assertEquals(1, session.artifacts().size());
        assertTrue(Files.exists(Path.of(session.artifacts().get(0).path())));
    }

    private static WebDriver fakeDriver() {
        return (WebDriver) Proxy.newProxyInstance(
                JsOverlayDebugTraceSessionTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName()) || "executeAsyncScript".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "fake-driver";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class || returnType == long.class || returnType == short.class || returnType == byte.class) {
                        return 0;
                    }
                    if (returnType == double.class || returnType == float.class) {
                        return 0.0;
                    }
                    if (returnType == char.class) {
                        return '\0';
                    }
                    return null;
                }
        );
    }

    private static WebDriver fakeScreenshotDriver(Path source) {
        return (WebDriver) Proxy.newProxyInstance(
                JsOverlayDebugTraceSessionTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class, TakesScreenshot.class},
                (proxy, method, args) -> {
                    if ("getScreenshotAs".equals(method.getName())) {
                        @SuppressWarnings("unchecked")
                        OutputType<File> outputType = (OutputType<File>) args[0];
                        return outputType.convertFromPngBytes(Files.readAllBytes(source));
                    }
                    if ("executeScript".equals(method.getName()) || "executeAsyncScript".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "fake-screenshot-driver";
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class || returnType == long.class || returnType == short.class || returnType == byte.class) {
                        return 0;
                    }
                    if (returnType == double.class || returnType == float.class) {
                        return 0.0;
                    }
                    if (returnType == char.class) {
                        return '\0';
                    }
                    return null;
                }
        );
    }

    private static long countEvents(UiTestLensSession session, TraceEventType type) {
        return session.events().stream().filter(event -> event.type() == type).count();
    }
}

