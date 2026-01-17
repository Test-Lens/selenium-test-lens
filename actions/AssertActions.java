package utils.jsExecHelper.actions;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import utils.jsExecHelper.OverlayConfig;
import utils.jsExecHelper.core.OverlayRootManager;
import utils.jsExecHelper.hud.HudPanel;

import java.util.Objects;
import java.util.function.Function;

public class AssertActions {

    private final JavascriptExecutor js;
    private final OverlayRootManager rootManager;
    private final OverlayConfig config;
    private final HudPanel hudPanel;

    public AssertActions(WebDriver driver,
                         OverlayRootManager rootManager,
                         OverlayConfig config,
                         HudPanel hudPanel) {
        if (!(driver instanceof JavascriptExecutor)) {
            throw new IllegalArgumentException("WebDriver must implement JavascriptExecutor");
        }
        this.js = (JavascriptExecutor) driver;
        this.rootManager = rootManager;
        this.config = config;
        this.hudPanel = hudPanel;
    }

    // ========== PUBLIC ASSERTIONS ==========

    /**
     * Tekst elementu po modyfikacji == expected po modyfikacji.
     * Uwaga: w wyniku i HUD pokaże wartości PO modyfikacji (expected/actual),
     * a overlay badge jest rysowany na oryginalnym elemencie.
     */
    public OverlayAssertionResult assertTextEqualsModified(WebElement element,
                                                           String expected,
                                                           Function<String, String> modifier,
                                                           String contextLabel) {
        if (element == null) {
            return failNullElement("TEXT_EQUALS_MOD", contextLabel, expected);
        }

        String rawActual = element.getText();
        String actualMod = applyModifier(rawActual, modifier);
        String expectedMod = applyModifier(expected, modifier);

        boolean ok = Objects.equals(expectedMod, actualMod);

        OverlayAssertionResult result = buildResult(
                ok,
                "TEXT_EQUALS_MOD",
                contextLabel,
                expectedMod,
                actualMod
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel("TEXT_EQUALS_MOD", ok, contextLabel));
        return result;
    }

    /**
     * Tekst elementu po modyfikacji zawiera expectedSubstring po modyfikacji.
     * Przydatne, jeśli UI ma dodatkowe formatowanie, a chcesz porównywać „po normalizacji”.
     */
    public OverlayAssertionResult assertTextContainsModified(WebElement element,
                                                             String expectedSubstring,
                                                             Function<String, String> modifier,
                                                             String contextLabel) {
        if (element == null) {
            return failNullElement("TEXT_CONTAINS_MOD", contextLabel, expectedSubstring);
        }

        String rawActual = safe(element.getText());
        String actualMod = safe(applyModifier(rawActual, modifier));
        String subMod = safe(applyModifier(expectedSubstring, modifier));

        boolean ok = actualMod.contains(subMod);

        OverlayAssertionResult result = buildResult(
                ok,
                "TEXT_CONTAINS_MOD",
                contextLabel,
                subMod,
                actualMod
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel("TEXT_CONTAINS_MOD", ok, contextLabel));
        return result;
    }

    private static String applyModifier(String s, Function<String, String> modifier) {
        if (modifier == null) return s;
        try {
            return modifier.apply(s);
        } catch (Exception e) {
            // Nie wysadzamy testu wyjątkiem z normalizatora — pokażmy to jako FAIL z czytelnym actual.
            return s;
        }
    }

    /** Tekst elementu (getText) == expected. */
    public OverlayAssertionResult assertTextEquals(WebElement element,
                                                   String expected,
                                                   String contextLabel) {
        if (element == null) {
            return failNullElement("TEXT_EQUALS", contextLabel, expected);
        }

        String actual = element.getText();
        boolean ok = Objects.equals(expected, actual);

        OverlayAssertionResult result = buildResult(
                ok,
                "TEXT_EQUALS",
                contextLabel,
                expected,
                actual
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel("TEXT_EQUALS", ok, contextLabel));
        return result;
    }

