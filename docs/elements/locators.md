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
lens.locator(By.id("terms"), "Terms").check();
lens.locator(By.id("attachment"), "Attachment").upload(Path.of("document.pdf"));
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

Matches an explicit or supported implicit role confirmed by WebDriver's computed `getAriaRole()`. The name overload compares the normalized, browser-computed `WebElement.getAccessibleName()` exactly and case-sensitively. Test Lens does not implement the accessibility-name algorithm itself and does not fall back to `aria-label`, text, title, or placeholder when the WebDriver endpoint fails.

```java
lens.getByRole("button", "Save").click();
```

Supported implicit candidate roles remain `button`, `link`, `textbox`, `checkbox`, and `radio`. The locator is lazy, preserves DOM order, and supports `count()`, `nth()`, `first()`, and `last()`.

## Label, placeholder, and alt text

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
UiLocator getByLabel(String label)
UiLocator getByPlaceholder(String placeholder)
UiLocator getByAltText(String altText)
```

`getByLabel` narrows candidates to native label relationships (`label[for]`, nesting, including multiple labels) or explicit `aria-label`/`aria-labelledby`, then compares their browser-computed accessible name. A title, placeholder, button text, or neighboring text alone is not a label source.

`getByPlaceholder` performs an exact placeholder-attribute match and does not treat it as a label. `getByAltText` matches a normalized real `alt` attribute on `img`, `area`, or `input[type=image]`; it does not use ARIA labels, title, SVG title, CSS, or adjacent text. Empty `alt=""` is searchable, though it normally denotes decorative content. Null alt text is rejected.

```java
lens.getByLabel("Accept terms").check();
lens.getByLabel("Attachment").upload(Path.of("document.pdf"));
lens.getByPlaceholder("Email address").fill("person@example.test");
lens.getByAltText("Company logo").waitUntilVisible();
```

Semantic comparison trims and collapses Unicode whitespace (including NBSP) to one space, then compares exactly and case-sensitively. Results depend on the browser/WebDriver accessibility implementation; unsupported typed accessibility commands fail without a hidden DOM or JavaScript fallback.

## UiLocator constructor

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator(WebDriver driver, By by, String label, JsOverlayDebug overlay, UiLocatorOptions options, OverlayLogger logger)
```

Advanced constructor for custom integration. Normal users obtain instances from `TestLens`; directly constructing one couples code to low-level overlay/logging types.

Related: [actions](actions.md), [waiting](waiting.md), [configuration](../reference/configuration.md#uilocatoroptions).
