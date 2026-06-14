# UI Test Lens module split plan

## Current module state

UI Test Lens has completed the first minimal Maven split. The root project is now a parent POM:

```text
io.github.mmaciekk111:ui-test-lens-parent:1.0-SNAPSHOT
```

Current child modules:

| Module | Current responsibility |
| ------ | ---------------------- |
| `ui-test-lens-core` | Selenium-free logging/event model, log exporters, and the neutral `BrowserScriptExecutor` contract. |
| `ui-test-lens-overlay` | Runtime JS resources, overlay runtime bridge/loaders, `OverlayRootManager`, `HudPanel`, `ApiOverlayPanel`, and overlay config/enums. |
| `ui-test-lens-selenium` | Selenium facade/actions module containing the current facade, Selenium actions, waits, popup helpers, API actions, and the Selenium browser executor adapter. |
| `ui-test-lens-react` | React-safe helpers and `react-select` support. |
| `ui-test-lens` | All-in-one compatibility artifact depending on core, overlay, selenium, and react modules. |

The main Selenium-facing artifact remains:

```text
io.github.mmaciekk111:ui-test-lens:1.0-SNAPSHOT
```

The Java package root is:

```text
io.github.mmaciekk111.uitestlens
```

The project already has:

- a standard Maven layout,
- runtime JavaScript resources under `src/main/resources/uitestlens/runtime/`,
- primary browser namespace `window.__uiTestLens`,
- legacy `window.__selenium...` aliases for compatibility,
- a logger/event model with text, JSON, and HTML exporters,
- unit tests for logging, exporters, resource loading, runtime markers, target resolver helpers, PageWaits state, overlay root, popup detector, and blocking overlay helper.

The `ui-test-lens-selenium` child module still mixes Selenium integration, actions, waits, and API call actions. It still depends on `ui-test-lens-react` because the current Selenium facade exposes `ReactSafeExecutor`. Later refactors should separate that facade coupling and optional API overlay concerns without changing public behavior.

`BrowserScriptExecutor` now exists as the neutral browser JavaScript execution contract, with `SeleniumBrowserScriptExecutor` as the Selenium adapter in `ui-test-lens-selenium`. Runtime bridge loaders, `HudPanel`, and `ApiOverlayPanel` can call this contract directly while existing Selenium-facing methods remain available.

The current package-level boundary matrix is tracked in:

```text
docs/ui-test-lens-module-boundaries.md
```

## Proposed first module layout

| Module | Responsibility | Dependencies | Should publish? |
| ------ | -------------- | ------------ | --------------- |
| `ui-test-lens-parent` | Parent POM, dependency management, plugin management, common version properties. | None at runtime. | No runtime artifact. |
| `ui-test-lens-core` | Logger/event model, log exporters, and neutral `BrowserScriptExecutor`. | JDK only for production. | Yes. |
| `ui-test-lens-overlay` | Runtime JS resources and overlay bridge abstractions for HUD, highlight, wait HUD, type hint, scroll arrow, assertion badges, API overlay panel, and overlay root. | `ui-test-lens-core`; currently also Selenium for WebDriver compatibility constructors. | Yes. |
| `ui-test-lens-selenium` | Current Selenium facade and integrations: `JsOverlayDebug`, waits, actions, guards, popup/blocking overlay helpers, target resolver. | `ui-test-lens-core`, `ui-test-lens-overlay`, `ui-test-lens-react`, `selenium-java`. | Yes. |
| `ui-test-lens-react` | React/SPA retry helpers and `react-select` support. | `ui-test-lens-overlay`, Selenium. | Yes. |
| `ui-test-lens-examples` | Examples, private adapter examples, sample tests, old `OverlayContentAssertions.java.example`. | Test/example scope dependencies only. | Usually no. |

Later optional modules:

