package io.github.testlens.junit5;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

class ManagedTestStateExtensionTest {
    @BeforeEach
    void reset() { Harness.reset(); }

    @Test
    void parameterizedParallelInvocationsIsolateScenarioAndShareOneSuiteValue() {
        executeParallel(ParallelParameters.class).testEvents()
                .assertStatistics(stats -> stats.started(10).succeeded(10));
        assertEquals(10, Harness.scenarioValues.size());
        assertEquals(10, Harness.sessionIds.size());
        assertEquals(1, Harness.suiteInitializers.get());
        assertEquals(1, Harness.suiteValues.size());
        assertEquals(10, Harness.resourceCleanups.get());
        assertEquals(10, Harness.driverQuits.get());

        Object firstRunValue = Harness.suiteValues.iterator().next();
        Harness.reset();
        execute(OneInvocation.class).testEvents().assertStatistics(stats -> stats.succeeded(1));
        assertEquals(1, Harness.suiteInitializers.get());
        assertNotSame(firstRunValue, Harness.suiteValues.iterator().next());
    }

    @Test
    void setupAndTestFailuresStillCleanResourcesAndKeepPrimaryFailure() {
        EngineExecutionResults setup = execute(FailingBeforeEach.class);
        setup.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
        assertEquals(1, Harness.resourceCleanups.get());

        Harness.reset();
        EngineExecutionResults body = execute(FailingBodyAndCleanup.class);
        body.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
        Throwable primary = body.testEvents().failed().list().get(0)
                .getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow();
        assertEquals("PRIMARY", primary.getMessage());
        assertEquals(1, primary.getSuppressed().length);
        assertEquals(1, Harness.resourceCleanups.get());
    }

    @Test
    void cleanupFailureTurnsSuccessfulInvocationIntoFailure() {
        EngineExecutionResults results = execute(PassingBodyFailingCleanup.class);
        results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
        Throwable failure = results.testEvents().failed().list().get(0)
                .getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow();
        assertTrue(failure.getMessage().contains("resources could not be cleaned"));
    }

    @Test
    void abortedAndAfterEachFailuresStillCleanRegisteredResources() {
        EngineExecutionResults aborted = execute(AbortedInvocation.class);
        aborted.testEvents().assertStatistics(stats -> stats.started(1).aborted(1));
        assertEquals(1, Harness.resourceCleanups.get());

        Harness.reset();
        EngineExecutionResults afterEach = execute(FailingAfterEach.class);
        afterEach.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
        assertEquals(1, Harness.resourceCleanups.get());
    }

    private static EngineExecutionResults execute(Class<?> fixture) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(fixture)).execute();
    }

    private static EngineExecutionResults executeParallel(Class<?> fixture) {
        return EngineTestKit.engine("junit-jupiter").selectors(selectClass(fixture))
                .configurationParameter("junit.jupiter.execution.parallel.enabled", "true")
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .execute();
    }

    @Execution(ExecutionMode.CONCURRENT)
    static class ParallelParameters {
        @RegisterExtension static final TestLensExtension LENS = Harness.extension();

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
        void invocation(int row, TestLens lens) {
            assertTrue(lens.scenarioState().get("row", Integer.class).isEmpty());
            lens.scenarioState().put("row", row);
            assertEquals(row, lens.scenarioState().require("row", Integer.class));
            Harness.scenarioValues.add(row);
            Harness.sessionIds.add(lens.session().orElseThrow().id());
            Object suite = lens.suiteState().computeIfAbsent("shared", Object.class, () -> {
                Harness.suiteInitializers.incrementAndGet();
                return new Object();
            });
            Harness.suiteValues.add(suite);
            lens.resources().register("row-resource", row, ignored -> Harness.resourceCleanups.incrementAndGet());
        }
    }

    static class OneInvocation {
        @RegisterExtension static final TestLensExtension LENS = Harness.extension();
        @org.junit.jupiter.api.Test void one(TestLens lens) {
            Harness.suiteValues.add(lens.suiteState().computeIfAbsent("shared", Object.class, () -> {
                Harness.suiteInitializers.incrementAndGet();
                return new Object();
            }));
        }
    }

    static class FailingBeforeEach {
        @RegisterExtension static final TestLensExtension LENS = Harness.extension();
        @BeforeEach void setup(TestLens lens) {
            lens.resources().register("setup-resource", new Object(), ignored -> Harness.resourceCleanups.incrementAndGet());
            throw new IllegalStateException("SETUP");
        }
        @org.junit.jupiter.api.Test void neverRuns() { fail("must not run"); }
    }

    static class FailingBodyAndCleanup {
        @RegisterExtension static final TestLensExtension LENS = Harness.extension();
        @org.junit.jupiter.api.Test void fails(TestLens lens) {
            lens.resources().register("body-resource", new Object(), ignored -> {
                Harness.resourceCleanups.incrementAndGet();
                throw new IllegalStateException("CLEANUP");
            });
            throw new AssertionError("PRIMARY");
        }
    }

    static class PassingBodyFailingCleanup {
        @RegisterExtension static final TestLensExtension LENS = Harness.extension();
        @org.junit.jupiter.api.Test void passes(TestLens lens) {
            lens.resources().register("resource", new Object(), ignored -> { throw new IllegalStateException("CLEANUP"); });
        }
    }

    static class AbortedInvocation {
        @RegisterExtension static final TestLensExtension LENS = Harness.extension();
        @org.junit.jupiter.api.Test void aborts(TestLens lens) {
            lens.resources().register("resource", new Object(), ignored -> Harness.resourceCleanups.incrementAndGet());
            Assumptions.assumeTrue(false, "intentional abort");
        }
    }

    static class FailingAfterEach {
        @RegisterExtension static final TestLensExtension LENS = Harness.extension();
        @org.junit.jupiter.api.Test void passes(TestLens lens) {
            lens.resources().register("resource", new Object(), ignored -> Harness.resourceCleanups.incrementAndGet());
        }
        @AfterEach void failsAfterEach() { throw new IllegalStateException("AFTER"); }
    }

    private static final class Harness {
        static final AtomicInteger suiteInitializers = new AtomicInteger();
        static final AtomicInteger resourceCleanups = new AtomicInteger();
        static final AtomicInteger driverQuits = new AtomicInteger();
        static final Set<Object> suiteValues = ConcurrentHashMap.newKeySet();
        static final Set<Integer> scenarioValues = ConcurrentHashMap.newKeySet();
        static final List<String> sessionIds = new CopyOnWriteArrayList<>();

        static void reset() {
            suiteInitializers.set(0);
            resourceCleanups.set(0);
            driverQuits.set(0);
            suiteValues.clear();
            scenarioValues.clear();
            sessionIds.clear();
        }

        static TestLensExtension extension() {
            return TestLensExtension.builder(Harness::driver)
                    .lensOptions(TestLensOptions.builder()
                            .outputRoot(Path.of("target", "managed-state-junit"))
                            .screenshotOnFailure(false)
                            .failureBundleOptions(io.github.testlens.selenium.evidence.FailureBundleOptions.builder()
                                    .enabled(false).build())
                            .build())
                    .build();
        }

        static WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(Harness.class.getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                        if (method.getName().equals("quit")) { driverQuits.incrementAndGet(); return null; }
                        if (method.getName().startsWith("execute")) return null;
                        if (method.getName().equals("toString")) return "managed-state-junit-driver";
                        Class<?> type = method.getReturnType();
                        if (!type.isPrimitive()) return null;
                        if (type == boolean.class) return false;
                        if (type == char.class) return '\0';
                        return 0;
                    });
        }
    }
}
