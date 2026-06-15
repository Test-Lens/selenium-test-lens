# API reference

This is a concise public API map for UI Test Lens 0.1.

## Overlay configuration

### OverlayConfig

Main visual overlay configuration.

Common builder methods:

```java
OverlayConfig.builder()
        .enabled(true)
        .showHudPanel(true)
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudOffset(16, 16)
        .hudMaxWidthPx(320)
        .hudTheme(HudThemePreset.GLASS)
        .hudTheme(customTheme)
        .highlightColor("#38bdf8")
        .decorationDurationMs(1500)
        .build();
```

### HudPosition

Controls HUD placement. Use the enum values exposed by `HudPosition`, such as `TOP_RIGHT` or `BOTTOM_RIGHT`.

### HudThemePreset

Built-in HUD presets:

- `DEFAULT`
- `DARK`
- `LIGHT`
- `GLASS`
- `COMPACT`
- `HIGH_CONTRAST`
- `BLACK_AND_COLORS`
- `MINIMAL`

### HudTheme

Immutable custom HUD theme model.

```java
HudTheme.builder()
        .background("rgba(15, 23, 42, 0.92)")
        .foreground("#f8fafc")
        .accent("#38bdf8")
        .borderRadiusPx(16)
        .fontFamily("Inter, system-ui, sans-serif")
        .maxHeightPx(420)
        .build();
```

## Selenium facade

### JsOverlayDebug

Main Selenium entry point.

Common constructors:

```java
new JsOverlayDebug(driver);
new JsOverlayDebug(driver, overlayConfig);
```

Core methods:

```java
overlay.setOverlayPolicy(policy);

overlay.locator(By.cssSelector("[data-testid='save']")).click();
overlay.getByTestId("save").click();
overlay.getByText("Save").click();
overlay.getByTextContaining("Saved");
overlay.getByLabel("Email").fill("test@example.com");
overlay.getByPlaceholder("Search").fill("invoice");
overlay.getByRole("button", "Save").click();

overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");

overlay.business("Order summary")
        .check("shows total", () -> overlay.getByTestId("total").expect().toHaveText("123.00 PLN"))
        .verify();

overlay.step("Save order", () -> {
    overlay.getByTestId("save").click();
});

overlay.setStep("Save order");
overlay.hudLog("info", "Save clicked", "local");
```

Trace and evidence:

```java
UiTestLensSession session = overlay.startSession("Checkout flow");
overlay.captureScreenshot("After save");
overlay.attachVideoFile("Grid video", Path.of("target/videos/checkout.mp4"));
overlay.attachVideoUrl("CI video", "https://ci.example.com/artifacts/video.mp4");
overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout.html"));
session.exportHtmlReport();
```

Auth/session state:

```java
AuthState state = overlay.auth().captureState(AuthStateOptions.builder()
        .label("standard-customer")
        .role("customer")
        .build());

overlay.auth().restoreState(state, AuthRestoreOptions.builder()
        .navigateToOrigin(true)
        .build());
```

Network diagnostics:

```java
overlay.network().start(NetworkDiagnosticsOptions.builder()
        .captureMode(NetworkCaptureMode.MANUAL)
        .failedStatusThreshold(400)
        .build());

overlay.network().expectResponse()
        .urlContains("/api/orders")
        .status(201)
        .within(Duration.ofSeconds(10));

overlay.network().assertNoFailedRequests();
```

## Locators

`UiLocator` resolves elements freshly and retries stale/intercepted/not-interactable Selenium failures according to `UiLocatorOptions`.

Supported helper entry points include:

- `locator(By)`
- `getByTestId(String)`
- `getByText(String)`
- `getByTextContaining(String)`
- `getByLabel(String)`
- `getByPlaceholder(String)`
- `getByRole(String)`
- `getByRole(String, String)`

These helpers return `UiLocator`, so they share retry, actionability and assertion behavior.

## Assertions

`UiExpect` provides retryable web assertions:

- visible / hidden
- enabled / disabled
- exact text
- contains text
- exact value
- contains value

`UiLocator.expect()` is the fluent locator-local form.

## Business assertions and steps

`BusinessAssertions` groups checks under a business subject and can collect multiple failures before throwing a readable `BusinessAssertionError`.

`UiStep` and `JsOverlayDebug.step(...)` wrap named test steps with status, timing, HUD logging and optional screenshot-on-failure.

## Trace and reports

`UiTestLensSession` stores trace metadata, events, failures and artifacts. It can export JSON or polished single-file HTML through `TraceJsonExporter` and `TraceHtmlExporter`.

Common HTML report methods:

```java
session.exportHtml();
session.exportHtml(Path.of("target/ui-test-lens-report/checkout.html"));
session.exportHtmlReport();
new TraceHtmlExporter().exportToDefault(session);
new TraceHtmlExporter().exportSuiteToDefault(List.of(session));
```

`exportHtmlReport()`, `TraceHtmlExporter.exportToDefault(...)`, and `TraceHtmlExporter.exportSuiteToDefault(...)` write `target/ui-test-lens-report/index.html`, creating parent directories and replacing an existing file. Use explicit paths for per-test files and reserve `index.html` for a combined run report.

Report theme options:

```java
TraceHtmlExportOptions options = TraceHtmlExportOptions.builder()
        .theme(HtmlReportTheme.LIGHT)
        .build();

new TraceHtmlExporter().exportSuiteToDefault(sessions, options);
```

`HtmlReportTheme` values are `LIGHT`, `DARK`, and `AUTO`. `AUTO` uses CSS `prefers-color-scheme` and is the default.

`TraceLogSink` maps UI Test Lens logger events into trace sessions when a session is attached.

For log-only reports, collect entries in `InMemoryLogSink`:

```java
InMemoryLogSink logs = new InMemoryLogSink();
logs.accept(UiTestLensLogEntry.info("Opening checkout"));
logs.exportHtmlReport();
```

## React support

React-specific helpers live in `ui-test-lens-react`, not in the Selenium module. `ui-test-lens-selenium` does not depend on React.
