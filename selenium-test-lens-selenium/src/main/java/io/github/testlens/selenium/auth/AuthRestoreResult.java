package io.github.testlens.selenium.auth;

import java.time.Duration;

public final class AuthRestoreResult {
    private final AuthRestoreStatus status;
    private final String message;
    private final int cookiesRestored;
    private final int localStorageEntriesRestored;
    private final int sessionStorageEntriesRestored;
    private final Throwable exception;
    private final Duration elapsed;

    private AuthRestoreResult(AuthRestoreStatus status,
                              String message,
                              int cookiesRestored,
                              int localStorageEntriesRestored,
                              int sessionStorageEntriesRestored,
                              Throwable exception,
                              Duration elapsed) {
        this.status = status == null ? AuthRestoreStatus.FAILED : status;
        this.message = message == null ? "" : message;
        this.cookiesRestored = Math.max(0, cookiesRestored);
        this.localStorageEntriesRestored = Math.max(0, localStorageEntriesRestored);
        this.sessionStorageEntriesRestored = Math.max(0, sessionStorageEntriesRestored);
        this.exception = exception;
        this.elapsed = elapsed == null ? Duration.ZERO : elapsed;
    }

    public static AuthRestoreResult restored(String message, int cookies, int localStorage, int sessionStorage, Duration elapsed) {
        return new AuthRestoreResult(AuthRestoreStatus.RESTORED, message, cookies, localStorage, sessionStorage, null, elapsed);
    }

    public static AuthRestoreResult failed(String message, Throwable exception, Duration elapsed) {
        return new AuthRestoreResult(AuthRestoreStatus.FAILED, message, 0, 0, 0, exception, elapsed);
    }

    public static AuthRestoreResult expired(String message, Duration elapsed) {
        return new AuthRestoreResult(AuthRestoreStatus.EXPIRED, message, 0, 0, 0, null, elapsed);
    }

    public static AuthRestoreResult originMismatch(String message, Duration elapsed) {
        return new AuthRestoreResult(AuthRestoreStatus.ORIGIN_MISMATCH, message, 0, 0, 0, null, elapsed);
    }

    public static AuthRestoreResult skipped(String message, Duration elapsed) {
        return new AuthRestoreResult(AuthRestoreStatus.SKIPPED, message, 0, 0, 0, null, elapsed);
    }

    public AuthRestoreStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public int cookiesRestored() {
        return cookiesRestored;
    }

    public int localStorageEntriesRestored() {
        return localStorageEntriesRestored;
    }

    public int sessionStorageEntriesRestored() {
        return sessionStorageEntriesRestored;
    }

    public Throwable exception() {
        return exception;
    }

    public Duration elapsed() {
        return elapsed;
    }
}
