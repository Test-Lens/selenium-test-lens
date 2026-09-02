---
hide:
  - navigation
  - toc
---

<div class="lens-hero" markdown>

# Selenium Test Lens

**Better visibility and diagnostics for Selenium tests.**

Selenium Test Lens adds retry-aware interactions, waits and failure diagnostics on top of the `WebDriver` you already use.

[Get started](getting-started.md){ .md-button .md-button--primary }
[GitHub](https://github.com/Test-Lens/selenium-test-lens){ .md-button }
[Maven Central](https://central.sonatype.com/artifact/io.github.test-lens/selenium-test-lens/0.1.0){ .md-button }
[Javadoc](https://javadoc.io/doc/io.github.test-lens/selenium-test-lens/0.1.0/){ .md-button }

</div>

<!-- SCREENSHOT TODO: assets/screenshots/home-hud-highlight.png
Show a real Test Lens action in a representative application.
The target must be highlighted and the HUD must show the same action and target label.
Crop unrelated browser chrome and do not display personal or secret data.
Feature documented: combined HUD and element-highlight workflow.
Suggested alt text: Test Lens HUD showing a click action beside its highlighted target.
-->

## Install the stable release

Selenium Test Lens requires Java 17 or newer. Add the main runtime artifact:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

!!! important "Selenium stays under your control"

    Selenium Test Lens does not bring its own Selenium version. Keep `selenium-java` as an explicit dependency in your project and create and close `WebDriver` exactly as you do today.

## Attach to an existing driver

```java
import io.github.testlens.TestLens;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

WebDriver driver = createExistingFrameworkDriver();
TestLens lens = TestLens.attach(driver);
try {
    lens.startSession("login");
    driver.get("https://example.test/login");

    lens.locator(By.id("username"), "Username").fill("john");
    lens.locator(By.id("login"), "Login").click();
    lens.locator(By.id("welcome"), "Welcome").expect().toBeVisible();
    lens.finishPassed();
} catch (RuntimeException | Error failure) {
    lens.finishFailed(failure);
    throw failure;
} finally {
    driver.quit();
}
```

`finishPassed()`, `finishFailed(Throwable)`, and `finishSkipped(String)` finalize the Lens session with the matching passed, failed, or skipped status and write its reports and diagnostics. None of them closes the driver.

## Why Test Lens?

Selenium Test Lens works alongside Selenium rather than replacing it. You can adopt it gradually and use raw `WebDriver` whenever you need lower-level control.

<div class="grid cards" markdown>

-   :material-eye-outline: **See what the test is doing**

    ---

    Lens operations feed the in-browser HUD and session trace, so you can follow the test as it runs and investigate failures afterwards.

-   :material-timer-outline: **Less repetitive waiting code**

    ---

    Locator operations and assertions include retry-aware waits for common UI states.

-   :material-file-chart-outline: **Better failure diagnostics**

    ---

    Generate HTML and JSON traces and keep screenshots and other evidence with the test session.

-   :material-cursor-default-click-outline: **Use it alongside Selenium**

    ---

    Keep your existing `WebDriver`, Page Objects and test runner. Use Selenium directly for operations Lens does not wrap.

</div>

## How it fits

```text
Existing Selenium project
        |
        v
Existing WebDriver
        |
        v
TestLens.attach(driver)
        |
        +--> interactions, waits, assertions
        +--> HUD and event trace
        +--> reports and evidence
```

Test Lens does not replace your test runner or reporting stack. JUnit, TestNG, Allure and existing Page Objects can stay where they are. JUnit 5 and TestNG users may add the optional published [`selenium-test-lens-junit5`](integrations/junit5.md) or [`selenium-test-lens-testng`](integrations/testng.md) adapter for per-invocation driver/Lens creation and native lifecycle mapping.

The React module is optional. For lower-level browser interactions that Lens doesn't wrap, use Selenium directly.

## Continue

- [Install and write your first test](getting-started.md)
- [Integrate with an existing Selenium project](framework-integration.md)
- [Explore capabilities](capabilities.md)
- [Migrate incrementally from raw Selenium](migration.md)
- [View the changelog](https://github.com/Test-Lens/selenium-test-lens/blob/main/CHANGELOG.md)
