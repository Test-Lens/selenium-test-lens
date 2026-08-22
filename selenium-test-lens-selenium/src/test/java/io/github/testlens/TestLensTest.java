package io.github.testlens;

import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

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
    void observabilityFailuresDoNotTurnPassedTestIntoFailure() {
        TestLens lens = TestLens.attach(driver(true), TestLensOptions.builder().outputRoot(temp).build());
        lens.startSession("hud failure");
        assertDoesNotThrow(lens::finishPassed);
    }

    @Test
    void failedFinalizationKeepsOriginalFailureInSession() {
        RuntimeException original = new RuntimeException("business failure");
        TestLens lens = TestLens.attach(driver(true), TestLensOptions.builder().outputRoot(temp).build());
        UiTestLensSession session = lens.startSession("failed");
        assertDoesNotThrow(() -> lens.finishFailed(original));
        assertTrue(session.events().stream().filter(e -> e.type() == TraceEventType.SESSION_FINISHED)
                .anyMatch(e -> e.failure() != null && e.failure().message().contains("business failure")));
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
}
