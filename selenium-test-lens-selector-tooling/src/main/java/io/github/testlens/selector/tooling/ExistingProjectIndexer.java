package io.github.testlens.selector.tooling;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.github.testlens.selector.tooling.ExistingProjectIndex.*;
import static io.github.testlens.selector.tooling.SelectorIndexModel.*;

/** Builds a bounded project index without executing project code. @since 0.5.0 */
public final class ExistingProjectIndexer {
    /** Returns the source-safe fingerprint used by {@link ExistingProjectIndex.ValueProjection}. */
    public static String selectorValueFingerprint(String value) {
        Objects.requireNonNull(value, "value");
        return fingerprint(List.of(canonicalSelectorValue(value)));
    }

    private static String canonicalSelectorValue(String value){
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("\\[([A-Za-z_][A-Za-z0-9_.:-]*)='([A-Za-z0-9_.:-]+)'\\]").matcher(value);StringBuilder out=new StringBuilder();while(matcher.find())matcher.appendReplacement(out,java.util.regex.Matcher.quoteReplacement("["+matcher.group(1)+"=\""+matcher.group(2)+"\"]"));matcher.appendTail(out);return out.toString();
    }

    public record Bounds(int maxSourceRoots, int maxFiles, int maxDeclarations, int maxTypes,
                         int maxMethods, int maxTests, int maxEdges) {
        public Bounds {
            if (maxSourceRoots <= 0 || maxFiles <= 0 || maxDeclarations <= 0 || maxTypes <= 0
                    || maxMethods <= 0 || maxTests <= 0 || maxEdges <= 0) {
                throw new IllegalArgumentException("Existing-project index bounds must be positive");
            }
        }

        public static Bounds defaults() { return new Bounds(64, 20_000, 100_000, 25_000, 100_000, 25_000, 250_000); }
    }