| Module | Responsibility | Dependencies | Should publish? |
| ------ | -------------- | ------------ | --------------- |
| `ui-test-lens-selenide` | Selenide adapter and idiomatic Selenide actions. | `ui-test-lens-core`, `ui-test-lens-overlay`, Selenide. | Yes, optional. |
| `ui-test-lens-api-overlay` | API overlay panel/model as a standalone feature. | `ui-test-lens-core`, `ui-test-lens-overlay` or script executor abstraction. | Yes, optional. |
| `ui-test-lens-restassured` | RestAssured adapter for API overlay. | `ui-test-lens-api-overlay`, RestAssured. | Yes, optional. |
| `ui-test-lens-allure` | Allure attachments/export integration. | `ui-test-lens-core`, Allure. | Yes, optional. |
| `ui-test-lens-teamcity` | TeamCity service messages/artifacts integration. | `ui-test-lens-core`. | Yes, optional. |

## Module responsibilities

### `ui-test-lens-core`

Currently contains:

- `core.logging.*`,
- `core.logging.export.*`,
- `core.browser.BrowserScriptExecutor`.

May later contain:

- neutral runtime constants from `UiTestLensRuntimeNames`,
- neutral enums if they do not force Selenium or overlay dependencies,
- config models that do not import Selenium,
- resource loading only if kept independent of browser execution.

Should not contain:

- `WebDriver`, `WebElement`, `By`, `JavascriptExecutor`, `WebDriverWait`,
- Selenium actions,
- React helpers,
- API overlay panel execution through a driver,
- runtime bridge classes that execute scripts in the browser.

Current blockers for adding more classes:

- `OverlayConfig` imports `HudPosition`, so config placement depends on whether `HudPosition` is core or overlay.
- `JsResources` is JDK-only, but semantically belongs to runtime/overlay if it only loads browser runtime scripts.
- `UiTestLensRuntimeNames` is neutral enough for core, but many constants describe overlay/runtime behavior.
- `SeleniumBrowserScriptExecutor` imports Selenium and is now in `ui-test-lens-selenium`.

### `ui-test-lens-overlay`

Should contain:

- runtime JS resources:
  - `api-overlay.js`,
  - `wait-hud.js`,
  - `highlight.js`,
  - `type-hint.js`,
  - `scroll-arrow.js`,
  - `hud-panel.js`,
  - `assertion-badges.js`,
- loader/bridge classes:
  - `AssertionBadgesJs`,
  - `HighlightJs`,
  - `HudPanelJs`,
  - `ScrollArrowJs`,
  - `TypeHintJs`,
  - `WaitHudJs`,
  - possibly `ApiOverlayJs` if API overlay remains part of first overlay module,
- `OverlayRootManager` after it depends on an executor abstraction instead of Selenium,
- `HudPanel` after it depends on an executor abstraction instead of Selenium,
- visual bridge concepts for highlight, HUD, wait HUD, type hints, scroll arrows, and assertion badges.

Dependencies:

- `ui-test-lens-core`,
- currently Selenium because compatible `WebDriver` constructors remain present. `SeleniumBrowserScriptExecutor` has moved to `ui-test-lens-selenium`; overlay uses only a small internal compatibility helper for WebDriver constructors.

Future alternative:

- Keep `BrowserScriptExecutor` in core or overlay.
- Remove the remaining temporary Selenium dependency from overlay when WebDriver compatibility can be preserved without direct Selenium imports.
- Then overlay can avoid Selenium imports.

### `ui-test-lens-selenium`

Should contain:

- `JsOverlayDebug` as the current Selenium facade,
- `OverlayWait`,
- Selenium actions:
  - `AssertActions`,
  - `HighlightActions`,
  - `TypingActions`,
  - `SmartClickActions`,
  - `SmartInputActions`,
  - `ScrollActions`,
  - `TargetResolverActions`,
- Selenium-specific core helpers:
  - `PageWaits`,
  - `PopupDetector`,
  - `BlockingOverlayHelper`,
  - `Guards`,
- Selenium implementations of overlay/script executor abstractions,
- current popup/blocking overlay heuristics until they become an optional heuristics module.

