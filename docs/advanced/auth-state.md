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

With the default `validateOrigin(true)`, restore follows a mutation-before-validation-safe sequence:

```text
preflight state origins
-> optional navigation
-> post-navigation origin validation
-> origin recheck and cookie restore
-> atomically origin-guarded storage clear/write
```

Origin comparison canonicalizes scheme and host case and treats the HTTP/HTTPS default ports as equivalent, while preserving non-default ports. Paths, query strings, and fragments do not affect the comparison. A redirect to another scheme, host, or effective port—including an SSO origin—returns `ORIGIN_MISMATCH` with zero restored counters before cookies or storage on that origin are changed. Empty, opaque, malformed, and `about:blank` URLs do not authorize restore. Storage entries that explicitly name a different origin are rejected during preflight before navigation.

Storage JavaScript checks `window.location.origin` in the same script invocation that performs `clear()` or `setItem()`. This closes the gap in which the active origin could change after the Java-side check. Cookie mutation remains implemented by Selenium and receives an additional current-origin check immediately before that stage.

`validateOrigin(false)` is an explicit security opt-out. It permits restore to mutate cookies and storage for whichever origin is active, so use it only when the surrounding navigation and origin are independently controlled. Test Lens does not restore application auth data into cross-origin SSO storage automatically; restore state only on the origin from which it was captured.

Supporting public types have these roles: `AuthStateJsonExporter` writes state JSON, while `AuthStateJsonParser` reads it; malformed or unsupported state data can surface as `AuthStateException`. `AuthStorageType` distinguishes `LOCAL_STORAGE` from `SESSION_STORAGE`. `AuthRestoreStatus` distinguishes `RESTORED`, `FAILED`, `SKIPPED`, `EXPIRED`, and `ORIGIN_MISMATCH`; inspect the full `AuthRestoreResult` rather than treating every non-restored status as the same failure.

## Security

Auth-state JSON can contain live session cookies, bearer-like storage values, user identifiers, domains, and expiry data. Never commit it, paste it into docs/logs, or expose it as an unrestricted CI artifact. Keep generated state under an ignored, access-controlled path; use short expiry and test-only accounts.

## Options

Every builder option/default is tabulated in [Configuration](../reference/configuration.md#authentication-options). `AuthCookie`, `AuthStorageEntry`, `AuthStateMetadata`, and result/status/storage enums are documented by signature in the [catalog](../reference/public-api-catalog.md).
