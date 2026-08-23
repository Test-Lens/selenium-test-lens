---
search:
  exclude: true
---

# selenium-test-lens-overlay: `io.github.testlens`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

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
public io.github.testlens.OverlayConfig$Builder hudTheme(io.github.testlens.hud.HudThemePreset)
public io.github.testlens.OverlayConfig$Builder highlightColor(java.lang.String)
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
```
