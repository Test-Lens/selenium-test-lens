# Migration

This guide summarizes the practical migration from the historical Selenium helper API to the current Selenium Test Lens `0.1.0-SNAPSHOT` API.

Selenium Test Lens is pre-1.0. This guide describes the current state, not a Maven Central release process.

## Naming

| Historical concept | Current Selenium Test Lens concept |
|---|---|
| historical helper codebase | Selenium Test Lens |
| historical helper package | `io.github.testlens` |
| old one-module helper layout | multi-module Maven layout |
| Selenium-only helper | WebDriver reliability and diagnostics layer |
| hardcoded overlay/popup handling | configurable `OverlayPolicy` |
| direct one-off element lookup | retryable `UiLocator` |
| one-off assertion | retryable `UiExpect` |
| ad-hoc step logging | `step(...)` DSL and HUD logging |
| manual screenshot path notes | trace/evidence artifacts |
| repeated login flow | auth/session state capture and restore |
| ad-hoc network checks | passive network diagnostics |

## Maven artifacts

All-in-one:

```xml
<dependency>
    <groupId>io.github.testlens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Selenium module:

```xml
<dependency>
    <groupId>io.github.testlens</groupId>
    <artifactId>selenium-test-lens-selenium</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Maven Central publishing is not configured yet. Use local builds, for example `mvn -q -DskipTests install`, until publishing metadata is finalized.

## Package root

Current package root:

```java
io.github.testlens
```

Common import:

```java
import io.github.testlens.JsOverlayDebug;
```

HUD theme classes live under the HUD package:

```java
import io.github.testlens.hud.HudTheme;
import io.github.testlens.hud.HudThemePreset;
```

## Common replacements

Direct Selenium click:

```java
driver.findElement(By.cssSelector("[data-testid='save']")).click();
```

Selenium Test Lens:

```java
overlay.getByTestId("save").click();
```

Direct assertion:

```java
assertEquals("Saved", driver.findElement(By.cssSelector("[data-testid='toast']")).getText());
```

Retryable assertion:

```java
overlay.expect(overlay.getByTestId("toast"))
        .toHaveText("Saved");
```

Named step:

```java
overlay.step("Save order", () -> {
    overlay.getByTestId("save-order").click();
});
```

Trace/evidence:

```java
overlay.startSession("Checkout flow");
overlay.captureScreenshot("After save");
overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
```

## What has not changed yet

- The project is still pre-1.0.
- Maven Central release metadata is not configured.
- The current package root is `io.github.testlens`.
- Some historical runtime aliases may still exist for browser compatibility.
- `getByRole` is not a full accessibility engine.
- network diagnostics are passive and do not provide mocking/interception.
- video evidence is attachment/reference based, not recording based.

