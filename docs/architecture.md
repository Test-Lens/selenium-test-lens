# Architecture

Selenium Test Lens is split into small Maven modules so Selenium code, browser overlay resources, reporting models, and optional React support remain separate. Normal Selenium tests integrate through the main `selenium-test-lens` artifact.

## Module boundaries

| Module | Responsibility | Dependency boundary |
| --- | --- | --- |
| `selenium-test-lens-core` | Trace, logging, central redaction, evidence metadata, and report exporters | Has no Selenium dependency |
| `selenium-test-lens-overlay` | Browser overlay resources, HUD support, and visual configuration | Depends on core; has no Selenium dependency |
| `selenium-test-lens` | Public `TestLens` runtime for Selenium tests | Depends on core and overlay; Selenium is optional and supplied by the consumer |
| `selenium-test-lens-junit5` | Published optional JUnit 5 lifecycle and parameter injection | Depends on the main runtime and JUnit Jupiter API; Selenium remains optional |
| `selenium-test-lens-testng` | Published optional TestNG invocation lifecycle | Depends on the main runtime and TestNG; Selenium remains optional |
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
selenium-test-lens-testng -> selenium-test-lens, TestNG, optional Selenium
selenium-test-lens-react -> selenium-test-lens, overlay, core, Selenium
selenium-test-lens-examples -> selenium-test-lens, selenium-test-lens-react
selenium-test-lens-browser-tests -> selenium-test-lens, junit5/testng adapters (test), Selenium, JUnit
```

The main runtime and runner integrations declare Selenium as optional, so the consuming project remains responsible for its version. JUnit and TestNG dependencies are confined to their respective optional adapter modules; core, overlay, the main runtime, React, and the other adapter do not acquire them.

## Build and verification boundaries

`mvn test` runs unit and contract tests without starting a browser. The `browser-it` profile adds the unpublished `selenium-test-lens-browser-tests` module and binds its `*IT` classes to Maven Failsafe's `integration-test` and `verify` phases. That module consumes `selenium-test-lens` through a normal Maven dependency, so it verifies the same artifact boundary used by an application rather than reaching into runtime test classes.

The real-browser fixture uses a JDK `HttpServer` on an ephemeral loopback port. It serves deterministic click, navigation, frame, popup, alert, blocking-overlay, and CSP pages without internet resources. Every test owns and closes its driver; the server is stopped after the suite.

Form actions stay inside the main Selenium locator layer. Semantic control resolution separates the state element from the native activation element, so a styled native input can be observed through the input and activated through its associated label. Checked-state confirmation re-resolves only state and never repeats the activation; file upload validates local input before its single `sendKeys` call; focus and scrolling are isolated one-script operations.

Semantic accessibility factories are represented by an internal lazy Selenium `By`: an ordinary selector narrows candidates, while typed `WebElement.getAriaRole()` and `getAccessibleName()` perform final matching in DOM order. No element is cached and no JavaScript accessibility algorithm is maintained by Test Lens.

Collection composition extends that approach with immutable internal `By` stages for scoped descendants, text and DOM-attribute filters, descendant-existence filters, and positional selection. Each observation replays the pipeline against the current frame/window, preserves DOM order, and discards the entire snapshot on stale-element failure.

Chrome and Firefox headless runs are required in CI. A headed Chrome run under Xvfb is available as a non-blocking manual smoke test. Edge and remote-grid execution are not currently in the browser matrix.

## Selenium boundary

Selenium types belong in the main integration module and extensions that explicitly build on it. Keeping core and overlay Selenium-free allows reports to be generated without a live browser, keeps trace models testable without `WebDriver`, and decouples browser resources from Selenium Java types.

Passive network capture is contained in the main Selenium module behind a package-private adapter. It creates one official Selenium 4.39 `Network` module per capture generation and subscribes to before-request, response-completed, and fetch-error events. Public Test Lens signatures do not expose beta BiDi types. The adapter uses neither CDP nor performance logs and closes only its own Network subscriptions, never the shared BiDi connection or WebDriver.

Network HUD filtering is a presentation boundary after capture: every raw entry is retained and emitted to the shared logger with a deterministic `hudVisible` decision. The trace and external sinks ignore that flag; only the HUD sink suppresses the three raw network event types, before alert deferral. Selenium 4.39.0 does not expose reliable fetch/XHR/beacon classification in its typed request model, so this layer uses explicit URL rules rather than resource heuristics.

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
      +--> per-session retry summary and policy decision
      |
      +--> HTML diagnostics (best effort)
      +--> JSON diagnostics (best effort)
```

Screenshots and other evidence are attached to the active session when produced. Finalization marks the accumulated session passed, failed, or skipped and attempts to export its HTML and JSON diagnostics beneath the configured Test Lens output root.

Recovery retry aggregation is deliberately per session and runner-neutral. Typed `RETRY` events feed an immutable summary before `SESSION_FINISHED`; ordinary waits and assertion polling remain timeline diagnostics and do not enter that summary. JUnit 5 and TestNG propagate a policy-induced failure from the shared runtime, while runner-level retries continue to create independent sessions.

`UiExpect` owns assertion polling. Through the locator's internal observation seam, each attempt executes one current query snapshot or one current element-state read, avoiding nested locator waits and duplicate operation logs. Collection/attachment assertions reevaluate the immutable composite query pipeline; checked assertions reuse the semantic control resolver without invoking its activation path. These reads do not mutate the application.

`UiPageExpect` is the runner-neutral counterpart for the active page. It shares assertion result/reporting semantics but represents no locator or element: each attempt directly performs one `getCurrentUrl()` or `getTitle()`. URL comparison uses the raw observation while only a sanitized scheme/host/port/path preview crosses the diagnostic boundary.

Failed facade finalization uses a per-session evidence pipeline in the Selenium module: snapshot trace diagnostics (including the active network summary), capture the HUD view, temporarily hide only the Test Lens shadow host for the clean view, run independent probes, stop Lens-owned network capture, record capture events, finalize, export reports, clean the HUD, then write the manifest and deterministic ZIP. Every finalizer stops active Lens-owned capture before `SESSION_FINISHED`; standalone diagnostics remain explicitly owned by their caller. Neither collector nor network adapter closes WebDriver.

Direct logger and sink APIs are intended for lower-level integrations.

Redaction is a cross-cutting diagnostic boundary owned by core. `UiTestLensLogger` creates one immutable safe entry before fan-out, so adding a sink does not duplicate secret rules. Direct session events are sanitized when stored; Selenium-layer boundaries apply the same effective policy to network/API-overlay values and failure-bundle text that bypasses logger fan-out. Matching and test control still use original runtime values, and propagated exceptions remain the originals.

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

## TestNG boundary

TestNG lifecycle integration is isolated in `selenium-test-lens-testng`. `TestLensTestNgListener` starts only around a physical test-method invocation and stores state on that invocation's `ITestResult`. `TestLensTestNgContext` reads TestNG's official current result, so parallel methods, DataProviders, retries, and reused class instances do not share mutable driver state. Registration is explicit; the artifact contains no global listener service entry.

## Dependency rules

Keep these boundaries intact when adding features or modules:

- core must not depend on Selenium;
- overlay must not depend on Selenium;
- the main Selenium runtime must not depend on React;
- JUnit APIs must remain in the optional JUnit 5 module;
- TestNG APIs must remain in the optional TestNG module;
- React-specific behavior must remain in the optional React module;
- examples must not become runtime dependencies or published artifacts;
- test-runner and reporter libraries must not become dependencies of the main runtime.

## Next steps

- [Browse the complete API reference](reference/index.md)
- [See usage examples](examples.md)
