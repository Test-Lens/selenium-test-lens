package io.github.testlens.selenium.auth;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/*
 * Managed lifecycle invariants:
 * I1 INCONCLUSIVE never triggers login. I2 one invocation logs in at most once.
 * I3/I4 persistence happens only after authenticated capture and failed replacement preserves old bytes.
 * I5 every decision and mutation is inside the canonical-path JVM + filesystem lock.
 * I6/I7 no logout and no driver close/replacement exist in this layer.
 * I8 managed events contain no request/state data. I9 every restored state is application-validated.
 * I10 low-level capture/restore/save/load remain separate and backward compatible.
 */
final class AuthStateLifecycle {
    private final WebDriver driver;
    private final AuthStateManager primitives;
    private final OverlayLogger logger;
    private final AuthStateStore store;
    private final AuthStateLockManager locks;
    private final AuthStateRegistrationRegistry registry;

    AuthStateLifecycle(WebDriver driver, AuthStateManager primitives, OverlayLogger logger,
                       AuthStateStore store, AuthStateLockManager locks,
                       AuthStateRegistrationRegistry registry) {
        this.driver = driver;
        this.primitives = primitives;
        this.logger = logger;
        this.store = store;
        this.locks = locks;
        this.registry = registry;
    }

    AuthStateEnsureResult ensure(AuthStateRequest request) {
        if (request == null) throw new IllegalArgumentException("request must not be null");
        Instant started = Instant.now();
        Path canonical = locks.canonicalize(request.path());
        AuthStateRegistrationRegistry.Registration registration = registry.register(request, canonical);
        return locks.withLock(canonical, () -> ensureLocked(registration.request(), canonical, started));
    }

    AuthStateEnsureResult refresh(String key) {
        Instant started = Instant.now();
        AuthStateRegistrationRegistry.Registration registration = registry.require(key);
        return locks.withLock(registration.canonicalPath(), () ->
                recreate(registration.request(), registration.canonicalPath(), true, started));
    }

    void invalidate(String key) {
        Instant started = Instant.now();
        AuthStateRegistrationRegistry.Registration registration = registry.require(key);
        locks.withLock(registration.canonicalPath(), () -> {
            store.invalidate(registration.canonicalPath());
            emit(UiTestLensEventType.AUTH_STATE_INVALIDATED, UiTestLensStatus.PASSED,
                    UiTestLensLogLevel.INFO, null, null, elapsed(started));
            return null;
        });
    }

    private AuthStateEnsureResult ensureLocked(AuthStateRequest request, Path path, Instant started) {
        if (!store.exists(path)) return create(request, path, started);
        AuthState state;
        try {
            state = store.load(path);
        } catch (RuntimeException corrupt) {
            if (corrupt.getCause() instanceof java.io.IOException) {
                throw failure(ManagedAuthStateFailureReason.RESTORE_FAILED,
                        "Managed auth state could not be read", corrupt);
            }
            return recreate(request, path, true, started);
        }

        AuthRestoreResult restore = primitives.restoreState(state, AuthRestoreOptions.defaults());
        return switch (restore.status()) {
            case RESTORED -> afterRestore(request, path, started);
            case EXPIRED, ORIGIN_MISMATCH -> recreate(request, path, true, started);
            case FAILED, SKIPPED -> throw failure(ManagedAuthStateFailureReason.RESTORE_FAILED,
                    "Managed auth state restore failed", restore.exception());
        };
    }

    private AuthStateEnsureResult afterRestore(AuthStateRequest request, Path path, Instant started) {
        AuthStateValidation validation = validate(request);
        return switch (validation) {
            case AUTHENTICATED -> success(AuthStateEnsureOutcome.RESTORED, started);
            case UNAUTHENTICATED -> recreate(request, path, true, started);
            case INCONCLUSIVE -> inconclusive(started);
        };
    }

    private AuthStateEnsureResult create(AuthStateRequest request, Path path, Instant started) {
        login(request);
        requireAuthenticatedAfterLogin(request, started);
        AuthState state = capture();
        store.atomicReplace(path, state);
        return success(AuthStateEnsureOutcome.CREATED, started);
    }

