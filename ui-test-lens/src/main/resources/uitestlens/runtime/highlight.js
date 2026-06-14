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
  lens.state.highlight = lens.state.highlight || {};

  if (lens.modules.highlight && lens.modules.highlight.__uiTestLensHighlight === true) {
    return;
  }

  var state = lens.state.highlight;
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

  function ensureStyle(root) {
    if (!root || !root.querySelector || root.querySelector('#selenium-highlight-style')) {
      return;
    }

    var style = document.createElement('style');
    style.id = 'selenium-highlight-style';
    style.textContent = ''
      + '.selenium-overlay-highlight,.selenium-overlay-highlight-parent,.selenium-overlay-highlight-closest{'
      + 'position:fixed;border-style:solid;border-width:2px;border-radius:4px;box-sizing:border-box;'
      + 'pointer-events:none;z-index:2147483647;}'
      + '.selenium-overlay-highlight-badge{position:absolute;top:-18px;left:0;padding:2px 6px;'
      + 'font-size:10px;line-height:1.3;color:#000;border-radius:3px;white-space:nowrap;'
      + 'font-family:Arial,sans-serif;}';
    root.appendChild(style);
  }

  function options(value) {
    value = value || {};
    return {
      duration: Number.isFinite(Number(value.duration)) ? Number(value.duration) : 1000,
      color: value.color || '#ffeb3b',
      className: value.className || 'selenium-overlay-highlight'
    };
  }

  function resolveParent(element, levels) {
    var target = element;
    var remaining = Math.max(1, Number(levels) || 1);
    while (remaining > 0 && target && target.parentElement) {
      target = target.parentElement;
      remaining--;
    }
    return target;
  }

  function resolveClosest(element, selector) {
    if (!element || !selector) {
      return null;
    }
    if (element.closest) {
      return element.closest(selector);
    }

    var node = element;
    while (node && node !== document.body) {
      if (node.matches && node.matches(selector)) {
        return node;
      }
      node = node.parentElement;
    }
    return null;
  }

  function decorate(target, label, rawOptions) {
    if (!target || !target.getBoundingClientRect) {
      return false;
    }

    var root = overlayRoot();
    if (!root) {
      return false;
    }
    ensureStyle(root);

    var opts = options(rawOptions);
    var container = document.createElement('div');
    container.className = opts.className;
    container.setAttribute('data-uitestlens-highlight', '1');
    container.style.borderColor = opts.color;

    var badge = document.createElement('div');
    badge.className = 'selenium-overlay-highlight-badge';
    badge.textContent = label || '';
    badge.style.background = opts.color;
    container.appendChild(badge);

    root.appendChild(container);
    state.sequence++;

    function update() {
      try {
        if (!document.body.contains(target)) {
          return;
        }
        var rect = target.getBoundingClientRect();
        container.style.left = rect.left + 'px';
        container.style.top = rect.top + 'px';
        container.style.width = rect.width + 'px';
        container.style.height = rect.height + 'px';
      } catch (ignored) {
      }
    }

    function onScroll() {
      update();
    }

    function cleanup() {
      window.removeEventListener('scroll', onScroll, true);
      window.removeEventListener('resize', onScroll, true);
      if (container && container.parentNode) {
        container.parentNode.removeChild(container);
      }
    }

    window.addEventListener('scroll', onScroll, true);
    window.addEventListener('resize', onScroll, true);
    update();
    window.setTimeout(cleanup, opts.duration);
    return true;
  }

  function element(target, label, rawOptions) {
    return decorate(target, label, rawOptions);
  }

  function parent(target, levels, label, rawOptions) {
    return decorate(resolveParent(target, levels), label, Object.assign({}, rawOptions, {
      className: 'selenium-overlay-highlight-parent'
    }));
  }

  function ancestor(target, levels, label, rawOptions) {
    return parent(target, levels, label, rawOptions);
  }

  function closest(target, selector, label, rawOptions) {
    return decorate(resolveClosest(target, selector), label, Object.assign({}, rawOptions, {
      className: 'selenium-overlay-highlight-closest'
    }));
  }

  function clear() {
    var root = overlayRoot();
    if (!root || !root.querySelectorAll) {
      return 0;
    }

    var nodes = Array.prototype.slice.call(root.querySelectorAll(
      '.selenium-overlay-highlight,.selenium-overlay-highlight-parent,.selenium-overlay-highlight-closest,[data-uitestlens-highlight="1"]'
    ));
    nodes.forEach(function (node) {
      if (node && node.parentNode) {
        node.parentNode.removeChild(node);
      }
    });
    return nodes.length;
  }

  lens.modules.highlight = {
    __uiTestLensHighlight: true,
    element: element,
    parent: parent,
    ancestor: ancestor,
    closest: closest,
    clear: clear
  };
})(window, document);
