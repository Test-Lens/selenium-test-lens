(function (window, document) {
  'use strict';
  window.__uiTestLens = window.__uiTestLens || { version: '1.0-SNAPSHOT', modules: {}, state: {} };
  var lens = window.__uiTestLens;
  lens.modules = lens.modules || {}; lens.state = lens.state || {};
  lens.state.overlay = lens.state.overlay || {}; lens.state.highlight = lens.state.highlight || {};
  if (lens.modules.highlight && lens.modules.highlight.__uiTestLensHighlight === true) return;
  var state = lens.state.highlight;
  var MAX_PENDING_OPERATIONS = 8;
  state.sequence = state.sequence || 0;
  state.records = state.records || [];
  state.lanes = state.lanes || [];
  state.lanesByTarget = state.lanesByTarget || (typeof WeakMap === 'function' ? new WeakMap() : null);

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
    var suppliedOperation = value.operationId != null && String(value.operationId) !== '';
    return { duration: isFinite(Number(value.duration)) ? Math.max(0, Number(value.duration)) : 1500,
      suppress: value.suppress === true,
      color: value.color || '#ffeb3b', className: value.className || 'selenium-overlay-highlight',
      borderWidth: isFinite(Number(value.borderWidth)) ? Math.max(1, Number(value.borderWidth)) : 2,
      showLabel: value.showLabel !== false, state: value.state || 'action',
      sessionId: String(value.sessionId || 'manual'), operationId: suppliedOperation ? String(value.operationId) : 'manual-' + (++state.sequence),
      standalone: value.standalone === true || !suppliedOperation };
  }
  function connected(target) {
    if (!target) return false;
    if (typeof target.isConnected === 'boolean') return target.isConnected;
    var root = target.getRootNode ? target.getRootNode() : document;
    return root === document ? document.documentElement.contains(target) : !!root && !!root.host && connected(root.host);
  }
  function terminal(name) { return name === 'success' || name === 'failure'; }
  function transient(name) { return name === 'action' || name === 'waiting' || name === 'retry'; }
  function laneFor(target) {
    var lane = state.lanesByTarget && state.lanesByTarget.get(target);
    if (lane) return lane;
    lane = { target: target, operations: [], visible: null, completed: [] };
    state.lanes.push(lane); if (state.lanesByTarget) state.lanesByTarget.set(target, lane);
    return lane;
  }
  function existingOperation(operationId) {
    for (var laneIndex = 0; laneIndex < state.lanes.length; laneIndex++) {
      var candidateLane = state.lanes[laneIndex];
      for (var operationIndex = 0; operationIndex < candidateLane.operations.length; operationIndex++) {
        if (candidateLane.operations[operationIndex].id === operationId) {
          return { lane: candidateLane, operation: candidateLane.operations[operationIndex] };
        }
      }
    }
    return null;
  }
  function removeVisual(record) {
    if (!record || record.removed) return;
    record.removed = true;
    if (record.timer != null) window.clearTimeout(record.timer);
    window.removeEventListener('scroll', record.listener, true);
    window.removeEventListener('resize', record.listener, true);
    if (record.container && record.container.parentNode) record.container.parentNode.removeChild(record.container);
    var index = state.records.indexOf(record); if (index >= 0) state.records.splice(index, 1);
  }
  function finishOperation(lane, operation) {
    var index = lane.operations.indexOf(operation); if (index >= 0) lane.operations.splice(index, 1);
    if (operation.terminal) {
      lane.completed.push(operation.id); if (lane.completed.length > 16) lane.completed.shift();
    }
  }
  function showNext(lane) {
    if (lane.visible) return;
    while (lane.operations.length) {
      var operation = lane.operations[0], request = operation.next;
      if (!request) {
        if (lane.operations.length > 1 || operation.standalone || operation.terminal) {
          finishOperation(lane, operation); continue;
        }
        return;
      }
      operation.next = null;
      if (request.suppress) {
        if (operation.standalone || terminal(request.state)) finishOperation(lane, operation);
        continue;
      }
      if (!connected(request.target)) { finishOperation(lane, operation); continue; }
      var root = overlayRoot(); if (!root) return; ensureStyle(root);
      var container = document.createElement('div'); container.className = request.className;
      var revision = ++state.sequence, shownAt = Date.now(), deadline = shownAt + request.duration;
      container.setAttribute('data-uitestlens-highlight', '1');
      container.setAttribute('data-uitestlens-highlight-state', request.state);
      container.setAttribute('data-uitestlens-highlight-session', request.sessionId);
      container.setAttribute('data-uitestlens-highlight-operation', request.operationId);
      container.setAttribute('data-uitestlens-highlight-revision', String(revision));
      container.setAttribute('data-uitestlens-highlight-shown-at', String(shownAt));
      container.setAttribute('data-uitestlens-highlight-deadline', String(deadline));
      container.style.borderColor = request.color; container.style.borderWidth = request.borderWidth + 'px';
      container.style.pointerEvents = 'none';
      if (request.showLabel && request.label) {
        var badge = document.createElement('div'); badge.className = 'selenium-overlay-highlight-badge';
        badge.textContent = String(request.label); badge.style.background = request.color; container.appendChild(badge);
      }
      root.appendChild(container);
      var record = { lane: lane, operation: operation, target: request.target, container: container,
        state: request.state, revision: revision, shownAt: shownAt, deadline: deadline,
        removed: false, timer: null, listener: null };
      record.listener = function () {
        if (!connected(record.target)) { clearLane(lane); return; }
        try {
          var rect = record.target.getBoundingClientRect();
          container.style.left = rect.left + 'px'; container.style.top = rect.top + 'px';
          container.style.width = rect.width + 'px'; container.style.height = rect.height + 'px';
        } catch (ignored) { clearLane(lane); }
      };
      lane.visible = record; state.records.push(record);
      window.addEventListener('scroll', record.listener, true); window.addEventListener('resize', record.listener, true);
      record.listener();
      record.timer = window.setTimeout(function () {
        if (!lane.visible || lane.visible.revision !== revision || lane.visible !== record) return;
        removeVisual(record); lane.visible = null;
        if (!operation.next) finishOperation(lane, operation);
        showNext(lane);
      }, request.duration);
      return;
    }
  }
  function clearLane(lane) {
    if (!lane) return;
    if (lane.visible) removeVisual(lane.visible);
    lane.visible = null; lane.operations.length = 0; lane.completed.length = 0;
    var index = state.lanes.indexOf(lane); if (index >= 0) state.lanes.splice(index, 1);
    if (state.lanesByTarget) state.lanesByTarget.delete(lane.target);
  }
  function accept(target, label, rawOptions) {
    if (!target || !target.getBoundingClientRect || !connected(target)) return false;
    var opts = options(rawOptions), existing = existingOperation(opts.operationId);
    var lane = existing ? existing.lane : laneFor(target);
    if (lane.completed.indexOf(opts.operationId) >= 0) return false;
    var operation = existing ? existing.operation : null;
    if (!operation) {
      if (lane.operations.length >= MAX_PENDING_OPERATIONS + 1) {
        if (window.console && console.debug) console.debug('Test Lens highlight dropped: bounded target backlog');
        return false;
      }
      operation = { id: opts.operationId, sessionId: opts.sessionId, terminal: false,
        standalone: opts.standalone, next: null };
      lane.operations.push(operation);
    }
    if (operation.terminal && !terminal(opts.state)) return false;
    var request = Object.assign({}, opts, { label: label, target: target });
    if (terminal(opts.state)) {
      operation.terminal = true; operation.next = request;
    } else if (transient(opts.state)) {
      if (lane.visible && lane.visible.operation === operation && lane.visible.state === opts.state) return true;
      operation.next = request;
    } else return false;
    showNext(lane);
    return !opts.suppress;
  }
  function decorate(target, label, rawOptions) { return accept(target, label, rawOptions); }
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
  function clear() {
    var count = state.records.length;
    state.lanes.slice().forEach(clearLane);
    state.lanesByTarget = typeof WeakMap === 'function' ? new WeakMap() : null;
    return count;
  }
  lens.modules.highlight = { __uiTestLensHighlight: true, element: element, parent: parent,
    ancestor: ancestor, closest: closest, clear: clear };
})(window, document);
