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
  lens.state.typography = lens.state.typography || {};

  if (lens.modules.visualTypography && lens.modules.visualTypography.__uiTestLensVisualTypography === true) {
    return;
  }

  var state = lens.state.typography;
  state.status = state.status || 'idle';
  state.listeners = state.listeners || [];
  var FONT_FAMILY = 'Test Lens Sora';
  var UI_STACK = '"' + FONT_FAMILY + '", system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
  var SYSTEM_STACK = 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif';
  var MONOSPACE_STACK = 'ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace';
  var LOAD_TIMEOUT_MS = 2000;

  function notify(status) {
    state.status = status;
    state.revision = (state.revision || 0) + 1;
    state.listeners.slice().forEach(function (listener) {
      try { listener(status); } catch (ignored) { }
    });
  }

  function subscribe(listener) {
    if (typeof listener !== 'function') return function () {};
    state.listeners.push(listener);
    return function () {
      var index = state.listeners.indexOf(listener);
      if (index >= 0) state.listeners.splice(index, 1);
    };
  }

  function ensureRoot(root) {
    if (!root || !root.querySelector || !root.appendChild
        || root.querySelector('style[data-test-lens-visual-typography]')) return;
    var style = document.createElement('style');
    style.setAttribute('data-test-lens-visual-typography', 'true');
    style.textContent = ':host{--ui-test-lens-font-family:' + UI_STACK + ';}';
    root.appendChild(style);
  }

  function installBytes(bytes) {
    if (state.loadPromise) return state.loadPromise;
    if (!bytes || !window.FontFace || !document.fonts || typeof document.fonts.add !== 'function') {
      notify('unsupported');
      state.loadPromise = Promise.resolve(false);
      return state.loadPromise;
    }

    try {
      state.fontFace = new window.FontFace(FONT_FAMILY, bytes, {
        style: 'normal',
        weight: '100 800',
        display: 'swap'
      });
      document.fonts.add(state.fontFace);
      notify('loading');
      var loaded = state.fontFace.load().then(function () {
        notify('loaded');
        return true;
      }, function () {
        if (document.fonts && typeof document.fonts.delete === 'function') {
          try { document.fonts.delete(state.fontFace); } catch (ignored) { }
        }
        notify('failed');
        return false;
      });
      var bounded = new Promise(function (resolve) {
        window.setTimeout(function () {
          if (state.status === 'loading') notify('timeout');
          resolve(false);
        }, LOAD_TIMEOUT_MS);
      });
      state.loadPromise = Promise.race([loaded, bounded]);
      return state.loadPromise;
    } catch (ignored) {
      notify('failed');
      state.loadPromise = Promise.resolve(false);
      return state.loadPromise;
    }
  }

  function installBase64(value) {
    if (state.loadPromise) return state.loadPromise;
    try {
      var binary = window.atob(String(value || ''));
      var bytes = new Uint8Array(binary.length);
      for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
      return installBytes(bytes.buffer);
    } catch (ignored) {
      notify('failed');
      state.loadPromise = Promise.resolve(false);
      return state.loadPromise;
    }
  }

  lens.modules.visualTypography = {
    __uiTestLensVisualTypography: true,
    family: FONT_FAMILY,
    uiStack: UI_STACK,
    systemStack: SYSTEM_STACK,
    monospaceStack: MONOSPACE_STACK,
    status: function () { return state.status; },
    subscribe: subscribe,
    ensureRoot: ensureRoot,
    installBytes: installBytes,
    installBase64: installBase64
  };

  var script = document.currentScript;
  if (script && script.src && window.fetch) {
    var fontUrl = new URL('fonts/Sora-wght.woff2', script.src).href;
    window.fetch(fontUrl).then(function (response) {
      if (!response.ok) throw new Error('Unable to load bundled Test Lens UI font');
      return response.arrayBuffer();
    }).then(installBytes, function () { notify('failed'); });
  }
})(window, document);
