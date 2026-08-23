---
search:
  exclude: true
---

# selenium-test-lens-react: `io.github.testlens.react`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.react.ReactOverlaySupport` {#io-github-testlens-react-reactoverlaysupport}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react`
- Classification: `ADVANCED_API`
- Type kind: `interface`

```java
public abstract io.github.testlens.OverlayConfig getConfig()
public abstract void setStep(java.lang.String)
public abstract org.openqa.selenium.WebElement highlightElement(org.openqa.selenium.WebElement, java.lang.String)
```

## `io.github.testlens.react.ReactSafeExecutor` {#io-github-testlens-react-reactsafeexecutor}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.react.ReactSafeExecutor(org.openqa.selenium.WebDriver, io.github.testlens.react.ReactOverlaySupport, int, java.time.Duration, java.time.Duration)
public io.github.testlens.react.ReactSafeExecutor(org.openqa.selenium.WebDriver, io.github.testlens.react.ReactOverlaySupport)
public <T> T doWithRetry(org.openqa.selenium.By, java.lang.String, java.util.function.Function<org.openqa.selenium.WebElement, T>)
public void click(org.openqa.selenium.By, java.lang.String)
public void clearAndType(org.openqa.selenium.By, java.lang.String, java.lang.String)
public java.lang.String getText(org.openqa.selenium.By, java.lang.String)
public java.lang.String getAttribute(org.openqa.selenium.By, java.lang.String, java.lang.String)
public boolean isDisplayed(org.openqa.selenium.By, java.lang.String)
public boolean isEnabled(org.openqa.selenium.By, java.lang.String)
public boolean isSelected(org.openqa.selenium.By, java.lang.String)
public io.github.testlens.react.ReactSelectHelper select()
```

## `io.github.testlens.react.ReactSelectHelper` {#io-github-testlens-react-reactselecthelper}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.react.ReactSelectHelper(org.openqa.selenium.WebDriver)
public java.lang.String resolveReactSelectBaseId(org.openqa.selenium.WebElement, org.openqa.selenium.WebElement)
public boolean jsClickReactSelectOptionContaining(java.lang.String, java.lang.String)
public void pickByLabel(io.github.testlens.react.ReactOverlaySupport, java.lang.String, java.lang.String, java.lang.String, long, java.lang.String)
public java.lang.String textContent(org.openqa.selenium.WebElement)
public static java.lang.String xpathLiteral(java.lang.String)
public static java.lang.String cssEscape(java.lang.String)
```

## `io.github.testlens.react.ReactSupport` {#io-github-testlens-react-reactsupport}

- Artifact/module: `selenium-test-lens-react`
- Package: `io.github.testlens.react`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public static io.github.testlens.react.ReactSafeExecutor reactSafe(io.github.testlens.JsOverlayDebug)
public static io.github.testlens.react.ReactOverlaySupport overlaySupport(io.github.testlens.JsOverlayDebug)
public static io.github.testlens.react.actionability.ReactActionabilityChecker actionability(io.github.testlens.JsOverlayDebug)
public static io.github.testlens.react.actionability.ReactActionabilityReport checkActionability(io.github.testlens.JsOverlayDebug, org.openqa.selenium.By, io.github.testlens.react.actionability.ReactActionabilityOptions)
public static void smartClick(io.github.testlens.JsOverlayDebug, org.openqa.selenium.By, java.lang.String)
public static org.openqa.selenium.WebElement findBySelectorContainingText(io.github.testlens.JsOverlayDebug, java.lang.String, java.lang.String, boolean, java.lang.String)
public static org.openqa.selenium.WebElement findFirst(io.github.testlens.JsOverlayDebug, org.openqa.selenium.By, java.util.function.Predicate<org.openqa.selenium.WebElement>, java.lang.String)
public static java.util.List<org.openqa.selenium.WebElement> findChildren(io.github.testlens.JsOverlayDebug, org.openqa.selenium.By, java.util.function.Predicate<org.openqa.selenium.WebElement>, org.openqa.selenium.By, java.lang.String)
public static org.openqa.selenium.WebElement findChildByText(io.github.testlens.JsOverlayDebug, org.openqa.selenium.By, java.util.function.Predicate<org.openqa.selenium.WebElement>, org.openqa.selenium.By, java.util.function.Predicate<org.openqa.selenium.WebElement>, java.lang.String)
public static org.openqa.selenium.WebElement findChildByTextThenFind(io.github.testlens.JsOverlayDebug, org.openqa.selenium.By, java.util.function.Predicate<org.openqa.selenium.WebElement>, org.openqa.selenium.By, java.util.function.Predicate<org.openqa.selenium.WebElement>, org.openqa.selenium.By, java.lang.String)
```