    /** Tekst elementu (getText) zawiera expectedSubstring. */
    public OverlayAssertionResult assertTextContains(WebElement element,
                                                     String expectedSubstring,
                                                     String contextLabel) {
        if (element == null) {
            return failNullElement("TEXT_CONTAINS", contextLabel, expectedSubstring);
        }

        String actual = safe(element.getText());
        String sub = safe(expectedSubstring);
        boolean ok = actual.contains(sub);

        OverlayAssertionResult result = buildResult(
                ok,
                "TEXT_CONTAINS",
                contextLabel,
                sub,
                actual
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel("TEXT_CONTAINS", ok, contextLabel));
        return result;
    }

    /** Atrybut HTML, np. "value", "type", "id". */
    public OverlayAssertionResult assertAttributeEquals(WebElement element,
                                                        String attributeName,
                                                        String expected,
                                                        String contextLabel) {
        String type = "ATTR_EQUALS:" + safe(attributeName);

        if (element == null) {
            return failNullElement(type, contextLabel, expected);
        }

        String actual = element.getAttribute(attributeName);
        boolean ok = Objects.equals(expected, actual);

        OverlayAssertionResult result = buildResult(
                ok,
                type,
                contextLabel,
                expected,
                actual
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel(type, ok, contextLabel));
        return result;
    }

    /** Dowolny CSS, np. "font-weight", "display", "background-color". */
    public OverlayAssertionResult assertCssEquals(WebElement element,
                                                  String cssProperty,
                                                  String expected,
                                                  String contextLabel) {
        String type = "CSS_EQUALS:" + safe(cssProperty);

        if (element == null) {
            return failNullElement(type, contextLabel, expected);
        }

        String actual = element.getCssValue(cssProperty);
        boolean ok = Objects.equals(expected, actual);

        OverlayAssertionResult result = buildResult(
                ok,
                type,
                contextLabel,
                expected,
                actual
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel(type, ok, contextLabel));
        return result;
    }

    /**
     * Specjalny helper do kolorów:
     * - pobiera getCssValue(cssProperty),
     * - normalizuje rgb(...) / rgba(...) do #rrggbb,
     * - normalizuje expected do lowercase, usuwając spacje,
     * - porównuje po normalizacji.
     */
    public OverlayAssertionResult assertColorEquals(WebElement element,
                                                    String cssProperty,
                                                    String expectedColor,
                                                    String contextLabel) {
        String type = "COLOR_EQUALS:" + safe(cssProperty);

        if (element == null) {
            return failNullElement(type, contextLabel, expectedColor);
        }

        String actualRaw = element.getCssValue(cssProperty);
        String expectedNorm = normalizeColor(expectedColor);
        String actualNorm = normalizeColor(actualRaw);

        boolean ok = Objects.equals(expectedNorm, actualNorm);

        OverlayAssertionResult result = buildResult(
                ok,
                type,
                contextLabel,
                expectedNorm,
                actualNorm
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel(type, ok, contextLabel));
        return result;
    }

    /** Czy element ma klasę CSS (contains w atrybucie "class"). */
    public OverlayAssertionResult assertHasClass(WebElement element,
                                                 String className,
                                                 boolean expectedPresent,
                                                 String contextLabel) {
        String type = "HAS_CLASS:" + safe(className);

        if (element == null) {
            return failNullElement(type, contextLabel,
                    expectedPresent ? "present" : "absent");
        }

        String classAttr = element.getAttribute("class");
        boolean present = classAttr != null &&
                java.util.Arrays.stream(classAttr.split("\\s+"))
                        .anyMatch(c -> c.equals(className));

        boolean ok = (present == expectedPresent);

        OverlayAssertionResult result = buildResult(
                ok,
                type,
                contextLabel,
                String.valueOf(expectedPresent),
                String.valueOf(present)
        );

        hudUpdate(result.getMessage());
        drawOverlayBadge(element, ok, buildBadgeLabel(type, ok, contextLabel));
        return result;
    }

