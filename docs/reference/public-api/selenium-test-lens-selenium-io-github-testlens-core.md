---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.core`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.core.BlockingOverlayHelper` {#io-github-testlens-core-blockingoverlayhelper}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.core`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.core.BlockingOverlayHelper(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager, io.github.testlens.actions.HighlightActions)
public boolean handleGlobalOverlayIfPresent(java.lang.String, java.lang.String)
public boolean handleBlockingOverlayFor(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
```

## `io.github.testlens.core.Guards$GuardResult` {#io-github-testlens-core-guards-guardresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.core`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public final java.lang.String label
public final boolean isProblem
public final java.lang.String hit
public final java.lang.String url
public final java.lang.String title
public final java.lang.String bodySample
public static io.github.testlens.core.Guards$GuardResult ok(java.lang.String)
public java.lang.String formatForException()
```

## `io.github.testlens.core.Guards` {#io-github-testlens-core-guards}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.core`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.core.Guards(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayLogger)
public io.github.testlens.core.Guards(org.openqa.selenium.WebDriver)
public io.github.testlens.core.Guards setEnabled(boolean)
public io.github.testlens.core.Guards setFailFast(boolean)
public io.github.testlens.core.Guards setBodySampleLimit(int)
public io.github.testlens.core.Guards addNeedle(java.lang.String)
public io.github.testlens.core.Guards$GuardResult checkpoint(java.lang.String)
public void assertOk(java.lang.String)
```

## `io.github.testlens.core.OverlayLogger` {#io-github-testlens-core-overlaylogger}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.core`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static io.github.testlens.core.OverlayLogger noop()
public static io.github.testlens.core.OverlayLogger from(io.github.testlens.core.logging.UiTestLensLogger)
public io.github.testlens.core.redaction.RedactionPolicy redactionPolicy()
public io.github.testlens.core.OverlayLogger withSink(io.github.testlens.core.logging.UiTestLensLogSink)
public void debug(java.lang.String)
public void info(java.lang.String)
public void warn(java.lang.String)
public void error(java.lang.String)
public void error(java.lang.String, java.lang.Throwable)
public void emit(io.github.testlens.core.logging.UiTestLensLogEntry)
```

## `io.github.testlens.core.PageWaits` {#io-github-testlens-core-pagewaits}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.core`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.core.PageWaits(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig)
public io.github.testlens.core.PageWaits(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, java.time.Duration)
public void waitForDocumentReady()
public void waitForDocumentReady(java.time.Duration)
public void waitForInteractiveOrComplete()
public void waitForInteractiveOrComplete(java.time.Duration)
public void waitForNetworkIdle(java.time.Duration, java.time.Duration)
public void waitForNetworkIdle()
public org.openqa.selenium.WebElement waitForReactRootMounted(org.openqa.selenium.By)
public org.openqa.selenium.WebElement waitForReactRootMounted(org.openqa.selenium.By, java.time.Duration)
public void waitForSpaDomStableUnder(org.openqa.selenium.By, java.time.Duration, java.time.Duration)
public void waitForSpaDomStableUnder(org.openqa.selenium.By)
public org.openqa.selenium.WebElement waitForReactComponentVisible(org.openqa.selenium.By, org.openqa.selenium.By)
public org.openqa.selenium.WebElement waitForReactComponentVisible(org.openqa.selenium.By, org.openqa.selenium.By, java.time.Duration)
public void waitForReactAndNetworkIdle(org.openqa.selenium.By)
public void waitForReactAndNetworkIdle(org.openqa.selenium.By, java.time.Duration)
```

## `io.github.testlens.core.PopupDetector` {#io-github-testlens-core-popupdetector}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.core`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.core.PopupDetector(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager, io.github.testlens.actions.HighlightActions)
public java.util.Optional<org.openqa.selenium.WebElement> findTopMostPopup()
public boolean highlightPopupIfPresent(java.lang.String)
public boolean closePopupIfPresent(java.lang.String, java.lang.String)
public boolean closePopupIfPresent()
```
