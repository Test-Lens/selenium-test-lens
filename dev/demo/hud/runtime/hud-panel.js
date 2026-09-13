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

  function overlayRoot() {
    var root = lens.state.overlay.root || window.__seleniumOverlayRoot;
    if (root) {
      lens.state.overlay.root = root;
      window.__seleniumOverlayRoot = root;
      return root;
    }
    return null;
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

  function metadataRowMarkup(label, value, extraClass) {
    return '<div class="stl-hud-meta-row ' + extraClass + '" style="display:flex;align-items:baseline;gap:6px;min-width:0;line-height:1.18;margin:0;">'
      + '<span class="stl-hud-meta-label" style="flex:0 0 auto;color:var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78));font-size:8.5px;font-weight:500;text-transform:uppercase;letter-spacing:.13em;line-height:1;white-space:nowrap;opacity:.72;transform:translateY(1px);">' + label + '</span>'
      + '<span class="stl-hud-meta-value" style="min-width:0;color:var(--ui-test-lens-hud-fg, #ffffff);font-weight:500;font-size:13.5px;line-height:1.22;word-break:break-word;overflow-wrap:anywhere;">' + escapeHtml(value) + '</span>'
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
    fontFamily: 'Inter, system-ui, sans-serif',
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

  function applyTheme(panel, config) {
    var theme = (config && config.theme) || {};
    var borderRadius = themeValue(theme, 'borderRadiusPx');
    var fontSize = themeValue(theme, 'fontSizePx');
    var padding = themeValue(theme, 'paddingPx');
    var gap = themeValue(theme, 'gapPx');
    var maxHeight = positiveNumber(theme.maxHeightPx);

    setVar(panel, '--ui-test-lens-hud-bg', themeValue(theme, 'background'));
    setVar(panel, '--ui-test-lens-hud-fg', themeValue(theme, 'foreground'));
    setVar(panel, '--ui-test-lens-hud-muted-fg', themeValue(theme, 'mutedForeground'));
    setVar(panel, '--ui-test-lens-hud-accent', themeValue(theme, 'accent'));
    setVar(panel, '--ui-test-lens-hud-success', themeValue(theme, 'success'));
    setVar(panel, '--ui-test-lens-hud-warning', themeValue(theme, 'warning'));
    setVar(panel, '--ui-test-lens-hud-danger', themeValue(theme, 'danger'));
    setVar(panel, '--ui-test-lens-hud-border', themeValue(theme, 'borderColor'));
    setVar(panel, '--ui-test-lens-hud-radius', borderRadius + 'px');
    setVar(panel, '--ui-test-lens-hud-font-size', fontSize + 'px');
    setVar(panel, '--ui-test-lens-hud-font-family', themeValue(theme, 'fontFamily'));
    setVar(panel, '--ui-test-lens-hud-shadow', themeValue(theme, 'boxShadow'));
    setVar(panel, '--ui-test-lens-hud-opacity', themeValue(theme, 'opacity'));
    setVar(panel, '--ui-test-lens-hud-padding-y', padding + 'px');
    setVar(panel, '--ui-test-lens-hud-padding-x', (padding + 2) + 'px');
    setVar(panel, '--ui-test-lens-hud-gap', gap + 'px');

    panel.style.background = 'var(--ui-test-lens-hud-bg, ' + DEFAULT_THEME.background + ')';
    panel.style.color = 'var(--ui-test-lens-hud-fg, ' + DEFAULT_THEME.foreground + ')';
    panel.style.fontSize = 'var(--ui-test-lens-hud-font-size, 11px)';
    panel.style.fontFamily = 'var(--ui-test-lens-hud-font-family, Arial, sans-serif)';
    panel.style.padding = 'var(--ui-test-lens-hud-padding-y, 8px) var(--ui-test-lens-hud-padding-x, 10px)';
    panel.style.borderRadius = 'var(--ui-test-lens-hud-radius, 4px)';
    panel.style.boxShadow = 'var(--ui-test-lens-hud-shadow, 0 2px 6px rgba(0,0,0,0.4))';
    panel.style.opacity = 'var(--ui-test-lens-hud-opacity, 1)';
    panel.style.border = '1px solid var(--ui-test-lens-hud-border, rgba(255,255,255,0.2))';
    panel.style.boxSizing = 'border-box';
    panel.style.display = 'block';

    if (maxHeight) {
      setVar(panel, '--ui-test-lens-hud-max-height', maxHeight + 'px');
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

    panel.setAttribute('data-ui-test-lens-theme', config && config.themeName ? config.themeName : 'custom');
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

  function ensureStructure(panel) {
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

    var sideRail = shell.querySelector('.stl-hud-side-rail');
    if (!sideRail) {
      sideRail = document.createElement('div');
      sideRail.className = 'stl-hud-side-rail';
      sideRail.style.flex = '0 0 20px';
      sideRail.style.width = '20px';
      sideRail.style.boxSizing = 'border-box';
      sideRail.style.display = 'flex';
      sideRail.style.alignItems = 'center';
      sideRail.style.justifyContent = 'center';
      sideRail.style.padding = '1px 0';
      sideRail.style.marginRight = '6px';
      sideRail.style.borderRight = '1px solid color-mix(in srgb, var(--ui-test-lens-hud-border, rgba(255,255,255,0.2)) 68%, transparent)';
      sideRail.style.color = 'var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78))';
      shell.insertBefore(sideRail, shell.firstChild);
    }

    var railBrand = sideRail.querySelector('.stl-hud-rail-brand');
    if (!railBrand) {
      railBrand = document.createElement('div');
      railBrand.className = 'stl-hud-rail-brand';
      railBrand.style.display = 'inline-flex';
      railBrand.style.alignItems = 'center';
      railBrand.style.justifyContent = 'center';
      railBrand.style.gap = '3px';
      railBrand.style.whiteSpace = 'nowrap';
      railBrand.style.transform = 'rotate(-90deg)';
      railBrand.style.transformOrigin = 'center';
      railBrand.style.opacity = '0.76';
      railBrand.style.color = 'var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78))';
      sideRail.appendChild(railBrand);
    }

    var brandIcon = railBrand.querySelector('.stl-hud-brand-icon');
    if (!brandIcon) {
      brandIcon = document.createElement('span');
      brandIcon.className = 'stl-hud-brand-icon';
      brandIcon.style.display = 'inline-flex';
      brandIcon.style.alignItems = 'center';
      brandIcon.style.justifyContent = 'center';
      brandIcon.style.width = '14px';
      brandIcon.style.height = '14px';
      brandIcon.innerHTML = brandIconMarkup();
      railBrand.insertBefore(brandIcon, railBrand.firstChild);
    }

    var railText = railBrand.querySelector('.stl-hud-side-rail-text');
    if (!railText) {
      railText = document.createElement('span');
      railText.className = 'stl-hud-side-rail-text';
      railText.textContent = 'TEST LENS';
      railText.style.fontSize = '8px';
      railText.style.lineHeight = '1';
      railText.style.fontWeight = '650';
      railText.style.letterSpacing = '0.06em';
      railBrand.appendChild(railText);
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

    var oldHeader = main.querySelector('.stl-hud-header');
    if (oldHeader) {
      removeNode(oldHeader);
    }

    return {
      shell: shell,
      sideRail: sideRail,
      railBrand: railBrand,
      main: main
    };
  }

  function placeAfter(parent, node, previous) {
    var next = previous ? previous.nextSibling : parent.firstChild;
    if (node.parentNode !== parent || node.previousSibling !== previous) {
      parent.insertBefore(node, next);
    }
  }

  function migrateHudContent(panel, structure) {
    var previous = null;
    var title = panel.querySelector('#selenium-hud-test');
    if (title) {
      placeAfter(structure.main, title, previous);
      previous = title;
    }

    var pipeline = panel.querySelector('#selenium-hud-pipeline');
    if (pipeline) {
      placeAfter(structure.main, pipeline, previous);
      previous = pipeline;
    }

    var step = panel.querySelector('#selenium-hud-step');
    if (step) {
      placeAfter(structure.main, step, previous);
      previous = step;
    }

    var logs = panel.querySelector('#selenium-hud-logs');
    if (logs && logs.parentNode !== structure.main) {
      structure.main.appendChild(logs);
    }
  }

  function positionPanel(panel, config) {
    var position = config.position || 'BOTTOM_RIGHT';
    var offsetX = config.offsetX == null ? 10 : config.offsetX;
    var offsetY = config.offsetY == null ? 10 : config.offsetY;

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

  function ensureLogs(panel) {
    var structure = ensureStructure(panel);
    migrateHudContent(panel, structure);
    var logs = panel.querySelector('#selenium-hud-logs');
    if (!logs) {
      logs = document.createElement('div');
      logs.id = 'selenium-hud-logs';
      logs.style.marginTop = '7px';
      logs.style.maxHeight = '160px';
      logs.style.overflowY = 'auto';
      logs.style.borderTop = '1px solid var(--ui-test-lens-hud-border, rgba(255,255,255,0.2))';
      logs.style.paddingTop = '8px';
      structure.main.appendChild(logs);
    } else if (logs.parentNode !== structure.main) {
      structure.main.appendChild(logs);
    }
    logs.style.flex = '1 1 auto';
    logs.style.minHeight = '0';
    return logs;
  }

  function updateScrollableRegions(panel) {
    var config = lens.state.hud.lastConfig || {};
    var theme = (config && config.theme) || {};
    var maxHeight = positiveNumber(theme.maxHeightPx);
    var logs = panel.querySelector('#selenium-hud-logs');
    if (!logs) {
      return;
    }

    if (!maxHeight) {
      logs.style.maxHeight = '160px';
      logs.style.overflowY = 'auto';
      return;
    }

    var title = panel.querySelector('#selenium-hud-test');
    var pipeline = panel.querySelector('#selenium-hud-pipeline');
    var step = panel.querySelector('#selenium-hud-step');
    var styles = window.getComputedStyle ? window.getComputedStyle(panel) : null;
    var paddingTop = styles ? parseFloat(styles.paddingTop) || 0 : 0;
    var paddingBottom = styles ? parseFloat(styles.paddingBottom) || 0 : 0;
    var fixedHeight = paddingTop + paddingBottom;

    [title, pipeline, step].forEach(function (node) {
      if (node) {
        fixedHeight += node.offsetHeight || 0;
      }
    });

    var logMarginTop = logs.offsetTop && step ? Math.max(0, logs.offsetTop - (step.offsetTop + step.offsetHeight)) : 0;
    var availableLogHeight = Math.max(80, Math.floor(maxHeight - fixedHeight - logMarginTop - 8));
    logs.style.maxHeight = availableLogHeight + 'px';
    logs.style.overflowY = 'auto';
  }

  function ensurePanel(config) {
    var shadow = overlayRoot();
    if (!shadow) {
      return null;
    }

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
    var structure = ensureStructure(panel);
    migrateHudContent(panel, structure);
    positionPanel(panel, config);
    panel.style.maxWidth = (config.maxWidth || 520) + 'px';
    updateScrollableRegions(panel);
    return panel;
  }

  function init(config) {
    config = config || {};
    lens.state.hud.lastConfig = config;

    var panel = ensurePanel(config);
    if (!panel) {
      return;
    }
    var structure = ensureStructure(panel);

    var title = panel.querySelector('#selenium-hud-test');
    if (!title) {
      title = document.createElement('div');
      title.id = 'selenium-hud-test';
    }
    title.style.lineHeight = '1.18';
    title.style.margin = '0';
    title.style.padding = '0';
    title.innerHTML = metadataRowMarkup('TEST', config.testName, 'stl-hud-test-row');
    placeAfter(structure.main, title, null);

    var pipeline = panel.querySelector('#selenium-hud-pipeline');
    if (!pipeline) {
      pipeline = document.createElement('div');
      pipeline.id = 'selenium-hud-pipeline';
    }
    pipeline.style.color = 'var(--ui-test-lens-hud-muted-fg, rgba(255,255,255,0.78))';
    pipeline.style.fontSize = '12px';
    pipeline.style.lineHeight = '1.18';
    pipeline.style.marginTop = '3px';
    pipeline.style.display = !config.pipelineId || config.pipelineId === '-' ? 'none' : 'block';
    pipeline.innerHTML = metadataRowMarkup('PIPE', config.pipelineId, 'stl-hud-pipeline-row');
    placeAfter(structure.main, pipeline, title);

    var step = panel.querySelector('#selenium-hud-step');
    if (!step) {
      step = document.createElement('div');
      step.id = 'selenium-hud-step';
    }
    step.style.marginTop = '4px';
    step.style.lineHeight = '1.2';
    step.style.fontSize = '13.5px';
    step.style.wordBreak = 'break-word';
    placeAfter(structure.main, step, pipeline);
    if (!step.innerHTML) {
      step.innerHTML = metadataRowMarkup('STEP', '-', 'stl-hud-step-row');
    }

    ensureLogs(panel);
    updateScrollableRegions(panel);
  }

  function setStep(stepDescription) {
    var panel = ensurePanel(lens.state.hud.lastConfig || {});
    if (!panel) {
      return;
    }

    var step = panel.querySelector('#selenium-hud-step');
    if (!step) {
      return;
    }
    step.innerHTML = metadataRowMarkup('STEP', stepDescription, 'stl-hud-step-row');
  }

  function log(message, level, timestamp) {
    var panel = ensurePanel(lens.state.hud.lastConfig || {});
    if (!panel) {
      return;
    }

    var logs = ensureLogs(panel);
    var row = document.createElement('div');
    row.style.fontFamily = 'monospace';
    row.style.fontSize = '10px';
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

    row.textContent = '[' + (timestamp || '') + '][' + (level || '').toUpperCase() + '] ' + (message || '');
    logs.appendChild(row);
    updateScrollableRegions(panel);
    logs.scrollTop = logs.scrollHeight;
  }

  function clear() {
    var panel = ensurePanel(lens.state.hud.lastConfig || {});
    if (!panel) {
      return;
    }
    var logs = ensureLogs(panel);
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

  lens.modules.hud = {
    __uiTestLensHud: true,
    init: init,
    setStep: setStep,
    log: log,
    clear: clear,
    remove: remove
  };
})(window, document);
