package io.github.testlens.selenium.locator;

public final class UiLocatorSelectors {
    private UiLocatorSelectors() {}

    public static String xpathLiteral(String value) {
        String input = value == null ? "" : value;
        if (!input.contains("'")) {
            return "'" + input + "'";
        }
        if (!input.contains("\"")) {
            return "\"" + input + "\"";
        }
        StringBuilder builder = new StringBuilder("concat(");
        for (int i = 0; i < input.length(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            char character = input.charAt(i);
            if (character == '\'') {
                builder.append("\"'\"");
            } else if (character == '"') {
                builder.append("'\"'");
            } else {
                builder.append("'").append(character).append("'");
            }
        }
        return builder.append(")").toString();
    }

    public static String normalizeSpaceExpression(String expression) {
        String input = expression == null || expression.isBlank() ? "." : expression;
        return "normalize-space(" + input + ")";
    }

    public static String cssAttributeEquals(String attributeName, String value) {
        String attribute = attributeName == null ? "" : attributeName.trim();
        if (attribute.isBlank()) {
            throw new IllegalArgumentException("attributeName must not be blank");
        }
        return "[" + attribute + "='" + cssString(value) + "']";
    }

    public static String cssString(String value) {
        String input = value == null ? "" : value;
        return input
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\A ")
                .replace("\r", "");
    }
}
