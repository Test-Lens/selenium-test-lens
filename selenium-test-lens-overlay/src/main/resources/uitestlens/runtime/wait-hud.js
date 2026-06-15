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
  lens.state.wait = lens.state.wait || {};
  lens.state.overlay = lens.state.overlay || {};

  if (lens.modules.waitHud && lens.modules.waitHud.__uiTestLensWaitHud === true) {
    window.__seleniumWaitHud = lens.modules.waitHud;
    syncLegacyState();
    return;
  }

  var state = lens.state.wait;
  state.lastMessage = state.lastMessage || window.__seleniumLastWaitMessage || '';
  state.lastElapsedMs = state.lastElapsedMs || window.__seleniumLastWaitElapsedMs || 0;
  state.active = !!state.active;

  function syncLegacyState() {
    window.__seleniumWaitHud = lens.modules.waitHud;
    window.__seleniumLastWaitMessage = state.lastMessage || '';
    window.__seleniumLastWaitElapsedMs = state.lastElapsedMs || 0;
  }

  function rootNode() {
    var root = lens.state.overlay.root || window.__seleniumOverlayRoot;
    if (root) {
      lens.state.overlay.root = root;
      window.__seleniumOverlayRoot = root;
      return root;
    }
    return document.body || document.documentElement;
  }

  function text(value, fallback) {
    if (value == null || value === '') {
      return fallback || '';
    }
    return String(value);
  }

  function safeNumber(value, fallback) {
    var number = Number(value);
    return Number.isFinite(number) ? number : fallback;
  }

  function removeBySelector(selector) {
    var root = rootNode();
    var nodes = [];
    if (root && root.querySelectorAll) {
      nodes = Array.prototype.slice.call(root.querySelectorAll(selector));
    }
    if (document.querySelectorAll) {
      nodes = nodes.concat(Array.prototype.slice.call(document.querySelectorAll(selector)));
    }

    nodes.forEach(function (node) {
      if (node && node.parentNode) {
        node.parentNode.removeChild(node);
      }
    });
  }

  function ensureStyle(root) {
    if (!root || !root.querySelector || root.querySelector('#selenium-wait-hud-style')) {
      return;
    }

    var style = document.createElement('style');
    style.id = 'selenium-wait-hud-style';
    style.textContent = ''
      + '#selenium-wait-hud{position:fixed;left:50%;bottom:42px;transform:translateX(-50%);'
      + 'z-index:2147483647;min-width:220px;max-width:min(560px,calc(100vw - 32px));'
      + 'padding:8px 12px;border-radius:8px;background:rgba(17,24,39,.94);color:#f9fafb;'
      + 'box-shadow:0 10px 30px rgba(0,0,0,.28);font:12px/1.4 Arial,sans-serif;'
      + 'pointer-events:none;border:1px solid rgba(148,163,184,.35);}'
      + '#selenium-wait-hud[data-status="done"]{border-color:rgba(34,197,94,.7);}'
      + '#selenium-wait-hud[data-status="failed"]{border-color:rgba(239,68,68,.8);}'
      + '#selenium-wait-indicator{position:fixed;bottom:8px;left:50%;transform:translateX(-50%);'
      + 'padding:4px 10px;background:rgba(0,0,0,.8);color:#fff;font:11px/1.4 Arial,sans-serif;'
      + 'border-radius:12px;z-index:2147483647;pointer-events:none;}';
    root.appendChild(style);
  }

  function ensureHud() {
    var root = rootNode();
    ensureStyle(root);

    var hud = root && root.querySelector ? root.querySelector('#selenium-wait-hud') : null;
    if (!hud) {
      hud = document.createElement('div');
      hud.id = 'selenium-wait-hud';
      hud.setAttribute('data-selenium-wait', '1');
      if (root && root.appendChild) {
        root.appendChild(hud);
      }
    }
    return hud;
  }

  function ensureIndicator() {
    var root = rootNode();
    ensureStyle(root);

    var indicator = root && root.querySelector ? root.querySelector('#selenium-wait-indicator') : null;
    if (!indicator) {
      indicator = document.createElement('div');
      indicator.id = 'selenium-wait-indicator';
      indicator.setAttribute('data-selenium-wait', '1');
      if (root && root.appendChild) {
        root.appendChild(indicator);
      }
    }
    return indicator;
  }

  function start(message) {
    state.active = true;
    state.startedAt = Date.now();
    state.lastMessage = text(message, 'Waiting...');
    state.lastElapsedMs = 0;

    var hud = ensureHud();
    hud.dataset.status = 'active';
    hud.textContent = 'Waiting: ' + state.lastMessage;
    syncLegacyState();
    return true;
  }

  function stop(status, elapsedMs) {
    state.active = false;
    state.lastMessage = text(status, state.lastMessage || 'Wait finished');
    state.lastElapsedMs = safeNumber(elapsedMs, state.lastElapsedMs || 0);

    var hud = ensureHud();
    var lower = state.lastMessage.toLowerCase();
    hud.dataset.status = lower.indexOf('fail') >= 0 || lower.indexOf('timeout') >= 0 ? 'failed' : 'done';
    hud.textContent = state.lastMessage + ' (' + state.lastElapsedMs + ' ms)';
    syncLegacyState();
    return true;
  }

  function showIndicator(message) {
    state.lastMessage = text(message, 'Waiting...');
    var indicator = ensureIndicator();
    indicator.textContent = 'Waiting: ' + state.lastMessage;
    syncLegacyState();
    return true;
  }

  function hideIndicator() {
    removeBySelector('#selenium-wait-indicator');
    syncLegacyState();
    return true;
  }

  function forceHide() {
    removeBySelector('#selenium-wait-indicator, #selenium-wait-hud, [data-selenium-wait="1"]');
    state.active = false;
    syncLegacyState();
    return true;
  }

  var api = {
    __uiTestLensWaitHud: true,
    start: start,
    stop: stop,
    showIndicator: showIndicator,
    hideIndicator: hideIndicator,
    forceHide: forceHide
  };

  lens.modules.waitHud = api;
  window.__seleniumWaitHud = api;
  syncLegacyState();
})(window, document);
