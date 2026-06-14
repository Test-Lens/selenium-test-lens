# UI Test Lens Playwright-inspired roadmap

## Product direction

UI Test Lens should not replace Selenium and should not add a Playwright dependency. Playwright is an inspiration for UX, API shape, and reliability patterns, not a runtime dependency or migration target.

The product direction is:

```text
Playwright-like reliability + business diagnostics + Selenium compatibility
```

UI Test Lens should remain a diagnostic and reliability layer over Selenium. The project should improve UI test stability and post-failure analysis through:

- retryable locators,
- actionability checks,
- configurable overlay handling,
- web-first assertions,
- business steps and assertions,
- trace/evidence reports,
- screenshots and video attachments,
- network diagnostics,
- auth/session state reuse,
- React-aware readiness.

The goal is to keep current Selenium users productive while giving them a more reliable, more observable testing layer.

## Epic 1 — Reliability layer

The reliability layer is responsible for reducing flaky UI tests before adding broader reporting or DSL features. It should standardize how UI Test Lens decides whether an action can be executed safely.

### 1.1 Configurable blocking overlay policy

Status: initial implementation exists in `ui-test-lens-selenium`.

Users should be able to define known blocking popups and overlays instead of relying only on built-in heuristics. Typical examples:

- cookie banner,
- newsletter modal,
- session expired modal,
- focus-lock overlay.

Each handler should describe:

- name,
- detection selector,
- ordered list of actions,
- optional/fatal behavior,
- timeout,
- fail-if-still-visible behavior.

Supported actions should start small:

- click,
- press escape,
- wait until gone,
- fail,
- future: custom callback.

Example future API:

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

overlay.setOverlayPolicy(policy);
```

`OverlayPolicyExecutor` should be a separate service, not hidden inside smart click. It should be reusable by:

- `SmartClickActions`,
- future `ActionabilityChecker`,
- retryable locator API,
- React-aware checks.

This keeps popup handling explicit, configurable, and testable without changing the action semantics in multiple places.

The first implementation provides `OverlayPolicy`, `OverlayHandler`, `OverlayAction`, `OverlayPolicyExecutor`, `JsOverlayDebug.setOverlayPolicy(...)`, and integration with smart click. The policy is intentionally Selenium-side and does not move any API into the Selenium-free overlay module.

### 1.2 Actionability checks

Status: initial Selenium implementation exists in `ui-test-lens-selenium`.

UI Test Lens should add Playwright-like actionability checks for Selenium actions. Initial checks:

- attached,
- visible,
- enabled,
- stable bounding box,
- scrollable into view,
- receives click at target point,
- not covered by overlay,
- no known blocking overlay.

Example future API:

```java
lens.locator(By.cssSelector("[data-testid='save']")).click();
```

The implementation should report which actionability check failed and include enough metadata for diagnostics without leaking sensitive input values.

The first implementation provides `ActionabilityChecker`, `ActionabilityOptions`, `ActionabilityReport`, and `JsOverlayDebug.checkActionability(...)`. It checks attached, visible, enabled, stable bounding box, scroll into view, click point receiving/coverage, and configured overlay policy. `SmartClickActions` uses it as a best-effort diagnostic before the legacy click fallback flow.

### 1.3 Retryable element resolving

Status: initial Selenium locator API exists in `ui-test-lens-selenium`.

The locator layer should avoid holding `WebElement` references longer than necessary. It should keep the `By` locator and resolve the element immediately before the action.

Retries should cover:

- `StaleElementReferenceException`,
- transient click interception,
- transient visibility issues,
- transient disabled/loading states where the configured timeout still allows retry.

Every retry should emit action logs with attempt number, reason, elapsed time, and the last known target metadata. This is the foundation for useful trace reports.

The first implementation provides `UiLocator`, `UiLocatorOptions`, `UiLocatorResolver`, result/status/failure models, `JsOverlayDebug.locator(...)`, and `JsOverlayDebug.getByTestId(...)`. It supports click, fill, clear, pressEnter, textContent, isVisible, isEnabled, resolve, and checkActionability. It is not yet the full web-first assertions API.

### 1.4 React-aware readiness

Status: initial React module implementation exists in `ui-test-lens-react`.

React helpers should extend the base actionability checks rather than duplicating Selenium action logic. React-specific handling should cover:

- rerender,
- stale node,
- portals/modals,
- focus lock,
- `aria-disabled`,
- `aria-busy`,
- `data-loading`,
- `data-pending`,
- `data-state`,
- spinners/skeletons.

React-specific logic belongs in `ui-test-lens-react`, not `ui-test-lens-selenium`. The Selenium module should expose neutral extension points; the React module should layer React readiness on top.

The first implementation provides `ReactActionabilityChecker`, `ReactActionabilityOptions`, `ReactActionabilityReport`, React readiness result/failure enums, and `ReactSupport.checkActionability(...)`. It remains diagnostic/best-effort around existing React-safe flows and is not yet the retryable locator API.

## Epic 2 — Assertions layer

The assertions layer should provide retryable, web-first assertions similar in spirit to Playwright `expect`. It should focus on what the user sees in the browser, not only on immediate `assertEquals`-style checks.

### 2.1 Retryable web assertions

Example future API:

```java
lens.expect(By.cssSelector("[data-testid='toast']"))
        .toHaveText("Saved");

