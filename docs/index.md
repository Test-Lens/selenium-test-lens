---
hide:
  - navigation
  - toc
---

<div class="lens-hero" markdown>

# Selenium Test Lens

**An observability and failure-evidence layer for Selenium WebDriver.**

Test Lens attaches to the driver your project already owns. It keeps Selenium's native behavior visible while recording structured diagnostics and preserving evidence that is useful after a failure.

[Get started](getting-started.md){ .md-button .md-button--primary }
[Why Test Lens](#why-test-lens){ .md-button }
[Reports](observability/reports.md){ .md-button }
[Maven Central](https://central.sonatype.com/artifact/io.github.test-lens/selenium-test-lens/0.2.0){ .md-button }

| Documentation | Status | Library availability |
|---|---|---|
| [0.2.0 stable](https://test-lens.github.io/selenium-test-lens/0.2.0/) | Latest published release | Maven Central |
| [0.1.0 historical](https://test-lens.github.io/selenium-test-lens/0.1.0/) | Previous release | Maven Central |

</div>

## The observable test path

```text
Attach an existing WebDriver
→ locate semantically and within scope
→ interact while HUD and trace observe
→ poll assertions and waits
→ finalize once
→ inspect a report or failure bundle
```

Test Lens does not replace Selenium, your test runner, Page Objects, or an existing reporting stack. Adopt it where clearer interactions and better diagnostics are useful; keep using raw WebDriver when it provides the control you need.

## Why Test Lens?

### Native interactions with visible recovery

```java
lens.getByRole("button", "Save").click();
```

The ordinary `UiLocator.click()` is the recommended path. HUD and trace observe it, while highlighting remains pointer-transparent visual decoration. Each activation attempt uses native `WebElement.click()`. An intercepted click may be followed by another native click after explicit overlay recovery, and the locator retry policy may start a fresh action attempt. There is no JavaScript, Actions, ancestor-click, or hidden state-mutation fallback. [Read the exact interaction contract](elements/actions.md#click-contract-native-activation-visible-recovery).

### Semantic and scoped queries

Release `0.2.0` adds browser-computed accessibility matching, expanded semantic factories, and locator composition to the test-id, text, and role-oriented entry points introduced in `0.1.0`.

```java
UiLocator cards = lens.locator(By.cssSelector(".product-card"))
        .filterByAttribute("data-status", "available")
        .filterHas(lens.getByRole("button", "Buy"));

cards.first().locator(lens.getByRole("button", "Buy")).click();
```

The query stays lazy and ordered. A child query cannot escape its parent container—even when user XPath starts with `//`—and browser/WebDriver accessibility semantics are not approximated from visible text alone. [Use semantic and scoped locators](elements/locators.md).

### Measurable recovery instead of silent flaky passes

> A passed test can still tell you it was flaky.

Recovery retry is recorded separately from condition polling and runner retry. You can report recovered attempts, warn about them, or reject an otherwise passed outcome after evidence is written. [Understand recovery and flakiness](observability/flakiness.md).

### Trace, reports, and automatic failure evidence

Trace and HTML/JSON reports have existed since `0.1.0`; they connect actions, waits, assertions, and lifecycle outcomes. Automatic failure evidence builds on that foundation:

```text
operation → session trace → finalization → HTML/JSON report → failure bundle
```

A final failed session can collect screenshots, reports, context, runtime/configuration allowlists, and a network summary. Collection is best-effort, finalization does not close WebDriver, and video is caller-supplied evidence rather than an automatic recording. [Follow the trace](observability/trace.md), [inspect reports](observability/reports.md), or [configure failure bundles](observability/failure-bundles.md).

### Safe diagnostics through central redaction

One immutable policy protects diagnostic copies before they fan out to HUD, trace, log sinks, reports, network/API diagnostics, and failure-bundle text files. Original exception types remain structural diagnostics, while the original throwable continues to control the test result. Screenshots and video are not pixel-redacted, auth-state files remain replayable and outside this transformation, and `disabled()` is a deliberate opt-out. [Review the security boundary](security/redaction.md).

## Quick start

The stable release requires Java 17 or newer:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.2.0</version>
</dependency>
```

```java
WebDriver driver = createExistingFrameworkDriver();
TestLens lens = TestLens.attach(driver);
try {
    lens.startSession("login");
    driver.get(applicationUrl);
    lens.locator(By.id("login"), "Login").click();
    lens.locator(By.id("welcome"), "Welcome").expect().toBeVisible();
    lens.finishPassed();
} catch (RuntimeException | Error failure) {
    lens.finishFailed(failure);
    throw failure;
} finally {
    driver.quit();
}
```

[Continue with installation and lifecycle](getting-started.md). Selenium remains an explicit consumer dependency, and your application or runner remains responsible for closing the driver.

## Advanced capabilities

| Capability | What it does—and does not do | Guide |
|---|---|---|
| WebDriver BiDi network diagnostics | Passively observes and correlates traffic; it is not interception, mocking, or CDP. Manual network events/waits/assertions existed in `0.1.0`; BiDi lifecycle and safe snapshots are `0.2.0`. | [Network](advanced/network.md) |
| Auth state | Restores cookies and storage only after origin validation; it is not automatic cross-origin SSO storage handling. | [Auth state](advanced/auth-state.md) |
| Page and SPA waits | Observes ready state and a limited XHR/fetch tracker; network idle does not cover every browser resource. | [Waiting](elements/waiting.md) |
| React helpers | Adds optional React-oriented waits and helpers; its legacy `smartClick` is separate from `UiLocator.click()`. | [React](integrations/react.md) |
| API overlay | Displays caller-supplied previews; it does not capture network traffic. | [Visual helpers](advanced/visual-helpers.md) |

## Integrations

The main library works with any runner. The optional JUnit 5 and TestNG lifecycle adapters create one driver/Lens pair per invocation, map runner outcomes, finalize evidence, and then close the driver. [Choose an integration model](framework-integration.md).

## Reference

- [Capability map](capabilities.md)
- [Element API](elements/locators.md)
- [Configuration](configuration.md)
- [Observability](observability/index.md)
- [Public API catalog](reference/public-api-catalog.md)
- [Migration from raw Selenium](migration.md)
- [Browser integration contracts](browser-integration-tests.md)
