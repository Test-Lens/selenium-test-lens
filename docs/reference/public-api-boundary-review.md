# Public API boundary review

This is the first controlled pre-1.0 API-boundary cleanup, planned for the 0.2.x line. The recommended `TestLens` API, the supported `JsOverlayDebug` facade, lifecycle, locators, assertions, retry, evidence, network, runner adapters, React API, and their public models remain supported. Only implementation-shaped types and construction seams were hidden.

## Result

| Classification | Before | After | Change |
| --- | ---: | ---: | ---: |
| `USER_API` | 39 | 39 | 0 |
| `ADVANCED_API` | 133 | 133 | 0 |
| `LOW_LEVEL_API` | 20 | 21 | +1 |
| `INTERNAL_STYLE_PUBLIC` | 38 | 21 | -17 |
| **All public types** | **230** | **214** | **-16** |
| **Public callables** | **1719** | **1653** | **-66** |

`BrowserScriptExecutor` is the one consciously supported low-level SPI and was reclassified rather than hidden. Sixteen implementation types were made package-private or removed. Every remaining internal-style type is deferred because hiding it requires a deliberate package or Maven-module boundary refactor.

## Audit of the original 38 internal-style types

“Signature exposure” records whether the type occurs in a supported public signature. Test/docs usage refers to repository consumers before this cleanup.

