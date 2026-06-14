package io.github.mmaciekk111.uitestlens.selenium.actionability;

final class ActionabilityScripts {
    static final String IS_ATTACHED = "return !!(arguments[0] && arguments[0].isConnected === true);";

    static final String SCROLL_INTO_VIEW = """
            arguments[0].scrollIntoView({block:'center', inline:'center'});
            return true;
            """;

    static final String BOUNDING_RECT = """
            var el = arguments[0];
            var rect = el.getBoundingClientRect();
            var vw = window.innerWidth || document.documentElement.clientWidth;
            var vh = window.innerHeight || document.documentElement.clientHeight;
            return {
              x: rect.x,
              y: rect.y,
              width: rect.width,
              height: rect.height,
              left: rect.left,
              top: rect.top,
              right: rect.right,
              bottom: rect.bottom,
              inViewport: rect.width > 0 && rect.height > 0 &&
                rect.bottom >= 0 && rect.right >= 0 && rect.top <= vh && rect.left <= vw
            };
            """;

    static final String CLICK_POINT = """
            var el = arguments[0];
            var rect = el.getBoundingClientRect();
            var x = rect.left + rect.width / 2;
            var y = rect.top + rect.height / 2;
            var top = document.elementFromPoint(x, y);
            function describe(node) {
              if (!node) return '';
              var id = node.id ? '#' + node.id : '';
              var cls = '';
              if (node.className && typeof node.className === 'string') {
                cls = '.' + node.className.trim().split(/\\s+/).slice(0, 3).join('.');
              }
              return String(node.tagName || '').toLowerCase() + id + cls;
            }
            var receives = !!top && (top === el || el.contains(top) || top.contains(el));
            return {
              receives: receives,
              x: x,
              y: y,
              topElement: describe(top)
            };
            """;

    private ActionabilityScripts() {
    }
}