lens.expect(By.cssSelector("[data-testid='save']"))
        .toBeEnabled();
```

Assertions should:

- retry until timeout,
- log attempts,
- include failure reason,
- include last observed text/state,
- emit an event to the trace report.

The first implementation should stay Selenium-oriented and avoid introducing a test framework dependency into production code.

Initial implementation status: `ui-test-lens-selenium` now provides `UiExpect`, `UiAssertionOptions`, `UiAssertionResult`, `UiAssertionError`, and assertion events. The first supported assertions are visible/hidden, enabled/disabled, exact text, contains text, value, and contains value. They reuse `UiLocator` so each retry resolves the element again instead of keeping a long-lived `WebElement`.

### 2.2 Business assertions

Business assertions should make reports readable for QA and business reviewers, not only engineers reading selectors.

Initial implementation status: `ui-test-lens-selenium` now provides a lightweight `BusinessAssertions` group DSL over retryable `UiExpect` checks. It intentionally does not add domain-specific methods to the library.

Example API:

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

The business layer maps readable check descriptions to concrete UI assertions while keeping evidence tied to locators and observed browser state. Domain-specific DSLs such as `shouldShowAmount(...)` should live in the consuming test project or an optional adapter.

### 2.3 Grouped assertions

Grouped assertions should allow multiple related checks to run and collect several failures at once. The group result should appear as a single trace/report section with child assertion events.

This is also a candidate boundary for later React/business adapters, because domain pages often need both retryable UI checks and framework-specific readiness.

## Epic 3 — Business test DSL

The business test DSL should organize test flows into meaningful steps and produce readable reports.

Initial implementation status: `ui-test-lens-selenium` now provides `JsOverlayDebug.step(...)`, `UiStepOptions`, `UiStepResult`, `UiStepError`, and step events. This is not yet an HTML trace report, but it records the data trace reporting will need.

Example API:

```java
lens.step("Log in as active customer", () -> {
    loginPage.loginAs(activeCustomer);
});

lens.step("Add product to cart", () -> {
    productPage.addToCart("Premium");
});

lens.step("Verify order summary", () -> {
    lens.business("Order summary")
            .check("shows total amount", () -> {
                lens.getByTestId("order-total").expect().toHaveText("123.00 PLN");
            })
            .verify();
});
```

Each step should have:

- name,
- start/end time,
- duration,
- status,
- nested actions,
- assertions,
- overlay events,
- network events,
- screenshot marker,
- optional video timestamp marker.

The DSL should remain test-framework-neutral. JUnit/TestNG adapters can be added later as optional integrations.

## Epic 4 — Trace and evidence

Trace and evidence features should make post-failure diagnosis possible without rerunning the test. The trace should connect steps, actions, waits, overlays, assertions, screenshots, logs, and network events.

Initial implementation status: `ui-test-lens-core` now provides a Selenium-free trace/evidence model: `UiTestLensSession`, trace metadata, events, failures, artifact references, manual JSON export, HTML export, and `TraceLogSink`. `ui-test-lens-selenium` can attach/start a session from `JsOverlayDebug`, records step events into the session, exposes artifact attachment helpers, delegates HTML export to the attached session, and can capture Selenium screenshots as evidence artifacts. This is not yet video recording.

### 4.1 HTML trace report

The HTML trace report should include:

- timeline,
- steps,
- actions,
- wait events,
- overlay events,
- assertions,
- network events,
- durations,
- failure reasons.

Example future API:

```java
UiTestLensSession session = UiTestLensSession.start(driver);

