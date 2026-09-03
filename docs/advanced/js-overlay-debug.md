# `JsOverlayDebug` advanced facade

Package: `io.github.testlens`<br>
Module: `selenium-test-lens-selenium`<br>
API level: **Advanced**

`JsOverlayDebug` is the older, broad Selenium/overlay facade underneath parts of the recommended `TestLens` API. Use `TestLens`, `UiLocator`, and `UiExpect` for ordinary tests. Use this facade only when a documented advanced family below is required. It wraps an existing driver and never owns or closes it.

The one- and two-argument constructors are the consumer constructors:

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
JsOverlayDebug(WebDriver driver)
JsOverlayDebug(WebDriver driver, OverlayConfig config)
```

These are the only public constructors. The pre-1.0 component-injection constructors accepting `ApiOverlayPanel`, `ApiCallActions`, `Guards`, or logger bridges were removed for the planned 0.2.x line. Migrate custom construction to one of the two forms above; no supported advanced operation was removed.

## Locators, assertions, and actionability

The following methods create the same lazy locator/assertion abstractions documented under [Elements](../elements/index.md):

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
ActionabilityReport checkActionability(By locator, ActionabilityOptions options)
ActionabilityReport checkActionability(WebElement element, ActionabilityOptions options)
UiLocator locator(By by)
UiLocator locator(By by, String label)
UiLocator locator(By by, UiLocatorOptions options)
UiLocator locator(By by, String label, UiLocatorOptions options)
UiLocator getByTestId(String testId)
UiLocator getByTestId(String testId, String label)
UiLocator getByPlaceholder(String placeholder)
UiLocator getByPlaceholder(String placeholder, String label)
UiLocator getByText(String text)
UiLocator getByText(String text, String label)
UiLocator getByTextContaining(String text)
UiLocator getByTextContaining(String text, String label)
UiLocator getByLabel(String labelText)
UiLocator getByLabel(String labelText, String label)
UiLocator getByRole(String role)
UiLocator getByRole(String role, String accessibleName)
UiExpect expect(By by)
UiExpect expect(By by, String label)
UiExpect expect(UiLocator locator)
UiExpect expect(UiLocator locator, UiAssertionOptions options)
```

All locator factories remain lazy. Placeholder, label, text, and role factories generate DOM selectors; role/name matching has the same limited accessible-name semantics described under [Locators](../elements/locators.md). Actionability overloads inspect either a locator or an already resolved element and return a report rather than performing an action.

The returned actionability model records individual `ActionabilityCheck`/`ActionabilityResult` values. `ActionabilityCheckType` identifies attachment, visibility, enabled state, stable bounds, scrolling, click-point receipt, coverage, and overlay-policy checks. `ActionabilityStatus` distinguishes ready, not-ready, failed, and skipped reports; `ActionabilityFailureReason` supplies the concrete diagnostic reason such as detached, covered, stale, timed out, or JavaScript failure. These values diagnose readiness and do not themselves click the element.

## Sessions, steps, trace, and evidence

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
void attachSession(UiTestLensSession session)
Optional<UiTestLensSession> session()
UiTestLensSession startSession(String name)
UiStepResult step(String name, Runnable body)
UiStepResult step(String name, UiStepOptions options, Runnable body)
TraceArtifact attachScreenshot(String name, Path path)
TraceArtifact attachVideo(String name, Path path)
TraceArtifact attachArtifact(TraceArtifact artifact)
String exportTraceHtml()
String exportTraceHtml(TraceHtmlExportOptions options)
Path exportTraceHtml(Path outputPath)
Path exportTraceHtml(Path outputPath, TraceHtmlExportOptions options)
ScreenshotCaptureResult captureScreenshot(String name)
ScreenshotCaptureResult captureScreenshot(String name, ScreenshotCaptureOptions options)
VideoEvidenceResult attachVideoFile(String name, Path path)
VideoEvidenceResult attachVideoFile(String name, Path path, VideoEvidenceOptions options)
VideoEvidenceResult attachVideoUrl(String name, String url)
VideoEvidenceResult attachVideoUrl(String name, String url, VideoEvidenceOptions options)
```

Methods that attach an existing artifact or export trace HTML require an attached session and throw `IllegalStateException` when none exists. `captureScreenshot(...)` can still write a PNG without a session; its result explains whether a trace artifact was attached. Video methods attach references only and do not record video. Step behavior and failure screenshot options are described under [Steps](steps-business-assertions.md); result and nullable evidence semantics are under [Screenshots and evidence](../observability/screenshots-evidence.md).

## Business, authentication, and network services

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
BusinessAssertions business(String subject)
BusinessAssertions business(String subject, BusinessAssertionOptions options)
AuthStateManager auth()
AuthState captureAuthState(AuthStateOptions options)
AuthRestoreResult restoreAuthState(AuthState state, AuthRestoreOptions options)
AuthRestoreResult restoreAuthState(Path path, AuthRestoreOptions options)
NetworkDiagnostics network()
NetworkDiagnosticsResult attachNetworkLog(Path outputPath)
```

