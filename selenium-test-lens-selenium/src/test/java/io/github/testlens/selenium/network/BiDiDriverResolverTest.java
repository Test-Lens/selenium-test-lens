package io.github.testlens.selenium.network;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.BiDiException;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.decorators.WebDriverDecorator;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiDiDriverResolverTest {
    @Test
    void alreadyInitializedHasBiDiRemainsUsable() {
        WebDriver driver = bidiDriver(true);
        AtomicInteger initializationCalls = new AtomicInteger();

        BiDiDriverResolver.ResolvedBiDiDriver resolved = BiDiDriverResolver.resolve(
                driver, ignored -> { throw new AssertionError("augmentation not expected"); }, hasBiDi -> {
                    initializationCalls.incrementAndGet();
                    return null;
                });

        assertSame(driver, resolved.driver());
        assertEquals(1, initializationCalls.get());
        assertEquals("true", resolved.diagnostics().get("bidiInitiallyInitialized"));
        assertEquals("false", resolved.diagnostics().get("driverAugmented"));
    }

    @Test
    void lazyHasBiDiUsesGetBiDiInsteadOfTreatingEmptyOptionalAsUnsupported() {
        WebDriver driver = bidiDriver(false);
        AtomicInteger initializationCalls = new AtomicInteger();

        BiDiDriverResolver.ResolvedBiDiDriver resolved = BiDiDriverResolver.resolve(
                driver, ignored -> { throw new AssertionError("augmentation not expected"); }, hasBiDi -> {
                    initializationCalls.incrementAndGet();
                    return null;
                });

        assertSame(driver, resolved.driver());
        assertEquals(1, initializationCalls.get());
        assertEquals("false", resolved.diagnostics().get("bidiInitiallyInitialized"));
        assertEquals("1", resolved.diagnostics().get("initializationAttempts"));
    }

    @Test
    void productionInitializerInvokesTheActualGetBiDiContract() {
        AtomicInteger getCalls = new AtomicInteger();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, HasBiDi.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "maybeGetBiDi" -> Optional.empty();
                    case "getBiDi" -> {
                        getCalls.incrementAndGet();
                        throw new BiDiException("expected test failure");
                    }
                    case "toString" -> "lazy-has-bidi";
                    default -> null;
                });

        BiDiCaptureException failure = assertThrows(BiDiCaptureException.class,
                () -> BiDiDriverResolver.resolve(driver));

        assertEquals(2, getCalls.get());
        assertEquals(BiDiFailureCategory.INITIALIZATION_FAILED, failure.category());
    }

    @Test
    void rawRemoteWebDriverUsesNativeBiDiOrIsAugmentedOnce() {
        WebDriver remote = new StubRemoteWebDriver(new ImmutableCapabilities(
                "browserName", "chrome", "webSocketUrl", "wss://remote.invalid/session"));
        WebDriver augmented = bidiDriver(false);
        AtomicInteger augmentations = new AtomicInteger();
        AtomicInteger initializations = new AtomicInteger();
        WebDriver expected = remote instanceof HasBiDi ? remote : augmented;

        BiDiDriverResolver.ResolvedBiDiDriver resolved = BiDiDriverResolver.resolve(remote, candidate -> {
            augmentations.incrementAndGet();
            assertSame(remote, candidate);
            return augmented;
        }, initialized -> {
            assertSame(expected, initialized);
            initializations.incrementAndGet();
            return null;
        });

        assertSame(expected, resolved.driver());
        assertEquals(remote instanceof HasBiDi ? 0 : 1, augmentations.get());
        assertEquals(1, initializations.get());
        assertEquals("true", resolved.diagnostics().get("remoteSession"));
        assertEquals("true", resolved.diagnostics().get("webSocketUrlCapability"));
        assertEquals(String.valueOf(!(remote instanceof HasBiDi)), resolved.diagnostics().get("driverAugmented"));
        assertTrue(resolved.diagnostics().values().stream()
                .noneMatch(value -> value.contains("remote.invalid")));
    }

    @Test
    void seleniumDecoratorPreservesNativeBiDiOrTheAugmentationInput() {
        WebDriver remote = new StubRemoteWebDriver(new ImmutableCapabilities(
                "browserName", "firefox", "webSocketUrl", "wss://remote.invalid/session"));
        WebDriver decorated = new WebDriverDecorator<>().decorate(remote);
        WebDriver augmented = bidiDriver(false);
        AtomicReference<WebDriver> augmentationInput = new AtomicReference<>();
        WebDriver expected = decorated instanceof HasBiDi ? decorated
                : remote instanceof HasBiDi ? remote : augmented;
        AtomicInteger initializations = new AtomicInteger();

        BiDiDriverResolver.ResolvedBiDiDriver resolved = BiDiDriverResolver.resolve(decorated, candidate -> {
            augmentationInput.set(candidate);
            return augmented;
        }, initialized -> {
            assertSame(expected, initialized);
            initializations.incrementAndGet();
            return null;
        });

        if (remote instanceof HasBiDi || decorated instanceof HasBiDi) {
            assertSame(null, augmentationInput.get());
            assertEquals("false", resolved.diagnostics().get("driverAugmented"));
        } else {
            assertSame(decorated, augmentationInput.get());
            assertEquals("true", resolved.diagnostics().get("driverAugmented"));
        }
        assertSame(expected, resolved.driver());
        assertEquals(1, initializations.get());
        assertEquals("true", resolved.diagnostics().get("remoteSession"));
    }

    @Test
    void unsupportedDriverDoesNotInvokeAugmenterWithoutRemoteEvidence() {
        WebDriver driver = webDriver(new Class<?>[]{WebDriver.class}, false, false);
        AtomicInteger augmentations = new AtomicInteger();

        NetworkCaptureUnsupportedException failure = assertThrows(NetworkCaptureUnsupportedException.class,
                () -> BiDiDriverResolver.resolve(driver, candidate -> {
                    augmentations.incrementAndGet();
                    return candidate;
                }, ignored -> null));

        assertEquals(0, augmentations.get());
        assertEquals(BiDiFailureCategory.UNSUPPORTED, failure.category());
        assertEquals("DISCOVER_INTERFACE", failure.diagnostics().get("initializationStage"));
    }

    @Test
    void retryableLazyInitializationFailureIsRetriedOnceBeforeListenersExist() {
        AtomicInteger attempts = new AtomicInteger();

        BiDiDriverResolver.ResolvedBiDiDriver resolved = BiDiDriverResolver.resolve(
                bidiDriver(false), ignored -> { throw new AssertionError("augmentation not expected"); }, ignored -> {
                    if (attempts.incrementAndGet() == 1) throw new BiDiException("transient endpoint failure");
                    return null;
                });

        assertEquals(2, attempts.get());
        assertEquals("2", resolved.diagnostics().get("initializationAttempts"));
    }

    @Test
    void initializationFailureIsClassifiedWithoutUnboundedRetry() {
        AtomicInteger attempts = new AtomicInteger();

        BiDiCaptureException failure = assertThrows(BiDiCaptureException.class,
                () -> BiDiDriverResolver.resolve(bidiDriver(false), ignored -> null, ignored -> {
                    attempts.incrementAndGet();
                    throw new BiDiException("endpoint unavailable");
                }));

        assertEquals(2, attempts.get());
        assertEquals(BiDiFailureCategory.INITIALIZATION_FAILED, failure.category());
        assertEquals("INITIALIZE_CONNECTION", failure.diagnostics().get("initializationStage"));
    }

    @Test
    void remoteWebSocketInitializationFailureIdentifiesTheUnavailableEndpoint() {
        WebDriver remote = new StubRemoteWebDriver(new ImmutableCapabilities(
                "browserName", "chrome", "webSocketUrl", "wss://remote.invalid/session"));

        BiDiCaptureException failure = assertThrows(BiDiCaptureException.class,
                () -> BiDiDriverResolver.resolve(remote, ignored -> bidiDriver(false), ignored -> {
                    throw new BiDiException("handshake failed");
                }));

        assertEquals(BiDiFailureCategory.REMOTE_ENDPOINT_UNAVAILABLE, failure.category());
        assertEquals("true", failure.diagnostics().get("remoteSession"));
        assertEquals("true", failure.diagnostics().get("webSocketUrlCapability"));
        assertEquals("2", failure.diagnostics().get("initializationAttempts"));
    }

    @Test
    void closedSessionIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();

        BiDiCaptureException failure = assertThrows(BiDiCaptureException.class,
                () -> BiDiDriverResolver.resolve(bidiDriver(false), ignored -> null, ignored -> {
                    attempts.incrementAndGet();
                    throw new NoSuchSessionException("closed");
                }));

        assertEquals(1, attempts.get());
        assertEquals(BiDiFailureCategory.SESSION_CLOSED, failure.category());
    }

    @Test
    void twoRemoteSessionsUseIndependentNativeOrAugmentedViews() {
        WebDriver firstRemote = new StubRemoteWebDriver(new ImmutableCapabilities("webSocketUrl", "wss://one.invalid"));
        WebDriver secondRemote = new StubRemoteWebDriver(new ImmutableCapabilities("webSocketUrl", "wss://two.invalid"));
        WebDriver firstAugmented = bidiDriver(false);
        WebDriver secondAugmented = bidiDriver(false);
        WebDriver firstExpected = firstRemote instanceof HasBiDi ? firstRemote : firstAugmented;
        WebDriver secondExpected = secondRemote instanceof HasBiDi ? secondRemote : secondAugmented;
        AtomicInteger augmentations = new AtomicInteger();
        AtomicInteger initializations = new AtomicInteger();

        BiDiDriverResolver.ResolvedBiDiDriver first = BiDiDriverResolver.resolve(
                firstRemote, candidate -> {
                    assertSame(firstRemote, candidate);
                    augmentations.incrementAndGet();
                    return firstAugmented;
                }, initialized -> {
                    assertSame(firstExpected, initialized);
                    initializations.incrementAndGet();
                    return null;
                });
        BiDiDriverResolver.ResolvedBiDiDriver second = BiDiDriverResolver.resolve(
                secondRemote, candidate -> {
                    assertSame(secondRemote, candidate);
                    augmentations.incrementAndGet();
                    return secondAugmented;
                }, initialized -> {
                    assertSame(secondExpected, initialized);
                    initializations.incrementAndGet();
                    return null;
                });

        assertSame(firstExpected, first.driver());
        assertSame(secondExpected, second.driver());
        assertEquals((firstRemote instanceof HasBiDi ? 0 : 1)
                + (secondRemote instanceof HasBiDi ? 0 : 1), augmentations.get());
        assertEquals(2, initializations.get());
        assertFalse(first.driver() == second.driver());
    }

    private static WebDriver bidiDriver(boolean initialized) {
        return webDriver(new Class<?>[]{WebDriver.class, HasBiDi.class}, initialized, false);
    }

    private static WebDriver webDriver(Class<?>[] interfaces, boolean initialized, boolean webSocketCapability) {
        return (WebDriver) Proxy.newProxyInstance(BiDiDriverResolverTest.class.getClassLoader(), interfaces,
                (proxy, method, args) -> switch (method.getName()) {
                    case "maybeGetBiDi" -> initialized ? Optional.of("initialized") : Optional.empty();
                    case "getCapabilities" -> new ImmutableCapabilities(
                            webSocketCapability ? "webSocketUrl" : "unused",
                            webSocketCapability ? "wss://remote.invalid" : false);
                    case "toString" -> "bidi-test-driver";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }

    private static final class StubRemoteWebDriver extends RemoteWebDriver {
        StubRemoteWebDriver(Capabilities capabilities) {
            super((CommandExecutor) command -> null, capabilities);
        }

        @Override
        protected void startSession(Capabilities desiredCapabilities) {
            capabilities = desiredCapabilities;
            setSessionId("remote-test-session");
        }
    }
}
