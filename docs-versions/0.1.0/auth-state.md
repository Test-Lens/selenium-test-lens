# Authentication state

**Selenium Test Lens 0.1.0 — stable**

`AuthStateManager` can capture cookies plus `localStorage` and `sessionStorage` entries from the current application origin. The state can be saved as JSON and restored in another browser session.

```java
AuthStateManager auth = new AuthStateManager(driver);

AuthState state = auth.captureState(AuthStateOptions.builder()
    .label("signed-in user")
    .role("buyer")
    .origin("https://app.example.test")
    .build());

state.save(Path.of("target/auth/buyer.json"));
```

Restore a previously captured state explicitly:

```java
AuthState saved = auth.load(Path.of("target/auth/buyer.json"));
AuthRestoreResult result = auth.restoreState(saved, AuthRestoreOptions.defaults());
```

By default, restore navigates to the recorded origin, clears existing cookies and web storage, restores all three state groups, rejects expired state, and enables the historical origin check.

## Important 0.1.0 origin limitation

Only restore a state while you control navigation and know that the browser remains on the exact origin from which the state was captured. The 0.1.0 implementation validates the current origin before optional navigation but does **not** validate it again after `driver.get(origin)` completes. A redirect to an SSO or other origin can therefore make subsequent cookie or storage operations target the wrong page.

Do not use 0.1.0 auth-state restore as automatic cross-origin SSO support. Avoid restore flows in which the application origin redirects elsewhere, and verify the browser origin in application code before restoring sensitive state.

Auth-state JSON contains credentials by design and predates the central redaction policy. Store it as a secret, exclude it from source control and ordinary reports, limit filesystem access, and delete it according to your test-data retention policy.

The manager requires a `JavascriptExecutor` for web storage. Storage is scoped to the active document origin; cookies remain subject to WebDriver and browser domain rules.