    /** Widoczność (isDisplayed). */
    public OverlayAssertionResult assertVisible(WebElement element,
                                                boolean expectedVisible,
                                                String contextLabel) {
        String type = "VISIBLE";

        boolean actual = element != null && element.isDisplayed();
        boolean ok = (actual == expectedVisible);

        OverlayAssertionResult result = buildResult(
                ok,
                type,
                contextLabel,
                String.valueOf(expectedVisible),
                String.valueOf(actual)
        );

        hudUpdate(result.getMessage());
        if (element != null) {
            drawOverlayBadge(element, ok, buildBadgeLabel(type, ok, contextLabel));
        }
        return result;
    }

    /** Enabled (isEnabled). */
    public OverlayAssertionResult assertEnabled(WebElement element,
                                                boolean expectedEnabled,
                                                String contextLabel) {
        String type = "ENABLED";

        boolean actual = element != null && element.isEnabled();
        boolean ok = (actual == expectedEnabled);

        OverlayAssertionResult result = buildResult(
                ok,
                type,
                contextLabel,
                String.valueOf(expectedEnabled),
                String.valueOf(actual)
        );

        hudUpdate(result.getMessage());
        if (element != null) {
            drawOverlayBadge(element, ok, buildBadgeLabel(type, ok, contextLabel));
        }
        return result;
    }

    /** Selected (np. checkbox, radio). */
    public OverlayAssertionResult assertSelected(WebElement element,
                                                 boolean expectedSelected,
                                                 String contextLabel) {
        String type = "SELECTED";

        boolean actual = element != null && element.isSelected();
        boolean ok = (actual == expectedSelected);

        OverlayAssertionResult result = buildResult(
                ok,
                type,
                contextLabel,
                String.valueOf(expectedSelected),
                String.valueOf(actual)
        );

        hudUpdate(result.getMessage());
        if (element != null) {
            drawOverlayBadge(element, ok, buildBadgeLabel(type, ok, contextLabel));
        }
        return result;
    }

    // ========== GENERIC / VALUE-BASED ASSERTIONS (bez WebElement) ==========

    public OverlayAssertionResult assertEquals(Object expected,
                                               Object actual,
                                               String contextLabel) {
        String exp = safe(String.valueOf(expected));
        String act = safe(String.valueOf(actual));
        boolean ok = Objects.equals(exp, act);

        OverlayAssertionResult result = buildResult(
                ok,
                "GENERIC_EQUALS",
                contextLabel,
                exp,
                act
        );

        hudUpdate(result.getMessage());
        return result;
    }

    public OverlayAssertionResult assertNotEquals(Object expected,
                                                  Object actual,
                                                  String contextLabel) {
        String exp = safe(String.valueOf(expected));
        String act = safe(String.valueOf(actual));
        boolean ok = !Objects.equals(exp, act);

        OverlayAssertionResult result = buildResult(
                ok,
                "GENERIC_NOT_EQUALS",
                contextLabel,
                exp,
                act
        );

        hudUpdate(result.getMessage());
        return result;
    }

    public OverlayAssertionResult assertContains(String actual,
                                                 String expectedSubstring,
                                                 String contextLabel) {
        String act = safe(actual);
        String sub = safe(expectedSubstring);
        boolean ok = act.contains(sub);

        OverlayAssertionResult result = buildResult(
                ok,
                "GENERIC_CONTAINS",
                contextLabel,
                sub,
                act
        );

        hudUpdate(result.getMessage());
        return result;
    }

    public OverlayAssertionResult assertNotContains(String actual,
                                                    String expectedSubstring,
                                                    String contextLabel) {
        String act = safe(actual);
        String sub = safe(expectedSubstring);
        boolean ok = !act.contains(sub);

        OverlayAssertionResult result = buildResult(
                ok,
                "GENERIC_NOT_CONTAINS",
                contextLabel,
                sub,
                act
        );

        hudUpdate(result.getMessage());
        return result;
    }

    public OverlayAssertionResult assertTrue(boolean condition,
                                             String contextLabel) {
        boolean ok = condition;

        OverlayAssertionResult result = buildResult(
                ok,
                "GENERIC_TRUE",
                contextLabel,
                "true",
                String.valueOf(condition)
        );

        hudUpdate(result.getMessage());
        return result;
    }

    public OverlayAssertionResult assertFalse(boolean condition,
                                              String contextLabel) {
        boolean ok = !condition;

        OverlayAssertionResult result = buildResult(
                ok,
                "GENERIC_FALSE",
                contextLabel,
                "false",
                String.valueOf(condition)
        );

        hudUpdate(result.getMessage());
        return result;
    }

