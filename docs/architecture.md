# Architecture

UI Test Lens is a multi-module project. Each module owns a boundary so WebDriver diagnostics do not leak into neutral runtime/model modules.

## Module boundaries

| Module | Responsibility | Dependency boundary |
|---|---|---|
| `ui-test-lens-core` | Logging, log sinks, trace/evidence model, JSON and HTML exporters. | No Selenium dependency. |
| `ui-test-lens-overlay` | Runtime JS resources, overlay config, HUD model and visual debugging assets. | No Selenium dependency. |
| `ui-test-lens-selenium` | Selenium facade, locators, assertions, actionability, overlay policy, evidence capture, auth/session state and network diagnostics. | Does not depend on React. |
| `ui-test-lens-react` | React-specific support and React-aware actionability extension layer. | May depend on Selenium integration. |
| `ui-test-lens` | All-in-one POM dependency bundle. | Pulls the publishable modules together without adding runtime code. |
| `ui-test-lens-examples` | Documentation and compile-check examples. | Not a runtime dependency. |

## Selenium boundary

Selenium-specific types are isolated to `ui-test-lens-selenium` and modules that intentionally extend it. Core trace/export models and overlay runtime configuration do not import Selenium.

This keeps:

- trace/evidence export neutral
- HTML report generation browser-driver independent
- runtime overlay resources usable without Selenium types

## Overlay runtime boundary

`ui-test-lens-overlay` ships JavaScript resources such as:

- `hud-panel.js`
- `wait-hud.js`
- `highlight.js`
- `assertion-badges.js`
- `api-overlay.js`
- `type-hint.js`
- `scroll-arrow.js`

The primary browser namespace is `window.__uiTestLens`. Legacy aliases are kept only for runtime compatibility.

## React boundary

React-specific helpers are not part of `ui-test-lens-selenium`. They live in `ui-test-lens-react`, which acts as an extension layer. This prevents the Selenium module from taking a React dependency.

## Trace and logging flow

`UiTestLensLogger` emits structured events. When a `UiTestLensSession` is attached through `JsOverlayDebug.startSession(...)` or `attachSession(...)`, `TraceLogSink` maps logger events into trace timeline events.

Artifacts such as screenshots, video references and network logs are attached separately to the session.

Report export lives in `ui-test-lens-core` and remains browser-driver independent. `TraceHtmlExporter` renders self-contained static HTML documents with inline CSS for individual trace sessions and combined suite/run reports, while `TraceJsonExporter` renders the machine-readable `schemaVersion` `1.0` integration format. `TraceReportBundleExporter` packages `index.html`, `report.json`, `manifest.json`, and copied artifacts into an offline ZIP bundle. `HtmlLogExporter` converts log entries into the same HTML report pipeline for log-only runs. File helpers create parent directories, overwrite existing reports, and default suite output to `target/ui-test-lens-report/index.html`, `target/ui-test-lens-report/report.json`, and `target/ui-test-lens-report/ui-test-lens-report.zip`.

Report color is controlled through `TraceHtmlExportOptions.theme(...)`. `HtmlReportTheme.AUTO` uses CSS variables plus `prefers-color-scheme`; `LIGHT` and `DARK` force a specific static palette. HUD theme and HTML report theme are intentionally separate contracts.

## Evidence boundary

The core trace model stores artifact metadata such as path, URL, media type and labels. Selenium-side capture code creates screenshot files through `TakesScreenshot`, then attaches the resulting path as a trace artifact. Video support is reference/attachment based and does not record video.

HTML and JSON reports link file artifacts relative to the report location when possible. Missing artifact files are rendered as warnings or JSON records instead of failing export, which keeps CI report generation resilient when optional evidence was not produced.

ZIP bundles normalize all entry names, reject absolute or parent-traversal entries, never store absolute artifact paths, deduplicate duplicate artifact file names, and list missing artifacts in `manifest.json`. The bundle is intended for CI artifact publishing or API upload without requiring external assets or frontend tooling.

## Dependency rules

The project avoids:

- Selenium imports in `ui-test-lens-core`
- Selenium imports in `ui-test-lens-overlay`
- React imports in `ui-test-lens-selenium`
- legacy utility imports outside the public module APIs
- framework-specific automation dependencies outside the intended module boundary
- runtime/test dependencies for documentation-only features

These boundaries should be checked before release work.
