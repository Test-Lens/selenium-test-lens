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
  lens.state.api = lens.state.api || {};
  lens.state.overlay = lens.state.overlay || {};

  if (lens.modules.apiOverlay && lens.modules.apiOverlay.__uiTestLensApiOverlay === true) {
    lens.modules.apiModal = lens.modules.apiOverlay;
    window.__seleniumApiModal = lens.modules.apiOverlay;
    return;
  }

  var state = lens.state.api;
  state.requests = state.requests || {};
  state.sequence = state.sequence || 0;
  state.activeRequestId = state.activeRequestId || null;
  state.autoCloseOkMs = state.autoCloseOkMs || 0;
  state.autoCloseErrMs = state.autoCloseErrMs || 0;
  state.delayAutoCloseUntilSearch = !!state.delayAutoCloseUntilSearch;

  function rootNode() {
    var root = lens.state.overlay.root || window.__seleniumOverlayRoot;
    if (root) {
      lens.state.overlay.root = root;
      window.__seleniumOverlayRoot = root;
      return root;
    }
    return document.body || document.documentElement;
  }

  function asText(value) {
    return value == null ? '' : String(value);
  }

  function safeNumber(value, fallback) {
    var number = Number(value);
    return Number.isFinite(number) ? number : fallback;
  }

  function ensureStyle(root) {
    if (!root || !root.querySelector || root.querySelector('#selenium-api-modal-style')) {
      return;
    }

    var style = document.createElement('style');
    style.id = 'selenium-api-modal-style';
    style.textContent = ''
      + '#selenium-api-modal{position:fixed;right:18px;bottom:18px;z-index:2147483647;'
      + 'width:min(760px,calc(100vw - 36px));max-height:min(720px,calc(100vh - 36px));'
      + 'overflow:auto;background:#101317;color:#f3f4f6;border:1px solid #374151;'
      + 'box-shadow:0 18px 48px rgba(0,0,0,.35);font:12px/1.45 Arial,sans-serif;'
      + 'border-radius:8px;padding:12px;}'
      + '#selenium-api-modal[data-filtered="true"]{border-color:#38bdf8;}'
      + '#selenium-api-modal .utl-api-head{display:flex;justify-content:space-between;gap:12px;'
      + 'font-weight:700;margin-bottom:10px;}'
      + '#selenium-api-modal .utl-api-status{color:#93c5fd;}'
      + '#selenium-api-modal .utl-api-grid{display:grid;grid-template-columns:110px minmax(0,1fr);gap:6px 10px;}'
      + '#selenium-api-modal .utl-api-label{color:#9ca3af;}'
      + '#selenium-api-modal pre{white-space:pre-wrap;word-break:break-word;background:#171b22;'
      + 'border:1px solid #2f3642;border-radius:6px;padding:8px;margin:4px 0 0;}';
    root.appendChild(style);
  }

  function ensureModal() {
    var root = rootNode();
    ensureStyle(root);

    var modal = root && root.querySelector ? root.querySelector('#selenium-api-modal') : null;
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'selenium-api-modal';
      modal.style.display = 'none';
      if (root && root.appendChild) {
        root.appendChild(modal);
      }
    }
    return modal;
  }

  function field(label, value) {
    var labelNode = document.createElement('div');
    labelNode.className = 'utl-api-label';
    labelNode.textContent = label;

    var valueNode = document.createElement('div');
    valueNode.textContent = asText(value);

    return [labelNode, valueNode];
  }

  function preField(label, value) {
    var labelNode = document.createElement('div');
    labelNode.className = 'utl-api-label';
    labelNode.textContent = label;

    var valueNode = document.createElement('pre');
    valueNode.textContent = asText(value);

    return [labelNode, valueNode];
  }

  function appendPair(parent, pair) {
    parent.appendChild(pair[0]);
    parent.appendChild(pair[1]);
  }

  function render(request) {
    var modal = ensureModal();
    if (!modal || !request) {
      return;
    }

    modal.style.display = 'block';
    modal.dataset.requestId = request.id;
    modal.dataset.filtered = state.filterPaths && state.filterPaths.length ? 'true' : 'false';

    while (modal.firstChild) {
      modal.removeChild(modal.firstChild);
    }

    var head = document.createElement('div');
    head.className = 'utl-api-head';

    var title = document.createElement('div');
    title.textContent = request.title || 'API call';

    var status = document.createElement('div');
    status.className = 'utl-api-status';
    status.textContent = request.statusText || 'request';

    head.appendChild(title);
    head.appendChild(status);
    modal.appendChild(head);

    var grid = document.createElement('div');
    grid.className = 'utl-api-grid';
    appendPair(grid, field('Method', request.method));
    appendPair(grid, field('URL', request.url));

    if (request.durationMs != null) {
      appendPair(grid, field('Duration', request.durationMs + ' ms'));
    }
    if (request.status != null) {
      appendPair(grid, field('HTTP status', request.status));
    }
    if (request.payloadPreview) {
      appendPair(grid, preField('Payload', request.payloadPreview));
    }
    if (request.headersPreview) {
      appendPair(grid, preField('Headers', request.headersPreview));
    }
    if (request.bodyPreview) {
      appendPair(grid, preField('Body', request.bodyPreview));
    }
    if (request.errorMessage) {
      appendPair(grid, preField('Error', request.errorMessage));
    }
    if (request.errorDetails) {
      appendPair(grid, preField('Details', request.errorDetails));
    }
    if (state.highlightedPath) {
      appendPair(grid, field('Highlighted', state.highlightedPath));
    }
    if (state.filterPaths && state.filterPaths.length) {
      appendPair(grid, preField('Filter', state.filterPaths.join('\n')));
    }

    modal.appendChild(grid);
  }

  function activeRequest() {
    return state.activeRequestId ? state.requests[state.activeRequestId] : null;
  }

  function tryParseJson(value) {
    if (!value) {
      return null;
    }
    try {
      return JSON.parse(value);
    } catch (ignored) {
      return null;
    }
  }

  function activeJson() {
    var request = activeRequest();
    return tryParseJson(request && request.bodyPreview);
  }

  function collectKeyPaths(value, key, prefix, result) {
    if (value == null || typeof value !== 'object') {
      return;
    }

    if (Array.isArray(value)) {
      for (var i = 0; i < value.length; i++) {
        collectKeyPaths(value[i], key, prefix + '[' + i + ']', result);
      }
      return;
    }

    Object.keys(value).forEach(function (name) {
      var path = prefix ? prefix + '.' + name : name;
      if (name === key) {
        result.push(path);
      }
      collectKeyPaths(value[name], key, path, result);
    });
  }

  function highlightPath(path) {
    if (!path) {
      return false;
    }

    state.highlightedPath = asText(path);
    var request = activeRequest();
    if (request) {
      render(request);
    }
    return true;
  }

  function highlightPathAnimated(path, stepDelayMs) {
    window.setTimeout(function () {
      highlightPath(path);
    }, Math.max(0, safeNumber(stepDelayMs, 0)));
    return !!path;
  }

  function highlightPathsAnimated(paths, stepDelayMs, betweenPathsMs) {
    var list = Array.isArray(paths) ? paths : [];
    var step = Math.max(0, safeNumber(stepDelayMs, 0));
    var between = Math.max(0, safeNumber(betweenPathsMs, 0));

    list.forEach(function (path, index) {
      window.setTimeout(function () {
        highlightPath(path);
      }, index * (step + between));
    });
    return list.length;
  }

  function highlightManyPaths(paths, delayMs) {
    return highlightPathsAnimated(paths, delayMs, 0);
  }

  function highlightPathsAnimatedAsync(paths, stepDelayMs, betweenPathsMs, done) {
    var count = highlightPathsAnimated(paths, stepDelayMs, betweenPathsMs);
    var delay = count * (Math.max(0, safeNumber(stepDelayMs, 0)) + Math.max(0, safeNumber(betweenPathsMs, 0)));
    window.setTimeout(function () {
      if (typeof done === 'function') {
        done(count);
      }
    }, delay);
  }

  function highlightPathsCandyAnimatedAsync(paths, stepDelayMs, betweenPathsMs, keepColorMs, focusFadeMs, done) {
    var count = highlightPathsAnimated(paths, stepDelayMs, betweenPathsMs);
    var delay = count * (Math.max(0, safeNumber(stepDelayMs, 0)) + Math.max(0, safeNumber(betweenPathsMs, 0)))
      + Math.max(0, safeNumber(keepColorMs, 0))
      + Math.max(0, safeNumber(focusFadeMs, 0));
    window.setTimeout(function () {
      if (typeof done === 'function') {
        done(count);
      }
    }, delay);
  }

  function highlightKeyAnimated(key, delayMs, maxHits) {
    var paths = findPathsByKey(key);
    var limit = safeNumber(maxHits, 0);
    if (limit > 0) {
      paths = paths.slice(0, limit);
    }

    paths.forEach(function (path, index) {
      window.setTimeout(function () {
        highlightPath(path);
      }, index * Math.max(0, safeNumber(delayMs, 0)));
    });
    return paths.length;
  }

  function findPathsByKey(key) {
    var json = activeJson();
    var result = [];
    if (!key || json == null) {
      return result;
    }
    collectKeyPaths(json, asText(key), '', result);
    return result;
  }

  function showRequest(title, method, url, payloadPreview) {
    var id = 'api-' + Date.now() + '-' + (++state.sequence);
    var request = {
      id: id,
      title: asText(title),
      method: asText(method),
      url: asText(url),
      payloadPreview: asText(payloadPreview),
      statusText: 'request'
    };
    state.requests[id] = request;
    state.activeRequestId = id;
    render(request);
    return id;
  }

  function setPending(requestId, timeoutMs) {
    var request = state.requests[requestId] || activeRequest();
    if (!request) {
      return false;
    }
    request.timeoutMs = safeNumber(timeoutMs, 0);
    request.statusText = 'pending';
    state.activeRequestId = request.id;
    render(request);
    return true;
  }

  function setResponse(requestId, status, durationMs, headersPreview, bodyPreview) {
    var request = state.requests[requestId] || activeRequest();
    if (!request) {
      return false;
    }
    request.status = safeNumber(status, status);
    request.durationMs = safeNumber(durationMs, durationMs);
    request.headersPreview = asText(headersPreview);
    request.bodyPreview = asText(bodyPreview);
    request.statusText = 'response';
    state.activeRequestId = request.id;
    render(request);
    return true;
  }

  function setError(requestId, message, details) {
    var request = state.requests[requestId] || activeRequest();
    if (!request) {
      return false;
    }
    request.errorMessage = asText(message);
    request.errorDetails = asText(details);
    request.statusText = 'error';
    state.activeRequestId = request.id;
    render(request);
    return true;
  }

  function hide() {
    var modal = ensureModal();
    modal.style.display = 'none';
    return true;
  }

  function resetFocus() {
    state.highlightedPath = null;
    var request = activeRequest();
    if (request) {
      render(request);
    }
    return true;
  }

  function filterToPaths(jsonPaths, keepParents) {
    state.filterPaths = Array.isArray(jsonPaths) ? jsonPaths.map(asText) : [];
    state.keepFilterParents = !!keepParents;
    var request = activeRequest();
    if (request) {
      render(request);
    }
    return true;
  }

  function clearFilter() {
    state.filterPaths = [];
    state.keepFilterParents = false;
    var request = activeRequest();
    if (request) {
      render(request);
    }
    return true;
  }

  function setAutoCloseMs(okMs, errMs) {
    state.autoCloseOkMs = Math.max(0, safeNumber(okMs, 0));
    state.autoCloseErrMs = Math.max(0, safeNumber(errMs, 0));
    return true;
  }

  function setDelayAutoCloseUntilSearch(on) {
    state.delayAutoCloseUntilSearch = !!on;
    return true;
  }

  var api = {
    __uiTestLensApiOverlay: true,
    showRequest: showRequest,
    setPending: setPending,
    setResponse: setResponse,
    setError: setError,
    hide: hide,
    highlightPath: highlightPath,
    highlightKeyAnimated: highlightKeyAnimated,
    highlightPathAnimated: highlightPathAnimated,
    highlightManyPaths: highlightManyPaths,
    highlightPathsAnimated: highlightPathsAnimated,
    highlightPathsAnimatedAsync: highlightPathsAnimatedAsync,
    highlightPathsCandyAnimatedAsync: highlightPathsCandyAnimatedAsync,
    findPathsByKey: findPathsByKey,
    resetFocus: resetFocus,
    filterToPaths: filterToPaths,
    clearFilter: clearFilter,
    setAutoCloseMs: setAutoCloseMs,
    setDelayAutoCloseUntilSearch: setDelayAutoCloseUntilSearch
  };

  lens.modules.apiOverlay = api;
  lens.modules.apiModal = api;
  window.__seleniumApiModal = api;
})(window, document);
