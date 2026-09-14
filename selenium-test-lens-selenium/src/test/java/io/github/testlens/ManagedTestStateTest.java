package io.github.testlens;

import io.github.testlens.core.trace.TraceStatus;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ManagedTestStateTest {
    @Test
    void scenarioStateProvidesTypedOperationsWithoutExposingValuesInErrors() {
        ScenarioStateManager state = new ScenarioStateManager();
        state.put("orderId", "order-secret");
        assertEquals(Optional.of("order-secret"), state.get("orderId", String.class));
        assertEquals("order-secret", state.require("orderId", String.class));
        assertTrue(state.contains("orderId"));
        assertEquals(1, state.size());
        assertTrue(state.remove("orderId"));
        assertFalse(state.remove("orderId"));
        assertEquals(Optional.empty(), state.get("orderId", String.class));
        TestStateException missing = assertThrows(TestStateException.class,
                () -> state.require("orderId", String.class));
        assertFalse(missing.getMessage().contains("orderId"));

        state.put("private-key", "private-value");
        TestStateException mismatch = assertThrows(TestStateException.class,
                () -> state.require("private-key", Integer.class));
        assertFalse(mismatch.getMessage().contains("private-key"));
        assertFalse(mismatch.getMessage().contains("private-value"));
    }

    @Test
    void stateRejectsInvalidInputAndClosedAccess() {
        ScenarioStateManager state = new ScenarioStateManager();
        assertThrows(NullPointerException.class, () -> state.put(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> state.put("  ", "x"));
        assertThrows(NullPointerException.class, () -> state.put("x", null));
        assertThrows(NullPointerException.class, () -> state.computeIfAbsent("x", String.class, () -> null));
        state.close();
        assertThrows(TestStateException.class, () -> state.put("x", "y"));
    }

    @Test
    void computeIfAbsentPublishesOneSuccessfulValueAndDoesNotCacheFailure() throws Exception {
        SuiteStateManager state = new SuiteStateManager();
        AtomicInteger calls = new AtomicInteger();
        var pool = Executors.newFixedThreadPool(32);
        try {
            CountDownLatch start = new CountDownLatch(1);
            var futures = new ArrayList<java.util.concurrent.Future<Object>>();
            for (int index = 0; index < 32; index++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return state.computeIfAbsent("tenant", Object.class, () -> {
                        calls.incrementAndGet();
                        return new Object();
                    });
                }));
            }
            start.countDown();
            Object expected = futures.get(0).get(5, TimeUnit.SECONDS);
            for (var future : futures) assertSame(expected, future.get(5, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
        } finally {
            pool.shutdownNow();
            state.close();
        }

        ScenarioStateManager retryable = new ScenarioStateManager();
        AtomicInteger attempts = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> retryable.computeIfAbsent("x", String.class, () -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("factory failure");
        }));
        assertEquals("ok", retryable.computeIfAbsent("x", String.class, () -> {
            attempts.incrementAndGet();
            return "ok";
        }));
        assertEquals(2, attempts.get());
    }

    @Test
    void resourcesAreLifoExactlyOnceAndContinueAfterFailures() {
        ScenarioResourceManager resources = new ScenarioResourceManager();
        List<String> order = new ArrayList<>();
        resources.register("company", "company", value -> order.add(value));
        resources.register("user", "user", value -> { order.add(value); throw new IllegalStateException("user cleanup"); });
        resources.register("order", "order", value -> { order.add(value); throw new IllegalArgumentException("order cleanup"); });

        List<Throwable> failures = resources.cleanup();
        assertEquals(List.of("order", "user", "company"), order);
        assertEquals(2, failures.size());
        assertTrue(resources.cleanup().isEmpty());
        assertThrows(TestStateException.class,
                () -> resources.register("late", new Object(), ignored -> { }));
    }

    @Test
    void failedFactoryRegistersNoCleanup() {
        ScenarioResourceManager resources = new ScenarioResourceManager();
        AtomicInteger cleanups = new AtomicInteger();
        assertThrows(Exception.class, () -> resources.create("resource", () -> {
            throw new Exception("create failed");
        }, ignored -> cleanups.incrementAndGet()));
        assertEquals(0, resources.size());
        assertTrue(resources.cleanup().isEmpty());
        assertEquals(0, cleanups.get());
    }

    @Test
    void manualSessionsIsolateStateAndCleanupOnEveryOutcome() {
        TestLens lens = TestLens.attach(driver(), options("manual-isolation"));
        lens.startSession("first");
        lens.scenarioState().put("id", "first");
        AtomicInteger cleaned = new AtomicInteger();
        lens.resources().register("first", new Object(), ignored -> cleaned.incrementAndGet());
        lens.finishPassed();
        assertEquals(1, cleaned.get());
        assertThrows(TestStateException.class, () -> lens.scenarioState().put("late", "value"));

        lens.startSession("second");
        assertEquals(Optional.empty(), lens.scenarioState().get("id", String.class));
        lens.resources().register("second", new Object(), ignored -> cleaned.incrementAndGet());
        lens.finishSkipped("skip");
        assertEquals(2, cleaned.get());
        assertEquals(0, ManagedStateSupport.activeScenarioScopes());
    }

    @Test
    void primaryFailureRemainsPrimaryAndCleanupFailureIsSuppressed() {
        TestLens lens = TestLens.attach(driver(), options("primary"));
        lens.startSession("primary");
        AssertionError primary = new AssertionError("PRIMARY");
        lens.resources().register("secret-resource", new Object(), ignored -> {
            throw new IllegalStateException("CLEANUP");
        });
        TestLensFinalizationResult result = lens.finishFailed(primary);
        assertEquals(TraceStatus.FAILED, result.session().metadata().status());
        assertEquals(1, primary.getSuppressed().length);
        assertInstanceOf(TestStateException.class, primary.getSuppressed()[0]);
        assertFalse(result.diagnosticFailures().isEmpty());
    }

    @Test
    void cleanupFailureTurnsPassIntoFailureAndStillRunsEveryCleanup() {
        TestLens lens = TestLens.attach(driver(), options("cleanup-failure"));
        lens.startSession("cleanup-failure");
        AtomicInteger cleanups = new AtomicInteger();
        lens.resources().register("first", new Object(), ignored -> cleanups.incrementAndGet());
        lens.resources().register("second", new Object(), ignored -> {
            cleanups.incrementAndGet();
            throw new IllegalStateException("cleanup secret");
        });
        TestStateException failure = assertThrows(TestStateException.class, lens::finishPassed);
        assertEquals(2, cleanups.get());
        assertEquals("One or more scenario resources could not be cleaned", failure.getMessage());
        assertEquals(TraceStatus.FAILED, lens.session().orElseThrow().metadata().status());
        assertEquals(0, ManagedStateSupport.activeScenarioScopes());
    }

    @Test
    void cleanupCannotRegisterAnotherResourceThroughTheLens() {
        TestLens lens = TestLens.attach(driver(), options("late-registration"));
        lens.startSession("late-registration");
        AtomicInteger rejected = new AtomicInteger();
        lens.resources().register("first", new Object(), ignored -> {
            assertThrows(TestStateException.class,
                    () -> lens.resources().register("late", new Object(), value -> { }));
            rejected.incrementAndGet();
        });
        lens.finishPassed();
        assertEquals(1, rejected.get());
        assertEquals(0, ManagedStateSupport.activeScenarioScopes());
    }

    @Test
    void concurrentFinalizersCleanEachResourceExactlyOnce() throws Exception {
        TestLens lens = TestLens.attach(driver(), options("concurrent-finalizers"));
        lens.startSession("concurrent-finalizers");
        AtomicInteger cleanups = new AtomicInteger();
        lens.resources().register("resource", new Object(), ignored -> cleanups.incrementAndGet());
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(lens::finishPassed);
            var second = pool.submit(lens::finishPassed);
            assertSame(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            assertEquals(1, cleanups.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void explicitRunScopesShareWithinRunAndIsolateAcrossRuns() {
        Object firstValue;
        try (TestRunScope first = TestRunScope.open()) {
            TestLens a = first.attach(driver(), options("run-a"));
            TestLens b = first.attach(driver(), options("run-b"));
            a.startSession("a");
            b.startSession("b");
            firstValue = a.suiteState().computeIfAbsent("tenant", Object.class, Object::new);
            assertSame(firstValue, b.suiteState().require("tenant", Object.class));
            a.finishPassed();
            b.finishPassed();
        }
        try (TestRunScope second = TestRunScope.open()) {
            assertEquals(Optional.empty(), second.suiteState().get("tenant", Object.class));
        }
        assertEquals(0, ManagedStateSupport.activeSuiteScopes());
    }

    @Test
    void thirtyTwoParallelScenarioScopesWithTheSameKeyRemainIsolated() throws Exception {
        var pool = Executors.newFixedThreadPool(32);
        List<ScenarioStateManager> states = new ArrayList<>();
        for (int index = 0; index < 32; index++) states.add(new ScenarioStateManager());
        CountDownLatch start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int index = 0; index < states.size(); index++) {
                int expected = index;
                ScenarioStateManager state = states.get(index);
                futures.add(pool.submit(() -> {
                    start.await();
                    state.put("same-key", expected);
                    return state.require("same-key", Integer.class);
                }));
            }
            start.countDown();
            for (int index = 0; index < futures.size(); index++) {
                assertEquals(index, futures.get(index).get(5, TimeUnit.SECONDS));
            }
        } finally {
            states.forEach(ScenarioStateManager::close);
            pool.shutdownNow();
        }
    }

    @Test
    void plainAttachRequiresExplicitRunScopeForSuiteState() {
        TestLens lens = TestLens.attach(driver(), options("no-run"));
        lens.startSession("no-run");
        assertThrows(TestStateException.class, lens::suiteState);
        lens.finishPassed();
    }

    @Test
    void stateKeysValuesResourcesAndCallbacksAreNotAutomaticallyEmitted() throws Exception {
        String password = "AUTH_PASSWORD_MUST_NOT_APPEAR_7F3A";
        String token = "AUTH_TOKEN_MUST_NOT_APPEAR_8C2B";
        String email = "john.smith@example.invalid";
        AtomicInteger toStringCalls = new AtomicInteger();
        Object guardedValue = new Object() {
            @Override public String toString() {
                toStringCalls.incrementAndGet();
                return token;
            }
        };
        Path root = Path.of("target", "managed-state-test", "no-evidence");
        TestLens lens = TestLens.attach(driver(), TestLensOptions.builder().outputRoot(root)
                .screenshotOnFailure(false)
                .failureBundleOptions(io.github.testlens.selenium.evidence.FailureBundleOptions.builder()
                        .enabled(false).build())
                .build());
        var session = lens.startSession("safe session");
        lens.scenarioState().put(email, guardedValue);
        lens.resources().register(password, guardedValue, ignored -> { });
        TestLensFinalizationResult result = lens.finishPassed();
        assertEquals(0, toStringCalls.get());
        String exported = Files.readString(result.jsonReport()) + Files.readString(result.htmlReport());
        assertFalse(exported.contains(password));
        assertFalse(exported.contains(token));
        assertFalse(exported.contains(email));
        assertEquals(TraceStatus.PASSED, session.metadata().status());
    }

    private static TestLensOptions options(String name) {
        return TestLensOptions.builder().outputRoot(Path.of("target", "managed-state-test", name))
                .screenshotOnFailure(false)
                .failureBundleOptions(io.github.testlens.selenium.evidence.FailureBundleOptions.builder()
                        .enabled(false).build())
                .build();
    }

    private static WebDriver driver() {
        return (WebDriver) Proxy.newProxyInstance(ManagedTestStateTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeScript") || method.getName().equals("executeAsyncScript")) return null;
                    if (method.getName().equals("toString")) return "managed-state-driver";
                    Class<?> type = method.getReturnType();
                    if (!type.isPrimitive()) return null;
                    if (type == boolean.class) return false;
                    if (type == char.class) return '\0';
                    return 0;
                });
    }
}