Dependencies:

- `ui-test-lens-core`,
- `ui-test-lens-overlay`,
- `ui-test-lens-react` while `JsOverlayDebug.reactSafe()` exposes `ReactSafeExecutor`,
- `org.seleniumhq.selenium:selenium-java`.

### `ui-test-lens-react`

Should contain:

- `ReactSafeExecutor`,
- `ReactSelectHelper`,
- React/SPA wait/action helpers.

Dependencies:

- `ui-test-lens-overlay`,
- Selenium.

Blocker:

- `JsOverlayDebug` currently exposes `ReactSafeExecutor`, so `ui-test-lens-selenium` still depends on `ui-test-lens-react`. A future facade cleanup should hide that behind smaller interfaces or an adapter.

### `ui-test-lens-examples`

Should contain:

- executable examples,
- sample Selenium tests,
- sample logger/exporter usage,
- private adapter examples,
- `docs/examples/OverlayContentAssertions.java.example` if it becomes compilable only in an example/private context.

Should not be a runtime dependency of published modules.

## Class to module map

| Class/File | Current package | Proposed module | Reason | Blocking dependency |
| ---------- | --------------- | --------------- | ------ | ------------------- |
| `JsOverlayDebug` | root | `ui-test-lens-selenium` | Current Selenium facade wiring actions, waits, HUD, API overlay, React helpers. | Selenium, Lombok, broad coupling to all feature areas. |
| `OverlayConfig` | root | `ui-test-lens-core` or `ui-test-lens-overlay` | Neutral configuration except HUD enum coupling. | Imports `HudPosition`; decide whether HUD position is core or overlay. |
| `OverlayWait` | root | `ui-test-lens-selenium` | Wraps `WebDriverWait` and emits wait events. | Selenium `WebDriver`, `By`, `WebElement`, `JavascriptExecutor`, `WebDriverWait`. |
| `actions/AssertActions` | `actions` | `ui-test-lens-selenium` initially; later overlay/assertions module | Selenium assertions and visual badge bridge. | Selenium and `HudPanel`; `OverlayAssertionResult` could move to core/assertions later. |
| `actions/HighlightActions` | `actions` | `ui-test-lens-selenium` initially; bridge pieces later `ui-test-lens-overlay` | Uses Selenium actions and visual highlight bridge. | Selenium, `OverlayRootManager`, `HighlightJs`; `highlightClick` still mixes highlight and click. |
| `actions/TypingActions` | `actions` | `ui-test-lens-selenium` | Selenium typing plus type hint bridge. | Selenium and overlay bridge. |
| `actions/SmartClickActions` | `actions` | `ui-test-lens-selenium` | Selenium click/fallback/popup handling. | Selenium, ReactSafeExecutor, BlockingOverlayHelper. |
| `actions/SmartInputActions` | `actions` | `ui-test-lens-selenium` | Selenium clear/type/upload behavior with blocking overlay handling. | Selenium and BlockingOverlayHelper. |
| `actions/ScrollActions` | `actions` | `ui-test-lens-selenium` initially; visual bridge later overlay | Selenium scroll and scroll arrow bridge. | Selenium and scroll arrow runtime. |
| `actions/TargetResolverActions` | `actions` | `ui-test-lens-selenium` | Resolves Selenium `WebElement` targets with page-query scripts. | Selenium `WebElement` arguments and `JavascriptExecutor`. |
| `api/ApiOverlayJs` | `api` | `ui-test-lens-overlay` or future `ui-test-lens-api-overlay` | Loads API overlay resource. | JDK-only now, but semantically API overlay runtime. |
| `api/ApiOverlayPanel` | `api` | future `ui-test-lens-api-overlay` or `ui-test-lens-selenium` initially | Drives API overlay through browser JS execution. | Stores `BrowserScriptExecutor`; existing WebDriver constructor delegates through Selenium adapter. |
| `api/ApiCallActions` | `api` | future `ui-test-lens-api-overlay` | Neutral API overlay action facade after RestAssured removal. | Depends on `ApiOverlayPanel`; currently not HTTP-client-specific. |
| `api/ApiOverlayContext` | `api` | future `ui-test-lens-api-overlay` | ThreadLocal API overlay context. | Static lifecycle needs review before publishing as core. |
| `api/ApiOverlayPlan` | `api` | future `ui-test-lens-api-overlay` | API overlay plan model. | JDK-only but API-overlay-specific. |
| `api/ApiOverlayRule` | `api` | future `ui-test-lens-api-overlay` | API overlay rule model. | JDK-only but API-overlay-specific. |
| `core/OverlayRootManager` | `core` | `ui-test-lens-overlay` after executor abstraction; otherwise `ui-test-lens-selenium` | Creates primary overlay root runtime state. | Stores `BrowserScriptExecutor`; existing WebDriver constructor delegates through Selenium adapter for compatibility. |
| `core/PageWaits` | `core` | `ui-test-lens-selenium` | Selenium wait logic, network tracker, DOM/React waits. | Selenium waits and browser JS execution. |
| `core/PopupDetector` | `core` | `ui-test-lens-selenium` | Selenium popup detection and close heuristics. | Selenium and `HighlightActions`. |
| `core/BlockingOverlayHelper` | `core` | `ui-test-lens-selenium` | Selenium blocking overlay heuristics. | Selenium and `HighlightActions`. |
| `core/Guards` | `core` | `ui-test-lens-selenium` | Reads browser page state through Selenium. | Selenium `JavascriptExecutor`, `WebDriver`. |
| `core/*Js` loader classes | `core` | `ui-test-lens-overlay` | Runtime script resource loaders. | Expose `BrowserScriptExecutor` inject overloads; existing WebDriver overloads remain for compatibility. |
| `core/UiTestLensRuntimeNames` | `core` | `ui-test-lens-core` | Neutral runtime names/constants. | None, but constant ownership must be decided. |
| `core/OverlayLogger` | `core` | `ui-test-lens-core` or `ui-test-lens-selenium` bridge | Bridge to `UiTestLensLogger`. | No Selenium; name is overlay-specific but implementation is neutral. |
| `core/browser/BrowserScriptExecutor` | `core.browser` | `ui-test-lens-core` or `ui-test-lens-overlay` | Neutral browser script execution contract. | Placement depends on whether core should know browser concepts. |
| `core/browser/SeleniumBrowserScriptExecutor` | `core.browser` | `ui-test-lens-selenium` | Selenium adapter for `JavascriptExecutor`. | Imports Selenium and must not remain in pure core after the split. |
| `core/ScriptExecutor` | `core` | remove or replace after migration | Historical empty placeholder. | Empty/incomplete and superseded by `BrowserScriptExecutor`. |
| `core/logging/*` | `core.logging` | `ui-test-lens-core` | Event model and sinks are JDK-only. | `ConsoleLogSink` writes to `System.out/err` only when explicitly used. |
| `core/logging/export/*` | `core.logging.export` | `ui-test-lens-core` | Text/JSON/HTML exporters are JDK-only. | None. |
| `hud/HudPanel` | `hud` | `ui-test-lens-overlay` after executor abstraction; otherwise `ui-test-lens-selenium` | HUD bridge into browser runtime. | Stores `BrowserScriptExecutor`; existing WebDriver constructor delegates through Selenium adapter. |
| `hud/HudPosition` | `hud` | `ui-test-lens-core` or `ui-test-lens-overlay` | Simple enum used by config. | Placement affects `OverlayConfig` module. |
| `react/ReactSafeExecutor` | `react` | `ui-test-lens-react` | React/SPA retry facade. | Selenium and `ReactOverlaySupport`. |
| `react/ReactSelectHelper` | `react` | `ui-test-lens-react` | React-select helper. | Selenium and `ReactOverlaySupport`. |
| `scroll/ScrollElementEdge` | `scroll` | `ui-test-lens-core` or `ui-test-lens-overlay` | Neutral enum for scroll visual behavior. | None. |
| `scroll/ScrollViewportEdge` | `scroll` | `ui-test-lens-core` or `ui-test-lens-overlay` | Neutral enum for scroll visual behavior. | None. |
| `utils/JsResources` | `utils` | `ui-test-lens-core` or `ui-test-lens-overlay` | JDK-only classpath resource loader. | Semantic ownership depends on whether only runtime scripts use it. |
| runtime JS files | resources | `ui-test-lens-overlay` | Browser runtime resources. | Selenium fallback paths remain compatibility policy. |
| tests for logging/exporters | `src/test/java/.../core/logging` | `ui-test-lens-core` tests | JDK-only unit tests. | None. |
| tests for runtime resource markers | `src/test/java/.../core`, `utils`, `api` | `ui-test-lens-overlay` tests | Resource marker tests without browser. | Package names should follow moved classes. |
| tests for target/page/popup helper scripts | `src/test/java/.../actions`, `core` | `ui-test-lens-selenium` tests | Test script markers without browser. | Class locations depend on final module ownership. |
| `docs/examples/OverlayContentAssertions.java.example` | docs example | `ui-test-lens-examples` | Private adapter example. | Private `ContentIssueCollector` / `LocalDateTimeUtils` if made compilable. |

