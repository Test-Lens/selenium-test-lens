# What's new in 0.3.0

Selenium Test Lens 0.3.0 adds configuration, security, lifecycle, and reporting capabilities around the same consumer-owned Selenium `WebDriver`. The release does not replace Selenium or require a new test runner.

## Configurable runtime HUD

The browser HUD now has an immutable product-level configuration model. Start with a preset, then override only the properties your test environment needs:

```java
HudOptions hud = HudOptions.builder()
        .preset(HudPreset.COMPACT)
        .position(HudPosition.TOP_RIGHT)
        .backgroundOpacity(0.88)
        .showNetwork(false)
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .hud(hud)
        .build());
```

`MINIMAL`, `COMPACT`, `STANDARD`, and `DEBUG` define coherent starting points. Explicit builder overrides win regardless of call order. Position, viewport-safe sizing, content visibility, atomic header layout, local font stacks, scrollbar styling, colors, opacity, and bounded PNG branding are supported without exposing arbitrary CSS or HTML.

[Configure the HUD](observability/visual-diagnostics.md#configurable-hud) or [open HUD Studio](observability/hud-studio.md).

## HUD Studio

HUD Studio is a static WYSIWYG editor that runs the same browser-side renderer as the runtime HUD. Drag and snap the panel, resize it, switch preview viewports, adjust supported appearance and content options, then copy Java configuration that uses `HudOptions`.

Studio-only editor chrome—selection outlines, drag affordances, and resize handles—is not part of the runtime HUD. Every option that changes the rendered HUD maps to the public configuration model.

[Customize your HUD](observability/hud-studio.md).

## Visual Redaction

`VisualRedactionOptions` protects screenshot pixels independently from text-oriented `RedactionPolicy`. Password inputs are masked with `SOLID` by default, and the default `STRICT` policy refuses to publish a screenshot when a required mask cannot be verified.

```java
VisualRedactionOptions visual = VisualRedactionOptions.builder()
        .mask(By.id("customer-email"), VisualMaskMode.SOLID)
        .failurePolicy(VisualRedactionFailurePolicy.STRICT)
        .build();
```

Diagnostic and clean screenshots use the same masking boundary. Full-page capture refreshes masks per tile, and one bounded batch retry handles targets replaced or moved during React/SPA rendering. `BLUR` remains visual obfuscation rather than irreversible secret removal.

[Read the visual-redaction boundary](security/visual-redaction.md).

## Managed Auth State

Managed Auth State adds a bounded restore–validate–recreate lifecycle above the existing cookie and Web Storage primitives. Validation is tri-state: `AUTHENTICATED`, `UNAUTHENTICATED`, or `INCONCLUSIVE`. An inconclusive result never starts login and never overwrites the previous file.

```java
AuthStateEnsureResult result = lens.authState().ensure(
        AuthStateRequest.builder()
                .key("primary-user")
                .path(Path.of("target/auth/primary.json"))
                .login(driver -> loginPage.login(username, password))
                .validate(driver -> accountMenu.isDisplayed()
                        ? AuthStateValidation.AUTHENTICATED
                        : AuthStateValidation.UNAUTHENTICATED)
                .build());
```

Creation and refresh perform at most one login, validate before capture, and replace persisted state atomically under JVM and filesystem locks. Existing low-level auth-state capture and restore remain available.

[Use Managed Auth State](advanced/auth-state.md).

## Managed Test State & Resources

Typed scenario state belongs to one physical invocation—not one method—so parameter rows, repetitions, retries, and parallel invocations do not share it. Suite state provides intentional, thread-safe sharing inside one logical runner suite or manual run scope.

```java
lens.scenarioState().put("orderId", order.id());
String orderId = lens.scenarioState().require("orderId", String.class);

Tenant tenant = lens.suiteState().computeIfAbsent(
        "tenant", Tenant.class, this::createTenant);
```

Scenario resources run cleanup exactly once in reverse registration order on pass, failure, or skip. Cleanup failures do not replace an existing primary test failure; a cleanup failure does fail an otherwise successful finalization.

[Manage test state and resources](features/managed-test-state.md).

## Allure integration

The optional `selenium-test-lens-allure` artifact streams finalized Test Lens evidence into the active Allure test or step. It does not capture another screenshot, access WebDriver, change Allure status, or make Allure transitive from the umbrella artifact.

```java
TestLensFinalizationResult result = lens.finishFailed(failure);
AllureAttachResult publication = AllureTestLens.attach(result);
```

When no Allure executable is active, the adapter returns `SKIPPED_NO_ACTIVE_CONTEXT`. Repeat publication for the same finalized result and context is idempotently skipped.

[Add Allure attachments](integrations/allure.md).

## React, SPA, and interaction documentation

React support is now classified as **React & SPA resilience**, not as a runner integration. Its DOM-based contract remains focused on rerender recovery, common busy/loading conventions, SPA-aware waits, and React Select helpers. It does not inspect the React component tree.

[Review React & SPA resilience](features/react-spa.md).

## Reliability and compatibility

The 0.3.0 line also strengthens browser and release contracts: responsive HUD clamping, Firefox cookie replay normalization, per-session Chrome process ownership in browser tests, best-effort evidence collector assertions, and clean-room verification of all seven published library artifacts on JDK 17 and JDK 21.

These changes improve interoperability and release validation; they do not transfer WebDriver ownership to Test Lens.

## Compatibility notes

- Java 17 remains the minimum runtime and bytecode level.
- Existing Selenium ownership and direct WebDriver use remain unchanged.
- Existing JUnit 5 and TestNG adapters remain supported.
- Existing `RedactionPolicy` remains the text diagnostic boundary; screenshot masking is a separate layer.
- Existing auth-state JSON remains readable, including insecure `SameSite=None` representations normalized safely during replay.
- The React guide moved to `/features/react-spa/`; the old development URL remains a noindex compatibility redirect.

For upgrade details, see [Migrating from 0.2.x to 0.3.0](migrating-0.2-to-0.3.md).
