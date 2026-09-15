---
hide:
  - navigation
  - toc
---

<div class="lens-hero" markdown>

# Observe Selenium. Recover deliberately. Keep useful evidence.

**Keep Selenium and your existing WebDriver. Add observability, resilient browser interaction, scoped test lifecycle, and evidence with explicit security boundaries.**

Test Lens shows actions, waits, assertions, retries, and network activity while a test runs. After finalization, the same structured session can become an HTML/JSON report, screenshots, a failure bundle, or an explicitly uploaded report package.

[See the live diagnostics](#live-hud-and-element-highlights){ .md-button .md-button--primary }
[What's new in 0.3.0](whats-new-0.3.0.md){ .md-button }
[Explore the capabilities](#signature-capabilities){ .md-button }
[Get started](#quick-start){ .md-button }
[Maven Central](https://central.sonatype.com/artifact/io.github.test-lens/selenium-test-lens/0.3.0){ .md-button }

| Documentation | Status | Library availability |
|---|---|---|
| [0.4.0-SNAPSHOT development](https://test-lens.github.io/selenium-test-lens/dev/) | Current source line | Source build only; not available from Maven Central |
| [0.3.0 stable](https://test-lens.github.io/selenium-test-lens/0.3.0/) | Latest published release | Maven Central |
| [0.2.0 historical](https://test-lens.github.io/selenium-test-lens/0.2.0/) | Previous release | Maven Central |
| [0.1.0 historical](https://test-lens.github.io/selenium-test-lens/0.1.0/) | Historical release | Maven Central |

</div>

## What Test Lens adds to Selenium

Selenium remains the browser automation engine. Test Lens attaches to the `WebDriver` that your project already creates and surrounds native browser operations with a connected diagnostic layer:

```text
your WebDriver and runner
        ↓
semantic queries · native interactions · application-aware waits
        ↓
HUD · highlights · trace · recovery and network diagnostics
        ↓
HTML/JSON reports · screenshots · failure bundle · optional HTTP upload
```

It does not replace Selenium, Page Objects, JUnit, TestNG, or an existing reporting stack. Raw WebDriver remains available whenever it is the clearer or more complete API.

## 0.3.0 at a glance

Release 0.3.0 adds configuration and lifecycle capabilities around the same Selenium session. These are public 0.3.0 APIs and were not features of release 0.2.0.

<div class="grid cards" markdown>

-   **NEW 0.3.0 · LIVE UI — Configurable HUD**

    Start from Minimal, Compact, Standard, or Debug; then override supported content, position, viewport-safe size, header layout, typography, scrollbar, palette, opacity, and bounded PNG branding.

    [Configure the runtime HUD](observability/visual-diagnostics.md#configurable-hud)

-   **NEW 0.3.0 · WYSIWYG — HUD Studio**

    Drag, snap, and resize the real runtime renderer in Desktop, Laptop, or Mobile preview. Copy minimal Java that reproduces the edited `HudOptions`.

    [Customize your HUD](observability/hud-studio.md)

-   **NEW 0.3.0 · SECURITY — Visual Redaction**

    Mask screenshot pixels independently from text redaction. Password inputs use SOLID masking by default; STRICT refuses to publish a screenshot when a required mask cannot be verified.

    [Define the screenshot boundary](security/visual-redaction.md)

-   **NEW 0.3.0 · AUTH — Managed Auth State**

    Restore and validate persisted browser state, perform at most one login when recreation is justified, and atomically preserve or replace the previous file.

    [Manage authentication state](advanced/auth-state.md)

-   **NEW 0.3.0 · STATE — Managed Test State & Resources**

    Isolate typed data per physical invocation, share state intentionally inside one suite, and clean scenario resources exactly once in LIFO order.

    [Manage state and resources](features/managed-test-state.md)

-   **NEW 0.3.0 · INTEGRATION — Allure**

    Attach finalized, redacted Test Lens evidence to the active Allure executable through an optional artifact. The adapter neither captures another screenshot nor changes Allure status.

    [Use the Allure adapter](integrations/allure.md)

</div>

## Live HUD and element highlights

The in-browser HUD makes the active Lens session visible in the page under test. It presents the current step and diagnostic rows emitted by element actions, waits, assertions, recovery retries, and network diagnostics. Rows carry their operation status and safe target or locator context; [`NetworkHudFilter`](advanced/network.md#hud-only-filtering) can reduce raw request/response noise without removing captured evidence.

<style>
.tl-hud-demo-frame { display: block; width: 100%; max-width: 100%; height: 650px; border: 1px solid var(--md-default-fg-color--lightest); border-radius: .6rem; background: #eef2f7; }
@media (max-width: 720px) { .tl-hud-demo-frame { height: 690px; } }
</style>

<iframe
  id="test-lens-hud-demo"
  class="tl-hud-demo-frame"
  src="demo/hud/"
  title="Interactive Test Lens HUD, highlight, and scroll-cue demonstration"
  loading="eager"
  sandbox="allow-scripts"
>
  The interactive demo could not be loaded. <a href="demo/hud/">Open the standalone HUD demo.</a>
</iframe>

<script>
(function () {
  var frame = document.getElementById('test-lens-hud-demo');
  if (!frame) return;

  var visible = false;
  function notifyDemo() {
    if (frame.contentWindow) {
      frame.contentWindow.postMessage({ type: 'test-lens-demo-visibility', visible: visible }, '*');
    }
  }

  window.addEventListener('message', function (event) {
    if (event.source === frame.contentWindow && event.data && event.data.type === 'test-lens-demo-ready') {
      notifyDemo();
    }
  });

  if (!('IntersectionObserver' in window)) {
    visible = true;
    frame.addEventListener('load', notifyDemo);
    notifyDemo();
    return;
  }

  var observer = new IntersectionObserver(function (entries) {
    var entry = entries[0];
    visible = Boolean(entry && entry.isIntersecting && entry.intersectionRatio >= 0.05);
    notifyDemo();
  }, { threshold: [0, 0.05] });
  observer.observe(frame);
  frame.addEventListener('load', notifyDemo);
}());
</script>

<small>This is a deterministic simulation: the checkout UI and network rows are synthetic, while the HUD, labeled highlight, and scroll arrow use the renderer for this documentation version. No request is sent.</small>

<!-- configurable-hud-dev:start -->
The development renderer defaults to the compact preset. Try **Compact**, **Minimal**, or **Debug** above, or [customize the complete HUD in HUD Studio](observability/hud-studio.md).
<!-- configurable-hud-dev:end -->

```java
lens.startSession("Checkout");
lens.getByRole("button", "Place order")
        .waitUntilClickable()
        .click();
```

For the standard `click()` path, Test Lens also draws a temporary, labeled highlight around the resolved target. The highlight is pointer-transparent visual decoration: it neither activates the element nor makes it actionable. Each activation attempt still uses native `WebElement.click()`. An intercepted click may be followed by another native click after an explicit overlay policy handles a blocker, and locator recovery may start a fresh action attempt. There is no hidden JavaScript-click, Actions-click, ancestor-click, or state-mutation fallback.

HUD injection, updates, and cleanup are best effort and cannot change the result of the Selenium operation. The persistent record is the session trace—not the transient panel. [See the visual diagnostics contract](observability/visual-diagnostics.md) and [the exact click contract](elements/actions.md#click-contract-native-activation-visible-recovery).

## Signature capabilities

<div class="grid cards" markdown>

-   **Live HUD and target highlights**

    Follow actions, waits, assertions, network entries, and recovery diagnostics in the tested page. Highlighting shows which element was resolved without intercepting pointer input.

    [Visual diagnostics](observability/visual-diagnostics.md) · [Customize your HUD](observability/hud-studio.md)

-   **Managed and low-level authentication state**

    Use managed restore/validate/recreate with tri-state validation and atomic replacement, or call the underlying cookie and Web Storage capture/restore primitives directly. Persisted state remains sensitive authentication material.

    [Authentication state](advanced/auth-state.md)

-   **Managed Test State & Resources**

    Keep typed data inside one physical test invocation, share selected values intentionally inside one suite/run, and clean temporary resources exactly once in LIFO order across pass, failure, skip, and retry.

    [Managed test state](features/managed-test-state.md)

-   **React & SPA resilience**

    Re-resolve elements across rerenders, inspect common busy/loading DOM conventions, wait for roots and DOM stability, and work with known React Select markup through the optional module.

    [React/SPA resilience and its boundaries](features/react-spa.md)

-   **Application-aware waits**

    Reuse ready-state, limited XHR/fetch-idle, React-root, component-visibility, and DOM-stability waits instead of rebuilding JavaScript polling helpers in every project.

    [Waiting and readiness](elements/waiting.md)

-   **WebDriver BiDi network diagnostics**

    Observe and correlate requests, responses, redirects, and fetch errors; wait for expected traffic and assert over a valid capture snapshot. This is passive diagnostics, not interception or mocking.

    [Network diagnostics](advanced/network.md)

-   **Secrets redacted before diagnostic fan-out**

    Apply one policy to recognized credentials, sensitive fields, URLs, exception copies, network data, reports, and failure-bundle text before they reach HUD, trace, or sinks.

    [Redaction boundary](security/redaction.md)

-   **Runner-neutral facade, optional runner adapters**

    Use `TestLens` with any runner, or add the JUnit 5 extension or TestNG listener for per-invocation lifecycle, outcome mapping, evidence finalization, and owned-driver cleanup.

    [Framework integration](framework-integration.md)

-   **Reports, failure evidence, and explicit upload**

    Finalize once to produce a trace and HTML/JSON reports. Failed sessions can add screenshots and a ZIP evidence bundle; completed output can be streamed explicitly to your own HTTP endpoint.

    [Reports](observability/reports.md) · [Failure bundles](observability/failure-bundles.md) · [Report upload](observability/report-upload.md)

-   **[INTEGRATION] Allure**

    Keep Allure as the report UI and attach Test Lens diagnostic and clean screenshots, HTML, trace, and failure evidence through Allure's native lifecycle.

    [Allure integration](integrations/allure.md)

</div>

## Reuse authentication state with application validation

Managed Auth State restores persisted cookies and Web Storage, asks the application whether the result is authenticated, and recreates state only when validation unambiguously returns `UNAUTHENTICATED`:

```java
AuthStateEnsureResult result = lens.authState().ensure(
        AuthStateRequest.builder()
                .key("primary-user")
                .path(Path.of("target/test-auth/primary.json"))
                .login(driver -> loginPage.login(username, password))
                .validate(driver -> accountMenu.isDisplayed()
                        ? AuthStateValidation.AUTHENTICATED
                        : AuthStateValidation.UNAUTHENTICATED)
                .build());
```

Missing state becomes `CREATED`; valid restored state becomes `RESTORED`; invalid restored state is cleared from the managed browser context and atomically replaced as `REFRESHED`. One operation performs at most one login. `INCONCLUSIVE` fails without login and preserves the old file, which is important when the application or identity backend is unavailable rather than logged out.

The lower-level `AuthStateManager` capture/load/restore primitives remain available when the consumer owns validation and persistence sequencing. Default restore validates origin before browser mutation and rejects expired state. A redirect to another scheme, host, or effective port returns `ORIGIN_MISMATCH` before foreign-origin cookies or storage are changed.

This is controlled same-origin test setup, not cross-origin SSO replay or automatic logout. Auth-state JSON can contain live credentials and is intentionally not transformed by diagnostic text redaction, because changing it would make restoration unreliable. Store it as access-controlled authentication material. [Read the managed lifecycle, primitives, and security contract](advanced/auth-state.md).

## React and dynamic SPA behavior

The optional React module helps around DOM replacement and application-specific readiness signals without pretending to inspect React's component tree.

- `ReactSafeExecutor` re-finds the element for each configured attempt and can recover from stale, missing, or intercepted elements.
- React actionability checks can observe `aria-disabled`, `aria-busy`, `data-loading`, `data-pending`, progress bars, spinners, skeletons, focus locks, dialogs, and configured busy/blocking locators.
- `PageWaits` and `JsOverlayDebug` expose React-root, component-visibility, DOM-stability, and combined React/network waits with one total deadline.
- `ReactSelectHelper` supports specific React Select DOM/id conventions, including its JavaScript option-click path.

```java
JsOverlayDebug overlay = new JsOverlayDebug(driver);
overlay.waitForReactAndNetworkIdle(By.id("app"), Duration.ofSeconds(5));

ReactSafeExecutor react = ReactSupport.reactSafe(overlay);
react.clearAndType(By.cssSelector("[data-testid='search']"), "camera", "Search");
```

These are DOM-convention helpers. They do not inspect the React component tree, guarantee compatibility with every component library, or cross frame/window/shadow-root boundaries automatically. The legacy React `smartClick` has a separate contract; normal interaction should use `UiLocator.click()`. [Choose the React/SPA helpers deliberately](features/react-spa.md).

## Smarter waits and application readiness

Raw Selenium gives you `WebDriverWait` and `JavascriptExecutor`; Test Lens packages recurring browser/application observations behind reusable methods and records their progress through the same HUD and trace pipeline.

```java
lens.waitForPageReady(Duration.ofSeconds(5));
lens.waitForNetworkIdle(
        Duration.ofMillis(250),
        Duration.ofSeconds(5));

lens.getByTestId("results").waitUntilVisible();
lens.getByRole("button", "Save").waitUntilClickable();
```

| Wait | Actual contract |
|---|---|
| `waitForPageReady` | Polls one `document.readyState` value per observation and accepts only `complete`. It does not prove that SPA data has rendered. |
| `waitForInteractiveOrComplete` | Accepts `interactive` or `complete`; useful with non-default page-load strategies and asynchronous navigation. |
| `waitForNetworkIdle` | Installs an idempotent page tracker and requires zero observed XHR/fetch calls for the complete idle window. It sees only calls started after installation—not images, CSS, scripts, WebSocket, EventSource, beacon, or earlier traffic. |
| Locator waits | Re-resolve against the current DOM and poll visibility, hidden state, Selenium clickability, text, or collection count using configured timeout and interval. Clickability is not proof that an overlay cannot intercept the next click. |
| React/SPA waits | Observe roots, component visibility, a `MutationObserver`-based DOM-stability window, and combined React/network stages under one total deadline. |

Page-wait polling emits one started event and one terminal passed/failed event. An unmet deadline throws Selenium `TimeoutException`; terminal WebDriver/JavaScript failures stop immediately. Ordinary condition polling is not a recovery retry and does not mark the session flaky. [See all waiting contracts and limitations](elements/waiting.md).

## WebDriver BiDi network visibility

For browser-level traffic evidence, enable BiDi when creating the Selenium session and start Lens capture explicitly:

```java
ChromeOptions browserOptions = new ChromeOptions().enableBiDi();
WebDriver driver = new ChromeDriver(browserOptions);

TestLens lens = TestLens.attach(driver);
lens.startSession("orders");

NetworkDiagnostics network = lens.network().start(
        NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.BIDI)
                .hudFilter(NetworkHudFilter.defaults())
                .build());

driver.get(applicationUrl);
network.waitForResponse("/api/orders", 200);
network.assertNoFailedRequests();
```

The typed adapter passively records requests, completed responses, redirects, and fetch errors from Selenium's WebDriver BiDi network module. Events preserve request IDs and redirect correlation; immutable snapshots feed waits, assertions, HUD rows, JSON evidence, trace, and failure-bundle network summaries.

Capture must actually become active before an empty snapshot can satisfy `assertNoFailedRequests()`. `AUTO` and `BIDI` report unsupported/start-failure states rather than silently falling back to manual events or performance logs. Test Lens does not intercept, block, modify, mock, replay, or capture bodies, and it does not use CDP. [Configure capture, waits, assertions, limits, and HUD filtering](advanced/network.md).

## Diagnostics with a defined secret boundary

`RedactionPolicy.defaults()` is enabled before a structured log entry fans out to the HUD, session trace, built-in or external sinks. The same policy is applied at direct network, API-overlay, report, and failure-bundle text boundaries.

Defaults recognize common authorization, cookie, password, secret, token, session, CSRF, and XSRF fields. Text handling also covers Bearer and Basic credentials, JWT-shaped values, structured key/value forms, sensitive URL query values, userinfo, fragments, and caller-supplied literal secrets.

```java
RedactionPolicy redaction = RedactionPolicy.builder()
        .sensitiveKey("tenant-session")
        .secret(System.getenv("TEST_CLIENT_SECRET"))
        .build();

TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
        .redactionPolicy(redaction)
        .build());
```

Sinks receive redacted diagnostic copies, not the original throwable. Reports retain the original exception class as structural `exceptionType`, while safe copies provide redacted messages, causes, suppressed failures, and stack text. The original exception still controls the test result.

Text redaction is not pixel processing or general personal-data detection. `VisualRedactionOptions` separately masks configured screenshot regions and password inputs by default; unmasked screenshot regions and attached video can still expose sensitive data. Page source and browser console are opt-in, best-effort text boundaries, while auth-state files remain replayable secret material. `RedactionPolicy.disabled()` is a deliberate text-redaction opt-out. [Review the text boundary](security/redaction.md) and [visual-redaction contract](security/visual-redaction.md).

## Better Selenium APIs without hiding Selenium

Observability is the main reason to add Test Lens, but the same facade removes repeated plumbing from everyday browser tests.

| Area | Available API and behavior |
|---|---|
| Semantic locators | `getByRole`, `getByLabel`, `getByPlaceholder`, `getByAltText`, `getByText`, and `getByTestId`; role/name matching uses WebDriver-computed accessibility data rather than approximating the name from visible text. |
| Scoped queries | Lazy `locator(...)`, `filterHas(...)`, text/attribute filters, `nth`, `first`, and `last`; descendant queries remain inside their parent container. |
| Collections | Immutable current snapshots through `resolveAll()`/`count()` plus count waits that query one fresh snapshot per poll. |
| Native interactions | `click`, `fill`, `clear`, key presses, hover, double/right click, check/uncheck, upload, focus, scrolling, and native select operations. Each method documents whether it uses `WebElement`, Selenium `Actions`, or JavaScript. |
| Assertions | Polling assertions for element visibility/text/state, collection count, and page URL/title. Assertion polling is distinct from recovery retry. |
| Browser context | Frame, parent/default-content, window/tab, new-window, and alert helpers retain explicit WebDriver context ownership. |
| Actionability and overlays | Best-effort diagnostics plus explicit overlay policies for known blockers; no claim that every page-specific obstruction can be recovered. |
| Steps and evidence | Named steps, attachments, viewport or opt-in full-page screenshots, and existing video references feed the trace without replacing runner behavior. |
| Visual API helpers | Arrow-style element cues, upload feedback, and a caller-supplied API operation preview are available for specialized flows. The API modal does not discover or capture browser traffic. |

Use these APIs where they reduce boilerplate; use raw Selenium alongside them for lower-level operations. [Browse the complete capability map](capabilities.md) or [the public API catalog](reference/public-api-catalog.md).

## Semantic and scoped queries

Semantic factories make intent visible, while composition keeps the query relative to the selected UI region:

```java
UiLocator availableCards = lens.locator(By.cssSelector(".product-card"), "Product cards")
        .filterByAttribute("data-status", "available")
        .filterHas(lens.getByRole("button", "Buy"));

availableCards.first()
        .locator(lens.getByRole("button", "Buy"))
        .click();
```

`getByRole(role, accessibleName)` checks the browser/WebDriver-computed role and accessible name. The current implementation supports explicit roles and selected implicit candidates; it does not implement the ARIA naming algorithm itself or fall back to guessed text when typed accessibility commands fail.

Composed locators are immutable and lazy. Pipeline order matters, elements are not cached, and a child query cannot escape its parent container—even when a supplied XPath begins with `//`. Queries remain in the current frame/window and do not cross shadow roots automatically. [Read the semantic and containment rules](elements/locators.md) and [collection behavior](elements/collections.md).

## Recovery that remains visible

A recovered operation should not become indistinguishable from a clean pass. Test Lens separates three mechanisms:

1. **Condition polling** observes a state until it matches or times out. It does not mark the session flaky.
2. **Recovery retry** starts another physical operation attempt after a configured stale, intercepted, or not-interactable failure. It contributes to `RetrySummary`.
3. **Runner retry** starts another JUnit/TestNG invocation and therefore another Lens session.

`RetrySummary` reports total recovery retries, time lost in failed attempts that led to another attempt, and deterministic grouping by action, locator, and effective exception type. `RetryOutcomePolicy` can report only, add a warning, fail on any recovery retry, or fail after an allowed threshold.

```java
TestLensOptions options = TestLensOptions.builder()
        .retryOutcomePolicy(RetryOutcomePolicy.FAIL_AFTER_N)
        .allowedRetries(1)
        .build();

TestLens lens = TestLens.attach(driver, options);
```

A fail policy completes screenshots, reports, HUD cleanup, and the failure bundle before propagating `RetryPolicyViolationException`. This policy applies to recovery attempts—not normal wait/assertion polls. [Interpret flakiness evidence](observability/flakiness.md).

## From live execution to portable evidence

The same session connects what you saw during execution with what you inspect after it:

```text
action / wait / assertion / network event
        ↓
HUD and target decoration (transient)
        ↓
session trace and retry summary
        ↓
exactly-once PASSED / FAILED / SKIPPED finalization
        ↓
trace.json + report.html
        ↓ FAILED
diagnostic screenshot + clean screenshot + failure-bundle/ + ZIP
        ↓ optional and explicit
HTTP report upload
```

HTML is the human investigation view; JSON is the structured event model. A final failed session can collect diagnostic and clean screenshots, failure details, trace/report output, context, runtime and allowlisted configuration, network summary, component manifest, and a ZIP. Collectors are best effort: evidence failure does not replace the original test failure.

Screenshots use the viewport by default. Opt-in full-page capture uses bounded scroll-and-stitch without CDP, keeps the current responsive viewport width, and records completed dimensions/tile count. It does not expand iframe documents or nested scroll containers. Configured [visual redaction](security/visual-redaction.md) protects both viewport and stitched screenshot pixels; video is an attachment to an existing recording and is not modified.

`ReportUploader` can synchronously stream one completed ZIP after finalization. It supports a bearer token, controlled headers, SHA-256 checksum, deterministic idempotency key, bounded opt-in retry, and direct/system/explicit proxy selection with no-proxy rules. Upload is never implicit and never needs or closes WebDriver:

```java
TestLensFinalizationResult finalized = lens.finishPassed();

ReportUploadOptions uploadOptions = ReportUploadOptions.builder()
        .endpoint(URI.create("https://reports.example.test/api/test-lens/reports"))
        .bearerToken(System.getenv("TEST_LENS_REPORT_TOKEN"))
        .build();

new ReportUploader(uploadOptions)
        .upload(finalized)
        .requireSuccess();

driver.quit();
```

The lifecycle ordering is `finish → upload → quit`. Failed uploads leave local evidence unchanged. [Inspect reports](observability/reports.md), [configure failure bundles](observability/failure-bundles.md), [choose screenshot modes](observability/screenshots-evidence.md), and [implement the upload endpoint](observability/report-upload.md).

## Runner integration and ownership

The `TestLens` facade is runner-neutral. In a manual integration, your framework creates and closes the driver; Lens finalization never calls `quit()`.

The optional [JUnit 5 extension](integrations/junit5.md) and [TestNG listener](integrations/testng.md) create a driver through your configured factory for each managed invocation, map passed/failed/skipped outcomes, finalize Lens evidence, and close that adapter-owned driver exactly once. Parameterized, repeated, parallel, and runner-retried invocations retain separate session state. Neither adapter uploads reports automatically.

This lets an existing framework adopt the diagnostic lifecycle without replacing its runner model. [Compare manual, JUnit 5, and TestNG integration](framework-integration.md).

## Quick start

The latest stable release requires Java 17 or newer. Selenium remains an explicit dependency of the consuming project.

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.3.0</version>
</dependency>
```

Attach Lens to the driver your framework already owns, start one session, and finalize it before closing the driver:

```java
WebDriver driver = createExistingFrameworkDriver();
TestLens lens = TestLens.attach(driver);

try {
    lens.startSession("checkout");
    driver.get(applicationUrl);

    lens.getByLabel("Email").fill("person@example.test");
    lens.getByRole("button", "Continue").click();
    lens.getByTestId("confirmation").expect().toBeVisible();

    lens.finishPassed();
} catch (RuntimeException | Error failure) {
    lens.finishFailed(failure);
    throw failure;
} finally {
    driver.quit();
}
```

Finalization is first-writer-wins and exactly once for the session. Concurrent or repeated facade finalizers reuse the same completed result rather than recreating reports, screenshots, network cleanup, or bundles. [Continue with installation and lifecycle](getting-started.md).

## Explore the full surface

The homepage highlights the connected workflow rather than treating every helper as an equal product feature. Use the focused guides for complete contracts and edge cases:

- [Capabilities overview](capabilities.md)
- [Locators and semantic queries](elements/locators.md)
- [Interactions and form controls](elements/actions.md)
- [Assertions](elements/assertions.md)
- [Waits and application readiness](elements/waiting.md)
- [Visual diagnostics](observability/visual-diagnostics.md)
- [Trace and reports](observability/trace.md)
- [Failure evidence](observability/failure-bundles.md)
- [WebDriver BiDi network diagnostics](advanced/network.md)
- [Authentication state](advanced/auth-state.md)
- [React & SPA resilience](features/react-spa.md)
- [Browser context helpers](browser-context/index.md)
- [Select controls](elements/select-controls.md)
- [Advanced visual and API helpers](advanced/visual-helpers.md)
- [Configuration](configuration.md)
- [Public API catalog](reference/public-api-catalog.md)
- [Browser integration contracts](browser-integration-tests.md)
