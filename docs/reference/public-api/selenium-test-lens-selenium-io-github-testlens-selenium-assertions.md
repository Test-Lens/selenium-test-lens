---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.assertions`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.assertions.UiAssertionError` {#io-github-testlens-selenium-assertions-uiassertionerror}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/result-types.md](../../reference/result-types.md)

```java
public io.github.testlens.selenium.assertions.UiAssertionError(io.github.testlens.selenium.assertions.UiAssertionResult)
public io.github.testlens.selenium.assertions.UiAssertionResult result()
```

## `io.github.testlens.selenium.assertions.UiAssertionFailureReason` {#io-github-testlens-selenium-assertions-uiassertionfailurereason}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/reference/result-types.md](../../reference/result-types.md)

```java
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason ELEMENT_NOT_FOUND
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason ELEMENT_NOT_VISIBLE
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason ELEMENT_STILL_VISIBLE
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason ELEMENT_NOT_ENABLED
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason ELEMENT_NOT_DISABLED
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason TEXT_MISMATCH
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason VALUE_MISMATCH
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason STALE_ELEMENT
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason TIMEOUT
public static final io.github.testlens.selenium.assertions.UiAssertionFailureReason UNKNOWN
public static io.github.testlens.selenium.assertions.UiAssertionFailureReason[] values()
public static io.github.testlens.selenium.assertions.UiAssertionFailureReason valueOf(java.lang.String)
```

## `io.github.testlens.selenium.assertions.UiAssertionOptions$Builder` {#io-github-testlens-selenium-assertions-uiassertionoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/configuration.md](../../reference/configuration.md)

```java
public io.github.testlens.selenium.assertions.UiAssertionOptions$Builder timeout(java.time.Duration)
public io.github.testlens.selenium.assertions.UiAssertionOptions$Builder pollInterval(java.time.Duration)
public io.github.testlens.selenium.assertions.UiAssertionOptions$Builder normalizeWhitespace(boolean)
public io.github.testlens.selenium.assertions.UiAssertionOptions$Builder caseSensitive(boolean)
public io.github.testlens.selenium.assertions.UiAssertionOptions$Builder actualTextPreviewLimit(int)
public io.github.testlens.selenium.assertions.UiAssertionOptions$Builder trimText(boolean)
public io.github.testlens.selenium.assertions.UiAssertionOptions$Builder failFastOnMissingElement(boolean)
public io.github.testlens.selenium.assertions.UiAssertionOptions build()
```

## `io.github.testlens.selenium.assertions.UiAssertionOptions` {#io-github-testlens-selenium-assertions-uiassertionoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/configuration.md](../../reference/configuration.md)

```java
public static io.github.testlens.selenium.assertions.UiAssertionOptions defaults()
public static io.github.testlens.selenium.assertions.UiAssertionOptions$Builder builder()
public java.time.Duration timeout()
public java.time.Duration pollInterval()
public boolean normalizeWhitespace()
public boolean caseSensitive()
public int actualTextPreviewLimit()
public boolean trimText()
public boolean failFastOnMissingElement()
```

## `io.github.testlens.selenium.assertions.UiAssertionReporter` {#io-github-testlens-selenium-assertions-uiassertionreporter}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.assertions.UiAssertionReporter(io.github.testlens.core.OverlayLogger)
public static io.github.testlens.selenium.assertions.UiAssertionReporter noop()
public void started(java.lang.String, java.lang.String)
public void retry(java.lang.String, java.lang.String, int, java.lang.String, java.lang.String)
public void passed(io.github.testlens.selenium.assertions.UiAssertionResult)
public void failed(io.github.testlens.selenium.assertions.UiAssertionResult)
```

## `io.github.testlens.selenium.assertions.UiAssertionResult` {#io-github-testlens-selenium-assertions-uiassertionresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/result-types.md](../../reference/result-types.md)

```java
public static io.github.testlens.selenium.assertions.UiAssertionResult passed(java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, java.time.Duration, java.lang.String)
public static io.github.testlens.selenium.assertions.UiAssertionResult failed(java.lang.String, io.github.testlens.selenium.assertions.UiAssertionFailureReason, java.lang.String, java.lang.String, java.lang.String, int, java.time.Duration, java.lang.String)
public static io.github.testlens.selenium.assertions.UiAssertionResult timedOut(java.lang.String, io.github.testlens.selenium.assertions.UiAssertionFailureReason, java.lang.String, java.lang.String, java.lang.String, int, java.time.Duration, java.lang.String)
public java.lang.String assertionName()
public io.github.testlens.selenium.assertions.UiAssertionStatus status()
public io.github.testlens.selenium.assertions.UiAssertionFailureReason failureReason()
public java.lang.String locatorDescription()
public java.lang.String expectedPreview()
public java.lang.String actualPreview()
public int attempts()
public java.time.Duration elapsed()
public java.lang.String message()
public boolean isPassed()
public java.lang.String summary()
```

## `io.github.testlens.selenium.assertions.UiAssertionStatus` {#io-github-testlens-selenium-assertions-uiassertionstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/reference/result-types.md](../../reference/result-types.md)

```java
public static final io.github.testlens.selenium.assertions.UiAssertionStatus PASSED
public static final io.github.testlens.selenium.assertions.UiAssertionStatus FAILED
public static final io.github.testlens.selenium.assertions.UiAssertionStatus TIMED_OUT
public static final io.github.testlens.selenium.assertions.UiAssertionStatus SKIPPED
public static io.github.testlens.selenium.assertions.UiAssertionStatus[] values()
public static io.github.testlens.selenium.assertions.UiAssertionStatus valueOf(java.lang.String)
```

## `io.github.testlens.selenium.assertions.UiExpect$ElementProbe` {#io-github-testlens-selenium-assertions-uiexpect-elementprobe}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `interface`

```java
public abstract io.github.testlens.selenium.assertions.UiExpect$ElementProbeResult probe(java.util.function.Function<org.openqa.selenium.WebElement, java.lang.String>)
```

## `io.github.testlens.selenium.assertions.UiExpect$ElementProbeResult` {#io-github-testlens-selenium-assertions-uiexpect-elementproberesult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `record`

```java
public io.github.testlens.selenium.assertions.UiExpect$ElementProbeResult(boolean, java.lang.String)
public static io.github.testlens.selenium.assertions.UiExpect$ElementProbeResult present(java.lang.String)
public static io.github.testlens.selenium.assertions.UiExpect$ElementProbeResult missingElement()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean present()
public java.lang.String value()
```

## `io.github.testlens.selenium.assertions.UiExpect$VisibilityProbe` {#io-github-testlens-selenium-assertions-uiexpect-visibilityprobe}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `interface`

```java
public abstract io.github.testlens.selenium.assertions.UiExpect$VisibilityProbeResult probe()
```

## `io.github.testlens.selenium.assertions.UiExpect$VisibilityProbeResult` {#io-github-testlens-selenium-assertions-uiexpect-visibilityproberesult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `record`

```java
public io.github.testlens.selenium.assertions.UiExpect$VisibilityProbeResult(boolean, boolean)
public static io.github.testlens.selenium.assertions.UiExpect$VisibilityProbeResult visibleElement()
public static io.github.testlens.selenium.assertions.UiExpect$VisibilityProbeResult hiddenElement()
public static io.github.testlens.selenium.assertions.UiExpect$VisibilityProbeResult missingElement()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean present()
public boolean visible()
```

## `io.github.testlens.selenium.assertions.UiExpect` {#io-github-testlens-selenium-assertions-uiexpect}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.assertions`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/elements/assertions.md](../../elements/assertions.md)

