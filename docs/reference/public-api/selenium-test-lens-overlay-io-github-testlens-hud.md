---
search:
  exclude: true
---

# selenium-test-lens-overlay: `io.github.testlens.hud`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.hud.HudBranding` {#io-github-testlens-hud-hudbranding}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static final io.github.testlens.hud.HudBranding TEST_LENS
public static final io.github.testlens.hud.HudBranding CUSTOM
public static final io.github.testlens.hud.HudBranding BOTH
public static final io.github.testlens.hud.HudBranding NONE
public static io.github.testlens.hud.HudBranding[] values()
public static io.github.testlens.hud.HudBranding valueOf(java.lang.String)
```

## `io.github.testlens.hud.HudFontPreset` {#io-github-testlens-hud-hudfontpreset}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static final io.github.testlens.hud.HudFontPreset SYSTEM
public static final io.github.testlens.hud.HudFontPreset MONOSPACE
public static final io.github.testlens.hud.HudFontPreset UI_SANS
public static io.github.testlens.hud.HudFontPreset[] values()
public static io.github.testlens.hud.HudFontPreset valueOf(java.lang.String)
```

## `io.github.testlens.hud.HudHeaderLayout` {#io-github-testlens-hud-hudheaderlayout}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static final io.github.testlens.hud.HudHeaderLayout AUTO
public static final io.github.testlens.hud.HudHeaderLayout INLINE
public static final io.github.testlens.hud.HudHeaderLayout STACKED
public static io.github.testlens.hud.HudHeaderLayout[] values()
public static io.github.testlens.hud.HudHeaderLayout valueOf(java.lang.String)
```

## `io.github.testlens.hud.HudLogoPlacement` {#io-github-testlens-hud-hudlogoplacement}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static final io.github.testlens.hud.HudLogoPlacement LEFT_RAIL
public static final io.github.testlens.hud.HudLogoPlacement HEADER_LEFT
public static final io.github.testlens.hud.HudLogoPlacement HEADER_RIGHT
public static io.github.testlens.hud.HudLogoPlacement[] values()
public static io.github.testlens.hud.HudLogoPlacement valueOf(java.lang.String)
```

