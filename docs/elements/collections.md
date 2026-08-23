# Element collections

## resolveAll()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
List<WebElement> resolveAll()
```

Calls `driver.findElements(by)` once against the current DOM, returns an immutable copy, and emits collection resolution events. An empty result is valid. This method does not wait for a non-empty collection.

## count()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
int count()
```

Returns `resolveAll().size()` with the same immediate-DOM semantics.

## nth(int index)

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator nth(int index)
```

Returns a lazy derived locator for the zero-based index. The current factory does not reject a negative index immediately; resolution later waits until timeout and fails when the requested index is outside the current collection. Prefer validating indexes in caller code.

## first() and last()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator first()
UiLocator last()
```

`first()` is the lazy zero index; `last()` resolves the last current match when used. Both retain selector, label, options, overlay, and logging behavior.

```java
UiLocator rows = lens.locator(By.cssSelector("table tbody tr"), "Order rows");
int currentCount = rows.count();
rows.first().click();
rows.nth(2).expect().toBeVisible();
rows.last().click();
```
