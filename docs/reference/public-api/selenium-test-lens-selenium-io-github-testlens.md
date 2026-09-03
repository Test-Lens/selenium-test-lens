---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.JsOverlayDebug$AssertionSummary$AssertionTextFormat` {#io-github-testlens-jsoverlaydebug-assertionsummary-assertiontextformat}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `ADVANCED_API`
- Type kind: `enum`
- Functional documentation: [docs/advanced/js-overlay-debug.md](../../advanced/js-overlay-debug.md)

```java
public static final io.github.testlens.JsOverlayDebug$AssertionSummary$AssertionTextFormat MESSAGE
public static final io.github.testlens.JsOverlayDebug$AssertionSummary$AssertionTextFormat JSON
public static io.github.testlens.JsOverlayDebug$AssertionSummary$AssertionTextFormat[] values()
public static io.github.testlens.JsOverlayDebug$AssertionSummary$AssertionTextFormat valueOf(java.lang.String)
```

## `io.github.testlens.JsOverlayDebug$AssertionSummary` {#io-github-testlens-jsoverlaydebug-assertionsummary}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `ADVANCED_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/js-overlay-debug.md](../../advanced/js-overlay-debug.md)

```java
public io.github.testlens.JsOverlayDebug$AssertionSummary(java.lang.String)
public java.util.List<io.github.testlens.actions.AssertActions$OverlayAssertionResult> getAllResults()
public java.util.List<io.github.testlens.actions.AssertActions$OverlayAssertionResult> getFailuresObjects()
public boolean hasFailures()
public java.lang.String getGroupName()
public java.util.List<java.lang.String> getAll(io.github.testlens.JsOverlayDebug$AssertionSummary$AssertionTextFormat)
public java.util.List<java.lang.String> getFailures(io.github.testlens.JsOverlayDebug$AssertionSummary$AssertionTextFormat)
public java.lang.String formatForException()
```

## `io.github.testlens.JsOverlayDebug$SoftAssertions` {#io-github-testlens-jsoverlaydebug-softassertions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `ADVANCED_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/js-overlay-debug.md](../../advanced/js-overlay-debug.md)

```java
public boolean textEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public boolean equals(org.openqa.selenium.WebElement, java.lang.String, java.util.function.Function<java.lang.String, java.lang.String>, java.lang.String)
public boolean contains(org.openqa.selenium.WebElement, java.lang.String, java.util.function.Function<java.lang.String, java.lang.String>, java.lang.String)
public boolean attributeEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public boolean cssEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public boolean colorEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public boolean hasClass(org.openqa.selenium.WebElement, java.lang.String, boolean, java.lang.String)
public boolean isVisible(org.openqa.selenium.WebElement, boolean, java.lang.String)
public boolean isEnabled(org.openqa.selenium.WebElement, boolean, java.lang.String)
public boolean isSelected(org.openqa.selenium.WebElement, boolean, java.lang.String)
public boolean contains(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public boolean equals(java.lang.Object, java.lang.Object, java.lang.String)
public boolean notEquals(java.lang.Object, java.lang.Object, java.lang.String)
public boolean contains(java.lang.String, java.lang.String, java.lang.String)
public boolean notContains(java.lang.String, java.lang.String, java.lang.String)
public boolean isTrue(boolean, java.lang.String)
public boolean isFalse(boolean, java.lang.String)
```

## `io.github.testlens.JsOverlayDebug` {#io-github-testlens-jsoverlaydebug}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `ADVANCED_API`
- Type kind: `class`
- Functional documentation: [docs/advanced/js-overlay-debug.md](../../advanced/js-overlay-debug.md)

