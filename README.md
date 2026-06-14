# UI Test Lens

Visual observability and debug layer for UI/browser automation tests.

## Status

This project is currently being migrated from an internal Selenium helper project into UI Test Lens. The artifact name, Java package namespace, and Maven coordinates now use UI Test Lens naming.

Current state:

- Maven artifactId is `ui-test-lens`.
- Maven groupId is `io.github.mmaciekk111`.
- Java packages use `io.github.mmaciekk111.uitestlens`.
- The project is now a Maven multi-module project with `ui-test-lens-core`, `ui-test-lens-overlay`, `ui-test-lens-selenium`, `ui-test-lens-react`, `ui-test-lens-examples`, and the all-in-one compatibility artifact `ui-test-lens`.
- Current project version is `0.1.0-SNAPSHOT`.
- `ui-test-lens-examples` contains compile-checked examples and is not intended as a runtime dependency.
- Runtime JavaScript state is initialized under `window.__uiTestLens`; legacy `window.__selenium...` globals remain as compatibility aliases.

## What It Does

UI Test Lens helps make browser automation tests easier to observe and debug. It currently focuses on Selenium-based tests and uses Selenium `JavascriptExecutor` to inject visual debugging helpers into the tested page.

It can:

- inject a debug overlay/HUD into the page,
- show the current test step,
- append log messages to the HUD,
- highlight elements interacted with by the test,
- instrument click/type/scroll/wait/assertion flows,
- emit structured events through the UI Test Lens logger/event-bus,
- collect events in memory,
- export collected logs as text, JSON, or HTML.

This repository does not yet promise a stable final public API, Maven Central publication, Playwright/Cypress support, a ready Selenide adapter, or full Allure/TeamCity integration.

UI Test Lens is planned to evolve toward Playwright-like reliability on top of Selenium: configurable overlay handling, actionability checks, retryable locators/assertions, trace reports, network diagnostics and auth/session state reuse.

## Versioning

UI Test Lens is currently pre-1.0. Public APIs may still change between 0.x releases.

The current local development version is `0.1.0-SNAPSHOT`. Maven Central publication is not configured yet; use `mvn install` for local consumption.

## Current Capabilities

- Selenium overlay/HUD.
- Element highlight and visual decorations.
- Smart click/type helpers.
- Wait HUD and wait event logging.
- Visual assertions/checks.
- React-safe helper methods.
- API overlay panel without a RestAssured dependency.
- Event logging model:
  - `UiTestLensLogger`,
  - `UiTestLensLogEntry`,
  - `UiTestLensLogSink`,
  - `TargetDescriptor`.
- Log sinks:
  - `InMemoryLogSink`,
  - `ConsoleLogSink`,
  - `ConsumerLogSink`.
- Log exporters:
  - plain text,
  - JSON,
  - HTML.

`ConsoleLogSink` is opt-in. Input values and file paths should not be logged as full values in event metadata; current action instrumentation records lengths such as `valueLength` or `pathLength`.

## Installation

The project is not published yet. Install it locally:

```powershell
mvn install
```

Use the current local coordinates:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

For the neutral logging/export model only, depend on the core module:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

For browser runtime resources and overlay bridge classes only, depend on the overlay module:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens-overlay</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

For Selenium-specific facade/actions directly, depend on the Selenium module:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens-selenium</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

For React-safe helpers directly, depend on the React module:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens-react</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

The examples module is part of this repository for compile-checked usage samples. Application projects normally should not depend on it at runtime.

React helpers are now accessed from the React module instead of the Selenium facade:

```java
import io.github.mmaciekk111.uitestlens.react.ReactSafeExecutor;
import io.github.mmaciekk111.uitestlens.react.ReactSupport;

ReactSafeExecutor react = ReactSupport.reactSafe(overlay);
ReactSupport.smartClick(overlay, By.cssSelector("[data-testid='save']"), "SAVE");
```

### Module Dependency Matrix