## `io.github.testlens.hud.HudOptions$Builder` {#io-github-testlens-hud-hudoptions-builder}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public io.github.testlens.hud.HudOptions$Builder preset(io.github.testlens.hud.HudPreset)
public io.github.testlens.hud.HudOptions$Builder position(io.github.testlens.hud.HudPosition)
public io.github.testlens.hud.HudOptions$Builder headerLayout(io.github.testlens.hud.HudHeaderLayout)
public io.github.testlens.hud.HudOptions$Builder offsetXPx(int)
public io.github.testlens.hud.HudOptions$Builder offsetYPx(int)
public io.github.testlens.hud.HudOptions$Builder widthPx(int)
public io.github.testlens.hud.HudOptions$Builder maxHeightPx(int)
public io.github.testlens.hud.HudOptions$Builder maxLogHeightPx(int)
public io.github.testlens.hud.HudOptions$Builder railWidthPx(int)
public io.github.testlens.hud.HudOptions$Builder fontPreset(io.github.testlens.hud.HudFontPreset)
public io.github.testlens.hud.HudOptions$Builder typography(io.github.testlens.hud.HudTypography)
public io.github.testlens.hud.HudOptions$Builder scrollbarStyle(io.github.testlens.hud.HudScrollbarStyle)
public io.github.testlens.hud.HudOptions$Builder scrollbarWidthPx(int)
public io.github.testlens.hud.HudOptions$Builder scrollbarTrackColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder scrollbarThumbColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder scrollbarThumbHoverColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder baseFontSizePx(int)
public io.github.testlens.hud.HudOptions$Builder headerFontSizePx(int)
public io.github.testlens.hud.HudOptions$Builder showTestName(boolean)
public io.github.testlens.hud.HudOptions$Builder showCurrentStep(boolean)
public io.github.testlens.hud.HudOptions$Builder showPipeline(boolean)
public io.github.testlens.hud.HudOptions$Builder showTimestamps(boolean)
public io.github.testlens.hud.HudOptions$Builder timestampFormat(io.github.testlens.hud.HudTimestampFormat)
public io.github.testlens.hud.HudOptions$Builder timestampPattern(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder timestampZone(java.time.ZoneId)
public io.github.testlens.hud.HudOptions$Builder systemTimestampZone()
public io.github.testlens.hud.HudOptions$Builder showEventLog(boolean)
public io.github.testlens.hud.HudOptions$Builder showNetwork(boolean)
public io.github.testlens.hud.HudOptions$Builder showRetries(boolean)
public io.github.testlens.hud.HudOptions$Builder showWaits(boolean)
public io.github.testlens.hud.HudOptions$Builder showAssertions(boolean)
public io.github.testlens.hud.HudOptions$Builder sourceNavigation(io.github.testlens.hud.SourceNavigationOptions)
public io.github.testlens.hud.HudOptions$Builder branding(io.github.testlens.hud.HudBranding)
public io.github.testlens.hud.HudOptions$Builder logoPlacement(io.github.testlens.hud.HudLogoPlacement)
public io.github.testlens.hud.HudOptions$Builder background(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder backgroundOpacity(double)
public io.github.testlens.hud.HudOptions$Builder accentColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder primaryTextColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder mutedTextColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder successColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder warningColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder failureColor(java.lang.String)
public io.github.testlens.hud.HudOptions$Builder customLogo(java.nio.file.Path)
public io.github.testlens.hud.HudOptions build()
```

## `io.github.testlens.hud.HudOptions` {#io-github-testlens-hud-hudoptions}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static io.github.testlens.hud.HudOptions defaults()
public static io.github.testlens.hud.HudOptions$Builder builder()
public io.github.testlens.hud.HudOptions$Builder toBuilder()
public io.github.testlens.hud.HudPreset preset()
public io.github.testlens.hud.HudPosition position()
public io.github.testlens.hud.HudHeaderLayout headerLayout()
public int offsetXPx()
public int offsetYPx()
public int widthPx()
public int maxHeightPx()
public int maxLogHeightPx()
public int railWidthPx()
public io.github.testlens.hud.HudFontPreset fontPreset()
public io.github.testlens.hud.HudTypography typography()
public io.github.testlens.hud.HudScrollbarStyle scrollbarStyle()
public int scrollbarWidthPx()
public java.lang.String scrollbarTrackColor()
public java.lang.String scrollbarThumbColor()
public java.lang.String scrollbarThumbHoverColor()
public int baseFontSizePx()
public int headerFontSizePx()
public boolean showTestName()
public boolean showCurrentStep()
public boolean showPipeline()
public boolean showTimestamps()
public io.github.testlens.hud.HudTimestampFormat timestampFormat()
public java.util.Optional<java.lang.String> timestampPattern()
public java.lang.String effectiveTimestampPattern()
public java.util.Optional<java.time.ZoneId> timestampZone()
public boolean usesSystemTimestampZone()
public java.time.ZoneId effectiveTimestampZone()
public boolean showEventLog()
public boolean showNetwork()
public boolean showRetries()
public boolean showWaits()
public boolean showAssertions()
public io.github.testlens.hud.SourceNavigationOptions sourceNavigation()
public io.github.testlens.hud.HudBranding branding()
public io.github.testlens.hud.HudLogoPlacement logoPlacement()
public java.lang.String background()
public double backgroundOpacity()
public java.lang.String accentColor()
public java.lang.String primaryTextColor()
public java.lang.String mutedTextColor()
public java.lang.String successColor()
public java.lang.String warningColor()
public java.lang.String failureColor()
public boolean hasCustomLogo()
```

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
public void appendLog(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
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

## `io.github.testlens.hud.HudPreset` {#io-github-testlens-hud-hudpreset}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static final io.github.testlens.hud.HudPreset MINIMAL
public static final io.github.testlens.hud.HudPreset COMPACT
public static final io.github.testlens.hud.HudPreset STANDARD
public static final io.github.testlens.hud.HudPreset DEBUG
public static io.github.testlens.hud.HudPreset[] values()
public static io.github.testlens.hud.HudPreset valueOf(java.lang.String)
```

## `io.github.testlens.hud.HudScrollbarStyle` {#io-github-testlens-hud-hudscrollbarstyle}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static final io.github.testlens.hud.HudScrollbarStyle NATIVE
public static final io.github.testlens.hud.HudScrollbarStyle SUBTLE
public static final io.github.testlens.hud.HudScrollbarStyle STANDARD
public static io.github.testlens.hud.HudScrollbarStyle[] values()
public static io.github.testlens.hud.HudScrollbarStyle valueOf(java.lang.String)
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

## `io.github.testlens.hud.HudTimestampFormat` {#io-github-testlens-hud-hudtimestampformat}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static final io.github.testlens.hud.HudTimestampFormat ISO_UTC
public static final io.github.testlens.hud.HudTimestampFormat TIME_ONLY
public static final io.github.testlens.hud.HudTimestampFormat DATE_TIME
public static io.github.testlens.hud.HudTimestampFormat[] values()
public static io.github.testlens.hud.HudTimestampFormat valueOf(java.lang.String)
```

## `io.github.testlens.hud.HudTypography$Builder` {#io-github-testlens-hud-hudtypography-builder}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public io.github.testlens.hud.HudTypography$Builder header(io.github.testlens.hud.HudFontPreset)
public io.github.testlens.hud.HudTypography$Builder currentStep(io.github.testlens.hud.HudFontPreset)
public io.github.testlens.hud.HudTypography$Builder eventLog(io.github.testlens.hud.HudFontPreset)
public io.github.testlens.hud.HudTypography$Builder metadata(io.github.testlens.hud.HudFontPreset)
public io.github.testlens.hud.HudTypography$Builder timestampFontSizePx(int)
public io.github.testlens.hud.HudTypography build()
```

## `io.github.testlens.hud.HudTypography` {#io-github-testlens-hud-hudtypography}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/hud-studio.md](../../observability/hud-studio.md)

```java
public static io.github.testlens.hud.HudTypography inheritAll()
public static io.github.testlens.hud.HudTypography$Builder builder()
public io.github.testlens.hud.HudTypography$Builder toBuilder()
public java.util.Optional<io.github.testlens.hud.HudFontPreset> header()
public java.util.Optional<io.github.testlens.hud.HudFontPreset> currentStep()
public java.util.Optional<io.github.testlens.hud.HudFontPreset> eventLog()
public java.util.Optional<io.github.testlens.hud.HudFontPreset> metadata()
public int timestampFontSizePx()
```

## `io.github.testlens.hud.SourceIde` {#io-github-testlens-hud-sourceide}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public static final io.github.testlens.hud.SourceIde INTELLIJ
public static final io.github.testlens.hud.SourceIde VSCODE
public static final io.github.testlens.hud.SourceIde CUSTOM
public static io.github.testlens.hud.SourceIde[] values()
public static io.github.testlens.hud.SourceIde valueOf(java.lang.String)
```

## `io.github.testlens.hud.SourceNavigationModifier` {#io-github-testlens-hud-sourcenavigationmodifier}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public static final io.github.testlens.hud.SourceNavigationModifier CTRL_ALT
public static io.github.testlens.hud.SourceNavigationModifier[] values()
public static io.github.testlens.hud.SourceNavigationModifier valueOf(java.lang.String)
```

## `io.github.testlens.hud.SourceNavigationOptions$Builder` {#io-github-testlens-hud-sourcenavigationoptions-builder}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public io.github.testlens.hud.SourceNavigationOptions$Builder enabled(boolean)
public io.github.testlens.hud.SourceNavigationOptions$Builder activationModifier(io.github.testlens.hud.SourceNavigationModifier)
public io.github.testlens.hud.SourceNavigationOptions$Builder ide(io.github.testlens.hud.SourceIde)
public io.github.testlens.hud.SourceNavigationOptions$Builder sourceRoots(java.nio.file.Path...)
public io.github.testlens.hud.SourceNavigationOptions$Builder customUriTemplate(java.lang.String)
public io.github.testlens.hud.SourceNavigationOptions build()
```

## `io.github.testlens.hud.SourceNavigationOptions` {#io-github-testlens-hud-sourcenavigationoptions}

- Artifact/module: `selenium-test-lens-overlay`
- Package: `io.github.testlens.hud`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/observability/visual-diagnostics.md](../../observability/visual-diagnostics.md)

```java
public static io.github.testlens.hud.SourceNavigationOptions defaults()
public static io.github.testlens.hud.SourceNavigationOptions$Builder builder()
public boolean enabled()
public io.github.testlens.hud.SourceNavigationModifier activationModifier()
public io.github.testlens.hud.SourceIde ide()
public java.util.List<java.nio.file.Path> sourceRoots()
public java.lang.String customUriTemplate()
```