```java
public io.github.testlens.JsOverlayDebug(org.openqa.selenium.WebDriver)
public io.github.testlens.JsOverlayDebug(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig)
public io.github.testlens.JsOverlayDebug(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.api.ApiOverlayPanel, io.github.testlens.api.ApiCallActions, io.github.testlens.core.Guards)
public io.github.testlens.JsOverlayDebug(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.api.ApiOverlayPanel, io.github.testlens.api.ApiCallActions, io.github.testlens.core.Guards, io.github.testlens.core.logging.UiTestLensLogger)
public io.github.testlens.JsOverlayDebug(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig, io.github.testlens.api.ApiOverlayPanel, io.github.testlens.api.ApiCallActions, io.github.testlens.core.Guards, io.github.testlens.core.OverlayLogger)
public void setOverlayPolicy(io.github.testlens.selenium.overlay.OverlayPolicy)
public io.github.testlens.selenium.actionability.ActionabilityReport checkActionability(org.openqa.selenium.By, io.github.testlens.selenium.actionability.ActionabilityOptions)
public io.github.testlens.selenium.actionability.ActionabilityReport checkActionability(org.openqa.selenium.WebElement, io.github.testlens.selenium.actionability.ActionabilityOptions)
public io.github.testlens.selenium.locator.UiLocator locator(org.openqa.selenium.By)
public io.github.testlens.selenium.locator.UiLocator locator(org.openqa.selenium.By, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator locator(org.openqa.selenium.By, io.github.testlens.selenium.locator.UiLocatorOptions)
public io.github.testlens.selenium.locator.UiLocator locator(org.openqa.selenium.By, java.lang.String, io.github.testlens.selenium.locator.UiLocatorOptions)
public io.github.testlens.selenium.locator.UiLocator getByTestId(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByTestId(java.lang.String, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByPlaceholder(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByPlaceholder(java.lang.String, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByText(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByText(java.lang.String, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByTextContaining(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByTextContaining(java.lang.String, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByLabel(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByLabel(java.lang.String, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByRole(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByRole(java.lang.String, java.lang.String)
public io.github.testlens.selenium.assertions.UiExpect expect(org.openqa.selenium.By)
public io.github.testlens.selenium.assertions.UiExpect expect(org.openqa.selenium.By, java.lang.String)
public io.github.testlens.selenium.assertions.UiExpect expect(io.github.testlens.selenium.locator.UiLocator)
public io.github.testlens.selenium.assertions.UiExpect expect(io.github.testlens.selenium.locator.UiLocator, io.github.testlens.selenium.assertions.UiAssertionOptions)
public io.github.testlens.selenium.business.BusinessAssertions business(java.lang.String)
public io.github.testlens.selenium.business.BusinessAssertions business(java.lang.String, io.github.testlens.selenium.business.BusinessAssertionOptions)
public io.github.testlens.selenium.auth.AuthStateManager auth()
public io.github.testlens.selenium.auth.AuthState captureAuthState(io.github.testlens.selenium.auth.AuthStateOptions)
public io.github.testlens.selenium.auth.AuthRestoreResult restoreAuthState(io.github.testlens.selenium.auth.AuthState, io.github.testlens.selenium.auth.AuthRestoreOptions)
public io.github.testlens.selenium.auth.AuthRestoreResult restoreAuthState(java.nio.file.Path, io.github.testlens.selenium.auth.AuthRestoreOptions)
public io.github.testlens.selenium.network.NetworkDiagnostics network()
public io.github.testlens.selenium.network.NetworkDiagnosticsResult attachNetworkLog(java.nio.file.Path)
public void attachSession(io.github.testlens.core.trace.UiTestLensSession)
public java.util.Optional<io.github.testlens.core.trace.UiTestLensSession> session()
public io.github.testlens.core.trace.UiTestLensSession startSession(java.lang.String)
public io.github.testlens.core.trace.TraceArtifact attachScreenshot(java.lang.String, java.nio.file.Path)
public io.github.testlens.core.trace.TraceArtifact attachVideo(java.lang.String, java.nio.file.Path)
public io.github.testlens.core.trace.TraceArtifact attachArtifact(io.github.testlens.core.trace.TraceArtifact)
public java.lang.String exportTraceHtml()
public java.lang.String exportTraceHtml(io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public java.nio.file.Path exportTraceHtml(java.nio.file.Path)
public java.nio.file.Path exportTraceHtml(java.nio.file.Path, io.github.testlens.core.trace.export.TraceHtmlExportOptions)
public io.github.testlens.selenium.evidence.ScreenshotCaptureResult captureScreenshot(java.lang.String)
public io.github.testlens.selenium.evidence.ScreenshotCaptureResult captureScreenshot(java.lang.String, io.github.testlens.selenium.evidence.ScreenshotCaptureOptions)
public io.github.testlens.selenium.evidence.VideoEvidenceResult attachVideoFile(java.lang.String, java.nio.file.Path)
public io.github.testlens.selenium.evidence.VideoEvidenceResult attachVideoFile(java.lang.String, java.nio.file.Path, io.github.testlens.selenium.evidence.VideoEvidenceOptions)
public io.github.testlens.selenium.evidence.VideoEvidenceResult attachVideoUrl(java.lang.String, java.lang.String)
public io.github.testlens.selenium.evidence.VideoEvidenceResult attachVideoUrl(java.lang.String, java.lang.String, io.github.testlens.selenium.evidence.VideoEvidenceOptions)
public io.github.testlens.selenium.steps.UiStepResult step(java.lang.String, java.lang.Runnable)
public io.github.testlens.selenium.steps.UiStepResult step(java.lang.String, io.github.testlens.selenium.steps.UiStepOptions, java.lang.Runnable)
public void initHud(java.lang.String, java.lang.String)
public void setStep(java.lang.String)
public void hudLog(java.lang.String, java.lang.String, java.lang.String)
public void highlightClick(org.openqa.selenium.WebElement, java.lang.String)
public org.openqa.selenium.WebElement highlightElement(org.openqa.selenium.WebElement, java.lang.String)
public void highlightParent(org.openqa.selenium.WebElement, java.lang.String)
public void highlightAncestor(org.openqa.selenium.WebElement, int, java.lang.String)
public void highlightClosest(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public void highlightThenClick(org.openqa.selenium.WebElement, java.lang.String)
public void typeWithHint(org.openqa.selenium.WebElement, java.lang.String)
public void clearAndType(org.openqa.selenium.WebElement, java.lang.String)
public void smartTypeWithHint(org.openqa.selenium.WebElement, java.lang.String)
public void smartTypeWithHintHighlighted(org.openqa.selenium.WebElement, java.lang.String)
public void smartTypeWithHintHighlighted(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public void smartClickWithOverlayHandler(org.openqa.selenium.WebElement, java.lang.String)
public void clearDebugArtifacts()
public void waitForPageReady()
public void waitForPageReady(java.time.Duration)
public void waitForNetworkIdle()
public void waitForNetworkIdle(java.time.Duration, java.time.Duration)
public void waitForInteractiveOrComplete()
public void waitForInteractiveOrComplete(java.time.Duration)
public void ensureWaitHudInjected()
public void waitHudStart(java.lang.String)
public void waitHudStop(java.lang.String, long)
public void forceHideWaitHud()
public void showWaitIndicator(java.lang.String)
public void hideWaitIndicator()
public void waitForReactRootMounted(org.openqa.selenium.By)
public void waitForReactRootMounted(org.openqa.selenium.By, java.time.Duration)
public void waitForSpaDomStableUnder(org.openqa.selenium.By)
public void waitForSpaDomStableUnder(org.openqa.selenium.By, java.time.Duration, java.time.Duration)
public org.openqa.selenium.WebElement waitForReactComponentVisible(org.openqa.selenium.By, org.openqa.selenium.By)
public org.openqa.selenium.WebElement waitForReactComponentVisible(org.openqa.selenium.By, org.openqa.selenium.By, java.time.Duration)
public void waitForReactAndNetworkIdle(org.openqa.selenium.By)
public void waitForReactAndNetworkIdle(org.openqa.selenium.By, java.time.Duration)
public void showLastWaitInHud()
public java.util.Optional<org.openqa.selenium.WebElement> detectPopup()
public boolean highlightPopupIfPresent(java.lang.String)
public boolean highlightPopupIfPresent()
public boolean closePopupIfPresent(java.lang.String, java.lang.String)
public boolean closePopupIfPresent()
public void scrollToElementWithArrow(org.openqa.selenium.WebElement)
public void scrollToElementWithArrow(org.openqa.selenium.WebElement, long)
public void scrollToElementWithArrow(org.openqa.selenium.WebElement, long, io.github.testlens.scroll.ScrollElementEdge, io.github.testlens.scroll.ScrollViewportEdge)
public void scrollToElementWithArrow(org.openqa.selenium.WebElement, io.github.testlens.scroll.ScrollElementEdge, io.github.testlens.scroll.ScrollViewportEdge)
public boolean assertTextEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public boolean assertTextContains(org.openqa.selenium.WebElement, java.lang.String, java.lang.String)
public boolean assertAttributeEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public boolean assertCssEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public boolean assertColorEquals(org.openqa.selenium.WebElement, java.lang.String, java.lang.String, java.lang.String)
public boolean assertHasClass(org.openqa.selenium.WebElement, java.lang.String, boolean, java.lang.String)
public boolean assertVisible(org.openqa.selenium.WebElement, boolean, java.lang.String)
public boolean assertEnabled(org.openqa.selenium.WebElement, boolean, java.lang.String)
public boolean assertSelected(org.openqa.selenium.WebElement, boolean, java.lang.String)
public io.github.testlens.JsOverlayDebug$AssertionSummary assertGroup(java.lang.String, java.util.function.Consumer<io.github.testlens.JsOverlayDebug$SoftAssertions>, boolean)
public org.openqa.selenium.WebElement resolveClickTarget(org.openqa.selenium.WebElement)
public org.openqa.selenium.WebElement resolveFileInputTarget(org.openqa.selenium.WebElement)
public java.lang.String resolveClickTargetCssSelector(org.openqa.selenium.WebElement)
public java.lang.String resolveFileInputCssSelector(org.openqa.selenium.WebElement)
public void smartClickResolved(org.openqa.selenium.WebElement, java.lang.String)
public void smartUploadFile(org.openqa.selenium.WebElement, java.lang.String)
public void showApiCall(java.lang.String, java.lang.String, java.lang.String, java.lang.String, long)
public void showApiResponse(java.lang.String, int, long, java.lang.String, java.lang.String)
public void hideApiModal()
public <T> T apiCallWithModal(java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.util.concurrent.Callable<T>, java.util.function.Function<T, java.lang.String>)
public java.lang.String apiShowRequest(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public void apiSetPending(java.lang.String, long)
public void apiSetResponse(java.lang.String, int, long, java.lang.String, java.lang.String)
public boolean apiHighlightJsonPath(java.lang.String)
public int apiHighlightKeyAnimated(java.lang.String, long, int)
public void highlightPathAnimated(java.lang.String, int)
public void apiHighlightJsonPathsAnimated(java.util.List<java.lang.String>, long)
public org.openqa.selenium.WebDriver getDriver()
public io.github.testlens.OverlayConfig getConfig()
```

