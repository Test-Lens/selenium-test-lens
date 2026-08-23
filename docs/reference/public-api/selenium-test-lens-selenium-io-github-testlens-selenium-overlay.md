---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.overlay`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.overlay.OverlayAction` {#io-github-testlens-selenium-overlay-overlayaction}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.overlay.OverlayAction click(org.openqa.selenium.By)
public static io.github.testlens.selenium.overlay.OverlayAction pressEscape()
public static io.github.testlens.selenium.overlay.OverlayAction waitUntilGone(org.openqa.selenium.By)
public static io.github.testlens.selenium.overlay.OverlayAction fail(java.lang.String)
public io.github.testlens.selenium.overlay.OverlayActionType type()
public org.openqa.selenium.By target()
public java.lang.String reason()
public java.lang.String describe()
```

## `io.github.testlens.selenium.overlay.OverlayActionType` {#io-github-testlens-selenium-overlay-overlayactiontype}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.overlay.OverlayActionType CLICK
public static final io.github.testlens.selenium.overlay.OverlayActionType PRESS_ESCAPE
public static final io.github.testlens.selenium.overlay.OverlayActionType WAIT_UNTIL_GONE
public static final io.github.testlens.selenium.overlay.OverlayActionType FAIL
public static io.github.testlens.selenium.overlay.OverlayActionType[] values()
public static io.github.testlens.selenium.overlay.OverlayActionType valueOf(java.lang.String)
```

## `io.github.testlens.selenium.overlay.OverlayHandler$Builder` {#io-github-testlens-selenium-overlay-overlayhandler-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.overlay.OverlayHandler$Builder detect(org.openqa.selenium.By)
public io.github.testlens.selenium.overlay.OverlayHandler$Builder action(io.github.testlens.selenium.overlay.OverlayAction)
public io.github.testlens.selenium.overlay.OverlayHandler$Builder actions(java.util.List<io.github.testlens.selenium.overlay.OverlayAction>)
public io.github.testlens.selenium.overlay.OverlayHandler$Builder optional(boolean)
public io.github.testlens.selenium.overlay.OverlayHandler$Builder timeout(java.time.Duration)
public io.github.testlens.selenium.overlay.OverlayHandler$Builder failIfStillVisible(boolean)
public io.github.testlens.selenium.overlay.OverlayHandler build()
```

## `io.github.testlens.selenium.overlay.OverlayHandler` {#io-github-testlens-selenium-overlay-overlayhandler}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.overlay.OverlayHandler$Builder builder(java.lang.String)
public java.lang.String name()
public org.openqa.selenium.By detect()
public java.util.List<io.github.testlens.selenium.overlay.OverlayAction> actions()
public boolean optional()
public java.time.Duration timeout()
public boolean failIfStillVisible()
```

## `io.github.testlens.selenium.overlay.OverlayHandlingResult` {#io-github-testlens-selenium-overlay-overlayhandlingresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.overlay.OverlayHandlingResult notDetected(java.lang.String, java.time.Duration)
public static io.github.testlens.selenium.overlay.OverlayHandlingResult handled(java.lang.String, java.util.List<java.lang.String>, java.time.Duration)
public static io.github.testlens.selenium.overlay.OverlayHandlingResult stillVisible(java.lang.String, java.util.List<java.lang.String>, java.time.Duration)
public static io.github.testlens.selenium.overlay.OverlayHandlingResult failed(java.lang.String, java.util.List<java.lang.String>, java.lang.String, java.lang.Throwable, java.time.Duration)
public static io.github.testlens.selenium.overlay.OverlayHandlingResult skipped(java.lang.String, java.lang.String, java.time.Duration)
public java.lang.String handlerName()
public io.github.testlens.selenium.overlay.OverlayHandlingStatus status()
public java.util.List<java.lang.String> attemptedActions()
public java.lang.String message()
public java.lang.Throwable exception()
public java.time.Duration elapsed()
public boolean detected()
```

## `io.github.testlens.selenium.overlay.OverlayHandlingStatus` {#io-github-testlens-selenium-overlay-overlayhandlingstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.overlay.OverlayHandlingStatus NOT_DETECTED
public static final io.github.testlens.selenium.overlay.OverlayHandlingStatus HANDLED
public static final io.github.testlens.selenium.overlay.OverlayHandlingStatus STILL_VISIBLE
public static final io.github.testlens.selenium.overlay.OverlayHandlingStatus FAILED
public static final io.github.testlens.selenium.overlay.OverlayHandlingStatus SKIPPED
public static io.github.testlens.selenium.overlay.OverlayHandlingStatus[] values()
public static io.github.testlens.selenium.overlay.OverlayHandlingStatus valueOf(java.lang.String)
```

## `io.github.testlens.selenium.overlay.OverlayPolicy$Builder` {#io-github-testlens-selenium-overlay-overlaypolicy-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.selenium.overlay.OverlayPolicy$Builder()
public io.github.testlens.selenium.overlay.OverlayPolicy$Builder handler(io.github.testlens.selenium.overlay.OverlayHandler)
public io.github.testlens.selenium.overlay.OverlayPolicy build()
```

## `io.github.testlens.selenium.overlay.OverlayPolicy` {#io-github-testlens-selenium-overlay-overlaypolicy}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.overlay.OverlayPolicy none()
public static io.github.testlens.selenium.overlay.OverlayPolicy$Builder builder()
public java.util.List<io.github.testlens.selenium.overlay.OverlayHandler> handlers()
public boolean isEmpty()
```

## `io.github.testlens.selenium.overlay.OverlayPolicyExecutor` {#io-github-testlens-selenium-overlay-overlaypolicyexecutor}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.overlay`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.overlay.OverlayPolicyExecutor(org.openqa.selenium.WebDriver, io.github.testlens.selenium.overlay.OverlayPolicy, io.github.testlens.core.OverlayLogger)
public java.util.List<io.github.testlens.selenium.overlay.OverlayHandlingResult> handleKnownOverlays()
public boolean handleKnownOverlaysIfAny()
```
