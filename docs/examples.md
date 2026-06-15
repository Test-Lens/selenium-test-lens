# Examples

This page collects short usage snippets. Selenium/WebDriver-dependent examples in `selenium-test-lens-examples` are disabled and documentation-only unless a real application and `WebDriver` are supplied.

## Default HUD

```java
JsOverlayDebug overlay = new JsOverlayDebug(driver);

overlay.setStep("Checkout");
overlay.hudLog("info", "Default HUD theme", "local");
```

## HUD theme presets

```java
OverlayConfig dark = OverlayConfig.builder()
        .hudTheme(HudThemePreset.DARK)
        .build();

OverlayConfig light = OverlayConfig.builder()
        .hudTheme(HudThemePreset.LIGHT)
        .build();

OverlayConfig glass = OverlayConfig.builder()
        .hudTheme(HudThemePreset.GLASS)
        .build();

OverlayConfig compact = OverlayConfig.builder()
        .hudTheme(HudThemePreset.COMPACT)
        .build();

OverlayConfig highContrast = OverlayConfig.builder()
        .hudTheme(HudThemePreset.HIGH_CONTRAST)
        .build();

OverlayConfig blackAndColors = OverlayConfig.builder()
        .hudTheme(HudThemePreset.BLACK_AND_COLORS)
        .build();

OverlayConfig minimal = OverlayConfig.builder()
        .hudTheme(HudThemePreset.MINIMAL)
        .build();
```

## Custom HUD theme

```java
HudTheme customTheme = HudTheme.builder()
        .background("rgba(15, 23, 42, 0.92)")
        .foreground("#f8fafc")
        .accent("#38bdf8")
        .borderColor("rgba(148, 163, 184, 0.35)")
        .borderRadiusPx(16)
        .fontFamily("Inter, system-ui, sans-serif")
        .fontSizePx(13)
        .boxShadow("0 18px 45px rgba(15, 23, 42, 0.35)")
        .maxHeightPx(420)
        .build();

OverlayConfig config = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(customTheme)
        .build();

JsOverlayDebug overlay = new JsOverlayDebug(driver, config);
```

See also `HudThemeExampleTest` in `selenium-test-lens-examples`.

## Locator helpers

```java
overlay.getByLabel("Email").fill("test@example.com");
overlay.getByPlaceholder("Search").fill("invoice");
overlay.getByRole("button", "Save").click();

overlay.expect(overlay.getByTextContaining("Saved"))
        .toBeVisible();
```

For critical flows, prefer `getByTestId(...)` when the application provides stable test IDs.

## Business assertions

```java
overlay.business("Order summary")
        .check("shows total", () -> overlay.getByTestId("order-total").expect().toHaveText("123.00 PLN"))
        .check("contains product", () -> overlay.getByTestId("product-name").expect().toContainText("Premium"))
        .verify();
```

## Named steps

```java
overlay.step("Save order", () -> {
    overlay.getByTestId("save-order").click();
    overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
});
```

## Per-test HTML trace and evidence report

```java
UiTestLensSession session = overlay.startSession("Checkout flow");

overlay.step("Save order", () -> {
    overlay.getByTestId("save-order").click();
});

overlay.captureScreenshot("After save");
overlay.attachVideoUrl("CI video", "https://ci.example.com/artifacts/checkout-flow.mp4");
session.exportHtmlReport();
```

`exportHtmlReport()` writes `target/ui-test-lens-report/index.html` by default. For one file per test, pass an explicit path such as `target/ui-test-lens-report/checkout-flow.html`.

The report is a self-contained HTML file with inline CSS, summary cards, failure diagnostics, timeline rows, metadata details, and artifact links. Publish the `target/ui-test-lens-report` folder as a CI artifact to inspect it after a run.

Screenshot capture uses Selenium `TakesScreenshot`. Video support attaches existing files or URLs; Selenium Test Lens does not record video. Local image artifacts are linked and previewed when present; missing files are shown as warnings instead of failing report generation.

## Log-only HTML report

```java
InMemoryLogSink logs = new InMemoryLogSink();
UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(logs)
        .build();

logger.info("Opening checkout");
logger.warn("Retrying slow save button");

logs.exportHtmlReport();
```

For custom output paths, use `session.exportHtml(Path.of("target/ui-test-lens-report/checkout.html"))` or `logs.exportHtml(Path.of("target/ui-test-lens-report/logs.html"))`.

## Suite HTML report

```java
UiTestLensSession checkout = UiTestLensSession.start("Checkout flow");
checkout.finishPassed();

UiTestLensSession profile = UiTestLensSession.start("Profile flow");
profile.finishSkipped("Example only");

new TraceHtmlExporter().exportSuiteToDefault(List.of(checkout, profile),
        TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.AUTO)
                .build());
```

`exportSuiteToDefault(...)` writes the combined run report to `target/ui-test-lens-report/index.html`. It keeps per-test reports optional and adds a suite summary, test table, failure rollup, and anchors to each test/session section.

Report themes:

- `HtmlReportTheme.AUTO` follows the viewer's system color preference.
- `HtmlReportTheme.LIGHT` uses a white/off-white report.
- `HtmlReportTheme.DARK` uses the HUD-like dark report.

Consumer demos can expose these as Maven properties, for example:

```powershell
mvn test
mvn test "-Dheaded=true" "-Dlens.theme=GLASS" "-Dlens.report.theme=LIGHT"
mvn test "-Dheaded=true" "-Dlens.theme=DARK" "-Dlens.report.theme=AUTO"
```

HUD theme and report theme are independent settings.

## JSON reports and portable bundles

```java
UiTestLensSession checkout = UiTestLensSession.start("Checkout flow");
checkout.finishPassed();

UiTestLensSession profile = UiTestLensSession.start("Profile flow");
profile.finishSkipped("Example only");

new TraceJsonExporter().exportSuiteToDefault(List.of(checkout, profile));
new TraceReportBundleExporter().exportSuiteToDefault(List.of(checkout, profile));
```

JSON reports are intended for machines and integrations. The default suite JSON path is `target/ui-test-lens-report/report.json`, with `schemaVersion` set to `1.0`. Per-session JSON can be written with `session.exportJsonReport()` or `session.exportJson(Path.of("target/ui-test-lens-report/checkout-flow.json"))`.

The portable ZIP bundle defaults to `target/ui-test-lens-report/ui-test-lens-report.zip`. It contains `index.html`, `report.json`, `manifest.json`, and existing local artifacts copied under safe relative `artifacts/...` entries. Missing artifacts do not fail export; they are listed in the manifest warnings. ZIP entries never include absolute paths or `..`.

For CI, publish the report folder or upload the bundle:

```powershell
curl -F "report=@target/ui-test-lens-report/ui-test-lens-report.zip" https://example.com/api/reports
```

## Auth/session state

```java
AuthState state = overlay.auth().captureState(AuthStateOptions.builder()
        .label("standard-customer")
        .role("customer")
        .build());

state.save(Path.of("target/ui-test-lens/auth/customer.json"));

AuthState restored = AuthState.load(Path.of("target/ui-test-lens/auth/customer.json"));

overlay.auth().restoreState(restored, AuthRestoreOptions.builder()
        .navigateToOrigin(true)
        .build());
```

Auth state files can contain cookies and tokens. Do not commit them.

## Passive network diagnostics

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

The reliable baseline is manual/fallback diagnostics. Browser network providers and interception/mocking are not implemented.