These are convenience delegates to the advanced services documented under [Authentication state](auth-state.md), [Network diagnostics](network.md), and [Business assertions](steps-business-assertions.md). `network()` lazily creates and reuses one diagnostics service. `attachNetworkLog(...)` requires an attached session. Auth state, network data, and output paths can contain secrets.

## HUD and explicit visual helpers

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
void initHud(String testName, String pipelineId)
void setStep(String stepDescription)
void hudLog(String level, String message, String timestamp)
void highlightClick(WebElement element, String label)
WebElement highlightElement(WebElement element, String label)
void highlightParent(WebElement element, String label)
void highlightAncestor(WebElement element, int levelsUp, String label)
void highlightClosest(WebElement element, String cssSelector, String label)
void highlightThenClick(WebElement element, String label)
void clearDebugArtifacts()
```

The `highlightClick(...)`, `highlightElement(...)`, `highlightParent(...)`, `highlightAncestor(...)`, and `highlightClosest(...)` methods only inject temporary DOM decoration; they never click or otherwise act on the application. `highlightElement(...)` returns the supplied element for chaining, while the parent/ancestor/closest variants change which DOM node is decorated. Use `highlightThenClick(...)` for decoration followed by exactly one Selenium `click()`, or `smartClickWithOverlayHandler(...)` when overlay handling and click retries are required. With the visual overlay disabled, decoration is skipped but `highlightThenClick(...)` still clicks once. HUD and cleanup operations are best-effort browser decoration and do not change test conditions.

## Typing and click helpers

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
void typeWithHint(WebElement element, String value)
void clearAndType(WebElement element, String value)
void smartTypeWithHint(WebElement element, String value)
void smartTypeWithHintHighlighted(WebElement element, String value)
void smartTypeWithHintHighlighted(WebElement element, String value, String label)
void smartClickWithOverlayHandler(WebElement element, String label)
WebElement resolveClickTarget(WebElement element)
WebElement resolveFileInputTarget(WebElement element)
String resolveClickTargetCssSelector(WebElement element)
String resolveFileInputCssSelector(WebElement element)
void smartClickResolved(WebElement containerOrLabel, String label)
void smartUploadFile(WebElement containerOrLabel, String absoluteFilePath)
```

Typing helpers operate on an already resolved element; their names distinguish plain clear/type, hint feedback, and an explicit pre-action highlight. The smart-click helper executes configured overlay handling before clicking. Target resolvers heuristically find a clickable element or associated file input; file-input resolution can return null. `smartClickResolved(...)` and `smartUploadFile(...)` return without throwing when no target is found, after HUD/log feedback where enabled. Selenium failures from the eventual click or `sendKeys` still propagate.

## Page readiness and wait feedback

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
void waitForPageReady()
void waitForPageReady(Duration timeout)
void waitForNetworkIdle()
void waitForNetworkIdle(Duration idleDuration, Duration timeout)
void waitForInteractiveOrComplete()
void waitForInteractiveOrComplete(Duration timeout)
void waitForReactRootMounted(By rootLocator)
void waitForReactRootMounted(By rootLocator, Duration timeout)
void waitForSpaDomStableUnder(By rootLocator)
void waitForSpaDomStableUnder(By rootLocator, Duration timeout, Duration stableFor)
WebElement waitForReactComponentVisible(By rootLocator, By componentLocator)
WebElement waitForReactComponentVisible(By rootLocator, By componentLocator, Duration timeout)
void waitForReactAndNetworkIdle(By rootLocator)
void waitForReactAndNetworkIdle(By rootLocator, Duration timeout)
void ensureWaitHudInjected()
void waitHudStart(String label)
void waitHudStop(String prefix, long elapsedMs)
void forceHideWaitHud()
void showWaitIndicator(String label)
void hideWaitIndicator()
void showLastWaitInHud()
```

Page-ready methods wait for browser document state; network-idle and SPA-stability methods use the facade's injected JavaScript observations and configured polling. They are heuristics, not CDP/BiDi network guarantees. React-named methods inspect rendered DOM and can be used without the optional React artifact, but the optional module provides the more focused supported React abstractions. Wait-HUD methods only control visual feedback; most catch injection errors so an overlay failure does not fail the test.

## Popup helpers and overlay policy

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
void setOverlayPolicy(OverlayPolicy overlayPolicy)
Optional<WebElement> detectPopup()
boolean highlightPopupIfPresent()
boolean highlightPopupIfPresent(String label)
boolean closePopupIfPresent()
boolean closePopupIfPresent(String overlayLabel, String closeButtonLabel)
```

