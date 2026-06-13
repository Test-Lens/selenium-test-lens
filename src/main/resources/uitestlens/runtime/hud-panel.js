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
      logs.style.marginTop = '6px';
      logs.style.maxHeight = '160px';
      logs.style.overflowY = 'auto';
      logs.style.borderTop = '1px solid rgba(255,255,255,0.2)';
      logs.style.paddingTop = '4px';
      panel.appendChild(logs);
    }
    return logs;
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
      panel.style.background = 'rgba(0, 0, 0, 0.75)';
      panel.style.color = '#ffffff';
      panel.style.fontSize = '11px';
      panel.style.padding = '8px 10px';
      panel.style.borderRadius = '4px';
      panel.style.boxShadow = '0 2px 6px rgba(0,0,0,0.4)';
      panel.style.pointerEvents = 'auto';
      panel.style.lineHeight = '1.4';
      shadow.appendChild(panel);
    }

    positionPanel(panel, config);
    panel.style.maxWidth = (config.maxWidth || 280) + 'px';
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
      step.style.marginTop = '4px';
      panel.appendChild(step);
    }
    if (!step.innerHTML) {
      step.innerHTML = '<b>Step:</b> -';
    }

    ensureLogs(panel);
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
    var color = '#ffffff';
    if (lvl === 'warn') {
      color = '#ffd93b';
    }
    if (lvl === 'error' || lvl === 'failed') {
      color = '#ff4c4c';
    }
    if (lvl === 'success') {
      color = '#00ff7f';
    }
    if (lvl === 'royal') {
      color = '#4ca3ff';
    }
    row.style.color = color;

    row.textContent = '[' + (timestamp || '') + '][' + (level || '').toUpperCase() + '] ' + (message || '');
    logs.appendChild(row);
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
