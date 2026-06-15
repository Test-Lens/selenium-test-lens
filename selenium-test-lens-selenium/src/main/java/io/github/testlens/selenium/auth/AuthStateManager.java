package io.github.testlens.selenium.auth;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AuthStateManager {
    private static final String READ_STORAGE_SCRIPT = """
            const storage = arguments[0] === 'session' ? window.sessionStorage : window.localStorage;
            const result = {};
            for (let i = 0; i < storage.length; i++) {
              const key = storage.key(i);
              result[key] = storage.getItem(key);
            }
            return result;
            """;
    private static final String CLEAR_STORAGE_SCRIPT = "window.localStorage.clear(); window.sessionStorage.clear();";
    private static final String SET_STORAGE_SCRIPT = """
            const storage = arguments[0] === 'session' ? window.sessionStorage : window.localStorage;
            storage.setItem(arguments[1], arguments[2]);
            """;

    private final WebDriver driver;
    private final OverlayLogger logger;

    public AuthStateManager(WebDriver driver) {
        this(driver, OverlayLogger.noop());
    }

    public AuthStateManager(WebDriver driver, OverlayLogger logger) {
        if (driver == null) {
            throw new IllegalArgumentException("driver must not be null");
        }
        this.driver = driver;
        this.logger = logger == null ? OverlayLogger.noop() : logger;
    }

    public AuthState captureState(AuthStateOptions options) {
        AuthStateOptions effectiveOptions = options == null ? AuthStateOptions.defaults() : options;
        emit(UiTestLensEventType.AUTH_STATE_CAPTURE_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Auth state capture started", effectiveOptions.label(), effectiveOptions.role(), effectiveOptions.origin(), 0, 0, 0, null);
        try {
            String currentUrl = safeCurrentUrl();
            String origin = !effectiveOptions.origin().isBlank() ? effectiveOptions.origin() : originOf(currentUrl);
            String domain = hostOf(origin.isBlank() ? currentUrl : origin);
            List<AuthCookie> cookies = effectiveOptions.includeCookies() ? captureCookies() : List.of();
            List<AuthStorageEntry> localStorage = effectiveOptions.includeLocalStorage()
                    ? captureStorage(origin, AuthStorageType.LOCAL_STORAGE)
                    : List.of();
            List<AuthStorageEntry> sessionStorage = effectiveOptions.includeSessionStorage()
                    ? captureStorage(origin, AuthStorageType.SESSION_STORAGE)
                    : List.of();
            AuthStateMetadata.Builder metadata = AuthStateMetadata.builder()
                    .label(effectiveOptions.label())
                    .role(effectiveOptions.role())
                    .origin(origin)
                    .domain(domain)
                    .expiresAt(effectiveOptions.expiresAt())
                    .labels(effectiveOptions.labels())
                    .notes(effectiveOptions.notes());
            AuthState state = new AuthState(metadata.build(), cookies, localStorage, sessionStorage);
            emit(UiTestLensEventType.AUTH_STATE_CAPTURE_PASSED, UiTestLensStatus.PASSED, UiTestLensLogLevel.INFO,
                    "Auth state captured", state.metadata().label(), state.metadata().role(), state.metadata().origin(),
                    cookies.size(), localStorage.size(), sessionStorage.size(), null);
            return state;
        } catch (RuntimeException e) {
            emit(UiTestLensEventType.AUTH_STATE_CAPTURE_FAILED, UiTestLensStatus.FAILED, UiTestLensLogLevel.ERROR,
                    "Auth state capture failed", effectiveOptions.label(), effectiveOptions.role(), effectiveOptions.origin(), 0, 0, 0, e);
            throw e;
        }
    }

    public AuthRestoreResult restoreState(AuthState state, AuthRestoreOptions options) {
        AuthRestoreOptions effectiveOptions = options == null ? AuthRestoreOptions.defaults() : options;
        Instant started = Instant.now();
        if (state == null) {
            AuthRestoreResult result = AuthRestoreResult.failed("Auth state must not be null", null, elapsedSince(started));
            emitRestore(result, "", "", "");
            return result;
        }
        AuthStateMetadata metadata = state.metadata();
        emit(UiTestLensEventType.AUTH_STATE_RESTORE_STARTED, UiTestLensStatus.STARTED, UiTestLensLogLevel.INFO,
                "Auth state restore started", metadata.label(), metadata.role(), metadata.origin(), 0, 0, 0, null);
        try {
            if (effectiveOptions.failIfExpired()
                    && metadata.expiresAt() != null
                    && metadata.expiresAt().isBefore(Instant.now())) {
                AuthRestoreResult result = AuthRestoreResult.expired("Auth state expired", elapsedSince(started));
                emitRestore(result, metadata.label(), metadata.role(), metadata.origin());
                return result;
            }
            if (effectiveOptions.validateOrigin() && !metadata.origin().isBlank()) {
                String currentOrigin = originOf(safeCurrentUrl());
                if (!currentOrigin.isBlank() && !currentOrigin.equals(metadata.origin()) && !effectiveOptions.navigateToOrigin()) {
                    AuthRestoreResult result = AuthRestoreResult.originMismatch(
                            "Current origin does not match auth state origin", elapsedSince(started));
                    emitRestore(result, metadata.label(), metadata.role(), metadata.origin());
                    return result;
                }
            }
            if (effectiveOptions.navigateToOrigin() && !metadata.origin().isBlank()) {
                driver.get(metadata.origin());
            }
            if (effectiveOptions.clearExistingCookies()) {
                driver.manage().deleteAllCookies();
            }
            int cookies = 0;
            if (effectiveOptions.restoreCookies()) {
                for (AuthCookie cookie : state.cookies()) {
                    driver.manage().addCookie(cookie.toSeleniumCookie());
                    cookies++;
                }
            }
            JavascriptExecutor js = requireJavascriptExecutor();
            if (effectiveOptions.clearExistingStorage()) {
                js.executeScript(CLEAR_STORAGE_SCRIPT);
            }
            int localStorage = 0;
            if (effectiveOptions.restoreLocalStorage()) {
                for (AuthStorageEntry entry : state.localStorage()) {
                    js.executeScript(SET_STORAGE_SCRIPT, "local", entry.key(), entry.value());
                    localStorage++;
                }
            }
            int sessionStorage = 0;
            if (effectiveOptions.restoreSessionStorage()) {
                for (AuthStorageEntry entry : state.sessionStorage()) {
                    js.executeScript(SET_STORAGE_SCRIPT, "session", entry.key(), entry.value());
                    sessionStorage++;
                }
            }
            AuthRestoreResult result = AuthRestoreResult.restored(
                    "Auth state restored", cookies, localStorage, sessionStorage, elapsedSince(started));
            emitRestore(result, metadata.label(), metadata.role(), metadata.origin());
            return result;
        } catch (RuntimeException e) {
            AuthRestoreResult result = AuthRestoreResult.failed("Auth state restore failed: " + messageFor(e), e, elapsedSince(started));
            emitRestore(result, metadata.label(), metadata.role(), metadata.origin());
            return result;
        }
    }

    public AuthState load(Path path) {
        return AuthState.load(path);
    }

    public AuthRestoreResult restoreState(Path path, AuthRestoreOptions options) {
        return restoreState(load(path), options);
    }

    private List<AuthCookie> captureCookies() {
        Set<Cookie> seleniumCookies = driver.manage().getCookies();
        List<AuthCookie> cookies = new ArrayList<>();
        for (Cookie cookie : seleniumCookies == null ? Set.<Cookie>of() : seleniumCookies) {
            cookies.add(AuthCookie.fromSeleniumCookie(cookie));
        }
        return cookies;
    }

    private List<AuthStorageEntry> captureStorage(String origin, AuthStorageType type) {
        JavascriptExecutor js = requireJavascriptExecutor();
        String kind = type == AuthStorageType.SESSION_STORAGE ? "session" : "local";
        Object response = js.executeScript(READ_STORAGE_SCRIPT, kind);
        List<AuthStorageEntry> entries = new ArrayList<>();
        if (response instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    entries.add(new AuthStorageEntry(origin, String.valueOf(entry.getKey()), String.valueOf(entry.getValue()), type));
                }
            }
        }
        return entries;
    }

    private JavascriptExecutor requireJavascriptExecutor() {
        if (!(driver instanceof JavascriptExecutor executor)) {
            throw new AuthStateException("WebDriver must implement JavascriptExecutor for storage state");
        }
        return executor;
    }

    private void emitRestore(AuthRestoreResult result, String label, String role, String origin) {
        UiTestLensEventType eventType = switch (result.status()) {
            case RESTORED -> UiTestLensEventType.AUTH_STATE_RESTORE_PASSED;
            case SKIPPED, EXPIRED, ORIGIN_MISMATCH -> UiTestLensEventType.AUTH_STATE_RESTORE_SKIPPED;
            case FAILED -> UiTestLensEventType.AUTH_STATE_RESTORE_FAILED;
        };
        UiTestLensStatus status = switch (result.status()) {
            case RESTORED -> UiTestLensStatus.PASSED;
            case SKIPPED, EXPIRED, ORIGIN_MISMATCH -> UiTestLensStatus.SKIPPED;
            case FAILED -> UiTestLensStatus.FAILED;
        };
        UiTestLensLogLevel level = result.status() == AuthRestoreStatus.FAILED ? UiTestLensLogLevel.ERROR : UiTestLensLogLevel.INFO;
        emit(eventType, status, level, result.message(), label, role, origin,
                result.cookiesRestored(), result.localStorageEntriesRestored(), result.sessionStorageEntriesRestored(), result.exception());
    }

    private void emit(UiTestLensEventType eventType,
                      UiTestLensStatus status,
                      UiTestLensLogLevel level,
                      String message,
                      String label,
                      String role,
                      String origin,
                      int cookies,
                      int localStorage,
                      int sessionStorage,
                      Throwable throwable) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message)
                    .action("auth.state")
                    .metadata("label", safe(label))
                    .metadata("role", safe(role))
                    .metadata("origin", safe(origin))
                    .metadata("cookies", String.valueOf(cookies))
                    .metadata("localStorageEntries", String.valueOf(localStorage))
                    .metadata("sessionStorageEntries", String.valueOf(sessionStorage))
                    .throwable(throwable)
                    .build());
        } catch (RuntimeException ignored) {}
    }

    static String originOf(String url) {
        try {
            if (url == null || url.isBlank()) {
                return "";
            }
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port == -1 ? "" : ":" + port);
        } catch (RuntimeException e) {
            return "";
        }
    }

    static String hostOf(String url) {
        try {
            if (url == null || url.isBlank()) {
                return "";
            }
            URI uri = URI.create(url);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private String safeCurrentUrl() {
        try {
            return driver.getCurrentUrl();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static Duration elapsedSince(Instant started) {
        return Duration.between(started, Instant.now());
    }

    private static String messageFor(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
