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

  if (lens.modules.hud && lens.modules.hud.__uiTestLensHud === true) {
    return;
  }

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

  var DEFAULT_THEME = {
    background: 'rgba(0, 0, 0, 0.75)',
    foreground: '#ffffff',
    mutedForeground: 'rgba(255,255,255,0.78)',
    accent: '#4ca3ff',
    success: '#00ff7f',
    warning: '#ffd93b',
    danger: '#ff4c4c',
    borderColor: 'rgba(255,255,255,0.2)',
    borderRadiusPx: 4,
    fontSizePx: 11,
    fontFamily: 'Arial, sans-serif',
    boxShadow: '0 2px 6px rgba(0,0,0,0.4)',
    opacity: 1,
    paddingPx: 8,
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
    panel.style.display = 'flex';
    panel.style.flexDirection = 'column';

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
    var logs = panel.querySelector('#selenium-hud-logs');
    if (!logs) {
      logs = document.createElement('div');
      logs.id = 'selenium-hud-logs';
      logs.style.marginTop = 'var(--ui-test-lens-hud-gap, 6px)';
      logs.style.maxHeight = '160px';
      logs.style.overflowY = 'auto';
      logs.style.borderTop = '1px solid var(--ui-test-lens-hud-border, rgba(255,255,255,0.2))';
      logs.style.paddingTop = '4px';
      panel.appendChild(logs);
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
    positionPanel(panel, config);
    panel.style.maxWidth = (config.maxWidth || 280) + 'px';
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

    var title = panel.querySelector('#selenium-hud-test');
    if (!title) {
      title = document.createElement('div');
      title.id = 'selenium-hud-test';
      panel.appendChild(title);
    }
    title.innerHTML = '<b>Test:</b> ' + valueOrDash(config.testName);

    var pipeline = panel.querySelector('#selenium-hud-pipeline');
    if (!pipeline) {
      pipeline = document.createElement('div');
      pipeline.id = 'selenium-hud-pipeline';
      panel.appendChild(pipeline);
    }
    pipeline.innerHTML = '<b>Pipeline:</b> ' + valueOrDash(config.pipelineId);

    var step = panel.querySelector('#selenium-hud-step');
    if (!step) {
      step = document.createElement('div');
      step.id = 'selenium-hud-step';
      step.style.marginTop = 'var(--ui-test-lens-hud-gap, 4px)';
      panel.appendChild(step);
    }
    if (!step.innerHTML) {
      step.innerHTML = '<b>Step:</b> -';
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
    step.innerHTML = '<b>Step:</b> ' + valueOrDash(stepDescription);
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
    row.style.marginBottom = '2px';
    row.style.whiteSpace = 'pre-wrap';
    row.style.wordBreak = 'break-word';

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
