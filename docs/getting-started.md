# Getting started

This guide shows the smallest path from a local build to a Selenium test using Selenium Test Lens.

## Requirements

- Java 17
- Maven 3.x
- A Selenium `WebDriver` supplied by your test project

Selenium Test Lens is currently `0.1.0-SNAPSHOT`. Maven Central publishing is not configured yet, so local development usually starts with:

```powershell
mvn -q -DskipTests install
```

## Choose a module

| Use case | Artifact |
|---|---|
| All-in-one dependency bundle | `selenium-test-lens` |
| Selenium locators, assertions, evidence, auth, network | `selenium-test-lens-selenium` |
| Runtime overlay resources only | `selenium-test-lens-overlay` |
| Logging and trace model only | `selenium-test-lens-core` |
| React-specific helpers | `selenium-test-lens-react` |

All-in-one dependency:

```xml
<dependency>
    <groupId>io.github.testlens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## First Selenium overlay

```java
import io.github.testlens.JsOverlayDebug;
import org.openqa.selenium.WebDriver;

WebDriver driver = createDriver();
JsOverlayDebug overlay = new JsOverlayDebug(driver);

overlay.setStep("Open checkout");
overlay.hudLog("info", "Checkout page opened", "local");

overlay.getByTestId("save-order").click();

overlay.expect(overlay.getByTestId("toast"))
        .toContainText("Saved");
```

## HUD configuration

```java
import io.github.testlens.OverlayConfig;
import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudThemePreset;

OverlayConfig config = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.DARK)
        .build();

JsOverlayDebug overlay = new JsOverlayDebug(driver, config);
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
