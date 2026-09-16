(function (window, document) {
  'use strict';

  window.__uiTestLens = window.__uiTestLens || {
    version: '1.0-SNAPSHOT',
    modules: {},
    state: {}
  };

  var lens = window.__uiTestLens;
  lens.modules = lens.modules || {};
  lens.state = lens.state || {};
  lens.state.overlay = lens.state.overlay || {};
  lens.state.hud = lens.state.hud || {};
  var SAFE_MARGIN_PX = 10;

  var HUD_PRESETS = {
    MINIMAL: {width:280,maxHeight:180,maxLogHeight:80,headerLayout:'AUTO',showTestName:false,showCurrentStep:true,showPipeline:false,showTimestamps:false,timestampFormat:'ISO_UTC',timestampZone:'SYSTEM',timestampZoneSource:'SYSTEM',showEventLog:false,showNetwork:false,showRetries:false,showWaits:true,showAssertions:true,fontPreset:'UI_SANS',baseFontSize:9,headerFontSize:10,scrollbarStyle:'SUBTLE',scrollbarWidth:6,scrollbarTrack:'#111827',scrollbarThumb:'#64748b',scrollbarThumbHover:'#94a3b8'},
    COMPACT: {width:420,maxHeight:280,maxLogHeight:180,headerLayout:'AUTO',showTestName:true,showCurrentStep:true,showPipeline:false,showTimestamps:false,timestampFormat:'ISO_UTC',timestampZone:'SYSTEM',timestampZoneSource:'SYSTEM',showEventLog:true,showNetwork:true,showRetries:true,showWaits:true,showAssertions:true,fontPreset:'UI_SANS',baseFontSize:10,headerFontSize:10,scrollbarStyle:'SUBTLE',scrollbarWidth:6,scrollbarTrack:'#111827',scrollbarThumb:'#64748b',scrollbarThumbHover:'#94a3b8'},
    STANDARD: {width:520,maxHeight:380,maxLogHeight:260,headerLayout:'AUTO',showTestName:true,showCurrentStep:true,showPipeline:false,showTimestamps:true,timestampFormat:'ISO_UTC',timestampZone:'SYSTEM',timestampZoneSource:'SYSTEM',showEventLog:true,showNetwork:true,showRetries:true,showWaits:true,showAssertions:true,fontPreset:'UI_SANS',baseFontSize:10,headerFontSize:10,scrollbarStyle:'SUBTLE',scrollbarWidth:6,scrollbarTrack:'#111827',scrollbarThumb:'#64748b',scrollbarThumbHover:'#94a3b8'},
    DEBUG: {width:620,maxHeight:520,maxLogHeight:360,headerLayout:'AUTO',showTestName:true,showCurrentStep:true,showPipeline:true,showTimestamps:true,timestampFormat:'ISO_UTC',timestampZone:'SYSTEM',timestampZoneSource:'SYSTEM',showEventLog:true,showNetwork:true,showRetries:true,showWaits:true,showAssertions:true,fontPreset:'MONOSPACE',baseFontSize:10,headerFontSize:11,scrollbarStyle:'STANDARD',scrollbarWidth:10,scrollbarTrack:'#1e293b',scrollbarThumb:'#94a3b8',scrollbarThumbHover:'#cbd5e1'}
  };
  var HUD_PRESET_SHARED = {
    position:'BOTTOM_RIGHT',offsetX:10,offsetY:10,railWidth:16,branding:'TEST_LENS',typography:{},
    logoPlacement:'LEFT_RAIL',background:'#0f172a',backgroundOpacity:0.96,accent:'#38bdf8',
    primaryText:'#f8fafc',mutedText:'#cbd5e1',success:'#22c55e',warning:'#f59e0b',failure:'#ef4444'
  };

  function overlayRoot() {
    var root = lens.state.overlay.root || window.__seleniumOverlayRoot;
    if (root) {
      lens.state.overlay.root = root;
      window.__seleniumOverlayRoot = root;
      return root;
    }
    return null;
  }

  function visualTypography() {
    return lens.modules.visualTypography || null;
  }

  function valueOrDash(value) {
    return value || '-';
  }

  function escapeHtml(value) {
    return String(valueOrDash(value))
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function metadataRowMarkup(label, value, extraClass, valueFontVariable) {
    return '<div class="stl-hud-meta-row ' + extraClass + '" style="display:flex;align-items:baseline;gap:4px;min-width:0;max-width:100%;line-height:1.05;margin:0;font-family:var(--ui-test-lens-hud-meta-font-family,var(--ui-test-lens-hud-font-family));">'
      + '<span class="stl-hud-meta-label" style="flex:0 0 auto;color:var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78));font-size:calc(var(--ui-test-lens-hud-header-font-size, 10px) * .7);font-weight:400;text-transform:uppercase;letter-spacing:.11em;line-height:1;white-space:nowrap;opacity:.58;">' + label + '</span>'
      + '<span class="stl-hud-meta-value" title="' + escapeHtml(value) + '" style="flex:1 1 auto;min-width:0;max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--ui-test-lens-hud-success, #22c55e);font-family:var(' + valueFontVariable + ',var(--ui-test-lens-hud-font-family));font-weight:400;font-size:var(--ui-test-lens-hud-header-font-size, 10px);line-height:1.05;">' + escapeHtml(value) + '</span>'
      + '</div>';
  }

  var DEFAULT_THEME = {
    background: 'rgba(15, 23, 42, 0.96)',
    foreground: '#f8fafc',
    mutedForeground: '#cbd5e1',
    accent: '#38bdf8',
    success: '#22c55e',
    warning: '#f59e0b',
    danger: '#ef4444',
    borderColor: 'rgba(148, 163, 184, 0.28)',
    borderRadiusPx: 10,
    fontSizePx: 12,
    fontFamily: '"Test Lens Sora", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
    boxShadow: '0 16px 40px rgba(2, 6, 23, 0.34)',
    opacity: 1,
    paddingPx: 10,
    gapPx: 6
  };

  function themeValue(theme, key) {
    if (theme && theme[key] !== undefined && theme[key] !== null && theme[key] !== '') {
      return theme[key];
    }
    return DEFAULT_THEME[key];
  }

  function setVar(panel, name, value) {
    if (value !== undefined && value !== null && value !== '') {
      panel.style.setProperty(name, String(value));
    }
  }

  function positiveNumber(value) {
    var parsed = Number(value);
    return isFinite(parsed) && parsed > 0 ? parsed : null;
  }

  function fontFamilyForPreset(fontPreset) {
    var typography = visualTypography();
    return fontPreset === 'MONOSPACE'
      ? (typography ? typography.monospaceStack : 'ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace')
      : fontPreset === 'SYSTEM'
        ? (typography ? typography.systemStack : 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif')
        : (typography ? typography.uiStack : '"Test Lens Sora", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif');
  }

  function viewportWidth() { return positiveNumber(window.innerWidth) || 1024; }
  function viewportHeight() { return positiveNumber(window.innerHeight) || 768; }
  function clamp(value, minimum, maximum) { return Math.max(minimum, Math.min(maximum, value)); }

  function hudOptions(config) {
    return (config && config.hudOptions) || {};
  }

  function option(config, name, fallback) {
    var options = hudOptions(config);
    return options[name] === undefined || options[name] === null ? fallback : options[name];
  }

  function hexWithOpacity(color, opacity) {
    var match = /^#([0-9a-f]{6})$/i.exec(String(color || ''));
    if (!match) return color;
    var value = parseInt(match[1], 16);
    return 'rgba(' + ((value >> 16) & 255) + ', ' + ((value >> 8) & 255) + ', ' + (value & 255) + ', ' + opacity + ')';
  }

  function ensureScrollbarStyles(shadow) {
    var style = shadow.querySelector('style[data-test-lens-hud-scrollbars]');
    if (style) return;
    style = document.createElement('style');
    style.setAttribute('data-test-lens-hud-scrollbars', 'true');
    style.textContent = '#selenium-hud-logs.stl-hud-custom-scrollbar{scrollbar-color:var(--ui-test-lens-scrollbar-thumb) var(--ui-test-lens-scrollbar-track);}'
      + '#selenium-hud-logs.stl-hud-scrollbar-subtle{scrollbar-width:thin;}'
      + '#selenium-hud-logs.stl-hud-scrollbar-standard{scrollbar-width:auto;}'
      + '#selenium-hud-logs.stl-hud-custom-scrollbar::-webkit-scrollbar{width:var(--ui-test-lens-scrollbar-width);height:var(--ui-test-lens-scrollbar-width);}'
      + '#selenium-hud-logs.stl-hud-custom-scrollbar::-webkit-scrollbar-track{background:var(--ui-test-lens-scrollbar-track);border-radius:999px;}'
      + '#selenium-hud-logs.stl-hud-custom-scrollbar::-webkit-scrollbar-thumb{background:var(--ui-test-lens-scrollbar-thumb);border-radius:999px;border:1px solid var(--ui-test-lens-scrollbar-track);}'
      + '#selenium-hud-logs.stl-hud-custom-scrollbar::-webkit-scrollbar-thumb:hover{background:var(--ui-test-lens-scrollbar-thumb-hover);}';
    shadow.appendChild(style);
  }

  function ensureHeaderStyles(shadow) {
    var style = shadow.querySelector('style[data-test-lens-hud-header]');
    if (style) return;
    style = document.createElement('style');
    style.setAttribute('data-test-lens-hud-header', 'true');
    style.textContent = '.stl-hud-context-header{display:flex;align-items:baseline;align-content:flex-start;flex-wrap:wrap;column-gap:10px;row-gap:0;width:100%;min-width:0;max-width:100%;}'
      + '.stl-hud-context-header>.stl-hud-header-item{box-sizing:border-box;flex:0 1 auto;width:max-content;min-width:0;max-width:100%;}'
      + '.stl-hud-context-header>.stl-hud-header-item>.stl-hud-meta-row{width:100%;}'
      + '.stl-hud-context-header[data-layout="INLINE"]{flex-wrap:nowrap;}'
      + '.stl-hud-context-header[data-layout="INLINE"]>.stl-hud-header-item{flex:1 1 0;width:auto;}'
      + '.stl-hud-context-header[data-layout="STACKED"]{flex-direction:column;align-items:stretch;}'
      + '.stl-hud-context-header[data-layout="STACKED"]>.stl-hud-header-item{flex:0 0 auto;width:100%;}'
      + '.stl-hud-meta-row{white-space:nowrap;}'
      + '.stl-hud-meta-value{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}';
    shadow.appendChild(style);
  }

  function applyScrollbar(logs, config) {
    var style = String(option(config, 'scrollbarStyle', 'SUBTLE')).toUpperCase();
    logs.classList.remove('stl-hud-custom-scrollbar', 'stl-hud-scrollbar-subtle', 'stl-hud-scrollbar-standard');
    ['--ui-test-lens-scrollbar-width', '--ui-test-lens-scrollbar-track', '--ui-test-lens-scrollbar-thumb', '--ui-test-lens-scrollbar-thumb-hover'].forEach(function (name) {
      logs.style.removeProperty(name);
    });
    logs.style.removeProperty('scrollbar-color');
    logs.style.removeProperty('scrollbar-width');
    if (style === 'NATIVE') return;
    logs.classList.add('stl-hud-custom-scrollbar', style === 'STANDARD' ? 'stl-hud-scrollbar-standard' : 'stl-hud-scrollbar-subtle');
    setVar(logs, '--ui-test-lens-scrollbar-width', option(config, 'scrollbarWidth', style === 'STANDARD' ? 10 : 6) + 'px');
    setVar(logs, '--ui-test-lens-scrollbar-track', option(config, 'scrollbarTrack', style === 'STANDARD' ? '#1e293b' : '#111827'));
    setVar(logs, '--ui-test-lens-scrollbar-thumb', option(config, 'scrollbarThumb', style === 'STANDARD' ? '#94a3b8' : '#64748b'));
    setVar(logs, '--ui-test-lens-scrollbar-thumb-hover', option(config, 'scrollbarThumbHover', style === 'STANDARD' ? '#cbd5e1' : '#94a3b8'));
  }

  function applyTheme(panel, config) {
    var theme = (config && config.theme) || {};
    var options = hudOptions(config);
    var borderRadius = themeValue(theme, 'borderRadiusPx');
    var fontSize = themeValue(theme, 'fontSizePx');
    var padding = themeValue(theme, 'paddingPx');
    var gap = themeValue(theme, 'gapPx');
    var maxHeight = positiveNumber(options.maxHeight) || positiveNumber(theme.maxHeightPx);
    var effectiveMaxHeight = Math.max(1, Math.min(maxHeight || viewportHeight(), viewportHeight() - (2 * SAFE_MARGIN_PX)));
    var fontPreset = options.fontPreset || 'UI_SANS';
    var fontFamily = fontFamilyForPreset(fontPreset);
    var typography = options.typography || {};

    var configuredBackground = options.background || themeValue(theme, 'background');
    if (options.background && options.backgroundOpacity !== undefined) {
      configuredBackground = hexWithOpacity(options.background, options.backgroundOpacity);
    }
    setVar(panel, '--ui-test-lens-hud-bg', configuredBackground);
    setVar(panel, '--ui-test-lens-hud-fg', options.primaryText || themeValue(theme, 'foreground'));
    setVar(panel, '--ui-test-lens-hud-muted-fg', options.mutedText || themeValue(theme, 'mutedForeground'));
    setVar(panel, '--ui-test-lens-hud-accent', options.accent || themeValue(theme, 'accent'));
    setVar(panel, '--ui-test-lens-hud-success', options.success || themeValue(theme, 'success'));
    setVar(panel, '--ui-test-lens-hud-warning', options.warning || themeValue(theme, 'warning'));
    setVar(panel, '--ui-test-lens-hud-danger', options.failure || themeValue(theme, 'danger'));
    setVar(panel, '--ui-test-lens-hud-border', themeValue(theme, 'borderColor'));
    setVar(panel, '--ui-test-lens-hud-radius', borderRadius + 'px');
    setVar(panel, '--ui-test-lens-hud-font-size', (options.baseFontSize || fontSize) + 'px');
    setVar(panel, '--ui-test-lens-hud-header-font-size', (options.headerFontSize || 10) + 'px');
    setVar(panel, '--ui-test-lens-hud-font-family', options.fontPreset ? fontFamily : themeValue(theme, 'fontFamily'));
    setVar(panel, '--ui-test-lens-hud-header-font-family', fontFamilyForPreset(typography.header || fontPreset));
    setVar(panel, '--ui-test-lens-hud-step-font-family', fontFamilyForPreset(typography.currentStep || fontPreset));
    setVar(panel, '--ui-test-lens-hud-event-font-family', fontFamilyForPreset(typography.eventLog || fontPreset));
    setVar(panel, '--ui-test-lens-hud-meta-font-family', fontFamilyForPreset(typography.metadata || fontPreset));
    setVar(panel, '--ui-test-lens-hud-shadow', themeValue(theme, 'boxShadow'));
    setVar(panel, '--ui-test-lens-hud-opacity', themeValue(theme, 'opacity'));
    setVar(panel, '--ui-test-lens-hud-padding-y', padding + 'px');
    setVar(panel, '--ui-test-lens-hud-padding-x', (padding + 2) + 'px');
    setVar(panel, '--ui-test-lens-hud-gap', gap + 'px');

    panel.style.background = 'var(--ui-test-lens-hud-bg, ' + DEFAULT_THEME.background + ')';
    panel.style.color = 'var(--ui-test-lens-hud-fg, ' + DEFAULT_THEME.foreground + ')';
    panel.style.fontSize = 'var(--ui-test-lens-hud-font-size, 11px)';
    panel.style.fontFamily = 'var(--ui-test-lens-hud-font-family, "Test Lens Sora", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif)';
    panel.style.padding = 'var(--ui-test-lens-hud-padding-y, 8px) var(--ui-test-lens-hud-padding-x, 10px)';
    panel.style.borderRadius = 'var(--ui-test-lens-hud-radius, 4px)';
    panel.style.boxShadow = 'var(--ui-test-lens-hud-shadow, 0 2px 6px rgba(0,0,0,0.4))';
    panel.style.opacity = 'var(--ui-test-lens-hud-opacity, 1)';
    panel.style.border = '1px solid var(--ui-test-lens-hud-border, rgba(255,255,255,0.2))';
    panel.style.boxSizing = 'border-box';
    panel.style.display = 'block';

    if (maxHeight || viewportHeight()) {
      setVar(panel, '--ui-test-lens-hud-max-height', effectiveMaxHeight + 'px');
      panel.style.maxHeight = 'var(--ui-test-lens-hud-max-height)';
      panel.style.overflow = 'hidden';
    } else {
      panel.style.removeProperty('--ui-test-lens-hud-max-height');
      panel.style.maxHeight = '';
      panel.style.overflow = '';
    }

    if (theme.backdropFilter) {
      panel.style.backdropFilter = theme.backdropFilter;
      panel.style.webkitBackdropFilter = theme.backdropFilter;
    } else {
      panel.style.backdropFilter = '';
      panel.style.webkitBackdropFilter = '';
    }

    if (theme.zIndex !== undefined && theme.zIndex !== null) {
      panel.style.zIndex = String(theme.zIndex);
    }

    panel.setAttribute('data-ui-test-lens-theme', options.preset || (config && config.themeName ? config.themeName : 'custom'));
  }

  function brandIconMarkup() {
    return '' +
      '<svg class="stl-hud-brand-icon-svg" width="14" height="14" viewBox="0 0 32 32" aria-hidden="true" focusable="false" xmlns="http://www.w3.org/2000/svg">' +
      '<path d="M5 12V7.5C5 6.1 6.1 5 7.5 5H12" fill="none" stroke="var(--ui-test-lens-hud-fg, #ffffff)" stroke-width="2.4" stroke-linecap="round"/>' +
      '<path d="M20 5h4.5C25.9 5 27 6.1 27 7.5V12" fill="none" stroke="var(--ui-test-lens-hud-accent, #4ca3ff)" stroke-width="2.4" stroke-linecap="round"/>' +
      '<path d="M27 20v4.5c0 1.4-1.1 2.5-2.5 2.5H20" fill="none" stroke="var(--ui-test-lens-hud-accent, #4ca3ff)" stroke-width="2.4" stroke-linecap="round"/>' +
      '<path d="M12 27H7.5C6.1 27 5 25.9 5 24.5V20" fill="none" stroke="var(--ui-test-lens-hud-fg, #ffffff)" stroke-width="2.4" stroke-linecap="round"/>' +
      '<circle cx="16" cy="16" r="6.5" fill="none" stroke="var(--ui-test-lens-hud-border, rgba(255,255,255,0.2))" stroke-width="2"/>' +
      '<circle cx="16" cy="16" r="3.2" fill="var(--ui-test-lens-hud-accent, #4ca3ff)"/>' +
      '<circle cx="18" cy="13.8" r="1.1" fill="var(--ui-test-lens-hud-fg, #ffffff)"/>' +
      '</svg>';
  }

  function safePngDataUri(value) {
    var candidate = String(value || '');
    return /^data:image\/png;base64,[a-z0-9+/=]+$/i.test(candidate) ? candidate : '';
  }

  function configureBrandContent(container, config, horizontal) {
    var branding = option(config, 'branding', 'TEST_LENS');
    container.style.width = 'fit-content';
    container.style.minWidth = '0';
    container.style.fontFamily = 'var(--ui-test-lens-hud-meta-font-family, var(--ui-test-lens-hud-font-family))';
    var brandIcon = container.querySelector('.stl-hud-brand-icon');
    if (!brandIcon) {
      brandIcon = document.createElement('span');
      brandIcon.className = 'stl-hud-brand-icon';
      brandIcon.style.display = 'inline-flex';
      brandIcon.style.alignItems = 'center';
      brandIcon.style.justifyContent = 'center';
      brandIcon.style.width = '14px';
      brandIcon.style.height = '14px';
      brandIcon.innerHTML = brandIconMarkup();
      container.appendChild(brandIcon);
    }
    brandIcon.style.display = branding === 'CUSTOM' ? 'none' : 'inline-flex';

    var customLogo = container.querySelector('.stl-hud-custom-logo');
    var customLogoSource = safePngDataUri(option(config, 'customLogo', ''));
    if ((branding === 'CUSTOM' || branding === 'BOTH') && customLogoSource) {
      if (!customLogo) {
        customLogo = document.createElement('img');
        customLogo.className = 'stl-hud-custom-logo';
        customLogo.alt = '';
        customLogo.style.width = 'auto';
        customLogo.style.height = horizontal ? '14px' : Math.max(12, Math.min(16, positiveNumber(option(config, 'railWidth', 16)) - 2)) + 'px';
        customLogo.style.maxWidth = horizontal ? '56px' : '64px';
        customLogo.style.flex = '0 1 auto';
        customLogo.style.objectFit = 'contain';
        customLogo.style.marginLeft = branding === 'BOTH' ? '2px' : '0';
        container.appendChild(customLogo);
      }
      customLogo.src = customLogoSource;
    } else if (customLogo) {
      removeNode(customLogo);
    }

    var brandText = container.querySelector('.stl-hud-brand-text');
    if (!brandText) {
      brandText = document.createElement('span');
      brandText.className = 'stl-hud-brand-text';
      brandText.textContent = 'TEST LENS';
      brandText.style.fontSize = '8px';
      brandText.style.lineHeight = '1';
      brandText.style.fontWeight = '600';
      brandText.style.letterSpacing = '0.06em';
      container.appendChild(brandText);
    }
    brandText.style.display = branding === 'CUSTOM' ? 'none' : 'inline';
    container.style.flexDirection = horizontal ? 'row' : 'row';
  }

  function removeNode(node) {
    if (!node || !node.parentNode) {
      return;
    }
    if (node.parentNode.removeChild) {
      node.parentNode.removeChild(node);
      return;
    }
    if (node.parentNode.children) {
      var index = node.parentNode.children.indexOf(node);
      if (index >= 0) {
        node.parentNode.children.splice(index, 1);
      }
      node.parentNode = null;
    }
  }

  function ensureStructure(panel, config) {
    var shell = panel.querySelector('.stl-hud-shell');
    if (!shell) {
      shell = document.createElement('div');
      shell.className = 'stl-hud-shell';
      shell.style.display = 'flex';
      shell.style.alignItems = 'stretch';
      shell.style.minWidth = '0';
      shell.style.minHeight = '0';
      panel.appendChild(shell);
    }

    var branding = option(config, 'branding', 'TEST_LENS');
    var logoPlacement = option(config, 'logoPlacement', 'LEFT_RAIL');
    var sideRail = shell.querySelector('.stl-hud-side-rail');
    if (branding !== 'NONE' && logoPlacement === 'LEFT_RAIL' && !sideRail) {
      sideRail = document.createElement('div');
      sideRail.className = 'stl-hud-side-rail';
      sideRail.style.boxSizing = 'border-box';
      sideRail.style.display = 'flex';
      sideRail.style.alignItems = 'center';
      sideRail.style.justifyContent = 'center';
      sideRail.style.padding = '0';
      sideRail.style.marginLeft = 'calc(2px - var(--ui-test-lens-hud-padding-x, 10px))';
      sideRail.style.marginRight = '2px';
      sideRail.style.borderRight = '1px solid color-mix(in srgb, var(--ui-test-lens-hud-border, rgba(255,255,255,0.2)) 68%, transparent)';
      sideRail.style.color = 'var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78))';
      shell.insertBefore(sideRail, shell.firstChild);
    }
    if (sideRail && (branding === 'NONE' || logoPlacement !== 'LEFT_RAIL')) {
      removeNode(sideRail);
      sideRail = null;
    }
    var railBrand = null;
    if (sideRail) {
      var railWidth = option(config, 'railWidth', 16);
      sideRail.style.flex = '0 0 ' + railWidth + 'px';
      sideRail.style.width = railWidth + 'px';
      sideRail.style.minWidth = railWidth + 'px';
      railBrand = sideRail.querySelector('.stl-hud-rail-brand');
      if (!railBrand) {
        railBrand = document.createElement('div');
        railBrand.className = 'stl-hud-rail-brand';
        railBrand.style.display = 'inline-flex';
        railBrand.style.alignItems = 'center';
        railBrand.style.justifyContent = 'center';
      railBrand.style.gap = '2px';
        railBrand.style.whiteSpace = 'nowrap';
        railBrand.style.transform = 'rotate(-90deg)';
        railBrand.style.transformOrigin = 'center';
        railBrand.style.opacity = '0.76';
        railBrand.style.color = 'var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78))';
        sideRail.appendChild(railBrand);
      }
      configureBrandContent(railBrand, config, false);
    }

    var main = shell.querySelector('.stl-hud-main');
    if (!main) {
      main = document.createElement('div');
      main.className = 'stl-hud-main';
      main.style.flex = '1 1 auto';
      main.style.minWidth = '0';
      main.style.minHeight = '0';
      main.style.display = 'flex';
      main.style.flexDirection = 'column';
      shell.appendChild(main);
    }

    var contextHeader = main.querySelector('.stl-hud-context-header');
    if (!contextHeader) {
      contextHeader = document.createElement('div');
      contextHeader.className = 'stl-hud-context-header';
      main.appendChild(contextHeader);
    }
    contextHeader.setAttribute('data-layout', String(option(config, 'headerLayout', 'AUTO')).toUpperCase());

    var headerBrand = main.querySelector('.stl-hud-header-brand');
    if (branding !== 'NONE' && logoPlacement !== 'LEFT_RAIL') {
      if (!headerBrand) {
        headerBrand = document.createElement('div');
        headerBrand.className = 'stl-hud-header-brand';
        headerBrand.style.display = 'flex';
        headerBrand.style.alignItems = 'center';
        headerBrand.style.gap = '4px';
        headerBrand.style.marginBottom = '2px';
        headerBrand.style.opacity = '0.76';
        headerBrand.style.color = 'var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78))';
      }
      headerBrand.style.justifyContent = logoPlacement === 'HEADER_RIGHT' ? 'flex-end' : 'flex-start';
      configureBrandContent(headerBrand, config, true);
      if (headerBrand.parentNode !== main) main.insertBefore(headerBrand, main.firstChild);
    } else if (headerBrand) {
      removeNode(headerBrand);
      headerBrand = null;
    }

    var oldHeader = main.querySelector('.stl-hud-header');
    if (oldHeader) {
      removeNode(oldHeader);
    }

    return {
      shell: shell,
      sideRail: sideRail,
      railBrand: railBrand,
      headerBrand: headerBrand,
      contextHeader: contextHeader,
      main: main
    };
  }

  function placeAfter(parent, node, previous) {
    var next = previous ? previous.nextSibling : parent.firstChild;
    if (node.parentNode !== parent || node.previousSibling !== previous) {
      parent.insertBefore(node, next);
    }
  }

  function migrateHudContent(panel, structure, config) {
    var title = panel.querySelector('#selenium-hud-test');
    if (title) placeAfter(structure.contextHeader, title, null);

    var step = panel.querySelector('#selenium-hud-step');
    if (step) placeAfter(structure.contextHeader, step, title);

    var previous = structure.contextHeader;

    var pipeline = panel.querySelector('#selenium-hud-pipeline');
    if (pipeline) {
      if (option(config, 'showPipeline', false)) {
        placeAfter(structure.main, pipeline, previous);
        previous = pipeline;
      } else {
        removeNode(pipeline);
      }
    }

    var logs = panel.querySelector('#selenium-hud-logs');
    if (logs && logs.parentNode !== structure.main) {
      structure.main.appendChild(logs);
    }
  }

  function positionPanel(panel, config) {
    var position = option(config, 'position', config.position || 'BOTTOM_RIGHT');
    var requestedX = config.offsetX == null ? option(config, 'offsetX', 10) : config.offsetX;
    var requestedY = config.offsetY == null ? option(config, 'offsetY', 10) : config.offsetY;
    var rect = panel.getBoundingClientRect ? panel.getBoundingClientRect() : {width: panel.offsetWidth || 0, height: panel.offsetHeight || 0};
    var panelWidth = positiveNumber(rect.width) || positiveNumber(panel.offsetWidth) || 0;
    var panelHeight = positiveNumber(rect.height) || positiveNumber(panel.offsetHeight) || 0;
    var maxOffsetX = Math.max(SAFE_MARGIN_PX, viewportWidth() - panelWidth - SAFE_MARGIN_PX);
    var maxOffsetY = Math.max(SAFE_MARGIN_PX, viewportHeight() - panelHeight - SAFE_MARGIN_PX);
    var offsetX = clamp(Number(requestedX) || 0, SAFE_MARGIN_PX, maxOffsetX);
    var offsetY = clamp(Number(requestedY) || 0, SAFE_MARGIN_PX, maxOffsetY);

    panel.style.top = 'auto';
    panel.style.right = 'auto';
    panel.style.bottom = 'auto';
    panel.style.left = 'auto';

    if (position === 'TOP_LEFT') {
      panel.style.top = offsetY + 'px';
      panel.style.left = offsetX + 'px';
    } else if (position === 'TOP_RIGHT') {
      panel.style.top = offsetY + 'px';
      panel.style.right = offsetX + 'px';
    } else if (position === 'BOTTOM_LEFT') {
      panel.style.bottom = offsetY + 'px';
      panel.style.left = offsetX + 'px';
    } else {
      panel.style.bottom = offsetY + 'px';
      panel.style.right = offsetX + 'px';
    }
  }

  function ensureLogs(panel, config) {
    var structure = ensureStructure(panel, config);
    migrateHudContent(panel, structure, config);
    var logs = panel.querySelector('#selenium-hud-logs');
    if (!option(config, 'showEventLog', true)) {
      removeNode(logs);
      return null;
    }
    if (!logs) {
      logs = document.createElement('div');
      logs.id = 'selenium-hud-logs';
      logs.style.marginTop = '3px';
      logs.style.maxHeight = option(config, 'maxLogHeight', 160) + 'px';
      logs.style.overflowY = 'auto';
      logs.style.borderTop = '1px solid var(--ui-test-lens-hud-border, rgba(255,255,255,0.2))';
      logs.style.paddingTop = '4px';
      structure.main.appendChild(logs);
    } else if (logs.parentNode !== structure.main) {
      structure.main.appendChild(logs);
    }
    logs.style.flex = '1 1 auto';
    logs.style.minHeight = '0';
    logs.style.display = 'block';
    ensureScrollbarStyles(overlayRoot());
    applyScrollbar(logs, config);
    return logs;
  }

  function updateScrollableRegions(panel) {
    var config = lens.state.hud.lastConfig || {};
    var theme = (config && config.theme) || {};
    var maxHeight = positiveNumber(option(config, 'maxHeight', theme.maxHeightPx));
    var panelHeightLimit = Math.min(maxHeight || viewportHeight(), viewportHeight() - (2 * SAFE_MARGIN_PX));
    var configuredLogHeight = positiveNumber(option(config, 'maxLogHeight', 160)) || 160;
    var logs = panel.querySelector('#selenium-hud-logs');
    if (!logs) {
      return;
    }

    if (!maxHeight) {
      logs.style.maxHeight = Math.min(configuredLogHeight, Math.max(0, viewportHeight() - (2 * SAFE_MARGIN_PX))) + 'px';
      logs.style.overflowY = 'auto';
      return;
    }

    var styles = window.getComputedStyle ? window.getComputedStyle(panel) : null;
    var paddingTop = styles ? parseFloat(styles.paddingTop) || 0 : 0;
    var paddingBottom = styles ? parseFloat(styles.paddingBottom) || 0 : 0;
    var fixedHeight = paddingTop + paddingBottom;

    var pipeline = panel.querySelector('#selenium-hud-pipeline');
    var headerBrand = panel.querySelector('.stl-hud-header-brand');
    var contextHeader = panel.querySelector('.stl-hud-context-header');
    [headerBrand, contextHeader, pipeline].forEach(function (node) {
      if (node) {
        fixedHeight += node.offsetHeight || 0;
      }
    });

    var lastContext = pipeline || contextHeader || headerBrand;
    var logMarginTop = logs.offsetTop && lastContext ? Math.max(0, logs.offsetTop - (lastContext.offsetTop + lastContext.offsetHeight)) : 0;
    var availableLogHeight = Math.max(0, Math.floor(panelHeightLimit - fixedHeight - logMarginTop - 8));
    logs.style.maxHeight = Math.min(configuredLogHeight, availableLogHeight) + 'px';
    logs.style.overflowY = 'auto';
  }

  function ensurePanel(config) {
    var shadow = overlayRoot();
    if (!shadow) {
      return null;
    }
    var typography = visualTypography();
    if (typography) typography.ensureRoot(shadow);

    var panel = shadow.querySelector('#selenium-hud-panel');
    if (!panel) {
      panel = document.createElement('div');
      panel.id = 'selenium-hud-panel';
      panel.style.position = 'fixed';
      panel.style.pointerEvents = 'auto';
      panel.style.lineHeight = '1.4';
      shadow.appendChild(panel);
    }

    applyTheme(panel, config);
    panel.setAttribute('data-test-lens-font-status', typography ? typography.status() : 'fallback');
    ensureHeaderStyles(overlayRoot());
    var structure = ensureStructure(panel, config);
    migrateHudContent(panel, structure, config);
    var requestedWidth = positiveNumber(option(config, 'width', config.maxWidth || 520)) || 520;
    var effectiveWidth = Math.max(1, Math.min(requestedWidth, viewportWidth() - (2 * SAFE_MARGIN_PX)));
    panel.style.width = effectiveWidth + 'px';
    panel.style.maxWidth = effectiveWidth + 'px';
    updateScrollableRegions(panel);
    positionPanel(panel, config);
    return panel;
  }

  function init(config) {
    config = config || {};
    lens.state.hud.lastConfig = config;

    var panel = ensurePanel(config);
    if (!panel) {
      return;
    }
    var structure = ensureStructure(panel, config);

    var title = panel.querySelector('#selenium-hud-test');
    if (option(config, 'showTestName', true) && !title) {
      title = document.createElement('div');
      title.id = 'selenium-hud-test';
    }
    if (option(config, 'showTestName', true)) {
      title.className = 'stl-hud-header-item';
      title.style.lineHeight = '1.18'; title.style.margin = '0'; title.style.padding = '0';
      title.innerHTML = metadataRowMarkup('TEST', config.testName, 'stl-hud-test-row', '--ui-test-lens-hud-header-font-family');
      placeAfter(structure.contextHeader, title, null);
    } else if (title) { removeNode(title); title = null; }

    var pipeline = panel.querySelector('#selenium-hud-pipeline');
    if (option(config, 'showPipeline', false)) {
      if (!pipeline) { pipeline = document.createElement('div'); pipeline.id = 'selenium-hud-pipeline'; }
      pipeline.innerHTML = metadataRowMarkup('PIPE', config.pipelineId, 'stl-hud-pipeline-row', '--ui-test-lens-hud-meta-font-family');
      placeAfter(structure.main, pipeline, structure.contextHeader);
    } else if (pipeline) { removeNode(pipeline); pipeline = null; }

    var step = panel.querySelector('#selenium-hud-step');
    if (option(config, 'showCurrentStep', true) && !step) {
      step = document.createElement('div');
      step.id = 'selenium-hud-step';
    }
    if (option(config, 'showCurrentStep', true)) {
      step.className = 'stl-hud-header-item';
      step.style.marginTop = '0'; step.style.lineHeight = '1.05'; step.style.fontSize = 'var(--ui-test-lens-hud-header-font-size, 10px)'; step.style.minWidth = '0';
      placeAfter(structure.contextHeader, step, title);
      if (!step.innerHTML) step.innerHTML = metadataRowMarkup('STEP', '-', 'stl-hud-step-row', '--ui-test-lens-hud-step-font-family');
    } else if (step) { removeNode(step); }

    ensureLogs(panel, config);
    updateScrollableRegions(panel);
    positionPanel(panel, config);
  }

  function setStep(stepDescription) {
    var panel = ensurePanel(lens.state.hud.lastConfig || {});
    if (!panel) {
      return;
    }

    if (!option(lens.state.hud.lastConfig || {}, 'showCurrentStep', true)) return;
    var step = panel.querySelector('#selenium-hud-step');
    if (!step) {
      return;
    }
    step.innerHTML = metadataRowMarkup('STEP', stepDescription, 'stl-hud-step-row', '--ui-test-lens-hud-step-font-family');
    updateScrollableRegions(panel);
    positionPanel(panel, lens.state.hud.lastConfig || {});
  }

  function eventVisible(config, eventType) {
    var type = String(eventType || 'GENERAL');
    if (type.indexOf('NETWORK_') === 0 && !option(config, 'showNetwork', true)) return false;
    if ((type === 'LOCATOR_RETRY' || type === 'ASSERTION_RETRY') && !option(config, 'showRetries', true)) return false;
    if ((type === 'WAIT' || type.indexOf('NETWORK_WAIT_') === 0) && !option(config, 'showWaits', true)) return false;
    if ((type === 'ASSERTION' || type.indexOf('ASSERTION_') === 0 || type.indexOf('BUSINESS_ASSERTION_') === 0 || type.indexOf('NETWORK_ASSERTION_') === 0) && !option(config, 'showAssertions', true)) return false;
    return true;
  }

  function acceptedTimestamp(value) {
    var raw = value == null ? '' : String(value).trim();
    // Only unambiguous ISO-8601 instants with an explicit UTC/offset suffix are accepted.
    var explicitInstant = /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:Z|[+-]\d{2}:\d{2})$/.test(raw);
    var millis = explicitInstant ? Date.parse(raw) : NaN;
    return Number.isFinite(millis) ? new Date(millis) : new Date();
  }

  function timestampZone(config) {
    var configured = option(config, 'timestampZone', 'SYSTEM');
    if (configured && configured !== 'SYSTEM') return configured;
    try { return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC'; }
    catch (ignored) { return 'UTC'; }
  }

  function readableTimestamp(date, format, zone) {
    var options = {timeZone:zone,hour:'2-digit',minute:'2-digit',second:'2-digit',hourCycle:'h23'};
    if (format === 'DATE_TIME') {
      options.day = '2-digit'; options.month = '2-digit'; options.year = '2-digit';
    }
    var parts;
    try { parts = new Intl.DateTimeFormat('en-GB', options).formatToParts(date); }
    catch (ignored) { options.timeZone = 'UTC'; parts = new Intl.DateTimeFormat('en-GB', options).formatToParts(date); }
    var values = {};
    parts.forEach(function(part) { if (part.type !== 'literal') values[part.type] = part.value; });
    var time = values.hour + ':' + values.minute + ':' + values.second;
    return format === 'DATE_TIME' ? values.day + '.' + values.month + '.' + values.year + ' ' + time : time;
  }

  function formatTimestamp(date, config) {
    var format = option(config, 'timestampFormat', 'ISO_UTC');
    if (format === 'TIME_ONLY' || format === 'DATE_TIME') {
      return readableTimestamp(date, format, timestampZone(config));
    }
    return date.toISOString();
  }

  function log(message, level, timestamp, eventType) {
    var config = lens.state.hud.lastConfig || {};
    if (!option(config, 'showEventLog', true) || !eventVisible(config, eventType)) return;
    var panel = ensurePanel(lens.state.hud.lastConfig || {});
    if (!panel) {
      return;
    }

    var logs = ensureLogs(panel, config);
    var row = document.createElement('div');
    row.style.fontFamily = 'var(--ui-test-lens-hud-event-font-family, var(--ui-test-lens-hud-font-family))';
    row.style.fontSize = 'var(--ui-test-lens-hud-font-size, 10px)';
    row.style.marginBottom = '5px';
    row.style.whiteSpace = 'pre-wrap';
    row.style.wordBreak = 'break-word';
    row.style.lineHeight = '1.32';

    var lvl = (level || '').toLowerCase();
    var color = 'var(--ui-test-lens-hud-fg, #ffffff)';
    if (lvl === 'warn') {
      color = 'var(--ui-test-lens-hud-warning, #ffd93b)';
    }
    if (lvl === 'error' || lvl === 'failed') {
      color = 'var(--ui-test-lens-hud-danger, #ff4c4c)';
    }
    if (lvl === 'success') {
      color = 'var(--ui-test-lens-hud-success, #00ff7f)';
    }
    if (lvl === 'royal') {
      color = 'var(--ui-test-lens-hud-accent, #4ca3ff)';
    }
    row.style.color = color;

    var eventTimestamp = acceptedTimestamp(timestamp);
    row.setAttribute('data-test-lens-timestamp', eventTimestamp.toISOString());
    var timestampText = option(config, 'showTimestamps', false) ? '[' + formatTimestamp(eventTimestamp, config) + ']' : '';
    row.textContent = timestampText + '[' + (level || '').toUpperCase() + '] ' + (message || '');
    logs.appendChild(row);
    updateScrollableRegions(panel);
    logs.scrollTop = logs.scrollHeight;
  }

  function clear() {
    var panel = ensurePanel(lens.state.hud.lastConfig || {});
    if (!panel) {
      return;
    }
    var logs = ensureLogs(panel, lens.state.hud.lastConfig || {});
    if (!logs) return;
    logs.textContent = '';
  }

  function remove() {
    var root = overlayRoot();
    if (!root) {
      return;
    }
    var panel = root.querySelector('#selenium-hud-panel');
    if (panel && panel.parentNode) {
      panel.parentNode.removeChild(panel);
    }
  }

  if (window.removeEventListener && lens.state.hud.resizeHandler) {
    window.removeEventListener('resize', lens.state.hud.resizeHandler);
  }
  lens.state.hud.resizeHandler = function () {
    var panel = ensurePanel(lens.state.hud.lastConfig || {});
    if (panel) {
      updateScrollableRegions(panel);
      positionPanel(panel, lens.state.hud.lastConfig || {});
    }
  };
  if (window.addEventListener) window.addEventListener('resize', lens.state.hud.resizeHandler);

  if (lens.state.hud.typographyUnsubscribe) lens.state.hud.typographyUnsubscribe();
  var sharedTypography = visualTypography();
  if (sharedTypography) {
    lens.state.hud.typographyUnsubscribe = sharedTypography.subscribe(function (status) {
      var panel = overlayRoot() && overlayRoot().querySelector('#selenium-hud-panel');
      if (!panel) return;
      panel.setAttribute('data-test-lens-font-status', status);
      panel.setAttribute('data-test-lens-font-reflow', String(Number(panel.getAttribute('data-test-lens-font-reflow') || 0) + 1));
      updateScrollableRegions(panel);
      positionPanel(panel, lens.state.hud.lastConfig || {});
    });
  }

  lens.modules.hud = {
    __uiTestLensHud: true,
    init: init,
    setStep: setStep,
    log: log,
    clear: clear,
    remove: remove,
    preset: function (name) {
      var value = HUD_PRESETS[String(name || '').toUpperCase()];
      if (!value) return null;
      var copy = {};
      Object.keys(HUD_PRESET_SHARED).forEach(function (key) { copy[key] = HUD_PRESET_SHARED[key]; });
      Object.keys(value).forEach(function (key) { copy[key] = value[key]; });
      return copy;
    }
  };
})(window, document);
