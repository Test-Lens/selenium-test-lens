# Element collections

## resolveAll()

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
List<WebElement> resolveAll()
```

Evaluates one current snapshot of the locator's immutable query pipeline, returns an immutable copy, and emits collection resolution events. An empty result is valid. This method does not wait for a non-empty collection. If a filter or descendant query encounters a stale element, the whole snapshot is retried; a partial result is never returned.

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

Selection is a pipeline stage, so order matters: `rows.filterByTextContaining("Open").first()` filters before selection, while `rows.first().filterByTextContaining("Open")` can produce zero or one match after selecting the first row.

## Count waits

<!-- API SIGNATURES: io.github.testlens.selenium.locator.UiLocator -->
```java
UiLocator waitUntilCount(int expected)
UiLocator waitUntilCountAtLeast(int minimum)
UiLocator waitUntilCountAtMost(int maximum)
```

These waits use the locator timeout and poll interval and evaluate one fresh, complete snapshot per poll. Zero is a valid target. Count polling is ordinary state observation: it does not emit a recovery `RETRY`, increment `RetrySummary`, or mark the session as a flaky candidate.