    // ========== INTERNAL HELPERS ==========

    private OverlayAssertionResult failNullElement(String type,
                                                   String context,
                                                   String expected) {
        String msg = "[ASSERT FAIL] " + safe(context) +
                " | element == null, expected='" + safe(expected) + "'";
        hudUpdate(msg);
        return new OverlayAssertionResult(
                false,
                type,
                safe(context),
                safe(expected),
                null,
                msg,
                System.currentTimeMillis()
        );
    }

    private OverlayAssertionResult buildResult(boolean ok,
                                               String type,
                                               String context,
                                               String expected,
                                               String actual) {
        String msg = String.format(
                "[ASSERT %s] %s | expected='%s', actual='%s'",
                ok ? "OK" : "FAIL",
                safe(context),
                safe(expected),
                safe(actual)
        );
        return new OverlayAssertionResult(
                ok,
                type,
                safe(context),
                safe(expected),
                safe(actual),
                msg,
                System.currentTimeMillis()
        );
    }

    private void drawOverlayBadge(WebElement element, boolean ok) {
        drawOverlayBadge(element, ok, null);
    }

    /**
     * Stackowalne badge'y:
     * - jeden container per element (target.__seleniumAssertContainer),
     * - kolejne badge'e wsuwane coraz wyżej (top: -18px, -36px, -54px ...),
     * - jeśli JAKAKOLWIEK asercja na elemencie jest FAIL → ramka i tło badge'y czerwone.
     */
    private void drawOverlayBadge(WebElement element, boolean ok, String label) {
        if (!config.isEnabled() || element == null) return;

        rootManager.ensureRootExists();
        long duration = config.getDecorationDurationMs();

        js.executeScript(
                "var target = arguments[0];" +
                        "var ok = arguments[1];" +
                        "var duration = arguments[2];" +
                        "var label = arguments[3];" +
                        "if (!target || !target.getBoundingClientRect) { return; }" +
                        "var shadow = window.__seleniumOverlayRoot;" +
                        "if (!shadow) { return; }" +

                        // wspólny kontener dla wszystkich asercji na tym elemencie
                        "var container = target.__seleniumAssertContainer;" +
                        "if (!container) {" +
                        "  container = document.createElement('div');" +
                        "  container.className = 'selenium-overlay-assert';" +
                        "  container.style.position = 'fixed';" +
                        "  container.style.pointerEvents = 'none';" +
                        "  container.style.zIndex = '2147483647';" +
                        "  container.style.boxSizing = 'border-box';" +
                        "  container.style.borderRadius = '4px';" +
                        "  target.__seleniumAssertContainer = container;" +
                        "  shadow.appendChild(container);" +
                        "}" +

                        // kolor bazowy / błędu
                        "var baseColor = '#4caf50';" +
                        "var failColor = '#f44336';" +
                        "if (!ok) {" +
                        "  container.dataset.hasFail = 'true';" +
                        "}" +
                        "var color = (container.dataset.hasFail === 'true') ? failColor : baseColor;" +
                        "container.style.border = '2px solid ' + color;" +

                        // tworzymy badge
                        "var badge = document.createElement('div');" +
                        "badge.className = 'selenium-assert-badge';" +
                        "badge.textContent = label || (ok ? 'ASSERT OK' : 'ASSERT FAIL');" +
                        "badge.style.position = 'absolute';" +
                        "badge.style.left = '0';" +
                        "badge.style.padding = '2px 6px';" +
                        "badge.style.fontSize = '10px';" +
                        "badge.style.background = color;" +
                        "badge.style.color = '#fff';" +
                        "badge.style.borderRadius = '3px';" +
                        "badge.style.whiteSpace = 'nowrap';" +

                        // offset w pionie – każdy badge osobny „wiersz”
                        "var existingBadges = container.querySelectorAll('.selenium-assert-badge');" +
                        "var count = existingBadges.length;" +
                        "var offset = (count + 1) * 18;" +
                        "badge.style.top = '-' + offset + 'px';" +

                        "container.appendChild(badge);" +

                        // pozycjonowanie kontenera nad elementem
                        "function update() {" +
                        "  if (!document.body.contains(target)) { return; }" +
                        "  var rect = target.getBoundingClientRect();" +
                        "  container.style.left = rect.left + 'px';" +
                        "  container.style.top = rect.top + 'px';" +
                        "  container.style.width = rect.width + 'px';" +
                        "  container.style.height = rect.height + 'px';" +
                        "}" +

                        "function onScroll() { update(); }" +

                        "function cleanup() {" +
                        "  window.removeEventListener('scroll', onScroll, true);" +
                        "  window.removeEventListener('resize', onScroll, true);" +
                        "  if (container && container.parentNode) {" +
                        "    container.parentNode.removeChild(container);" +
                        "    if (target) {" +
                        "      try { delete target.__seleniumAssertContainer; } catch(e) {}" +
                        "    }" +
                        "  }" +
                        "}" +

                        "window.addEventListener('scroll', onScroll, true);" +
                        "window.addEventListener('resize', onScroll, true);" +
                        "update();" +
                        "setTimeout(cleanup, duration);",
                element, ok, duration, label
        );
    }

