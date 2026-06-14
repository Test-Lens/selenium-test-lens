# UI Test Lens

Visual observability and debug layer for UI/browser automation tests.

## Status

This project is currently being migrated from an internal Selenium helper project into UI Test Lens. The artifact name, Java package namespace, and Maven coordinates now use UI Test Lens naming.

Current state:

- Maven artifactId is `ui-test-lens`.
- Maven groupId is `io.github.mmaciekk111`.
- Java packages use `io.github.mmaciekk111.uitestlens`.
- The project is currently a single-module Maven project.
- Multi-module split is planned later.
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
    <version>1.0-SNAPSHOT</version>
</dependency>
```

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

WebDriver driver = /* existing Selenium driver */;

OverlayConfig config = OverlayConfig.builder()
        .showHudPanel(true)
        .highlightColor("#ffeb3b")
        .build();

OverlayRootManager rootManager = new OverlayRootManager(driver, config);
ApiOverlayPanel apiPanel = new ApiOverlayPanel(driver, rootManager, config);
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

InMemoryLogSink memorySink = new InMemoryLogSink();

UiTestLensLogger eventLogger = UiTestLensLogger.builder()
        .sink(memorySink)
        .build();

OverlayLogger overlayLogger = OverlayLogger.from(eventLogger);

OverlayConfig config = OverlayConfig.builder().build();
OverlayRootManager rootManager = new OverlayRootManager(driver, config);
ApiOverlayPanel apiPanel = new ApiOverlayPanel(driver, rootManager, config);
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
- API overlay JavaScript is loaded from `src/main/resources/uitestlens/runtime/api-overlay.js`.
- Wait HUD JavaScript is loaded from `src/main/resources/uitestlens/runtime/wait-hud.js`.
- Highlight JavaScript is loaded from `src/main/resources/uitestlens/runtime/highlight.js`.
- Type hint JavaScript is loaded from `src/main/resources/uitestlens/runtime/type-hint.js`.
- Scroll arrow JavaScript is loaded from `src/main/resources/uitestlens/runtime/scroll-arrow.js`.
- HUD panel JavaScript is loaded from `src/main/resources/uitestlens/runtime/hud-panel.js`.
- Assertion badge JavaScript is loaded from `src/main/resources/uitestlens/runtime/assertion-badges.js`.
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
- API is not final.
- No multi-module split yet.
- No Maven Central publication yet.
- No ready Selenide, Allure, or TeamCity adapters yet.
- No full Selenium `WebDriverListener` adapter yet.
- API overlay no longer has a RestAssured dependency; a RestAssured adapter can be added separately later.

## Roadmap

1. Use the runtime JS audit to review remaining small inline JavaScript snippets and decide which should become runtime resources.
2. Introduce a `BrowserScriptExecutor` abstraction before moving files.
3. Split into `ui-test-lens-core`, `ui-test-lens-overlay`, `ui-test-lens-selenium`, `ui-test-lens-react`, and `ui-test-lens-examples`.
4. Selenium `WebDriverListener` adapter.
5. Selenide adapter.
6. Allure/TeamCity exporters and adapters.
7. HTML report improvements.
8. Maven publication.

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
- then split modules and publish artifacts.

## Future API Direction

The current construction API is intentionally not hidden in the examples. A future version may introduce a simpler facade such as `UiTestLens.selenium(...)`, but that API does not exist yet.
