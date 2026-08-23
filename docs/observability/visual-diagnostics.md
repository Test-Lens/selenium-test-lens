# Visual runtime diagnostics

Visual diagnostics appear inside the tested page while a test is running. They are most useful during headed local execution and debugging: the browser itself shows what Test Lens is doing, what it is waiting for, and which assertion passed or failed. These diagnostics are transient; persistent output is covered under [screenshots and evidence](screenshots-evidence.md), [trace](trace.md), and [reports](reports.md).

## HUD

The HUD is an in-browser diagnostic panel displayed over the tested page. It shows the active session, current step, element actions, waits, assertions, and diagnostic log rows so you can follow a test without switching to a console or report.

The standard overlay and HUD are enabled by default. Attach Test Lens and start a session to use them:

```java
TestLens lens = TestLens.attach(driver);
lens.startSession("Checkout");
```

The HUD is especially useful when debugging a headed test, demonstrating a flow, or investigating a wait/retry. It does not replace the persistent trace or test-runner output.

### Enable and configure the HUD

Configure the HUD through [`OverlayConfig`](../reference/configuration.md#overlayconfig) and pass it to the public [`TestLens.attach(WebDriver, OverlayConfig)`](../reference/test-lens.md#creation-and-lifecycle) overload:

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .enabled(true)
        .showHudPanel(true)
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.DARK)
        .build();

TestLens lens = TestLens.attach(driver, overlayConfig);
lens.startSession("Checkout");
```

To hide only the HUD while retaining overlay capabilities such as click decoration:

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .showHudPanel(false)
        .build();
```

To disable all visual overlay behavior:

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .enabled(false)
        .build();
```

See [Getting Started](../getting-started.md), the complete [`OverlayConfig` table](../reference/configuration.md#overlayconfig), [`HudTheme`](../reference/configuration.md#hudtheme), and the `hudPosition(...)` row in [`OverlayConfig`](../reference/configuration.md#overlayconfig).

`TestLens.startSession(...)` attempts the initial HUD injection. Events retry injection lazily when a browser document was not available earlier, such as around navigation. When [`TestLensOptions.cleanupHudOnFinish`](../reference/configuration.md#testlensoptions) is enabled, finalization removes HUD/debug artifacts on a best-effort basis. Injection and cleanup failures do not change the WebDriver operation's intended result.

<!-- SCREENSHOT TODO: assets/screenshots/hud-full-panel.png
Show the complete HUD during a running session with a session name, current step, and several log rows.
Use a real library build and keep text readable at documentation width.
Feature documented: HUD layout and runtime information hierarchy.
Suggested alt text: Full Test Lens HUD with session, current step, and structured event rows.
-->

## Highlights

A highlight is a temporary border/label drawn around an element so you can see which DOM target Test Lens selected. It is useful when a selector matches an unexpected element or a click is intercepted by page UI.

The standard overlay-aware [`click()`](../elements/actions.md#click) highlights its resolved target automatically when [`OverlayConfig.enabled()`](../reference/configuration.md#overlayconfig) is true:

```java
lens.getByRole("button", "Save").click();
```

`fill()`, `clear()`, `press()`, `hover()`, `doubleClick()`, and `rightClick()` do not currently apply the same click decoration. `UiLocatorOptions.highlightBeforeAction()` is a retained public option but is not consulted by the current `UiLocator` implementation; it does not enable or disable highlights.

Use [`OverlayConfig.highlightColor(...)`](../reference/configuration.md#overlayconfig) and [`decorationDurationMs(...)`](../reference/configuration.md#overlayconfig) to control the color and lifetime. For deliberate non-click decoration of a resolved element or related DOM node, use the [advanced explicit highlight helpers](../advanced/js-overlay-debug.md#hud-and-explicit-visual-helpers).

<!-- SCREENSHOT TODO: assets/screenshots/target-highlight.png
Show a real target decoration with its label and enough application context to identify the element.
Do not duplicate the click screenshot: use the explicit highlight helper or another non-click context.
Feature documented: standalone visual element highlighting.
Suggested alt text: Page element outlined and labelled by the Test Lens highlight helper.
-->

## Wait feedback

Wait feedback shows that a condition is still being polled and reports its progress through the HUD/log/trace pipeline. It appears during explicit [element waits](../elements/waiting.md) and relevant retry loops, helping distinguish an active wait from a stalled test.

```java
lens.getByTestId("results").waitUntilVisible();
```

The visual feedback does not change the Selenium condition, polling interval, or deadline. Configure those through [`UiLocatorOptions.timeout(...)` and `pollInterval(...)`](../reference/configuration.md#uilocatoroptions).

## Assertion feedback

Assertion feedback makes fluent assertion progress visible. Reporter events distinguish assertion start, retries, pass, timeout, and failure, which is useful when an expected UI state arrives late or never appears.

```java
lens.getByTestId("status").expect().toHaveText("Saved");
```

Assertions use their own polling and comparison settings. See [Element assertions](../elements/assertions.md) and [`UiAssertionOptions`](../reference/configuration.md#uiassertionoptions). Diagnostic previews are bounded by the options, but page content and screenshots can still expose sensitive values.

## Theme and placement

Position and theme are opt-in customizations of the default HUD:

```java
OverlayConfig overlayConfig = OverlayConfig.builder()
        .hudPosition(HudPosition.TOP_RIGHT)
        .hudTheme(HudThemePreset.HIGH_CONTRAST)
        .build();
```

`HudPosition` chooses the anchored location, while a `HudThemePreset` selects a built-in palette and layout. Custom `HudTheme` values are also supported. Use the full [`OverlayConfig`](../reference/configuration.md#overlayconfig) and [`HudTheme`](../reference/configuration.md#hudtheme) configuration tables for presets, offsets, width, palette, spacing, and validation rules instead of relying on duplicated defaults here. Arbitrary custom theme strings become generated CSS and must be trusted test configuration.

## Low-level HUD API

`HudPanel`, `ApiOverlayPanel`, JS resource wrappers, `OverlayRootManager`, and related `*Js` types are formally public but intended for custom integrations and internal-style use. See [advanced/low-level API](../reference/advanced-low-level.md).
