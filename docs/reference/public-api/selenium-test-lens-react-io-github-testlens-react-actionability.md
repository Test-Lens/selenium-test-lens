---
search:
  exclude: true
---

# selenium-test-lens-react: `io.github.testlens.react.actionability`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.react.actionability.ReactActionabilityChecker` {#io-github-testlens-react-actionability-reactactionabilitychecker}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.react.actionability.ReactActionabilityChecker(io.github.testlens.JsOverlayDebug)
public io.github.testlens.react.actionability.ReactActionabilityReport check(org.openqa.selenium.By, io.github.testlens.react.actionability.ReactActionabilityOptions)
public io.github.testlens.react.actionability.ReactActionabilityReport check(org.openqa.selenium.WebElement, io.github.testlens.react.actionability.ReactActionabilityOptions)
```

## `io.github.testlens.react.actionability.ReactActionabilityOptions$Builder` {#io-github-testlens-react-actionability-reactactionabilityoptions-builder}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder baseOptions(io.github.testlens.selenium.actionability.ActionabilityOptions)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkAriaDisabled(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkAriaBusy(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkDataLoading(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkDataPending(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkProgressbar(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkSpinner(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkSkeleton(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkFocusLock(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder checkDialogOrModal(boolean)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder timeout(java.time.Duration)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder pollInterval(java.time.Duration)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder customBusyIndicator(org.openqa.selenium.By)
public io.github.testlens.react.actionability.ReactActionabilityOptions$Builder customBlockingOverlay(org.openqa.selenium.By)
public io.github.testlens.react.actionability.ReactActionabilityOptions build()
```

## `io.github.testlens.react.actionability.ReactActionabilityOptions` {#io-github-testlens-react-actionability-reactactionabilityoptions}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.react.actionability.ReactActionabilityOptions defaults()
public static io.github.testlens.react.actionability.ReactActionabilityOptions$Builder builder()
public io.github.testlens.selenium.actionability.ActionabilityOptions baseOptions()
public boolean checkAriaDisabled()
public boolean checkAriaBusy()
public boolean checkDataLoading()
public boolean checkDataPending()
public boolean checkProgressbar()
public boolean checkSpinner()
public boolean checkSkeleton()
public boolean checkFocusLock()
public boolean checkDialogOrModal()
public java.time.Duration timeout()
public java.time.Duration pollInterval()
public java.util.List<org.openqa.selenium.By> customBusyIndicators()
public java.util.List<org.openqa.selenium.By> customBlockingOverlays()
```

## `io.github.testlens.react.actionability.ReactActionabilityReport` {#io-github-testlens-react-actionability-reactactionabilityreport}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.react.actionability.ReactActionabilityReport(io.github.testlens.selenium.actionability.ActionabilityReport, java.util.List<io.github.testlens.react.actionability.ReactReadinessResult>)
public static io.github.testlens.react.actionability.ReactActionabilityReport of(io.github.testlens.selenium.actionability.ActionabilityReport, java.util.List<io.github.testlens.react.actionability.ReactReadinessResult>)
public io.github.testlens.selenium.actionability.ActionabilityReport baseReport()
public java.util.List<io.github.testlens.react.actionability.ReactReadinessResult> reactResults()
public boolean isReady()
public java.util.Optional<io.github.testlens.react.actionability.ReactReadinessResult> firstReactFailure()
public java.lang.String summary()
```

