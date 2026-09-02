package io.github.testlens;

import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TestLensTest {
    @TempDir Path temp;

    @Test
    void attachesExistingDriverAndFinalizesIntoUniqueSessionDirectory() {
        WebDriver driver = driver(false);
        TestLens first = TestLens.attach(driver, TestLensOptions.builder().outputRoot(temp).build());
        TestLens second = TestLens.attach(driver, TestLensOptions.builder().outputRoot(temp).build());
        UiTestLensSession firstSession = first.startSession("same test");
        UiTestLensSession secondSession = second.startSession("same test");

        TestLensFinalizationResult one = first.finishPassed();
        TestLensFinalizationResult two = second.finishPassed();

        assertSame(driver, first.driver());
        assertNotEquals(firstSession.id(), secondSession.id());
        assertNotEquals(one.outputDirectory(), two.outputDirectory());
        assertTrue(Files.exists(one.jsonReport()));
        assertTrue(Files.exists(one.htmlReport()));
    }

    @Test
    void skippedFinalizationWritesSkippedReportsAndReasonWithoutScreenshot() throws Exception {
        AtomicInteger screenshotCalls = new AtomicInteger();
        TestLens lens = TestLens.attach(screenshotDriver(screenshotCalls), TestLensOptions.builder()
                .outputRoot(temp)
                .build());
        UiTestLensSession session = lens.startSession("skipped");

        TestLensFinalizationResult result = lens.finishSkipped("assumption not met");

        assertEquals(TraceStatus.SKIPPED, session.metadata().status());
        List<TraceEvent> finished = finishedEvents(session);
        assertEquals(1, finished.size());
        assertEquals(TraceStatus.SKIPPED, finished.get(0).status());
        assertEquals("assumption not met", finished.get(0).message());
        assertNull(finished.get(0).failure());
        assertTrue(Files.exists(result.jsonReport()));
        assertTrue(Files.exists(result.htmlReport()));
        assertTrue(Files.readString(result.jsonReport()).contains("\"status\":\"SKIPPED\""));
        assertTrue(Files.readString(result.jsonReport()).contains("assumption not met"));
        assertTrue(Files.readString(result.htmlReport()).contains("SKIPPED"));
        assertTrue(Files.readString(result.htmlReport()).contains("assumption not met"));
        assertNull(result.failureScreenshot());
        assertEquals(0, screenshotCalls.get());
    }

    @Test
    void nullSkippedReasonRemainsSkipped() {
        TestLens lens = TestLens.attach(driver(false), TestLensOptions.builder().outputRoot(temp).build());
        UiTestLensSession session = lens.startSession("null skip reason");

        TestLensFinalizationResult result = assertDoesNotThrow(() -> lens.finishSkipped(null));

        assertEquals(TraceStatus.SKIPPED, result.session().metadata().status());
        assertEquals(TraceStatus.SKIPPED, session.metadata().status());
        assertEquals("", finishedEvents(session).get(0).message());
    }

    @Test
    void failedFinalizationWithNullFailureRemainsFailedAndCapturesScreenshot() throws Exception {
        AtomicInteger screenshotCalls = new AtomicInteger();
        TestLens lens = TestLens.attach(screenshotDriver(screenshotCalls), TestLensOptions.builder()
                .outputRoot(temp)
                .build());
        lens.startSession("failed without throwable");

        TestLensFinalizationResult result = lens.finishFailed(null);

        assertEquals(TraceStatus.FAILED, result.session().metadata().status());
        assertEquals(TraceStatus.FAILED, finishedEvents(result.session()).get(0).status());
        assertNull(finishedEvents(result.session()).get(0).failure());
        assertEquals(1, screenshotCalls.get());
        assertNotNull(result.failureScreenshot());
        assertTrue(Files.exists(result.failureScreenshot()));
        assertTrue(Files.exists(result.jsonReport()));
        assertTrue(Files.exists(result.htmlReport()));
    }

    @Test
    void passedFinalizationNeverCapturesFailureScreenshot() {
        AtomicInteger screenshotCalls = new AtomicInteger();
        TestLens lens = TestLens.attach(screenshotDriver(screenshotCalls), TestLensOptions.builder()
                .outputRoot(temp)
                .build());
        lens.startSession("passed");

        TestLensFinalizationResult result = lens.finishPassed();

        assertEquals(TraceStatus.PASSED, result.session().metadata().status());
        assertNull(result.failureScreenshot());
        assertEquals(0, screenshotCalls.get());
    }

    @Test
    void observabilityFailuresDoNotTurnPassedTestIntoFailure() {
        TestLens lens = TestLens.attach(driver(true), TestLensOptions.builder().outputRoot(temp).build());
        lens.startSession("hud failure");
        assertDoesNotThrow(lens::finishPassed);
    }

    @Test
    void failedFinalizationKeepsOriginalFailureInSession() {
        RuntimeException original = new RuntimeException("business failure");
        TestLens lens = TestLens.attach(driver(true), TestLensOptions.builder()
                .outputRoot(temp)
                .screenshotOnFailure(false)
                .build());
        UiTestLensSession session = lens.startSession("failed");
        TestLensFinalizationResult result = assertDoesNotThrow(() -> lens.finishFailed(original));

        TraceEvent finished = finishedEvents(session).get(0);
        assertEquals(TraceStatus.FAILED, session.metadata().status());
        assertEquals(original.getClass().getName(), finished.failure().exceptionType());
        assertEquals(original.getMessage(), finished.failure().message());
        assertFalse(result.diagnosticFailures().isEmpty());
    }

    @Test
    void reportFailureIsReturnedAndNeverMasksOriginalFailure() throws Exception {
        Path notDirectory = temp.resolve("not-a-directory");
        Files.writeString(notDirectory, "occupied");
        RuntimeException original = new RuntimeException("primary");
        TestLens lens = TestLens.attach(driver(false), TestLensOptions.builder().outputRoot(notDirectory).build());
        lens.startSession("report failure");
        TestLensFinalizationResult result = assertDoesNotThrow(() -> lens.finishFailed(original));
        assertFalse(result.fullySuccessful());
        assertFalse(result.diagnosticFailures().isEmpty());
    }

    @Test
    void allFinalizersWithoutSessionReturnDiagnosticInsteadOfThrowing() {
        TestLens lens = TestLens.attach(driver(false), TestLensOptions.builder().outputRoot(temp).build());

        List<TestLensFinalizationResult> results = assertDoesNotThrow(() -> List.of(
                lens.finishPassed(),
                lens.finishFailed(null),
                lens.finishSkipped("not applicable")));

        assertEquals(3, results.size());
        results.forEach(result -> {
            assertNull(result.session());
            assertFalse(result.diagnosticFailures().isEmpty());
            assertTrue(result.diagnosticFailures().get(0).getMessage().contains("No Test Lens session"));
        });
    }

    @Test
    void diagnosticFailuresNeverMaskAnyRequestedFinalStatus() throws Exception {
        Path notDirectory = temp.resolve("blocked-output");
        Files.writeString(notDirectory, "occupied");

        assertDiagnosticFailureKeepsStatus(notDirectory, TraceStatus.PASSED);
        assertDiagnosticFailureKeepsStatus(notDirectory, TraceStatus.FAILED);
        assertDiagnosticFailureKeepsStatus(notDirectory, TraceStatus.SKIPPED);
    }

    private static void assertDiagnosticFailureKeepsStatus(Path outputRoot, TraceStatus expected) {
        TestLens lens = TestLens.attach(driver(true), TestLensOptions.builder()
                .outputRoot(outputRoot)
                .screenshotOnFailure(false)
                .build());
        lens.startSession("diagnostic " + expected);

        TestLensFinalizationResult result = switch (expected) {
            case PASSED -> lens.finishPassed();
            case FAILED -> lens.finishFailed(new AssertionError("primary failure"));
            case SKIPPED -> lens.finishSkipped("not applicable");
            default -> throw new IllegalArgumentException("Unsupported final status: " + expected);
        };

        assertEquals(expected, result.session().metadata().status());
        assertFalse(result.diagnosticFailures().isEmpty());
    }

    private static List<TraceEvent> finishedEvents(UiTestLensSession session) {
        return session.events().stream()
                .filter(event -> event.type() == TraceEventType.SESSION_FINISHED)
                .toList();
    }

    private static WebDriver driver(boolean failJavascript) {
        return (WebDriver) Proxy.newProxyInstance(TestLensTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().startsWith("execute") && failJavascript) throw new RuntimeException("HUD unavailable");
                    if (method.getName().startsWith("execute")) return null;
                    if (method.getName().equals("toString")) return "existing-driver";
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type.isPrimitive()) return 0;
                    return null;
                });
    }

    private WebDriver screenshotDriver(AtomicInteger screenshotCalls) {
        Path source = temp.resolve("browser-screenshot.png");
        try {
            Files.write(source, new byte[]{1, 2, 3});
        } catch (java.io.IOException failure) {
            throw new RuntimeException(failure);
        }
        return (WebDriver) Proxy.newProxyInstance(TestLensTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class, TakesScreenshot.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getScreenshotAs")) {
                        assertSame(OutputType.FILE, args[0]);
                        screenshotCalls.incrementAndGet();
                        return source.toFile();
                    }
                    if (method.getName().startsWith("execute")) return null;
                    if (method.getName().equals("toString")) return "screenshot-driver";
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type.isPrimitive()) return 0;
                    return null;
                });
    }
}
