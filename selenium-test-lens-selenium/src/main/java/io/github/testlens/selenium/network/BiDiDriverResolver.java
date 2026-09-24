package io.github.testlens.selenium.network;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.bidi.BiDiException;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.decorators.Decorated;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves a BiDi-capable view without replacing the consumer's WebDriver reference. */
final class BiDiDriverResolver {
    private static final int MAX_UNWRAP_DEPTH = 16;
    private static final int MAX_INITIALIZATION_ATTEMPTS = 2;

    private BiDiDriverResolver() {}

    static ResolvedBiDiDriver resolve(WebDriver initial) {
        return resolve(initial, driver -> new Augmenter().augment(driver), HasBiDi::getBiDi);
    }

    static ResolvedBiDiDriver resolve(WebDriver initial,
                                      DriverAugmenter augmenter,
                                      BiDiInitializer initializer) {
        if (initial == null) {
            throw new NetworkCaptureUnsupportedException("WebDriver is required for BiDi capture");
        }
        List<WebDriver> candidates = driverChain(initial);
        DriverFacts facts = DriverFacts.from(candidates);
        for (WebDriver candidate : candidates) {
            if (candidate instanceof HasBiDi hasBiDi) {
                return initialize(candidate, hasBiDi, false, facts.withHasBiDi(true), initializer);
            }
        }

        if (!facts.remote() && !facts.webSocketUrlCapability()) {
            throw unsupported(facts);
        }

        WebDriver augmented;
        try {
            augmented = augmenter.augment(initial);
        } catch (RuntimeException failure) {
            throw failure(BiDiFailureCategory.INITIALIZATION_FAILED,
                    "Selenium could not augment the remote WebDriver for BiDi", facts, "AUGMENT_DRIVER", 0, failure);
        }
        if (!(augmented instanceof HasBiDi hasBiDi)) {
            throw unsupported(facts);
        }
        return initialize(augmented, hasBiDi, true, facts.withHasBiDi(true), initializer);
    }