    public record Request(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries,
                          Charset encoding, Bounds bounds) {
        public Request(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries) {
            this(projectRoot, sourceRoots, classpathEntries, StandardCharsets.UTF_8, Bounds.defaults());
        }

        public Request {
            Objects.requireNonNull(projectRoot, "projectRoot");
            sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "sourceRoots"));
            classpathEntries = List.copyOf(Objects.requireNonNull(classpathEntries, "classpathEntries"));
            Objects.requireNonNull(encoding, "encoding");
            Objects.requireNonNull(bounds, "bounds");
        }
    }

    public ExistingProjectIndex index(Request request) {
        Objects.requireNonNull(request, "request");
        Limits selectorLimits = new Limits(4L * 1024L * 1024L, request.bounds().maxFiles(),
                request.bounds().maxSourceRoots(), 512, 64, 32, 4_096);
        ScanRequest scan = new ScanRequest(request.projectRoot(), request.sourceRoots(), request.classpathEntries(),
                "JAVA_17", request.encoding(), null, selectorLimits);
        return new JavaLocatorScanner().scanExistingProject(scan, request.bounds());
    }

    static final class Collector {
        private final Bounds bounds;
        private final List<UnitFacts> units = new ArrayList<>();

        Collector(Bounds bounds) { this.bounds = bounds; }

        void accept(String logicalPath, String contentHash, CompilationUnit unit,
                    List<DeclarationRecord> declarations) {
            units.add(new UnitFacts(logicalPath, contentHash, unit, declarations));
        }

        ExistingProjectIndex finish(SelectorIndex selectorIndex) {
            List<Limitation> limitations = new ArrayList<>();
            List<ClassDraft> classDrafts = collectClasses(limitations);
            Map<TypeDeclaration<?>, String> classIds = new HashMap<>();
            List<ClassEntry> classes = new ArrayList<>();
            for (ClassDraft draft : classDrafts) {
                classIds.put(draft.node(), draft.entry().id());
                classes.add(draft.entry());
            }

            List<ElementEntry> elements = collectElements(classIds, limitations);
            MethodCollection methodCollection = collectMethods(classIds, limitations);
            List<MethodEntry> methods = new ArrayList<>(methodCollection.entries());
            List<TestEntry> tests = collectTests(methodCollection, limitations);
            List<Edge> edges = collectEdges(methodCollection, tests, elements, classes, limitations);

            classes.sort(Comparator.comparing(ClassEntry::id));
            elements.sort(Comparator.comparing(ElementEntry::id));
            methods.sort(Comparator.comparing(MethodEntry::id));
            tests.sort(Comparator.comparing(TestEntry::id));
            edges.sort(Comparator.comparing(Edge::id));
            if (selectorIndex.coverage().incompleteClasspath()) {
                limitations.add(new Limitation("INCOMPLETE_CLASSPATH", null, null));
            }
            if (selectorIndex.coverage().filesFailed() > 0 || selectorIndex.coverage().filesExcluded() > 0) {
                limitations.add(new Limitation("UNINDEXED_FILES", null,
                        Integer.toString(selectorIndex.coverage().filesFailed() + selectorIndex.coverage().filesExcluded())));
            }
            boolean partial = !limitations.isEmpty() || selectorIndex.issues().stream()
                    .anyMatch(issue -> issue.code().endsWith("LIMIT"));
            int declarations = selectorIndex.coverage().declarationsFound();
            if (declarations > bounds.maxDeclarations()) {
                limitations.add(new Limitation("DECLARATIONS_LIMIT", null, Integer.toString(bounds.maxDeclarations())));
                partial = true;
            }
            limitations.sort(Comparator.comparing(Limitation::code)
                    .thenComparing(value -> safe(value.logicalPath())).thenComparing(value -> safe(value.subject())));
            String fingerprint = fingerprint(units.stream().map(value -> value.logicalPath() + "\u0000" + value.contentHash())
                    .sorted().toList());
            Metrics metrics = new Metrics(selectorIndex.coverage().sourceRootsFound(), selectorIndex.files().size(),
                    Math.min(declarations, bounds.maxDeclarations()), classes.size(), elements.size(), methods.size(),
                    tests.size(), edges.size(), selectorIndex.coverage().filesParsed());
            List<SourceFile> sourceFiles=units.stream().map(value->new SourceFile(value.logicalPath(),value.contentHash())).sorted(Comparator.comparing(SourceFile::logicalPath)).toList();
            return new ExistingProjectIndex(ExistingProjectIndex.SCHEMA_VERSION, fingerprint, sourceFiles, classes, elements, methods, tests, edges,
                    partial ? Completeness.PARTIAL : Completeness.COMPLETE, limitations, metrics);
        }

        private List<ClassDraft> collectClasses(List<Limitation> limitations) {
            List<ClassDraft> result = new ArrayList<>();
            outer: for (UnitFacts facts : units) {
                for (TypeDeclaration<?> type : facts.unit().findAll(TypeDeclaration.class)) {
                    if (result.size() >= bounds.maxTypes()) {
                        limitations.add(new Limitation("TYPES_LIMIT", facts.logicalPath(), null));
                        break outer;
                    }
                    String qualified = type.getFullyQualifiedName().orElseGet(() -> packagePrefix(facts.unit())
                            + nestedName(type));
                    String id = id("class", facts.logicalPath(), qualified);
                    List<String> parents = type instanceof ClassOrInterfaceDeclaration declaration
                            ? declaration.getExtendedTypes().stream().map(Node::toString).sorted().toList()
                            : List.of();
                    ClassClassification classification = classifyType(type, facts.declarations(), qualified, parents);
                    result.add(new ClassDraft(type, new ClassEntry(id, facts.logicalPath(), qualified,
                            type.getNameAsString(), classification, origin(type), parents)));
                }
            }
            return result;
        }

        private List<ElementEntry> collectElements(Map<TypeDeclaration<?>, String> classIds,
                                                   List<Limitation> limitations) {
            List<ElementEntry> result = new ArrayList<>();
            int seen = 0;
            outer: for (UnitFacts facts : units) {
                for (DeclarationRecord declaration : facts.declarations()) {
                    if (++seen > bounds.maxDeclarations()) break outer;
                    TypeDeclaration<?> owner = ownerType(facts.unit(), declaration.declaringSymbol().qualifiedTypeName());
                    String ownerId = classIds.get(owner);
                    if (ownerId == null) continue;
                    String name = Optional.ofNullable(declaration.declaringSymbol().memberSignature()).orElse("<expression>");
                    ResolvedLocator resolved = declaration.resolvedLocator();
                    String strategy = resolved == null ? null : resolved.strategy();
                    ValueProjection projection = resolved == null
                            ? new ValueProjection(declaration.resolutionStatus().name(), null)
                            : new ValueProjection("RESOLVED", selectorValueFingerprint(resolved.value()));
                    SelectorIndexModel.SourceRange range = declaration.sourceRange();
                    ExistingProjectIndex.SourceRange publicRange = new ExistingProjectIndex.SourceRange(
                            range.startLine(), range.startColumn(), range.endLine(),
                            range.endColumn(), range.startOffset(), range.endOffsetExclusive());
                    result.add(new ElementEntry(id("element", declaration.declarationRef()), ownerId, name,
                            declaration.declarationRef(), strategy, projection, publicRange,
                            declaration.contentFingerprint()));
                }
            }
            return result;
        }

        private MethodCollection collectMethods(Map<TypeDeclaration<?>, String> classIds,
                                                List<Limitation> limitations) {
            List<MethodDraft> drafts = new ArrayList<>();
            outer: for (UnitFacts facts : units) {
                for (MethodDeclaration method : facts.unit().findAll(MethodDeclaration.class)) {
                    if (drafts.size() >= bounds.maxMethods()) {
                        limitations.add(new Limitation("METHODS_LIMIT", facts.logicalPath(), null));
                        break outer;
                    }
                    TypeDeclaration<?> owner = method.findAncestor(TypeDeclaration.class).orElse(null);
                    String ownerId = classIds.get(owner);
                    if (ownerId == null) continue;
                    String signature = method.getSignature().asString();
                    String methodId = id("method", ownerId, signature);
                    List<Parameter> parameters = method.getParameters().stream()
                            .map(value -> new Parameter(value.getNameAsString(), value.getTypeAsString())).toList();
                    List<String> actions = method.findAll(MethodCallExpr.class).stream()
                            .map(call -> call.getNameAsString()).distinct().sorted().toList();
                    Set<String> referencedNames = method.findAll(com.github.javaparser.ast.expr.NameExpr.class).stream()
                            .map(value -> value.getNameAsString()).collect(java.util.stream.Collectors.toSet());
                    String ownerQualified = owner.getFullyQualifiedName().orElse(null);
                    List<String> declarationRefs = facts.declarations().stream()
                            .filter(value -> ownerQualified != null && ownerQualified.equals(value.declaringSymbol().qualifiedTypeName()))
                            .filter(value -> referencedNames.contains(value.declaringSymbol().memberSignature()))
                            .map(DeclarationRecord::declarationRef).distinct().sorted().toList();
                    List<String> outgoingTypes = method.findAll(MethodCallExpr.class).stream()
                            .map(call -> call.getScope().map(Object::toString).orElse(null))
                            .filter(Objects::nonNull).distinct().sorted().toList();
                    MethodEntry entry = new MethodEntry(methodId, ownerId, method.getNameAsString(), signature,
                            parameters, method.getTypeAsString(), actions, declarationRefs, outgoingTypes,
                            classifyMethod(method, actions));
                    drafts.add(new MethodDraft(facts.logicalPath(), method, entry));
                }
            }
            return new MethodCollection(drafts, drafts.stream().map(MethodDraft::entry).toList());
        }

        private List<TestEntry> collectTests(MethodCollection methods, List<Limitation> limitations) {
            List<TestEntry> result = new ArrayList<>();
            for (MethodDraft draft : methods.drafts()) {
                TestFramework framework = testFramework(draft.node());
                if (framework == null) continue;
                if (result.size() >= bounds.maxTests()) {
                    limitations.add(new Limitation("TESTS_LIMIT", draft.logicalPath(), null));
                    break;
                }
                List<String> tags = annotationStrings(draft.node(), Set.of("Tag", "Tags"));
                List<String> groups = annotationStrings(draft.node(), Set.of("Test"));
                List<String> calls = draft.node().findAll(MethodCallExpr.class).stream()
                        .map(call -> call.getNameAsString() + "/" + call.getArguments().size()).distinct().sorted().toList();
                result.add(new TestEntry(id("test", draft.entry().id()), draft.entry().ownerClassId(),
                        draft.entry().id(), framework, tags, framework == TestFramework.TESTNG ? groups : List.of(), calls));
            }
            return result;
        }

        private List<Edge> collectEdges(MethodCollection methods, List<TestEntry> tests, List<ElementEntry> elements,List<ClassEntry> classes,
                                        List<Limitation> limitations) {
            Map<String, List<MethodEntry>> byCall = new HashMap<>();
            methods.entries().forEach(method -> byCall.computeIfAbsent(method.name() + "/" + method.parameters().size(),
                    ignored -> new ArrayList<>()).add(method));
            Map<String, TestEntry> testsByMethod = new HashMap<>();
            tests.forEach(test -> testsByMethod.put(test.methodId(), test));
            Map<String, List<ElementEntry>> elementsByName = new HashMap<>();
            elements.forEach(element -> elementsByName.computeIfAbsent(element.name(), ignored -> new ArrayList<>()).add(element));
            Map<String,ClassEntry>classesById=classes.stream().collect(java.util.stream.Collectors.toMap(ClassEntry::id,value->value));
            Map<String,String>classIdsByName=new HashMap<>();classes.forEach(value->{classIdsByName.put(value.qualifiedName(),value.id());classIdsByName.putIfAbsent(value.simpleName(),value.id());});
            LinkedHashMap<String, Edge> result = new LinkedHashMap<>();
            for (MethodDraft source : methods.drafts()) {
                Set<String>allowedOwners=ownerHierarchy(source.entry().ownerClassId(),classesById,classIdsByName);
                for (MethodCallExpr call : source.node().findAll(MethodCallExpr.class)) {
                    String callKey = call.getNameAsString() + "/" + call.getArguments().size();
                    List<MethodEntry> targets = byCall.getOrDefault(callKey, List.of());
                    if (call.getScope().isEmpty()) {
                        targets = targets.stream().filter(value -> allowedOwners.contains(value.ownerClassId())).toList();
                    } else {
                        try {
                            String ownerName = call.resolve().declaringType().getQualifiedName();
                            String resolvedOwner = classIdsByName.get(ownerName);
                            targets = resolvedOwner == null ? List.of() : targets.stream()
                                    .filter(value -> value.ownerClassId().equals(resolvedOwner)).toList();
                        } catch (RuntimeException unresolved) {
                            limitations.add(new Limitation("UNRESOLVED_DYNAMIC_CALL", source.logicalPath(), callKey));
                            continue;
                        }
                    }
                    if (targets.size() == 1) {
                        EdgeType type = testsByMethod.containsKey(source.entry().id())
                                ? EdgeType.TEST_TO_METHOD : EdgeType.METHOD_TO_METHOD;
                        String from = type == EdgeType.TEST_TO_METHOD
                                ? testsByMethod.get(source.entry().id()).id() : source.entry().id();
                        addEdge(result, type, from, targets.get(0).id(), limitations, source.logicalPath(), callKey);
                    } else if (targets.size() > 1 || call.getScope().isEmpty()) {
                        limitations.add(new Limitation(targets.size() > 1 ? "AMBIGUOUS_CALL" : "DYNAMIC_CALL",
                                source.logicalPath(), callKey));
                    }
                }
                Set<String> names = source.node().findAll(com.github.javaparser.ast.expr.NameExpr.class).stream()
                        .map(value -> value.getNameAsString()).collect(java.util.stream.Collectors.toSet());
                for (String name : names) {
                    for (ElementEntry element : elementsByName.getOrDefault(name, List.of())) {
                        if (allowedOwners.contains(element.ownerClassId())) {
                            addEdge(result, EdgeType.METHOD_TO_DECLARATION, source.entry().id(),
                                    element.declarationRef(), limitations, source.logicalPath(), name);
                        }
                    }
                }
                for (String declarationRef : source.entry().declarationRefs()) {
                    addEdge(result, EdgeType.METHOD_TO_DECLARATION, source.entry().id(), declarationRef,
                            limitations, source.logicalPath(), declarationRef);
                }
            }
            return new ArrayList<>(result.values());
        }

        private Set<String> ownerHierarchy(String ownerId,Map<String,ClassEntry>classesById,Map<String,String>classIdsByName){Set<String>result=new LinkedHashSet<>();java.util.ArrayDeque<String>queue=new java.util.ArrayDeque<>();queue.add(ownerId);while(!queue.isEmpty()&&result.size()<=classesById.size()){String current=queue.removeFirst();if(!result.add(current))continue;ClassEntry entry=classesById.get(current);if(entry==null)continue;for(String parent:entry.extendsTypes()){String parentId=classIdsByName.get(parent);if(parentId==null&&parent.contains("."))parentId=classIdsByName.get(parent.substring(parent.lastIndexOf('.')+1));if(parentId!=null)queue.addLast(parentId);}}return result;}

        private void addEdge(Map<String, Edge> edges, EdgeType type, String from, String to,
                             List<Limitation> limitations, String path, String subject) {
            String edgeId = id("edge", type.name(), from, to);
            if (edges.containsKey(edgeId)) return;
            if (edges.size() >= bounds.maxEdges()) {
                if (limitations.stream().noneMatch(value -> value.code().equals("EDGES_LIMIT")))
                    limitations.add(new Limitation("EDGES_LIMIT", path, subject));
                return;
            }
            edges.put(edgeId, new Edge(edgeId, type, from, to));
        }

        private ClassClassification classifyType(TypeDeclaration<?> type, List<DeclarationRecord> declarations,
                                                 String qualified, List<String> parents) {
            String lower = type.getNameAsString().toLowerCase(Locale.ROOT);
            if (lower.contains("basepage"))
                return ClassClassification.BASE_PAGE;
            if (lower.contains("component") || lower.contains("widget") || lower.contains("fragment")
                    || lower.contains("navigation") || lower.endsWith("menu"))
                return ClassClassification.COMPONENT;
            if (lower.contains("helper") || lower.contains("util")) return ClassClassification.TEST_HELPER;
            if (lower.endsWith("page") || declarations.stream().anyMatch(value ->
                    qualified.equals(value.declaringSymbol().qualifiedTypeName()))) return ClassClassification.PAGE_OBJECT;
            return ClassClassification.UNKNOWN;
        }

        private Origin origin(TypeDeclaration<?> type) {
            String name = type.getNameAsString().toLowerCase(Locale.ROOT);
            if (name.endsWith("generatedbase") || name.startsWith("generated")) return Origin.GENERATED_BASE;
            if (name.endsWith("generated") || name.endsWith("extension")) return Origin.GENERATED_EXTENSION;
            return Origin.HAND_WRITTEN;
        }

        private MethodClassification classifyMethod(MethodDeclaration method, List<String> actions) {
            String name = method.getNameAsString().toLowerCase(Locale.ROOT);
            if (name.startsWith("assert") || actions.stream().anyMatch(value -> value.toLowerCase(Locale.ROOT).startsWith("assert")))
                return MethodClassification.ASSERTION;
            if (name.startsWith("open") || name.startsWith("goto") || name.startsWith("navigate")
                    || (!method.getType().isVoidType() && method.getTypeAsString().endsWith("Page")
                    && actions.stream().anyMatch(value -> value.equals("click"))))
                return MethodClassification.NAVIGATION;
            if (actions.stream().anyMatch(value -> Set.of("click", "sendKeys", "clear", "select").contains(value)))
                return MethodClassification.ACTION;
            if (actions.size() > 1) return MethodClassification.WORKFLOW;
            if (name.startsWith("get") || name.startsWith("is") || name.startsWith("has") || !method.getType().isVoidType())
                return MethodClassification.QUERY;
            if (method.isStatic()) return MethodClassification.UTILITY;
            return MethodClassification.UNKNOWN;
        }

        private TestFramework testFramework(MethodDeclaration method) {
            for (AnnotationExpr annotation : method.getAnnotations()) {
                if (!annotation.getName().getIdentifier().equals("Test")) continue;
                try {
                    String qualified = annotation.resolve().getQualifiedName();
                    if (qualified.equals("org.junit.jupiter.api.Test")) return TestFramework.JUNIT5;
                    if (qualified.equals("org.testng.annotations.Test")) return TestFramework.TESTNG;
                } catch (RuntimeException ignored) {
                    CompilationUnit unit = method.findCompilationUnit().orElseThrow();
                    if (hasImport(unit, "org.junit.jupiter.api.Test")) return TestFramework.JUNIT5;
                    if (hasImport(unit, "org.testng.annotations.Test")) return TestFramework.TESTNG;
                }
            }
            return null;
        }

        private List<String> annotationStrings(MethodDeclaration method, Set<String> names) {
            return method.getAnnotations().stream().filter(value -> names.contains(value.getName().getIdentifier()))
                    .flatMap(value -> value.findAll(StringLiteralExpr.class).stream()).map(StringLiteralExpr::asString)
                    .distinct().sorted().toList();
        }

        private boolean hasImport(CompilationUnit unit, String qualified) {
            return unit.getImports().stream().anyMatch(value -> value.getNameAsString().equals(qualified));
        }

        private TypeDeclaration<?> ownerType(CompilationUnit unit, String qualifiedName) {
            if (qualifiedName == null) return null;
            return unit.findAll(TypeDeclaration.class).stream()
                    .filter(value -> value.getFullyQualifiedName().orElse("").equals(qualifiedName)).findFirst().orElse(null);
        }

        private String nestedName(TypeDeclaration<?> type) {
            List<String> names = new ArrayList<>();
            Node current = type;
            while (current instanceof TypeDeclaration<?> declaration) {
                names.add(0, declaration.getNameAsString());
                current = current.getParentNode().orElse(null);
            }
            return String.join(".", names);
        }

        private String packagePrefix(CompilationUnit unit) {
            return unit.getPackageDeclaration().map(value -> value.getNameAsString() + ".").orElse("");
        }
    }

    private static String id(String kind, String... fields) {
        StringBuilder canonical = new StringBuilder(kind);
        for (String field : fields) canonical.append('\u0000').append(field == null ? "" : field);
        return kind + "-v1:sha256:" + sha256(canonical.toString());
    }

    private static String fingerprint(List<String> values) { return "sha256:" + sha256(String.join("\u0000", values)); }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private record UnitFacts(String logicalPath, String contentHash, CompilationUnit unit,
                             List<DeclarationRecord> declarations) { }
    private record ClassDraft(TypeDeclaration<?> node, ClassEntry entry) { }
    private record MethodDraft(String logicalPath, MethodDeclaration node, MethodEntry entry) { }
    private record MethodCollection(List<MethodDraft> drafts, List<MethodEntry> entries) { }
}
