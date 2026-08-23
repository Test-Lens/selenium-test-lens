---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.selenium.locator`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selenium.locator.UiLocator` {#io-github-testlens-selenium-locator-uilocator}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/elements/locators.md](../../elements/locators.md)

```java
public io.github.testlens.selenium.locator.UiLocator(org.openqa.selenium.WebDriver, org.openqa.selenium.By, java.lang.String, io.github.testlens.JsOverlayDebug, io.github.testlens.selenium.locator.UiLocatorOptions, io.github.testlens.core.OverlayLogger)
public io.github.testlens.selenium.locator.UiLocator click()
public io.github.testlens.selenium.locator.UiLocator fill(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator clear()
public io.github.testlens.selenium.locator.UiLocator pressEnter()
public io.github.testlens.selenium.locator.UiLocator press(java.lang.CharSequence...)
public io.github.testlens.selenium.locator.UiLocator selectByVisibleText(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator selectByValue(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator selectByIndex(int)
public java.lang.String selectedText()
public java.lang.String selectedValue()
public io.github.testlens.selenium.locator.UiLocator hover()
public io.github.testlens.selenium.locator.UiLocator doubleClick()
public io.github.testlens.selenium.locator.UiLocator rightClick()
public java.lang.String textContent()
public boolean isVisible()
public boolean isEnabled()
public java.lang.String attribute(java.lang.String)
public java.lang.String property(java.lang.String)
public java.lang.String value()
public java.util.List<org.openqa.selenium.WebElement> resolveAll()
public int count()
public io.github.testlens.selenium.locator.UiLocator nth(int)
public io.github.testlens.selenium.locator.UiLocator first()
public io.github.testlens.selenium.locator.UiLocator last()
public io.github.testlens.selenium.locator.UiLocator waitUntilVisible()
public io.github.testlens.selenium.locator.UiLocator waitUntilHidden()
public io.github.testlens.selenium.locator.UiLocator waitUntilClickable()
public io.github.testlens.selenium.locator.UiLocator waitUntilText(java.lang.String)
public io.github.testlens.selenium.assertions.UiExpect expect()
public io.github.testlens.selenium.assertions.UiExpect expect(io.github.testlens.selenium.assertions.UiAssertionOptions)
public org.openqa.selenium.WebElement resolve()
public io.github.testlens.selenium.actionability.ActionabilityReport checkActionability()
public io.github.testlens.selenium.actionability.ActionabilityReport checkActionability(io.github.testlens.selenium.actionability.ActionabilityOptions)
public org.openqa.selenium.By by()
public java.lang.String description()
```

## `io.github.testlens.selenium.locator.UiLocatorDescription` {#io-github-testlens-selenium-locator-uilocatordescription}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public static io.github.testlens.selenium.locator.UiLocatorDescription of(org.openqa.selenium.By, java.lang.String)
public org.openqa.selenium.By by()
public java.lang.String label()
public java.lang.String displayName()
public java.lang.String toString()
```

## `io.github.testlens.selenium.locator.UiLocatorException` {#io-github-testlens-selenium-locator-uilocatorexception}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/result-types.md](../../reference/result-types.md)

```java
public io.github.testlens.selenium.locator.UiLocatorException(java.lang.String, java.lang.String, java.lang.String, java.lang.Throwable, java.lang.String)
public java.lang.String action()
public java.lang.String locatorDescription()
public java.lang.String actionabilitySummary()
```

## `io.github.testlens.selenium.locator.UiLocatorFailureReason` {#io-github-testlens-selenium-locator-uilocatorfailurereason}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.locator.UiLocatorFailureReason NOT_FOUND
public static final io.github.testlens.selenium.locator.UiLocatorFailureReason STALE_ELEMENT
public static final io.github.testlens.selenium.locator.UiLocatorFailureReason CLICK_INTERCEPTED
public static final io.github.testlens.selenium.locator.UiLocatorFailureReason NOT_INTERACTABLE
public static final io.github.testlens.selenium.locator.UiLocatorFailureReason ACTIONABILITY_NOT_READY
public static final io.github.testlens.selenium.locator.UiLocatorFailureReason TIMEOUT
public static final io.github.testlens.selenium.locator.UiLocatorFailureReason UNKNOWN
public static io.github.testlens.selenium.locator.UiLocatorFailureReason[] values()
public static io.github.testlens.selenium.locator.UiLocatorFailureReason valueOf(java.lang.String)
```

## `io.github.testlens.selenium.locator.UiLocatorOptions$Builder` {#io-github-testlens-selenium-locator-uilocatoroptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/configuration.md](../../reference/configuration.md)

```java
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder actionabilityOptions(io.github.testlens.selenium.actionability.ActionabilityOptions)
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder timeout(java.time.Duration)
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder pollInterval(java.time.Duration)
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder maxRetries(int)
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder retryOnStaleElement(boolean)
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder retryOnClickIntercepted(boolean)
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder retryOnNotInteractable(boolean)
public io.github.testlens.selenium.locator.UiLocatorOptions$Builder highlightBeforeAction(boolean)
public io.github.testlens.selenium.locator.UiLocatorOptions build()
```

## `io.github.testlens.selenium.locator.UiLocatorOptions` {#io-github-testlens-selenium-locator-uilocatoroptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/configuration.md](../../reference/configuration.md)

```java
public static io.github.testlens.selenium.locator.UiLocatorOptions defaults()
public static io.github.testlens.selenium.locator.UiLocatorOptions$Builder builder()
public io.github.testlens.selenium.actionability.ActionabilityOptions actionabilityOptions()
public java.time.Duration timeout()
public java.time.Duration pollInterval()
public int maxRetries()
public boolean retryOnStaleElement()
public boolean retryOnClickIntercepted()
public boolean retryOnNotInteractable()
public boolean highlightBeforeAction()
```

## `io.github.testlens.selenium.locator.UiLocatorResolver` {#io-github-testlens-selenium-locator-uilocatorresolver}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.locator.UiLocatorResolver(org.openqa.selenium.WebDriver)
public org.openqa.selenium.WebElement resolve(org.openqa.selenium.By, io.github.testlens.selenium.locator.UiLocatorOptions)
public io.github.testlens.selenium.locator.UiLocatorResult resolveResult(org.openqa.selenium.By, io.github.testlens.selenium.locator.UiLocatorOptions)
```

## `io.github.testlens.selenium.locator.UiLocatorResult$Builder` {#io-github-testlens-selenium-locator-uilocatorresult-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.selenium.locator.UiLocatorResult$Builder failureReason(io.github.testlens.selenium.locator.UiLocatorFailureReason)
public io.github.testlens.selenium.locator.UiLocatorResult$Builder action(java.lang.String)
public io.github.testlens.selenium.locator.UiLocatorResult$Builder description(java.lang.String)
public io.github.testlens.selenium.locator.UiLocatorResult$Builder attempts(int)
public io.github.testlens.selenium.locator.UiLocatorResult$Builder elapsed(java.time.Duration)
public io.github.testlens.selenium.locator.UiLocatorResult$Builder message(java.lang.String)
public io.github.testlens.selenium.locator.UiLocatorResult build()
```

## `io.github.testlens.selenium.locator.UiLocatorResult` {#io-github-testlens-selenium-locator-uilocatorresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static io.github.testlens.selenium.locator.UiLocatorResult$Builder builder(io.github.testlens.selenium.locator.UiLocatorStatus)
public io.github.testlens.selenium.locator.UiLocatorStatus status()
public io.github.testlens.selenium.locator.UiLocatorFailureReason failureReason()
public java.lang.String action()
public java.lang.String description()
public int attempts()
public java.time.Duration elapsed()
public java.lang.String message()
public boolean passed()
```

## `io.github.testlens.selenium.locator.UiLocatorSelectors` {#io-github-testlens-selenium-locator-uilocatorselectors}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public static java.lang.String xpathLiteral(java.lang.String)
public static java.lang.String normalizeSpaceExpression(java.lang.String)
public static java.lang.String cssAttributeEquals(java.lang.String, java.lang.String)
public static java.lang.String cssString(java.lang.String)
```

## `io.github.testlens.selenium.locator.UiLocatorStatus` {#io-github-testlens-selenium-locator-uilocatorstatus}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.selenium.locator`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `enum`

```java
public static final io.github.testlens.selenium.locator.UiLocatorStatus PASSED
public static final io.github.testlens.selenium.locator.UiLocatorStatus FAILED
public static final io.github.testlens.selenium.locator.UiLocatorStatus RETRYING
public static io.github.testlens.selenium.locator.UiLocatorStatus[] values()
public static io.github.testlens.selenium.locator.UiLocatorStatus valueOf(java.lang.String)
```
