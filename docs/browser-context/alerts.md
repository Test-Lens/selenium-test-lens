# Alerts, confirms, and prompts

`TestLens.alert()` returns a `TestLensAlert` bound to the attached driver and locator wait settings. Each method emits native-dialog diagnostic events.

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
TestLensAlert alert()
```

## waitUntilPresent()

<!-- API SIGNATURES: io.github.testlens.TestLensAlert -->
```java
TestLensAlert waitUntilPresent()
```

Polls Selenium's alert-present condition and returns the same wrapper. Timeout and polling come from `UiLocatorOptions`; timeout/WebDriver failures are rethrown after failure reporting.

## text()

<!-- API SIGNATURES: io.github.testlens.TestLensAlert -->
```java
String text()
```

Reads the current native dialog text. It does not wait implicitly; call `waitUntilPresent()` first when timing is uncertain.

## accept() and dismiss()

<!-- API SIGNATURES: io.github.testlens.TestLensAlert -->
```java
void accept()
void dismiss()
```

Delegate to Selenium's active `Alert`. `accept` covers alerts/confirm acceptance; `dismiss` rejects a confirm. They emit pass/failure events.

## fill(String value)

<!-- API SIGNATURES: io.github.testlens.TestLensAlert -->
```java
void fill(String value)
```

Sends keys to a native prompt. Null is sent as an empty string. Diagnostics omit the literal value and record its length. The method returns `void`, so call `accept()` separately. The browser may reject text for non-prompt dialogs.

```java
TestLensAlert prompt = lens.alert().waitUntilPresent();
prompt.fill("non-secret input");
prompt.accept();
```
