# Examples

These examples cover common Selenium Test Lens usage. The first sections use the `TestLens` facade; lower-level reporting and implementation APIs are grouped near the end.

## Start a Lens session

Attach Lens to the driver your project already created, then start a session for the current test:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");
```

See [Getting started](getting-started.md) for complete success, failure, and driver-cleanup handling.

## Locators and assertions

```java
lens.locator(By.id("email"), "Email").fill("test@example.com");
lens.locator(By.name("search"), "Search").fill("invoice");
lens.getByRole("button", "Save").click();
lens.getByTextContaining("Saved").expect().toBeVisible();
```

Prefer `getByTestId(...)` for critical flows when the application provides stable test IDs. See [Locators](elements/locators.md) for matching behavior and scope.

## Named steps

```java
lens.step("Save order", () -> {
    lens.getByTestId("save-order").click();
    lens.getByTestId("toast").expect().toContainText("Saved");
});
```

A named step records its name and outcome in the session trace and report.

## Configure the HUD

Use a preset to change the in-browser diagnostic HUD:

```java
OverlayConfig config = OverlayConfig.builder()
        .hudTheme(HudThemePreset.DARK)
        .hudPosition(HudPosition.TOP_RIGHT)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .overlayConfig(config)
        .build());
```

See [Configuration](configuration.md) for the available presets and custom `HudTheme` options.

## Screenshots and diagnostics

The normal lifecycle stays on the `TestLens` facade:

```java
lens.startSession("Checkout flow");

lens.step("Save order", () -> {
    lens.getByTestId("save-order").click();
});

lens.captureScreenshot("After save");
lens.finishPassed();
```

With the default `TestLensOptions`, finalization writes `report.html` and `trace.json` under a session-specific directory beneath `target/ui-test-lens`. `captureScreenshot(...)` captures through Selenium and attaches the result to the active session.

Use `finishFailed(Throwable)` when the test fails and `finishSkipped(String)` when the runner reports an aborted, assumed, or skipped test. The [framework integration guide](framework-integration.md) shows runner lifecycle patterns.

## Reporting APIs

`TestLens` handles normal per-test lifecycle and diagnostics. The trace model and exporter classes below are lower-level APIs for direct report generation or combining multiple sessions. Their default output directory is `target/ui-test-lens-report`, separate from the session-scoped `TestLens` output above.

### Per-session reports

```java
UiTestLensSession session = UiTestLensSession.start("Checkout flow");
session.finishPassed();

session.exportHtml(Path.of("target/ui-test-lens-report/checkout.html"));
session.exportJsonReport();
```

`exportHtml(Path)` writes a self-contained HTML report to the chosen path. `exportHtmlReport()` uses `target/ui-test-lens-report/index.html`; `exportJsonReport()` writes the session JSON report under `target/ui-test-lens-report` by default.

### Suite reports

```java
UiTestLensSession checkout = UiTestLensSession.start("Checkout flow");
checkout.finishPassed();

UiTestLensSession profile = UiTestLensSession.start("Profile flow");
profile.finishSkipped("Example only");

List<UiTestLensSession> sessions = List.of(checkout, profile);

new TraceHtmlExporter().exportSuiteToDefault(sessions,
        TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.AUTO)
                .build());
```

`exportSuiteToDefault(...)` writes `target/ui-test-lens-report/index.html`. Normal consumers can record the same skipped outcome through `TestLens.finishSkipped(reason)`; direct `UiTestLensSession.finishSkipped(...)` remains available to lower-level trace integrations.

### JSON and portable bundles

```java
new TraceJsonExporter().exportSuiteToDefault(sessions);
new TraceReportBundleExporter().exportSuiteToDefault(sessions);
```

The suite JSON defaults to `target/ui-test-lens-report/report.json` and uses schema version `1.0`. The ZIP defaults to `target/ui-test-lens-report/ui-test-lens-report.zip`; it contains `index.html`, `report.json`, `manifest.json`, and existing local artifacts when artifact copying is enabled. Missing artifacts are recorded in the manifest instead of failing the export.

Publish the report directory or ZIP bundle as a CI artifact using your CI system.

### Log-only report

Use the core logging API only when you need a report that is not attached to a Lens browser session:

```java
InMemoryLogSink logs = new InMemoryLogSink();
UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(logs)
        .build();

logger.info("Opening checkout");
logger.warn("Retrying slow save button");

logs.exportHtmlReport();
```

The default log-only HTML path is `target/ui-test-lens-report/index.html`. Use `logs.exportHtml(Path)` for a custom path.

The default log-only report uses the same `index.html` path as the suite report, so use `exportHtml(Path)` when generating both.

## Advanced APIs

!!! warning "Lower-level APIs"

    The following examples assume an existing `JsOverlayDebug` instance named `overlay`. These APIs are lower level than the normal `TestLens` facade and are not required for ordinary Selenium interactions or test lifecycle.

### Business assertions

Business assertion groups currently use the lower-level `JsOverlayDebug` facade:

```java
overlay.business("Order summary")
        .check("shows total", () -> overlay.getByTestId("order-total")
                .expect().toHaveText("123.00 PLN"))
        .check("contains product", () -> overlay.getByTestId("product-name")
                .expect().toContainText("Premium"))
        .verify();
```

### Auth/session state

```java
AuthState state = overlay.auth().captureState(AuthStateOptions.builder()
        .label("standard-customer")
        .role("customer")
        .build());

state.save(Path.of("target/ui-test-lens/auth/customer.json"));

AuthState restored = AuthState.load(
        Path.of("target/ui-test-lens/auth/customer.json"));

overlay.auth().restoreState(restored, AuthRestoreOptions.builder()
        .navigateToOrigin(true)
        .build());
```

!!! danger "Protect saved authentication state"

    Auth state files can contain cookies and tokens. Do not commit them.

### Passive network diagnostics

```java
overlay.network().start(NetworkDiagnosticsOptions.builder()
        .captureMode(NetworkCaptureMode.MANUAL)
        .failedStatusThreshold(400)
        .build());

overlay.network().expectResponse()
        .urlContains("/api/orders")
        .method("POST")
        .status(201)
        .within(Duration.ofSeconds(10));

overlay.network().assertNoFailedRequests();
```

The current reliable baseline is passive/manual diagnostics. Request interception and mocking are not provided by Test Lens.

## Next steps

- [Complete the getting-started flow](getting-started.md)
- [Browse the complete API reference](reference/index.md)
- [Integrate with JUnit, TestNG, or existing reporters](framework-integration.md)
- [Configure the visual overlay and HUD](configuration.md)
