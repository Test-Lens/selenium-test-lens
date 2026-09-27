package io.github.testlens.selector.tooling;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Bounded symbol-resolved declaration-to-use projection for local migration review. */
public final class TrustedSelectorUseGraph {
    public static final int MAX_RETAINED_USE_SITES = 100_000;
    public static final int MAX_DETAILED_USE_SITES = 256;
    public static final int MAX_UNRESOLVED_ISSUES = 512;

    private TrustedSelectorUseGraph() { }

    public static Graph analyze(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries,
                                TrustedSelectorSourceProjection.Snapshot projection) {
        Objects.requireNonNull(projection);
        try {
            Path root = projectRoot.toRealPath();
            CombinedTypeSolver solver = new CombinedTypeSolver(new ReflectionTypeSolver(false));
            List<Path> roots = new ArrayList<>();
            for (Path supplied : sourceRoots) {
                Path sourceRoot = supplied.isAbsolute() ? supplied.normalize() : root.resolve(supplied).normalize();
                if (!sourceRoot.startsWith(root) || !Files.isDirectory(sourceRoot)) continue;
                Path real = sourceRoot.toRealPath();
                if (!real.startsWith(root)) throw new IllegalArgumentException("source root escapes project");
                roots.add(real); solver.add(new JavaParserTypeSolver(real));
            }
            for (Path entry : classpathEntries) {
                Path actual = entry.toAbsolutePath().normalize();
                if (Files.isDirectory(actual)) solver.add(new JavaParserTypeSolver(actual));
                else if (Files.isRegularFile(actual) && actual.toString().endsWith(".jar")) solver.add(new JarTypeSolver(actual));
            }
            JavaParser parser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                    .setCharacterEncoding(StandardCharsets.UTF_8).setSymbolResolver(new JavaSymbolSolver(solver)));
            Map<String, TrustedSelectorSourceProjection.Declaration> fields = new HashMap<>();
            projection.declarations().stream().filter(value -> value.declarationKind() == TrustedSelectorSourceProjection.DeclarationKind.BY_FIELD)
                    .forEach(value -> fields.put(value.declaringSymbol().replace("#null", ""), value));
            List<UseSite> retained = new ArrayList<>(); List<String> unresolved = new ArrayList<>(); int[] total = {0};Map<String,Integer>declarationCounts=new HashMap<>();
            for (Path sourceRoot : roots) {
                try (var paths = Files.walk(sourceRoot)) {
                    for (Path file : paths.filter(value -> Files.isRegularFile(value) && value.toString().endsWith(".java")).sorted().toList()) {
                        if (Files.isSymbolicLink(file) || !file.toRealPath().startsWith(root)) continue;
                        var parsed = parser.parse(file);
                        if (parsed.getResult().isEmpty() || !parsed.isSuccessful()) { add(unresolved, "PARSE:" + logical(root, file)); continue; }
                        for (NameExpr name : parsed.getResult().orElseThrow().findAll(NameExpr.class)) {
                            try {
                                ResolvedValueDeclaration resolved = name.resolve();
                                if (!resolved.isField()) continue;
                                String key = resolved.asField().declaringType().getQualifiedName() + "#" + resolved.getName() + "#";
                                var declaration = fields.get(key);
                                if (declaration == null || insideDeclaration(name, declaration, logical(root, file))) continue;
                                total[0]++;
                                declarationCounts.merge(declaration.declarationRef(),1,Integer::sum);
                                if (retained.size() >= MAX_RETAINED_USE_SITES) continue;
                                MethodDeclaration method = name.findAncestor(MethodDeclaration.class).orElse(null);
                                String symbol = method == null ? "<initializer>" : method.getSignature().asString();
                                String role = name.getParentNode().map(value -> value.getClass().getSimpleName()).orElse("UNKNOWN");
                                String path = logical(root, file);
                                int line = name.getBegin().map(value -> value.line).orElse(-1);
                                String testRef = method != null && method.getAnnotations().stream().anyMatch(a -> a.getNameAsString().equals("Test"))
                                        ? "java-test-v1:sha256:" + digest(path, symbol) : null;
                                String ref = "selector-use-v1:sha256:" + digest(declaration.declarationRef(), path, symbol, role, Integer.toString(line));
                                retained.add(new UseSite(ref, declaration.declarationRef(), path, symbol, role, line, testRef));
                            } catch (RuntimeException failure) { add(unresolved, "SYMBOL:" + logical(root, file)); }
                        }
                    }
                }
            }
            retained.sort(Comparator.comparing(UseSite::declarationRef).thenComparing(UseSite::logicalPath)
                    .thenComparingInt(UseSite::line).thenComparing(UseSite::useSiteRef));
            Coverage coverage = new Coverage(roots.size(), projection.coverage().sourceRootsRequested(), total[0], retained.size(),
                    unresolved.size(), roots.size() == projection.coverage().sourceRootsRequested() && unresolved.isEmpty()
                            && total[0] <= MAX_RETAINED_USE_SITES);
            return new Graph(retained, coverage, declarationCounts, unresolved);
        } catch (IOException failure) { throw new IllegalArgumentException("Cannot analyze selector use sites", failure); }
    }

    private static boolean insideDeclaration(Node node, TrustedSelectorSourceProjection.Declaration declaration, String path) {
        if (!declaration.logicalPath().equals(path) || node.getBegin().isEmpty()) return false;
        var begin=node.getBegin().orElseThrow();var end=node.getEnd().orElse(begin);var range=declaration.range();
        boolean afterStart=begin.line>range.startLine()||begin.line==range.startLine()&&begin.column>=range.startColumn();
        boolean beforeEnd=end.line<range.endLine()||end.line==range.endLine()&&end.column<=range.endColumn();
        return afterStart&&beforeEnd;
    }
    private static void add(List<String> values, String value) { if (values.size() < MAX_UNRESOLVED_ISSUES) values.add(value); }
    private static String logical(Path root, Path file) { return root.relativize(file).toString().replace('\\', '/'); }
    private static String digest(String... fields) {
        try { MessageDigest md = MessageDigest.getInstance("SHA-256"); for (String field : fields) {
            byte[] value = field.getBytes(StandardCharsets.UTF_8); md.update(java.nio.ByteBuffer.allocate(8).putLong(value.length).array()); md.update(value);
        } return HexFormat.of().formatHex(md.digest()); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public record UseSite(String useSiteRef, String declarationRef, String logicalPath, String declaringSymbol,
                          String astRole, int line, String affectedTestRef) { }
    public record Coverage(int sourceRootsParsed, int sourceRootsRequested, int totalKnownUseCount,
                           int retainedUseCount, int unresolvedSymbolIssues, boolean complete) { }
    public record Graph(List<UseSite> useSites, Coverage coverage, Map<String,Integer> declarationUseCounts,List<String> issues) {
        public Graph { useSites = List.copyOf(useSites); declarationUseCounts=Map.copyOf(declarationUseCounts);issues = List.copyOf(issues); }
        public List<UseSite> forDeclaration(String declarationRef) {
            return useSites.stream().filter(value -> value.declarationRef().equals(declarationRef))
                    .limit(MAX_DETAILED_USE_SITES).toList();
        }
        public int countForDeclaration(String declarationRef) {
            return declarationUseCounts.getOrDefault(declarationRef,0);
        }
    }
}
