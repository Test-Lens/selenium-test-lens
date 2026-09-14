# Migrating from 0.2.x to 0.3.0

Version 0.3.0 is an additive release for normal Test Lens consumers. It keeps the Java 17 baseline, the consumer-owned `WebDriver`, existing locator and evidence APIs, and the JUnit 5 and TestNG integrations. No application needs to adopt the new HUD, visual redaction, managed state, or Allure module all at once.

## Upgrade the coordinates you use

Update each Test Lens dependency declared by your project to `0.3.0`. Keep Selenium explicit and at the version selected by your project.

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.3.0</version>
</dependency>
```

Optional modules—React/SPA helpers, JUnit 5, TestNG, and Allure—are separate artifacts. Add only the adapters you use. `selenium-test-lens-allure` depends on `allure-java-commons`; the ordinary umbrella artifact does not.

## Existing code remains valid

- `TestLens.attach(driver)` and the existing `startSession`/`finish...` lifecycle remain available.
- Raw Selenium and existing Page Objects can continue alongside Lens.
- Existing locator, action, wait, report, upload, and failure-bundle contracts are unchanged unless called out in the changelog.
- Existing JUnit 5 and TestNG integrations remain the runner-specific lifecycle adapters.
- The low-level `AuthStateManager` capture, save, load, and restore primitives remain available.
- Existing `RedactionPolicy` configuration still protects diagnostic text.

## HUD configuration

`HudOptions` is the preferred 0.3.0 configuration model for the runtime HUD:

```java
HudOptions hud = HudOptions.builder()
        .preset(HudPreset.COMPACT)
        .position(HudPosition.TOP_RIGHT)
        .showNetwork(false)
        .build();

TestLensOptions options = TestLensOptions.builder()
        .hud(hud)
        .build();
```

The default effective preset is `COMPACT`. Presets provide base values; explicit builder overrides win regardless of call order.

Legacy `OverlayConfig` HUD position, offset, width, and `HudTheme` settings remain compatible when no explicit `HudOptions` is supplied. When both are present, explicit `HudOptions` is authoritative. Other overlay settings, such as whether the overlay is enabled and the element highlight color, still come from `OverlayConfig`.

Use [HUD Studio](observability/hud-studio.md) to generate the preferred API rather than translating CSS manually.

## Text redaction and screenshot pixels are separate

`RedactionPolicy` continues to protect recognized and caller-supplied secrets in diagnostic text, including logs, trace, network diagnostics, reports, and text bundle components. It does not modify screenshot pixels.

Version 0.3.0 adds `VisualRedactionOptions` for screenshot capture. Default Test Lens options automatically mask password inputs with `SOLID` and use the fail-closed `STRICT` policy. Review selectors and capture behavior before relying on evidence from pages that contain other sensitive visual fields.

```java
VisualRedactionOptions visual = VisualRedactionOptions.builder()
        .mask(By.id("account-number"), VisualMaskMode.SOLID)
        .build();
```

See [Visual redaction](security/visual-redaction.md) for supported contexts and explicit exclusions such as page source, video, and native browser UI.

## Managed Auth State is optional and additive

Existing auth-state JSON remains readable. Managed Auth State adds orchestration around the same primitives: restore, application validation, at most one login, authenticated capture, and atomic replacement.

Persisted 0.2.x cookie data containing `SameSite=None` with `secure=false` is normalized during Selenium cookie replay without changing the stored file. The runtime omits that non-replayable SameSite attribute; it does not force `Secure`, `Lax`, or `Strict`.

Adopt `lens.authState().ensure(...)` only where your application can provide an unambiguous tri-state validator. `INCONCLUSIVE` is a failure and never triggers login or replacement. Test Lens does not perform automatic logout.

## Managed Test State is in memory

`scenarioState()`, `suiteState()`, and `resources()` are new lifecycle helpers. They do not replace persisted auth state:

- scenario state is isolated to one physical invocation, including each retry or parameter row;
- suite state is intentionally shared only inside one runner suite or explicit `TestRunScope`;
- registered scenario resources are cleaned exactly once in LIFO order during finalization.

Values and keys are not automatically written to evidence. They are lifecycle storage, not a secret vault or cross-process data store.

## Allure stays optional

Add `selenium-test-lens-allure` only if the project uses Allure. Call `AllureTestLens.attach(...)` after Test Lens finalization and before the active Allure executable closes. The adapter streams the already-finalized, already-redacted artifacts and never takes its own screenshot.

## React documentation URL

The React helpers are now documented as **React & SPA resilience** at `/features/react-spa/`. The old development path `/integrations/react/` is a static noindex compatibility redirect. The Maven artifact remains `selenium-test-lens-react`; no package or artifact rename is required.

## Migration checklist

- [ ] Change every declared Test Lens artifact to `0.3.0` together.
- [ ] Keep Selenium explicit and keep Java 17 or newer.
- [ ] Run existing tests before adopting optional 0.3.0 features.
- [ ] If configuring the HUD, prefer `HudOptions` and verify legacy/explicit precedence.
- [ ] Review screenshot selectors and the default STRICT visual-redaction policy.
- [ ] Protect managed auth-state files as replayable authentication material.
- [ ] Verify that authentication validators can return `INCONCLUSIVE` for outages or ambiguity.
- [ ] Confirm scenario resource cleanup errors are handled by the runner lifecycle.
- [ ] Add the Allure artifact only to projects that publish to Allure.
- [ ] Update bookmarks from `/integrations/react/` to `/features/react-spa/`.
- [ ] Run the project on its supported Chrome and Firefox versions before release adoption.

For a feature-oriented overview, see [What's new in 0.3.0](whats-new-0.3.0.md). For exact fixes and compatibility notes, see the [changelog](https://github.com/Test-Lens/selenium-test-lens/blob/main/CHANGELOG.md).
