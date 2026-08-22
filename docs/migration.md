# Migration

This guide summarizes practical migration from existing Selenium code to Selenium Test Lens 0.1.0.

## Naming

| Historical concept | Current Selenium Test Lens concept |
|---|---|
| historical helper codebase | Selenium Test Lens |
| pre-release helper package | superseded by `io.github.testlens` |
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
    <version>0.1.0</version>
</dependency>
```

Selenium module:

```xml
<dependency>
    <groupId>io.github.testlens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

The consuming project continues to own its Selenium dependency and WebDriver lifecycle.

## Package root

Current package root:

```java
io.github.testlens
```

Common import:

```java
import io.github.testlens.TestLens;
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
TestLens lens = TestLens.attach(driver);
lens.locator(By.cssSelector("[data-testid='save']"), "Save").click();
```

Direct assertion:

```java
assertEquals("Saved", driver.findElement(By.cssSelector("[data-testid='toast']")).getText());
```

Retryable assertion:

```java
lens.locator(By.cssSelector("[data-testid='toast']"), "Toast")
        .expect().toHaveText("Saved");
```

Named step:

```java
lens.step("Save order", () -> {
    lens.getByTestId("save-order").click();
});
```

Trace/evidence:

```java
lens.startSession("Checkout flow");
lens.captureScreenshot("After save");
lens.finishPassed();
```

Additional mechanical mappings:

| Existing Selenium | Selenium Test Lens 0.1.0 |
|---|---|
| `findElement(by).clear(); findElement(by).sendKeys(value)` | `lens.locator(by, label).fill(value)` |
| `findElements(by).size()` | `lens.locator(by, label).count()` |
| `findElements(by).get(index).click()` | `lens.locator(by, label).nth(index).click()` |
| `new Select(element).selectByVisibleText(value)` | `lens.locator(by, label).selectByVisibleText(value)` |
| `new Actions(driver).moveToElement(element).perform()` | `lens.locator(by, label).hover()` |
| `new Actions(driver).doubleClick(element).perform()` | `lens.locator(by, label).doubleClick()` |
| `new Actions(driver).contextClick(element).perform()` | `lens.locator(by, label).rightClick()` |
| `driver.switchTo().frame(driver.findElement(by))` | `lens.switchToFrame(by, label)` |
| `driver.switchTo().defaultContent()` | `lens.switchToDefaultContent()` |
| `driver.switchTo().window(handle)` | `lens.switchToWindow(handle, label)` |
| `driver.switchTo().alert().accept()` | `lens.alert().accept()` |

## What has not changed yet

- The project is still pre-1.0.
- The current package root is `io.github.testlens`.
- Some historical runtime aliases may still exist for browser compatibility.
- `getByRole` is not a full accessibility engine.
- network diagnostics are passive and do not provide mocking/interception.
- video evidence is attachment/reference based, not recording based.

