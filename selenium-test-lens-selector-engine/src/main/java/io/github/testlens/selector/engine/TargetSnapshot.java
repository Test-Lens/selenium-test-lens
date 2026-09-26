package io.github.testlens.selector.engine;

import java.util.List;
import java.util.Map;

/** Internal immutable input captured by an explicit live analysis request. */
public record TargetSnapshot(String tagName, String id, String name, List<String> classTokens,
                             Map<String, String> testAttributes, String visibleText,
                             String accessibleName, String ariaRole, List<AncestorHint> ancestors,
                             int instrumentationNodesExcluded, boolean truncated) {
    public static final int MAX_TEXT_CODE_POINTS = 256;
    public static final int MAX_LOCATOR_CODE_POINTS = 2_048;
    public static final int MAX_CLASS_TOKENS = 8;
    public static final int MAX_TEST_ATTRIBUTES = 8;
    public static final int MAX_ANCESTOR_DEPTH = 3;
    public static final int MAX_SNAPSHOT_BYTES = 32 * 1024;

    public TargetSnapshot {
        tagName = bounded(tagName, 128); id = bounded(id, MAX_LOCATOR_CODE_POINTS);
        name = bounded(name, MAX_LOCATOR_CODE_POINTS); visibleText = bounded(visibleText, MAX_TEXT_CODE_POINTS);
        accessibleName = bounded(accessibleName, MAX_TEXT_CODE_POINTS); ariaRole = bounded(ariaRole, 128);
        classTokens = List.copyOf(classTokens == null ? List.of() : classTokens.stream().limit(MAX_CLASS_TOKENS).toList());
        testAttributes = Map.copyOf(testAttributes == null ? Map.of() : testAttributes);
        ancestors = List.copyOf(ancestors == null ? List.of() : ancestors.stream().limit(MAX_ANCESTOR_DEPTH).toList());
    }

    public record AncestorHint(int depth, String tagName, String id, Map<String, String> testAttributes,
                               List<String> classTokens) {
        public AncestorHint {
            if (depth < 1 || depth > MAX_ANCESTOR_DEPTH) throw new IllegalArgumentException("ancestor depth out of range");
            tagName = bounded(tagName, 128); id = bounded(id, MAX_LOCATOR_CODE_POINTS);
            testAttributes = Map.copyOf(testAttributes == null ? Map.of() : testAttributes);
            classTokens = List.copyOf(classTokens == null ? List.of() : classTokens.stream().limit(MAX_CLASS_TOKENS).toList());
        }
    }

    private static String bounded(String value, int max) {
        if (value == null) return null;
        int count=value.codePointCount(0,value.length());
        return count<=max?value:value.substring(0,value.offsetByCodePoints(0,max));
    }
}
