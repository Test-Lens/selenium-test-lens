package io.github.testlens.selenium.locator;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/** Immutable internal query stages used by compositional locators. */
final class CompositeBy extends By {
    private final Query query;
    private final String description;

    private CompositeBy(Query query, String description) {
        this.query = query;
        this.description = description;
    }

    static By descendants(By parents, By descendants) {
        Objects.requireNonNull(parents, "parent query must not be null");
        Objects.requireNonNull(descendants, "descendant query must not be null");
        return new CompositeBy(context -> {
            LinkedHashSet<WebElement> result = new LinkedHashSet<>();
            for (WebElement parent : find(context, parents)) {
                result.addAll(find(parent, descendants));
            }
            return List.copyOf(result);
        }, safe(parents) + " >> " + safe(descendants));
    }

    static By text(By source, String expected, boolean containing) {
        Objects.requireNonNull(source, "source query must not be null");
        Objects.requireNonNull(expected, "expected text must not be null");
        String normalized = normalize(expected);
        return filter(source, element -> {
            String actual = normalize(element.getText());
            return containing ? actual.contains(normalized) : actual.equals(normalized);
        }, containing ? "text contains" : "text equals");
    }

    static By attribute(By source, String name, String expected) {
        Objects.requireNonNull(source, "source query must not be null");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("attribute name must not be blank");
        Objects.requireNonNull(expected, "expected attribute value must not be null");
        return filter(source, element -> expected.equals(element.getDomAttribute(name)),
                "attribute " + preview(name));
    }

    static By has(By source, By descendant) {
        Objects.requireNonNull(source, "source query must not be null");
        Objects.requireNonNull(descendant, "descendant query must not be null");
        return filter(source, element -> !find(element, descendant).isEmpty(),
                "has(" + safe(descendant) + ")");
    }

    static By index(By source, int index) {
        Objects.requireNonNull(source, "source query must not be null");
        return new CompositeBy(context -> {
            List<WebElement> current = find(context, source);
            if (index < 0 || index >= current.size()) {
                throw new CollectionSelectionException(String.valueOf(index), current.size());
            }
            return List.of(current.get(index));
        }, safe(source) + " [" + index + "]");
    }

    static By last(By source) {
        Objects.requireNonNull(source, "source query must not be null");
        return new CompositeBy(context -> {
            List<WebElement> current = find(context, source);
            if (current.isEmpty()) throw new CollectionSelectionException("last", 0);
            return List.of(current.get(current.size() - 1));
        }, safe(source) + " [last]");
    }

    private static By filter(By source, ElementPredicate predicate, String suffix) {
        return new CompositeBy(context -> {
            List<WebElement> result = new ArrayList<>();
            for (WebElement element : find(context, source)) {
                if (predicate.test(element)) result.add(element);
            }
            return List.copyOf(result);
        }, safe(source) + " | " + suffix);
    }

    @Override
    public List<WebElement> findElements(SearchContext context) {
        return query.find(Objects.requireNonNull(context, "search context must not be null"));
    }

    @Override
    public String toString() {
        return description;
    }

    static String normalize(String value) {
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

    static List<WebElement> find(SearchContext context, By query) {
        return query instanceof By.Remotable ? context.findElements(query) : query.findElements(context);
    }

    private static String safe(By by) {
        return preview(String.valueOf(by));
    }

    private static String preview(String value) {
        String normalized = normalize(value);
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 117) + "...";
    }

    @FunctionalInterface private interface Query { List<WebElement> find(SearchContext context); }
    @FunctionalInterface private interface ElementPredicate { boolean test(WebElement element); }
}
