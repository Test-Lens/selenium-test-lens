package io.github.testlens;

import io.github.testlens.selenium.locator.UiLocatorSelectors;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Internal lazy candidate filter backed exclusively by typed WebElement accessibility APIs. */
final class SemanticBy extends By {
    private final By candidates;
    private final Matcher matcher;
    private final String description;

    private SemanticBy(By candidates, Matcher matcher, String description) {
        this.candidates = Objects.requireNonNull(candidates, "candidates must not be null");
        this.matcher = Objects.requireNonNull(matcher, "matcher must not be null");
        this.description = description;
    }

    static By role(String role, String accessibleName) {
        String expectedRole = normalizeRole(requireNonBlank(role, "role"));
        String literal = UiLocatorSelectors.xpathLiteral(expectedRole);
        String explicit = "contains(concat(' ', normalize-space(@role), ' '), concat(' ', " + literal + ", ' '))";
        String predicate = explicit + " or " + implicitRolePredicate(expectedRole);
        By candidates = By.xpath("//*[" + predicate + "]");
        String expectedName = accessibleName == null || accessibleName.isBlank()
                ? null
                : normalizeText(accessibleName);
        return new SemanticBy(candidates, element -> {
            if (!expectedRole.equals(normalizeRole(element.getAriaRole()))) return false;
            return expectedName == null || expectedName.equals(normalizeText(element.getAccessibleName()));
        }, expectedName == null
                ? "By.semanticRole: " + preview(expectedRole)
                : "By.semanticRole: " + preview(expectedRole) + ", name=" + preview(expectedName));
    }

    static By label(String label) {
        String expected = normalizeText(requireNonNull(label, "label"));
        String nativeLabelable = "self::button or self::input or self::meter or self::output or "
                + "self::progress or self::select or self::textarea";
        String hasNativeLabel = "((" + nativeLabelable + ") and "
                + "((@id and @id = //label[@for]/@for) or ancestor::label))";
        By candidates = By.xpath("//*[(" + hasNativeLabel + ") or @aria-label or @aria-labelledby]");
        return new SemanticBy(candidates,
                element -> expected.equals(normalizeText(element.getAccessibleName())),
                "By.semanticLabel: " + preview(expected));
    }

    static By altText(String altText) {
        String expected = normalizeText(requireNonNull(altText, "altText"));
        By candidates = By.xpath("//img[@alt] | //area[@alt] | "
                + "//input[translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='image'][@alt]");
        return new SemanticBy(candidates,
                element -> expected.equals(normalizeText(element.getDomAttribute("alt"))),
                "By.semanticAltText: " + preview(expected));
    }

    @Override
    public List<WebElement> findElements(SearchContext context) {
        List<WebElement> matched = new ArrayList<>();
        for (WebElement candidate : context.findElements(candidates)) {
            if (matcher.matches(candidate)) matched.add(candidate);
        }
        return List.copyOf(matched);
    }

    @Override
    public String toString() {
        return description;
    }

    static String normalizeText(String value) {
        if (value == null) return "";
        StringBuilder normalized = new StringBuilder();
        boolean pendingSpace = false;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                if (normalized.length() > 0) pendingSpace = true;
            } else {
                if (pendingSpace) normalized.append(' ');
                normalized.appendCodePoint(codePoint);
                pendingSpace = false;
            }
        }
        return normalized.toString();
    }

    private static String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }

    private static String preview(String value) {
        String safe = normalizeText(value);
        return safe.length() <= 80 ? safe : safe.substring(0, 77) + "...";
    }

    private static String requireNonNull(String value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " must not be null");
        return value;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    private static String implicitRolePredicate(String role) {
        return switch (role) {
            case "button" -> "self::button or (self::input and (" + typeEquals("button") + " or "
                    + typeEquals("submit") + " or " + typeEquals("reset") + " or " + typeEquals("image") + "))";
            case "link" -> "self::a[@href]";
            case "textbox" -> "self::textarea or (self::input and (not(@type) or " + typeEquals("text")
                    + " or " + typeEquals("email") + " or " + typeEquals("search") + " or "
                    + typeEquals("password") + " or " + typeEquals("tel") + " or " + typeEquals("url") + "))";
            case "checkbox" -> "self::input[" + typeEquals("checkbox") + "]";
            case "radio" -> "self::input[" + typeEquals("radio") + "]";
            default -> "false()";
        };
    }

    private static String typeEquals(String value) {
        return "translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='" + value + "'";
    }

    @FunctionalInterface
    private interface Matcher {
        boolean matches(WebElement element);
    }
}
