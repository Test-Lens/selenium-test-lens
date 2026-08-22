# Getting started

## Existing WebDriver and native operations

Attach Lens to the driver already owned by the framework. It does not create, wrap, close, or replace that driver.

```java
TestLens lens = TestLens.attach(existingDriver);
lens.startSession("checkout");
lens.locator(By.id("country"), "Country").selectByValue("PL");
```

Context APIs are equally direct: `switchToFrame(By, label)`, `switchToFrame(index, label)`, `switchToParentFrame()`, `switchToDefaultContent()`, `switchToWindow(handle, label)`, and deterministic `switchToNewWindow(handlesBefore, label)`. Native browser dialogs use `lens.alert()`.

<p align="center">
  <img src="assets/brand/test-lens-badge.png" alt="Test Lens badge" width="420">
</p>

This guide shows the smallest path from a Maven dependency to a Selenium test using Selenium Test Lens 0.1.0.

## Requirements

- Java 17
- Maven 3.x
- A Selenium `WebDriver` supplied by your test project

The consuming framework supplies Selenium. Version 0.1.0 is verified with Selenium 4.39.0; no broader version range is claimed here.

## Choose a module

| Use case | Artifact |
|---|---|
| Selenium locators, assertions, evidence, auth, network | `selenium-test-lens` |
| Runtime overlay resources only | `selenium-test-lens-overlay` |
| Logging and trace model only | `selenium-test-lens-core` |
| React-specific helpers | `selenium-test-lens-react` |

Main runtime dependency:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

## First Selenium Test Lens session

```java
import io.github.testlens.TestLens;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

WebDriver driver = createExistingFrameworkDriver();
TestLens lens = TestLens.attach(driver);
lens.startSession("checkout");

driver.get("https://example.test/checkout");
lens.locator(By.id("customer"), "Customer").fill("John");
lens.locator(By.id("save-order"), "Save order").click();
lens.locator(By.id("toast"), "Saved confirmation").expect().toContainText("Saved");
lens.finishPassed();
```

## HUD configuration

```java
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudThemePreset;

OverlayConfig config = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.DARK)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .overlayConfig(config)
        .build());
```

## Build and examples

Full checks:

```powershell
mvn -q test
mvn -q -DskipTests compile
```

Selected module checks:

```powershell
mvn -q -pl selenium-test-lens-overlay -am test
mvn -q -pl selenium-test-lens-selenium -am test
mvn -q -pl selenium-test-lens-examples -am test
mvn -q -pl selenium-test-lens -am test
```

The `selenium-test-lens-examples` module contains documentation-style examples. Selenium/WebDriver-dependent examples are disabled and intended to compile and document API usage, not to run without a real application and driver.

