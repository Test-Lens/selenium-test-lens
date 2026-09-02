package io.github.testlens.testng;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.UiTestLensSession;
import org.openqa.selenium.WebDriver;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/** Owns and finalizes one WebDriver, Lens, and session for every physical TestNG invocation. */
public final class TestLensTestNgListener implements IInvokedMethodListener {
    static final String STATE_ATTRIBUTE = "io.github.testlens.testng.invocation-state";
    private static final String SKIPPED_REASON_FALLBACK = "Test skipped by TestNG";

    /** Creates a listener suitable for explicit registration with {@code @Listeners}. */
    public TestLensTestNgListener() {
    }

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod() || result.getStatus() == ITestResult.SKIP
                || result.getAttribute(STATE_ATTRIBUTE) != null) {
            return;
        }
        TestLensTestNg annotation = annotation(result);
        if (annotation == null) {
            return;
        }

        WebDriver driver = null;
        try {
            TestLensTestNgFactory factory = createFactory(annotation.factory());
            driver = Objects.requireNonNull(factory.createDriver(),
                    "TestLensTestNgFactory.createDriver() returned null");
            TestLensOptions options = Objects.requireNonNull(factory.lensOptions(),
                    "TestLensTestNgFactory.lensOptions() returned null");
            TestLens lens = TestLens.attach(driver, options);
            String sessionName = Objects.requireNonNull(factory.sessionName(result),
                    "TestLensTestNgFactory.sessionName() returned null");
            UiTestLensSession session = lens.startSession(sessionName);
            result.setAttribute(STATE_ATTRIBUTE, new InvocationState(driver, lens, session));
        } catch (Throwable setupFailure) {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Throwable quitFailure) {
                    addSuppressed(setupFailure, quitFailure);
                }
            }
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(setupFailure);
            rethrow(setupFailure);
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) {
            return;
        }
        Object stored = result.getAttribute(STATE_ATTRIBUTE);
        if (!(stored instanceof InvocationState state) || !state.beginCleanup()) {
            return;
        }

        int originalStatus = result.getStatus();
        Throwable original = result.getThrowable();
        Throwable cleanupFailure = null;
        try {
            try {
                finish(state.lens, result, originalStatus, original);
            } catch (Throwable finalizationFailure) {
                cleanupFailure = finalizationFailure;
            }
            try {
                state.quit();
            } catch (Throwable quitFailure) {
                if (cleanupFailure == null) {
                    cleanupFailure = quitFailure;
                } else {
                    addSuppressed(cleanupFailure, quitFailure);
                }
            }
        } finally {
            result.removeAttribute(STATE_ATTRIBUTE);
        }

        if (cleanupFailure == null) {
            return;
        }
        if (originalStatus == ITestResult.SUCCESS) {
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(cleanupFailure);
        } else if (original != null) {
            addSuppressed(original, cleanupFailure);
        } else {
            result.setThrowable(cleanupFailure);
        }
    }

    private static void finish(TestLens lens, ITestResult result, int status, Throwable original) {
        switch (status) {
            case ITestResult.SUCCESS -> lens.finishPassed();
            case ITestResult.SKIP -> lens.finishSkipped(skippedReason(original));
            case ITestResult.FAILURE, ITestResult.SUCCESS_PERCENTAGE_FAILURE -> lens.finishFailed(original);
            default -> lens.finishFailed(original);
        }
    }

    private static String skippedReason(Throwable skipped) {
        if (skipped == null || skipped.getMessage() == null || skipped.getMessage().isBlank()) {
            return SKIPPED_REASON_FALLBACK;
        }
        return skipped.getMessage();
    }

    private static TestLensTestNg annotation(ITestResult result) {
        Object instance = result.getInstance();
        return instance == null ? null : instance.getClass().getAnnotation(TestLensTestNg.class);
    }

    private static TestLensTestNgFactory createFactory(Class<? extends TestLensTestNgFactory> type) {
        try {
            return type.getConstructor().newInstance();
        } catch (InvocationTargetException failure) {
            rethrow(failure.getCause());
            throw new AssertionError("unreachable");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("TestLens TestNG factory " + type.getName()
                    + " must have an accessible public no-argument constructor", failure);
        }
    }

    private static void addSuppressed(Throwable primary, Throwable secondary) {
        if (primary != secondary) {
            primary.addSuppressed(secondary);
        }
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new RuntimeException(failure);
    }

    static final class InvocationState {
        private final WebDriver driver;
        private final TestLens lens;
        private final UiTestLensSession session;
        private final TestLensTestNgContext context;
        private boolean cleanupStarted;
        private boolean quit;

        private InvocationState(WebDriver driver, TestLens lens, UiTestLensSession session) {
            this.driver = driver;
            this.lens = lens;
            this.session = session;
            this.context = new TestLensTestNgContext(driver, lens, session);
        }

        private synchronized boolean beginCleanup() {
            if (cleanupStarted) {
                return false;
            }
            cleanupStarted = true;
            return true;
        }

        private synchronized void quit() {
            if (quit) {
                return;
            }
            quit = true;
            driver.quit();
        }

        TestLensTestNgContext context() {
            return context;
        }
    }
}
