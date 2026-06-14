# UI Test Lens

UI Test Lens is an observability and reliability toolkit for browser-based UI automation. It adds resilient WebDriver actions, retryable assertions, visual debugging, evidence capture, auth/session state helpers, network diagnostics, and trace reports.

The project is currently `0.1.0-SNAPSHOT` and pre-1.0. Public APIs are usable, but may still be polished before a first release.

## Requirements

- Java 17
- Maven 3.x
- Selenium supplied by the consuming test project

Maven Central publishing is not configured yet. For local use, build and install from this repository.

## Quick Start

All-in-one dependency:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Selenium-only layer:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens-selenium</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Minimal usage:

```java
import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
import io.github.mmaciekk111.uitestlens.OverlayConfig;
import io.github.mmaciekk111.uitestlens.hud.HudPosition;
import io.github.mmaciekk111.uitestlens.hud.HudTheme;
import io.github.mmaciekk111.uitestlens.hud.HudThemePreset;
import org.openqa.selenium.WebDriver;

WebDriver driver = createDriver();

OverlayConfig config = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.GLASS)
        .build();

HudTheme cappedHud = HudTheme.builder()
        .maxHeightPx(420)
        .build();

JsOverlayDebug overlay = new JsOverlayDebug(driver, config);

overlay.setStep("Save order");
overlay.hudLog("info", "Clicking save", "local");

overlay.getByTestId("save-order").click();

overlay.expect(overlay.getByTestId("toast"))
        .toContainText("Saved");
```

## Build

Recommended checks:

```powershell
mvn -q test
mvn -q -DskipTests compile
```

Module checks:

```powershell
mvn -q -pl ui-test-lens-core test
mvn -q -pl ui-test-lens-overlay -am test
mvn -q -pl ui-test-lens-selenium -am test
mvn -q -pl ui-test-lens-react -am test
mvn -q -pl ui-test-lens-examples -am test
mvn -q -pl ui-test-lens -am test
```

## Modules

| Module | Responsibility |
|---|---|
| `ui-test-lens-core` | Logging, trace/evidence model, JSON/HTML exporters. No Selenium dependency. |
| `ui-test-lens-overlay` | Runtime JavaScript overlay resources, HUD configuration, visual debugging assets. No Selenium dependency. |
| `ui-test-lens-selenium` | Selenium facade, locators, assertions, actionability, evidence, auth/session state, network diagnostics. |
| `ui-test-lens-react` | React-specific support layered on top of Selenium integration. |
| `ui-test-lens` | All-in-one POM dependency bundle. |
| `ui-test-lens-examples` | Documentation and compile-check examples. Not intended as a runtime dependency. |

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

UI Test Lens is licensed under the [Apache License 2.0](LICENSE).

## Current Scope

UI Test Lens runs on top of Selenium/WebDriver and adds diagnostic APIs:

- visual HUD and element debugging
- configurable blocking overlay policy
- actionability checks
- ergonomic `UiLocator` helpers
- retryable web assertions
- business assertions and named steps
- trace/evidence session model and HTML report export
- screenshot capture and video attachments
- auth/session state capture and restore
- passive network diagnostics and wait-for-response assertions

Current limitations:

- `getByRole` does not implement the full ARIA accessible-name algorithm.
- network diagnostics are passive; browser capture providers and interception/mocking are not implemented.
- video support is attachment/reference based; UI Test Lens does not record video.
- HUD themes focus on the HUD panel; Wait HUD and assertion badges are not fully covered by the common theme system.