```java
public io.github.testlens.selenium.assertions.UiExpect(io.github.testlens.selenium.locator.UiLocator, io.github.testlens.selenium.assertions.UiAssertionOptions, io.github.testlens.core.OverlayLogger)
public io.github.testlens.selenium.assertions.UiExpect(io.github.testlens.selenium.locator.UiLocator, io.github.testlens.selenium.assertions.UiAssertionOptions, io.github.testlens.core.OverlayLogger, io.github.testlens.selenium.assertions.UiExpect$VisibilityProbe)
public io.github.testlens.selenium.assertions.UiExpect(io.github.testlens.selenium.locator.UiLocator, io.github.testlens.selenium.assertions.UiAssertionOptions, io.github.testlens.core.OverlayLogger, io.github.testlens.selenium.assertions.UiExpect$VisibilityProbe, io.github.testlens.selenium.assertions.UiExpect$ElementProbe)
public io.github.testlens.selenium.assertions.UiAssertionResult toBeVisible()
public io.github.testlens.selenium.assertions.UiAssertionResult toBeHidden()
public io.github.testlens.selenium.assertions.UiAssertionResult toBeEnabled()
public io.github.testlens.selenium.assertions.UiAssertionResult toBeDisabled()
public io.github.testlens.selenium.assertions.UiAssertionResult toHaveText(java.lang.String)
public io.github.testlens.selenium.assertions.UiAssertionResult toContainText(java.lang.String)
public io.github.testlens.selenium.assertions.UiAssertionResult toHaveValue(java.lang.String)
public io.github.testlens.selenium.assertions.UiAssertionResult toContainValue(java.lang.String)
```