## Split blockers

- Several classes currently under `core` import Selenium: `PageWaits`, `PopupDetector`, `BlockingOverlayHelper`, `Guards`, and `SeleniumBrowserScriptExecutor`.
- `OverlayRootManager`, `HudPanel`, and `ApiOverlayPanel` now store `BrowserScriptExecutor`, but existing WebDriver constructors remain Selenium-bound for compatibility.
- Runtime bridge loader classes expose `BrowserScriptExecutor` overloads. `HudPanel` uses the neutral executor internally, but callers such as `HighlightActions`, `AssertActions`, `TypingActions`, and `ScrollActions` still use Selenium execution directly.
- `JsResources` is technically JDK-only but semantically tied to runtime resources; decide whether it is core utility or overlay/runtime utility.
- `OverlayConfig` currently imports `HudPosition`; decide if `HudPosition` is core, overlay, or if HUD config becomes a separate overlay config object.
- `HudPosition` and scroll enums are neutral, but they describe overlay behavior. Their module affects dependency direction.
- `ApiOverlayPanel` still requires Selenium execution and should not move to pure core.
- `ApiOverlayJs` is JDK-only but API-overlay-specific; keeping it in first overlay module is acceptable until `ui-test-lens-api-overlay` exists.
- `JsOverlayDebug` should remain in `ui-test-lens-selenium` as the compatibility facade.
- `ReactSafeExecutor` and `ReactSelectHelper` now live in `ui-test-lens-react` and use `ReactOverlaySupport` instead of directly importing `JsOverlayDebug`.
- `BlockingOverlayHelper` and `PopupDetector` depend on `HighlightActions`; this keeps heuristics tied to Selenium action/overlay behavior.
- `BrowserScriptExecutor` exists, but adoption is partial.
- Current `ScriptExecutor` is empty and should be removed or replaced in a separate cleanup.
- ThreadLocal API overlay context needs lifecycle review before a standalone API overlay module.
- Optional adapters must not leak dependencies like RestAssured, Allure, TeamCity, or private project utilities into core or selenium modules.

