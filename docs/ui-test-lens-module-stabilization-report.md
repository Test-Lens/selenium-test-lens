# UI Test Lens module stabilization report

Current project version: `0.1.0-SNAPSHOT`.

UI Test Lens is still pre-1.0. Public APIs may change between 0.x releases. Maven Central publication is not configured yet; local usage currently relies on `mvn install`.

## Current modules

| Module | Purpose | Main dependencies | Notes |
| ------ | ------- | ----------------- | ----- |
| `ui-test-lens-core` | Selenium-free logging/event model, exporters, neutral `BrowserScriptExecutor`, and neutral `JsResources`. | JDK for production; JUnit for tests. | Boundary scan found no Selenium imports. |
| `ui-test-lens-overlay` | Runtime JavaScript resources and browser overlay bridge/loaders, including HUD, API overlay, root manager, highlight, wait HUD, type hint, scroll arrow, and assertion badges. | `ui-test-lens-core`, JUnit for tests. | Selenium-free; primary browser integration uses `BrowserScriptExecutor`. |
| `ui-test-lens-selenium` | Selenium facade, actions, waits, popup/blocking overlay helpers, target resolver, API call helpers, and Selenium executor adapter. | `ui-test-lens-core`, `ui-test-lens-overlay`, Selenium, Lombok provided, JUnit for tests. | Selenium dependency is expected; no dependency on `ui-test-lens-react`. |
| `ui-test-lens-react` | React-safe helpers, `ReactSupport`, and `react-select` support. | `ui-test-lens-core`, `ui-test-lens-overlay`, `ui-test-lens-selenium`, Selenium, JUnit for tests. | React production classes live only in this module and layer on top of the Selenium facade. |
| `ui-test-lens` | All-in-one compatibility artifact for current local usage. | `ui-test-lens-core`, `ui-test-lens-overlay`, `ui-test-lens-selenium`, `ui-test-lens-react`, JUnit for tests. | Keeps `io.github.mmaciekk111:ui-test-lens` usable as the simple aggregate dependency. |
| `ui-test-lens-examples` | Compile-checked usage examples. | `ui-test-lens`, JUnit for tests. | Not intended as a runtime dependency; browser examples are disabled documentation-only tests. |

## Dependency graph summary

`ui-test-lens-core` has no production dependencies outside the JDK. Its only declared dependency is JUnit 5 in test scope.

`ui-test-lens-overlay` depends on `ui-test-lens-core` only for production. Selenium-compatible construction moved to `ui-test-lens-selenium`.

`ui-test-lens-selenium` depends on `ui-test-lens-core`, `ui-test-lens-overlay`, Selenium, Lombok in provided scope, and JUnit in test scope. This is currently the main module for Selenium users who do not want the all-in-one artifact.

`ui-test-lens-react` depends on `ui-test-lens-core`, `ui-test-lens-overlay`, `ui-test-lens-selenium`, Selenium, and JUnit in test scope. This direction keeps the base Selenium module independent from React helpers.

`ui-test-lens` depends on core, overlay, selenium, and react modules. It is intentionally an aggregate compatibility artifact.

`ui-test-lens-examples` depends on the all-in-one artifact and JUnit. Logging/export examples execute as normal unit tests; Selenium and React examples are disabled documentation-only tests so they compile without launching a browser.

## Transitional dependencies

| Dependency | Current reason | Removal path |
| ---------- | -------------- | ------------ |
| `ui-test-lens-react -> ui-test-lens-selenium` | `ReactSupport` layers React-safe helpers on top of `JsOverlayDebug`. | Keep until React helpers get a smaller Selenium facade interface. |
| `ui-test-lens-react -> selenium-java` | React helpers operate directly on Selenium `WebDriver`, `WebElement`, and `By`. | Keep as-is unless a future non-Selenium React adapter is introduced. |

## Artifact usage matrix

| Use case | Dependency |
| -------- | ---------- |
| Event logging/export model only | `io.github.mmaciekk111:ui-test-lens-core:0.1.0-SNAPSHOT` |
| Browser overlay runtime bridge only | `io.github.mmaciekk111:ui-test-lens-overlay:0.1.0-SNAPSHOT` |
| Selenium tests with overlay/actions/waits | `io.github.mmaciekk111:ui-test-lens-selenium:0.1.0-SNAPSHOT` |
| React-safe Selenium helpers | `io.github.mmaciekk111:ui-test-lens-react:0.1.0-SNAPSHOT` |
| Simple all-in-one local usage | `io.github.mmaciekk111:ui-test-lens:0.1.0-SNAPSHOT` |
| Compile-checked examples | `ui-test-lens-examples` module in this repository |

## Duplicate and resource scans

Production class duplicate scan: no duplicate `.java` relative paths were found under `src/main/java`.

Test class duplicate scan: no duplicate `.java` relative paths were found under `src/test/java`.

Runtime resource scan found exactly seven JavaScript resources, all under `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/`:

- `api-overlay.js`
- `assertion-badges.js`
- `highlight.js`
- `hud-panel.js`
- `scroll-arrow.js`
- `type-hint.js`
- `wait-hud.js`

## Boundary scans

Core Selenium scan: no Selenium imports or Selenium type usages were found in `ui-test-lens-core/src/main/java`.

Overlay Selenium scan: no Selenium imports or Selenium type usages were found in `ui-test-lens-overlay/src/main/java`.

Selenium module scan: Selenium usages are expected in facade, actions, waits, popup/blocking overlay helpers, target resolver, `SeleniumBrowserScriptExecutor`, `SeleniumOverlayFactory`, and the moved `OverlayBrowserScriptExecutors` compatibility helper.

React package location scan: production React classes are only in `ui-test-lens-react`.

Forbidden import scan: no matches for `LogWraper`, `TimeStamp`, `ContentIssueCollector`, `LocalDateTimeUtils`, or `io.restassured`.

`System.out/System.err` scan: the only production usage is `ConsoleLogSink` in `ui-test-lens-core`.

## Verification

Passed:

- `mvn -q test`
- `mvn -q -DskipTests compile`
- `mvn -q -pl ui-test-lens-core test`
- `mvn -q -pl ui-test-lens-overlay -am test`
- `mvn -q -pl ui-test-lens-selenium -am test`
- `mvn -q -pl ui-test-lens-react -am test`
- `mvn -q -pl ui-test-lens -am test`

Blocked by local Maven/plugin resolution:

- `mvn -q install`

`mvn install` failed while resolving `org.apache.maven.plugins:maven-install-plugin:3.1.4` from Maven Central with a PKIX certificate validation error:

```text
certificate_unknown
PKIX path building failed
```

The same failure occurred after retrying with elevated network permissions, so this is recorded as an environment/trust-store blocker rather than a project compilation failure.

Dependency tree checks:

- `mvn -q dependency:tree` and module-specific `-q dependency:tree` commands completed successfully but produced no tree output due quiet logging.
- The same dependency tree commands without `-q` completed successfully and confirmed the dependency graph summarized above.

## Next recommended steps

1. Use [`ui-test-lens-playwright-inspired-roadmap.md`](ui-test-lens-playwright-inspired-roadmap.md) to drive the next reliability and diagnostics epics.
2. Add Maven Wrapper.
3. Add publication metadata once the public API is ready.
4. Revisit API stability before the first non-SNAPSHOT release.
