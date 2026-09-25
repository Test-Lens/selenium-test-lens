package io.github.testlens.testng;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.TestRunScope;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceFailure;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.selenium.execution.BrowserExecutionConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.NoSuchSessionException;
import org.testng.IClassListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestClass;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Manages Test Lens invocation state and the configured TestNG WebDriver ownership lifetime. */
public final class TestLensTestNgListener extends TestLensTestNgConfigurationBridge
        implements IInvokedMethodListener, ISuiteListener, IClassListener {
    static final String STATE_ATTRIBUTE = "io.github.testlens.testng.invocation-state";
    static final String SUITE_SCOPE_ATTRIBUTE = "io.github.testlens.testng.run-scope";
    private static final String SKIPPED_REASON_FALLBACK = "Test skipped by TestNG";
    private static final String PARALLEL_CONFLICT = "Shared WebDriver scope PER_CLASS conflicts with parallel method execution. "
            + "Use PER_METHOD, or execute this class sequentially.";

    /* Callback association hint only; owner identity is suite + context + concrete instance. */
    private final ThreadLocal<DriverOwner> classCallbackCandidate = new ThreadLocal<>();

    /** Creates a listener suitable for explicit registration with {@code @Listeners}. */
    public TestLensTestNgListener() {
    }

    @Override
    void beforeManagedMethodConfiguration(ITestResult configurationResult, ITestNGMethod testMethod) {
        TestLensTestNg annotation = annotation(configurationResult);
        if (annotation == null || annotation.driverScope() != DriverScope.PER_CLASS) return;
        DriverOwner owner = holder(configurationResult).owner(configurationResult, annotation);
        try {
            InvocationState invocation = owner.beginOrBind(testMethod, configurationResult);
            bind(configurationResult, invocation);
            classCallbackCandidate.set(owner);
        } catch (Throwable failure) {
            failResult(configurationResult, failure);
            rethrow(failure);
        }
    }

    @Override
    void afterManagedConfiguration(ITestResult configurationResult, ITestNGMethod testMethod) {
        TestLensTestNg annotation = annotation(configurationResult);
        if (annotation == null || annotation.driverScope() != DriverScope.PER_CLASS) return;
        SuiteScopeHolder suiteHolder = holder(configurationResult);
        DriverOwner owner = suiteHolder.ownerIfPresent(configurationResult);
        if (owner == null) return;
        ITestNGMethod configurationMethod = configurationResult.getMethod();
        if (configurationMethod.isBeforeMethodConfiguration()
                || configurationMethod.isAfterMethodConfiguration()) {
            InvocationState invocation = owner.active();
            if (invocation != null) {
                invocation.observeConfiguration(configurationResult);
                if (configurationMethod.isAfterMethodConfiguration()
                        && invocation.afterMethodObserved(configurationMethod)) {
                    owner.finish(invocation, configurationResult);
                }
            }
        } else if (configurationMethod.isAfterClassConfiguration()) {
            owner.afterClassMethodObserved(configurationMethod);
            classCallbackCandidate.set(owner);
            if (owner.classLifecycleComplete()) suiteHolder.release(owner);
        }
    }

    @Override
    void afterManagedTestResult(ITestResult result) {
        TestLensTestNg annotation = annotation(result);
        if (annotation == null || annotation.driverScope() != DriverScope.PER_CLASS) return;
        DriverOwner owner = holder(result).ownerIfPresent(result);
        if (owner == null) return;
        InvocationState invocation = owner.active();
        if (invocation == null) return;
        invocation.attachTestResult(result);
        if (invocation.expectedAfterMethods().isEmpty()) owner.finish(invocation, result);
    }

    @Override
    public void beforeInvocation(IInvokedMethod invoked, ITestResult result) {
        TestLensTestNg annotation = annotation(result);
        if (annotation == null) return;
        if (annotation.driverScope() == DriverScope.PER_METHOD) {
            beforePerMethod(invoked, result, annotation);
            return;
        }

        DriverOwner owner = holder(result).ownerIfPresent(result);
        try {
            if (invoked.isTestMethod()) {
                if (owner == null) {
                    if (result.getStatus() == ITestResult.SKIP) return;
                    owner = holder(result).owner(result, annotation);
                }
                InvocationState invocation = owner.beginOrBind(invoked.getTestMethod(), result);
                invocation.attachTestResult(result);
                bind(result, invocation);
                classCallbackCandidate.set(owner);
            } else if (invoked.isConfigurationMethod() && result.getMethod().isAfterMethodConfiguration()) {
                if (owner != null && owner.active() != null) {
                    bind(result, owner.active());
                    classCallbackCandidate.set(owner);
                }
            } else if (invoked.isConfigurationMethod() && result.getMethod().isAfterClassConfiguration()
                    && owner != null) {
                classCallbackCandidate.set(owner);
            }
        } catch (Throwable failure) {
            failResult(result, failure);
            rethrow(failure);
        }
    }

    private void beforePerMethod(IInvokedMethod method, ITestResult result, TestLensTestNg annotation) {
        if (!method.isTestMethod() || result.getStatus() == ITestResult.SKIP
                || result.getAttribute(STATE_ATTRIBUTE) != null) return;
        WebDriver driver = null;
        try {
            BrowserExecutionConfig executionConfig = BrowserExecutionConfig.resolve(annotation.headless());
            TestLensTestNgFactory factory = createFactory(annotation.factory());
            driver = Objects.requireNonNull(factory.createDriver(executionConfig),
                    "TestLensTestNgFactory.createDriver(BrowserExecutionConfig) returned null");
            TestLensOptions options = Objects.requireNonNull(factory.lensOptions(),
                    "TestLensTestNgFactory.lensOptions() returned null");
            TestLens lens = holder(result).scope.attach(driver, options);
            String sessionName = Objects.requireNonNull(factory.sessionName(result),
                    "TestLensTestNgFactory.sessionName() returned null");
            UiTestLensSession session = lens.startSession(sessionName);
            result.setAttribute(STATE_ATTRIBUTE, InvocationState.perMethod(driver, lens, session));
        } catch (Throwable setupFailure) {
            if (driver != null) {
                try { driver.quit(); } catch (Throwable quitFailure) { addSuppressed(setupFailure, quitFailure); }
            }
            failResult(result, setupFailure);
            rethrow(setupFailure);
        }
    }

    @Override
    public void afterInvocation(IInvokedMethod invoked, ITestResult result) {
        TestLensTestNg annotation = annotation(result);
        if (annotation == null) return;
        if (annotation.driverScope() == DriverScope.PER_METHOD) {
            afterPerMethod(invoked, result);
            return;
        }

        SuiteScopeHolder suiteHolder = holder(result);
        DriverOwner owner = suiteHolder.ownerIfPresent(result);
        InvocationState invocation = state(result);
        try {
            if (invocation != null) {
                if (isTerminalFailureCandidate(invoked, result)) markFailureWatermark(invocation, result);
                invocation.observe(result, invoked);
                if (invoked.isTestMethod() && invocation.expectedAfterMethods().isEmpty()) {
                    owner.finish(invocation, result);
                }
            }
        } finally {
            result.removeAttribute(STATE_ATTRIBUTE);
        }
    }

    private static void afterPerMethod(IInvokedMethod method, ITestResult result) {
        if (!method.isTestMethod()) return;
        InvocationState state = state(result);
        if (state == null || !state.beginCleanup()) return;
        int originalStatus = result.getStatus();
        Throwable original = result.getThrowable();
        Throwable cleanupFailure = null;
        try {
            try { finish(state.lens, originalStatus, original); }
            catch (Throwable failure) { cleanupFailure = failure; }
            try { state.quit(); }
            catch (Throwable failure) {
                if (cleanupFailure == null) cleanupFailure = failure;
                else addSuppressed(cleanupFailure, failure);
            }
        } finally {
            result.removeAttribute(STATE_ATTRIBUTE);
        }
        applyCleanupFailure(result, originalStatus, original, cleanupFailure);
    }

    /**
     * Records TestNG's class-completion notification. Closing is delayed until all user AfterClass
     * configurations for the same concrete instance have completed, regardless of listener ordering.
     *
     * @param testClass completed TestNG class notification
     * @since 0.4.0
     */
    @Override
    public void onAfterClass(ITestClass testClass) {
        DriverOwner owner = classCallbackCandidate.get();
        classCallbackCandidate.remove();
        if (owner == null || !owner.matches(testClass)) return;
        owner.classListenerObserved(testClass);
        if (owner.classLifecycleComplete()) owner.holder.release(owner);
    }

    @Override
    public void onFinish(ISuite suite) {
        SuiteScopeHolder holder;
        synchronized (suite) {
            Object stored = suite.getAttribute(SUITE_SCOPE_ATTRIBUTE);
            holder = stored instanceof SuiteScopeHolder found ? found : null;
            suite.removeAttribute(SUITE_SCOPE_ATTRIBUTE);
        }
        if (holder != null) holder.close();
    }

    private static void bind(ITestResult result, InvocationState invocation) {
        result.setAttribute(STATE_ATTRIBUTE, invocation);
    }

    private static InvocationState state(ITestResult result) {
        Object stored = result.getAttribute(STATE_ATTRIBUTE);
        return stored instanceof InvocationState invocation ? invocation : null;
    }

    private static SuiteScopeHolder holder(ITestResult result) {
        return holder(result.getTestContext().getSuite());
    }

    private static SuiteScopeHolder holder(ISuite suite) {
        synchronized (suite) {
            Object stored = suite.getAttribute(SUITE_SCOPE_ATTRIBUTE);
            if (stored instanceof SuiteScopeHolder holder) return holder;
            SuiteScopeHolder created = new SuiteScopeHolder();
            suite.setAttribute(SUITE_SCOPE_ATTRIBUTE, created);
            return created;
        }
    }

    private static void finish(TestLens lens, int status, Throwable original) {
        switch (status) {
            case ITestResult.SUCCESS -> lens.finishPassed();
            case ITestResult.SKIP -> lens.finishSkipped(skippedReason(original));
            case ITestResult.FAILURE, ITestResult.SUCCESS_PERCENTAGE_FAILURE -> lens.finishFailed(original);
            default -> lens.finishFailed(original);
        }
    }

    private static boolean isTerminalFailureCandidate(IInvokedMethod invoked, ITestResult result) {
        if (result.getStatus() != ITestResult.FAILURE
                && result.getStatus() != ITestResult.SUCCESS_PERCENTAGE_FAILURE) return false;
        return invoked.isTestMethod() || invoked.isConfigurationMethod()
                && result.getMethod().isBeforeMethodConfiguration();
    }

    private static void markFailureWatermark(InvocationState invocation, ITestResult result) {
        Throwable failure = result.getThrowable();
        invocation.session.addEvent(TraceEvent.builder(TraceEventType.CUSTOM, TraceStatus.FAILED,
                        "Uncaught TestNG failure boundary")
                .message(failure == null ? "TestNG reported an uncaught failure" : failure.getMessage())
                .failure(failure == null ? null : TraceFailure.from(failure, false))
                .attribute("testlens.recorder.failureWatermark", "true")
                .attribute("runner", "testng")
                .build());
    }

    private static String skippedReason(Throwable skipped) {
        if (skipped == null || skipped.getMessage() == null || skipped.getMessage().isBlank()) {
            return SKIPPED_REASON_FALLBACK;
        }
        return skipped.getMessage();
    }

    private static TestLensTestNg annotation(ITestResult result) {
        Object instance = result == null ? null : result.getInstance();
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

    private static void validateSequential(ITestResult result, ITestNGMethod method) {
        XmlSuite.ParallelMode parallel = result.getTestContext().getCurrentXmlTest().getParallel();
        if (parallel == XmlSuite.ParallelMode.METHODS) throw parallelConflict(result, "parallel=methods");
        if (method.getDataProviderMethod() != null && method.getDataProviderMethod().isParallel()) {
            throw parallelConflict(result, "@DataProvider(parallel=true)");
        }
        if (method.getInvocationCount() > 1 && method.getThreadPoolSize() > 1) {
            throw parallelConflict(result, "invocationCount with threadPoolSize=" + method.getThreadPoolSize());
        }
    }

    private static IllegalStateException parallelConflict(ITestResult result, String source) {
        return new IllegalStateException(PARALLEL_CONFLICT + " class="
                + result.getInstance().getClass().getName() + ", source=" + source);
    }

    private static void failResult(ITestResult result, Throwable failure) {
        result.setStatus(ITestResult.FAILURE);
        result.setThrowable(failure);
    }

    private static void applyCleanupFailure(ITestResult result, int originalStatus,
                                            Throwable original, Throwable cleanupFailure) {
        if (cleanupFailure == null) return;
        if (originalStatus == ITestResult.SUCCESS) {
            result.setStatus(ITestResult.FAILURE);
            result.setThrowable(cleanupFailure);
        } else if (original != null) addSuppressed(original, cleanupFailure);
        else result.setThrowable(cleanupFailure);
    }

    private static void addSuppressed(Throwable primary, Throwable secondary) {
        if (primary == secondary) return;
        for (Throwable existing : primary.getSuppressed()) if (existing == secondary) return;
        primary.addSuppressed(secondary);
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException runtimeException) throw runtimeException;
        throw new RuntimeException(failure);
    }

    static final class InvocationState {
        private final WebDriver driver;
        private final TestLens lens;
        private final UiTestLensSession session;
        private final TestLensTestNgContext context;
        private final ITestNGMethod testMethod;
        private final Set<Method> expectedAfterMethods;
        private final Set<Method> observedAfterMethods = new LinkedHashSet<>();
        private ITestResult testResult;
        private Throwable configurationFailure;
        private Throwable skipCause;
        private boolean cleanupStarted;
        private boolean quit;

        private InvocationState(WebDriver driver, TestLens lens, UiTestLensSession session,
                                ITestNGMethod testMethod, Set<Method> expectedAfterMethods) {
            this.driver = driver;
            this.lens = lens;
            this.session = session;
            this.testMethod = testMethod;
            this.expectedAfterMethods = expectedAfterMethods;
            this.context = new TestLensTestNgContext(driver, lens, session);
        }

        static InvocationState perMethod(WebDriver driver, TestLens lens, UiTestLensSession session) {
            return new InvocationState(driver, lens, session, null, Set.of());
        }

        synchronized void assertCallback(ITestNGMethod method) {
            if (testMethod != null && !sameMethod(testMethod, method)) {
                throw new IllegalStateException(PARALLEL_CONFLICT + " Another invocation entered the same owner.");
            }
        }

        synchronized void attachTestResult(ITestResult result) { this.testResult = result; }

        synchronized void observe(ITestResult result, IInvokedMethod invoked) {
            if (invoked.isTestMethod()) testResult = result;
        }

        synchronized void observeConfiguration(ITestResult result) {
            if (result.getStatus() == ITestResult.FAILURE) configurationFailure = result.getThrowable();
            else if (result.getStatus() == ITestResult.SKIP && skipCause == null) skipCause = result.getThrowable();
        }

        synchronized boolean afterMethodObserved(ITestNGMethod method) {
            Method javaMethod = javaMethod(method);
            if (javaMethod != null) observedAfterMethods.add(javaMethod);
            return observedAfterMethods.containsAll(expectedAfterMethods);
        }

        synchronized int outcome() {
            if (configurationFailure != null) return ITestResult.FAILURE;
            if (testResult == null) return skipCause == null ? ITestResult.FAILURE : ITestResult.SKIP;
            return testResult.getStatus();
        }

        synchronized Throwable cause() {
            if (configurationFailure != null) return configurationFailure;
            if (testResult != null && testResult.getThrowable() != null) return testResult.getThrowable();
            return skipCause;
        }

        Set<Method> expectedAfterMethods() { return expectedAfterMethods; }
        private synchronized boolean beginCleanup() { if (cleanupStarted) return false; cleanupStarted = true; return true; }
        private synchronized void quit() { if (quit) return; quit = true; driver.quit(); }
        TestLensTestNgContext context() { return context; }
    }

    private static final class DriverOwner {
        private final SuiteScopeHolder holder;
        private final ITestContext testContext;
        private final Object instance;
        private final TestLensTestNg annotation;
        private final Set<Method> observedClassAfter = new LinkedHashSet<>();
        private Set<Method> expectedClassAfter = Set.of();
        private TestLensTestNgFactory factory;
        private TestLensOptions options;
        private WebDriver driver;
        private InvocationState active;
        private ITestNGMethod startingMethod;
        private boolean classListenerSeen;
        private boolean broken;
        private boolean closed;
        private boolean quitAttempted;

        private DriverOwner(SuiteScopeHolder holder, ITestContext testContext,
                            Object instance, TestLensTestNg annotation) {
            this.holder = holder;
            this.testContext = testContext;
            this.instance = instance;
            this.annotation = annotation;
        }

        InvocationState beginOrBind(ITestNGMethod method, ITestResult callbackResult) {
            synchronized (this) {
                if (closed) throw new IllegalStateException("Shared WebDriver owner is already closed");
                if (broken) throw new IllegalStateException("Shared WebDriver scope PER_CLASS cannot continue because "
                        + "the Selenium session was lost; Test Lens will not silently replace the shared browser");
                if (active != null) { active.assertCallback(method); return active; }
                if (startingMethod != null) throw parallelConflict(callbackResult, "concurrent owner entry");
                validateSequential(callbackResult, method);
                startingMethod = method;
            }
            try {
                ensureOpen();
                ITestResult identity = callbackResult.getMethod() == method
                        ? callbackResult : new InvocationResultView(method, testContext, instance);
                String sessionName = Objects.requireNonNull(factory.sessionName(identity),
                        "TestLensTestNgFactory.sessionName() returned null");
                TestLens invocationLens = holder.scope.attach(driver, options);
                UiTestLensSession session = invocationLens.startSession(sessionName);
                InvocationState created = new InvocationState(driver, invocationLens, session, method,
                        applicableAfterMethods(method));
                synchronized (this) {
                    active = created;
                    startingMethod = null;
                    return created;
                }
            } catch (Throwable failure) {
                synchronized (this) { startingMethod = null; }
                try { close(); } catch (Throwable cleanupFailure) { addSuppressed(failure, cleanupFailure); }
                throw failure;
            }
        }

        private void ensureOpen() {
            synchronized (this) { if (driver != null) return; }
            BrowserExecutionConfig executionConfig = BrowserExecutionConfig.resolve(annotation.headless());
            TestLensTestNgFactory createdFactory = createFactory(annotation.factory());
            WebDriver createdDriver = Objects.requireNonNull(createdFactory.createDriver(executionConfig),
                    "TestLensTestNgFactory.createDriver(BrowserExecutionConfig) returned null");
            boolean registered = false;
            try {
                holder.claimDriver(createdDriver, this);
                registered = true;
                TestLensOptions options = Objects.requireNonNull(createdFactory.lensOptions(),
                        "TestLensTestNgFactory.lensOptions() returned null");
                synchronized (this) { factory = createdFactory; driver = createdDriver; this.options = options; }
            } catch (Throwable failure) {
                if (registered) {
                    try { createdDriver.quit(); } catch (Throwable quitFailure) { addSuppressed(failure, quitFailure); }
                    holder.releaseDriver(createdDriver, this);
                }
                throw failure;
            }
        }

        synchronized InvocationState active() { return active; }

        void finish(InvocationState invocation, ITestResult callbackResult) {
            synchronized (this) { if (active != invocation) return; }
            if (!invocation.beginCleanup()) return;
            Throwable failure = null;
            try { TestLensTestNgListener.finish(invocation.lens, invocation.outcome(), invocation.cause()); }
            catch (Throwable found) { failure = found; }
            if (sessionLost(invocation.cause())) synchronized (this) { broken = true; }
            synchronized (this) { if (active == invocation) active = null; }
            if (failure != null) {
                applyCleanupFailure(callbackResult, callbackResult.getStatus(),
                        callbackResult.getThrowable(), failure);
            }
        }

        synchronized void classListenerObserved(ITestClass testClass) {
            classListenerSeen = true;
            expectedClassAfter = methods(testClass.getAfterClassMethods());
        }

        synchronized void afterClassMethodObserved(ITestNGMethod method) {
            Method javaMethod = javaMethod(method);
            if (javaMethod != null) observedClassAfter.add(javaMethod);
        }

        synchronized boolean classLifecycleComplete() {
            return classListenerSeen && observedClassAfter.containsAll(expectedClassAfter);
        }

        synchronized boolean matches(ITestClass testClass) {
            if (closed || testClass == null) return false;
            for (ITestNGMethod method : testClass.getTestMethods()) {
                if (method.getInstance() == instance && method.getXmlTest() == testContext.getCurrentXmlTest()) return true;
            }
            return false;
        }

        void close() {
            InvocationState pending;
            WebDriver owned;
            synchronized (this) {
                if (closed) return;
                closed = true;
                pending = active;
                active = null;
                owned = driver;
            }
            Throwable cleanupFailure = null;
            if (pending != null && pending.beginCleanup()) {
                try { TestLensTestNgListener.finish(pending.lens, pending.outcome(), pending.cause()); }
                catch (Throwable failure) { cleanupFailure = failure; }
            }
            if (owned != null) {
                synchronized (this) { if (quitAttempted) return; quitAttempted = true; }
                try { owned.quit(); }
                catch (Throwable quitFailure) {
                    if (cleanupFailure == null) cleanupFailure = quitFailure;
                    else addSuppressed(cleanupFailure, quitFailure);
                } finally { holder.releaseDriver(owned, this); }
            }
            if (cleanupFailure != null) rethrow(cleanupFailure);
        }
    }

    private static final class SuiteScopeHolder {
        private final TestRunScope scope = TestRunScope.open();
        private final Map<ITestContext, IdentityHashMap<Object, DriverOwner>> owners = new IdentityHashMap<>();
        private final IdentityHashMap<WebDriver, DriverOwner> claimedDrivers = new IdentityHashMap<>();
        private boolean closed;

        synchronized DriverOwner owner(ITestResult result, TestLensTestNg annotation) {
            if (closed) throw new IllegalStateException("TestNG suite scope is already closed");
            ITestContext context = result.getTestContext();
            Object instance = Objects.requireNonNull(result.getInstance(), "TestNG result instance");
            IdentityHashMap<Object, DriverOwner> byInstance = owners.computeIfAbsent(context,
                    ignored -> new IdentityHashMap<>());
            return byInstance.computeIfAbsent(instance,
                    ignored -> new DriverOwner(this, context, instance, annotation));
        }

        synchronized DriverOwner ownerIfPresent(ITestResult result) {
            IdentityHashMap<Object, DriverOwner> byInstance = owners.get(result.getTestContext());
            return byInstance == null ? null : byInstance.get(result.getInstance());
        }

        synchronized void claimDriver(WebDriver driver, DriverOwner owner) {
            DriverOwner existing = claimedDrivers.get(driver);
            if (existing != null && existing != owner) {
                throw new IllegalStateException("TestLensTestNgFactory returned a WebDriver already owned by another "
                        + "PER_CLASS test instance; shared aliases cannot have two closing owners");
            }
            claimedDrivers.put(driver, owner);
        }

        synchronized void releaseDriver(WebDriver driver, DriverOwner owner) {
            if (claimedDrivers.get(driver) == owner) claimedDrivers.remove(driver);
        }

        void release(DriverOwner owner) {
            synchronized (this) {
                IdentityHashMap<Object, DriverOwner> byInstance = owners.get(owner.testContext);
                if (byInstance != null && byInstance.get(owner.instance) == owner) {
                    byInstance.remove(owner.instance);
                    if (byInstance.isEmpty()) owners.remove(owner.testContext);
                }
            }
            owner.close();
        }

        void close() {
            Set<DriverOwner> remaining = Collections.newSetFromMap(new IdentityHashMap<>());
            synchronized (this) {
                if (closed) return;
                closed = true;
                owners.values().forEach(map -> remaining.addAll(map.values()));
                owners.clear();
            }
            Throwable failure = null;
            for (DriverOwner owner : remaining) {
                try { owner.close(); }
                catch (Throwable found) {
                    if (failure == null) failure = found;
                    else addSuppressed(failure, found);
                }
            }
            try { scope.close(); }
            catch (Throwable found) {
                if (failure == null) failure = found;
                else addSuppressed(failure, found);
            }
            if (failure != null) rethrow(failure);
        }
    }

    private static Set<Method> applicableAfterMethods(ITestNGMethod testMethod) {
        Set<Method> result = new LinkedHashSet<>();
        Set<String> testGroups = Set.of(testMethod.getGroups());
        for (ITestNGMethod method : testMethod.getTestClass().getAfterTestMethods()) {
            if (!method.getEnabled()) continue;
            String[] onlyForGroups = method.getGroups();
            if (onlyForGroups.length > 0 && Collections.disjoint(Set.of(onlyForGroups), testGroups)) continue;
            Method javaMethod = javaMethod(method);
            if (javaMethod != null) result.add(javaMethod);
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<Method> methods(ITestNGMethod[] methods) {
        Set<Method> result = new LinkedHashSet<>();
        for (ITestNGMethod method : methods) {
            if (!method.getEnabled()) continue;
            Method javaMethod = javaMethod(method);
            if (javaMethod != null) result.add(javaMethod);
        }
        return Collections.unmodifiableSet(result);
    }

    private static Method javaMethod(ITestNGMethod method) {
        return method == null || method.getConstructorOrMethod() == null
                ? null : method.getConstructorOrMethod().getMethod();
    }

    private static boolean sameMethod(ITestNGMethod left, ITestNGMethod right) {
        return left == right || (left != null && right != null && Objects.equals(javaMethod(left), javaMethod(right))
                && left.getInstance() == right.getInstance());
    }

    private static boolean sessionLost(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof NoSuchSessionException) return true;
        }
        return false;
    }
}