## Proposed split commit sequence

### 1. Continue `BrowserScriptExecutor` adoption

Scope:

- Keep the new neutral `BrowserScriptExecutor` contract.
- Move more runtime bridge call sites to the contract where it does not change public behavior.
- Keep existing public APIs intact.
- Decide whether the old empty `ScriptExecutor` should be removed or deprecated in a separate cleanup.

Risk:

- Medium. It touches constructors and bridge classes.

Verification:

- `mvn -q test`
- `mvn -q -DskipTests compile`
- resource marker tests still pass.

Expected commit title:

```text
Adopt browser script executor in runtime bridges
```

### 2. Separate resource loading and runtime bridge ownership

Scope:

- Move runtime loader classes conceptually behind overlay package boundaries.
- Keep resource paths unchanged.
- Decide whether `JsResources` stays in core or overlay.

Risk:

- Low to medium. Mostly package ownership and imports.

Verification:

- `mvn -q test`
- runtime resource tests for all JS files.

Expected commit title:

```text
Separate runtime resource bridge boundaries
```

### 3. Extract `ui-test-lens-core`

Status: done for logging/export and `BrowserScriptExecutor`.

Scope:

- Parent POM and `ui-test-lens-core` were created.
- Logger/event model, exporters, and the neutral browser script executor contract were moved.
- Neutral constants and enums/config remain pending until ownership decisions are made.
- Keep no Selenium imports in core.

