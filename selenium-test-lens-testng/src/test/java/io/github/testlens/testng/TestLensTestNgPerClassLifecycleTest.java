package io.github.testlens.testng;

import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.IRetryAnalyzer;
import org.testng.IClassListener;
import org.testng.ITestClass;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.TestNG;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Listeners;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlTest;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLensTestNgPerClassLifecycleTest {
    private String symmetricBefore;

    @BeforeEach
    void reset() {
        Harness.reset();
        symmetricBefore = System.getProperty("testng.listener.execution.symmetric");
    }

    @AfterEach
    void restoreSymmetricProperty() {
        if (symmetricBefore == null) System.clearProperty("testng.listener.execution.symmetric");
        else System.setProperty("testng.listener.execution.symmetric", symmetricBefore);
    }

    @Test
    void inheritedMethodEnvelopeUsesOneDriverAndIndependentSessions() {
        run(SharedFixture.class);

        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.drivers.size());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertEquals(2, Harness.bodySessions.size());
        assertEquals(2, Harness.bodySessions.stream().distinct().count());
        assertEquals(1, Harness.bodyDrivers.stream().distinct().count());
        assertEquals(4, Harness.beforeSessions.size());
        assertEquals(4, Harness.afterSessions.size());
        assertEquals(Set.copyOf(Harness.bodySessions), Set.copyOf(Harness.afterSessions));
        assertTrue(Harness.events.stream().allMatch(event -> !event.contains("context-missing")));
        assertEquals(List.of(TraceStatus.PASSED, TraceStatus.PASSED),
                Harness.sessions.stream().map(session -> session.metadata().status()).toList());
    }

    @Test
    void noMethodHooksStillCreatesIndependentInvocationSessions() {
        run(NoHooksFixture.class);

        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertEquals(2, Harness.sessions.size());
        assertEquals(2, Harness.sessions.stream().map(UiTestLensSession::id).distinct().count());
    }

    @Test
    void perClassNativeObservationIsBoundToEachFreshLensInvocation() {
        run(NativeObservationFixture.class);

        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertEquals(2, Harness.sessions.size());
        assertEquals(2, Harness.observedDrivers.size());
        org.junit.jupiter.api.Assertions.assertNotSame(
                Harness.observedDrivers.get(0), Harness.observedDrivers.get(1));
        assertTrue(Harness.sessions.stream().allMatch(session -> session.events().stream()
                .filter(event -> "selenium.get".equals(event.attributes().get("action"))).count() == 2));
    }

    @Test
    void ownersDoNotLeakAcrossXmlTestsOrConsecutiveSuitesInOneJvm() {
        XmlSuite suite = new XmlSuite();
        suite.setName("two-xml-tests");
        XmlTest first = new XmlTest(suite);
        first.setName("first-context");
        first.setXmlClasses(List.of(new XmlClass(NoHooksFixture.class)));
        XmlTest second = new XmlTest(suite);
        second.setName("second-context");
        second.setXmlClasses(List.of(new XmlClass(NoHooksFixture.class)));
        execute(testng -> testng.setXmlSuites(List.of(suite)));

        assertEquals(2, Harness.created.get());
        assertTrue(Harness.drivers.stream().allMatch(driver -> driver.quitCalls.get() == 1));

        run(NoHooksFixture.class);
        assertEquals(3, Harness.created.get());
        assertTrue(Harness.drivers.stream().allMatch(driver -> driver.quitCalls.get() == 1));
    }

    @Test
    void onlyForGroupsUsesTheActualApplicableAfterMethodBoundary() {
        run(GroupHookFixture.class);
        assertEquals(1, Harness.created.get());
        assertEquals(2, Harness.sessions.size());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertEquals(1, Harness.events.stream().filter("fast-only-after"::equals).count());
    }

    @Test
    void teardownFailureFinalizesThatInvocationFailedAndNextInvocationIsFresh() {
        execute(testng -> {
            testng.setTestClasses(new Class<?>[]{TeardownFailureFixture.class});
            testng.setConfigFailurePolicy(XmlSuite.FailurePolicy.CONTINUE);
        });

        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertEquals(2, Harness.sessions.size());
        assertTrue(Harness.sessions.stream().anyMatch(s -> s.metadata().status() == TraceStatus.FAILED),
                Harness.sessions.stream().map(s -> s.metadata().status()).toList().toString());
        assertTrue(Harness.sessions.stream().anyMatch(s -> s.metadata().status() == TraceStatus.PASSED));
    }

    @Test
    void setupFailureAndSkipKeepAlwaysRunTeardownInTheCorrectInvocation() {
        execute(testng -> {
            testng.setTestClasses(new Class<?>[]{SetupFailureAndSkipFixture.class});
            testng.setConfigFailurePolicy(XmlSuite.FailurePolicy.CONTINUE);
        });

        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertTrue(Harness.events.contains("always-run-after"));
        assertTrue(Harness.sessions.stream().anyMatch(s -> s.metadata().status() == TraceStatus.FAILED));
        assertTrue(Harness.sessions.stream().anyMatch(s -> s.metadata().status() == TraceStatus.SKIPPED));
    }

    @Test
    void setupFailureWithoutAfterMethodFinalizesAtTheConfigurationBoundary() {
        run(SetupFailureWithoutAfterFixture.class);

        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertEquals(1, Harness.sessionNames.size());
        assertTrue(Harness.failures.stream().anyMatch(result -> result.getThrowable() != null
                && String.valueOf(result.getThrowable().getMessage()).contains("setup failed without teardown")));
    }

    @Test
    void retryAndSequentialDataRowsReuseDriverButNotLogicalSessions() {
        run(DataAndRetryFixture.class);

        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertEquals(4, Harness.sessions.size());
        assertEquals(4, Harness.sessions.stream().map(UiTestLensSession::id).distinct().count());
        assertTrue(Harness.sessionNames.stream().noneMatch(name -> name.contains("secret")));
    }

    @Test
    void classListenerOrderingNeverQuitsBeforeInheritedAfterClassHooks() {
        for (boolean symmetric : List.of(false, true)) {
            Harness.reset();
            System.setProperty("testng.listener.execution.symmetric", Boolean.toString(symmetric));
            execute(testng -> {
                testng.setTestClasses(new Class<?>[]{ClassTeardownFixture.class});
                testng.addListener(new IClassListener() {
                    @Override public void onAfterClass(ITestClass testClass) {
                        Harness.classTeardownEvents.add("listener-after-class:"
                                + Harness.drivers.get(0).quitCalls.get());
                    }
                });
            });

            List<String> expected = symmetric
                    ? List.of("derived-after-class:0", "base-after-class:0", "listener-after-class:0")
                    : List.of("listener-after-class:0", "derived-after-class:0", "base-after-class:0");
            assertEquals(expected, Harness.classTeardownEvents, "symmetric=" + symmetric);
            assertEquals(1, Harness.drivers.get(0).quitCalls.get(), "symmetric=" + symmetric);
        }
    }

    @Test
    void factoryInstancesOwnDifferentDriversAndCloseIndependently() {
        execute(testng -> {
            testng.setTestClasses(new Class<?>[]{FactoryProducer.class});
            testng.setGroupByInstances(true);
        });

        assertEquals(2, Harness.created.get());
        assertEquals(2, Harness.drivers.size());
        assertTrue(Harness.drivers.stream().allMatch(driver -> driver.quitCalls.get() == 1));
        assertEquals(4, Harness.sessions.size());
        assertEquals(2, Harness.instanceDrivers.size());
        assertNotEquals(Harness.instanceDrivers.get("one"), Harness.instanceDrivers.get("two"));
        assertTrue(Harness.failures.isEmpty(), failureMessages());

        Harness.reset();
        execute(testng -> {
            testng.setTestClasses(new Class<?>[]{FactoryProducer.class});
            testng.setParallel(XmlSuite.ParallelMode.INSTANCES);
            testng.setThreadCount(2);
            testng.setGroupByInstances(true);
        });
        assertEquals(2, Harness.created.get(), Harness.failures.stream()
                .map(result -> String.valueOf(result.getThrowable())).toList().toString());
        assertTrue(Harness.drivers.stream().allMatch(driver -> driver.quitCalls.get() == 1));
        assertEquals(2, Harness.instanceDrivers.size());
        assertTrue(Harness.failures.isEmpty(), failureMessages());
    }

    private static String failureMessages() {
        return Harness.failures.stream().map(result -> String.valueOf(result.getThrowable())).toList().toString();
    }

    @Test
    void parallelMethodsAndParallelDataProviderFailBeforeDriverCreation() {
        runParallel(ParallelMethodsFixture.class);
        assertEquals(0, Harness.created.get());
        assertTrue(Harness.failures.stream().anyMatch(TestLensTestNgPerClassLifecycleTest::parallelMessage));

        Harness.reset();
        run(ParallelDataFixture.class);
        assertEquals(0, Harness.created.get());
        assertTrue(Harness.failures.stream().anyMatch(TestLensTestNgPerClassLifecycleTest::parallelMessage));
    }

    @Test
    void definitivelyLostSessionIsNotSilentlyReplaced() {
        run(BrokenSessionFixture.class);
        assertEquals(1, Harness.created.get());
        assertEquals(1, Harness.sessions.size());
        assertEquals(1, Harness.drivers.get(0).quitCalls.get());
        assertTrue(Harness.failures.stream().anyMatch(result -> result.getThrowable() != null
                && String.valueOf(result.getThrowable().getMessage()).contains("will not silently replace")));
    }

    @Test
    void perMethodDefaultStillCreatesAndQuitsPerPhysicalInvocation() {
        run(DefaultScopeFixture.class);

        assertEquals(2, Harness.created.get());
        assertEquals(2, Harness.drivers.size());
        assertTrue(Harness.drivers.stream().allMatch(driver -> driver.quitCalls.get() == 1));
        assertEquals(2, Harness.sessions.size());
    }

    private static boolean parallelMessage(ITestResult result) {
        Throwable failure = result.getThrowable();
        return failure != null && String.valueOf(failure.getMessage()).contains("PER_CLASS conflicts");
    }

    private static void run(Class<?> fixture) {
        execute(testng -> testng.setTestClasses(new Class<?>[]{fixture}));
    }

    private static void runParallel(Class<?> fixture) {
        execute(testng -> {
            testng.setTestClasses(new Class<?>[]{fixture});
            testng.setParallel(XmlSuite.ParallelMode.METHODS);
            testng.setThreadCount(2);
        });
    }

    private static void execute(java.util.function.Consumer<TestNG> configure) {
        Collector collector = new Collector();
        TestNG testng = new TestNG(false);
        testng.setUseDefaultListeners(false);
        testng.setOutputDirectory(Harness.output.resolve(UUID.randomUUID().toString()).toString());
        testng.addListener(collector);
        configure.accept(testng);
        testng.run();
        Harness.failures.addAll(collector.failures);
    }

    static class BaseHooks {
        @BeforeMethod(alwaysRun = true)
        public void baseBefore() { Harness.observeBefore("base-before"); }

        @AfterMethod(alwaysRun = true)
        public void baseAfter() { Harness.observeAfter("base-after"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class SharedFixture extends BaseHooks {
        @BeforeMethod(alwaysRun = true)
        public void derivedBefore() { Harness.observeBefore("derived-before"); }

        @org.testng.annotations.Test public void first() { Harness.observeBody("first"); }
        @org.testng.annotations.Test public void second() { Harness.observeBody("second"); }

        @AfterMethod(alwaysRun = true)
        public void derivedAfter() { Harness.observeAfter("derived-after"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class NoHooksFixture {
        @org.testng.annotations.Test public void first() { Harness.observeBody("no-hooks-1"); }
        @org.testng.annotations.Test public void second() { Harness.observeBody("no-hooks-2"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class GroupHookFixture {
        @org.testng.annotations.Test(groups = "fast") public void fast() { Harness.observeBody("fast"); }
        @org.testng.annotations.Test(groups = "slow") public void slow() { Harness.observeBody("slow"); }
        @AfterMethod(alwaysRun = true) public void commonAfter() { Harness.observeAfter("common-after"); }
        @AfterMethod(alwaysRun = true, onlyForGroups = "fast")
        public void fastAfter() { Harness.observeAfter("fast-only-after"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class TeardownFailureFixture {
        private int calls;
        @org.testng.annotations.Test public void first() { Harness.observeBody("teardown-fails"); }
        @org.testng.annotations.Test public void second() { Harness.observeBody("after-failure"); }
        @AfterMethod(alwaysRun = true) public void teardown() {
            Harness.observeAfter("teardown");
            if (calls++ == 0) throw new IllegalStateException("teardown failed");
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class SetupFailureAndSkipFixture {
        private int setupCalls;
        @BeforeMethod(alwaysRun = true) public void setup() {
            TestLensTestNgContext.current();
            if (setupCalls++ == 0) throw new IllegalStateException("setup failed");
        }
        @org.testng.annotations.Test public void blockedBySetup() { Harness.observeBody("blocked"); }
        @org.testng.annotations.Test public void skipped() {
            Harness.observeBody("skip-body");
            throw new SkipException("intentional skip");
        }
        @AfterMethod(alwaysRun = true) public void alwaysRunAfter() {
            Harness.observeAfter("always-run-after");
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class SetupFailureWithoutAfterFixture {
        @BeforeMethod(alwaysRun = true) public void setup() {
            TestLensTestNgContext.current();
            throw new IllegalStateException("setup failed without teardown");
        }
        @org.testng.annotations.Test public void blockedBySetup() { Harness.observeBody("must-not-run"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class DataAndRetryFixture {
        private static final AtomicBoolean FIRST = new AtomicBoolean(true);
        @DataProvider public Object[][] rows() { return new Object[][]{{"secret-a"}, {"secret-b"}}; }
        @org.testng.annotations.Test(dataProvider = "rows")
        public void row(String ignored) { Harness.observeBody("row"); }
        @org.testng.annotations.Test(retryAnalyzer = RetryOnce.class)
        public void retry() {
            Harness.observeBody("retry");
            if (FIRST.getAndSet(false)) throw new AssertionError("first attempt");
        }
    }

    public static class RetryOnce implements IRetryAnalyzer {
        private final AtomicBoolean used = new AtomicBoolean();
        @Override public boolean retry(ITestResult result) { return used.compareAndSet(false, true); }
    }

    static class BaseClassTeardown {
        @AfterClass(alwaysRun = true)
        public void baseClassAfter() { Harness.classTeardown("base-after-class"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class ClassTeardownFixture extends BaseClassTeardown {
        @org.testng.annotations.Test public void test() { Harness.observeBody("class-teardown"); }
        @AfterClass(alwaysRun = true)
        public void derivedClassAfter() { Harness.classTeardown("derived-after-class"); }
    }

    public static class FactoryProducer {
        @Factory public Object[] instances() {
            return new Object[]{new FactoryFixture("one"), new FactoryFixture("two")};
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class FactoryFixture {
        private final String id;
        public FactoryFixture(String id) { this.id = id; }
        @org.testng.annotations.Test public void first() { Harness.observeInstance(id); }
        @org.testng.annotations.Test public void second() { Harness.observeInstance(id); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class ParallelMethodsFixture {
        @org.testng.annotations.Test public void first() { }
        @org.testng.annotations.Test public void second() { }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class ParallelDataFixture {
        @DataProvider(parallel = true) public Object[][] rows() { return new Object[][]{{1}, {2}}; }
        @org.testng.annotations.Test(dataProvider = "rows") public void row(int ignored) { }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class BrokenSessionFixture {
        @org.testng.annotations.Test public void first() {
            Harness.observeBody("lost-session");
            throw new NoSuchSessionException("session is gone");
        }
        @org.testng.annotations.Test(dependsOnMethods = {}, alwaysRun = true) public void second() {
            Harness.observeBody("must-not-run-with-new-driver");
        }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class)
    public static class DefaultScopeFixture {
        @org.testng.annotations.Test public void first() { Harness.observeBody("default-1"); }
        @org.testng.annotations.Test public void second() { Harness.observeBody("default-2"); }
    }

    @Listeners(TestLensTestNgListener.class)
    @TestLensTestNg(factory = FactoryImpl.class, driverScope = DriverScope.PER_CLASS)
    public static class NativeObservationFixture {
        private static WebDriver firstInvocation;

        @org.testng.annotations.Test
        public void first() {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            firstInvocation = context.lens().observeDriver();
            Harness.observedDrivers.add(firstInvocation);
            firstInvocation.get("https://example.test/first");
            Harness.remember(context.session());
        }

        @org.testng.annotations.Test(priority = 1)
        public void second() {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            WebDriver current = context.lens().observeDriver();
            Harness.observedDrivers.add(current);
            firstInvocation.get("https://example.test/stale-view");
            current.get("https://example.test/second");
            Harness.remember(context.session());
        }
    }

    public static class FactoryImpl implements TestLensTestNgFactory {
        @Override public WebDriver createDriver() {
            TrackingDriver driver = new TrackingDriver();
            Harness.created.incrementAndGet();
            Harness.drivers.add(driver);
            return driver.proxy;
        }

        @Override public TestLensOptions lensOptions() {
            return TestLensOptions.builder().outputRoot(Harness.output).screenshotOnFailure(false).build();
        }

        @Override public String sessionName(ITestResult result) {
            String name = result.getMethod().getMethodName() + "-" + UUID.randomUUID().toString().substring(0, 8);
            Harness.sessionNames.add(name);
            return name;
        }
    }

    private static final class Collector implements org.testng.ITestListener {
        private final List<ITestResult> failures = new CopyOnWriteArrayList<>();
        @Override public void onTestFailure(ITestResult result) { failures.add(result); }
        @Override public void onTestSkipped(ITestResult result) {
            if (result.getThrowable() != null) failures.add(result);
        }
    }

    private static final class Harness {
        private static final Path output = Path.of("target", "testng-per-class-" + UUID.randomUUID());
        private static final AtomicInteger created = new AtomicInteger();
        private static final List<TrackingDriver> drivers = new CopyOnWriteArrayList<>();
        private static final List<UiTestLensSession> sessions = new CopyOnWriteArrayList<>();
        private static final List<String> sessionNames = new CopyOnWriteArrayList<>();
        private static final List<String> beforeSessions = new CopyOnWriteArrayList<>();
        private static final List<String> bodySessions = new CopyOnWriteArrayList<>();
        private static final List<String> afterSessions = new CopyOnWriteArrayList<>();
        private static final List<WebDriver> bodyDrivers = new CopyOnWriteArrayList<>();
        private static final List<WebDriver> observedDrivers = new CopyOnWriteArrayList<>();
        private static final List<String> events = new CopyOnWriteArrayList<>();
        private static final List<String> classTeardownEvents = new CopyOnWriteArrayList<>();
        private static final Map<String, WebDriver> instanceDrivers = new java.util.concurrent.ConcurrentHashMap<>();
        private static final List<ITestResult> failures = new CopyOnWriteArrayList<>();

        static void observeBefore(String label) {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            beforeSessions.add(context.session().id());
            events.add(label);
            remember(context.session());
        }

        static void observeBody(String label) {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            bodySessions.add(context.session().id());
            bodyDrivers.add(context.driver());
            events.add(label);
            remember(context.session());
        }

        static void observeAfter(String label) {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            afterSessions.add(context.session().id());
            events.add(label);
            remember(context.session());
        }

        static void observeInstance(String id) {
            TestLensTestNgContext context = TestLensTestNgContext.current();
            WebDriver previous = instanceDrivers.putIfAbsent(id, context.driver());
            if (previous != null) assertSame(previous, context.driver());
            remember(context.session());
        }

        static void classTeardown(String label) {
            classTeardownEvents.add(label + ":" + drivers.get(0).quitCalls.get());
            try { TestLensTestNgContext.current(); events.add("context-leaked-to-class-hook"); }
            catch (IllegalStateException expected) { }
        }

        private static void remember(UiTestLensSession session) {
            if (sessions.stream().noneMatch(existing -> existing == session)) sessions.add(session);
        }

        static void reset() {
            created.set(0); drivers.clear(); sessions.clear(); sessionNames.clear(); beforeSessions.clear();
            bodySessions.clear(); afterSessions.clear(); bodyDrivers.clear(); events.clear();
            observedDrivers.clear();
            classTeardownEvents.clear(); instanceDrivers.clear(); failures.clear();
            DataAndRetryFixture.FIRST.set(true);
        }
    }

    private static final class TrackingDriver {
        private final AtomicInteger quitCalls = new AtomicInteger();
        private final WebDriver proxy = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (ignored, method, arguments) -> {
                    if (method.getName().equals("quit")) { quitCalls.incrementAndGet(); return null; }
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