| Use case | Maven artifact |
| -------- | -------------- |
| Event logging/export model only | `io.github.mmaciekk111:ui-test-lens-core:0.1.0-SNAPSHOT` |
| Browser overlay runtime bridge only | `io.github.mmaciekk111:ui-test-lens-overlay:0.1.0-SNAPSHOT` |
| Selenium tests with overlay/actions/waits | `io.github.mmaciekk111:ui-test-lens-selenium:0.1.0-SNAPSHOT` |
| React-safe Selenium helpers | `io.github.mmaciekk111:ui-test-lens-react:0.1.0-SNAPSHOT` |
| Simple all-in-one local usage | `io.github.mmaciekk111:ui-test-lens:0.1.0-SNAPSHOT` |

`ui-test-lens` is intentionally kept as an all-in-one compatibility artifact. Current module stabilization notes are tracked in [`docs/ui-test-lens-module-stabilization-report.md`](docs/ui-test-lens-module-stabilization-report.md).

## Minimal Selenium Usage

The current API still exposes low-level constructor dependencies. The example below matches the current codebase.

```java
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.api.ApiCallActions;
import io.github.mmaciekk111.uitestlens.api.ApiOverlayPanel;
import io.github.mmaciekk111.uitestlens.core.Guards;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.selenium.SeleniumOverlayFactory;

WebDriver driver = /* existing Selenium driver */;

OverlayConfig config = OverlayConfig.builder()
        .showHudPanel(true)
        .highlightColor("#ffeb3b")
        .build();

OverlayRootManager rootManager = SeleniumOverlayFactory.overlayRoot(driver, config);
ApiOverlayPanel apiPanel = SeleniumOverlayFactory.apiOverlayPanel(driver, rootManager, config);
ApiCallActions apiCalls = new ApiCallActions(apiPanel);
Guards guards = new Guards(driver);

JsOverlayDebug lens = new JsOverlayDebug(
        driver,
        config,
        apiPanel,
        apiCalls,
        guards
);

lens.initHud("Checkout test", "local");
lens.setStep("Open checkout");
lens.hudLog("info", "Opening checkout page", "local");

WebElement button = driver.findElement(By.id("submit"));
lens.highlightElement(button, "Submit");
lens.smartClickWithOverlayHandler(button, "Submit");

lens.clearDebugArtifacts();
```

## Blocking Overlay Policy

Known popups and blocking overlays can be configured in the Selenium module with `OverlayPolicy`. This is intended for predictable UI blockers such as cookie banners, newsletter modals, session-expired dialogs, and focus-lock overlays.

Handlers define how to detect the overlay, which actions to run, whether the overlay is optional or fatal, and whether it must disappear after handling. Smart click uses the configured policy before clicking and once more after a click interception before falling back to the legacy blocking overlay heuristics.

```java
OverlayPolicy policy = OverlayPolicy.builder()
        .handler(OverlayHandler.builder("Cookie consent")
                .detect(By.cssSelector("[data-testid='cookie-banner']"))
                .action(OverlayAction.click(By.cssSelector("[data-testid='accept-cookies']")))
                .optional(true)
                .build())
        .handler(OverlayHandler.builder("Session expired")
                .detect(By.cssSelector("[data-testid='session-expired']"))
                .action(OverlayAction.fail("Session expired popup detected"))
                .optional(false)
                .build())
        .build();

JsOverlayDebug overlay = new JsOverlayDebug(driver);
overlay.setOverlayPolicy(policy);
```

`OverlayAction.fail(...)` marks the handler as fatal and prevents the click from continuing. The policy executor emits overlay policy events into the existing logger/event model. Actionability checks and future React-aware readiness reuse the same policy executor.

## Actionability Checks

UI Test Lens now includes a first Selenium-side actionability layer inspired by Playwright reliability checks. The initial implementation lives in `ui-test-lens-selenium` and checks whether a target element is attached, visible, enabled, stable, scrolled into view, receiving its center click point, and not blocked by configured overlay policy.