    private AuthStateEnsureResult recreate(AuthStateRequest request, Path path, boolean oldStateExists, Instant started) {
        clearManagedBrowserState();
        login(request);
        requireAuthenticatedAfterLogin(request, started);
        AuthState state = capture();
        store.atomicReplace(path, state);
        return success(oldStateExists ? AuthStateEnsureOutcome.REFRESHED : AuthStateEnsureOutcome.CREATED, started);
    }

    private void login(AuthStateRequest request) {
        try {
            request.login().login(driver);
        } catch (RuntimeException e) {
            throw failure(ManagedAuthStateFailureReason.LOGIN_FAILED, "Managed auth state login failed", e);
        }
    }

    private AuthStateValidation validate(AuthStateRequest request) {
        try {
            AuthStateValidation result = request.validator().validate(driver);
            if (result == null) {
                throw failure(ManagedAuthStateFailureReason.VALIDATION_FAILED,
                        "Managed auth state validation returned no result", null);
            }
            return result;
        } catch (ManagedAuthStateException e) {
            throw e;
        } catch (RuntimeException e) {
            throw failure(ManagedAuthStateFailureReason.VALIDATION_FAILED,
                    "Managed auth state validation failed", e);
        }
    }

    private void requireAuthenticatedAfterLogin(AuthStateRequest request, Instant started) {
        switch (validate(request)) {
            case AUTHENTICATED -> { }
            case UNAUTHENTICATED -> throw failure(ManagedAuthStateFailureReason.LOGIN_DID_NOT_AUTHENTICATE,
                    "Managed auth state login was not authenticated", null);
            case INCONCLUSIVE -> inconclusive(started);
        }
    }

    private AuthStateEnsureResult inconclusive(Instant started) {
        emit(UiTestLensEventType.AUTH_STATE_VALIDATION_INCONCLUSIVE, UiTestLensStatus.FAILED,
                UiTestLensLogLevel.WARN, null, ManagedAuthStateFailureReason.VALIDATION_INCONCLUSIVE, elapsed(started));
        throw failure(ManagedAuthStateFailureReason.VALIDATION_INCONCLUSIVE,
                "Managed auth state validation was inconclusive", null);
    }

    private AuthState capture() {
        try {
            return primitives.captureState(AuthStateOptions.defaults());
        } catch (RuntimeException e) {
            throw failure(ManagedAuthStateFailureReason.CAPTURE_FAILED,
                    "Managed auth state capture failed", e);
        }
    }

    private void clearManagedBrowserState() {
        try {
            primitives.clearManagedBrowserState();
        } catch (RuntimeException e) {
            throw failure(ManagedAuthStateFailureReason.BROWSER_STATE_CLEAR_FAILED,
                    "Managed browser auth state clearing failed", e);
        }
    }

    private AuthStateEnsureResult success(AuthStateEnsureOutcome outcome, Instant started) {
        Duration duration = elapsed(started);
        UiTestLensEventType type = switch (outcome) {
            case RESTORED -> UiTestLensEventType.AUTH_STATE_RESTORED;
            case CREATED -> UiTestLensEventType.AUTH_STATE_CREATED;
            case REFRESHED -> UiTestLensEventType.AUTH_STATE_REFRESHED;
        };
        emit(type, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO, outcome, null, duration);
        return new AuthStateEnsureResult(outcome, duration);
    }

    private void emit(UiTestLensEventType type, UiTestLensStatus status, UiTestLensLogLevel level,
                      AuthStateEnsureOutcome outcome, ManagedAuthStateFailureReason reason, Duration duration) {
        try {
            UiTestLensLogEntry.Builder entry = UiTestLensLogEntry.builder().eventType(type).status(status).level(level)
                    .action("auth.state.managed").message("Managed auth state lifecycle event")
                    .metadata("durationMs", String.valueOf(Math.max(0, duration.toMillis())));
            if (outcome != null) entry.metadata("outcome", outcome.name());
            if (reason != null) entry.metadata("reason", reason.name());
            logger.emit(entry.build());
        } catch (RuntimeException ignored) {}
    }

    private static ManagedAuthStateException failure(ManagedAuthStateFailureReason reason,
                                                      String safeMessage, Throwable cause) {
        return new ManagedAuthStateException(reason, safeMessage, cause);
    }

    private static Duration elapsed(Instant started) { return Duration.between(started, Instant.now()); }
}