lens.step("Save form", () -> {
    lens.locator(By.cssSelector("[data-testid='save']")).click();
});

session.exportHtml(Path.of("target/ui-test-lens/session.html"));
```

The first report should be static HTML generated from structured events. It should not require a server or frontend build tool.

Initial implementation status: `TraceHtmlExporter` now generates a self-contained HTML string/report from `UiTestLensSession`. It includes metadata, status, summary cards, timeline, step events, failures, artifacts, and optional raw JSON. It is intentionally simple and not yet a full interactive trace viewer.

### 4.2 Screenshots

Screenshot evidence should support:

- screenshot on failure,
- screenshot per step optionally,
- screenshot before/after action optionally,
- attaching screenshot path to trace event.

The Selenium implementation should use `TakesScreenshot`. Screenshot capture should be opt-in or policy-driven to control report size.

Initial implementation status: `ui-test-lens-selenium` now provides `ScreenshotCapture`, capture options/results/status, filesystem-safe evidence paths, `JsOverlayDebug.captureScreenshot(...)`, and opt-in `UiStepOptions.captureScreenshotOnFailure(...)`. Captured screenshots are written to `target/ui-test-lens/screenshots` by default and attached to the active trace session when present.

### 4.3 Optional video support

UI Test Lens should not require a video recording dependency. Video should be supported as an attachment, link, or path from the execution environment.

Potential integrations:

- Selenium Grid video,
- Docker Selenium video recording,
- Selenoid,
- BrowserStack/Sauce Labs artifacts,
- custom CI artifact path.

Example future API:

```java
overlay.attachVideoFile("Selenium Grid recording", Path.of("target/videos/test.mp4"));
overlay.attachVideoUrl("CI video", "https://ci.example.com/artifacts/test.mp4");
```

The trace should link video evidence to the whole session and optionally to timestamps for steps/actions.

Initial implementation status: `ui-test-lens-selenium` now provides video evidence attachment APIs for local files and remote URLs. `VideoEvidenceOptions` records source labels such as Selenium Grid, Selenoid, BrowserStack, Sauce Labs, CI artifact, or custom provider metadata. This does not record video, download provider artifacts, or require ffmpeg.

### 4.4 CI artifact integration

CI integration should focus on attaching artifacts, not replacing CI tooling. Supported artifact types:

- trace HTML,
- screenshots,
- video,
- logs,
- JSON export.

Initial integrations should document artifact paths for TeamCity, GitLab CI, GitHub Actions, and generic CI. Dedicated adapters can come later.

## Epic 5 — Network diagnostics

Network diagnostics should start with passive observation, then later support optional interception/mocking where browser support allows it.

### 5.1 Passive network logging

Passive network logging should collect:

- request URL,
- method,
- status,
- duration,
- failure,
- resource type, if available,
- trace attachment.

The first implementation can build on the existing network active request state and later add richer data through WebDriver BiDi/CDP when available.

Initial implementation status: `ui-test-lens-selenium` now provides passive network diagnostics models, a manual/fallback collector exposed through `JsOverlayDebug.network()`, JSON export, `assertNoFailedRequests()`, ignored URL patterns, header omission/masking, and trace attachment as a `NETWORK_LOG` artifact. It does not mock/intercept requests and does not add Chrome DevTools or Playwright dependencies.

### 5.2 Wait for response

Example future API:

```java
lens.network().waitForResponse("/api/orders", 200);
```

This should wait for an observed response matching URL/pattern and status without requiring request mocking.

### 5.3 Assert no failed requests

Example future API:

```java
lens.network().assertNoFailedRequests();
```

This should be useful at the end of critical flows and should report failed URLs/statuses in the trace.

### 5.4 Future request interception/mocking

Request interception and mocking should be a later stage:

- use WebDriver BiDi/CDP if available,
- treat browser support as optional,
- avoid mixing mocking into core Selenium actions at the beginning,
- keep mocking as an opt-in module or advanced feature.

This avoids making core reliability depend on browser-specific network capabilities.

## Epic 6 — Auth and session state

Auth/session state should let tests log in once, save browser state, and reuse it across later tests where appropriate.

### 6.1 Capture auth state

Example future API:

```java
AuthState state = lens.auth()
        .captureState(AuthStateOptions.builder()
                .label("standard-customer")
                .role("customer")
                .origin("https://app.example.com")
                .build());

