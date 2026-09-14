package io.github.testlens.testng;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.testng.IResultMap;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.TestNG;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ManagedTestStateTestNgTest {
    @BeforeEach void reset() { Harness.reset(); }

    @Test
    void parallelDataProviderIsolatesScenarioAndSharesSuiteAtomically() {
        TestNG testng = run(ParallelDataFixture.class);
        assertFalse(testng.hasFailure());
        assertEquals(10, Harness.rows.size());
        assertEquals(10, Harness.sessions.size());
        assertEquals(1, Harness.suiteInitializers.get());
        assertEquals(1, Harness.suiteValues.size());
        assertEquals(10, Harness.cleanups.get());
        assertEquals(10, Harness.quits.get());

        Object firstSuiteValue = Harness.suiteValues.iterator().next();
        TestNG secondRun = run(ParallelDataFixture.class);
        assertFalse(secondRun.hasFailure());
        assertEquals(2, Harness.suiteInitializers.get());
        assertEquals(2, Harness.suiteValues.size());
        Object secondSuiteValue = Harness.suiteValues.stream()
                .filter(value -> value != firstSuiteValue).findFirst().orElseThrow();
        assertNotSame(firstSuiteValue, secondSuiteValue);
        assertEquals(20, Harness.cleanups.get());
        assertEquals(20, Harness.quits.get());
    }

    @Test
    void retryGetsFreshScenarioAndResourcesPerAttempt() {
        TestNG testng = run(RetryFixture.class);
        assertFalse(testng.hasFailure());
        assertEquals(2, Harness.retryAttempts.get());
        assertEquals(2, Harness.cleanups.get());
        assertEquals(2, Harness.sessions.size());
        assertTrue(Harness.retryStateWasFresh.get());
    }

    @Test
    void cleanupFailurePreservesPrimaryAndFailsSuccess() {
        ResultCollector failed = new ResultCollector();
        run(failed, PrimaryAndCleanupFailureFixture.class);
        ITestResult primaryResult = failed.failedTests.getAllResults().iterator().next();
        assertEquals("PRIMARY", primaryResult.getThrowable().getMessage());
        assertEquals(1, primaryResult.getThrowable().getSuppressed().length);

        Harness.reset();
        ResultCollector passed = new ResultCollector();
        run(passed, CleanupFailureFixture.class);
        ITestResult cleanupResult = passed.failedTests.getAllResults().iterator().next();
        assertTrue(cleanupResult.getThrowable().getMessage().contains("resources could not be cleaned"));
    }

    @Test
    void skippedAndAfterMethodFailuresStillCleanRegisteredResources() {
        TestNG skipped = run(SkippedFixture.class);
        assertFalse(skipped.hasFailure());
        assertEquals(1, Harness.cleanups.get());

        Harness.reset();
        TestNG afterMethod = run(FailingAfterMethodFixture.class);
        assertTrue(afterMethod.hasFailure());
        assertEquals(1, Harness.cleanups.get());
    }

    private static TestNG run(Class<?> fixture) {
        return run(new ResultCollector(), fixture);
    }

    private static TestNG run(ResultCollector collector, Class<?> fixture) {
        TestNG testng = new TestNG(false);
        testng.setUseDefaultListeners(false);
        testng.setOutputDirectory(Path.of("target", "managed-state-testng-native").toString());
        testng.setTestClasses(new Class<?>[]{fixture});
        testng.addListener(collector);
        testng.run();
        return testng;
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = Factory.class)
    public static class ParallelDataFixture {
        @DataProvider(parallel = true)
        public Object[][] rows() {
            Object[][] values = new Object[10][1];
            for (int index = 0; index < values.length; index++) values[index][0] = index;
            return values;
        }

        @org.testng.annotations.Test(dataProvider = "rows")
        public void row(int row) {
            TestLens lens = TestLensTestNgContext.current().lens();
            assertEquals(Optional.empty(), lens.scenarioState().get("row", Integer.class));
            lens.scenarioState().put("row", row);
            assertEquals(row, lens.scenarioState().require("row", Integer.class));
            Harness.rows.add(row);
            Harness.sessions.add(lens.session().orElseThrow().id());
            Harness.suiteValues.add(lens.suiteState().computeIfAbsent("tenant", Object.class, () -> {
                Harness.suiteInitializers.incrementAndGet();
                return new Object();
            }));
            lens.resources().register("row", row, ignored -> Harness.cleanups.incrementAndGet());
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = Factory.class)
    public static class RetryFixture {
        @org.testng.annotations.Test(retryAnalyzer = RetryOnce.class)
        public void retry() {
            int attempt = Harness.retryAttempts.incrementAndGet();
            TestLens lens = TestLensTestNgContext.current().lens();
            Harness.sessions.add(lens.session().orElseThrow().id());
            Harness.retryStateWasFresh.compareAndSet(true,
                    lens.scenarioState().get("attempt", Integer.class).isEmpty());
            lens.scenarioState().put("attempt", attempt);
            lens.resources().register("attempt", attempt, ignored -> Harness.cleanups.incrementAndGet());
            if (attempt == 1) throw new AssertionError("first attempt");
        }
    }

    public static class RetryOnce implements org.testng.IRetryAnalyzer {
        private final AtomicBoolean retried = new AtomicBoolean();
        @Override public boolean retry(ITestResult result) { return retried.compareAndSet(false, true); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = Factory.class)
    public static class PrimaryAndCleanupFailureFixture {
        @org.testng.annotations.Test public void fails() {
            TestLens lens = TestLensTestNgContext.current().lens();
            lens.resources().register("secret", new Object(), ignored -> { throw new IllegalStateException("CLEANUP"); });
            throw new AssertionError("PRIMARY");
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = Factory.class)
    public static class CleanupFailureFixture {
        @org.testng.annotations.Test public void passes() {
            TestLensTestNgContext.current().lens().resources().register("secret", new Object(), ignored -> {
                throw new IllegalStateException("CLEANUP");
            });
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = Factory.class)
    public static class SkippedFixture {
        @org.testng.annotations.Test public void skips() {
            TestLensTestNgContext.current().lens().resources().register(
                    "resource", new Object(), ignored -> Harness.cleanups.incrementAndGet());
            throw new SkipException("intentional skip");
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = Factory.class)
    public static class FailingAfterMethodFixture {
        @org.testng.annotations.Test public void passes() {
            TestLensTestNgContext.current().lens().resources().register(
                    "resource", new Object(), ignored -> Harness.cleanups.incrementAndGet());
        }
        @org.testng.annotations.AfterMethod public void failsAfterMethod() {
            throw new IllegalStateException("AFTER");
        }
    }

    public static class Factory implements TestLensTestNgFactory {
        @Override public WebDriver createDriver() { return Harness.driver(); }
        @Override public TestLensOptions lensOptions() {
            return TestLensOptions.builder().outputRoot(Path.of("target", "managed-state-testng"))
                    .screenshotOnFailure(false)
                    .failureBundleOptions(io.github.testlens.selenium.evidence.FailureBundleOptions.builder()
                            .enabled(false).build())
                    .build();
        }
    }

    private static final class ResultCollector implements org.testng.ITestListener {
        private IResultMap failedTests;
        @Override public void onFinish(ITestContext context) { failedTests = context.getFailedTests(); }
    }

    private static final class Harness {
        static final Set<Integer> rows = ConcurrentHashMap.newKeySet();
        static final Set<String> sessions = ConcurrentHashMap.newKeySet();
        static final Set<Object> suiteValues = ConcurrentHashMap.newKeySet();
        static final AtomicInteger suiteInitializers = new AtomicInteger();
        static final AtomicInteger cleanups = new AtomicInteger();
        static final AtomicInteger quits = new AtomicInteger();
        static final AtomicInteger retryAttempts = new AtomicInteger();
        static final AtomicBoolean retryStateWasFresh = new AtomicBoolean(true);

        static void reset() {
            rows.clear(); sessions.clear(); suiteValues.clear();
            suiteInitializers.set(0); cleanups.set(0); quits.set(0); retryAttempts.set(0);
            retryStateWasFresh.set(true);
        }

        static WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(Harness.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                        if (method.getName().equals("quit")) { quits.incrementAndGet(); return null; }
                        if (method.getName().startsWith("execute")) return null;
                        if (method.getName().equals("toString")) return "managed-state-testng-driver";
                        Class<?> type = method.getReturnType();
                        if (!type.isPrimitive()) return null;
                        if (type == boolean.class) return false;
                        if (type == char.class) return '\0';
                        return 0;
                    });
        }
    }
}
