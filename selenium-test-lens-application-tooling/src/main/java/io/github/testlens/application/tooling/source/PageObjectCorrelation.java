package io.github.testlens.application.tooling.source;

import java.util.Comparator;
import java.util.List;

/** Evidence-bearing relation between observed application elements and existing source declarations. @since 0.5.0 */
public record PageObjectCorrelation(int schemaVersion, List<ClassCorrelation> classes,
                                    List<ElementCorrelation> elements, Completeness completeness,
                                    List<String> limitations, Metrics metrics) {
    public static final int SCHEMA_VERSION = 1;
    public PageObjectCorrelation {
        if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported schemaVersion: " + schemaVersion);
        classes = canonical(classes, Comparator.comparing(ClassCorrelation::pageId).thenComparing(ClassCorrelation::classId));
        elements = canonical(elements, Comparator.comparing(ElementCorrelation::applicationElementId)
                .thenComparing(value -> value.sourceElementId() == null ? "" : value.sourceElementId()));
        limitations = strings(limitations);
        if (completeness == null || metrics == null) throw new IllegalArgumentException("completeness and metrics are required");
    }
    public record ClassCorrelation(String pageId, String classId, State state, List<Evidence> evidence,
                                   List<String> conflicts, List<String> limitations) {
        public ClassCorrelation { evidence=canonical(evidence,Comparator.comparing(Enum::name));conflicts=strings(conflicts);limitations=strings(limitations); }
    }
    public record ElementCorrelation(String pageId, String applicationElementId, String sourceElementId,
                                     String sourceDeclarationRef, State state, List<Evidence> evidence,
                                     List<String> conflicts, List<String> limitations) {
        public ElementCorrelation { evidence=canonical(evidence,Comparator.comparing(Enum::name));conflicts=strings(conflicts);limitations=strings(limitations); }
    }
    public record Metrics(int pages, int applicationElements, int sourceElements, int exact, int strong,
                          int probable, int ambiguous, int noMatch, int conflicts) { }
    public enum State { EXACT, STRONG, PROBABLE, AMBIGUOUS, NO_MATCH, CONFLICT }
    public enum Evidence { SAME_CANDIDATE_ID, SAME_DECLARATION_PROVENANCE, LIVE_SAME_TARGET,
        NORMALIZED_SELECTOR_MATCH, SAME_PAGE_IDENTITY, SEMANTIC_ROLE_MATCH, ACCESSIBLE_NAME_MATCH, USER_OVERRIDE }
    public enum Completeness { COMPLETE, PARTIAL }
    private static List<String> strings(List<String> values){return(values==null?List.<String>of():values).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
    private static <T> List<T> canonical(List<T> values,Comparator<? super T> comparator){return(values==null?List.<T>of():values).stream().filter(java.util.Objects::nonNull).distinct().sorted(comparator).toList();}
}
