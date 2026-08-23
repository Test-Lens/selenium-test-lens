---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.actions`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.actions.AssertActions$OverlayAssertionResult` {#io-github-testlens-actions-assertactions-overlayassertionresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.AssertActions$OverlayAssertionResult(boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, long)
public boolean isSuccess()
public java.lang.String getAssertionType()
public java.lang.String getContext()
public java.lang.String getExpected()
public java.lang.String getActual()
public java.lang.String getMessage()
public long getTimestampMillis()
public java.lang.String toMessage()
public java.lang.String toJson()
```

## `io.github.testlens.actions.AssertActions` {#io-github-testlens-actions-assertactions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.AssertActions(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig, io.github.testlens.hud.HudPanel)
public io.github.testlens.actions.AssertActions(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig, io.github.testlens.hud.HudPanel, io.github.testlens.core.OverlayLogger)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertTextEqualsModified(org.openqa.selenium.WebElement, java.lang.String, java.util.function.Function<java.lang.String, java.lang.String>, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertTextContainsModified(org.openqa.selenium.WebElement, java.lang.String, java.util.function.Function<java.lang.String, java.lang.String>, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertTextEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertTextContains(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertAttributeEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertCssEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertColorEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertHasClass(org.openqa.selenium.WebElement, java.lang.String, boolean, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertVisible(org.openqa.selenium.WebElement, boolean, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertEnabled(org.openqa.selenium.WebElement, boolean, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertSelected(org.openqa.selenium.WebElement, boolean, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertEquals(java.lang.Object, java.lang.Object, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertNotEquals(java.lang.Object, java.lang.Object, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertContains(java.lang.String, java.lang.String, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertNotContains(java.lang.String, java.lang.String, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertTrue(boolean, java.lang.String)
public io.github.testlens.actions.AssertActions$OverlayAssertionResult assertFalse(boolean, java.lang.String)
```

## `io.github.testlens.actions.HighlightActions` {#io-github-testlens-actions-highlightactions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.HighlightActions(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig)
public io.github.testlens.actions.HighlightActions(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayLogger)
public void highlightClick(org.openqa.selenium.WebElement, java.lang.String)
public void highlightParent(org.openqa.selenium.WebElement, int, java.lang.String)
public void highlightClosest(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
```

## `io.github.testlens.actions.ScrollActions` {#io-github-testlens-actions-scrollactions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.ScrollActions(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager)
public io.github.testlens.actions.ScrollActions(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager, io.github.testlens.core.OverlayLogger)
public void scrollToElementWithArrow(org.openqa.selenium.WebElement)
public void scrollToElementWithArrow(org.openqa.selenium.WebElement, long)
public void scrollToElementWithArrow(org.openqa.selenium.WebElement, long, io.github.testlens.scroll.ScrollElementEdge, io.github.testlens.scroll.ScrollViewportEdge)
```

## `io.github.testlens.actions.SmartClickActions` {#io-github-testlens-actions-smartclickactions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.SmartClickActions(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager, io.github.testlens.actions.HighlightActions)
public io.github.testlens.actions.SmartClickActions(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager, io.github.testlens.actions.HighlightActions, io.github.testlens.core.OverlayLogger)
public void setOverlayPolicy(io.github.testlens.selenium.overlay.OverlayPolicy)
public void clickWithOverlayHandling(org.openqa.selenium.WebElement, java.lang.String)
public void smartClick(org.openqa.selenium.WebElement, java.lang.String)
```

## `io.github.testlens.actions.SmartInputActions` {#io-github-testlens-actions-smartinputactions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.SmartInputActions(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager, io.github.testlens.actions.TypingActions)
public io.github.testlens.actions.SmartInputActions(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayRootManager, io.github.testlens.actions.TypingActions, io.github.testlens.core.OverlayLogger)
public void smartTypeWithHint(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
```

## `io.github.testlens.actions.TargetResolverActions` {#io-github-testlens-actions-targetresolveractions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.TargetResolverActions(org.openqa.selenium.WebDriver)
public io.github.testlens.actions.TargetResolverActions(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayLogger)
public org.openqa.selenium.WebElement resolveClickTarget(org.openqa.selenium.WebElement)
public org.openqa.selenium.WebElement resolveFileInputTarget(org.openqa.selenium.WebElement)
public java.lang.String resolveClickTargetSelector(org.openqa.selenium.WebElement)
public java.lang.String resolveFileInputSelector(org.openqa.selenium.WebElement)
```

## `io.github.testlens.actions.TypingActions` {#io-github-testlens-actions-typingactions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.actions`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.actions.TypingActions(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig)
public io.github.testlens.actions.TypingActions(org.openqa.selenium.WebDriver, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig, io.github.testlens.core.OverlayLogger)
public void typeWithHint(org.openqa.selenium.WebElement, java.lang.String)
public void clearAndType(org.openqa.selenium.WebElement, java.lang.String)
```
