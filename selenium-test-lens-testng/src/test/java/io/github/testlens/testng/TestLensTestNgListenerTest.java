package io.github.testlens.testng;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.TestNG;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.BeforeMethod;
import org.testng.xml.XmlSuite;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLensTestNgListenerTest {
    @BeforeEach
    void resetHarness() {
        Harness.reset();
    }

    @AfterEach
    void everyCreatedDriverIsClosedExactlyOnce() {
        Harness.drivers.forEach(driver -> assertEquals(1, driver.quitCalls.get()));
        Harness.results.forEach(result ->
                assertEquals(null, result.getAttribute(TestLensTestNgListener.STATE_ATTRIBUTE)));
    }

    @Test
    void successFinalizesReportsBeforeOneQuitAndRemovesState() {
        run(PassedFixture.class);

        Observation observation = Harness.onlyObservation();
        assertEquals(TraceStatus.PASSED, observation.session.metadata().status());
        assertReportsExist(observation);
        assertEquals(List.of("body", "quit:PASSED"), observation.driver.events);
        assertEquals(1, Harness.passed().size());
    }

    @Test
    void failurePreservesTheSameThrowableAndNullFailureStillFinalizesFailed() {
        Harness.testFailure = new AssertionError("original failure");
        run(FailedFixture.class);
        Observation failed = Harness.onlyObservation();
        assertEquals(TraceStatus.FAILED, failed.session.metadata().status());
        assertSame(Harness.testFailure, Harness.failed().get(0).getThrowable());
        TraceEvent event = onlyFinishedEvent(failed.session);
        assertEquals(Harness.testFailure.getClass().getName(), event.failure().exceptionType());

        Harness.reset();
        run(NullThrowableFailureFixture.class);
        assertEquals(TraceStatus.FAILED, Harness.onlyObservation().session.metadata().status());
        assertEquals(1, Harness.failed().size());
    }

    @Test
    void skipExceptionBecomesSkippedAndPreservesReason() {
        run(SkippedFixture.class);

        Observation observation = Harness.onlyObservation();
        assertEquals(TraceStatus.SKIPPED, observation.session.metadata().status());
        assertEquals("environment unavailable", onlyFinishedEvent(observation.session).message());
        assertEquals(1, Harness.skipped().size());
    }

    @Test
    void successPercentageFailureIsFinalizedAsFailed() {
        run(SuccessPercentageFixture.class);

        assertEquals(2, Harness.observations.size());
        assertTrue(Harness.observations.stream().anyMatch(o -> o.session.metadata().status() == TraceStatus.FAILED));
        assertTrue(Harness.observations.stream().anyMatch(o -> o.session.metadata().status() == TraceStatus.PASSED));
        assertEquals(1, Harness.percentageFailures().size());
    }

    @Test
    void quitFailureChangesSuccessButIsSuppressedForFailureAndSkip() {
        Harness.quitFailure.set(true);
        run(PassedFixture.class);
        assertEquals(1, Harness.failed().size());
        assertEquals("quit failed", Harness.failed().get(0).getThrowable().getMessage());

        Harness.reset();
        Harness.quitFailure.set(true);
        Harness.testFailure = new AssertionError("primary");
        run(FailedFixture.class);
        assertSame(Harness.testFailure, Harness.failed().get(0).getThrowable());
        assertEquals("quit failed", Harness.testFailure.getSuppressed()[0].getMessage());

        Harness.reset();
        Harness.quitFailure.set(true);
        run(SkippedFixture.class);
        Throwable skipped = Harness.skipped().get(0).getThrowable();
        assertEquals("quit failed", skipped.getSuppressed()[0].getMessage());
    }

    @Test
    void factoryAndAttachFailuresKeepPrimaryAndCloseOnlyCreatedDriver() {
        Harness.factoryFailure = new IllegalStateException("factory failed");
        run(PassedFixture.class);
        assertTrue(Harness.drivers.isEmpty());
        assertTrue(Harness.observations.isEmpty());
        assertEquals("factory failed", Harness.failed().get(0).getThrowable().getMessage());

        Harness.reset();
        Harness.nonScriptDriver.set(true);
        Harness.quitFailure.set(true);
        run(PassedFixture.class);
        assertEquals(1, Harness.drivers.size());
        Throwable setup = Harness.failed().get(0).getThrowable();
        assertFalse(setup.getMessage().equals("quit failed"));
        assertEquals("quit failed", setup.getSuppressed()[0].getMessage());
    }

    @Test
    void listenerWithoutAnnotationIsNoOpAndCurrentOutsideInvocationFailsClearly() {
        run(UnmanagedFixture.class);
        assertTrue(Harness.drivers.isEmpty());
        assertTrue(Harness.observations.isEmpty());
        assertEquals(1, Harness.passed().size());
        IllegalStateException failure = assertThrows(IllegalStateException.class, TestLensTestNgContext::current);
        assertTrue(failure.getMessage().contains("No active Selenium Test Lens TestNG invocation"));
    }

    @Test
    void disabledDependencySkippedAndConfigurationFailureCreateNoOrphanSessions() {
        run(DisabledFixture.class);
        run(DependencyFixture.class);
        run(BeforeMethodFailureFixture.class);

        assertEquals(1, Harness.drivers.size(), "only the physically invoked dependency method is managed");
        assertEquals(1, Harness.observations.size());
        assertEquals("dependency", Harness.observations.get(0).label);
    }

    @Test
    void customOptionsAndSessionNameComeFromFreshFactoryPerInvocation() {
        run(CustomFactoryFixture.class);

        assertEquals(2, Harness.factoryInstances.get());
        assertEquals(2, Harness.observations.size());
        assertTrue(Harness.observations.stream().allMatch(o -> o.session.metadata().name().equals("custom-session")));
        Harness.observations.forEach(TestLensTestNgListenerTest::assertReportsExist);
    }

    @Test
    void sequentialAndParallelDataProvidersHaveIndependentStateWithoutParameterLeakage() {
        run(SequentialDataFixture.class);
        assertIndependent(2);
        assertTrue(Harness.observations.stream().noneMatch(o -> o.session.metadata().name().contains("secret")));

        Harness.reset();
        Harness.barrier = new CountDownLatch(2);
        run(ParallelDataFixture.class);
        assertIndependent(2);
        assertTrue(Harness.observations.stream().noneMatch(o -> o.session.metadata().name().contains("secret")));
    }

    @Test
    void reusedInstanceAndParallelMethodsDoNotCrossInvocationState() {
        ReusedInstanceFixture instance = new ReusedInstanceFixture();
        runInstances(instance);
        assertIndependent(2);
        assertEquals(1, Harness.instanceIdentities.stream().distinct().count());

        Harness.reset();
        Harness.barrier = new CountDownLatch(2);
        runParallel(ParallelMethodsFixture.class);
        assertIndependent(2);
    }

    @Test
    void retryAnalyzerCreatesFailedAndPassedIsolatedSessions() {
        run(RetryFixture.class);

        assertEquals(2, Harness.observations.size());
        assertEquals(2, Harness.observations.stream().map(o -> o.driver.proxy).distinct().count());
        assertEquals(2, Harness.observations.stream().map(o -> o.session.id()).distinct().count());
        assertEquals(Set.of(TraceStatus.FAILED, TraceStatus.PASSED),
                Harness.observations.stream().map(o -> o.session.metadata().status()).collect(java.util.stream.Collectors.toSet()));
    }

    private static void run(Class<?>... fixtures) {
        execute(testng -> testng.setTestClasses(fixtures));
    }

    private static void runInstances(Object... fixtures) {
        execute(testng -> testng.setTestClasses(new Class<?>[]{fixtures[0].getClass()}));
    }

    private static void runParallel(Class<?>... fixtures) {
        execute(testng -> {
            testng.setTestClasses(fixtures);
            testng.setParallel(XmlSuite.ParallelMode.METHODS);
            testng.setThreadCount(2);
        });
    }

    private static void execute(java.util.function.Consumer<TestNG> configuration) {
        ResultCollector collector = new ResultCollector();
        TestNG testng = new TestNG(false);
        testng.setUseDefaultListeners(false);
        testng.setOutputDirectory(Harness.testngOutput.resolve(UUID.randomUUID().toString()).toString());
        testng.addListener(collector);
        configuration.accept(testng);
        testng.run();
        Harness.results.addAll(collector.results);
    }

    private static void assertIndependent(int expected) {
        assertEquals(expected, Harness.observations.size());
        assertEquals(expected, Harness.drivers.size());
        assertEquals(expected, Harness.observations.stream().map(o -> o.driver.proxy).distinct().count());
        assertEquals(expected, Harness.observations.stream().map(o -> o.lens).distinct().count());
        assertEquals(expected, Harness.observations.stream().map(o -> o.session.id()).distinct().count());
    }

    private static void assertReportsExist(Observation observation) {
        Path directory = observation.options.outputRoot()
                .resolve(sanitize(observation.session.metadata().name())).resolve(observation.session.id());
        assertTrue(Files.isRegularFile(directory.resolve("trace.json")), directory.toString());
        assertTrue(Files.isRegularFile(directory.resolve("report.html")), directory.toString());
    }

    private static TraceEvent onlyFinishedEvent(UiTestLensSession session) {
        List<TraceEvent> events = session.events().stream()
                .filter(event -> event.type() == TraceEventType.SESSION_FINISHED).toList();
        assertEquals(1, events.size());
        return events.get(0);
    }

    private static String sanitize(String value) {
        String safe = value.trim().toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-").replaceAll("-+", "-")
                .replaceAll("(^[-.]+|[-.]+$)", "");
        return safe.isBlank() ? "session" : safe;
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class PassedFixture {
        @org.testng.annotations.Test public void passes() { Harness.observe("body"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class FailedFixture {
        @org.testng.annotations.Test public void fails() { Harness.observe("failure"); throw Harness.testFailure; }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class NullThrowableFailureFixture {
        @org.testng.annotations.Test public void fails() {
            Harness.observe("null-failure");
            ITestResult result = org.testng.Reporter.getCurrentTestResult();
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(null);
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class SkippedFixture {
        @org.testng.annotations.Test public void skips() { Harness.observe("skip"); throw new SkipException("environment unavailable"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class SuccessPercentageFixture {
        private static final AtomicInteger CALLS = new AtomicInteger();
        @org.testng.annotations.Test(invocationCount = 2, successPercentage = 50)
        public void partlyFails() {
            Harness.observe("percentage");
            if (CALLS.getAndIncrement() % 2 == 0) throw new AssertionError("allowed percentage failure");
        }
    }

    @Listeners(TestLensTestNgListener.class)
    public static class UnmanagedFixture {
        @org.testng.annotations.Test public void unmanaged() {
            assertThrows(IllegalStateException.class, TestLensTestNgContext::current);
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class DisabledFixture {
        @org.testng.annotations.Test(enabled = false) public void disabled() { Harness.observe("disabled"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class DependencyFixture {
        @org.testng.annotations.Test(groups = "prerequisite") public void dependency() { Harness.observe("dependency"); throw new AssertionError("dependency failed"); }
        @org.testng.annotations.Test(dependsOnGroups = "prerequisite") public void blocked() { Harness.observe("blocked"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class BeforeMethodFailureFixture {
        @BeforeMethod public void reject() { throw new IllegalStateException("configuration failed"); }
        @org.testng.annotations.Test public void blocked() { Harness.observe("configuration-blocked"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = CustomFactory.class)
    public static class CustomFactoryFixture {
        @org.testng.annotations.Test(invocationCount = 2) public void custom() { Harness.observe("custom"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class SequentialDataFixture {
        @DataProvider public Object[][] values() { return new Object[][]{{"secret-alpha"}, {"secret-beta"}}; }
        @org.testng.annotations.Test(dataProvider = "values") public void value(String ignored) { Harness.observe("sequential-data"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class ParallelDataFixture {
        @DataProvider(parallel = true) public Object[][] values() { return new Object[][]{{"secret-alpha"}, {"secret-beta"}}; }
        @org.testng.annotations.Test(dataProvider = "values") public void value(String ignored) { Harness.awaitBarrier(); Harness.observe("parallel-data"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class ReusedInstanceFixture {
        @org.testng.annotations.Test(invocationCount = 2) public void repeatedInstance() {
            Harness.instanceIdentities.add(System.identityHashCode(this));
            Harness.observe("same-instance");
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class ParallelMethodsFixture {
        @org.testng.annotations.Test public void first() { Harness.awaitBarrier(); Harness.observe("parallel-first"); }
        @org.testng.annotations.Test public void second() { Harness.awaitBarrier(); Harness.observe("parallel-second"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = HarnessFactory.class)
    public static class RetryFixture {
        private static final AtomicInteger ATTEMPTS = new AtomicInteger();
        @org.testng.annotations.Test(retryAnalyzer = RetryOnce.class)
        public void retry() {
            Harness.observe("retry");
            if (ATTEMPTS.getAndIncrement() % 2 == 0) throw new AssertionError("first attempt");
        }
    }

    public static class RetryOnce implements org.testng.IRetryAnalyzer {
        private final AtomicBoolean retried = new AtomicBoolean();
        @Override public boolean retry(ITestResult result) { return retried.compareAndSet(false, true); }
    }

    public static class HarnessFactory implements TestLensTestNgFactory {
        public HarnessFactory() { Harness.factoryInstances.incrementAndGet(); }
        @Override public WebDriver createDriver() {
            if (Harness.factoryFailure != null) throw Harness.factoryFailure;
            TrackingDriver driver = new TrackingDriver(Harness.quitFailure.get(), Harness.nonScriptDriver.get());
            Harness.drivers.add(driver);
            return driver.proxy;
        }
        @Override public TestLensOptions lensOptions() { return Harness.defaultOptions; }
    }

    public static class CustomFactory extends HarnessFactory {
        public CustomFactory() { }
        @Override public TestLensOptions lensOptions() { return Harness.customOptions; }
        @Override public String sessionName(ITestResult result) { return "custom-session"; }
    }

    private static final class ResultCollector implements org.testng.ITestListener {
        private final List<ITestResult> results = new CopyOnWriteArrayList<>();
        @Override public void onTestSuccess(ITestResult result) { results.add(result); }
        @Override public void onTestFailure(ITestResult result) { results.add(result); }
        @Override public void onTestSkipped(ITestResult result) { results.add(result); }
        @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) { results.add(result); }
    }

    private static final class Harness {
        private static final Path outputRoot = Path.of("target", "testng-listener-test-" + UUID.randomUUID());
        private static final Path customOutputRoot = outputRoot.resolve("custom");
        private static final Path testngOutput = outputRoot.resolve("testng-native");
        private static final TestLensOptions defaultOptions = TestLensOptions.builder()
                .outputRoot(outputRoot).screenshotOnFailure(false).build();
        private static final TestLensOptions customOptions = TestLensOptions.builder()
                .outputRoot(customOutputRoot).screenshotOnFailure(false).build();
        private static final List<TrackingDriver> drivers = new CopyOnWriteArrayList<>();
        private static final List<Observation> observations = new CopyOnWriteArrayList<>();
        private static final List<ITestResult> results = new CopyOnWriteArrayList<>();
        private static final List<Integer> instanceIdentities = new CopyOnWriteArrayList<>();
        private static final AtomicInteger factoryInstances = new AtomicInteger();
        private static final AtomicBoolean quitFailure = new AtomicBoolean();
        private static final AtomicBoolean nonScriptDriver = new AtomicBoolean();
        private static volatile AssertionError testFailure;
        private static volatile RuntimeException factoryFailure;
        private static volatile CountDownLatch barrier;

        private static void observe(String label) {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            TrackingDriver driver = drivers.stream().filter(candidate -> candidate.proxy == context.driver())
                    .findFirst().orElseThrow();
            assertSame(context.driver(), context.lens().driver());
            driver.events.add("body");
            TestLensOptions options = context.session().metadata().name().equals("custom-session")
                    ? customOptions : defaultOptions;
            observations.add(new Observation(label, driver, context.lens(), context.session(), options));
        }

        private static void awaitBarrier() {
            CountDownLatch current = barrier;
            current.countDown();
            try {
                assertTrue(current.await(5, TimeUnit.SECONDS), "parallel invocations did not rendezvous");
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        }

        private static Observation onlyObservation() {
            assertEquals(1, observations.size());
            return observations.get(0);
        }

        private static List<ITestResult> passed() { return filter(ITestResult.SUCCESS); }
        private static List<ITestResult> failed() { return filter(ITestResult.FAILURE); }
        private static List<ITestResult> skipped() { return filter(ITestResult.SKIP); }
        private static List<ITestResult> percentageFailures() { return filter(ITestResult.SUCCESS_PERCENTAGE_FAILURE); }
        private static List<ITestResult> filter(int status) {
            return results.stream().filter(result -> result.getStatus() == status).toList();
        }

        private static void reset() {
            drivers.clear(); observations.clear(); results.clear(); instanceIdentities.clear();
            factoryInstances.set(0); quitFailure.set(false); nonScriptDriver.set(false);
            testFailure = null; factoryFailure = null; barrier = null;
        }
    }

    private record Observation(String label, TrackingDriver driver, TestLens lens,
                               UiTestLensSession session, TestLensOptions options) { }

    private static final class TrackingDriver {
        private final AtomicInteger quitCalls = new AtomicInteger();
        private final List<String> events = new CopyOnWriteArrayList<>();
        private final WebDriver proxy;

        private TrackingDriver(boolean failQuit, boolean withoutJavascript) {
            Class<?>[] interfaces = withoutJavascript
                    ? new Class<?>[]{WebDriver.class}
                    : new Class<?>[]{WebDriver.class, JavascriptExecutor.class};
            this.proxy = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(), interfaces,
                    (ignored, method, arguments) -> {
                        if (method.getName().equals("quit")) {
                            quitCalls.incrementAndGet();
                            Observation observation = Harness.observations.stream()
                                    .filter(item -> item.driver == this).findFirst().orElse(null);
                            events.add("quit:" + (observation == null ? "SETUP" : observation.session.metadata().status()));
                            if (failQuit) throw new IllegalStateException("quit failed");
                            return null;
                        }
                        if (method.getName().startsWith("execute")) return null;
                        if (method.getName().equals("findElements")) return List.<WebElement>of();
                        if (method.getName().equals("getWindowHandles")) return Set.of("window");
                        if (method.getName().equals("getWindowHandle")) return "window";
                        Class<?> type = method.getReturnType();
                        if (type == boolean.class) return false;
                        if (type == int.class) return 0;
                        if (type == long.class) return 0L;
                        if (type == double.class) return 0D;
                        if (type == String.class) return "";
                        return null;
                    });
        }
    }
}