```java
ActionabilityOptions options = ActionabilityOptions.builder()
        .timeout(Duration.ofSeconds(5))
        .checkStableBounds(true)
        .checkReceivesClickPoint(true)
        .build();

ActionabilityReport report = overlay.checkActionability(
        By.cssSelector("[data-testid='save']"),
        options
);

if (!report.isReady()) {
    throw new AssertionError(report.summary());
}
```

`SmartClickActions` runs the checker as a best-effort diagnostic before the existing click flow. It preserves legacy fallback behavior and reuses `OverlayPolicyExecutor` for known blocking overlays.

## React-aware Actionability Checks

The React module extends the Selenium actionability report with React-specific readiness signals. `ReactSupport.checkActionability(...)` first calls the base `JsOverlayDebug.checkActionability(...)`, then checks common React loading/blocking signals such as `aria-disabled`, `aria-busy`, `data-loading`, `data-pending`, `data-state`, progress bars, spinners, skeletons, focus-lock overlays, dialogs, modals, and custom busy/blocking locators.

```java
ReactActionabilityOptions options = ReactActionabilityOptions.builder()
        .checkAriaBusy(true)
        .checkDataLoading(true)
        .checkSpinner(true)
        .checkSkeleton(true)
        .build();

ReactActionabilityReport report = ReactSupport.checkActionability(
        overlay,
        By.cssSelector("[data-testid='save']"),
        options
);

if (!report.isReady()) {
    throw new AssertionError(report.summary());
}
```

This stays in `ui-test-lens-react`; `ui-test-lens-selenium` still has no dependency on React. It is not a full retryable locator API yet.

## Retryable UI Locator API

The Selenium module now includes a first Playwright-like locator wrapper. `UiLocator` stores a `By` locator and description rather than keeping a long-lived `WebElement`. Each action resolves a fresh element just before use, runs base actionability diagnostics, and retries transient stale/intercept/not-interactable failures within the configured limit.

```java
overlay.locator(By.cssSelector("[data-testid='save']")).click();

overlay.getByTestId("email").fill("test@example.com");
overlay.getByTestId("save-button").click();

String toast = overlay.getByTestId("toast").textContent();
boolean visible = overlay.getByTestId("modal").isVisible();
```

Initial methods include `locator(By)`, `getByTestId(...)`, `click`, `fill`, `clear`, `pressEnter`, `textContent`, `isVisible`, `isEnabled`, `resolve`, and `checkActionability`. Click delegates to the existing smart click/overlay policy path; fill and reads resolve fresh elements directly. Richer `getByRole`/`getByLabel`/`getByText` helpers are planned later.

## Retryable Web Assertions

`UiExpect` adds Playwright-like web assertions on top of `UiLocator`. Assertions resolve a fresh element on every attempt and retry until the configured timeout, which reduces flaky checks after Selenium actions, React rerenders, and delayed UI updates.

```java
overlay.expect(By.cssSelector("[data-testid='toast']"))
        .toHaveText("Saved");

overlay.expect(overlay.getByTestId("save-button"))
        .toBeEnabled();

overlay.expect(overlay.getByTestId("modal"))
        .toBeVisible();

overlay.expect(overlay.getByTestId("toast"))
        .toContainText("Saved");
```

The initial assertion set covers visible/hidden, enabled/disabled, exact text, contains text, value, and contains value. Failures include the locator description, expected/actual previews, attempts, elapsed time, and a reason. Value assertions intentionally log value lengths instead of full input values. Existing `AssertActions` and grouped visual assertions remain available for the older assertion flows.

## Business Assertions

`BusinessAssertions` is a lightweight business-level grouping DSL over retryable `UiExpect` checks. It does not add domain methods such as `shouldShowAmount` to the library; those belong in the consuming test project or an adapter. The library provides the grouping, failure collection, logging events, and readable summary.

```java
overlay.business("Order summary")
        .check("shows total amount", () -> {
            overlay.getByTestId("order-total").expect().toHaveText("123.00 PLN");
        })
        .check("contains premium product", () -> {
            overlay.getByTestId("product-name").expect().toContainText("Premium");
        })
        .verify();
```

