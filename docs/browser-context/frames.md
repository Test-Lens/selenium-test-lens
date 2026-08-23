# Frames

All methods return the same `TestLens`, emit started/pass/failure context events, and rethrow Selenium runtime failures.

## switchToFrame(By frame, String label)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
TestLens switchToFrame(By frame, String label)
```

Creates a labelled locator, resolves it with locator waiting/retry behavior, then calls `driver.switchTo().frame(WebElement)`.

## switchToFrame(UiLocator frame)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
TestLens switchToFrame(UiLocator frame)
```

Resolves the supplied locator and switches using its `WebElement`. Useful when the locator is already shared/configured.

## switchToFrame(int index, String label)

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
TestLens switchToFrame(int index, String label)
```

Calls Selenium's index overload directly. It does not poll for a specific frame element; Selenium validates the zero-based frame index. `label` is diagnostic only.

## Parent and default content

<!-- API SIGNATURES: io.github.testlens.TestLens -->
```java
TestLens switchToParentFrame()
TestLens switchToDefaultContent()
```

Delegate to `parentFrame()` and `defaultContent()` respectively. They do not remember the previous frame.