Risk:

- Medium. Maven structure changes and imports move.

Verification:

- `mvn -q test` at parent.
- `rg "org\\.openqa\\.selenium" ui-test-lens-core/src/main/java` must have no matches.

Expected commit title:

```text
Extract UI Test Lens core module
```

### 4. Extract `ui-test-lens-overlay`

Status: done as an intermediate split. Runtime resources, bridge loaders, `OverlayRootManager`, `HudPanel`, `ApiOverlayPanel`, `OverlayConfig`, and `HudPosition` moved to `ui-test-lens-overlay`.

Scope:

- Move runtime resources and overlay bridge classes that can depend on `BrowserScriptExecutor`.
- Include resource loader tests.
- Keep legacy fallback paths.
- Keep temporary Selenium compatibility constructors until `ui-test-lens-selenium` exists.

Risk:

- Medium. The executor abstraction is stable enough, but overlay still has temporary Selenium imports.

Verification:

- `mvn -q test` at parent.
- All resource marker tests pass.
- Selenium module tests still compile through overlay dependency.

Expected commit title:

```text
Extract UI Test Lens overlay module
```

### 5. Extract `ui-test-lens-selenium`

Status: done as the third Maven module. `JsOverlayDebug`, `OverlayWait`, Selenium actions, waits, popup/blocking overlay helpers, API call actions/models, React helpers, scroll enums, and `SeleniumBrowserScriptExecutor` moved to `ui-test-lens-selenium`. The `ui-test-lens` artifact now remains as an all-in-one compatibility module.

Scope:

- Move `JsOverlayDebug`, `OverlayWait`, Selenium actions, `PageWaits`, `Guards`, target resolver, popup/blocking overlay helpers.
- Keep the current facade and public method names.

Risk:

- High. This is the primary runtime integration module.

Verification:

- `mvn -q test` at parent.
- Compile examples against `ui-test-lens-selenium`.
- Selenium imports only in selenium/react/optional adapter modules.

Expected commit title:

```text
Extract UI Test Lens Selenium module
```

### 6. Extract `ui-test-lens-react`

Status: done as a separate Maven module. `ReactSafeExecutor`, `ReactSelectHelper`, `ReactOverlaySupport`, and the React package metadata moved to `ui-test-lens-react`. The Selenium facade still depends on this module because `JsOverlayDebug.reactSafe()` returns `ReactSafeExecutor`.

Scope:

- Move `ReactSafeExecutor` and `ReactSelectHelper`.
- Replace direct `JsOverlayDebug` coupling with small interfaces if needed.

Risk:

- Medium. Behavior must stay identical for React-safe helpers.

Verification:

- `mvn -q test`
- compile sample usage.

Expected commit title:

```text
Extract UI Test Lens React module
```

### 7. Add `ui-test-lens-examples`

Scope:

- Move examples and private adapter samples.
- Include `OverlayContentAssertions.java.example` as a documented non-runtime example.
- Add sample usage for logger/exporters and Selenium facade.

