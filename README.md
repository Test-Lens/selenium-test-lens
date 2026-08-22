<p align="center">
  <img src="docs/assets/brand/test-lens-logo-horizontal.png" alt="Test Lens logo" width="760">
</p>

# Selenium Test Lens

Attach the public facade to the WebDriver your framework already owns:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("login");
lens.locator(By.id("login"), "Login").click();
```

See [framework integration](docs/framework-integration.md) for JUnit 5, TestNG, parallel execution, and Allure coexistence.

## Supported native Selenium operations

The public `TestLens` / `UiLocator` API covers click, fill, clear, key presses, retryable waits and assertions, reads, collections (`count`, `resolveAll`, `nth`, `first`, `last`), HTML select, hover/double/right click, frames, deterministic window/tab switching, and native JavaScript dialogs.

```java
lens.switchToFrame(PAYMENT_FRAME, "Payment frame");
lens.switchToDefaultContent();

Set<String> before = lens.windowHandles();
lens.locator(OPEN_PAYMENT, "Open payment").click();
lens.switchToNewWindow(before, "Payment");

lens.locator(COUNTRY, "Country").selectByVisibleText("Poland");
lens.locator(USER_MENU, "User menu").hover();
lens.locator(ROW, "Result row").doubleClick();
lens.locator(ITEM, "Context item").rightClick();

String message = lens.alert().waitUntilPresent().text();
lens.alert().accept();
```

Advanced pointer sequences, low-level W3C actions, complex select/multi-select flows, and application-specific browser management can continue to use raw Selenium alongside Lens.

Selenium Test Lens is an observability, diagnostics, and reporting toolkit for Selenium/WebDriver UI tests. It adds resilient WebDriver actions, retryable assertions, visual debugging, evidence capture, auth/session state helpers, network diagnostics, offline HTML reports, machine-readable JSON reports, and portable report ZIP bundles.

The first public release is `0.1.0`. As a pre-1.0 library, its API may evolve between minor releases.

Repository: https://github.com/Test-Lens/selenium-test-lens

Maven coordinates use `io.github.testlens`, module artifactIds use the `selenium-test-lens-*` naming scheme, and public Java packages live under `io.github.testlens`. Default report and evidence output paths still use `target/ui-test-lens...` for compatibility with existing local and CI artifacts.

## Requirements

- Java 17
- Maven 3.x
- Selenium supplied by the consuming test project; release validation is performed with Selenium 4.39.0

## Quick Start

All-in-one dependency:

```xml
<dependency>
    <groupId>io.github.testlens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

The all-in-one POM deliberately does not choose a Selenium version for the consumer. Add the Selenium version owned by your existing framework, for example the version used for 0.1.0 validation:

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.39.0</version>
</dependency>
```

Minimal usage:

```java
import io.github.testlens.TestLens;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

WebDriver driver = createExistingFrameworkDriver();
TestLens lens = TestLens.attach(driver);
lens.startSession("example");

driver.get("https://example.test/login");
lens.locator(By.id("username"), "Username").fill("john");
lens.locator(By.id("login"), "Login").click();
lens.locator(By.id("welcome"), "Welcome").expect().toBeVisible();
lens.finishPassed();
```

## Build

Recommended checks:

```powershell
mvn -q test
mvn -q -DskipTests compile
```

Module checks:

```powershell
mvn -q -pl selenium-test-lens-core test
mvn -q -pl selenium-test-lens-overlay -am test
mvn -q -pl selenium-test-lens-selenium -am test
mvn -q -pl selenium-test-lens-react -am test
mvn -q -pl selenium-test-lens-examples -am test
mvn -q -pl selenium-test-lens -am test
```

Consumer demo command:

```powershell
mvn test "-Dheaded=true" "-Dlens.theme=GLASS" "-Dlens.report.theme=LIGHT"
```

## Reports

<p align="center">
  <img src="docs/assets/brand/test-lens-badge.png" alt="Test Lens badge" width="420">
</p>

HTML is the human-readable report. JSON is the machine-readable integration format. ZIP bundles package the static HTML, JSON, manifest, and copied artifacts for CI artifacts or API uploads.

Default output paths:

```text
target/ui-test-lens-report/index.html
target/ui-test-lens-report/report.json
target/ui-test-lens-report/ui-test-lens-report.zip
```

```java
UiTestLensSession checkout = lens.startSession("Checkout flow");
checkout.exportHtml(Path.of("target/ui-test-lens-report/checkout-flow.html"));
checkout.exportJsonReport();

new TraceHtmlExporter().exportSuiteToDefault(List.of(checkout),
        TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.AUTO)
                .build());

new TraceJsonExporter().exportSuiteToDefault(List.of(checkout));
new TraceReportBundleExporter().exportSuiteToDefault(List.of(checkout));
```

Report themes are independent from HUD themes: `HtmlReportTheme.LIGHT`, `DARK`, and `AUTO`.

The JSON schema uses `schemaVersion` `1.0`. ZIP bundles never store absolute artifact paths or `..` entries; missing artifact files are listed in `manifest.json` warnings instead of failing export.

Example upload command:

```powershell
curl -F "report=@target/ui-test-lens-report/ui-test-lens-report.zip" https://example.com/api/reports
```

## Modules

| Module | Responsibility |
|---|---|
| `selenium-test-lens-core` | Internal logging, trace/evidence model, JSON/HTML/ZIP report exporters. No Selenium dependency. |
| `selenium-test-lens-overlay` | Runtime JavaScript overlay resources, HUD configuration, visual debugging assets. No Selenium dependency. |
| `selenium-test-lens-selenium` | Selenium facade, locators, assertions, actionability, evidence, auth/session state, network diagnostics. |
| `selenium-test-lens-react` | React-specific support layered on top of Selenium integration. |
| `selenium-test-lens` | All-in-one POM dependency bundle. |
| `selenium-test-lens-examples` | Documentation and compile-check examples. Not intended as a runtime dependency. |

## Documentation

- [Getting started](docs/getting-started.md)
- [Configuration](docs/configuration.md)
- [Visual overlay and HUD](docs/visual-overlay-hud.md)
- [API reference](docs/api-reference.md)
- [Examples](docs/examples.md)
- [Architecture](docs/architecture.md)
- [Migration](docs/migration.md)
- [Release verification](docs/release.md)
- [Roadmap](docs/roadmap.md)

## License

Selenium Test Lens is licensed under the [Apache License 2.0](LICENSE).

## Current Scope

Selenium Test Lens runs on top of Selenium/WebDriver and adds diagnostic APIs:

- visual HUD and element debugging
- configurable blocking overlay policy
- actionability checks
- ergonomic `UiLocator` helpers
- retryable web assertions
- business assertions and named steps
- trace/evidence session model, polished HTML reports, JSON reports, and portable ZIP bundles
- screenshot capture and video attachments
- auth/session state capture and restore
- passive network diagnostics and wait-for-response assertions

Current limitations:

- `getByRole` does not implement the full ARIA accessible-name algorithm.
- network diagnostics are passive; browser capture providers and interception/mocking are not implemented.
- video support is attachment/reference based; Selenium Test Lens does not record video.
- HUD themes focus on the HUD panel; Wait HUD and assertion badges are not fully covered by the common theme system.

