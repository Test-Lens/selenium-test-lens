# UI Test Lens API migration guide

This guide summarizes the migration from the historical JsTestTools-style helper codebase to the current UI Test Lens 0.1.0-SNAPSHOT API.

UI Test Lens is still pre-1.0. Some API names, helper visibility, and compatibility surfaces may still change before a stable release. This guide describes the current 0.1 state and is not a Maven Central release guide.

## Naming migration

| Historical / old concept | Current UI Test Lens concept |
| ------------------------ | ---------------------------- |
| `JsTestTools` | `UI Test Lens` |
| old package / historical helper package | `io.github.mmaciekk111.uitestlens` |
| old one-module layout | multi-module Maven layout |
| Selenium-only helper | Selenium-compatible diagnostics/reliability layer |
| hardcoded overlay/popup handling | configurable `OverlayPolicy` |
| one-off Selenium element click | retryable `UiLocator` |
| one-off assertions | retryable `UiExpect` |
| ad-hoc step logging | `step(...)` DSL |
| manual screenshots/links | trace/evidence artifacts |
| repeated login flow | auth/session state capture and restore |
| ad-hoc network checks | passive network diagnostics |

## Maven module migration

| Use case | Recommended artifact |
| -------- | -------------------- |
| logging/event model only | `ui-test-lens-core` |
| runtime browser overlay resources | `ui-test-lens-overlay` |
| Selenium actions/locators/assertions/evidence/auth/network | `ui-test-lens-selenium` |
| React helpers/readiness checks | `ui-test-lens-react` |
| all-in-one usage | `ui-test-lens` |
| documentation examples | `ui-test-lens-examples`, but not as a runtime dependency |

All-in-one local usage:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Modular Selenium usage:

```xml
<dependency>
    <groupId>io.github.mmaciekk111</groupId>
    <artifactId>ui-test-lens-selenium</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Maven Central publication is not configured yet. For local usage, install the project with `mvn install` when the local Maven environment and certificate/PKIX setup allow dependency resolution.

## Package migration

The current Java package root is:

```text
io.github.mmaciekk111.uitestlens
```

Responsibilities are split by module and package area:

- `core.logging`
- `core.trace`
- `selenium.locator`
- `selenium.assertions`
- `selenium.business`
- `selenium.steps`
- `selenium.evidence`
- `selenium.auth`
- `selenium.network`
- `react`
- `react.actionability`

Main Selenium facade import:

```java
import io.github.mmaciekk111.uitestlens.JsOverlayDebug;
```

## Common migration recipes

### 6.1 Basic Selenium overlay usage

Before: historical helper or direct Selenium style code manually updated the overlay/log state around test actions.

After:

```java
WebDriver driver = ...;

JsOverlayDebug overlay = new JsOverlayDebug(driver);

overlay.setStep("Open checkout");
overlay.hudLog("Opening checkout page");
```

### 6.2 Replace direct Selenium click with retryable locator

Before:

```java
driver.findElement(By.cssSelector("[data-testid='save']")).click();
```

After:

```java
overlay.getByTestId("save").click();
```

Or:

```java
overlay.locator(By.cssSelector("[data-testid='save']")).click();
```

### 6.3 Replace one-off assertion with retryable assertion

Before:

```java
assertEquals("Saved", driver.findElement(By.cssSelector("[data-testid='toast']")).getText());
```

After:

```java
overlay.expect(overlay.getByTestId("toast"))
        .toHaveText("Saved");
```

### 6.4 Add overlay/popup handling

After:

```java
OverlayPolicy policy = OverlayPolicy.builder()
        .handler(OverlayHandler.builder("Cookie consent")
                .detect(By.cssSelector("[data-testid='cookie-banner']"))
                .action(OverlayAction.click(By.cssSelector("[data-testid='accept-cookies']")))
                .optional(true)
                .build())
        .build();

overlay.setOverlayPolicy(policy);
```

### 6.5 Add business assertions

After:

```java
overlay.business("Order summary")
        .check("shows total", () -> overlay.getByTestId("order-total").expect().toHaveText("123.00 PLN"))
        .check("contains product", () -> overlay.getByTestId("product-name").expect().toContainText("Premium"))
        .verify();