## `io.github.testlens.react.actionability.ReactReadinessCheckType` {#io-github-testlens-react-actionability-reactreadinesschecktype}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.react.actionability.ReactReadinessCheckType ARIA_DISABLED
public static final io.github.testlens.react.actionability.ReactReadinessCheckType ARIA_BUSY
public static final io.github.testlens.react.actionability.ReactReadinessCheckType DATA_LOADING
public static final io.github.testlens.react.actionability.ReactReadinessCheckType DATA_PENDING
public static final io.github.testlens.react.actionability.ReactReadinessCheckType PROGRESSBAR_PRESENT
public static final io.github.testlens.react.actionability.ReactReadinessCheckType SPINNER_PRESENT
public static final io.github.testlens.react.actionability.ReactReadinessCheckType SKELETON_PRESENT
public static final io.github.testlens.react.actionability.ReactReadinessCheckType FOCUS_LOCK_ACTIVE
public static final io.github.testlens.react.actionability.ReactReadinessCheckType DIALOG_OR_MODAL_ACTIVE
public static final io.github.testlens.react.actionability.ReactReadinessCheckType STALE_AFTER_RESOLVE
public static final io.github.testlens.react.actionability.ReactReadinessCheckType BASE_ACTIONABILITY
public static io.github.testlens.react.actionability.ReactReadinessCheckType[] values()
public static io.github.testlens.react.actionability.ReactReadinessCheckType valueOf(java.lang.String)
```

## `io.github.testlens.react.actionability.ReactReadinessFailureReason` {#io-github-testlens-react-actionability-reactreadinessfailurereason}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason ARIA_DISABLED_TRUE
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason ARIA_BUSY_TRUE
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason DATA_LOADING_ACTIVE
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason DATA_PENDING_ACTIVE
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason PROGRESSBAR_BLOCKING
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason SPINNER_BLOCKING
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason SKELETON_BLOCKING
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason FOCUS_LOCK_BLOCKING
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason DIALOG_BLOCKING
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason STALE_NODE
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason BASE_ACTIONABILITY_NOT_READY
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason JAVASCRIPT_ERROR
public static final io.github.testlens.react.actionability.ReactReadinessFailureReason UNKNOWN
public static io.github.testlens.react.actionability.ReactReadinessFailureReason[] values()
public static io.github.testlens.react.actionability.ReactReadinessFailureReason valueOf(java.lang.String)
```

## `io.github.testlens.react.actionability.ReactReadinessResult$Builder` {#io-github-testlens-react-actionability-reactreadinessresult-builder}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.react.actionability.ReactReadinessResult$Builder failureReason(io.github.testlens.react.actionability.ReactReadinessFailureReason)
public io.github.testlens.react.actionability.ReactReadinessResult$Builder message(java.lang.String)
public io.github.testlens.react.actionability.ReactReadinessResult$Builder elapsed(java.time.Duration)
public io.github.testlens.react.actionability.ReactReadinessResult$Builder detail(java.lang.String, java.lang.Object)
public io.github.testlens.react.actionability.ReactReadinessResult$Builder details(java.util.Map<java.lang.String, java.lang.Object>)
public io.github.testlens.react.actionability.ReactReadinessResult build()
```

## `io.github.testlens.react.actionability.ReactReadinessResult` {#io-github-testlens-react-actionability-reactreadinessresult}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react.actionability`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.react.actionability.ReactReadinessResult ready(io.github.testlens.react.actionability.ReactReadinessCheckType, java.lang.String, java.time.Duration)
public static io.github.testlens.react.actionability.ReactReadinessResult notReady(io.github.testlens.react.actionability.ReactReadinessCheckType, io.github.testlens.react.actionability.ReactReadinessFailureReason, java.lang.String, java.time.Duration)
public static io.github.testlens.react.actionability.ReactReadinessResult failed(io.github.testlens.react.actionability.ReactReadinessCheckType, io.github.testlens.react.actionability.ReactReadinessFailureReason, java.lang.String, java.time.Duration)
public static io.github.testlens.react.actionability.ReactReadinessResult skipped(io.github.testlens.react.actionability.ReactReadinessCheckType, java.lang.String, java.time.Duration)
public static io.github.testlens.react.actionability.ReactReadinessResult$Builder builder(io.github.testlens.react.actionability.ReactReadinessCheckType, io.github.testlens.selenium.actionability.ActionabilityStatus)
public io.github.testlens.react.actionability.ReactReadinessCheckType checkType()
public io.github.testlens.selenium.actionability.ActionabilityStatus status()
public io.github.testlens.react.actionability.ReactReadinessFailureReason failureReason()
public java.lang.String message()
public java.time.Duration elapsed()
public java.util.Map<java.lang.String, java.lang.Object> details()
public boolean ready()
```
