# Steps and business assertions

## Named steps

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiStepResult step(String name, Runnable body)
UiStepResult step(String name, UiStepOptions options, Runnable body)
```

Steps record start/pass/failure and can display the name in the HUD, capture nested events, include a stack trace, and capture a screenshot on failure. `failFast=true` rethrows as `UiStepError`; otherwise the returned result carries failure state. `UiStepScope`/`UiStepReporter` are lower-level composition hooks.

`UiStepResult` has public `passed(...)`, `failed(...)`, and `skipped(...)` factories. Consumers read `name()`, `status()`, `startedAt()`, `endedAt()`, `elapsed()`, nullable `failure()`, immutable `children()`, `isPassed()`, and `summary()`. `UiStepFailure.from(Throwable, UiStepOptions)` captures bounded failure information and exposes `message()`, `cause()`, `causeType()`, and `stackTrace()`. `UiStepError.result()` retrieves the failed step result.

## BusinessAssertions

`BusinessAssertions` records assertions such as pass/fail checks with a business-facing description and returns/collects `BusinessAssertionResult` values. `BusinessAssertionOptions` selects collection, fail-fast behavior, stack inclusion, and message preview length. `BusinessAssertionError` carries the failed result. This supplements rather than replaces JUnit/TestNG assertions; decide which layer owns the final test failure.

`BusinessAssertionStatus` is `PASSED`, `FAILED`, or `SKIPPED`. A `BusinessAssertionFailure` carries the stored failure details used by a result/error; it is diagnostic data, not a separate assertion entry point.

Exact reporter, factory, result, and error signatures are in the [public API catalog](../reference/public-api-catalog.md).
