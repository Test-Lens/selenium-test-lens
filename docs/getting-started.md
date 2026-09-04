# Getting started

Selenium Test Lens works with the `WebDriver` your test framework already uses. You can add it to an existing Selenium project without changing how the driver is created or closed.

This guide shows the shortest path from the Maven dependency to a working Lens session.

## Requirements

- Java 17 or newer
- Maven 3.x
- A Selenium `WebDriver` created by your test project

The latest published Selenium Test Lens release is `0.1.0`, verified with Selenium 4.39.0. The source tree is currently the unreleased `0.2.0-SNAPSHOT` development line.

## Installation

Add the main Selenium Test Lens runtime to your Maven project:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

Keep Selenium as an explicit dependency and use the version already managed by your project:

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>${selenium.version}</version>
</dependency>
```

For a source build of the current development line, add the optional JUnit 5 integration as a test dependency. The `0.2.0-SNAPSHOT` coordinate below is not the stable Maven Central release. It brings the lifecycle extension, while your project still selects the Selenium version:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-junit5</artifactId>
    <version>0.2.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

See [JUnit 5 integration](integrations/junit5.md) for `@RegisterExtension` and parameter injection.

For TestNG, add `selenium-test-lens-testng` with test scope and register both `@Listeners(TestLensTestNgListener.class)` and `@TestLensTestNg(factory = YourFactory.class)`. The listener exposes the current driver, Lens, and session through `TestLensTestNgContext.current()` and owns driver shutdown. See [TestNG integration](integrations/testng.md).

## Your first Lens session

Attach Lens after your project creates its driver. Start a session before using Lens operations, then finalize it with the test outcome.

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
    lens.locator(By.id("remember"), "Remember me").check();
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

Common form operations remain on the lazy locator abstraction: use `check()`/`uncheck()` for semantic native or ARIA controls, `upload(Path...)` for file inputs, and `focus()` or `scrollIntoView()` when those browser operations are intentional. You do not need to expose a raw `WebElement`; see [Element actions](elements/actions.md).

The main facade also exposes lazy semantic factories: `getByLabel`, `getByPlaceholder`, `getByAltText`, and named `getByRole`. Named roles and labels use the accessible name computed by WebDriver, so native labels, `aria-labelledby`, multiple references, and descendant image alt text follow the browser implementation rather than a partial `aria-label || text` approximation.

Lens finalization writes the session diagnostics. Use `finishSkipped(reason)` for an aborted test or unmet assumption; unlike `finishFailed(...)`, it does not request a failure screenshot. Keep your existing `WebDriver` cleanup as-is.

For JUnit, TestNG and reporter lifecycle examples, see [Framework integration](framework-integration.md). The runner adapters are the recommended JUnit 5 and TestNG paths for the `0.2.0-SNAPSHOT` development line.

## Run your test

Run the test with your existing Maven command:

```bash
mvn test
```

When the session is finalized, Test Lens writes its HTML and JSON reports under `target/ui-test-lens` by default.

## Optional: configure the HUD

The default configuration is enough to get started. To change the in-browser HUD, pass `TestLensOptions` when attaching Lens:

```java
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudThemePreset;

OverlayConfig overlay = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.DARK)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .overlayConfig(overlay)
        .build());
```

The HUD is only a diagnostic aid and does not change test execution or assertions.

## Next steps

- [Integrate Lens with JUnit, TestNG, or an existing reporter](framework-integration.md)
- [Use the JUnit 5 lifecycle extension](integrations/junit5.md)
- [Use locators, actions, waits, and assertions](elements/index.md)
- [Configure Test Lens](configuration.md)
- [Migrate incrementally from raw Selenium](migration.md)
- [Add the optional React extension](framework-integration.md#optional-react-extension)

You can keep using existing Page Objects and call raw Selenium directly for operations Lens does not wrap.