```

### 6.6 Add named steps

After:

```java
overlay.step("Save order", () -> {
    overlay.getByTestId("save-order").click();
    overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
});
```

### 6.7 Add trace/evidence report

After:

```java
UiTestLensSession session = overlay.startSession("Checkout flow");

overlay.step("Save order", () -> {
    overlay.getByTestId("save-order").click();
});

overlay.captureScreenshot("After save");
overlay.exportTraceHtml(Path.of("target/ui-test-lens/checkout-flow.html"));
```

### 6.8 Attach video artifact

After:

```java
overlay.attachVideoUrl(
        "CI video",
        "https://ci.example.com/artifacts/checkout-flow.mp4"
);
```

### 6.9 Save and restore auth/session state

After:

```java
AuthState state = overlay.auth().captureState(AuthStateOptions.builder()
        .label("standard-customer")
        .role("customer")
        .build());

state.save(Path.of("target/ui-test-lens/auth/customer.json"));
```

Restore:

```java
AuthState state = AuthState.load(Path.of("target/ui-test-lens/auth/customer.json"));

overlay.auth().restoreState(state, AuthRestoreOptions.builder()
        .navigateToOrigin(true)
        .build());
```

### 6.10 Add passive network diagnostics

After:

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

Real browser network capture providers are not implemented yet. The current supported baseline is the manual/fallback collector over `NetworkEvent` data.

## React migration

React-specific APIs live in `ui-test-lens-react`. The Selenium module does not depend on React; the React module depends on the Selenium module as an extension layer.

If older code used a Selenium-side React-safe shortcut, use React-side entry points instead:

```java
ReactSafeExecutor react = ReactSupport.reactSafe(overlay);

ReactSupport.checkActionability(
        overlay,
        By.cssSelector("[data-testid='save']"),
        ReactActionabilityOptions.builder().build()
);
```

Use `ReactSupport.reactSafe(overlay)` instead of Selenium facade methods for React-safe execution.

## Evidence and reports migration

Evidence APIs separate existing artifact references from active capture:

- `attachScreenshot(...)` attaches an existing screenshot path to the current trace session.
- `captureScreenshot(...)` captures a Selenium screenshot through `TakesScreenshot`.
- `attachVideoFile(...)` attaches an existing local video file reference.
- `attachVideoUrl(...)` attaches an existing remote/CI video URL reference.
- Video recording is not implemented yet.
- `exportTraceHtml(...)` generates an HTML report from the current `UiTestLensSession`.

Prefer explicit video APIs (`attachVideoFile(...)` and `attachVideoUrl(...)`) in new code.

## Auth/session safety migration

Auth state JSON can contain cookies and tokens. The recommended path is:

```text
target/ui-test-lens/auth/
```

This path is ignored by the project. Do not commit generated auth state files. UI Test Lens 0.1 does not encrypt auth state, does not model passwords as dedicated fields, and does not implement login flows.

Some applications may need a refresh or navigation after restore, depending on how they initialize client-side auth state.

## Network migration limitations

The network layer is passive diagnostics only:

- no mocking,
- no route fulfillment,
- no interception,
- `PERFORMANCE_LOGS` and `BIDI` are modeled but unsupported/fallback,
- the reliable current baseline is the manual/fallback collector,
- real browser network capture providers should be added later behind guarded support.

## Removed or isolated historical dependencies

The current module split removed or isolated private/historical utilities from the main artifact path:

- `LogWraper`,
- `TimeStamp`,
- `ContentIssueCollector`,
- `LocalDateTimeUtils`.

RestAssured is removed from the main artifact path. The overlay module no longer depends on Selenium. The Selenium module no longer depends on React. The core module remains Selenium-free.

## Pre-1.0 caveats

- API may change before a stable release.
- Some helper visibility may be reduced.
- Legacy helper classes may be moved or internalized.
- `attachVideo(...)` naming may be cleaned up in favor of explicit video file/URL APIs.
- `getByRole`, `getByLabel`, and `getByText` are not implemented yet.
- Real browser network capture is not implemented yet.
- Maven Central release is not configured yet.