By default, `verify()` runs all registered checks, collects failures, and throws one `BusinessAssertionError` with a summary. `BusinessAssertionOptions` can switch to fail-fast behavior. Use `UiExpect` for technical web assertions, `BusinessAssertions` for readable grouped business checks, and existing `AssertActions` for the older visual/grouped assertion layer.

## Business Step DSL

`step(...)` executes a named test/business step, measures its duration, emits step events, and optionally writes the current step to the HUD. It is different from `setStep(...)`: `setStep(...)` only updates the current HUD label, while `step(...)` runs code and returns a `UiStepResult`.

```java
overlay.step("Fill checkout form", () -> {
    overlay.getByTestId("email").fill("test@example.com");
    overlay.getByTestId("save-button").click();
});

overlay.step("Verify order summary", () -> {
    overlay.business("Order summary")
            .check("shows total amount", () -> {
                overlay.getByTestId("order-total").expect().toHaveText("123.00 PLN");
            })
            .check("contains premium product", () -> {
                overlay.getByTestId("product-name").expect().toContainText("Premium");
            })
            .verify();
});
```

With default `UiStepOptions`, failed steps throw `UiStepError` and preserve the original cause. With `failFast(false)`, failed steps return a `FAILED` result without throwing. The step model stores name, status, start/end time, elapsed duration, and failure summary as a foundation for future HTML trace reports and screenshot/video markers.

## Trace Evidence Model

`ui-test-lens-core` now includes a Selenium-free trace/evidence model. `UiTestLensSession` stores session metadata, timeline events, failures, and artifact references. Artifacts are references only: screenshots, videos, logs, JSON exports, custom files, and URLs can be attached as paths or links without reading or capturing the files.

```java
UiTestLensSession session = overlay.startSession("Checkout flow");

overlay.step("Save form", () -> {
    overlay.getByTestId("save-button").click();
    overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
});

overlay.attachScreenshot("Save form", Path.of("target/screenshots/save-form.png"));
overlay.attachVideoFile("Checkout flow video", Path.of("target/videos/checkout-flow.mp4"));
overlay.attachVideoUrl("CI video", "https://ci.example.com/artifacts/checkout-flow.mp4");

String json = session.exportJson();
```

`TraceJsonExporter` produces a JSON-friendly structure using only JDK code. `TraceLogSink` can forward existing UI Test Lens log entries into a trace session. Selenium integration is intentionally light in this stage: `JsOverlayDebug` can start or attach a session, step events are added to the session, and artifacts can be attached through convenience methods.

Selenium-side screenshot capture is available through `JsOverlayDebug.captureScreenshot(...)`. Video evidence can be attached as local file paths or remote URLs. Video recording and provider-specific downloads are not implemented yet.

## HTML Trace Report Exporter

`TraceHtmlExporter` turns a `UiTestLensSession` into a self-contained HTML report with inline CSS. The report includes session metadata, final status, summary cards, timeline events, step events, failures, artifact links, and optional raw JSON.

```java
Path report = overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
```

The exporter lives in `ui-test-lens-core` and has no Selenium dependency. `UiTestLensSession` exposes `exportHtml(...)` convenience methods, while `JsOverlayDebug` delegates to the attached session. Screenshot and video artifacts are shown as paths/links; the exporter does not embed binary files and does not capture screenshots or record video.

## Screenshot Evidence Capture

The Selenium module can capture screenshots through Selenium `TakesScreenshot`, write them to `target/ui-test-lens/screenshots`, and attach them to the active `UiTestLensSession` as `TraceArtifactType.SCREENSHOT`.

```java
overlay.captureScreenshot("After save");
```

Screenshot output is configured with `ScreenshotCaptureOptions`, including output directory, file name prefix, timestamp usage, overwrite behavior, and whether to attach to the session. `UiStepOptions.captureScreenshotOnFailure(true)` enables opt-in screenshot capture for failed steps; it is disabled by default to avoid changing existing test behavior.

Captured screenshots appear in the HTML trace report as artifact links/paths. UI Test Lens does not embed binary data in the report and does not implement video recording in this stage.

