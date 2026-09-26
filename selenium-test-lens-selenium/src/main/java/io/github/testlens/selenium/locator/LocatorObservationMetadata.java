package io.github.testlens.selenium.locator;

import org.openqa.selenium.By;

import java.util.LinkedHashMap;
import java.util.Map;

/** Package-private encoder for the shared structured locator observation contract. */
final class LocatorObservationMetadata {
    private static final String PREFIX = "testlens.selector.";
    private static final int MAX_DISPLAY_CODE_POINTS = 512;

    private LocatorObservationMetadata() {}

    static Map<String, String> observation(By by, String label, String knownDisplay, String intent, String outcome,
                                           Integer matchCount, long durationNanos) {
        Locator locator = locator(by, label, knownDisplay);
        Map<String, String> values = new LinkedHashMap<>();
        values.put(PREFIX + "schemaVersion", "1");
        values.put(PREFIX + "locator.strategy", locator.strategy());
        put(values, PREFIX + "locator.value", locator.value());
        values.put(PREFIX + "locator.valueState", locator.valueState());
        values.put(PREFIX + "locator.display", locator.display());
        values.put(PREFIX + "locator.supportKind", locator.supportKind());
        put(values, PREFIX + "locator.label", locator.label());
        put(values, PREFIX + "locator.implementationClass", locator.implementationClass());
        values.put(PREFIX + "context.knowledge", "KNOWN");
        values.put(PREFIX + "context.segmentCount", "1");
        values.put(PREFIX + "context.segment.0.kind", "DRIVER_ROOT");
        values.put(PREFIX + "context.segment.0.knowledge", "KNOWN");
        values.put(PREFIX + "usageIntent", intent);
        values.put(PREFIX + "outcome", outcome);
        values.put(PREFIX + "matchCount.knowledge", matchCount == null ? "UNKNOWN" : "KNOWN");
        if (matchCount != null) values.put(PREFIX + "matchCount.value", String.valueOf(matchCount));
        values.put(PREFIX + "resolutionDurationNanos", String.valueOf(Math.max(0L, durationNanos)));
        values.put("testlens.internal.captureSourceLocation", "true");
        return Map.copyOf(values);
    }

    static String display(By by) {
        if (by == null) return "Unknown locator";
        try {
            String value = by.toString();
            if (value != null && !value.isBlank()) return truncate(value);
        } catch (Throwable ignored) {
            // A diagnostic display must not change the locator operation.
        }
        return "Locator " + by.getClass().getName();
    }

    static String compact(By by, String knownDisplay) {
        Locator locator = locator(by, "", knownDisplay);
        if (!"STRUCTURED".equals(locator.supportKind()) || !"KNOWN".equals(locator.valueState())) {
            return locator.display();
        }
        String value = locator.value();
        return switch (locator.strategy()) {
            case "id" -> "#" + cssIdentifier(value);
            case "css selector" -> value;
            case "name" -> "[name=\"" + cssString(value) + "\"]";
            case "class name" -> singleCssClass(value) ? "." + cssIdentifier(value) : quoted("class", value);
            case "tag name" -> singleCssClass(value) ? cssIdentifier(value) : quoted("tag", value);
            case "xpath" -> "xpath: " + value;
            case "link text" -> quoted("link", value);
            case "partial link text" -> quoted("link~", value);
            default -> locator.strategy() + ": " + value;
        };
    }

    private static String quoted(String prefix, String value) {
        return prefix + ": \"" + cssString(value) + "\"";
    }

    private static boolean singleCssClass(String value) {
        return value != null && !value.isBlank() && value.codePoints().noneMatch(Character::isWhitespace);
    }

    private static String cssString(String value) {
        if (value == null) return "";
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (codePoint == '\\' || codePoint == '"') out.append('\\').appendCodePoint(codePoint);
            else if (codePoint == 0 || codePoint < 0x20 || codePoint == 0x7f) {
                out.append('\\').append(Integer.toHexString(codePoint == 0 ? 0xfffd : codePoint)).append(' ');
            } else out.appendCodePoint(codePoint);
        });
        return out.toString();
    }

    private static String cssIdentifier(String value) {
        if (value == null || value.isEmpty()) return "\\fffd ";
        StringBuilder out = new StringBuilder(value.length());
        int position = 0;
        int first = value.codePointAt(0);
        int codePointCount = value.codePointCount(0, value.length());
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            boolean escapeAsCodePoint = codePoint == 0 || codePoint < 0x20 || codePoint == 0x7f
                    || (position == 0 && Character.isDigit(codePoint))
                    || (position == 1 && first == '-' && Character.isDigit(codePoint));
            if (escapeAsCodePoint) {
                out.append('\\').append(Integer.toHexString(codePoint == 0 ? 0xfffd : codePoint)).append(' ');
            } else if (position == 0 && codePoint == '-' && codePointCount == 1) {
                out.append("\\-");
            } else if (codePoint >= 0x80 || codePoint == '-' || codePoint == '_'
                    || Character.isLetterOrDigit(codePoint)) {
                out.appendCodePoint(codePoint);
            } else {
                out.append('\\').appendCodePoint(codePoint);
            }
            offset += Character.charCount(codePoint);
            position++;
        }
        return out.toString();
    }

    private static Locator locator(By by, String label, String knownDisplay) {
        String safeLabel = label == null ? "" : label;
        String safeDisplay = knownDisplay == null || knownDisplay.isBlank() ? display(by) : knownDisplay;
        if (by instanceof By.Remotable remotable) {
            try {
                By.Remotable.Parameters parameters = remotable.getRemoteParameters();
                String strategy = parameters == null || parameters.using() == null
                        ? "" : truncate(String.valueOf(parameters.using()));
                Object value = parameters == null ? null : parameters.value();
                if (isScalar(value)) return new Locator(strategy, truncate(String.valueOf(value)), "KNOWN",
                        safeDisplay, "STRUCTURED", safeLabel, "");
                return new Locator(strategy, "", "UNAVAILABLE", genericDisplay(by),
                        "REMOTE_COMPLEX", safeLabel, by.getClass().getName());
            } catch (Throwable ignored) {
                // Fall through to an opaque, non-fatal representation.
            }
        }
        return new Locator("custom", "", "UNAVAILABLE", safeDisplay,
                "CUSTOM_OPAQUE", safeLabel, by == null ? "" : by.getClass().getName());
    }

    private static boolean isScalar(Object value) {
        return value instanceof CharSequence || value instanceof Character
                || value instanceof Number || value instanceof Boolean;
    }

    private static void put(Map<String, String> values, String key, String value) {
        if (value != null && !value.isBlank()) values.put(key, value);
    }

    private static String truncate(String value) {
        if (value == null) return "";
        int count = value.codePointCount(0, value.length());
        if (count <= MAX_DISPLAY_CODE_POINTS) return value;
        return value.substring(0, value.offsetByCodePoints(0, MAX_DISPLAY_CODE_POINTS - 1)) + "…";
    }

    private static String genericDisplay(By by) {
        return by == null ? "Unknown locator" : "Locator " + by.getClass().getName();
    }

    private record Locator(String strategy, String value, String valueState, String display,
                           String supportKind, String label, String implementationClass) {}
}
