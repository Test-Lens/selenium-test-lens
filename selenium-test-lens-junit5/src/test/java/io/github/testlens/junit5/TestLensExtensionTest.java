package io.github.testlens.junit5;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.RetryOutcomePolicy;
import io.github.testlens.core.trace.RetryPolicyViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.engine.TestExecutionResult;
import org.opentest4j.TestAbortedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class TestLensExtensionTest {
    @BeforeEach
    void resetHarness() {
        Harness.reset();
    }

    @AfterEach
    void verifyEveryCreatedDriverWasClosedOnce() {
        Harness.drivers.forEach(driver -> assertEquals(1, driver.quitCalls.get()));
    }

    @Test
    void passedInvocationFinalizesReportsAndQuitsOnce() {
        execute(PassedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1));

        Observation observation = Harness.onlyObservation();
        assertEquals(TraceStatus.PASSED, observation.session.metadata().status());
        assertReportsExist(observation.session);
    }

    @Test
    void failedInvocationPreservesOriginalFailureAndQuitsOnce() {
        Harness.testFailure = new AssertionError("original test failure");
        execute(FailedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));

        Observation observation = Harness.onlyObservation();
        assertEquals(TraceStatus.FAILED, observation.session.metadata().status());
        TraceEvent finished = onlyFinishedEvent(observation.session);
        assertEquals(Harness.testFailure.getClass().getName(), finished.failure().exceptionType());
        assertEquals(Harness.testFailure.getMessage(), finished.failure().message());
        assertEquals(0, Harness.testFailure.getSuppressed().length);
    }

    @Test
    void abortedAssumptionBecomesSkippedWithItsReason() {
        execute(AbortedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).aborted(1));

        Observation observation = Harness.onlyObservation();
        assertEquals(TraceStatus.SKIPPED, observation.session.metadata().status());
        assertEquals(Harness.abortedFailure.getMessage(), onlyFinishedEvent(observation.session).message());
    }

    @Test
    void setupFailureClosesDriverAndKeepsOriginalFailure() {
        Harness.setupFailure = new IllegalStateException("session name failed");
        Harness.quitFailure.set(true);
        execute(SetupFailureFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));

        assertEquals(1, Harness.drivers.size());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertTrue(Harness.observations.isEmpty());
        assertEquals(1, Harness.setupFailure.getSuppressed().length);
        assertEquals("quit failed", Harness.setupFailure.getSuppressed()[0].getMessage());
    }

    @Test
    void nullDriverFailsSetupClearlyWithoutCreatingState() {
        execute(NullDriverFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));
        assertTrue(Harness.drivers.isEmpty());
        assertTrue(Harness.observations.isEmpty());
    }

    @Test
    void quitFailureIsSuppressedForFailedAndAbortedInvocations() {
        Harness.quitFailure.set(true);
        Harness.testFailure = new AssertionError("primary failure");
        execute(FailedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));
        assertEquals(1, Harness.testFailure.getSuppressed().length);
        assertEquals("quit failed", Harness.testFailure.getSuppressed()[0].getMessage());

        Harness.reset();
        Harness.quitFailure.set(true);
        execute(AbortedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).aborted(1));
        assertNotNull(Harness.abortedFailure);
        assertEquals(1, Harness.abortedFailure.getSuppressed().length);
        assertEquals("quit failed", Harness.abortedFailure.getSuppressed()[0].getMessage());
    }

    @Test
    void quitFailureTurnsPassedInvocationIntoFailure() {
        Harness.quitFailure.set(true);
        execute(PassedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).failed(1));
        assertEquals(TraceStatus.PASSED, Harness.onlyObservation().session.metadata().status());
    }

    @Test
    void retryPolicyViolationFailsInvocationOnceAndOwnsQuitFailureAsSuppressed() {
        Harness.quitFailure.set(true);
        EngineExecutionResults results = execute(PolicyFixture.class);
        results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));

        Throwable failure = results.testEvents().failed().list().get(0)
                .getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow();
        assertTrue(failure instanceof RetryPolicyViolationException);
        assertEquals(1, failure.getSuppressed().length);
        assertEquals("quit failed", failure.getSuppressed()[0].getMessage());
        Observation observation = Harness.onlyObservation();
        assertEquals(TraceStatus.FAILED, observation.session.metadata().status());
        assertNotNull(onlyFinishedEvent(observation.session).failure());
        assertReportsExist(observation.session);
    }

    @Test
    void resolverProvidesSameInvocationDriverAndLensWithoutClaimingTestInfo() {
        execute(ParameterFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(1).succeeded(1));
        Observation observation = Harness.onlyObservation();
        assertSame(observation.driver.proxy, observation.lens.driver());
        assertEquals(TraceStatus.PASSED, observation.session.metadata().status());
    }

    @Test
    void parameterizedAndRepeatedTestsUseIndependentInvocationState() {
        execute(ParameterizedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(2));
        assertIndependentObservations(2);

        Harness.reset();
        execute(RepeatedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(3).succeeded(3));
        assertIndependentObservations(3);
    }

    @Test
    void nestedTestsDoNotShareState() {
        execute(NestedFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(2));
        assertIndependentObservations(2);
    }

    @Test
    void oneExtensionObjectIsSafeForParallelInvocations() {
        Harness.parallelBarrier = new CountDownLatch(2);
        executeParallel(ParallelFixture.class).testEvents()
                .assertStatistics(stats -> stats.started(2).succeeded(2));
        assertIndependentObservations(2);
        assertNotEquals(Harness.observations.get(0).driver.proxy, Harness.observations.get(1).driver.proxy);
    }

    @Test
    void disabledTestCreatesNeitherDriverNorReport() {
        long reportsBefore = reportCount();
        execute(DisabledFixture.class).testEvents()
                .assertStatistics(stats -> stats.skipped(1));
        assertTrue(Harness.drivers.isEmpty());
        assertEquals(reportsBefore, reportCount());
    }

    private static EngineExecutionResults execute(Class<?> fixture) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(fixture))
                .execute();
    }

    private static EngineExecutionResults executeParallel(Class<?> fixture) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(fixture))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                .execute();
    }

    private static void assertIndependentObservations(int count) {
        assertEquals(count, Harness.drivers.size());
        assertEquals(count, Harness.observations.size());
        assertEquals(count, Harness.observations.stream().map(o -> o.session.id()).distinct().count());
        assertEquals(count, Harness.observations.stream().map(o -> o.session.metadata().name()).distinct().count());
        Harness.observations.forEach(observation -> {
            assertSame(observation.driver.proxy, observation.lens.driver());
            assertEquals(TraceStatus.PASSED, observation.session.metadata().status());
            assertReportsExist(observation.session);
        });
    }

    private static TraceEvent onlyFinishedEvent(UiTestLensSession session) {
        List<TraceEvent> events = session.events().stream()
                .filter(event -> event.type() == TraceEventType.SESSION_FINISHED)
                .toList();
        assertEquals(1, events.size());
        return events.get(0);
    }

    private static void assertReportsExist(UiTestLensSession session) {
        Path directory = Harness.outputRoot.resolve(sanitize(session.metadata().name())).resolve(session.id());
        assertTrue(Files.isRegularFile(directory.resolve("trace.json")), directory.toString());
        assertTrue(Files.isRegularFile(directory.resolve("report.html")), directory.toString());
    }

    private static long reportCount() {
        if (!Files.exists(Harness.outputRoot)) {
            return 0;
        }
        try (var paths = Files.walk(Harness.outputRoot)) {
            return paths.filter(path -> path.getFileName().toString().equals("trace.json")).count();
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }

    private static String sanitize(String value) {
        String safe = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-").replaceAll("-+", "-")
                .replaceAll("(^[-.]+|[-.]+$)", "");
        return safe.isBlank() ? "session" : safe;
    }

    static final class PassedFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @Test
        void passes(WebDriver driver, TestLens lens) {
            Harness.observe("passed", driver, lens);
        }
    }

    static final class FailedFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @Test
        void fails(WebDriver driver, TestLens lens) {
            Harness.observe("failed", driver, lens);
            throw Harness.testFailure;
        }
    }

    static final class AbortedFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @Test
        void aborts(WebDriver driver, TestLens lens) {
            Harness.observe("aborted", driver, lens);
            try {
                Assumptions.assumeTrue(false, "required environment is absent");
            } catch (TestAbortedException aborted) {
                Harness.abortedFailure = aborted;
                throw aborted;
            }
        }
    }

    static final class SetupFailureFixture {
        @RegisterExtension
        static final TestLensExtension LENS = TestLensExtension.builder(Harness::newDriver)
                .lensOptions(Harness.options)
                .sessionName(context -> {
                    throw Harness.setupFailure;
                })
                .build();

        @Test
        void neverRuns() {
            throw new AssertionError("test body must not run");
        }
    }

    static final class ParameterFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @Test
        void injectsOnlySupportedTypes(TestInfo info, WebDriver driver, TestLens lens) {
            assertFalse(info.getDisplayName().isBlank());
            Harness.observe("parameters", driver, lens);
        }
    }

    static final class NullDriverFixture {
        @RegisterExtension
        static final TestLensExtension LENS = TestLensExtension.builder(() -> null).build();

        @Test
        void neverRuns() {
            throw new AssertionError("test body must not run");
        }
    }

    static final class ParameterizedFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @ParameterizedTest(name = "value {0}")
        @ValueSource(strings = {"alpha", "beta"})
        void values(String value, WebDriver driver, TestLens lens) {
            Harness.observe(value, driver, lens);
        }
    }

    static final class RepeatedFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @RepeatedTest(3)
        void repetitions(WebDriver driver, TestLens lens) {
            Harness.observe("repeat", driver, lens);
        }
    }

    static final class NestedFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @Nested
        class First {
            @Test
            void first(WebDriver driver, TestLens lens) {
                Harness.observe("nested-first", driver, lens);
            }
        }

        @Nested
        class Second {
            @Test
            void second(WebDriver driver, TestLens lens) {
                Harness.observe("nested-second", driver, lens);
            }
        }
    }

    @Execution(ExecutionMode.CONCURRENT)
    static final class ParallelFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @Test
        void first(WebDriver driver, TestLens lens) throws Exception {
            Harness.awaitPeers();
            Harness.observe("parallel-first", driver, lens);
        }

        @Test
        void second(WebDriver driver, TestLens lens) throws Exception {
            Harness.awaitPeers();
            Harness.observe("parallel-second", driver, lens);
        }
    }

    static final class DisabledFixture {
        @RegisterExtension
        static final TestLensExtension LENS = Harness.extension();

        @Disabled("contract")
        @Test
        void disabled(WebDriver driver, TestLens lens) {
            Harness.observe("disabled", driver, lens);
        }
    }

    static final class PolicyFixture {
        @RegisterExtension
        static final TestLensExtension LENS = TestLensExtension.builder(Harness::newDriver)
                .lensOptions(TestLensOptions.builder().outputRoot(Harness.outputRoot)
                        .screenshotOnFailure(false)
                        .retryOutcomePolicy(RetryOutcomePolicy.FAIL_ON_ANY_RETRY).build())
                .build();

        @Test
        void policyFailure(WebDriver driver, TestLens lens) {
            Harness.observe("policy", driver, lens);
            lens.session().orElseThrow().addEvent(TraceEvent.builder(TraceEventType.RETRY, TraceStatus.WARNING, "retry")
                    .duration(Duration.ofMillis(1)).build());
        }
    }

    private static final class Harness {
        private static final Path outputRoot = Path.of("target", "junit5-extension-test-" + UUID.randomUUID());
        private static final TestLensOptions options = TestLensOptions.builder()
                .outputRoot(outputRoot)
                .screenshotOnFailure(false)
                .build();
        private static final List<TrackingDriver> drivers = new CopyOnWriteArrayList<>();
        private static final List<Observation> observations = new CopyOnWriteArrayList<>();
        private static final AtomicBoolean quitFailure = new AtomicBoolean();
        private static volatile AssertionError testFailure;
        private static volatile RuntimeException setupFailure;
        private static volatile TestAbortedException abortedFailure;
        private static volatile CountDownLatch parallelBarrier;

        private static TestLensExtension extension() {
            return TestLensExtension.builder(Harness::newDriver).lensOptions(options).build();
        }

        private static WebDriver newDriver() {
            TrackingDriver driver = new TrackingDriver(quitFailure.get());
            drivers.add(driver);
            return driver.proxy;
        }

        private static void observe(String label, WebDriver driver, TestLens lens) {
            TrackingDriver tracked = drivers.stream()
                    .filter(candidate -> candidate.proxy == driver)
                    .findFirst().orElseThrow();
            observations.add(new Observation(label, tracked, lens, lens.session().orElseThrow()));
        }

        private static void awaitPeers() throws Exception {
            CountDownLatch barrier = parallelBarrier;
            barrier.countDown();
            assertTrue(barrier.await(5, TimeUnit.SECONDS), "parallel invocations did not rendezvous");
        }

        private static Observation onlyObservation() {
            assertEquals(1, observations.size());
            return observations.get(0);
        }

        private static void reset() {
            drivers.clear();
            observations.clear();
            quitFailure.set(false);
            testFailure = null;
            setupFailure = null;
            abortedFailure = null;
            parallelBarrier = null;
        }
    }

    private record Observation(String label, TrackingDriver driver, TestLens lens, UiTestLensSession session) {
    }

    private static final class TrackingDriver {
        private final AtomicInteger quitCalls = new AtomicInteger();
        private final boolean failQuit;
        private final WebDriver proxy;

        private TrackingDriver(boolean failQuit) {
            this.failQuit = failQuit;
            this.proxy = (WebDriver) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                    (ignored, method, arguments) -> {
                        if (method.getName().equals("quit")) {
                            quitCalls.incrementAndGet();
                            if (this.failQuit) {
                                throw new IllegalStateException("quit failed");
                            }
                            return null;
                        }
                        if (method.getName().equals("executeScript") || method.getName().equals("executeAsyncScript")) {
                            return null;
                        }
                        if (method.getName().equals("findElements")) {
                            return List.<WebElement>of();
                        }
                        if (method.getName().equals("findElement")) {
                            throw new org.openqa.selenium.NoSuchElementException(String.valueOf(arguments[0]));
                        }
                        if (method.getName().equals("getWindowHandles")) {
                            return Set.of("window");
                        }
                        if (method.getName().equals("getWindowHandle")) {
                            return "window";
                        }
                        Class<?> returnType = method.getReturnType();
                        if (returnType == boolean.class) return false;
                        if (returnType == int.class) return 0;
                        if (returnType == long.class) return 0L;
                        if (returnType == double.class) return 0D;
                        if (returnType == String.class) return "";
                        return null;
                    });
        }
    }
}
