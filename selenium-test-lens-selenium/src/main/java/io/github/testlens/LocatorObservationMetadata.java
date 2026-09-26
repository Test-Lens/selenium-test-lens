package io.github.testlens;

import org.openqa.selenium.By;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/** Internal, flattened selector-observation contract carried by existing log events. */
final class LocatorObservationMetadata {
    static final String PREFIX = "testlens.selector.";
    static final int MAX_CONTEXT_SEGMENTS = 16;
    private static final int MAX_DISPLAY_CODE_POINTS = 512;

    private LocatorObservationMetadata() {}

    static Locator locator(By by, String label) {
        String safeLabel = label == null ? "" : label;
        if (by == null) return Locator.unknown(safeLabel);
        if (by instanceof By.Remotable remotable) {
            try {
                By.Remotable.Parameters parameters = remotable.getRemoteParameters();
                String strategy = parameters == null ? "" : safeScalar(parameters.using());
                Object value = parameters == null ? null : parameters.value();
                if (isScalar(value)) {
                    return new Locator(strategy, safeScalar(value), "KNOWN", safeDisplay(by),
                            "STRUCTURED", safeLabel, "");
                }
                return new Locator(strategy, "", "UNAVAILABLE", genericDisplay(by),
                        "REMOTE_COMPLEX", safeLabel, by.getClass().getName());
            } catch (Throwable ignored) {
                return new Locator("custom", "", "UNAVAILABLE", safeDisplay(by),
                        "CUSTOM_OPAQUE", safeLabel, by.getClass().getName());
            }
        }
        return new Locator("custom", "", "UNAVAILABLE", safeDisplay(by),
                "CUSTOM_OPAQUE", safeLabel, by.getClass().getName());
    }

    static Context root() {
        return new Context("KNOWN", List.of(new Segment("DRIVER_ROOT", "KNOWN", null, "", null, "")));
    }

    static Context unknown() {
        return new Context("UNKNOWN", List.of(new Segment("UNKNOWN", "UNKNOWN", null, "", null, "")));
    }

    static Map<String, String> observation(Locator locator, Context context, String intent,
                                           String outcome, Integer matchCount, long durationNanos) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(PREFIX + "schemaVersion", "1");
        putLocator(values, PREFIX + "locator.", locator);
        Context safeContext = context == null ? unknown() : context;
        values.put(PREFIX + "context.knowledge", safeContext.knowledge());
        values.put(PREFIX + "context.segmentCount", String.valueOf(safeContext.segments().size()));
        for (int index = 0; index < safeContext.segments().size(); index++) {
            Segment segment = safeContext.segments().get(index);
            String base = PREFIX + "context.segment." + index + ".";
            values.put(base + "kind", segment.kind());
            values.put(base + "knowledge", segment.knowledge());
            if (segment.locator() != null) putLocator(values, base + "locator.", segment.locator());
            put(values, base + "label", segment.label());
            put(values, base + "reference.kind", segment.referenceKind());
            put(values, base + "reference.value", segment.referenceValue());
        }
        values.put(PREFIX + "usageIntent", intent == null ? "UNKNOWN" : intent);
        values.put(PREFIX + "outcome", outcome == null ? "UNKNOWN" : outcome);
        values.put(PREFIX + "matchCount.knowledge", matchCount == null ? "UNKNOWN" : "KNOWN");
        if (matchCount != null) values.put(PREFIX + "matchCount.value", String.valueOf(matchCount));
        if (durationNanos >= 0) values.put(PREFIX + "resolutionDurationNanos", String.valueOf(durationNanos));
        return Map.copyOf(values);
    }

    private static void putLocator(Map<String, String> values, String prefix, Locator locator) {
        Locator safe = locator == null ? Locator.unknown("") : locator;
        values.put(prefix + "strategy", safe.strategy());
        put(values, prefix + "value", safe.value());
        values.put(prefix + "valueState", safe.valueState());
        values.put(prefix + "display", safe.display());
        values.put(prefix + "supportKind", safe.supportKind());
        put(values, prefix + "label", safe.label());
        put(values, prefix + "implementationClass", safe.implementationClass());
    }

    private static void put(Map<String, String> values, String key, String value) {
        if (value != null && !value.isBlank()) values.put(key, value);
    }

    private static boolean isScalar(Object value) {
        return value instanceof CharSequence || value instanceof Character
                || value instanceof Number || value instanceof Boolean;
    }

    private static String safeScalar(Object value) {
        return value == null ? "" : truncate(String.valueOf(value));
    }

    static String safeDisplay(By by) {
        if (by == null) return "Unknown locator";
        try {
            String value = by.toString();
            if (value != null && !value.isBlank()) return truncate(value);
        } catch (Throwable ignored) {
            // Display diagnostics cannot alter the Selenium command.
        }
        return genericDisplay(by);
    }

    static String compact(Locator locator) {
        if (locator == null) return "";
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

    private static String genericDisplay(By by) {
        return by == null ? "Unknown locator" : "Locator " + by.getClass().getName();
    }

    private static String truncate(String value) {
        if (value == null) return "";
        int count = value.codePointCount(0, value.length());
        if (count <= MAX_DISPLAY_CODE_POINTS) return value;
        int end = value.offsetByCodePoints(0, MAX_DISPLAY_CODE_POINTS - 1);
        return value.substring(0, end) + "…";
    }

    record Locator(String strategy, String value, String valueState, String display,
                   String supportKind, String label, String implementationClass) {
        static Locator unknown(String label) {
            return new Locator("custom", "", "UNAVAILABLE", "Unknown locator",
                    "CUSTOM_OPAQUE", label == null ? "" : label, "");
        }

        Locator withLabel(String value) {
            return new Locator(strategy, this.value, valueState, display, supportKind,
                    value == null ? "" : value, implementationClass);
        }

        Locator redacted(UnaryOperator<String> redactor) {
            return new Locator(strategy, redact(redactor, value), valueState, redact(redactor, display), supportKind,
                    redact(redactor, label), implementationClass);
        }

        private static String redact(UnaryOperator<String> redactor, String value) {
            return value == null || value.isBlank() ? value : redactor.apply(value);
        }
    }

    record Segment(String kind, String knowledge, Locator locator, String label,
                   String referenceKind, String referenceValue) {}

    record Context(String knowledge, List<Segment> segments) {
        Context {
            knowledge = knowledge == null ? "UNKNOWN" : knowledge;
            segments = segments == null ? List.of() : List.copyOf(segments);
        }

        Context append(Segment segment) {
            if (segment == null) return this;
            List<Segment> copy = new ArrayList<>(segments);
            copy.add(segment);
            String nextKnowledge = knowledge;
            if (copy.size() > MAX_CONTEXT_SEGMENTS) {
                Segment root = copy.get(0);
                List<Segment> bounded = new ArrayList<>(MAX_CONTEXT_SEGMENTS);
                bounded.add(root.kind().equals("DRIVER_ROOT") ? root
                        : new Segment("UNKNOWN", "UNKNOWN", null, "", null, ""));
                bounded.addAll(copy.subList(copy.size() - (MAX_CONTEXT_SEGMENTS - 1), copy.size()));
                copy = bounded;
                nextKnowledge = "PARTIAL";
            }
            if (!"KNOWN".equals(segment.knowledge())) nextKnowledge = "PARTIAL";
            return new Context(nextKnowledge, copy);
        }
    }
}