    private void hudUpdate(String msg) {
        if (hudPanel != null && config.isShowHudPanel()) {
            hudPanel.updateStep(msg);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String normalizeColor(String color) {
        if (color == null) return "";
        String c = color.trim().toLowerCase();

        // hex: #rrggbb
        if (c.matches("#[0-9a-f]{6}")) {
            return c;
        }

        // rgb(...) lub rgba(...)
        if (c.startsWith("rgb")) {
            int left = c.indexOf('(');
            int right = c.indexOf(')');
            if (left > 0 && right > left) {
                String inside = c.substring(left + 1, right);
                String[] parts = inside.split(",");
                if (parts.length >= 3) {
                    try {
                        int r = Integer.parseInt(parts[0].trim());
                        int g = Integer.parseInt(parts[1].trim());
                        int b = Integer.parseInt(parts[2].trim());
                        return String.format("#%02x%02x%02x", r, g, b);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // fallback – zwróć jak jest (np. "red")
        return c.replace(" ", "");
    }

    private String buildBadgeLabel(String type, boolean ok, String contextLabel) {
        String status = ok ? "OK" : "FAIL";
        String ctx = safe(contextLabel);
        if (ctx.isEmpty()) {
            return "[" + status + "] " + type;
        }
        return "[" + status + "] " + type + " - " + ctx;
    }

    // ========== RESULT ==========

    public static final class OverlayAssertionResult {
        private final boolean success;
        private final String assertionType;
        private final String context;
        private final String expected;
        private final String actual;
        private final String message;
        private final long timestampMillis;

        public OverlayAssertionResult(boolean success,
                                      String assertionType,
                                      String context,
                                      String expected,
                                      String actual,
                                      String message,
                                      long timestampMillis) {
            this.success = success;
            this.assertionType = assertionType;
            this.context = context;
            this.expected = expected;
            this.actual = actual;
            this.message = message;
            this.timestampMillis = timestampMillis;
        }

        public boolean isSuccess() { return success; }
        public String getAssertionType() { return assertionType; }
        public String getContext() { return context; }
        public String getExpected() { return expected; }
        public String getActual() { return actual; }
        public String getMessage() { return message; }
        public long getTimestampMillis() { return timestampMillis; }

        public String toMessage() {
            return message;
        }

        public String toJson() {
            return new StringBuilder()
                    .append("{")
                    .append("\"success\":").append(success).append(',')
                    .append("\"assertionType\":\"").append(escape(assertionType)).append("\",")
                    .append("\"context\":\"").append(escape(context)).append("\",")
                    .append("\"expected\":\"").append(escape(expected)).append("\",")
                    .append("\"actual\":").append(actual == null ? "null" :
                            "\"" + escape(actual) + "\"").append(",")
                    .append("\"message\":\"").append(escape(message)).append("\",")
                    .append("\"timestampMillis\":").append(timestampMillis)
                    .append("}")
                    .toString();
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
