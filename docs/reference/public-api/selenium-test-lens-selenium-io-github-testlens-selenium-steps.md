---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.steps`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.steps.UiStepContext` {#io-github-testlens-selenium-steps-uistepcontext}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.steps.UiStepContext()
public void push(java.lang.String)
public void pop()
public java.util.List<java.lang.String> currentPath()
```

## `io.github.testlens.selenium.steps.UiStepError` {#io-github-testlens-selenium-steps-uisteperror}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/steps-business-assertions.md](../../advanced/steps-business-assertions.md)

```java
public io.github.testlens.selenium.steps.UiStepError(io.github.testlens.selenium.steps.UiStepResult)
public io.github.testlens.selenium.steps.UiStepResult result()
```

## `io.github.testlens.selenium.steps.UiStepFailure` {#io-github-testlens-selenium-steps-uistepfailure}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/steps-business-assertions.md](../../advanced/steps-business-assertions.md)

```java
public static io.github.testlens.selenium.steps.UiStepFailure from(java.lang.Throwable, io.github.testlens.selenium.steps.UiStepOptions)
public java.lang.String message()
public java.lang.Throwable cause()
public java.lang.String causeType()
public java.lang.String stackTrace()
```

## `io.github.testlens.selenium.steps.UiStepOptions$Builder` {#io-github-testlens-selenium-steps-uistepoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/steps-business-assertions.md](../../advanced/steps-business-assertions.md)

```java
public io.github.testlens.selenium.steps.UiStepOptions$Builder failFast(boolean)
public io.github.testlens.selenium.steps.UiStepOptions$Builder logToHud(boolean)
public io.github.testlens.selenium.steps.UiStepOptions$Builder captureNestedEvents(boolean)
public io.github.testlens.selenium.steps.UiStepOptions$Builder includeStackTrace(boolean)
public io.github.testlens.selenium.steps.UiStepOptions$Builder captureScreenshotOnFailure(boolean)
public io.github.testlens.selenium.steps.UiStepOptions$Builder screenshotCaptureOptions(io.github.testlens.selenium.evidence.ScreenshotCaptureOptions)
public io.github.testlens.selenium.steps.UiStepOptions$Builder messagePreviewLimit(int)
public io.github.testlens.selenium.steps.UiStepOptions build()
```

## `io.github.testlens.selenium.steps.UiStepOptions` {#io-github-testlens-selenium-steps-uistepoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/steps-business-assertions.md](../../advanced/steps-business-assertions.md)

```java
public static io.github.testlens.selenium.steps.UiStepOptions defaults()
public static io.github.testlens.selenium.steps.UiStepOptions$Builder builder()
public boolean failFast()
public boolean logToHud()
public boolean captureNestedEvents()
public boolean includeStackTrace()
public boolean captureScreenshotOnFailure()
public io.github.testlens.selenium.evidence.ScreenshotCaptureOptions screenshotCaptureOptions()
public int messagePreviewLimit()
```

## `io.github.testlens.selenium.steps.UiStepReporter` {#io-github-testlens-selenium-steps-uistepreporter}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.steps.UiStepReporter(io.github.testlens.core.OverlayLogger)
public void started(java.lang.String, io.github.testlens.selenium.steps.UiStepOptions)
public void finished(io.github.testlens.selenium.steps.UiStepResult, io.github.testlens.selenium.steps.UiStepOptions)
public static java.lang.String formatFailure(io.github.testlens.selenium.steps.UiStepResult)
```

## `io.github.testlens.selenium.steps.UiStepResult` {#io-github-testlens-selenium-steps-uistepresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/steps-business-assertions.md](../../advanced/steps-business-assertions.md)

```java
public static io.github.testlens.selenium.steps.UiStepResult passed(java.lang.String, java.time.Instant, java.time.Instant)
public static io.github.testlens.selenium.steps.UiStepResult failed(java.lang.String, java.time.Instant, java.time.Instant, io.github.testlens.selenium.steps.UiStepFailure)
public static io.github.testlens.selenium.steps.UiStepResult skipped(java.lang.String, java.time.Instant, java.time.Instant, java.lang.String)
public java.lang.String name()
public io.github.testlens.selenium.steps.UiStepStatus status()
public java.time.Instant startedAt()
public java.time.Instant endedAt()
public java.time.Duration elapsed()
public io.github.testlens.selenium.steps.UiStepFailure failure()
public java.util.List<io.github.testlens.selenium.steps.UiStepResult> children()
public boolean isPassed()
public java.lang.String summary()
```

## `io.github.testlens.selenium.steps.UiStepScope` {#io-github-testlens-selenium-steps-uistepscope}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.steps.UiStepScope(io.github.testlens.core.OverlayLogger, java.util.function.Consumer<java.lang.String>, java.util.function.Consumer<java.lang.String>)
public io.github.testlens.selenium.steps.UiStepResult run(java.lang.String, io.github.testlens.selenium.steps.UiStepOptions, java.lang.Runnable)
public io.github.testlens.selenium.steps.UiStepContext context()
```

## `io.github.testlens.selenium.steps.UiStepStatus` {#io-github-testlens-selenium-steps-uistepstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.steps`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/advanced/steps-business-assertions.md](../../advanced/steps-business-assertions.md)

```java
public static final io.github.testlens.selenium.steps.UiStepStatus RUNNING
public static final io.github.testlens.selenium.steps.UiStepStatus PASSED
public static final io.github.testlens.selenium.steps.UiStepStatus FAILED
public static final io.github.testlens.selenium.steps.UiStepStatus SKIPPED
public static io.github.testlens.selenium.steps.UiStepStatus[] values()
public static io.github.testlens.selenium.steps.UiStepStatus valueOf(java.lang.String)
```
