# Manual network diagnostics

**Selenium Test Lens 0.1.0 — stable**

Version 0.1.0 provides a manual event collector. Your test or integration supplies request, response, and failure events; Test Lens can summarize, wait for, assert, export, and attach those events.

```java
NetworkDiagnostics network = new NetworkDiagnostics(driver).start(
    NetworkDiagnosticsOptions.builder()
        .captureMode(NetworkCaptureMode.MANUAL)
        .build());

network.addManualEvent(NetworkEvent.request(
    new NetworkRequest("request-1", "GET", "/api/orders", "fetch",
        Instant.now(), Map.of())));
network.addManualEvent(NetworkEvent.response(
    NetworkResponse.of("request-1", "/api/orders", 200)));

network.waitForResponse("/api/orders", 200);
network.assertNoFailedRequests();
```

`waitForResponse(...)` returns a structured `NetworkWaitResult`; `expectResponse().within(...)` converts a non-match into `NetworkAssertionError`. `attachToSession(...)` can add a summary or a JSON network artifact.

## Important limitations

- `BIDI` and `PERFORMANCE_LOGS` are reported as unsupported in 0.1.0; there is no typed WebDriver BiDi source, CDP fallback, interception, mocking, or response-body capture.
- `AUTO` selects the manual collector. It does not observe browser traffic by itself.
- Header masking is a local collector option, not the central redaction policy added later.
- The 0.1.0 `assertNoFailedRequests()` checks the current summary only; it did not yet reject every never-started or unsupported capture state. Start `MANUAL` explicitly before relying on it.
- URLs and events should be treated as potentially sensitive diagnostic data.