| Type | Module | Production consumers | Cross-module use | Tests / examples / docs | Signature exposure | Decision | Technical reason |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `BrowserScriptExecutor` | core | Overlay JS wrappers and Selenium overlay factory | core → overlay → selenium | Unit tests; core package docs | Constructors of low-level overlay wrappers | `SUPPORTED_SPI` | Framework-neutral script bridge is a real composition seam; classified `LOW_LEVEL_API`. |
| `TraceHtmlEscaper` | core | HTML exporter package only | None | Escaper/exporter tests; generated catalog | None | `HIDE_NOW` | Pure output-encoding helper; package-private. |
| `TraceJsonWriter` | core | `TraceJsonExporter` and bundle exporters | Cross-package inside core | Exporter tests; generated catalog | None | `DEFERRED_CROSS_MODULE` | Shared across `trace` and `trace.export`; needs exporter-boundary consolidation. |
| `TraceReportSupport` | core | JSON/HTML/bundle exporters | Cross-package inside core | Exporter tests; generated catalog | None | `DEFERRED_CROSS_MODULE` | Shared paths/constants need relocation before visibility can shrink. |
| `JsResources` | core | All overlay raw-JS wrappers | core → overlay | Resource and wrapper tests | Public wrapper implementation | `DEFERRED_CROSS_MODULE` | Resource loader is required across Maven artifacts; hide with an overlay resource-boundary refactor. |
| `ApiOverlayJs` | overlay | `ApiOverlayPanel`, `JsOverlayDebug` | overlay → selenium | Wrapper tests; generated docs | Raw constants used across artifact | `DEFERRED_CROSS_MODULE` | Selenium facade still consumes the wrapper directly. |
| `ApiOverlayPanel` | overlay | `JsOverlayDebug`, `SeleniumOverlayFactory` | overlay → selenium | Panel tests and factory tests | Low-level factory return/arguments | `DEFERRED_CROSS_MODULE` | Concrete panel spans artifact boundary; injection constructors were removed first. |
| `AssertionBadgesJs` | overlay | `AssertActions` | overlay → selenium | Wrapper/action tests | Constructor dependency of low-level action | `DEFERRED_CROSS_MODULE` | Requires moving badge orchestration behind overlay artifact boundary. |
| `HighlightJs` | overlay | `HighlightActions` | overlay → selenium | Wrapper/action tests | Constructor dependency of low-level action | `DEFERRED_CROSS_MODULE` | Raw wrapper is consumed from the Selenium artifact. |
| `HudPanelJs` | overlay | `HudPanel` | Within overlay | Wrapper/HUD tests | None directly | `DEFERRED_CROSS_MODULE` | Coupled to public low-level `HudPanel`; consolidate its implementation in a later overlay pass. |
| `OverlayRootManager` | overlay | HUD, actions, popup, policies, factory | overlay → selenium | Broad overlay/action tests and examples | Many supported low-level constructors/factory methods | `DEFERRED_CROSS_MODULE` | Central cross-artifact DOM lifecycle dependency; hiding needs a coordinated facade boundary. |
| `ScrollArrowJs` | overlay | `ScrollActions` | overlay → selenium | Wrapper/action tests | Constructor dependency of low-level action | `DEFERRED_CROSS_MODULE` | Raw wrapper is consumed from the Selenium artifact. |
| `TypeHintJs` | overlay | `TypingActions` | overlay → selenium | Wrapper/action tests | Constructor dependency of low-level action | `DEFERRED_CROSS_MODULE` | Raw wrapper is consumed from the Selenium artifact. |
| `UiTestLensRuntimeNames` | overlay | HUD and Selenium cleanup/evidence | overlay → selenium | Runtime-name/browser tests | Public constants referenced across artifact | `DEFERRED_CROSS_MODULE` | Shared DOM identifiers need an internal bridge owned by overlay. |
| `WaitHudJs` | overlay | `JsOverlayDebug` waits | overlay → selenium | Wrapper/wait tests | None directly | `DEFERRED_CROSS_MODULE` | Selenium facade currently invokes this raw wrapper across artifact boundary. |
| `ApiCallActions` | selenium | `JsOverlayDebug.apiCallWithModal` | None | Injection examples and generated docs | Removed `JsOverlayDebug` constructors | `HIDE_NOW` | Behavior moved privately into the facade; type deleted without changing facade behavior. |
| `ApiOverlayContext` | selenium | No production consumer | None | Generated docs only | None | `HIDE_NOW` | Unused ThreadLocal plumbing; package-private. |
| `ApiOverlayPlan` | selenium | No production consumer | None | Generated docs only | None | `HIDE_NOW` | Unused mutable plan helper; package-private. |
| `ApiOverlayRule` | selenium | No production consumer | None | Generated docs only | None | `HIDE_NOW` | Unused rule helper; package-private. |
| `OverlayBrowserScriptExecutors` | selenium | No production consumer | None | Generated docs only | None | `HIDE_NOW` | Redundant adapter factory; package-private. |
| `SeleniumBrowserScriptExecutor` | selenium | `JsOverlayDebug`, `SeleniumOverlayFactory` | Cross-package inside selenium | Dedicated adapter tests and generated docs | Factory returns neutral SPI, not concrete type | `DEFERRED_CROSS_MODULE` | Concrete adapter still bridges root facade and factory packages; next pass can nest/relocate it behind the factory. |
| `OverlayLogger` | selenium | Facade, actions, assertions, locators, steps | Cross-package throughout selenium | Broad unit tests and old examples | Many supported low-level constructors | `DEFERRED_CROSS_MODULE` | Removing it requires coordinated constructor cleanup across supported low-level APIs. |
| `ScriptExecutor` | selenium | No production consumer | None | Generated docs only | None | `HIDE_NOW` | Empty implementation artifact; package-private. |
| `UiAssertionReporter` | selenium | `UiExpect` package only | None | Reporter test | None | `HIDE_NOW` | Assertion event plumbing; package-private. |
| `UiExpect.ElementProbe` | selenium | `UiLocator.expect` → `UiExpect` | Cross-package inside selenium | `UiExpectTest`; assertions docs | Public injection constructor on `UiExpect` | `DEFERRED_CROSS_MODULE` | Cross-package one-shot DOM observation preserves no-nested-wait assertion semantics; needs a dedicated package-boundary refactor. |
| `UiExpect.ElementProbeResult` | selenium | Same probe path | Cross-package inside selenium | `UiExpectTest`; assertions docs | Probe return type | `DEFERRED_CROSS_MODULE` | Must move with the probe contract without changing assertion polling. |
| `UiExpect.VisibilityProbe` | selenium | `UiLocator.expect` → `UiExpect` | Cross-package inside selenium | `UiExpectTest`; assertions docs | Public injection constructor on `UiExpect` | `DEFERRED_CROSS_MODULE` | Same protected one-observation assertion seam; not safe to replace with `resolve()`. |
| `UiExpect.VisibilityProbeResult` | selenium | Same probe path | Cross-package inside selenium | `UiExpectTest`; assertions docs | Probe return type | `DEFERRED_CROSS_MODULE` | Must move with the probe contract while retaining missing/stale distinction. |
| `BusinessAssertionReporter` | selenium | `BusinessAssertions` package only | None | Reporter behavior covered through business tests | None | `HIDE_NOW` | Event plumbing; package-private. |
| `UiLocatorResolver` | selenium | `UiLocator` package only | None | Dedicated resolver tests | None in supported API | `HIDE_NOW` | Resolver implementation; package-private. |
| `UiLocatorResult` | selenium | Resolver/locator package only | None | Result/resolver tests; result docs | Not returned by recommended locator facade | `HIDE_NOW` | Internal result carrier; package-private. |
| `UiLocatorResult.Builder` | selenium | Resolver package only | None | Result tests | Only through internal result | `HIDE_NOW` | Builder hidden with its owning result. |
| `UiLocatorFailureReason` | selenium | Resolver/result package only | None | Resolver/result tests | Only through internal result | `HIDE_NOW` | Internal failure taxonomy; package-private. |
| `UiLocatorStatus` | selenium | Resolver/result package only | None | Resolver/result tests | Only through internal result | `HIDE_NOW` | Internal status taxonomy; package-private. |
| `OverlayPolicyExecutor` | selenium | `JsOverlayDebug`, click/actionability paths | Cross-package inside selenium | Policy/actionability tests | Public low-level constructors | `DEFERRED_CROSS_MODULE` | Hiding requires coordinated low-level constructor refactor, not a local visibility edit. |
| `UiStepContext` | selenium | `UiStepScope` package only | None | Scope tests | Formerly returned only by internal-style scope | `HIDE_NOW` | Mutable stack plumbing and its accessor are package-private. |
| `UiStepReporter` | selenium | `UiStepScope` package only | None | Reporter/scope tests | None | `HIDE_NOW` | Step event plumbing; package-private. |
| `UiStepScope` | selenium | Constructed and used by `JsOverlayDebug` | Cross-package inside selenium | Step tests and advanced step docs | Facade implementation field only | `DEFERRED_CROSS_MODULE` | Hiding requires moving scope construction behind a same-package internal step facade. |

## `JsOverlayDebug` construction boundary

The supported public constructors are now exactly:

```java
new JsOverlayDebug(driver);
new JsOverlayDebug(driver, overlayConfig);
```

The three constructors accepting combinations of `ApiOverlayPanel`, `ApiCallActions`, `Guards`, `UiTestLensLogger`, and `OverlayLogger` were implementation injection seams and have been removed. Consumers using those pre-1.0 constructors must migrate to one of the two supported constructors. No replacement public builder or test-support SPI was added.

## Deferred next stage

The next boundary stage should address three coherent groups rather than moving isolated classes: (1) exporter writer/support ownership inside core, (2) the core/overlay/selenium JavaScript-wrapper boundary, and (3) cross-package assertion, policy, logging, and step construction. Those refactors must preserve the current single-observation assertion contract and supported low-level APIs.
