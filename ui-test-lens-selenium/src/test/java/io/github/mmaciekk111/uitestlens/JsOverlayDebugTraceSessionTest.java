package io.github.mmaciekk111.uitestlens;

import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifact;
import io.github.mmaciekk111.uitestlens.core.trace.TraceArtifactType;
import io.github.mmaciekk111.uitestlens.core.trace.TraceEventType;
import io.github.mmaciekk111.uitestlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsOverlayDebugTraceSessionTest {
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

        assertTrue(session.events().stream().anyMatch(event -> event.type() == TraceEventType.STEP_STARTED));
        assertTrue(session.events().stream().anyMatch(event -> event.type() == TraceEventType.STEP_PASSED));
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
    void exportsTraceHtmlWhenSessionIsAttached() throws Exception {
        JsOverlayDebug overlay = new JsOverlayDebug(fakeDriver());
        overlay.startSession("Checkout flow");
        overlay.step("Save form", () -> {});

        String html = overlay.exportTraceHtml();
        Path report = overlay.exportTraceHtml(tempDir.resolve("trace.html"));

        assertTrue(html.contains("Checkout flow"));
        assertTrue(Files.readString(report).contains("Checkout flow"));
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
}
