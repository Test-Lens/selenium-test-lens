# Architecture

Selenium Test Lens is split into small Maven modules so Selenium code, browser overlay resources, reporting models, and optional React support remain separate. Normal Selenium tests integrate through the main `selenium-test-lens` artifact.

## Module boundaries

| Module | Responsibility | Dependency boundary |
| --- | --- | --- |
| `selenium-test-lens-core` | Trace, logging, evidence metadata, and report exporters | Has no Selenium dependency |
| `selenium-test-lens-overlay` | Browser overlay resources, HUD support, and visual configuration | Depends on core; has no Selenium dependency |
| `selenium-test-lens` | Public `TestLens` runtime for Selenium tests | Depends on core and overlay; Selenium is optional and supplied by the consumer |
| `selenium-test-lens-junit5` | Published optional JUnit 5 lifecycle and parameter injection | Depends on the main runtime and JUnit Jupiter API; Selenium remains optional |
| `selenium-test-lens-react` | Optional React- and SPA-specific Selenium helpers | Depends directly on the main runtime, core, overlay, and Selenium |
| `selenium-test-lens-examples` | Compile-checked and documentation examples | Depends on the main runtime and React module; built with the reactor but excluded from Maven Central publication |
| `selenium-test-lens-browser-tests` | Consumer-level Chrome and Firefox integration tests against deterministic local pages | Added to the reactor only by `browser-it`; depends on the built main artifact and is never published |

The `selenium-test-lens` artifact is built from the source directory `selenium-test-lens-selenium/`.

## Module dependency graph

```text
consumer -> selenium-test-lens
selenium-test-lens -> core
selenium-test-lens -> overlay -> core
selenium-test-lens-junit5 -> selenium-test-lens, JUnit Jupiter API, optional Selenium
selenium-test-lens-react -> selenium-test-lens, overlay, core, Selenium
selenium-test-lens-examples -> selenium-test-lens, selenium-test-lens-react
selenium-test-lens-browser-tests -> selenium-test-lens, selenium-test-lens-junit5 (test), Selenium, JUnit
```

The main runtime and JUnit 5 integration declare Selenium as optional, so the consuming project remains responsible for its version. JUnit dependencies are confined to `selenium-test-lens-junit5`; core, overlay, the main runtime, and React do not acquire a JUnit runtime dependency.

## Build and verification boundaries

`mvn test` runs unit and contract tests without starting a browser. The `browser-it` profile adds the unpublished `selenium-test-lens-browser-tests` module and binds its `*IT` classes to Maven Failsafe's `integration-test` and `verify` phases. That module consumes `selenium-test-lens` through a normal Maven dependency, so it verifies the same artifact boundary used by an application rather than reaching into runtime test classes.

The real-browser fixture uses a JDK `HttpServer` on an ephemeral loopback port. It serves deterministic click, navigation, frame, popup, alert, blocking-overlay, and CSP pages without internet resources. Every test owns and closes its driver; the server is stopped after the suite.

Chrome and Firefox headless runs are required in CI. A headed Chrome run under Xvfb is available as a non-blocking manual smoke test. Edge and remote-grid execution are not currently in the browser matrix.

## Selenium boundary

Selenium types belong in the main integration module and extensions that explicitly build on it. Keeping core and overlay Selenium-free allows reports to be generated without a live browser, keeps trace models testable without `WebDriver`, and decouples browser resources from Selenium Java types.

## Runtime flow

`TestLens` first attaches to the existing `WebDriver`. A diagnostic session starts when `startSession(...)` is called. During that session, Lens operations can invoke browser behavior and emit diagnostic events. The active `UiTestLensSession` records them; HUD updates are best-effort.

```text
TestLens operation
      |
      +--> browser / Selenium operation (when applicable)
      |
      +--> diagnostic event
              |
              +--> session trace
              +--> HUD update (best effort)

Evidence-producing operation
      |
      +--> writes or references artifact file
      |
      +--> attaches artifact metadata
              |
              v
         active session

Finalized session
      |
      +--> HTML diagnostics (best effort)
      +--> JSON diagnostics (best effort)
```

Screenshots and other evidence are attached to the active session when produced. Finalization marks the accumulated session passed, failed, or skipped and attempts to export its HTML and JSON diagnostics beneath the configured Test Lens output root.

Direct logger and sink APIs are intended for lower-level integrations.

## Overlay runtime

The overlay module contains resources for the HUD, highlighting and decorations, assertion and wait indicators, and API/debug overlays. The Selenium runtime injects and updates them through the driver. Consumers configure this layer through `OverlayConfig`.

## Reporting boundary

Report generation lives in core. Collected sessions can be exported as HTML, JSON, or portable bundles without Selenium or a live browser. See [Examples](examples.md) and [Reports](observability/reports.md) for usage.

## Evidence boundary

Core stores artifact metadata such as paths and URLs. Selenium integration creates browser-dependent evidence, including screenshots, and attaches it to the session. Exporters consume that session model; the producing feature writes the underlying file. Video support attaches existing files or URLs and does not record video.

## React boundary

React support is isolated in `selenium-test-lens-react`, which depends on the main runtime, core, overlay, and Selenium. The main runtime does not depend on React. Normal DOM interactions continue to use `selenium-test-lens`.

## JUnit 5 boundary

JUnit lifecycle integration is isolated in the published `selenium-test-lens-junit5` artifact. `TestLensExtension` owns drivers returned by the consumer's factory and stores invocation state in `ExtensionContext.Store`, keyed by JUnit's unique invocation ID. This keeps parameterized, repeated, nested, and parallel invocations independent without a singleton or `ThreadLocal`. The main runtime remains runner-agnostic.

## Dependency rules

Keep these boundaries intact when adding features or modules:

- core must not depend on Selenium;
- overlay must not depend on Selenium;
- the main Selenium runtime must not depend on React;
- JUnit APIs must remain in the optional JUnit 5 module;
- React-specific behavior must remain in the optional React module;
- examples must not become runtime dependencies or published artifacts;
- test-runner and reporter libraries must not become dependencies of the main runtime.

## Next steps

- [Browse the complete API reference](reference/index.md)
- [See usage examples](examples.md)
