package io.github.testlens.selector.tooling;

import java.util.List;
import java.util.Comparator;

/**
 * Deterministic, source-safe description of the page objects and tests already present in a project.
 * This tooling contract is not persisted by the runtime and may evolve until the tooling artifact is published.
 *
 * @since 0.5.0
 */
public record ExistingProjectIndex(
        int schemaVersion,
        String projectFingerprint,
        List<SourceFile> sourceFiles,
        List<ClassEntry> classes,
        List<ElementEntry> elements,
        List<MethodEntry> methods,
        List<TestEntry> tests,
        List<Edge> edges,
        Completeness completeness,
        List<Limitation> limitations,
        Metrics metrics) {

    public static final int SCHEMA_VERSION = 1;

    public ExistingProjectIndex(int schemaVersion,String projectFingerprint,List<ClassEntry>classes,List<ElementEntry>elements,List<MethodEntry>methods,List<TestEntry>tests,List<Edge>edges,Completeness completeness,List<Limitation>limitations,Metrics metrics){this(schemaVersion,projectFingerprint,List.of(),classes,elements,methods,tests,edges,completeness,limitations,metrics);}

    public ExistingProjectIndex {
        sourceFiles = canonical(sourceFiles, Comparator.comparing(SourceFile::logicalPath));
        classes = canonical(classes, Comparator.comparing(ClassEntry::id));
        elements = canonical(elements, Comparator.comparing(ElementEntry::id));
        methods = canonical(methods, Comparator.comparing(MethodEntry::id));
        tests = canonical(tests, Comparator.comparing(TestEntry::id));
        edges = canonical(edges, Comparator.comparing(Edge::id));
        limitations = canonical(limitations, Comparator.comparing(Limitation::code).thenComparing(Limitation::logicalPath).thenComparing(Limitation::subject));
    }

    public enum ClassClassification { PAGE_OBJECT, COMPONENT, BASE_PAGE, TEST_HELPER, UNKNOWN }
    public enum Origin { HAND_WRITTEN, GENERATED_BASE, GENERATED_EXTENSION, UNKNOWN }
    public enum MethodClassification { ACTION, QUERY, ASSERTION, NAVIGATION, WORKFLOW, UTILITY, UNKNOWN }
    public enum TestFramework { JUNIT5, TESTNG, UNKNOWN }
    public enum EdgeType { TEST_TO_METHOD, METHOD_TO_METHOD, METHOD_TO_DECLARATION }
    public enum Completeness { COMPLETE, PARTIAL }

    /** Content-addressed file identity used for future changed-file re-indexing; it never contains source text. */
    public record SourceFile(String logicalPath,String contentFingerprint) { }

    public record ClassEntry(String id, String logicalPath, String qualifiedName, String simpleName,
                             ClassClassification classification, Origin origin, List<String> extendsTypes) {
        public ClassEntry { extendsTypes = List.copyOf(extendsTypes); }
    }

    public record ValueProjection(String resolution, String fingerprint) { }

    public record SourceRange(int startLine, int startColumn, int endLine, int endColumn,
                              int startOffset, int endOffsetExclusive) { }

    public record ElementEntry(String id, String ownerClassId, String name, String declarationRef,
                               String strategy, ValueProjection valueProjection, SourceRange range,
                               String declarationFingerprint) {
        public ElementEntry(String id, String ownerClassId, String name, String declarationRef,
                            String strategy, ValueProjection valueProjection, SourceRange range) {
            this(id, ownerClassId, name, declarationRef, strategy, valueProjection, range, null);
        }
    }

    public record Parameter(String name, String type) { }

    public record MethodEntry(String id, String ownerClassId, String name, String signature,
                              List<Parameter> parameters, String returnType, List<String> actions,
                              List<String> declarationRefs, List<String> outgoingTypes,
                              MethodClassification classification) {
        public MethodEntry {
            parameters = List.copyOf(parameters);
            actions = List.copyOf(actions);
            declarationRefs = List.copyOf(declarationRefs);
            outgoingTypes = List.copyOf(outgoingTypes);
        }
    }

    public record TestEntry(String id, String ownerClassId, String methodId, TestFramework framework,
                            List<String> tags, List<String> groups, List<String> calls) {
        public TestEntry {
            tags = List.copyOf(tags);
            groups = List.copyOf(groups);
            calls = List.copyOf(calls);
        }
    }

    public record Edge(String id, EdgeType type, String fromId, String toId) { }
    public record Limitation(String code, String logicalPath, String subject) { }
    public record Metrics(int sourceRoots, int files, int declarations, int classes, int elements,
                          int methods, int tests, int edges, int parsedFiles) { }
    private static <T> List<T> canonical(List<T> values,Comparator<? super T> comparator){return(values==null?List.<T>of():values).stream().filter(java.util.Objects::nonNull).distinct().sorted(comparator).toList();}
}
