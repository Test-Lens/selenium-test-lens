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
  lens.state.scrollArrow = lens.state.scrollArrow || {};

  if (lens.modules.scrollArrow && lens.modules.scrollArrow.__uiTestLensScrollArrow === true) {
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

  function clear() {
    var root = overlayRoot();
    var removed = 0;
    if (root && root.querySelectorAll) {
      Array.prototype.slice.call(root.querySelectorAll('#selenium-scroll-indicator,[data-uitestlens-scroll-arrow="1"]'))
        .forEach(function (node) {
          if (node && node.parentNode) {
            node.parentNode.removeChild(node);
            removed++;
          }
        });
    }
    return removed;
  }

  function hide() {
    clear();
    return true;
  }

  function show(target, options) {
    options = options || {};
    var root = overlayRoot();
    if (!root || !target || !target.getBoundingClientRect) {
      return null;
    }

    clear();

    var rect = target.getBoundingClientRect();
    var startY = window.scrollY || window.pageYOffset || 0;
    var vh = window.innerHeight || document.documentElement.clientHeight || 800;
    var elemEdge = options.elementEdge || 'CENTER';
    var viewEdge = options.viewportEdge || 'CENTER';

    var elemAnchor;
    if (elemEdge === 'TOP') {
      elemAnchor = rect.top + startY;
    } else if (elemEdge === 'BOTTOM') {
      elemAnchor = rect.bottom + startY;
    } else {
      elemAnchor = rect.top + startY + rect.height / 2;
    }

    var viewportOffset;
    if (viewEdge === 'TOP') {
      viewportOffset = 0;
    } else if (viewEdge === 'BOTTOM') {
      viewportOffset = vh;
    } else {
      viewportOffset = vh / 2;
    }

    var targetY = elemAnchor - viewportOffset;
    var dirDown = targetY > startY;

    var arrow = document.createElement('div');
    arrow.id = 'selenium-scroll-indicator';
    arrow.setAttribute('data-uitestlens-scroll-arrow', '1');
    arrow.style.position = 'fixed';
    arrow.style.width = '0';
    arrow.style.height = '0';
    arrow.style.zIndex = '2147483647';
    arrow.style.pointerEvents = 'none';
    arrow.style.borderLeft = '10px solid transparent';
    arrow.style.borderRight = '10px solid transparent';
    if (dirDown) {
      arrow.style.borderTop = '14px solid #ffeb3b';
      arrow.style.top = 'calc(100vh - 40px)';
    } else {
      arrow.style.borderBottom = '14px solid #ffeb3b';
      arrow.style.top = '40px';
    }
    arrow.style.left = '50%';
    arrow.style.transform = 'translateX(-50%)';
    root.appendChild(arrow);
    return arrow;
  }

  function scrollToElementWithArrow(target, duration, elemEdge, viewEdge, done) {
    done = typeof done === 'function' ? done : function () {};
    duration = duration || 800;

    if (!target || !target.getBoundingClientRect) {
      done();
      return;
    }

    var root = overlayRoot();
    if (!root) {
      done();
      return;
    }

    var rect = target.getBoundingClientRect();
    var startY = window.scrollY || window.pageYOffset || 0;
    var vh = window.innerHeight || document.documentElement.clientHeight || 800;

    var elemAnchor;
    if (elemEdge === 'TOP') {
      elemAnchor = rect.top + startY;
    } else if (elemEdge === 'BOTTOM') {
      elemAnchor = rect.bottom + startY;
    } else {
      elemAnchor = rect.top + startY + rect.height / 2;
    }

    var viewportOffset;
    if (viewEdge === 'TOP') {
      viewportOffset = 0;
    } else if (viewEdge === 'BOTTOM') {
      viewportOffset = vh;
    } else {
      viewportOffset = vh / 2;
    }

    var targetY = elemAnchor - viewportOffset;
    var dirDown = targetY > startY;
    var arrow = show(target, {
      elementEdge: elemEdge,
      viewportEdge: viewEdge
    });

    if (!arrow) {
      done();
      return;
    }

    var startTime = null;
    function step(ts) {
      if (!startTime) {
        startTime = ts;
      }
      var progress = (ts - startTime) / duration;
      if (progress > 1) {
        progress = 1;
      }
      var y = startY + (targetY - startY) * progress;
      window.scrollTo(0, y);
      var current = window.scrollY || window.pageYOffset || 0;
      if (progress >= 1 || Math.abs(current - targetY) < 2) {
        var r = target.getBoundingClientRect();
        var arrowY;
        if (dirDown) {
          arrowY = r.top - 18;
        } else {
          arrowY = r.bottom + 4;
        }
        arrow.style.top = arrowY + 'px';
        arrow.style.left = (r.left + r.width / 2) + 'px';
        arrow.style.transform = 'translateX(-50%)';
        window.setTimeout(function () {
          if (arrow && arrow.parentNode) {
            arrow.parentNode.removeChild(arrow);
          }
          done();
        }, 1000);
        return;
      }
      window.requestAnimationFrame(step);
    }
    window.requestAnimationFrame(step);
  }

  lens.modules.scrollArrow = {
    __uiTestLensScrollArrow: true,
    show: show,
    hide: hide,
    clear: clear,
    scrollToElementWithArrow: scrollToElementWithArrow
  };
})(window, document);