## Video Evidence Attachments

Video evidence is modeled as an attachment/reference, not as built-in recording. Use `attachVideoFile(...)` for recordings produced by Selenium Grid, Docker Selenium, Selenoid, BrowserStack, Sauce Labs, or a custom runner, and `attachVideoUrl(...)` for CI artifact links.

```java
overlay.attachVideoFile(
        "Selenium Grid recording",
        Path.of("target/videos/checkout-flow.mp4"),
        VideoEvidenceOptions.builder()
                .source(VideoEvidenceSource.SELENIUM_GRID)
                .metadata("provider", "Docker Selenium")
                .build()
);

overlay.attachVideoUrl(
        "CI video artifact",
        "https://ci.example.com/artifacts/checkout-flow.mp4",
        VideoEvidenceOptions.builder()
                .source(VideoEvidenceSource.CI_ARTIFACT)
                .metadata("job", "checkout-ui-tests")
                .build()
);
```

`VideoEvidenceOptions` can label the source, media type, provider/build/job metadata, and whether a local file must exist. URLs are not fetched or validated over the network. Attached video artifacts appear in the HTML trace report as paths or links.

## Auth/session State Capture And Restore

The Selenium module can capture browser auth/session state in a Playwright `storageState`-style JSON file. The state includes cookies, `localStorage`, `sessionStorage`, and metadata such as label, role, origin, domain, creation time, and optional expiration.

```java
AuthState state = overlay.auth().captureState(AuthStateOptions.builder()
        .label("standard-customer")
        .role("customer")
        .origin("https://app.example.com")
        .build());

state.save(Path.of("target/ui-test-lens/auth/customer.json"));
```

Restore is explicit:

```java
AuthState state = AuthState.load(Path.of("target/ui-test-lens/auth/customer.json"));

overlay.auth().restoreState(state, AuthRestoreOptions.builder()
        .navigateToOrigin(true)
        .build());
```

Auth state files can contain cookies and tokens. Store them under `target/ui-test-lens/auth/`, do not commit them, do not put passwords in them, and treat them as generated sensitive artifacts. The first implementation does not encrypt state files and does not implement any provider-specific login flow. Restore is scoped to the captured origin; cross-origin storage is intentionally not handled yet, and some applications may need a refresh or navigation after restore.

## Passive Network Diagnostics

The Selenium module includes a passive network diagnostics model. The first implementation is a manual/fallback collector with explicit request, response, and failure events; it does not mock, intercept, or route requests.

```java
overlay.network().start(NetworkDiagnosticsOptions.builder()
        .captureMode(NetworkCaptureMode.AUTO)
        .failedStatusThreshold(400)
        .ignoreUrlPattern(".*analytics.*")
        .build());

overlay.network().assertNoFailedRequests();

overlay.network().attachToSession(
        overlay.session().orElseThrow(),
        Path.of("target/ui-test-lens/network/checkout-flow-network.json")
);
```

`AUTO` currently falls back to the manual collector unless browser-specific support is added later. `PERFORMANCE_LOGS` and `BIDI` are modeled but reported as unsupported without crashing, so Chrome-only dependencies are not required. Headers are omitted by default and sensitive header names such as `Authorization`, `Cookie`, `Set-Cookie`, `X-Api-Key`, and `X-Auth-Token` are masked when headers are enabled. Network JSON can be attached to the active trace session as a `NETWORK_LOG` artifact.

## Wait-For-Response Network Assertions

Network diagnostics also supports retryable passive waits over collected request/response events. This is an assertion layer over `NetworkDiagnostics`; it does not mock, intercept, route, or fulfill requests.

```java
overlay.network().waitForResponse("/api/orders", 200);

overlay.network().expectResponse()
        .urlContains("/api/orders")
        .method("POST")
        .status(201)
        .within(Duration.ofSeconds(10));
```

