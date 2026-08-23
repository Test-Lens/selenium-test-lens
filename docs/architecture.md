# Architecture

Selenium Test Lens is split into small Maven modules so Selenium code, browser overlay resources, reporting models, and optional React support remain separate. Normal Selenium tests integrate through the main `selenium-test-lens` artifact.

## Module boundaries

| Module | Responsibility | Dependency boundary |
| --- | --- | --- |
| `selenium-test-lens-core` | Trace, logging, evidence metadata, and report exporters | Has no Selenium dependency |
| `selenium-test-lens-overlay` | Browser overlay resources, HUD support, and visual configuration | Depends on core; has no Selenium dependency |
| `selenium-test-lens` | Public `TestLens` runtime for Selenium tests | Depends on core and overlay; Selenium is optional and supplied by the consumer |
| `selenium-test-lens-react` | Optional React- and SPA-specific Selenium helpers | Depends directly on the main runtime, core, overlay, and Selenium |
| `selenium-test-lens-examples` | Compile-checked and documentation examples | Depends on the main runtime and React module; built with the reactor but excluded from Maven Central publication |

The `selenium-test-lens` artifact is built from the source directory `selenium-test-lens-selenium/`.

## Module dependency graph

```text
consumer -> selenium-test-lens
selenium-test-lens -> core
selenium-test-lens -> overlay -> core
selenium-test-lens-react -> selenium-test-lens, overlay, core, Selenium
selenium-test-lens-examples -> selenium-test-lens, selenium-test-lens-react
```

The main runtime declares Selenium as optional, so the consuming project remains responsible for its version.

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

Screenshots and other evidence are attached to the active session when produced. Finalization marks the accumulated session passed or failed and attempts to export its HTML and JSON diagnostics beneath the configured Test Lens output root.

Direct logger and sink APIs are intended for lower-level integrations.

## Overlay runtime

The overlay module contains resources for the HUD, highlighting and decorations, assertion and wait indicators, and API/debug overlays. The Selenium runtime injects and updates them through the driver. Consumers configure this layer through `OverlayConfig`.

## Reporting boundary

Report generation lives in core. Collected sessions can be exported as HTML, JSON, or portable bundles without Selenium or a live browser. See [Examples](examples.md) and [Reports](observability/reports.md) for usage.

## Evidence boundary

Core stores artifact metadata such as paths and URLs. Selenium integration creates browser-dependent evidence, including screenshots, and attaches it to the session. Exporters consume that session model; the producing feature writes the underlying file. Video support attaches existing files or URLs and does not record video.

## React boundary

React support is isolated in `selenium-test-lens-react`, which depends on the main runtime, core, overlay, and Selenium. The main runtime does not depend on React. Normal DOM interactions continue to use `selenium-test-lens`.

## Dependency rules

Keep these boundaries intact when adding features or modules:

- core must not depend on Selenium;
- overlay must not depend on Selenium;
- the main Selenium runtime must not depend on React;
- React-specific behavior must remain in the optional React module;
- examples must not become runtime dependencies or published artifacts;
- test-runner and reporter libraries must not become dependencies of the main runtime.

## Next steps

- [Browse the complete API reference](reference/index.md)
- [See usage examples](examples.md)
