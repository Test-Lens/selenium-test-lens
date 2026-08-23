---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.business`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.business.BusinessAssertionError` {#io-github-testlens-selenium-business-businessassertionerror}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.business.BusinessAssertionError(java.lang.String, java.util.List<io.github.testlens.selenium.business.BusinessAssertionResult>, io.github.testlens.selenium.business.BusinessAssertionOptions)
public java.lang.String subject()
public java.util.List<io.github.testlens.selenium.business.BusinessAssertionResult> results()
```

## `io.github.testlens.selenium.business.BusinessAssertionFailure` {#io-github-testlens-selenium-business-businessassertionfailure}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.business.BusinessAssertionFailure(java.lang.String, java.lang.String, java.lang.String, java.lang.Throwable, java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.business.BusinessAssertionFailure fromAssertion(java.lang.String, java.lang.String, io.github.testlens.selenium.assertions.UiAssertionResult, java.lang.Throwable, java.time.Duration)
public static io.github.testlens.selenium.business.BusinessAssertionFailure unexpected(java.lang.String, java.lang.String, java.lang.Throwable, java.time.Duration, int)
public java.lang.String subject()
public java.lang.String description()
public java.lang.String message()
public java.lang.Throwable cause()
public java.lang.String assertionSummary()
public java.time.Duration elapsed()
```

## `io.github.testlens.selenium.business.BusinessAssertionOptions$Builder` {#io-github-testlens-selenium-business-businessassertionoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.business.BusinessAssertionOptions$Builder collectFailures(boolean)
public io.github.testlens.selenium.business.BusinessAssertionOptions$Builder failFast(boolean)
public io.github.testlens.selenium.business.BusinessAssertionOptions$Builder includeStackTrace(boolean)
public io.github.testlens.selenium.business.BusinessAssertionOptions$Builder messagePreviewLimit(int)
public io.github.testlens.selenium.business.BusinessAssertionOptions build()
```

## `io.github.testlens.selenium.business.BusinessAssertionOptions` {#io-github-testlens-selenium-business-businessassertionoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.business.BusinessAssertionOptions defaults()
public static io.github.testlens.selenium.business.BusinessAssertionOptions$Builder builder()
public boolean collectFailures()
public boolean failFast()
public boolean includeStackTrace()
public int messagePreviewLimit()
```

## `io.github.testlens.selenium.business.BusinessAssertionReporter` {#io-github-testlens-selenium-business-businessassertionreporter}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.business.BusinessAssertionReporter(io.github.testlens.core.OverlayLogger)
public void groupStarted(java.lang.String, int)
public void checkStarted(java.lang.String, java.lang.String)
public void checkFinished(io.github.testlens.selenium.business.BusinessAssertionResult)
public void groupFinished(java.lang.String, boolean, int, int)
```

## `io.github.testlens.selenium.business.BusinessAssertionResult` {#io-github-testlens-selenium-business-businessassertionresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.business.BusinessAssertionResult passed(java.lang.String, java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.business.BusinessAssertionResult failed(java.lang.String, java.lang.String, io.github.testlens.selenium.business.BusinessAssertionFailure, java.time.Duration)
public static io.github.testlens.selenium.business.BusinessAssertionResult skipped(java.lang.String, java.lang.String, java.lang.String)
public java.lang.String subject()
public java.lang.String description()
public io.github.testlens.selenium.business.BusinessAssertionStatus status()
public java.lang.String message()
public java.time.Duration elapsed()
public io.github.testlens.selenium.business.BusinessAssertionFailure failure()
public boolean isPassed()
public java.lang.String summary()
```

## `io.github.testlens.selenium.business.BusinessAssertionStatus` {#io-github-testlens-selenium-business-businessassertionstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.business.BusinessAssertionStatus PASSED
public static final io.github.testlens.selenium.business.BusinessAssertionStatus FAILED
public static final io.github.testlens.selenium.business.BusinessAssertionStatus SKIPPED
public static io.github.testlens.selenium.business.BusinessAssertionStatus[] values()
public static io.github.testlens.selenium.business.BusinessAssertionStatus valueOf(java.lang.String)
```

## `io.github.testlens.selenium.business.BusinessAssertions` {#io-github-testlens-selenium-business-businessassertions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.business`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.business.BusinessAssertions(java.lang.String, io.github.testlens.selenium.business.BusinessAssertionOptions, io.github.testlens.core.OverlayLogger)
public io.github.testlens.selenium.business.BusinessAssertions check(java.lang.String, java.lang.Runnable)
public io.github.testlens.selenium.business.BusinessAssertions check(java.lang.String, java.util.function.Supplier<io.github.testlens.selenium.assertions.UiAssertionResult>)
public io.github.testlens.selenium.business.BusinessAssertionResult verify()
public java.lang.String subject()
public java.util.List<io.github.testlens.selenium.business.BusinessAssertionResult> results()
```
