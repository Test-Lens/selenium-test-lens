package io.github.testlens.react.actionability;

final class ReactReadinessScripts {
    static final String READINESS = """
            var el = arguments[0];
            function visible(node) {
              if (!node) return false;
              var style = window.getComputedStyle(node);
              var rect = node.getBoundingClientRect();
              return style.display !== 'none' && style.visibility !== 'hidden' &&
                style.opacity !== '0' && rect.width > 0 && rect.height > 0;
            }
            function describe(node) {
              if (!node) return '';
              var id = node.id ? '#' + node.id : '';
              var cls = '';
              if (node.className && typeof node.className === 'string') {
                cls = '.' + node.className.trim().split(/\\s+/).slice(0, 3).join('.');
              }
              var role = node.getAttribute ? node.getAttribute('role') : '';
              var label = node.getAttribute ? node.getAttribute('aria-label') : '';
              return String(node.tagName || '').toLowerCase() + id + cls +
                (role ? '[role=' + role + ']' : '') +
                (label ? '[aria-label=' + label.substring(0, 60) + ']' : '');
            }
            function attrOnSelfOrAncestor(name, values) {
              var node = el;
              while (node && node.nodeType === 1) {
                var value = node.getAttribute(name);
                if (value !== null) {
                  var normalized = String(value).toLowerCase();
                  if (!values || values.indexOf(normalized) >= 0) {
                    return {active: true, value: value, element: describe(node)};
                  }
                }
                node = node.parentElement;
              }
              return {active: false};
            }
            function firstVisible(selector) {
              var nodes = document.querySelectorAll(selector);
              for (var i = 0; i < nodes.length; i++) {
                if (visible(nodes[i])) return describe(nodes[i]);
              }
              return '';
            }
            var dataLoading = attrOnSelfOrAncestor('data-loading', ['true', 'loading', 'pending', 'busy', '']);
            var dataPending = attrOnSelfOrAncestor('data-pending', ['true', 'pending', 'loading', 'busy', '']);
            var dataState = attrOnSelfOrAncestor('data-state', ['loading', 'pending', 'busy', 'disabled']);
            var focusLock = firstVisible('[data-focus-lock], [data-focus-lock-disabled="false"], .focus-lock, [aria-hidden="false"][data-lock]');
            var dialog = firstVisible('[role="dialog"], [role="alertdialog"], [aria-modal="true"], dialog[open], .modal, [data-testid*="modal"]');
            return {
              ariaDisabled: attrOnSelfOrAncestor('aria-disabled', ['true']),
              ariaBusy: attrOnSelfOrAncestor('aria-busy', ['true']),
              dataLoading: dataLoading.active ? dataLoading : dataState,
              dataPending: dataPending,
              progressbar: firstVisible('[role="progressbar"], progress'),
              spinner: firstVisible('[class*="spinner"], [class*="loading"], [data-testid*="spinner"], [data-testid*="loading"]'),
              skeleton: firstVisible('[class*="skeleton"], [data-testid*="skeleton"], [aria-label*="loading"]'),
              focusLock: focusLock,
              dialogOrModal: dialog
            };
            """;

    private ReactReadinessScripts() {
    }
}

