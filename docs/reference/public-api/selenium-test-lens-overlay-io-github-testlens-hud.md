---
search:
  exclude: true
---

# selenium-test-lens-overlay: `io.github.testlens.hud`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.hud.HudPanel` {#io-github-testlens-hud-hudpanel}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `LOW_LEVEL_API`
- Type kind: `class`

```java
public io.github.testlens.hud.HudPanel(io.github.testlens.core.browser.BrowserScriptExecutor, io.github.testlens.core.OverlayRootManager, io.github.testlens.OverlayConfig)
public void init(java.lang.String, java.lang.String)
public void updateStep(java.lang.String)
public void appendLog(java.lang.String, java.lang.String, java.lang.String)
```

## `io.github.testlens.hud.HudPosition` {#io-github-testlens-hud-hudposition}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public static final io.github.testlens.hud.HudPosition TOP_LEFT
public static final io.github.testlens.hud.HudPosition TOP_RIGHT
public static final io.github.testlens.hud.HudPosition BOTTOM_LEFT
public static final io.github.testlens.hud.HudPosition BOTTOM_RIGHT
public static io.github.testlens.hud.HudPosition[] values()
public static io.github.testlens.hud.HudPosition valueOf(java.lang.String)
```

## `io.github.testlens.hud.HudTheme$Builder` {#io-github-testlens-hud-hudtheme-builder}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public io.github.testlens.hud.HudTheme$Builder()
public io.github.testlens.hud.HudTheme$Builder background(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder foreground(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder mutedForeground(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder accent(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder success(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder warning(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder danger(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder borderColor(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder fontFamily(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder boxShadow(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder backdropFilter(java.lang.String)
public io.github.testlens.hud.HudTheme$Builder borderRadiusPx(java.lang.Integer)
public io.github.testlens.hud.HudTheme$Builder fontSizePx(java.lang.Integer)
public io.github.testlens.hud.HudTheme$Builder opacity(java.lang.Double)
public io.github.testlens.hud.HudTheme$Builder zIndex(java.lang.Integer)
public io.github.testlens.hud.HudTheme$Builder paddingPx(java.lang.Integer)
public io.github.testlens.hud.HudTheme$Builder gapPx(java.lang.Integer)
public io.github.testlens.hud.HudTheme$Builder maxHeightPx(java.lang.Integer)
public io.github.testlens.hud.HudTheme build()
```

## `io.github.testlens.hud.HudTheme` {#io-github-testlens-hud-hudtheme}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public static io.github.testlens.hud.HudTheme$Builder builder()
public static io.github.testlens.hud.HudTheme defaultTheme()
public static io.github.testlens.hud.HudTheme dark()
public static io.github.testlens.hud.HudTheme light()
public static io.github.testlens.hud.HudTheme glass()
public static io.github.testlens.hud.HudTheme compact()
public static io.github.testlens.hud.HudTheme highContrast()
public static io.github.testlens.hud.HudTheme blackAndColors()
public static io.github.testlens.hud.HudTheme minimal()
public static io.github.testlens.hud.HudTheme fromPreset(io.github.testlens.hud.HudThemePreset)
public java.lang.String background()
public java.lang.String foreground()
public java.lang.String mutedForeground()
public java.lang.String accent()
public java.lang.String success()
public java.lang.String warning()
public java.lang.String danger()
public java.lang.String borderColor()
public java.lang.Integer borderRadiusPx()
public java.lang.Integer fontSizePx()
public java.lang.String fontFamily()
public java.lang.String boxShadow()
public java.lang.Double opacity()
public java.lang.Integer zIndex()
public java.lang.String backdropFilter()
public java.lang.Integer paddingPx()
public java.lang.Integer gapPx()
public java.lang.Integer maxHeightPx()
public java.util.Map<java.lang.String, java.lang.Object> toMap()
```

## `io.github.testlens.hud.HudThemePreset` {#io-github-testlens-hud-hudthemepreset}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public static final io.github.testlens.hud.HudThemePreset DEFAULT
public static final io.github.testlens.hud.HudThemePreset DARK
public static final io.github.testlens.hud.HudThemePreset LIGHT
public static final io.github.testlens.hud.HudThemePreset GLASS
public static final io.github.testlens.hud.HudThemePreset COMPACT
public static final io.github.testlens.hud.HudThemePreset HIGH_CONTRAST
public static final io.github.testlens.hud.HudThemePreset BLACK_AND_COLORS
public static final io.github.testlens.hud.HudThemePreset MINIMAL
public static io.github.testlens.hud.HudThemePreset[] values()
public static io.github.testlens.hud.HudThemePreset valueOf(java.lang.String)
```
