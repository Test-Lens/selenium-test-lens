# Locators

Package: `io.github.testlens.selenium.locator`<br>
Module: `selenium-test-lens-selenium`<br>
API level: **Recommended**

Create locators from an attached [`TestLens`](../reference/test-lens.md). Labels appear in trace, logs, HUD, and errors; they do not change matching.

## locator(By by)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiLocator locator(By by)
```

Requires a non-null Selenium `By`. The diagnostic label is derived from the locator. Returns a lazy `UiLocator`; no DOM lookup occurs at creation.

## locator(By by, String label)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiLocator locator(By by, String label)
```

Uses any Selenium locator and the supplied user-facing label. Blank labels fall back to the selector description. Failure behavior and retry settings apply when the returned locator is used.

```java
lens.locator(By.cssSelector("button.save"), "Save order").click();
```

## getByTestId(String testId)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiLocator getByTestId(String testId)
```

Builds a CSS selector for `data-testid`. The selector helper escapes CSS-sensitive content. Use stable, non-secret test IDs.

## getByText overloads

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiLocator getByText(String text)
UiLocator getByText(String text, String label)
UiLocator getByTextContaining(String text)
```

The first two match normalized exact text; the third matches a normalized substring. Text helpers use XPath over rendered DOM text and can be broader/slower than a stable ID or CSS locator. Null input is rejected by selector construction.

## getByRole overloads

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiLocator getByRole(String role)
UiLocator getByRole(String role, String accessibleName)
```

Matches an explicit role or the library's supported implicit-role mapping. The name overload compares `aria-label` or normalized element text. It is **not** the complete ARIA accessible-name algorithm: it does not promise full `aria-labelledby`, subtree, hidden-content, or host-language computation.

```java
lens.getByRole("button", "Save").click();
```

## UiLocator constructor

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator(WebDriver driver, By by, String label, JsOverlayDebug overlay, UiLocatorOptions options, OverlayLogger logger)
```

Advanced constructor for custom integration. Normal users obtain instances from `TestLens`; directly constructing one couples code to low-level overlay/logging types.

Related: [actions](actions.md), [waiting](waiting.md), [configuration](../reference/configuration.md#uilocatoroptions).