Conditions can match URL contains, exact URL, URL regex, method, exact status, or status range. `waitForResponse(...)` returns a `NetworkWaitResult`; `expectResponse().within(...)` is assertion-style and throws `NetworkAssertionError` on timeout or failed wait. Real-time event availability still depends on the configured capture mode. The manual/fallback collector remains the stable baseline, while `PERFORMANCE_LOGS` and `BIDI` stay modeled without extra dependencies.

## Logging And Event Bus

`UiTestLensLogger` is the central event bus. `OverlayLogger` is the current bridge used by existing overlay/Selenium classes.

```java
import java.util.List;
import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.api.ApiCallActions;
import io.github.mmaciekk111.uitestlens.api.ApiOverlayPanel;
import io.github.mmaciekk111.uitestlens.core.Guards;
import io.github.mmaciekk111.uitestlens.core.OverlayLogger;
import io.github.mmaciekk111.uitestlens.core.OverlayRootManager;
import io.github.mmaciekk111.uitestlens.core.logging.InMemoryLogSink;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogger;
import io.github.mmaciekk111.uitestlens.selenium.SeleniumOverlayFactory;

InMemoryLogSink memorySink = new InMemoryLogSink();

UiTestLensLogger eventLogger = UiTestLensLogger.builder()
        .sink(memorySink)
        .build();

OverlayLogger overlayLogger = OverlayLogger.from(eventLogger);

OverlayConfig config = OverlayConfig.builder().build();
OverlayRootManager rootManager = SeleniumOverlayFactory.overlayRoot(driver, config);
ApiOverlayPanel apiPanel = SeleniumOverlayFactory.apiOverlayPanel(driver, rootManager, config);
ApiCallActions apiCalls = new ApiCallActions(apiPanel);
Guards guards = new Guards(driver, overlayLogger);

JsOverlayDebug lens = new JsOverlayDebug(
        driver,
        config,
        apiPanel,
        apiCalls,
        guards,
        overlayLogger
);

lens.setStep("Open checkout");
lens.hudLog("info", "Checkout opened", "local");

List<UiTestLensLogEntry> entries = memorySink.entries();
```

## Exporting Logs

`InMemoryLogSink` can export collected events directly.

```java
import io.github.mmaciekk111.uitestlens.core.logging.export.PlainTextLogExporter;

String text = memorySink.exportAsText();
String json = memorySink.exportAsJson();
String html = memorySink.exportAsHtml();

String custom = memorySink.export(new PlainTextLogExporter());
```

The JSON and HTML exporters use only the JDK. There is no Jackson, Gson, or template engine dependency.

## Existing Project Logger Integration

Use `ConsumerLogSink` to forward UI Test Lens events into an existing project logger without a compile-time dependency from this library.

```java
import io.github.mmaciekk111.uitestlens.core.logging.ConsumerLogSink;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogger;

UiTestLensLogger logger = UiTestLensLogger.builder()
        .sink(new ConsumerLogSink(entry -> existingLogger.info(entry.message())))
        .build();
```

The public artifact no longer depends on private `LogWraper`. A `LogWraper` adapter can live in a downstream project or a future private adapter.

## Current Limitations

