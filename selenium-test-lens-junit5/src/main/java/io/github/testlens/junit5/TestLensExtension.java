package io.github.testlens.junit5;

import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.extension.AfterEachCallback;
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
        implements BeforeEachCallback, AfterEachCallback, ParameterResolver {
    private static final String STATE_KEY_PREFIX = "test-lens-invocation:";
    private static final String ABORTED_REASON_FALLBACK = "Test aborted by JUnit 5";

    private final Supplier<? extends WebDriver> driverFactory;
    private final TestLensOptions lensOptions;
    private final Function<ExtensionContext, String> sessionNameFactory;
    private final ExtensionContext.Namespace namespace;

    private TestLensExtension(Builder builder) {
        this.driverFactory = builder.driverFactory;
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

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        WebDriver driver = Objects.requireNonNull(driverFactory.get(),
                "TestLensExtension driverFactory returned null");
        try {
            TestLens lens = TestLens.attach(driver, lensOptions);
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
        if (primary != secondary) {
            primary.addSuppressed(secondary);
        }
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
        private final Supplier<? extends WebDriver> driverFactory;
        private TestLensOptions lensOptions = TestLensOptions.defaults();
        private Function<ExtensionContext, String> sessionNameFactory = TestLensExtension::defaultSessionName;

        private Builder(Supplier<? extends WebDriver> driverFactory) {
            this.driverFactory = Objects.requireNonNull(driverFactory, "driverFactory");
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
}
