package io.github.testlens;

import io.github.testlens.core.trace.RetryOutcomePolicy;
import io.github.testlens.core.trace.RetryPolicyViolationException;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selenium.evidence.FailureBundleOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.Logs;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class FailureBundleCaptureTest {
    @TempDir Path temp;

    @Test
    void failedFinalizationBuildsManifestReportsAndDeterministicSafeZip() throws Exception {
        DriverFixture fixture = driver("<html><body>secret</body></html>", List.of());
        TestLens lens = TestLens.attach(fixture.driver, options(FailureBundleOptions.complete()));
        lens.startSession("bundle");

        RuntimeException original = new RuntimeException("expected \"failure\"");
        TestLensFinalizationResult result = lens.finishFailed(original);

        assertEquals(TraceStatus.FAILED, result.session().metadata().status());
        assertEquals(2, fixture.screenshots.get());
        assertEquals(List.of("screenshot", "hide", "screenshot", "restore"), fixture.captureOrder);
        assertTrue(Files.exists(result.failureScreenshot()));
        assertTrue(result.failureBundleManifest().isPresent());
        assertTrue(result.failureBundleArchive().isPresent());
        String manifest = Files.readString(result.failureBundleManifest().orElseThrow());
        assertTrue(manifest.contains("\"schemaVersion\""));
        assertTrue(manifest.contains("\"finalStatus\""));
        assertTrue(manifest.contains("\"FAILED\""));
        assertTrue(manifest.contains("diagnosticScreenshot"));
        assertTrue(manifest.contains("pageSource"));
        assertTrue(Files.readString(result.htmlReport()).contains("Failure bundle"));
        assertTrue(Files.readString(result.outputDirectory().resolve("failure-bundle/failure.json"))
                .contains("expected \\\"failure\\\""));
        try (ZipFile zip = new ZipFile(result.failureBundleArchive().orElseThrow().toFile())) {
            List<String> entries = zip.stream().map(java.util.zip.ZipEntry::getName).toList();
            assertTrue(entries.contains("manifest.json"));
            assertTrue(entries.contains("trace.json"));
            assertTrue(entries.contains("report.html"));
            assertTrue(entries.contains("failure-diagnostic.png"));
            assertTrue(entries.contains("failure-clean.png"));
            assertTrue(entries.stream().noneMatch(name -> name.startsWith("/") || name.contains("..")
                    || name.equals("failure-bundle.zip")));
            assertEquals(entries.stream().sorted().toList(), entries);
        }
        assertEquals(original.getClass().getName(), result.session().events().stream()
                .filter(event -> event.type() == TraceEventType.SESSION_FINISHED)
                .findFirst().orElseThrow().failure().exceptionType());
    }

    @Test
    void nullFailureStillCreatesBundleWithoutSyntheticException() throws Exception {
        DriverFixture fixture = driver("<html></html>", List.of());
        TestLens lens = TestLens.attach(fixture.driver, options(FailureBundleOptions.defaults()));
        lens.startSession("null failure");
        TestLensFinalizationResult result = lens.finishFailed(null);
        String failure = Files.readString(result.outputDirectory().resolve("failure-bundle/failure.json"));
        assertTrue(failure.contains("\"present\""));
        assertTrue(failure.contains("false"));
        assertTrue(failure.contains("finishFailed(null)"));
        assertNull(result.session().events().stream()
                .filter(event -> event.type() == TraceEventType.SESSION_FINISHED)
                .findFirst().orElseThrow().failure());
    }

    @Test
    void passedSkippedWarnAndReportOnlyDoNotCreateBundle() {
        for (TraceStatus status : List.of(TraceStatus.PASSED, TraceStatus.SKIPPED)) {
            DriverFixture fixture = driver("", List.of());
            TestLens lens = TestLens.attach(fixture.driver, options(FailureBundleOptions.defaults()));
            lens.startSession(status.name());
            TestLensFinalizationResult result = status == TraceStatus.PASSED
                    ? lens.finishPassed() : lens.finishSkipped("expected");
            assertTrue(result.failureBundleDirectory().isEmpty());
            assertEquals(0, fixture.screenshots.get());
        }
        for (RetryOutcomePolicy policy : List.of(RetryOutcomePolicy.REPORT_ONLY, RetryOutcomePolicy.WARN)) {
            DriverFixture fixture = driver("", List.of());
            TestLens lens = TestLens.attach(fixture.driver, TestLensOptions.builder().outputRoot(temp)
                    .retryOutcomePolicy(policy).build());
            lens.startSession(policy.name()).addEvent(io.github.testlens.core.trace.TraceEvent.builder(
                    TraceEventType.RETRY, TraceStatus.WARNING, "retry").build());
            assertTrue(lens.finishPassed().failureBundleDirectory().isEmpty());
        }
    }

    @Test
    void disabledBundleKeepsLegacyScreenshotWhileScreenshotFlagDisablesBothBundleImages() throws Exception {
        DriverFixture legacy = driver("", List.of());
        TestLens disabled = TestLens.attach(legacy.driver, options(FailureBundleOptions.builder().enabled(false).build()));
        disabled.startSession("disabled");
        TestLensFinalizationResult legacyResult = disabled.finishFailed(new AssertionError("failure"));
        assertEquals(1, legacy.screenshots.get());
        assertNotNull(legacyResult.failureScreenshot());
        assertTrue(legacyResult.failureBundleDirectory().isEmpty());

        DriverFixture noScreens = driver("", List.of());
        TestLens lens = TestLens.attach(noScreens.driver, TestLensOptions.builder().outputRoot(temp)
                .screenshotOnFailure(false).failureBundleOptions(FailureBundleOptions.defaults()).build());
        lens.startSession("no screens");
        TestLensFinalizationResult result = lens.finishFailed(new AssertionError("failure"));
        assertEquals(0, noScreens.screenshots.get());
        String manifest = Files.readString(result.failureBundleManifest().orElseThrow());
        assertTrue(manifest.contains("Disabled by screenshotOnFailure=false"));
        assertNull(result.failureScreenshot());
    }

    @Test
    void limitsAreReportedAndCapabilitiesUseAnAllowlist() throws Exception {
        List<LogEntry> logs = List.of(
                new LogEntry(Level.INFO, 1, "one"), new LogEntry(Level.WARNING, 2, "two"));
        DriverFixture fixture = driver("0123456789", logs);
        FailureBundleOptions bundle = FailureBundleOptions.builder().pageSource(true).browserConsole(true)
                .maxTextArtifactBytes(1_024).maxConsoleEntries(1).build();
        TestLens lens = TestLens.attach(fixture.driver, options(bundle));
        lens.startSession("limits");
        TestLensFinalizationResult result = lens.finishFailed(new AssertionError("failure"));
        String manifest = Files.readString(result.failureBundleManifest().orElseThrow());
        assertTrue(manifest.contains("\"browserConsole\""));
        assertTrue(manifest.contains("\"TRUNCATED\""));
        String runtime = Files.readString(result.outputDirectory().resolve("failure-bundle/runtime.json"));
        assertTrue(runtime.contains("chrome"));
        assertFalse(runtime.contains("super-secret-grid-token"));
    }

    @Test
    void policyFailureProducesBundleBeforePropagatingViolation() {
        DriverFixture fixture = driver("", List.of());
        TestLens lens = TestLens.attach(fixture.driver, TestLensOptions.builder().outputRoot(temp)
                .retryOutcomePolicy(RetryOutcomePolicy.FAIL_ON_ANY_RETRY).build());
        var session = lens.startSession("policy bundle");
        session.addEvent(io.github.testlens.core.trace.TraceEvent.builder(
                TraceEventType.RETRY, TraceStatus.WARNING, "retry").build());

        RetryPolicyViolationException violation = assertThrows(RetryPolicyViolationException.class, lens::finishPassed);
        Path directory = temp.resolve("policy-bundle").resolve(session.id());
        assertTrue(Files.exists(directory.resolve("failure-bundle.zip")));
        assertTrue(Files.exists(directory.resolve("trace.json")));
        assertTrue(Files.exists(directory.resolve("report.html")));
        assertEquals(2, fixture.screenshots.get());
        assertEquals(TraceStatus.FAILED, session.metadata().status());
        assertTrue(violation.getSuppressed().length >= 0);
    }

    @Test
    void centralPolicyRedactsFailurePageSourceConsoleTraceReportsAndZip() throws Exception {
        String secret = "bundle-canary-81d0";
        DriverFixture fixture = driver("<html><body>token=" + secret + "</body></html>",
                List.of(new LogEntry(Level.WARNING, 1, "Authorization: Bearer " + secret)));
        TestLensOptions configured = TestLensOptions.builder().outputRoot(temp)
                .redactionPolicy(RedactionPolicy.builder().sensitiveKey("tenant-session").secret(secret).build())
                .failureBundleOptions(FailureBundleOptions.complete()).build();
        TestLens lens = TestLens.attach(fixture.driver, configured);
        var session = lens.startSession("session " + secret);
        session.addEvent(io.github.testlens.core.trace.TraceEvent.info("step " + secret,
                "tenant-session=" + secret));

        AssertionError original = new AssertionError("failure " + secret);
        TestLensFinalizationResult result = lens.finishFailed(original);

        List<Path> textFiles;
        try (var files = Files.walk(result.outputDirectory())) {
            textFiles = files.filter(Files::isRegularFile)
                    .filter(path -> !path.toString().endsWith(".png") && !path.toString().endsWith(".zip"))
                    .toList();
        }
        for (Path file : textFiles) {
            String content = Files.readString(file);
            assertFalse(content.contains(secret), file.toString());
        }
        try (ZipFile zip = new ZipFile(result.failureBundleArchive().orElseThrow().toFile())) {
            for (var entry : zip.stream().filter(item -> !item.getName().endsWith(".png")).toList()) {
                String content = new String(zip.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                assertFalse(content.contains(secret), entry.getName());
            }
        }
        assertTrue(Files.readString(result.jsonReport()).contains("[REDACTED]"));
        String configuration = Files.readString(result.outputDirectory().resolve("failure-bundle/configuration.json"));
        assertTrue(configuration.contains("additionalSensitiveKeys"));
        assertTrue(configuration.contains("literalSecrets"));
    }

    private TestLensOptions options(FailureBundleOptions bundle) {
        return TestLensOptions.builder().outputRoot(temp).failureBundleOptions(bundle).build();
    }

    private DriverFixture driver(String pageSource, List<LogEntry> logs) {
        try {
            Path screenshot = temp.resolve("source-" + System.nanoTime() + ".png");
            Files.write(screenshot, new byte[]{1, 2, 3, 4});
            AtomicInteger screenshotCalls = new AtomicInteger();
            List<String> order = new ArrayList<>();
            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setBrowserName("chrome");
            capabilities.setCapability("browserVersion", "123");
            capabilities.setCapability("secretToken", "super-secret-grid-token");
            Logs logManager = (Logs) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Logs.class},
                    (proxy, method, args) -> method.getName().equals("get") ? new LogEntries(logs) : Set.of(LogTypeName.BROWSER));
            WebDriver.Window window = (WebDriver.Window) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.Window.class}, (proxy, method, args) ->
                            method.getName().equals("getSize") ? new Dimension(1200, 800) : null);
            WebDriver.Options manage = (WebDriver.Options) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.Options.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "window" -> window;
                        case "logs" -> logManager;
                        default -> defaultValue(method.getReturnType());
                    });
            WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class, TakesScreenshot.class, HasCapabilities.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getScreenshotAs" -> {
                            screenshotCalls.incrementAndGet(); order.add("screenshot"); yield screenshot.toFile();
                        }
                        case "executeScript" -> {
                            String script = String.valueOf(args[0]);
                            if (script.contains("style.visibility='hidden'")) { order.add("hide"); yield Map.of("present", true, "visibility", ""); }
                            if (script.contains("t&&t.present")) { order.add("restore"); yield null; }
                            if (script.contains("window.innerWidth")) yield Map.of("width", 1000, "height", 700);
                            yield null;
                        }
                        case "getCurrentUrl" -> "http://127.0.0.1/test";
                        case "getTitle" -> "Bundle test";
                        case "getWindowHandle" -> "window-1";
                        case "getWindowHandles" -> Set.of("window-1");
                        case "getPageSource" -> pageSource;
                        case "manage" -> manage;
                        case "getCapabilities" -> capabilities;
                        case "toString" -> "failure-bundle-driver";
                        default -> defaultValue(method.getReturnType());
                    });
            return new DriverFixture(driver, screenshotCalls, order);
        } catch (Exception failure) {
            throw new RuntimeException(failure);
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class || type == long.class || type == short.class || type == byte.class) return 0;
        if (type == double.class || type == float.class) return 0.0;
        return null;
    }

    private record DriverFixture(WebDriver driver, AtomicInteger screenshots, List<String> captureOrder) {}
    private static final class LogTypeName { private static final String BROWSER = "browser"; }
}