state.save(Path.of("target/ui-test-lens/auth/customer.json"));
```

Capture should be explicit and should record metadata so stale state can be detected before reuse.

### 6.2 Restore auth state

Example future API:

```java
AuthState state = AuthState.load(Path.of("target/ui-test-lens/auth/customer.json"));

lens.auth().restoreState(state, AuthRestoreOptions.builder()
        .navigateToOrigin(true)
        .build());
```

Restore should validate target domain/origin and expiration metadata before applying state.

### 6.3 Supported data

Supported state data:

- cookies,
- localStorage,
- sessionStorage,
- origin/domain metadata,
- createdAt,
- expiresAt,
- user/role label,
- optional notes.

### 6.4 Safety rules

Auth state must be treated as sensitive:

- do not save passwords,
- do not commit tokens to the repository,
- default to target/generated paths,
- provide `.gitignore` guidance,
- validate expiry,
- validate domain.

The feature should make safe defaults easy and unsafe persistence explicit.

Initial implementation status: `ui-test-lens-selenium` now provides `AuthState`, cookie/storage state models, metadata/options, manual JSON export/parser, save/load helpers, and `AuthStateManager` through `JsOverlayDebug.auth()`. It captures Selenium cookies, `localStorage`, and `sessionStorage`, restores them for the captured origin, and ignores `target/ui-test-lens/auth/`. It does not encrypt files, store passwords as first-class fields, implement login flows, or handle cross-origin storage beyond the current page origin.

### 6.5 Role-based states

Role-based state examples:

- admin,
- standard customer,
- company user,
- unauthenticated.

Role labels should appear in trace/session metadata to help diagnose authorization-specific failures.

## Suggested implementation order

1. Add configurable blocking overlay policy.
   This should come first because existing smart actions already deal with popups and overlays. Making this configurable reduces flakiness without requiring a new locator API.
2. Add Selenium actionability checks.
   Initial implementation exists. Continue by hardening logging, public examples, and failure evidence before building a locator API on top.
3. Add React-aware actionability checks.
   Initial implementation exists in `ui-test-lens-react`. Continue by using it as the readiness layer for future locator/actions without adding a Selenium-to-React dependency.
4. Add retryable UI locator API.
   Initial implementation exists. Continue with richer locator factories such as getByRole/getByLabel/getByText and then web-first assertions.
5. Add retryable web assertions.
   Assertions should reuse locator resolution and retry semantics rather than inventing separate wait behavior.
6. Add business step DSL.
   Steps become more valuable once actions and assertions produce structured events.
7. Add HTML trace report.
   Initial static HTML export now exists. Continue by improving event mapping and adding a richer viewer only after the model stabilizes.
8. Add screenshots as evidence.
   Selenium `TakesScreenshot` capture now exists as opt-in evidence capture, with failed-step capture controlled by `UiStepOptions`.
9. Add optional video attachments.
   Passive video attachments now exist as paths/URLs with provider/source metadata. Later work can add CI/provider-specific discovery or download helpers.
10. Add auth/session state save and restore.
    Initial Selenium-side capture/restore is implemented. Later work should focus on stricter safety checks, optional encryption guidance, and higher-level test framework integration.
11. Add passive network diagnostics.
    Initial passive/manual diagnostics are implemented. Continue by improving browser-specific collection only behind safe optional fallbacks.
12. Add wait-for-response and no-failed-requests assertions.
    These APIs should build on passive network data once collection is stable.
13. Add optional network interception/mocking.
    Mocking is browser-capability-dependent and should remain an advanced opt-in feature.

## Module ownership

| Feature | Module |
| ------- | ------ |
| Event model, logging, export, trace/evidence model | `ui-test-lens-core` |
| Runtime JavaScript overlay | `ui-test-lens-overlay` |
| Selenium actions, actionability, locator, assertions, session integration, network | `ui-test-lens-selenium` |
| React readiness and `ReactSupport` | `ui-test-lens-react` |
| End-user dependency bundle | `ui-test-lens` |
| Examples | `ui-test-lens-examples` |

## Non-goals

- Do not add a Playwright dependency.
- Do not replace Selenium.
- Do not build a full test runner.
- Do not require a specific test framework.
- Do not add network mocking in the first stage.
- Do not require a video recorder dependency in core.
- Do not save secrets/passwords in auth state.
