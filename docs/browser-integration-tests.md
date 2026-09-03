# Real-browser integration tests

The ordinary Maven test lifecycle is intentionally fast and browser-free. It exercises logic with unit tests, fakes, and WebDriver mocks:

```powershell
mvn test -Dheaded=false
```

The `browser-it` profile adds a separate, unpublished consumer module. Maven Failsafe runs its `*IT` tests during `verify` against a real locally installed browser:

```powershell
mvn -Pbrowser-it -Dbrowser=chrome -Dheaded=false verify
mvn -Pbrowser-it -Dbrowser=firefox -Dheaded=false verify
```

Set `-Dheaded=true` to watch the same tests locally. A missing browser or unusable driver fails the build; the suite does not silently skip. Selenium Manager may locate a compatible local driver according to Selenium's normal behavior.

## Fixture and isolation

The suite starts a JDK `HttpServer` on a random loopback port and serves all HTML, JavaScript, CSS, and API traffic locally. There are no external pages, CDNs, or fixed ports. The fixture includes click counters, navigation, an iframe, a popup, a native alert, a deterministic blocking overlay, a strict CSP page, redirects, failed responses, and a deterministic truncated response that produces a fetch error.

Each test invocation creates its own `WebDriver` and closes it in teardown. Conditions use `WebDriverWait`; the tests contain no timing sleeps. Page state and driver state are not shared, so Maven/JUnit may schedule tests in parallel safely. The server is always stopped after the suite.

## Contract coverage

The browser gate verifies:

- decorative `highlightClick()` and `highlightElement()` never dispatch an application click with the overlay enabled or disabled;
- `highlightThenClick()` and `UiLocator.click()` dispatch exactly one trusted application click in both overlay modes;
- highlight markup lives in the Test Lens shadow root and both its host and marker ignore pointer events;
- the HUD initializes, is injected again after navigation, and is retained or cleaned for `finishPassed()`, `finishFailed(...)`, and `finishSkipped(...)` according to `cleanupHudOnFinish`; skipped finalization retains `SKIPPED` metadata and never creates a failure screenshot;
- a prepared blocking overlay is closed deterministically before exactly one target click;
- frame switching, new-window waiting/switching, and `TestLensAlert` work through a real browser;
- strict page CSP does not allow a diagnostic decoration failure to change the intended Selenium click outcome.
- the JUnit 5 extension and TestNG listener each create, expose, finalize, report, and close a real browser invocation through their public adapter APIs.
- failed finalization captures diagnostic and clean screenshots, restores HUD for `cleanupHudOnFinish=false`, collects explicitly enabled page source plus context/runtime, writes manifest/reports/ZIP, remains CSP-safe, and leaves the driver alive until test or adapter cleanup.
- a real stale element causes exactly one recovery retry and one successful physical click; report-only and fail-on-any-retry paths verify Flakiness JSON/HTML.
- dedicated Chrome and Firefox drivers created with `enableBiDi()` validate real `BIDI` and `AUTO` startup, request/response/fetch-error capture, 503 accounting, redirect correlation, event-driven waits, sensitive-header masking, ignored URLs, restart/stop isolation, JSON, failure-bundle network snapshots, and HUD-only URL filtering that preserves hidden evidence. Existing browser contracts are not globally switched to BiDi. These tests do not assert fetch/XHR/beacon resource classification because Selenium Java 4.39.0 does not expose it through typed `RequestData`.

## CI

The required `browser-integration` job runs a matrix of Chrome and Firefox on `ubuntu-latest` with:

```text
mvn -Pbrowser-it -Dbrowser=<browser> -Dheaded=false verify
```

A workflow-dispatch-only Chrome job runs the suite headed under `xvfb-run` and is non-blocking. On failure, both jobs upload Surefire/Failsafe reports and any `target/ui-test-lens` diagnostics. Edge and `RemoteWebDriver` grids are not covered by the current matrix.

The required job prints the selected browser and driver paths/versions and notes that BiDi is enabled for the dedicated network contracts. A remote Grid may pass a WebSocket URL through its node, but real Grid/RemoteWebDriver execution is not claimed by this matrix.
