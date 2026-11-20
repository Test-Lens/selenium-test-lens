package io.github.testlens;

import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.selenium.evidence.FailureBundleOptions;
import io.github.testlens.selenium.network.NetworkCaptureMode;
import io.github.testlens.selenium.network.NetworkDiagnosticsOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LifecycleFailureAuditTest {
    @TempDir Path temp;

    @Test
    void criticalEvidenceFailureStillCleansResourcesAndClosesSessionExactlyOnce() {
        LinkageError evidenceFailure = new LinkageError("synthetic evidence linkage failure");
        TestLens lens = new TestLens(driver(), TestLensOptions.builder()
                .outputRoot(temp).failureBundleOptions(FailureBundleOptions.builder().enabled(false).build()).build(),
                stage -> { if (stage == TestLens.FinalizationStage.FAILURE_EVIDENCE) throw evidenceFailure; });
        UiTestLensSession session = lens.startSession("critical evidence");
        AtomicInteger cleanups = new AtomicInteger();
        lens.resources().register("temporary", new Object(), ignored -> cleanups.incrementAndGet());
        lens.network().start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.MANUAL).build());
        AssertionError primary = new AssertionError("test failure");

        assertSame(evidenceFailure, assertThrows(LinkageError.class, () -> lens.finishFailed(primary)));
        assertEquals(1, cleanups.get());
        assertEquals(TraceStatus.FAILED, session.metadata().status());
        assertFalse(lens.network().isStarted());
        assertTrue(java.util.Arrays.asList(primary.getSuppressed()).contains(evidenceFailure));
        assertSame(evidenceFailure, assertThrows(LinkageError.class, lens::finishPassed));
        assertEquals(1, cleanups.get());
    }

    @Test
    void interruptedCleanupRestoresInterruptAndDoesNotPass() {
        TestLens lens = TestLens.attach(driver(), TestLensOptions.builder().outputRoot(temp)
                .screenshotOnFailure(false).build());
        UiTestLensSession session = lens.startSession("interrupted cleanup");
        AtomicInteger cleanups = new AtomicInteger();
        lens.resources().register("last", new Object(), ignored -> cleanups.incrementAndGet());
        lens.resources().register("interrupted", new Object(), ignored -> { throw new InterruptedException("cancelled"); });
        try {
            assertThrows(TestStateException.class, lens::finishPassed);
            assertTrue(Thread.currentThread().isInterrupted(), "cleanup must preserve cancellation signal");
            assertEquals(TraceStatus.FAILED, session.metadata().status());
            assertEquals(1, cleanups.get());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void criticalCleanupErrorsAreRethrownAfterAllResourcesForPassedAndFailedInvocations() {
        for (boolean alreadyFailed : new boolean[]{false, true}) {
            for (Error failure : new Error[]{new AssertionError("cleanup assertion"),
                    new LinkageError("cleanup linkage"), new OutOfMemoryError("synthetic cleanup OOM")}) {
                TestLens lens = TestLens.attach(driver(), TestLensOptions.builder().outputRoot(temp)
                        .screenshotOnFailure(false).build());
                UiTestLensSession session = lens.startSession("critical cleanup");
                AtomicInteger cleanups = new AtomicInteger();
                lens.resources().register("last", new Object(), ignored -> cleanups.incrementAndGet());
                lens.resources().register("error", new Object(), ignored -> { throw failure; });
                AssertionError original = new AssertionError("primary test assertion");

                Error actual = assertThrows(Error.class,
                        () -> { if (alreadyFailed) lens.finishFailed(original); else lens.finishPassed(); });
                assertSame(failure, actual);
                assertEquals(1, cleanups.get());
                assertEquals(TraceStatus.FAILED, session.metadata().status());
                if (alreadyFailed) assertTrue(java.util.Arrays.asList(original.getSuppressed()).contains(failure));
                assertSame(failure, assertThrows(Error.class, lens::finishPassed));
                assertEquals(1, cleanups.get());
            }
        }
    }
    private static WebDriver driver() {
        return (WebDriver) Proxy.newProxyInstance(LifecycleFailureAuditTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("toString")) return "lifecycle-audit-driver";
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType().isPrimitive()) return 0;
                    return null;
                });
    }
}
