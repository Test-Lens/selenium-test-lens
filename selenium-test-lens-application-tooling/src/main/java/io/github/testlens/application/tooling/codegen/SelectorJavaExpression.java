package io.github.testlens.application.tooling.codegen;

import io.github.testlens.application.model.ApplicationModel;

import java.util.Locale;
import java.util.Objects;

/** Deterministic Java projection for a selector already chosen by Selector Intelligence. @since 0.5.0 */
public final class SelectorJavaExpression {
    private SelectorJavaExpression() { }

    public static String byExpression(ApplicationModel.SelectorProjection selector) {
        Objects.requireNonNull(selector, "selector");
        return byExpression(selector.strategy(), selector.value());
    }

    public static String byExpression(String strategy, String value) {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(value, "value");
        String escaped = javaString(value);
        return switch (strategy.trim().toLowerCase(Locale.ROOT)) {
            case "css", "cssselector", "css selector" -> "By.cssSelector(\"" + escaped + "\")";
            case "id" -> "By.id(\"" + escaped + "\")";
            case "name" -> "By.name(\"" + escaped + "\")";
            case "xpath" -> "By.xpath(\"" + escaped + "\")";
            case "tag", "tagname", "tag name" -> "By.tagName(\"" + escaped + "\")";
            case "linktext", "link text" -> "By.linkText(\"" + escaped + "\")";
            case "partiallinktext", "partial link text" -> "By.partialLinkText(\"" + escaped + "\")";
            default -> null;
        };
    }

    private static String javaString(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            switch (character) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (character < 0x20 || character == 0x7f) out.append(String.format("\\u%04x", (int) character));
                    else out.append(character);
                }
            }
        }
        return out.toString();
    }
}
