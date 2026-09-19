---
search:
  exclude: true
---

# selenium-test-lens-overlay: `io.github.testlens`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.HighlightOptions$Builder` {#io-github-testlens-highlightoptions-builder}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/configuration.md](../../configuration.md)

```java
public io.github.testlens.HighlightOptions$Builder enabled(boolean)
public io.github.testlens.HighlightOptions$Builder automaticFeedback(boolean)
public io.github.testlens.HighlightOptions$Builder actionColor(java.lang.String)
public io.github.testlens.HighlightOptions$Builder waitingColor(java.lang.String)
public io.github.testlens.HighlightOptions$Builder retryColor(java.lang.String)
public io.github.testlens.HighlightOptions$Builder successColor(java.lang.String)
public io.github.testlens.HighlightOptions$Builder failureColor(java.lang.String)
public io.github.testlens.HighlightOptions$Builder durationMs(long)
public io.github.testlens.HighlightOptions$Builder borderWidthPx(int)
public io.github.testlens.HighlightOptions$Builder showLabels(boolean)
public io.github.testlens.HighlightOptions build()
```

## `io.github.testlens.HighlightOptions` {#io-github-testlens-highlightoptions}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/configuration.md](../../configuration.md)

```java
public static io.github.testlens.HighlightOptions defaults()
public static io.github.testlens.HighlightOptions$Builder builder()
public boolean enabled()
public boolean automaticFeedback()
public java.lang.String actionColor()
public java.lang.String waitingColor()
public java.lang.String retryColor()
public java.lang.String successColor()
public java.lang.String failureColor()
public long durationMs()
public int borderWidthPx()
public boolean showLabels()
public java.lang.String color(io.github.testlens.HighlightState)
public io.github.testlens.HighlightOptions$Builder toBuilder()
public java.util.Map<java.lang.String, java.lang.Object> toRuntimeMap(io.github.testlens.HighlightState)
```

## `io.github.testlens.HighlightState` {#io-github-testlens-highlightstate}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/configuration.md](../../configuration.md)

```java
public static final io.github.testlens.HighlightState ACTION
public static final io.github.testlens.HighlightState WAITING
public static final io.github.testlens.HighlightState RETRY
public static final io.github.testlens.HighlightState SUCCESS
public static final io.github.testlens.HighlightState FAILURE
public static io.github.testlens.HighlightState[] values()
public static io.github.testlens.HighlightState valueOf(java.lang.String)
```

## `io.github.testlens.OverlayConfig$Builder` {#io-github-testlens-overlayconfig-builder}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public io.github.testlens.OverlayConfig$Builder()
public io.github.testlens.OverlayConfig$Builder enabled(boolean)
public io.github.testlens.OverlayConfig$Builder showHudPanel(boolean)
public io.github.testlens.OverlayConfig$Builder decorationDurationMs(long)
public io.github.testlens.OverlayConfig$Builder globalOverlayCloseButtonSelector(java.lang.String)
public io.github.testlens.OverlayConfig$Builder hudPosition(io.github.testlens.hud.HudPosition)
public io.github.testlens.OverlayConfig$Builder hudOffset(int, int)
public io.github.testlens.OverlayConfig$Builder hudMaxWidthPx(int)
public io.github.testlens.OverlayConfig$Builder hudTheme(io.github.testlens.hud.HudTheme)
public io.github.testlens.OverlayConfig$Builder hudOptions(io.github.testlens.hud.HudOptions)
public io.github.testlens.OverlayConfig$Builder hudTheme(io.github.testlens.hud.HudThemePreset)
public io.github.testlens.OverlayConfig$Builder highlightColor(java.lang.String)
public io.github.testlens.OverlayConfig$Builder highlightOptions(io.github.testlens.HighlightOptions)
public io.github.testlens.OverlayConfig build()
```

## `io.github.testlens.OverlayConfig` {#io-github-testlens-overlayconfig}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public static io.github.testlens.OverlayConfig$Builder builder()
public boolean isEnabled()
public boolean isShowHudPanel()
public long getDecorationDurationMs()
public java.lang.String getGlobalOverlayCloseButtonSelector()
public io.github.testlens.hud.HudPosition getHudPosition()
public int getHudOffsetX()
public int getHudOffsetY()
public int getHudMaxWidthPx()
public io.github.testlens.hud.HudTheme getHudTheme()
public io.github.testlens.hud.HudThemePreset getHudThemePreset()
public java.lang.String getHighlightColor()
public io.github.testlens.HighlightOptions getHighlightOptions()
public boolean isHighlightOptionsAuthoritative()
public io.github.testlens.hud.HudOptions getHudOptions()
public boolean isHudOptionsAuthoritative()
```
