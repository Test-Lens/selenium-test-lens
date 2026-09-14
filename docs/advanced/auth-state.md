# Authentication state

Package: `io.github.testlens.selenium.auth`<br>
Module: `selenium-test-lens-selenium`<br>
API level: **User API for managed lifecycle; Advanced for low-level primitives**

## Managed Auth State (0.3.0)

For the normal workflow, define only where state is stored, how the application performs a real login, and how
the application unambiguously validates authentication:

```java
import static io.github.testlens.selenium.auth.AuthStateValidation.AUTHENTICATED;
import static io.github.testlens.selenium.auth.AuthStateValidation.UNAUTHENTICATED;

AuthStateEnsureResult result = lens.authState().ensure(
        AuthStateRequest.builder()
                .key("primary-user")
                .path(Path.of("target/ui-test-lens/authstate/primary.json"))
                .login(driver -> loginPage.login(username, password))
                .validate(driver -> accountMenu.isDisplayed()
                        ? AUTHENTICATED
                        : UNAUTHENTICATED)
                .build()
);
```

`key` is a process-local lifecycle identifier used by `refresh(key)` and `invalidate(key)`. It is not a
credential, but it can still contain personal data, so Test Lens does not include it in lifecycle events or result
strings. Requests are registered on their owning `AuthStateManager`/`TestLens` instance. Reusing a key for a
different canonical path fails fast; callback identity is deliberately not compared.

Validation is tri-state:

- `AUTHENTICATED` unambiguously confirms a valid application session.
- `UNAUTHENTICATED` unambiguously confirms that the application session is not valid.
- `INCONCLUSIVE` represents a timeout, unavailable application/backend, transient network failure, or any state
  where logged-out cannot be distinguished reliably from an outage. It fails the operation without login and
  without overwriting persisted state. A validator exception is a separate `VALIDATION_FAILED` execution failure;
  its original cause is retained and it never triggers login.

The bounded lifecycle is:

```text
missing: login once -> validate AUTHENTICATED -> capture -> atomic save -> CREATED
valid:   restore -> validate AUTHENTICATED -> RESTORED (no login, no write)
invalid: restore -> UNAUTHENTICATED -> clear managed browser state -> login once
         -> validate AUTHENTICATED -> capture -> atomic replace -> REFRESHED
inconclusive: fail -> no login -> no overwrite
corrupt/expired/wrong origin: clear managed browser state -> login once -> validate
                              -> capture -> atomic replace, or fail with old bytes preserved
```

Every `ensure` performs at most one login and has no retry loop. Technical restore success is never accepted as
authentication success without the application validator. Before recreation Test Lens clears only the browser
state it manages—cookies, local storage and session storage. It never performs a business logout, invokes a
logout callback, closes/restarts the WebDriver, creates another driver, or automatically logs out after a test.

The full decision is serialized by a canonical-path JVM lock and a stable sibling `.lock` file. Capture is
serialized to a temporary file in the target directory, flushed, parsed, and moved with
`ATOMIC_MOVE + REPLACE_EXISTING`. Unsupported atomic moves fail with `PERSIST_FAILED`; there is no silent
non-atomic fallback. Until that move succeeds, an existing file remains byte-for-byte unchanged.

Explicit lifecycle operations reuse the registered request:

```java
AuthStateEnsureResult refreshed = lens.authState().refresh("primary-user");
lens.authState().invalidate("primary-user");
```

`refresh` keeps the old file until login, validation, capture and atomic replacement all succeed. `invalidate`
deletes only the persisted file under the same lock; it retains registration and performs no login, logout, or
browser-state clearing. Unknown keys fail with `UNKNOWN_KEY`.

## AuthStateManager

<!-- API SIGNATURES: io.github.testlens.selenium.auth.AuthStateManager -->
```java
AuthStateManager(WebDriver driver)
AuthStateManager(WebDriver driver, OverlayLogger logger)
AuthStateEnsureResult ensure(AuthStateRequest request)
AuthStateEnsureResult refresh(String key)
void invalidate(String key)
AuthState captureState(AuthStateOptions options)
AuthRestoreResult restoreState(AuthState state, AuthRestoreOptions options)
AuthState load(Path path)
AuthRestoreResult restoreState(Path path, AuthRestoreOptions options)
```

Capture reads selected cookies/local/session storage for an origin and returns `AuthState`. JSON export/parser types serialize it. Restore can navigate to the origin, clear existing cookies/storage, restore chosen components, validate origin, and reject expired state. Browser origin/security rules and WebDriver cookie rules still apply; restore results report counts/status/exception/elapsed time.

Auth state is a controlled same-origin test setup mechanism, not automatic login federation. In particular, it does not copy application credentials into cross-origin SSO storage.

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

Managed lifecycle events contain only outcome/reason/duration fields. They omit key, path, callbacks, URLs,
cookies and storage contents, and still pass through the central `RedactionPolicy`. The persisted state file itself
is replayable authentication material and is outside report redaction; consumers must protect it with their own
filesystem permissions, CI artifact policy and secret-handling controls.

## Options

Every builder option/default is tabulated in [Configuration](../reference/configuration.md#authentication-options). `AuthCookie`, `AuthStorageEntry`, `AuthStateMetadata`, and result/status/storage enums are documented by signature in the [catalog](../reference/public-api-catalog.md).
