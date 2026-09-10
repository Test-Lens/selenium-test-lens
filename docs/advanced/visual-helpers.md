# Advanced visual helpers

These `TestLens` facade methods are public but specialized. They complement the main interaction/trace workflow; they are not required for ordinary locator actions.

## scrollToElementWithArrow(WebElement)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
void scrollToElementWithArrow(WebElement element)
```

Scrolls to the supplied Selenium element and displays an arrow-style visual cue. It uses injected JavaScript/overlay resources and can fail when script execution or DOM decoration is unavailable.

## smartUploadFile(WebElement, String)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
void smartUploadFile(WebElement element, String absolutePath)
```

Uses the existing element and an absolute local path to drive file upload with visual feedback. The path must exist in the WebDriver execution environment (especially important for remote drivers). Do not log or expose sensitive local filenames unnecessarily.

## apiCallWithModal(...)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
<T> T apiCallWithModal(String title, String method, String url, String payloadPreview, long timeoutMs, Callable<T> call, Function<T, String> responsePreview)
```

Displays a modal around a caller-supplied synchronous Java API operation, invokes `call`, and renders the mapped response preview. It visualizes values supplied by the caller; it does not discover, capture, or intercept browser traffic. The callable's exception propagates; preview functions can also fail. Central redaction protects the diagnostic copy in the `0.2.0` development line, but callers should still avoid collecting unnecessary secrets. Use [network diagnostics](network.md) for passive browser traffic evidence.
