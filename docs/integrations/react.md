# Optional React API

Module: `selenium-test-lens-react`<br>
Package: `io.github.testlens.react.*`<br>
API level: **Advanced / optional**

Add the version matching the main artifact:

```xml
<dependency>
  <groupId>io.github.test-lens</groupId>
  <artifactId>selenium-test-lens-react</artifactId>
  <version>0.1.0</version>
</dependency>
```

The module depends on core, overlay, and Selenium Test Lens. Use it only for React/SPA re-render windows, React Select conventions, or DOM readiness conventions not covered by standard `UiLocator`.

## ReactSupport factories and helpers

<!-- API SIGNATURES: io.github.testlens.react.ReactSupport -->
```java
ReactSafeExecutor reactSafe(JsOverlayDebug overlay)
ReactOverlaySupport overlaySupport(JsOverlayDebug overlay)
ReactActionabilityChecker actionability(JsOverlayDebug overlay)
ReactActionabilityReport checkActionability(JsOverlayDebug overlay, By locator, ReactActionabilityOptions options)
void smartClick(JsOverlayDebug overlay, By locator, String label)
```

`ReactSupport.checkActionability(...)` resolves and checks the supplied `By`. For an already resolved element, obtain the checker and use its separate overload:

<!-- API SIGNATURES: io.github.testlens.react.actionability.ReactActionabilityChecker -->
```java
ReactActionabilityReport check(By locator, ReactActionabilityOptions options)
ReactActionabilityReport check(WebElement element, ReactActionabilityOptions options)
```

Both `check(...)` methods belong to `ReactActionabilityChecker`, not `ReactSupport`.

Additional `findBySelectorContainingText`, `findFirst`, `findChildren`, `findChildByText`, and `findChildByTextThenFind` overloads perform concrete DOM searches described by their selectors/text and return Selenium elements/lists. They use rendered DOM and are not React component-tree queries; see their exact signatures in the [catalog](../reference/public-api-catalog.md).

## ReactSafeExecutor

<!-- API SIGNATURES: io.github.testlens.react.ReactSafeExecutor -->
```java
ReactSafeExecutor(WebDriver, ReactOverlaySupport)
ReactSafeExecutor(WebDriver, ReactOverlaySupport, int maxRetries, Duration retryDelay, Duration waitPerAttempt)
<T> T doWithRetry(By, String, Function<WebElement,T>)
void click(By, String)
void clearAndType(By, String, String)
String getText(By, String)
String getAttribute(By, String, String)
boolean isDisplayed(By, String)
boolean isEnabled(By, String)
boolean isSelected(By, String)
ReactSelectHelper select()
```

Each attempt re-finds a present element, optionally updates/highlights via the overlay, and retries stale, missing, or intercepted failures. Non-positive `maxRetries` falls back to 3; null delay/wait use 200 ms/15 s. Other operation failures propagate. This executor has its own retry settings, separate from `UiLocatorOptions`.

## ReactSelectHelper

`resolveReactSelectBaseId` derives a `react-select-*` base id from ARIA/live-region/placeholder conventions. `jsClickReactSelectOptionContaining` clicks the first visible matching option using JavaScript. `pickByLabel` types, resolves an option id, clicks by text substring, and confirms a hidden input. It is coupled to React Select DOM/id conventions and can break when markup differs. `textContent`, `xpathLiteral`, and `cssEscape` are utility methods.

## React actionability

`ReactActionabilityChecker` combines base Selenium actionability with optional checks for `aria-disabled`, `aria-busy`, `data-loading`, `data-pending`, progressbar, spinner, skeleton, focus lock, dialog/modal, and custom busy/blocking locators. `ReactActionabilityOptions` configures every check, timeout/polling, custom locators, and base options. Reports/results identify check type, status/failure reason, message, elapsed time, and details.

Each `ReactReadinessResult` (constructed directly or through `ReactReadinessResult.Builder`) describes one readiness observation. `ReactReadinessCheckType` identifies the convention checked, including ARIA/data loading signals, progress/spinner/skeleton, focus lock, modal/dialog, staleness, and base actionability. `ReactReadinessFailureReason` records why that check was not ready, including a matching busy convention, stale node, base-actionability failure, JavaScript error, or unknown failure.

These are heuristic DOM conventions, not React internals and not guarantees for every design system. Prefer standard [Elements](../elements/index.md) unless a verified application behavior needs these helpers.
