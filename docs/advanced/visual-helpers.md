# Advanced visual helpers

These `TestLens` facade methods are public but specialized.

## scrollToElementWithArrow(WebElement)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
void scrollToElementWithArrow(WebElement element)
```

Scrolls to the supplied Selenium element and displays an arrow-style visual cue. It uses injected JavaScript/overlay resources and can fail when script execution or DOM decoration is unavailable.

<!-- SCREENSHOT TODO: assets/screenshots/scroll-arrow.png
Show scrollToElementWithArrow() after the viewport reaches the target.
The arrow and target must both be visible, with enough page context to understand the scroll.
Feature documented: guided scrolling visualization.
Suggested alt text: Test Lens scroll arrow pointing at the target element after scrolling.
-->

## smartUploadFile(WebElement, String)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
void smartUploadFile(WebElement element, String absolutePath)
```

Uses the existing element and an absolute local path to drive file upload with visual feedback. The path must exist in the WebDriver execution environment (especially important for remote drivers). Do not log or expose sensitive local filenames unnecessarily.

<!-- SCREENSHOT TODO: assets/screenshots/file-upload-feedback.png
Only add this image if smartUploadFile() produces distinct useful visual feedback.
Show a synthetic filename and successful upload state; never expose a real local path.
Feature documented: visually guided file upload.
Suggested alt text: Test Lens file upload feedback using a synthetic filename.
-->

## apiCallWithModal(...)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
<T> T apiCallWithModal(String title, String method, String url, String payloadPreview, long timeoutMs, Callable<T> call, Function<T, String> responsePreview)
```

Displays a modal around a caller-supplied synchronous Java API operation, invokes `call`, and renders the mapped response preview. It is not browser network interception. The callable's exception propagates; preview functions can also fail. Never pass tokens, unredacted payloads, or sensitive URLs to display/report fields.

<!-- SCREENSHOT TODO: assets/screenshots/api-call-modal.png
Show apiCallWithModal() during a real callable using a synthetic URL, payload, and response preview.
The modal title, method, progress/result state, and timeout context should be readable.
Feature documented: visual diagnostics around caller-supplied API work.
Suggested alt text: Test Lens API call modal showing a synthetic request and response state.
-->
