package io.github.testlens.junit5;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.TestRunScope;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceFailure;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.selenium.execution.BrowserExecutionConfig;
import io.github.testlens.selenium.execution.HeadlessMode;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.opentest4j.TestAbortedException;
import org.openqa.selenium.WebDriver;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Owns one WebDriver and one Test Lens session for every JUnit 5 test invocation.
 * Register an instance with {@code @RegisterExtension}; obtain the invocation's
 * driver and Lens through method parameters.
 */
public final class TestLensExtension
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver, FailureWatermarkCallback {
    private static final String STATE_KEY_PREFIX = "test-lens-invocation:";
    private static final String ABORTED_REASON_FALLBACK = "Test aborted by JUnit 5";
    private static final ExtensionContext.Namespace RUN_SCOPE_NAMESPACE =
            ExtensionContext.Namespace.create(TestLensExtension.class, "run-scope");

    private final Supplier<? extends WebDriver> legacyDriverFactory;
    private final Function<BrowserExecutionConfig, ? extends WebDriver> configuredDriverFactory;
    private final HeadlessMode headless;
    private final TestLensOptions lensOptions;
    private final Function<ExtensionContext, String> sessionNameFactory;
    private final ExtensionContext.Namespace namespace;

    private TestLensExtension(Builder builder) {
        this.legacyDriverFactory = builder.legacyDriverFactory;
        this.configuredDriverFactory = builder.configuredDriverFactory;
        this.headless = builder.headless;
        this.lensOptions = builder.lensOptions;
        this.sessionNameFactory = builder.sessionNameFactory;
        this.namespace = ExtensionContext.Namespace.create(TestLensExtension.class, this);
    }

    /**
     * Creates a builder for an extension that owns drivers returned by {@code driverFactory}.
     *
     * @param driverFactory factory invoked once for every test invocation
     * @return a new extension builder
     */
    public static Builder builder(Supplier<? extends WebDriver> driverFactory) {
        return new Builder(driverFactory);
    }

    /**
     * @param driverFactory execution-config-aware factory invoked once per test invocation
     * @return a new extension builder
     * @since 0.4.0
     */
    public static Builder builder(Function<BrowserExecutionConfig, ? extends WebDriver> driverFactory) {
        return new Builder(driverFactory);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        BrowserExecutionConfig executionConfig = BrowserExecutionConfig.resolve(headless);
        WebDriver driver = Objects.requireNonNull(createDriver(executionConfig),
                "TestLensExtension driverFactory returned null");
        try {
            TestLens lens = runScope(context).attach(driver, lensOptions);
            String sessionName = Objects.requireNonNull(sessionNameFactory.apply(context),
                    "TestLensExtension session name function returned null");
            UiTestLensSession session = lens.startSession(sessionName);
            store(context).put(stateKey(context), new InvocationState(driver, lens, session));
        } catch (Throwable setupFailure) {
            try {
                driver.quit();
            } catch (Throwable quitFailure) {
                addSuppressed(setupFailure, quitFailure);
            }
            rethrow(setupFailure);
        }
    }

    private WebDriver createDriver(BrowserExecutionConfig executionConfig) {
        if (configuredDriverFactory != null) return configuredDriverFactory.apply(executionConfig);
        if (executionConfig.headless() != HeadlessMode.UNSET) {
            throw new IllegalStateException("Configured headed/headless execution requires a "
                    + "BrowserExecutionConfig-aware driver factory; use TestLensExtension.builder(Function)");
        }
        return legacyDriverFactory.get();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        ExtensionContext.Store store = store(context);
        String key = stateKey(context);
        InvocationState state = store.get(key, InvocationState.class);
        if (state == null || !state.beginCleanup()) {
            return;
        }

        Throwable original = context.getExecutionException().orElse(null);
        Throwable finalizationFailure = null;
        Throwable quitFailure = null;
        try {
            try {
                if (original == null) {
                    state.lens.finishPassed();
                } else if (original instanceof TestAbortedException) {
                    state.lens.finishSkipped(abortedReason(original));
                } else {
                    state.lens.finishFailed(original);
                }
                state.finalized = true;
            } catch (Throwable lensFinalizationFailure) {
                finalizationFailure = lensFinalizationFailure;
            }

            try {
                state.quit();
            } catch (Throwable driverQuitFailure) {
                quitFailure = driverQuitFailure;
            }
        } finally {
            store.remove(key);
        }

        if (original != null) {
            if (finalizationFailure != null) addSuppressed(original, finalizationFailure);
            if (quitFailure != null) addSuppressed(original, quitFailure);
            return;
        }
        if (finalizationFailure != null) {
            if (quitFailure != null) addSuppressed(finalizationFailure, quitFailure);
            rethrow(finalizationFailure);
        }
        if (quitFailure != null) {
            rethrow(quitFailure);
        }
    }

    void markFailureWatermark(ExtensionContext context) {
        Throwable failure = context.getExecutionException().orElse(null);
        if (failure == null || failure instanceof TestAbortedException) return;
        InvocationState state = store(context).get(stateKey(context), InvocationState.class);
        if (state == null) return;
        state.session.addEvent(TraceEvent.builder(TraceEventType.CUSTOM, TraceStatus.FAILED,
                        "Uncaught JUnit failure boundary")
                .message(failure.getMessage())
                .failure(TraceFailure.from(failure, false))
                .attribute("testlens.recorder.failureWatermark", "true")
                .attribute("runner", "junit5")
                .build());
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return type == WebDriver.class || type == TestLens.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        if (!supportsParameter(parameterContext, extensionContext)) {
            throw new ParameterResolutionException("TestLensExtension resolves only WebDriver and TestLens parameters");
        }
        InvocationState state = store(extensionContext).get(stateKey(extensionContext), InvocationState.class);
        if (state == null) {
            throw new ParameterResolutionException(
                    "No TestLensExtension state exists for invocation " + extensionContext.getUniqueId());
        }
        return parameterContext.getParameter().getType() == WebDriver.class ? state.driver : state.lens;
    }

    private ExtensionContext.Store store(ExtensionContext context) {
        return context.getStore(namespace);
    }

    private TestRunScope runScope(ExtensionContext context) {
        RunScopeResource resource = context.getRoot().getStore(RUN_SCOPE_NAMESPACE)
                .getOrComputeIfAbsent(RunScopeResource.class, ignored -> new RunScopeResource(), RunScopeResource.class);
        return resource.scope;
    }

    private static String stateKey(ExtensionContext context) {
        return STATE_KEY_PREFIX + context.getUniqueId();
    }

    private static String defaultSessionName(ExtensionContext context) {
        String invocationId = UUID.nameUUIDFromBytes(
                context.getUniqueId().getBytes(StandardCharsets.UTF_8)).toString().substring(0, 8);
        return context.getRequiredTestClass().getSimpleName() + "." + context.getDisplayName()
                + " [" + invocationId + "]";
    }

    private static String abortedReason(Throwable aborted) {
        String message = aborted.getMessage();
        return message == null || message.isBlank() ? ABORTED_REASON_FALLBACK : message;
    }

    private static void addSuppressed(Throwable primary, Throwable secondary) {
        if (primary == secondary) return;
        for (Throwable existing : primary.getSuppressed()) {
            if (existing == secondary) return;
        }
        primary.addSuppressed(secondary);
    }

    private static void rethrow(Throwable failure) throws Exception {
        if (failure instanceof Error error) {
            throw error;
        }
        if (failure instanceof Exception exception) {
            throw exception;
        }
        throw new RuntimeException(failure);
    }

    /** Builds an immutable, parallel-safe extension configuration. */
    public static final class Builder {
        private final Supplier<? extends WebDriver> legacyDriverFactory;
        private final Function<BrowserExecutionConfig, ? extends WebDriver> configuredDriverFactory;
        private HeadlessMode headless = HeadlessMode.UNSET;
        private TestLensOptions lensOptions = TestLensOptions.defaults();
        private Function<ExtensionContext, String> sessionNameFactory = TestLensExtension::defaultSessionName;

        private Builder(Supplier<? extends WebDriver> driverFactory) {
            this.legacyDriverFactory = Objects.requireNonNull(driverFactory, "driverFactory");
            this.configuredDriverFactory = null;
        }

        private Builder(Function<BrowserExecutionConfig, ? extends WebDriver> driverFactory) {
            this.legacyDriverFactory = null;
            this.configuredDriverFactory = Objects.requireNonNull(driverFactory, "driverFactory");
        }

        /**
         * @param mode explicit intent; UNSET consults property/environment sources
         * @return this builder
         * @since 0.4.0
         */
        public Builder headless(HeadlessMode mode) {
            this.headless = Objects.requireNonNull(mode, "mode");
            return this;
        }

        /**
         * Sets the options used for every independently created Lens instance.
         *
         * @param options immutable Lens configuration
         * @return this builder
         */
        public Builder lensOptions(TestLensOptions options) {
            this.lensOptions = Objects.requireNonNull(options, "options");
            return this;
        }

        /**
         * Sets a per-invocation session-name function.
         *
         * @param sessionNameFactory function evaluated against the current JUnit context
         * @return this builder
         */
        public Builder sessionName(Function<ExtensionContext, String> sessionNameFactory) {
            this.sessionNameFactory = Objects.requireNonNull(sessionNameFactory, "sessionNameFactory");
            return this;
        }

        /**
         * Creates the extension.
         *
         * @return an immutable, parallel-safe extension
         */
        public TestLensExtension build() {
            return new TestLensExtension(this);
        }
    }

    private static final class InvocationState {
        private final WebDriver driver;
        private final TestLens lens;
        @SuppressWarnings("unused")
        private final UiTestLensSession session;
        private boolean cleanupStarted;
        private boolean finalized;
        private boolean quit;

        private InvocationState(WebDriver driver, TestLens lens, UiTestLensSession session) {
            this.driver = driver;
            this.lens = lens;
            this.session = session;
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
    }

    private static final class RunScopeResource implements ExtensionContext.Store.CloseableResource {
        private final TestRunScope scope = TestRunScope.open();

        @Override
        public void close() {
            scope.close();
        }
    }
}

interface FailureWatermarkCallback extends AfterTestExecutionCallback {
    @Override
    default void afterTestExecution(ExtensionContext context) {
        ((TestLensExtension) this).markFailureWatermark(context);
    }
}
