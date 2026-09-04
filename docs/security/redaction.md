# Sensitive-data redaction

Selenium Test Lens applies `RedactionPolicy.defaults()` before a structured log entry is fanned out to the HUD, the session trace, or any built-in or caller-provided `UiTestLensLogSink`. The same policy is applied at direct trace, network, API-overlay, report, and failure-bundle boundaries that do not pass through the logger.

```java
RedactionPolicy redaction = RedactionPolicy.builder()
        .sensitiveKey("tenant-session")
        .secret(System.getenv("TEST_CLIENT_SECRET"))
        .build();

TestLensOptions options = TestLensOptions.builder()
        .redactionPolicy(redaction)
        .build();

TestLens lens = TestLens.attach(driver, options);
```

The default replacement is `[REDACTED]`. Matching of known key names is case-insensitive and token-based, including structural prefixes such as `request.headers.Authorization`; it does not classify ordinary names such as `tokenizer`, `passwordPolicy`, or `sessionName`. Defaults cover common authorization, cookie, password, secret, token, session, CSRF, and XSRF keys. Text handling also recognizes HTTP header/key-value/JSON/form/query shapes, Bearer and Basic credentials, JWT-shaped values, and explicitly configured literal secrets. Custom keys extend rather than replace the safe defaults. The immutable policy is safe to share between parallel sessions, and configured literal values are never exposed by a getter or configuration export.

`redactUrl(...)` accepts absolute and relative URLs. It removes userinfo and fragments, masks values of sensitive query keys (including percent-encoded key names), and applies literal-secret masking. A malformed URL fails closed as `url[length=N]`; raw input is never returned as a parser fallback. More restrictive local rules remain in force: network HUD messages and page-assertion diagnostics still omit the entire query and fragment.

Network matching continues to use the captured value internally, while values exposed through events, waits, trace, network JSON, the HUD, reports, and failure evidence are redacted. `maskSensitiveHeaders(false)` disables only the network-specific mask; the central policy still protects Test Lens artifacts. Raw header values are exposed only when both mechanisms are deliberately disabled.

Failure-bundle text components—including optional page source and browser console—receive the same best-effort redaction. The bundle configuration snapshot records only whether redaction is enabled, its replacement, and counts of added keys and literal secrets. It never records their values.

## Protection boundary

Redaction recognizes known structured secret formats and caller-provided literals; it is not a general personal-data detector.

- Screenshot and video pixels are not modified and may show data rendered by the application.
- Page source and console redaction are best effort because arbitrary unknown secrets cannot be inferred.
- Authentication/storage-state artifacts remain deliberately outside this transformation so they stay usable for session restoration, and are not automatically added to failure bundles.
- Existing limits, query stripping, and upload-path protections remain active even when central redaction is disabled.

`RedactionPolicy.disabled()` is an explicit opt-out and can expose messages, metadata, network values, page source, console output, and failure details. Use it only in a controlled environment. Redaction complements, but does not replace, avoiding secrets in test names, labels, screenshots, and application-visible content.
