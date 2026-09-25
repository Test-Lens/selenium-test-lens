# Getting started

Test Lens for Selenium works with the `WebDriver` your test framework already uses. You can add it to an existing Selenium project without changing how the driver is created or closed.

This guide shows the shortest path from the Maven dependency to a working Lens session.

## Requirements

- Java 17 or newer
- Maven 3.x or Gradle
- A Selenium `WebDriver` created by your test project

The latest Test Lens for Selenium release is `0.3.0`, verified with Selenium 4.39.0.

## Installation

Add the main Test Lens for Selenium runtime to your Maven project:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.3.0</version>
</dependency>
```

The same stable release is available to Gradle consumers.

=== "Kotlin DSL"

    ```kotlin
    dependencies {
        testImplementation("io.github.test-lens:selenium-test-lens:0.3.0")
    }
    ```

=== "Groovy DSL"

    ```groovy
    dependencies {
        testImplementation 'io.github.test-lens:selenium-test-lens:0.3.0'
    }
    ```

Java 17 is the minimum runtime and bytecode level; Java 11 is not supported.
Published-artifact consumers are continuously checked with Maven and Gradle
on JDK 17 and JDK 21.

Keep Selenium as an explicit dependency and use the version already managed by your project:

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>${selenium.version}</version>
</dependency>
```

Add the optional JUnit 5 integration as a test dependency. It brings the lifecycle extension, while your project still selects the Selenium version:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-junit5</artifactId>
    <version>0.3.0</version>
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

## Observe existing Selenium Page Objects (0.4.0)

The unreleased 0.4.0 line can observe ordinary Selenium calls without changing a Page Object to `UiLocator`. Attach Lens to the driver your project already owns, then pass the explicit observed facade to Page Objects **before** constructing them:

```java
WebDriver rawDriver = createDriver();
TestLens lens = TestLens.attach(rawDriver, options);
WebDriver driver = lens.observeDriver();

LoginPage page = new LoginPage(driver);
lens.startSession("native selenium");
page.login();
```

Calls such as `driver.findElement(by).click()`, `element.clear()`, and `element.sendKeys(...)` keep native Selenium semantics. Observation adds the existing semantic HUD, trace/report correlation, source location, redaction, duration, and automatic highlight lifecycle. It does not add Smart Click, waits, actionability checks, retries, stale recovery, another lookup, or JavaScript fallback. A native command is delegated once and its original Selenium exception is returned unchanged. Use `UiLocator` when Test Lens interaction and recovery semantics are wanted.

`lens.driver()` still returns the attached/raw driver. Calling `observeDriver()` does not instrument old references: the returned facade must be the reference used by the Page Object. Repeated calls return the stable facade for that Lens. Provider code that intentionally needs a concrete driver class can continue to use `lens.driver()`; the observed facade preserves Selenium interfaces such as `JavascriptExecutor`, `TakesScreenshot`, `Interactive`, and `HasCapabilities`, but is not promised to be a concrete `ChromeDriver` or `RemoteWebDriver`.

An existing element can instead be observed selectively, without a lookup or any other browser command at wrapping time:

```java
lens.observe(rawDriver.findElement(By.id("save")), "Save").click();
```

Labels describe that particular observed view and are redacted before diagnostic presentation. `sendKeys` values, JavaScript source/arguments/results, and arbitrary returned application data are not collected.

To observe an explicit JavaScript command, execute it through the observed driver. Direct observed-element arguments are visual targets; nested object graphs are deliberately not searched:

```java
WebElement button = lens.observe(rawDriver.findElement(By.id("save")), "Save");
((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
```

Using the raw executor with an observed element remains valid Selenium serialization, but the raw command itself is not observed and Test Lens does not invent a PASSED/FAILED lifecycle for it. `executeAsyncScript` completes only when Selenium returns or throws. Script text, arguments, and result values are not recorded.

