# Windows and tabs

Selenium represents both windows and tabs as handles.

## Handle reads

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
String currentWindowHandle()
Set<String> windowHandles()
```

These delegate to the driver. `windowHandles()` returns an immutable copy; ordering is not guaranteed.

## switchToWindow overloads

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
TestLens switchToWindow(String handle)
TestLens switchToWindow(String handle, String label)
```

The one-argument overload uses the handle as its diagnostic label. Both call `driver.switchTo().window(handle)`, emit context events, return the same Lens, and rethrow `NoSuchWindowException`/other WebDriver failures.

## waitForNewWindow(Set<String> existingHandles)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
String waitForNewWindow(Set<String> existingHandles)
```

Polls driver handles using locator timeout/polling settings. A null set is treated as empty. It returns the only newly observed handle. Timeout means none appeared; more than one new handle throws `NoSuchWindowException` because the choice would be ambiguous.

## switchToNewWindow(Set<String>, String)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
TestLens switchToNewWindow(Set<String> existingHandles, String label)
```

Calls `waitForNewWindow` and then the labelled switch method.

```java
Set<String> before = lens.windowHandles();
lens.getByTestId("open-receipt").click();
lens.switchToNewWindow(before, "Receipt");
```
