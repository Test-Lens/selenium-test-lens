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
  lens.state.assertionBadges = lens.state.assertionBadges || {};

  if (lens.modules.assertionBadges && lens.modules.assertionBadges.__uiTestLensAssertionBadges === true) {
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

  function removeContainer(target, container, onScroll) {
    window.removeEventListener('scroll', onScroll, true);
    window.removeEventListener('resize', onScroll, true);
    if (container && container.parentNode) {
      container.parentNode.removeChild(container);
      if (target) {
        try {
          delete target.__seleniumAssertContainer;
        } catch (e) {
          target.__seleniumAssertContainer = null;
        }
      }
    }
  }

  function show(target, result, options) {
    result = result || {};
    options = options || {};

    var ok = !!result.ok;
    var duration = options.duration == null ? 1500 : options.duration;
    var label = result.label;

    if (!target || !target.getBoundingClientRect) {
      return;
    }

    var shadow = overlayRoot();
    if (!shadow) {
      return;
    }

    var container = target.__seleniumAssertContainer;
    if (!container) {
      container = document.createElement('div');
      container.className = 'selenium-overlay-assert';
      container.style.position = 'fixed';
      container.style.pointerEvents = 'none';
      container.style.zIndex = '2147483647';
      container.style.boxSizing = 'border-box';
      container.style.borderRadius = '4px';
      target.__seleniumAssertContainer = container;
      shadow.appendChild(container);
    }

    var baseColor = '#4caf50';
    var failColor = '#f44336';
    if (!ok) {
      container.dataset.hasFail = 'true';
    }
    var color = (container.dataset.hasFail === 'true') ? failColor : baseColor;
    container.style.border = '2px solid ' + color;

    var badge = document.createElement('div');
    badge.className = 'selenium-assert-badge';
    badge.textContent = label || (ok ? 'ASSERT OK' : 'ASSERT FAIL');
    badge.style.position = 'absolute';
    badge.style.left = '0';
    badge.style.padding = '2px 6px';
    badge.style.fontSize = '10px';
    badge.style.background = color;
    badge.style.color = '#fff';
    badge.style.borderRadius = '3px';
    badge.style.whiteSpace = 'nowrap';

    var existingBadges = container.querySelectorAll('.selenium-assert-badge');
    var count = existingBadges.length;
    var offset = (count + 1) * 18;
    badge.style.top = '-' + offset + 'px';

    container.appendChild(badge);

    function update() {
      if (!document.body.contains(target)) {
        return;
      }
      var rect = target.getBoundingClientRect();
      container.style.left = rect.left + 'px';
      container.style.top = rect.top + 'px';
      container.style.width = rect.width + 'px';
      container.style.height = rect.height + 'px';
    }

    function onScroll() {
      update();
    }

    window.addEventListener('scroll', onScroll, true);
    window.addEventListener('resize', onScroll, true);
    update();
    window.setTimeout(function () {
      removeContainer(target, container, onScroll);
    }, duration);
  }

  function clear() {
    var root = overlayRoot();
    if (!root || !root.querySelectorAll) {
      return;
    }
    Array.prototype.slice.call(root.querySelectorAll('.selenium-overlay-assert'))
      .forEach(function (node) {
        if (node && node.parentNode) {
          node.parentNode.removeChild(node);
        }
      });
  }

  lens.modules.assertionBadges = {
    __uiTestLensAssertionBadges: true,
    show: show,
    clear: clear
  };
})(window, document);
