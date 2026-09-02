# Flakiness and retry outcomes

A **physical attempt** starts when an operation begins using the current DOM observation. A **recovery retry** is counted only when that attempt fails with a retryable exception and Lens decides to start another attempt. The first attempt is not a retry, and the final failed attempt is not counted when no next attempt follows.

Polling is different: `WebDriverWait`, resolver polls, alert/network waits, waiting for an element, and an assertion whose condition is not satisfied may evaluate repeatedly without marking the session flaky. Runner-level retries are also separate sessions. A **flaky candidate** is one session containing at least one recovery retry.

`UiTestLensSession.retrySummary()`, `TestLens.retrySummary()`, and `TestLensFinalizationResult.retrySummary()` expose immutable, key-sorted totals by action, locator, and effective exception type. `timeLost` sums only failed physical attempts that caused another attempt; it excludes successful/terminal attempts and poll intervals.

Recovery events currently come from Selenium locator actions/reads, intercepted-click recovery, and React-safe operations executed through `ReactSupport` with an attached Lens session. A React presence wait that has not yet obtained an element is still polling and is not counted.

<!-- API SIGNATURES: io.github.testlens.core.trace.RetrySummary -->
```java
public long totalRetries()
public java.time.Duration timeLost()
public boolean flakyCandidate()
public RetryOutcomePolicy policy()
public boolean policyTriggered()
public java.util.Map<String, Long> byAction()
public java.util.Map<String, Long> byLocator()
public java.util.Map<String, Long> byException()
```

<!-- API SIGNATURES: io.github.testlens.core.trace.RetryPolicyViolationException -->
```java
public RetryOutcomePolicy policy()
public RetrySummary retrySummary()
```

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
public RetrySummary retrySummary()
```

<!-- API SIGNATURES: io.github.testlens.TestLensFinalizationResult -->
```java
public RetrySummary retrySummary()
```

<!-- API SIGNATURES: io.github.testlens.TestLensOptions$Builder -->
```java
public TestLensOptions.Builder retryOutcomePolicy(RetryOutcomePolicy)
public TestLensOptions.Builder allowedRetries(int)
```

| Policy | Otherwise passed session |
| --- | --- |
| `REPORT_ONLY` | remains `PASSED`; default |
| `WARN` | remains `PASSED`, with a visible warning |
| `FAIL_ON_ANY_RETRY` | becomes `FAILED` for one or more retries |
| `FAIL_AFTER_N` | becomes `FAILED` only when `totalRetries > allowedRetries` |

Fail policies create `RetryPolicyViolationException`, finalize the session as failed, capture the configured failure screenshot, export reports, clean the HUD, and only then propagate the exception to the runner. Explicit `finishFailed(...)` and `finishSkipped(...)` keep their original status regardless of policy.

```java
TestLensOptions options = TestLensOptions.builder()
        .retryOutcomePolicy(RetryOutcomePolicy.FAIL_AFTER_N)
        .allowedRetries(1)
        .build();
```

The JSON report always includes:

```json
"flakiness": {
  "flakyCandidate": true,
  "totalRetries": 2,
  "timeLostMs": 37,
  "policy": "WARN",
  "policyTriggered": false,
  "byAction": {},
  "byLocator": {},
  "byException": {}
}
```
