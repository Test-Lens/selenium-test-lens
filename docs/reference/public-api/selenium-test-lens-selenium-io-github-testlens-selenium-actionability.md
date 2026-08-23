---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.actionability`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.actionability.ActionabilityCheck` {#io-github-testlens-selenium-actionability-actionabilitycheck}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.actionability.ActionabilityCheck enabled(io.github.testlens.selenium.actionability.ActionabilityCheckType)
public static io.github.testlens.selenium.actionability.ActionabilityCheck skipped(io.github.testlens.selenium.actionability.ActionabilityCheckType)
public io.github.testlens.selenium.actionability.ActionabilityCheckType type()
public boolean enabled()
```

## `io.github.testlens.selenium.actionability.ActionabilityCheckType` {#io-github-testlens-selenium-actionability-actionabilitychecktype}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType ATTACHED
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType VISIBLE
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType ENABLED
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType STABLE_BOUNDS
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType SCROLL_INTO_VIEW
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType RECEIVES_CLICK_POINT
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType NOT_COVERED
public static final io.github.testlens.selenium.actionability.ActionabilityCheckType OVERLAY_POLICY
public static io.github.testlens.selenium.actionability.ActionabilityCheckType[] values()
public static io.github.testlens.selenium.actionability.ActionabilityCheckType valueOf(java.lang.String)
```

## `io.github.testlens.selenium.actionability.ActionabilityChecker` {#io-github-testlens-selenium-actionability-actionabilitychecker}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.actionability.ActionabilityChecker(org.openqa.selenium.WebDriver, io.github.testlens.selenium.overlay.OverlayPolicyExecutor, io.github.testlens.core.OverlayLogger)
public io.github.testlens.selenium.actionability.ActionabilityReport check(org.openqa.selenium.By, io.github.testlens.selenium.actionability.ActionabilityOptions)
public io.github.testlens.selenium.actionability.ActionabilityReport check(org.openqa.selenium.WebElement, io.github.testlens.selenium.actionability.ActionabilityOptions)
```

## `io.github.testlens.selenium.actionability.ActionabilityFailureReason` {#io-github-testlens-selenium-actionability-actionabilityfailurereason}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason ELEMENT_NOT_ATTACHED
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason ELEMENT_NOT_VISIBLE
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason ELEMENT_NOT_ENABLED
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason ELEMENT_NOT_STABLE
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason ELEMENT_OUTSIDE_VIEWPORT
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason ELEMENT_COVERED
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason CLICK_POINT_NOT_RECEIVED
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason BLOCKING_OVERLAY_DETECTED
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason JAVASCRIPT_ERROR
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason STALE_ELEMENT
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason TIMEOUT
public static final io.github.testlens.selenium.actionability.ActionabilityFailureReason UNKNOWN
public static io.github.testlens.selenium.actionability.ActionabilityFailureReason[] values()
public static io.github.testlens.selenium.actionability.ActionabilityFailureReason valueOf(java.lang.String)
```

## `io.github.testlens.selenium.actionability.ActionabilityOptions$Builder` {#io-github-testlens-selenium-actionability-actionabilityoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder timeout(java.time.Duration)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder pollInterval(java.time.Duration)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder checkAttached(boolean)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder checkVisible(boolean)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder checkEnabled(boolean)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder checkStableBounds(boolean)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder scrollIntoView(boolean)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder checkReceivesClickPoint(boolean)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder checkOverlayPolicy(boolean)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder stableBoundsSamples(int)
public io.github.testlens.selenium.actionability.ActionabilityOptions$Builder stableBoundsSampleDelay(java.time.Duration)
public io.github.testlens.selenium.actionability.ActionabilityOptions build()
```

## `io.github.testlens.selenium.actionability.ActionabilityOptions` {#io-github-testlens-selenium-actionability-actionabilityoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.actionability.ActionabilityOptions defaults()
public static io.github.testlens.selenium.actionability.ActionabilityOptions$Builder builder()
public java.time.Duration timeout()
public java.time.Duration pollInterval()
public boolean checkAttached()
public boolean checkVisible()
public boolean checkEnabled()
public boolean checkStableBounds()
public boolean scrollIntoView()
public boolean checkReceivesClickPoint()
public boolean checkOverlayPolicy()
public int stableBoundsSamples()
public java.time.Duration stableBoundsSampleDelay()
```

## `io.github.testlens.selenium.actionability.ActionabilityReport` {#io-github-testlens-selenium-actionability-actionabilityreport}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.actionability.ActionabilityReport(java.util.List<io.github.testlens.selenium.actionability.ActionabilityResult>)
public static io.github.testlens.selenium.actionability.ActionabilityReport of(java.util.List<io.github.testlens.selenium.actionability.ActionabilityResult>)
public java.util.List<io.github.testlens.selenium.actionability.ActionabilityResult> results()
public io.github.testlens.selenium.actionability.ActionabilityStatus status()
public boolean isReady()
public java.util.Optional<io.github.testlens.selenium.actionability.ActionabilityResult> firstFailure()
public java.lang.String summary()
```

## `io.github.testlens.selenium.actionability.ActionabilityResult$Builder` {#io-github-testlens-selenium-actionability-actionabilityresult-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.actionability.ActionabilityResult$Builder failureReason(io.github.testlens.selenium.actionability.ActionabilityFailureReason)
public io.github.testlens.selenium.actionability.ActionabilityResult$Builder message(java.lang.String)
public io.github.testlens.selenium.actionability.ActionabilityResult$Builder elapsed(java.time.Duration)
public io.github.testlens.selenium.actionability.ActionabilityResult$Builder selectorDescription(java.lang.String)
public io.github.testlens.selenium.actionability.ActionabilityResult$Builder elementDescription(java.lang.String)
public io.github.testlens.selenium.actionability.ActionabilityResult$Builder detail(java.lang.String, java.lang.Object)
public io.github.testlens.selenium.actionability.ActionabilityResult$Builder details(java.util.Map<java.lang.String, java.lang.Object>)
public io.github.testlens.selenium.actionability.ActionabilityResult build()
```

## `io.github.testlens.selenium.actionability.ActionabilityResult` {#io-github-testlens-selenium-actionability-actionabilityresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.actionability.ActionabilityResult ready(io.github.testlens.selenium.actionability.ActionabilityCheckType, java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.actionability.ActionabilityResult notReady(io.github.testlens.selenium.actionability.ActionabilityCheckType, io.github.testlens.selenium.actionability.ActionabilityFailureReason, java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.actionability.ActionabilityResult failed(io.github.testlens.selenium.actionability.ActionabilityCheckType, io.github.testlens.selenium.actionability.ActionabilityFailureReason, java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.actionability.ActionabilityResult skipped(io.github.testlens.selenium.actionability.ActionabilityCheckType, java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.actionability.ActionabilityResult$Builder builder(io.github.testlens.selenium.actionability.ActionabilityCheckType, io.github.testlens.selenium.actionability.ActionabilityStatus)
public io.github.testlens.selenium.actionability.ActionabilityCheckType checkType()
public io.github.testlens.selenium.actionability.ActionabilityStatus status()
public io.github.testlens.selenium.actionability.ActionabilityFailureReason failureReason()
public java.lang.String message()
public java.time.Duration elapsed()
public java.lang.String selectorDescription()
public java.lang.String elementDescription()
public java.util.Map<java.lang.String, java.lang.Object> details()
public boolean ready()
```

## `io.github.testlens.selenium.actionability.ActionabilityStatus` {#io-github-testlens-selenium-actionability-actionabilitystatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.actionability`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.actionability.ActionabilityStatus READY
public static final io.github.testlens.selenium.actionability.ActionabilityStatus NOT_READY
public static final io.github.testlens.selenium.actionability.ActionabilityStatus FAILED
public static final io.github.testlens.selenium.actionability.ActionabilityStatus SKIPPED
public static io.github.testlens.selenium.actionability.ActionabilityStatus[] values()
public static io.github.testlens.selenium.actionability.ActionabilityStatus valueOf(java.lang.String)
```