- Runtime JavaScript still keeps legacy `window.__selenium...` aliases for compatibility.
- Overlay root primary state is `window.__uiTestLens.state.overlay.root`; `window.__seleniumOverlayRoot` is kept as a legacy alias.
- Page wait helpers use primary state under `window.__uiTestLens.state.network`, `window.__uiTestLens.state.dom`, and `window.__uiTestLens.state.wait`; legacy wait/network globals remain compatibility aliases.
- API overlay JavaScript is loaded from `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/api-overlay.js`.
- Wait HUD JavaScript is loaded from `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/wait-hud.js`.
- Highlight JavaScript is loaded from `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/highlight.js`.
- Type hint JavaScript is loaded from `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/type-hint.js`.
- Scroll arrow JavaScript is loaded from `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/scroll-arrow.js`.
- HUD panel JavaScript is loaded from `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/hud-panel.js`.
- Assertion badge JavaScript is loaded from `ui-test-lens-overlay/src/main/resources/uitestlens/runtime/assertion-badges.js`.
- Legacy `selenium/api-overlay.js` remains a loader fallback for compatibility.
- Legacy `selenium/wait/WaitHud.js` remains a loader fallback for compatibility.
- Legacy `selenium/highlight.js` remains a loader fallback for compatibility.
- Legacy `selenium/type-hint.js` remains a loader fallback for compatibility.
- Legacy `selenium/scroll-arrow.js` remains a loader fallback for compatibility.
- Legacy `selenium/hud-panel.js` remains a loader fallback for compatibility.
- Legacy `selenium/assertion-badges.js` remains a loader fallback for compatibility.
- Type hints may still display the typed value in the browser overlay; value masking is planned for a later stage.
- Main known runtime JavaScript resources now cover API overlay, Wait HUD, Highlight, Type hint, Scroll arrow, HUD panel, and Assertion badges.
- Runtime JavaScript audit: [`docs/ui-test-lens-runtime-js-audit.md`](docs/ui-test-lens-runtime-js-audit.md).
- Module split plan: [`docs/ui-test-lens-module-split-plan.md`](docs/ui-test-lens-module-split-plan.md).
- Module boundary matrix: [`docs/ui-test-lens-module-boundaries.md`](docs/ui-test-lens-module-boundaries.md).
- [Playwright-inspired roadmap](docs/ui-test-lens-playwright-inspired-roadmap.md).
- `BrowserScriptExecutor` is now the neutral browser JavaScript execution contract; `SeleniumBrowserScriptExecutor` adapts Selenium `JavascriptExecutor` in the `ui-test-lens-selenium` module.
- `HudPanel` uses `BrowserScriptExecutor` internally; Selenium `WebDriver` construction is provided by `SeleniumOverlayFactory`.
- `ApiOverlayPanel` uses `BrowserScriptExecutor` internally; Selenium `WebDriver` construction is provided by `SeleniumOverlayFactory`.
- API is not final.
- Maven splits are in progress: `ui-test-lens-core` contains Selenium-free logging/export code and `BrowserScriptExecutor`; `ui-test-lens-overlay` contains runtime resources and overlay bridge classes without Selenium imports; `ui-test-lens-selenium` contains the Selenium facade/actions/waits and WebDriver-compatible overlay factories; `ui-test-lens-react` contains React-safe helpers; `ui-test-lens-examples` contains compile-checked examples; `ui-test-lens` is an all-in-one compatibility artifact.
- `ui-test-lens-overlay` uses `BrowserScriptExecutor` as its primary API and no longer depends on Selenium directly.
- `ui-test-lens-selenium` no longer depends on `ui-test-lens-react`; React helpers live behind `ReactSupport` in the React module.
- No Maven Central publication yet.
- No ready Selenide, Allure, or TeamCity adapters yet.
- No full Selenium `WebDriverListener` adapter yet.
- API overlay no longer has a RestAssured dependency; a RestAssured adapter can be added separately later.

## Roadmap

1. Use the runtime JS audit to review remaining small inline JavaScript snippets and decide which should become runtime resources.
2. Continue moving runtime bridge classes toward `BrowserScriptExecutor`.
3. Selenium `WebDriverListener` adapter.
4. Selenide adapter.
5. Allure/TeamCity exporters and adapters.
6. HTML report improvements.
7. Maven publication.

## Development

Current unit tests cover the logger/event model and exporters. They do not require Selenium or a browser.

Integration tests with a real browser are planned for a later stage.

## Build

```powershell
mvn -q test
mvn -q -DskipTests compile
```

## Notes On Project Migration

The current project deliberately keeps runtime names separate from the completed Java package and Maven coordinates migration. This keeps the migration incremental:

- first stabilize Maven layout, dependencies, logger/event model, exporters, and documentation,
- then rename Java packages,
- then clean up Maven coordinates,
- next clean up browser namespace,
- then continue the module split and publish artifacts.

## Future API Direction

The current construction API is intentionally not hidden in the examples. A future version may introduce a simpler facade such as `UiTestLens.selenium(...)`, but that API does not exist yet.
