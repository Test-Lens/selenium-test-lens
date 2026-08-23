---
search:
  exclude: true
---

# selenium-test-lens-overlay: `io.github.testlens.api`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.api.ApiOverlayJs` {#io-github-testlens-api-apioverlayjs}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.api`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static final java.lang.String INIT_MODAL
public static void inject(io.github.testlens.core.browser.BrowserScriptExecutor)
```

## `io.github.testlens.api.ApiOverlayPanel` {#io-github-testlens-api-apioverlaypanel}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.api`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.api.ApiOverlayPanel(io.github.testlens.core.browser.BrowserScriptExecutor, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig)
public java.lang.String showRequest(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public void setPending(java.lang.String, long)
public void setResponse(java.lang.String, int, long, java.lang.String, java.lang.String)
public void setError(java.lang.String, java.lang.String, java.lang.String)
public void hide()
public boolean apiHighlightJsonPath(java.lang.String)
public int apiHighlightKeyAnimated(java.lang.String, long, int)
public void ensureOpen()
public void highlightPathAnimated(java.lang.String, int)
public void highlightPathsAnimated(java.util.List<java.lang.String>, int, int)
public boolean highlightPathsAnimatedAndWait(java.util.List<java.lang.String>, int, int)
public java.util.List<java.lang.String> findPathsByKey(java.lang.String)
public boolean highlightPathsCandyAnimatedAndWait(java.util.List<java.lang.String>, int, int, int, int)
public void resetApiFocus()
public boolean filterToPaths(java.util.List<java.lang.String>, boolean)
public boolean clearFilter()
public void setAutoCloseMs(long, long)
public void setDelayAutoCloseUntilSearch(boolean)
```
