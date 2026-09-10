# Sensitive-data redaction

!!! info "Coming in 0.2.0"
    The central `RedactionPolicy` is part of the current development line and is not available in Maven Central `0.1.0`.

Selenium Test Lens applies `RedactionPolicy.defaults()` before a structured log entry is fanned out to the HUD, the session trace, or any built-in or caller-provided `UiTestLensLogSink`. The same policy is applied at direct trace, network, API-overlay, report, and failure-bundle boundaries that do not pass through the logger.

Complete JSON documents are redacted structurally by a bounded, single-pass scanner. JSON apostrophes are ordinary string content, and escaped quotes, backslashes, control escapes, Unicode escapes, nested objects, arrays, duplicate keys, numbers, booleans, and null are parsed without treating them as delimiters. A sensitive key replaces its entire value—regardless of that value's JSON type—with one correctly escaped replacement string. Strings under non-sensitive keys still receive Bearer, Basic, JWT, structured-pair, and configured-literal redaction. Malformed or excessively nested JSON is never returned merely because structural parsing failed; it goes through the fail-closed plain-text and tolerant key/value fallback instead.

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

Network matching and redirect correlation continue to use a private raw capture buffer. Public events, immutable summaries (including nested `firstFailure` data), wait results, assertion errors and their diagnostic throwable graphs, trace, network JSON, the HUD, reports, and failure evidence receive safe snapshots through the effective policy. URLs lose userinfo/fragments and mask sensitive query values; malformed URLs fail closed. `maskSensitiveHeaders(false)` disables only the network-specific mask; the central policy still protects public summaries and Test Lens artifacts. Raw header values are exposed only when both mechanisms are deliberately disabled.

Failure-bundle text components—including optional page source and browser console—receive the same best-effort redaction. The bundle configuration snapshot records only whether redaction is enabled, its replacement, and counts of added keys and literal secrets. It never records their values.

Throwable redaction keeps content and identity separate. Logger sinks receive a newly built diagnostic throwable graph whose message, causes, suppressed failures, and textual stack representation are safe; they never receive the original throwable object. Before that copy is built, the logger records the original class name as structural provenance. Consequently `exceptionType` in trace, JSON, HTML, and plain-text diagnostics remains the real application exception type even though the runtime class of the safe copy is an internal wrapper. The original throwable remains owned by the execution path and runner and is neither mutated nor replaced there.

## Protection boundary

Redaction recognizes known structured secret formats and caller-provided literals; it is not a general personal-data detector.

- Screenshot and video pixels are not modified and may show data rendered by the application.
- Page source and console redaction are best effort because arbitrary unknown secrets cannot be inferred.
- Structural parsing applies only to complete valid JSON documents; mixed HTML, console prose, and partial JSON use the tolerant text boundary. Recognized fields and configured literals remain protected, but this is not a general parser for arbitrary embedded application formats.
- Authentication/storage-state artifacts remain deliberately outside this transformation so they stay usable for session restoration, and are not automatically added to failure bundles.
- Existing limits, query stripping, and upload-path protections remain active even when central redaction is disabled.

`RedactionPolicy.disabled()` is an explicit opt-out and can expose messages, metadata, network values, page source, console output, and failure details. Use it only in a controlled environment. Redaction complements, but does not replace, avoiding secrets in test names, labels, screenshots, and application-visible content.

Auth-state restore separately validates origin by default; this is an isolation control, not text redaction. A cross-origin redirect is rejected before foreign cookies or storage are mutated, and storage writes carry an atomic in-page origin guard. Disabling `AuthRestoreOptions.validateOrigin` deliberately removes that protection and can write credentials into the currently active origin.
