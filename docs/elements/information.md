# Element information

Reads resolve against the current DOM and use locator retry/reporting. They do not cache a `WebElement`.

| Method | Returns | Meaning / limitations |
| --- | --- | --- |
| `WebElement resolve()` | current element | Waits/retries resolution; exposes raw Selenium for unsupported operations. |
| `String textContent()` | Selenium `getText()` | Despite the name, this is rendered/visible-text semantics, not raw JS `textContent`. |
| `boolean isVisible()` | `isDisplayed()` | Missing elements fail resolution rather than returning false. |
| `boolean isEnabled()` | `isEnabled()` | Selenium enabled state. |
| `String attribute(String name)` | `getAttribute(name)` | Name must be non-null; result may be null. |
| `String property(String name)` | `getDomProperty(name)` | Name must be non-null; result may be null. |
| `String value()` | `attribute("value")` | May be null. |
| `By by()` | selector | The underlying Selenium locator. |
| `String description()` | label/selector description | Used in diagnostics. |

```java
String ariaExpanded = lens.getByTestId("menu").attribute("aria-expanded");
```

Reads can expose page data in caller code and diagnostic messages. Avoid placing tokens or secrets in labels and exported metadata.
