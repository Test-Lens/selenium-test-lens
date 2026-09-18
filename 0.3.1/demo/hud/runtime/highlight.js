(function (window, document) {
  'use strict';
  window.__uiTestLens = window.__uiTestLens || { version: '1.0-SNAPSHOT', modules: {}, state: {} };
  var lens = window.__uiTestLens;
  lens.modules = lens.modules || {}; lens.state = lens.state || {};
  lens.state.overlay = lens.state.overlay || {}; lens.state.highlight = lens.state.highlight || {};
  if (lens.modules.highlight && lens.modules.highlight.__uiTestLensHighlight === true) return;
  var state = lens.state.highlight;
  state.sequence = state.sequence || 0; state.records = state.records || [];
  state.activeByTarget = state.activeByTarget || (typeof WeakMap === 'function' ? new WeakMap() : null);

  function overlayRoot() {
    var root = lens.state.overlay.root || window.__seleniumOverlayRoot;
    if (root) return root;
    var host = document.getElementById('selenium-overlay-host');
    if (!host) {
      host = document.createElement('div'); host.id = 'selenium-overlay-host';
      host.style.cssText = 'position:fixed;left:0;top:0;width:0;height:0;z-index:2147483647;pointer-events:none';
      if (!document.body) return null; document.body.appendChild(host);
    }
    root = host.shadowRoot || host.attachShadow({ mode: 'open' });
    lens.state.overlay.root = root; window.__seleniumOverlayRoot = root; return root;
  }
  function ensureStyle(root) {
    if (!root || !root.querySelector || root.querySelector('#selenium-highlight-style')) return;
    var typography = lens.modules.visualTypography; if (typography) typography.ensureRoot(root);
    var style = document.createElement('style'); style.id = 'selenium-highlight-style';
    style.textContent = '.selenium-overlay-highlight,.selenium-overlay-highlight-parent,.selenium-overlay-highlight-closest{position:fixed;border-style:solid;border-radius:4px;box-sizing:border-box;pointer-events:none;z-index:2147483647}.selenium-overlay-highlight-badge{position:absolute;top:-18px;left:0;padding:2px 6px;font-size:10px;line-height:1.3;color:#000;border-radius:3px;white-space:nowrap;pointer-events:none;font-family:var(--ui-test-lens-font-family,"Test Lens Sora",system-ui,sans-serif)}';
    root.appendChild(style);
  }
  function options(value) {
    value = value || {};
    return { duration: isFinite(Number(value.duration)) ? Math.max(0, Number(value.duration)) : 1500,
      color: value.color || '#ffeb3b', className: value.className || 'selenium-overlay-highlight',
      borderWidth: isFinite(Number(value.borderWidth)) ? Math.max(1, Number(value.borderWidth)) : 2,
      showLabel: value.showLabel !== false, state: value.state || 'action' };
  }
  function connected(target) {
    if (!target) return false;
    if (typeof target.isConnected === 'boolean') return target.isConnected;
    var root = target.getRootNode ? target.getRootNode() : document;
    return root === document ? document.documentElement.contains(target) : !!root && !!root.host && connected(root.host);
  }
  function removeRecord(record) {
    if (!record || record.removed) return; record.removed = true;
    if (record.timer != null) window.clearTimeout(record.timer);
    window.removeEventListener('scroll', record.listener, true); window.removeEventListener('resize', record.listener, true);
    if (record.container && record.container.parentNode) record.container.parentNode.removeChild(record.container);
    if (state.activeByTarget && state.activeByTarget.get(record.target) === record) state.activeByTarget.delete(record.target);
    var index = state.records.indexOf(record); if (index >= 0) state.records.splice(index, 1);
  }
  function decorate(target, label, rawOptions) {
    if (!target || !target.getBoundingClientRect || !connected(target)) return false;
    var root = overlayRoot(); if (!root) return false; ensureStyle(root);
    var opts = options(rawOptions), previous = state.activeByTarget && state.activeByTarget.get(target);
    if (previous) removeRecord(previous);
    var container = document.createElement('div'); container.className = opts.className;
    container.setAttribute('data-uitestlens-highlight', '1'); container.setAttribute('data-uitestlens-highlight-state', opts.state);
    container.setAttribute('data-uitestlens-highlight-token', String(++state.sequence));
    container.style.borderColor = opts.color; container.style.borderWidth = opts.borderWidth + 'px'; container.style.pointerEvents = 'none';
    if (opts.showLabel && label) { var badge = document.createElement('div'); badge.className = 'selenium-overlay-highlight-badge'; badge.textContent = String(label); badge.style.background = opts.color; container.appendChild(badge); }
    root.appendChild(container);
    var record = { target: target, container: container, removed: false, timer: null, listener: null };
    record.listener = function () {
      if (!connected(target)) { removeRecord(record); return; }
      try { var rect = target.getBoundingClientRect(); container.style.left = rect.left + 'px'; container.style.top = rect.top + 'px'; container.style.width = rect.width + 'px'; container.style.height = rect.height + 'px'; }
      catch (ignored) { removeRecord(record); }
    };
    state.records.push(record); if (state.activeByTarget) state.activeByTarget.set(target, record);
    window.addEventListener('scroll', record.listener, true); window.addEventListener('resize', record.listener, true);
    record.listener(); record.timer = window.setTimeout(function () { removeRecord(record); }, opts.duration);
    return !record.removed;
  }
  function parent(target, levels, label, rawOptions) {
    var current = target, remaining = Math.max(1, Number(levels) || 1);
    while (remaining-- > 0 && current && current.parentElement) current = current.parentElement;
    return decorate(current, label, Object.assign({}, rawOptions, { className: 'selenium-overlay-highlight-parent' }));
  }
  function element(target, label, rawOptions) { return decorate(target, label, rawOptions); }
  function ancestor(target, levels, label, rawOptions) { return parent(target, levels, label, rawOptions); }
  function closest(target, selector, label, rawOptions) {
    var current = target && selector && target.closest ? target.closest(selector) : null;
    return decorate(current, label, Object.assign({}, rawOptions, { className: 'selenium-overlay-highlight-closest' }));
  }
  function clear() { var records = state.records.slice(); records.forEach(removeRecord); return records.length; }
  lens.modules.highlight = { __uiTestLensHighlight: true, element: element, parent: parent, ancestor: ancestor, closest: closest, clear: clear };
})(window, document);