`WebDriverWait` remains a Selenium wait: repeated finds and state reads are technical DEBUG diagnostics rather than Test Lens retries or functional failures. `PageFactory`, child searches, lists, active elements, frame/window navigation, Actions, and open Shadow DOM descendants retain observation where supported by Selenium 4.39. Locator provenance is captured from the real `findElement(s)(By)` call without a second find; a selectively wrapped raw element correctly has an unknown locator.

Collections can be composed without leaving `UiLocator`: scope a descendant with `locator(...)`, keep parents with `filterHas(...)`, filter visible text or DOM attributes, then wait for a current count. The order of filters, positional selection, and descendant lookup is preserved.

Collection and state assertions use the same fresh-DOM polling model:

```java
lens.locator(By.cssSelector(".product-card"))
        .filterByAttribute("data-status", "available")
        .expect()
        .toHaveCount(3);
lens.getByTestId("save").expect().toHaveAttribute("aria-busy", "false");
lens.getByRole("checkbox", "Terms").expect().toBeChecked();
```

These assertions read state only. They do not click controls or mutate the page, and their polling does not count as a recovery retry.

Use page assertions for navigation and document readiness without creating a locator:

```java
lens.expectPage().toContainUrl("/checkout");
lens.expectPage().toHaveTitle("Checkout");
```

They observe the current window selected by your Selenium flow. URL matching is raw and case-sensitive; title matching follows `UiAssertionOptions` text settings. Reported URL previews omit credentials, query strings, and fragments.

For readiness rather than an assertion value, the main facade also delegates to the page waits:

```java
lens.waitForPageReady();
lens.waitForNetworkIdle(Duration.ofMillis(500), Duration.ofSeconds(5));
```

The defaults come from `TestLensOptions.locatorOptions()`. The network wait is an XHR/fetch tracker heuristic, not complete browser network-idle detection; see [Element and page waiting](elements/waiting.md#page-and-javascript-waits).

Lens finalization writes the session diagnostics. Use `finishSkipped(reason)` for an aborted test or unmet assumption; unlike `finishFailed(...)`, it does not request a failure screenshot. Keep your existing `WebDriver` cleanup as-is.

For JUnit, TestNG and reporter lifecycle examples, see [Framework integration](framework-integration.md). The runner adapters are the recommended JUnit 5 and TestNG paths in 0.3.0.

## Run your test

Run the test with your existing Maven command:

```bash
mvn test
```

When the session is finalized, Test Lens writes its HTML and JSON reports under `target/ui-test-lens` by default.

## Optional: configure the 0.3.0 HUD

The default `COMPACT` HUD is enough to get started. In release 0.3.0, customize it through immutable `HudOptions` and pass it with `TestLensOptions`:

```java
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPosition;
import io.github.testlens.hud.HudPreset;

HudOptions hud = HudOptions.builder()
        .preset(HudPreset.COMPACT)
        .position(HudPosition.TOP_RIGHT)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .hud(hud)
        .build());
```

The HUD is only a diagnostic aid and does not change test execution or assertions. [HUD Studio](observability/hud-studio.md) edits the same renderer and generates matching Java configuration. Legacy `OverlayConfig` HUD setters remain compatible; explicit `HudOptions` wins for overlapping values.

## Next steps

- [Integrate Lens with JUnit, TestNG, or an existing reporter](framework-integration.md)
- [Use the JUnit 5 lifecycle extension](integrations/junit5.md)
- [Use locators, actions, waits, and assertions](elements/index.md)
- [Configure Test Lens](configuration.md)
- [Review what's new in 0.3.0](whats-new-0.3.0.md)
- [Migrate from 0.2.x](migrating-0.2-to-0.3.md)
- [Migrate incrementally from raw Selenium](migration.md)
- [Add the optional React/SPA helpers module](framework-integration.md#optional-reactspa-helpers-module)

You can keep using existing Page Objects and call raw Selenium directly for operations Lens does not wrap.
