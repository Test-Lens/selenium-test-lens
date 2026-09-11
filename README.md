<p align="center">
  <img src="docs/assets/brand/test-lens-logo-horizontal.png" alt="Selenium Test Lens" width="720">
</p>

# Selenium Test Lens

**An observability and failure-evidence layer for Selenium WebDriver.** Test Lens works with the driver your test framework already owns: it makes interactions visible, records structured diagnostics, exposes recovery retries, and preserves useful evidence when a test fails.

| Version | Status | Availability | Documentation |
|---|---|---|---|
| `0.2.0` | Latest stable | Maven Central | [Stable documentation — 0.2.0](https://test-lens.github.io/selenium-test-lens/0.2.0/) |
| `0.3.0-SNAPSHOT` | Development | Source build only; not available from Maven Central | [Development documentation](https://test-lens.github.io/selenium-test-lens/dev/) |
| `0.1.0` | Previous release | Maven Central | [Historical documentation — 0.1.0](https://test-lens.github.io/selenium-test-lens/0.1.0/) |

Install the current stable release (Java 17 or newer):

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.2.0</version>
</dependency>
```

Keep Selenium as an explicit, consumer-owned dependency.

```java
WebDriver driver = createExistingFrameworkDriver();
TestLens lens = TestLens.attach(driver);
try {
    lens.startSession("save profile");
    driver.get(applicationUrl);
    lens.locator(By.id("save"), "Save profile").click();
    lens.locator(By.id("status"), "Save status").expect().toBeVisible();
    lens.finishPassed();
} catch (RuntimeException | Error failure) {
    lens.finishFailed(failure);
    throw failure;
} finally {
    driver.quit();
}
```

[Getting started](docs/getting-started.md) · [Why Test Lens](#why-test-lens) · [Reports](docs/observability/reports.md) · [Stable documentation](https://test-lens.github.io/selenium-test-lens/0.2.0/) · [Previous 0.1.0 documentation](https://test-lens.github.io/selenium-test-lens/0.1.0/) · [Development documentation](https://test-lens.github.io/selenium-test-lens/dev/)

## Why Test Lens

### Native interactions with visible recovery

The recommended click path is the ordinary locator API:

```java
lens.getByRole("button", "Save").click();
```

The HUD and trace describe the attempt while an optional highlight marks the target. Highlighting is visual decoration only: it does not intercept pointer events or activate the element. Each activation attempt uses native `WebElement.click()`. An intercepted click may be followed by another native click after explicit overlay recovery, and the locator retry policy may start a fresh action attempt.

There is no JavaScript-click, Selenium Actions-click, ancestor-click, or hidden state-mutation fallback. Actionability checks are best-effort diagnostics, not a guarantee that the browser will accept the click. See [element actions](docs/elements/actions.md) and [visual diagnostics](docs/observability/visual-diagnostics.md).

### Semantic and scoped queries

Release `0.2.0` extends the original test-id, text, and role-oriented locator entry points with browser-computed accessibility semantics and an immutable query pipeline:

```java
UiLocator availableCards = lens.locator(By.cssSelector(".product-card"))
        .filterByAttribute("data-status", "available")
        .filterHas(lens.getByRole("button", "Buy"));

availableCards.first()
        .locator(lens.getByRole("button", "Buy"))
        .click();
```

The browser/WebDriver computes role and accessible name; Lens does not approximate them from visible text alone. `locator(...)` searches only true descendants of the current parents, even for user XPath beginning with `//`, while `filterHas(...)` retains the parent. This avoids global selector concatenation without allowing a nested query to escape its container. See [semantic and scoped locators](docs/elements/locators.md) and [collections](docs/elements/collections.md).

### Measurable recovery instead of silent flaky passes

> A passed test can still tell you it was flaky.

Recovery retry records failed action or resolution attempts that were followed by another attempt. It is distinct from condition polling in waits and assertions, and from retrying an entire test in JUnit or TestNG. `RetrySummary` makes recovery visible; `RetryOutcomePolicy` can report it, warn, or reject an otherwise passed test after evidence has been written. See [flakiness and retry outcomes](docs/observability/flakiness.md).

### Trace, reports, and automatic failure evidence

Trace and HTML/JSON reports have been available since `0.1.0`. They form one observable lifecycle:

```text
action / wait / assertion
→ session trace
→ finalization
→ HTML and JSON report
→ failure evidence bundle
```

For a final `FAILED` outcome, the bundle can collect diagnostic and clean screenshots, trace, report, context, runtime/configuration allowlists, and a network summary. Collectors are best-effort, and finalization never closes the WebDriver. Optional page source and browser console have separate security limits. Video is an attachment supplied by the caller, not an automatic recording. Screenshots and video are not pixel-redacted. See [trace](docs/observability/trace.md), [reports](docs/observability/reports.md), and [failure bundles](docs/observability/failure-bundles.md).

```java
ScreenshotCaptureOptions fullPage = ScreenshotCaptureOptions.builder()
        .captureMode(ScreenshotCaptureMode.FULL_PAGE)
        .build();

lens.captureScreenshot("checkout-page", fullPage);
```

`VIEWPORT` remains the default. `FULL_PAGE` scrolls and stitches the current responsive layout with standard WebDriver APIs; it does not use CDP or resize the window. See [screenshots and evidence](docs/observability/screenshots-evidence.md).

After local finalization succeeds, `ReportUploader` can stream the completed failure bundle, or a temporary ZIP containing `trace.json` and `report.html`, to an authenticated HTTP endpoint. Upload is an explicit `finish -> upload -> quit` step; it never runs automatically, repeats finalization, or owns the driver. The JDK client supports direct, system-proxy, and explicit-proxy modes with literal no-proxy rules. See the [receiver contract and upload guide](docs/observability/report-upload.md).

### Safe diagnostics through central redaction

One immutable policy protects diagnostic copies before fan-out to the HUD, trace, built-in and external log sinks, reports, network diagnostics, API previews, and text files in failure bundles. Reported exception types retain the original class while messages, causes, suppressed exceptions, and stack diagnostics are redacted; the original exception still controls the test outcome.

Redaction is not pixel processing. Screenshots and video, replayable auth-state files, and unknown secret formats remain outside that guarantee; page-source and console handling is best-effort. `RedactionPolicy.disabled()` is an explicit opt-out that can expose secrets. See [sensitive-data redaction](docs/security/redaction.md).

## Advanced capabilities

- **WebDriver BiDi network diagnostics (0.2.0):** passive observation, correlation, waits, assertions, safe snapshots, and HUD filtering. It is not interception, mocking, or CDP. The manual event/wait/assertion path existed in `0.1.0`. [Network diagnostics](docs/advanced/network.md)
- **Origin-isolated auth state:** capture and restore cookies and web storage for the same validated origin. It is not automatic cross-origin SSO storage handling. [Authentication state](docs/advanced/auth-state.md)
- **Page and SPA waits:** document readiness plus an intentionally limited XHR/fetch-idle heuristic. It sees only XHR/fetch started after tracker installation—not images, CSS, scripts, WebSocket, EventSource, or beacon traffic. [Waiting](docs/elements/waiting.md)
- **JUnit 5 and TestNG lifecycle adapters:** optional `0.2.0` artifacts that map runner outcomes and own drivers created by their factories. [Framework integrations](docs/framework-integration.md)
- **React helpers:** an optional module for React-oriented waits and operations. Its legacy `smartClick` helper is not the contract of `UiLocator.click()`. [React integration](docs/integrations/react.md)
- **API overlay:** `apiCallWithModal()` visualizes caller-supplied request/response previews; it does not intercept network traffic. [API overlay and visual helpers](docs/advanced/visual-helpers.md)

Test Lens also covers routine element operations—fill, clear, key presses, checkboxes, radio buttons, selects, uploads, frames, windows, and alerts. These make the API usable without claiming to replace Selenium; use raw WebDriver whenever lower-level control is the clearer choice. See the [capability map](docs/capabilities.md).

## Installation alternatives

Gradle Kotlin DSL:

```kotlin
dependencies {
    testImplementation("io.github.test-lens:selenium-test-lens:0.2.0")
}
```

Gradle Groovy DSL:

```groovy
dependencies {
    testImplementation 'io.github.test-lens:selenium-test-lens:0.2.0'
}
```

Selenium is consumer-owned and must be declared at the version managed by your project. Java 11 is not supported. Clean-room Maven and Gradle consumers are verified on JDK 17 and 21.

## Build

```powershell
mvn clean verify
```

Real-browser integration tests are isolated in an unpublished consumer module:

```powershell
mvn -Pbrowser-it -Dbrowser=chrome -Dheaded=false verify
mvn -Pbrowser-it -Dbrowser=firefox -Dheaded=false verify
```

See [browser integration tests](docs/browser-integration-tests.md), the [changelog](CHANGELOG.md), [Maven Central](https://central.sonatype.com/artifact/io.github.test-lens/selenium-test-lens/0.2.0), and [0.2.0 Javadoc](https://javadoc.io/doc/io.github.test-lens/selenium-test-lens/0.2.0/).

## License

Licensed under the [Apache License 2.0](LICENSE).