## `io.github.testlens.OverlayWait` {#io-github-testlens-overlaywait}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.OverlayWait(org.openqa.selenium.WebDriver, java.time.Duration, io.github.testlens.JsOverlayDebug, io.github.testlens.core.OverlayLogger)
public io.github.testlens.OverlayWait(org.openqa.selenium.WebDriver, java.time.Duration, io.github.testlens.JsOverlayDebug)
public <T> T until(java.util.function.Function<? super org.openqa.selenium.WebDriver, T>, java.lang.String)
```

## `io.github.testlens.TestLens` {#io-github-testlens-testlens}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/test-lens.md](../../reference/test-lens.md)

```java
public static io.github.testlens.TestLens attach(org.openqa.selenium.WebDriver)
public static io.github.testlens.TestLens attach(org.openqa.selenium.WebDriver, io.github.testlens.OverlayConfig)
public static io.github.testlens.TestLens attach(org.openqa.selenium.WebDriver, io.github.testlens.TestLensOptions)
public org.openqa.selenium.WebDriver driver()
public io.github.testlens.core.trace.UiTestLensSession startSession(java.lang.String)
public java.util.Optional<io.github.testlens.core.trace.UiTestLensSession> session()
public io.github.testlens.core.trace.RetrySummary retrySummary()
public io.github.testlens.selenium.locator.UiLocator locator(org.openqa.selenium.By)
public io.github.testlens.selenium.locator.UiLocator locator(org.openqa.selenium.By, java.lang.String)
public io.github.testlens.selenium.assertions.UiExpect expect(org.openqa.selenium.By)
public io.github.testlens.selenium.assertions.UiExpect expect(org.openqa.selenium.By, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByTestId(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByText(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByText(java.lang.String, java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByTextContaining(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByRole(java.lang.String)
public io.github.testlens.selenium.locator.UiLocator getByRole(java.lang.String, java.lang.String)
public io.github.testlens.selenium.evidence.ScreenshotCaptureResult captureScreenshot(java.lang.String)
public io.github.testlens.selenium.evidence.ScreenshotCaptureResult captureScreenshot(java.lang.String, io.github.testlens.selenium.evidence.ScreenshotCaptureOptions)
public void scrollToElementWithArrow(org.openqa.selenium.WebElement)
public void smartUploadFile(org.openqa.selenium.WebElement, java.lang.String)
public <T> T apiCallWithModal(java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.util.concurrent.Callable<T>, java.util.function.Function<T, java.lang.String>)
public io.github.testlens.selenium.steps.UiStepResult step(java.lang.String, java.lang.Runnable)
public io.github.testlens.selenium.steps.UiStepResult step(java.lang.String, io.github.testlens.selenium.steps.UiStepOptions, java.lang.Runnable)
public io.github.testlens.TestLens switchToFrame(org.openqa.selenium.By, java.lang.String)
public io.github.testlens.TestLens switchToFrame(io.github.testlens.selenium.locator.UiLocator)
public io.github.testlens.TestLens switchToFrame(int, java.lang.String)
public io.github.testlens.TestLens switchToParentFrame()
public io.github.testlens.TestLens switchToDefaultContent()
public java.lang.String currentWindowHandle()
public java.util.Set<java.lang.String> windowHandles()
public io.github.testlens.TestLens switchToWindow(java.lang.String)
public io.github.testlens.TestLens switchToWindow(java.lang.String, java.lang.String)
public java.lang.String waitForNewWindow(java.util.Set<java.lang.String>)
public io.github.testlens.TestLens switchToNewWindow(java.util.Set<java.lang.String>, java.lang.String)
public io.github.testlens.TestLensAlert alert()
public io.github.testlens.selenium.network.NetworkDiagnostics network()
public io.github.testlens.TestLensFinalizationResult finishPassed()
public io.github.testlens.TestLensFinalizationResult finishFailed(java.lang.Throwable)
public io.github.testlens.TestLensFinalizationResult finishSkipped(java.lang.String)
```

## `io.github.testlens.TestLensAlert` {#io-github-testlens-testlensalert}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/browser-context/alerts.md](../../browser-context/alerts.md)

```java
public io.github.testlens.TestLensAlert waitUntilPresent()
public java.lang.String text()
public void accept()
public void dismiss()
public void fill(java.lang.String)
```

## `io.github.testlens.TestLensFinalizationResult` {#io-github-testlens-testlensfinalizationresult}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/reference/result-types.md](../../reference/result-types.md)

```java
public io.github.testlens.TestLensFinalizationResult(io.github.testlens.core.trace.UiTestLensSession, java.nio.file.Path, java.nio.file.Path, java.nio.file.Path, java.nio.file.Path, java.util.List<java.lang.Throwable>)
public boolean fullySuccessful()
public io.github.testlens.core.trace.RetrySummary retrySummary()
public java.util.Optional<java.nio.file.Path> failureBundleDirectory()
public java.util.Optional<java.nio.file.Path> failureBundleManifest()
public java.util.Optional<java.nio.file.Path> failureBundleArchive()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.core.trace.UiTestLensSession session()
public java.nio.file.Path outputDirectory()
public java.nio.file.Path jsonReport()
public java.nio.file.Path htmlReport()
public java.nio.file.Path failureScreenshot()
public java.util.List<java.lang.Throwable> diagnosticFailures()
```

## `io.github.testlens.TestLensOptions$Builder` {#io-github-testlens-testlensoptions-builder}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/configuration.md](../../reference/configuration.md)

```java
public io.github.testlens.TestLensOptions$Builder overlayConfig(io.github.testlens.OverlayConfig)
public io.github.testlens.TestLensOptions$Builder locatorOptions(io.github.testlens.selenium.locator.UiLocatorOptions)
public io.github.testlens.TestLensOptions$Builder outputRoot(java.nio.file.Path)
public io.github.testlens.TestLensOptions$Builder screenshotOnFailure(boolean)
public io.github.testlens.TestLensOptions$Builder cleanupHudOnFinish(boolean)
public io.github.testlens.TestLensOptions$Builder retryOutcomePolicy(io.github.testlens.core.trace.RetryOutcomePolicy)
public io.github.testlens.TestLensOptions$Builder allowedRetries(int)
public io.github.testlens.TestLensOptions$Builder failureBundleOptions(io.github.testlens.selenium.evidence.FailureBundleOptions)
public io.github.testlens.TestLensOptions build()
```

## `io.github.testlens.TestLensOptions` {#io-github-testlens-testlensoptions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/reference/configuration.md](../../reference/configuration.md)

```java
public static io.github.testlens.TestLensOptions defaults()
public static io.github.testlens.TestLensOptions$Builder builder()
public io.github.testlens.OverlayConfig overlayConfig()
public io.github.testlens.selenium.locator.UiLocatorOptions locatorOptions()
public java.nio.file.Path outputRoot()
public boolean screenshotOnFailure()
public boolean cleanupHudOnFinish()
public io.github.testlens.core.trace.RetryOutcomePolicy retryOutcomePolicy()
public int allowedRetries()
public io.github.testlens.selenium.evidence.FailureBundleOptions failureBundleOptions()
```
