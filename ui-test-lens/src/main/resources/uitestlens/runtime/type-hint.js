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
  lens.state.typeHint = lens.state.typeHint || {};

  if (lens.modules.typeHint && lens.modules.typeHint.__uiTestLensTypeHint === true) {
    return;
  }

  var state = lens.state.typeHint;
  state.sequence = state.sequence || 0;

  function overlayRoot() {
    var root = lens.state.overlay.root || window.__seleniumOverlayRoot;
    if (root) {
      lens.state.overlay.root = root;
      window.__seleniumOverlayRoot = root;
      return root;
    }

    var host = document.getElementById('selenium-overlay-host');
    if (!host) {
      host = document.createElement('div');
      host.id = 'selenium-overlay-host';
      host.style.position = 'fixed';
      host.style.left = '0';
      host.style.top = '0';
      host.style.width = '0';
      host.style.height = '0';
      host.style.zIndex = '2147483647';
      host.style.pointerEvents = 'none';
      document.body.appendChild(host);
    }

    root = host.shadowRoot || host.attachShadow({ mode: 'open' });
    lens.state.overlay.root = root;
    window.__seleniumOverlayRoot = root;
    return root;
  }

  function options(value) {
    value = value || {};
    return {
      duration: Number.isFinite(Number(value.duration)) ? Number(value.duration) : 1000
    };
  }

  function show(element, valueOrLabel, rawOptions) {
    if (!element || !element.getBoundingClientRect) {
      return false;
    }

    var root = overlayRoot();
    if (!root) {
      return false;
    }

    var opts = options(rawOptions);
    var rect = element.getBoundingClientRect();
    var hint = document.createElement('div');
    hint.className = 'selenium-type-hint';
    hint.setAttribute('data-uitestlens-type-hint', '1');
    hint.textContent = valueOrLabel == null ? '' : String(valueOrLabel);
    hint.style.position = 'fixed';
    hint.style.left = (rect.right + 6) + 'px';
    hint.style.top = rect.top + 'px';
    hint.style.padding = '2px 6px';
    hint.style.fontSize = '10px';
    hint.style.background = 'rgba(0,0,0,0.8)';
    hint.style.color = '#ffffff';
    hint.style.borderRadius = '3px';
    hint.style.maxWidth = '200px';
    hint.style.zIndex = '2147483647';
    hint.style.pointerEvents = 'none';
    hint.style.boxShadow = '0 0 4px rgba(0,0,0,0.4)';

    root.appendChild(hint);
    state.sequence++;

    window.setTimeout(function () {
      hide(hint);
    }, opts.duration);
    return true;
  }

  function hide(elementOrId) {
    var node = null;
    if (typeof elementOrId === 'string') {
      var root = overlayRoot();
      node = root && root.querySelector ? root.querySelector('#' + elementOrId) : null;
    } else {
      node = elementOrId;
    }

    if (node && node.parentNode) {
      node.parentNode.removeChild(node);
      return true;
    }
    return false;
  }

  function clear() {
    var root = overlayRoot();
    if (!root || !root.querySelectorAll) {
      return 0;
    }

    var nodes = Array.prototype.slice.call(root.querySelectorAll(
      '.selenium-type-hint,[data-uitestlens-type-hint="1"]'
    ));
    nodes.forEach(function (node) {
      if (node && node.parentNode) {
        node.parentNode.removeChild(node);
      }
    });
    return nodes.length;
  }

  lens.modules.typeHint = {
    __uiTestLensTypeHint: true,
    show: show,
    hide: hide,
    clear: clear
  };
})(window, document);