A null policy becomes `OverlayPolicy.none()`. Popup detection uses the facade's predefined heuristics; highlight/close methods return false when no popup is detected or handled. Application-specific blocker workflows should use the explicit [overlay policy](overlay-policies.md), whose actions are limited to click, Escape, wait-until-gone, and fail.

## Direct overlay assertions

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
boolean assertTextEquals(WebElement element, String expected, String contextLabel)
boolean assertTextContains(WebElement element, String substring, String contextLabel)
boolean assertAttributeEquals(WebElement element, String attribute, String expected, String contextLabel)
boolean assertCssEquals(WebElement element, String property, String expected, String contextLabel)
boolean assertColorEquals(WebElement element, String property, String expectedColor, String contextLabel)
boolean assertHasClass(WebElement element, String className, boolean expectedPresent, String contextLabel)
boolean assertVisible(WebElement element, boolean expectedVisible, String contextLabel)
boolean assertEnabled(WebElement element, boolean expectedEnabled, String contextLabel)
boolean assertSelected(WebElement element, boolean expectedSelected, String contextLabel)
JsOverlayDebug.AssertionSummary assertGroup(String groupName, Consumer<JsOverlayDebug.SoftAssertions> consumer, boolean failTestOnErrors)
```

These legacy assertions act on an already resolved `WebElement`, render assertion feedback, and return success as a boolean. `assertGroup(...)` collects element and value assertions through `SoftAssertions`; when `failTestOnErrors` is true, it throws one `AssertionError` after the consumer completes. `AssertionSummary` exposes the collected result objects, failures, group name, message/JSON formatting, and `formatForException()`. Prefer retryable [`UiExpect`](../elements/assertions.md) for normal element assertions.

## Scroll and API overlay helpers

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
void scrollToElementWithArrow(WebElement element)
void scrollToElementWithArrow(WebElement element, long durationMs)
void scrollToElementWithArrow(WebElement element, ScrollElementEdge elementEdge, ScrollViewportEdge viewportEdge)
void scrollToElementWithArrow(WebElement element, long durationMs, ScrollElementEdge elementEdge, ScrollViewportEdge viewportEdge)
void showApiCall(String title, String method, String url, String payloadPreview, long timeoutMs)
void showApiResponse(String requestId, int status, long durationMs, String headersPreview, String bodyPreview)
void hideApiModal()
<T> T apiCallWithModal(String title, String method, String url, String payloadPreview, long timeoutMs, Callable<T> call, Function<T, String> responsePreview)
String apiShowRequest(String title, String method, String url, String payloadPreview)
void apiSetPending(String requestId, long timeoutMs)
void apiSetResponse(String requestId, int status, long durationMs, String headersPreview, String bodyPreview)
boolean apiHighlightJsonPath(String path)
int apiHighlightKeyAnimated(String key, long delayMs, int maxHits)
void highlightPathAnimated(String path, int stepDelayMs)
void apiHighlightJsonPathsAnimated(List<String> paths, long delayMs)
```

Scroll helpers use injected JavaScript and an arrow decoration; the edge overloads control alignment. API overlay methods visualize a caller-supplied request/response preview. They do not perform interception or HTTP traffic capture. `apiCallWithModal(...)` invokes the supplied synchronous callable and maps its result for display. The lower-level `api*` methods expose modal state directly and generally treat JavaScript failures as no result/false/zero; `highlightPathAnimated(...)` is an exception and can propagate script execution failure. Never pass credentials, tokens, unredacted headers, or sensitive payloads into overlay previews.

## Accessors

<!-- API SIGNATURES: io.github.testlens.JsOverlayDebug -->
```java
WebDriver getDriver()
OverlayConfig getConfig()
```

These return the existing driver and effective overlay configuration. They do not transfer lifecycle ownership.

For every constructor, nested soft-assertion member, and exact generic signature, use the optional [binary API catalog](../reference/public-api-catalog.md). The long component-injection constructors and direct HUD/API-modal primitives are retained legacy surface even though the class as a whole remains supported advanced API.
