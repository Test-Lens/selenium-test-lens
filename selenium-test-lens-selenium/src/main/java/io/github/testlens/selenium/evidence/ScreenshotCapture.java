package io.github.testlens.selenium.evidence;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.UiTestLensSession;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;

public final class ScreenshotCapture {
    private final WebDriver driver;

    public ScreenshotCapture(WebDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        this.driver = driver;
    }

    public ScreenshotCaptureResult capture(String name, ScreenshotCaptureOptions options) {
        return capture(name, options, null);
    }

    public ScreenshotCaptureResult capture(String name, ScreenshotCaptureOptions options, UiTestLensSession session) {
        ScreenshotCaptureOptions effectiveOptions = options == null ? ScreenshotCaptureOptions.defaults() : options;
        String effectiveName = name == null || name.isBlank() ? "Screenshot" : name.trim();
        if (!(driver instanceof TakesScreenshot takesScreenshot)) {
            return ScreenshotCaptureResult.failed(effectiveName, null, "WebDriver does not implement TakesScreenshot", null);
        }
        Path destination = null;
        try {
            Files.createDirectories(effectiveOptions.outputDirectory());
            destination = EvidencePathStrategy.screenshotPath(effectiveName, effectiveOptions);
            Path source = takesScreenshot.getScreenshotAs(OutputType.FILE).toPath();
            if (effectiveOptions.overwriteExisting()) {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(source, destination);
            }
            TraceArtifact artifact = null;
            String message = "Screenshot captured";
            if (effectiveOptions.attachToSession()) {
                if (session != null) {
                    artifact = session.attachScreenshot(effectiveName, destination)
                            .withMetadata("capturedAt", Instant.now().toString());
                } else {
                    message = "Screenshot captured; no UiTestLensSession attached";
                }
            }
            return ScreenshotCaptureResult.captured(effectiveName, destination, artifact, message);
        } catch (IOException | RuntimeException e) {
            return ScreenshotCaptureResult.failed(effectiveName, destination, "Screenshot capture failed: " + messageFor(e), e);
        }
    }

    private static String messageFor(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }
}