    private static ResolvedBiDiDriver initialize(WebDriver driver,
                                                  HasBiDi hasBiDi,
                                                  boolean augmented,
                                                  DriverFacts facts,
                                                  BiDiInitializer initializer) {
        boolean initiallyInitialized;
        try {
            initiallyInitialized = hasBiDi.maybeGetBiDi().isPresent();
        } catch (RuntimeException ignored) {
            initiallyInitialized = false;
        }
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= MAX_INITIALIZATION_ATTEMPTS; attempt++) {
            try {
                BiDi connection = initializer.initialize(hasBiDi);
                Map<String, String> diagnostics = facts.diagnostics();
                diagnostics.put("bidiInitiallyInitialized", String.valueOf(initiallyInitialized));
                diagnostics.put("driverAugmented", String.valueOf(augmented));
                diagnostics.put("initializationAttempts", String.valueOf(attempt));
                return new ResolvedBiDiDriver(driver, augmented ? connection : null, diagnostics);
            } catch (NoSuchSessionException closed) {
                throw failure(BiDiFailureCategory.SESSION_CLOSED,
                        "WebDriver session closed before BiDi initialization", facts,
                        "INITIALIZE_CONNECTION", attempt, closed);
            } catch (RuntimeException failure) {
                lastFailure = failure;
                if (!(failure instanceof BiDiException) || attempt == MAX_INITIALIZATION_ATTEMPTS) {
                    BiDiFailureCategory category = facts.remote() && facts.webSocketUrlCapability()
                            ? BiDiFailureCategory.REMOTE_ENDPOINT_UNAVAILABLE
                            : BiDiFailureCategory.INITIALIZATION_FAILED;
                    throw failure(category, "Unable to initialize the WebDriver BiDi connection", facts,
                            "INITIALIZE_CONNECTION", attempt, failure);
                }
            }
        }
        throw failure(BiDiFailureCategory.INITIALIZATION_FAILED,
                "Unable to initialize the WebDriver BiDi connection", facts,
                "INITIALIZE_CONNECTION", MAX_INITIALIZATION_ATTEMPTS, lastFailure);
    }

    private static NetworkCaptureUnsupportedException unsupported(DriverFacts facts) {
        Map<String, String> diagnostics = facts.diagnostics();
        diagnostics.put("failureCategory", BiDiFailureCategory.UNSUPPORTED.name());
        diagnostics.put("initializationStage", "DISCOVER_INTERFACE");
        return new NetworkCaptureUnsupportedException(
                "WebDriver session does not expose Selenium BiDi; enable BiDi when creating the session",
                diagnostics);
    }

    private static BiDiCaptureException failure(BiDiFailureCategory category,
                                                String message,
                                                DriverFacts facts,
                                                String stage,
                                                int attempts,
                                                Throwable cause) {
        Map<String, String> diagnostics = facts.diagnostics();
        diagnostics.put("failureCategory", category.name());
        diagnostics.put("initializationStage", stage);
        diagnostics.put("initializationAttempts", String.valueOf(attempts));
        return new BiDiCaptureException(category, message, diagnostics, cause);
    }

    private static List<WebDriver> driverChain(WebDriver initial) {
        List<WebDriver> result = new ArrayList<>();
        IdentityHashMap<WebDriver, Boolean> seen = new IdentityHashMap<>();
        WebDriver current = initial;
        for (int depth = 0; current != null && depth < MAX_UNWRAP_DEPTH; depth++) {
            if (seen.put(current, Boolean.TRUE) != null) {
                throw new NetworkCaptureUnsupportedException("Cyclic WebDriver wrapper chain prevents BiDi discovery");
            }
            result.add(current);
            WebDriver next = null;
            if (current instanceof Decorated<?> decorated && decorated.getOriginal() instanceof WebDriver original) {
                next = original;
            } else if (current instanceof WrapsDriver wrapsDriver) {
                next = wrapsDriver.getWrappedDriver();
            }
            if (next == null) break;
            if (next == current) {
                throw new NetworkCaptureUnsupportedException("Cyclic WebDriver wrapper chain prevents BiDi discovery");
            }
            if (depth == MAX_UNWRAP_DEPTH - 1) {
                throw new NetworkCaptureUnsupportedException(
                        "WebDriver wrapper depth exceeds the BiDi discovery limit");
            }
            current = next;
        }
        return result;
    }

    record ResolvedBiDiDriver(WebDriver driver, BiDi ownedConnection, Map<String, String> diagnostics) {}

    @FunctionalInterface
    interface DriverAugmenter {
        WebDriver augment(WebDriver driver);
    }

    @FunctionalInterface
    interface BiDiInitializer {
        BiDi initialize(HasBiDi hasBiDi);
    }

    private record DriverFacts(String driverType, boolean remote,
                               boolean hasBiDi, boolean webSocketUrlCapability) {
        static DriverFacts from(List<WebDriver> drivers) {
            String type = drivers.isEmpty() ? "unknown" : drivers.get(0).getClass().getName();
            boolean remote = false;
            boolean hasBiDi = false;
            boolean webSocket = false;
            for (WebDriver driver : drivers) {
                remote |= driver instanceof RemoteWebDriver;
                hasBiDi |= driver instanceof HasBiDi;
                if (driver instanceof HasCapabilities hasCapabilities) {
                    try {
                        Capabilities capabilities = hasCapabilities.getCapabilities();
                        webSocket |= capabilities != null && capabilities.getCapability("webSocketUrl") != null;
                    } catch (RuntimeException ignored) {
                        // Capability diagnostics are best-effort; interface discovery remains authoritative.
                    }
                }
            }
            return new DriverFacts(type, remote || webSocket, hasBiDi, webSocket);
        }

        DriverFacts withHasBiDi(boolean value) {
            return new DriverFacts(driverType, remote, value, webSocketUrlCapability);
        }

        Map<String, String> diagnostics() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("driverType", driverType);
            values.put("remoteSession", String.valueOf(remote));
            values.put("hasBiDi", String.valueOf(hasBiDi));
            values.put("webSocketUrlCapability", String.valueOf(webSocketUrlCapability));
            return values;
        }
    }
}