Risk:

- Low. Should not affect runtime modules.

Verification:

- `mvn -q test`
- examples compile only if dependencies are public or explicitly test/example scoped.

Expected commit title:

```text
Add UI Test Lens examples module
```

### 8. Add optional adapters

Scope:

- Add `ui-test-lens-selenide`, `ui-test-lens-api-overlay`, `ui-test-lens-restassured`, `ui-test-lens-allure`, and `ui-test-lens-teamcity` only after the first split is stable.

Risk:

- Varies by adapter.

Verification:

- Each optional dependency stays out of core and selenium main artifacts unless intentionally declared.

Expected commit titles:

```text
Add Selenide adapter module
Add API overlay module
Add RestAssured API overlay adapter
Add Allure logging adapter
Add TeamCity logging adapter
```

## Test strategy after the split

`ui-test-lens-core`:

- logging/event model tests,
- sink tests,
- exporter tests,
- neutral config/enum tests,
- no Selenium dependency.

`ui-test-lens-overlay`:

- runtime JS resource loading tests,
- marker tests for `__uiTestLens.modules.*`,
- fallback path tests,
- `BrowserScriptExecutor` bridge tests with fakes, not browsers.

`ui-test-lens-selenium`:

- compile and unit tests for Selenium-bound helper script builders,
- target resolver marker/helper tests,
- PageWaits script marker tests,
- popup/blocking overlay helper marker tests,
- later browser integration tests on a simple HTML page.

`ui-test-lens-react`:

- helper unit tests where possible,
- compile tests against Selenium abstractions,
- later browser integration tests for React-like stale element behavior.

`ui-test-lens-examples`:

- examples compile,
- no runtime dependency from published artifacts to examples.

Parent POM:

- `mvn test` should run all module unit tests.
- Integration/browser tests should be separated by Maven profile, for example `-Pbrowser-it`, so default CI remains stable.

## Dependency classification summary

Classes depending on Selenium:

- root facade/waits: `JsOverlayDebug`, `OverlayWait`,
- actions: all current action classes,
- API overlay panel: `ApiOverlayPanel`,
- core helpers: `OverlayRootManager`, `PageWaits`, `PopupDetector`, `BlockingOverlayHelper`, `Guards`,
- HUD bridge: `HudPanel`,
- React helpers: `ReactSafeExecutor`, `ReactSelectHelper`.

JDK-only or effectively neutral:

- `core.logging.*`,
- `core.logging.export.*`,
- `TargetDescriptor`,
- `UiTestLensRuntimeNames`,
- `JsResources`,
- `ApiOverlayPlan`,
- `ApiOverlayRule`,
- `ApiOverlayContext`,
- scroll/HUD enums.

Runtime-resource-specific:

- `ApiOverlayJs`,
- `AssertionBadgesJs`,
- `HighlightJs`,
- `HudPanelJs`,
- `ScrollArrowJs`,
- `TypeHintJs`,
- `WaitHudJs`,
- all files in `src/main/resources/uitestlens/runtime/`.

React-specific:

- `ReactSafeExecutor`,
- `ReactSelectHelper`,
- React wait methods currently in `PageWaits` and facade methods in `JsOverlayDebug`.

API-overlay-specific:

- `ApiOverlayJs`,
- `ApiOverlayPanel`,
- `ApiCallActions`,
- `ApiOverlayContext`,
- `ApiOverlayPlan`,
- `ApiOverlayRule`,
- `api-overlay.js`.

Examples/private adapters:

- `docs/examples/OverlayContentAssertions.java.example`,
- future downstream adapters for private loggers/content collectors.

## Recommended first commit after this audit

The executor abstraction has been introduced. The next implementation commit should adopt it in remaining runtime bridge call sites before moving files. That reduces risk because the overlay module boundary still has Selenium-bound callers.

Recommended commit title:

```text
Adopt browser script executor in runtime bridges
```
