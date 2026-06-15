# Examples

This page collects short usage snippets. Browser-dependent examples in `ui-test-lens-examples` are disabled and documentation-only unless a real application and `WebDriver` are supplied.

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

See also `HudThemeExampleTest` in `ui-test-lens-examples`.

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

## HTML trace and evidence report

```java
UiTestLensSession session = overlay.startSession("Checkout flow");

overlay.step("Save order", () -> {
    overlay.getByTestId("save-order").click();
});

overlay.captureScreenshot("After save");
overlay.attachVideoUrl("CI video", "https://ci.example.com/artifacts/checkout-flow.mp4");
session.exportHtmlReport();
```

`exportHtmlReport()` writes `target/ui-test-lens-report/index.html` by default. The report is a self-contained dark HTML file with inline CSS, summary cards, failure diagnostics, timeline rows, metadata details, and artifact links. Publish the `target/ui-test-lens-report` folder as a CI artifact to inspect it after a run.

Screenshot capture uses Selenium `TakesScreenshot`. Video support attaches existing files or URLs; UI Test Lens does not record video. Local image artifacts are linked and previewed when present; missing files are shown as warnings instead of failing report generation.

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
