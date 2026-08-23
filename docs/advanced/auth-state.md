# Authentication state

Package: `io.github.testlens.selenium.auth`<br>
Module: `selenium-test-lens-selenium`<br>
API level: **Advanced**

## AuthStateManager

<!-- API SIGNATURES: io.github.testlens.selenium.auth.AuthStateManager -->
```java
AuthStateManager(WebDriver driver)
AuthStateManager(WebDriver driver, OverlayLogger logger)
AuthState captureState(AuthStateOptions options)
AuthRestoreResult restoreState(AuthState state, AuthRestoreOptions options)
AuthState load(Path path)
AuthRestoreResult restoreState(Path path, AuthRestoreOptions options)
```

Capture reads selected cookies/local/session storage for an origin and returns `AuthState`. JSON export/parser types serialize it. Restore can navigate to the origin, clear existing cookies/storage, restore chosen components, validate origin, and reject expired state. Browser origin/security rules and WebDriver cookie rules still apply; restore results report counts/status/exception/elapsed time.

Supporting public types have these roles: `AuthStateJsonExporter` writes state JSON, while `AuthStateJsonParser` reads it; malformed or unsupported state data can surface as `AuthStateException`. `AuthStorageType` distinguishes `LOCAL_STORAGE` from `SESSION_STORAGE`. `AuthRestoreStatus` distinguishes `RESTORED`, `FAILED`, `SKIPPED`, `EXPIRED`, and `ORIGIN_MISMATCH`; inspect the full `AuthRestoreResult` rather than treating every non-restored status as the same failure.

## Security

Auth-state JSON can contain live session cookies, bearer-like storage values, user identifiers, domains, and expiry data. Never commit it, paste it into docs/logs, or expose it as an unrestricted CI artifact. Keep generated state under an ignored, access-controlled path; use short expiry and test-only accounts.

## Options

Every builder option/default is tabulated in [Configuration](../reference/configuration.md#authentication-options). `AuthCookie`, `AuthStorageEntry`, `AuthStateMetadata`, and result/status/storage enums are documented by signature in the [catalog](../reference/public-api-catalog.md).
