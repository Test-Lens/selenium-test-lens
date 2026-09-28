package io.github.testlens.selector.tooling;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Narrow, local-only projection of selector source facts for trusted migration tooling.
 * It intentionally exposes neither source text nor JavaParser nodes.
 */
public final class TrustedSelectorSourceProjection {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_DETAILED_USE_SITES = 256;

    private TrustedSelectorSourceProjection() { }

    public static Snapshot scan(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries) {
        return scan(new SelectorIndexModel.ScanRequest(projectRoot, sourceRoots, classpathEntries));
    }

    static Snapshot scan(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries, int maxFiles) {
        var defaults = SelectorIndexModel.Limits.defaults();
        var limits = new SelectorIndexModel.Limits(defaults.maxFileBytes(), maxFiles, defaults.maxSourceRoots(),
                defaults.maxAstDepth(), defaults.maxExpressionDepth(), defaults.maxConstantDepth(), defaults.maxVisitedSymbols());
        return scan(new SelectorIndexModel.ScanRequest(projectRoot, sourceRoots, classpathEntries, "JAVA_17",
                StandardCharsets.UTF_8, null, limits));
    }

    private static Snapshot scan(SelectorIndexModel.ScanRequest request) {
        var index = new JavaLocatorScanner().scan(request);
        List<Declaration> declarations = new ArrayList<>();
        for (var file : index.files()) {
            for (var value : file.declarations()) {
                ExpressionShape shape = shape(value);
                String strategy = value.resolvedLocator() == null ? null : value.resolvedLocator().strategy();
                String scalar = value.resolvedLocator() == null ? null : value.resolvedLocator().value();
                var symbol = value.declaringSymbol();
                String declaringSymbol = String.join("#", safe(symbol.qualifiedTypeName()),
                        safe(symbol.memberSignature()), safe(symbol.localScopeFingerprint()));
                String construct = "selector-source-construct-v1:sha256:" + digest("selector-source-construct-v1",
                        value.declarationRef(), value.declarationKind().name(), value.locatorExpression().normalizedExpression(),
                        declaringSymbol);
                List<String> limitations = new ArrayList<>();
                if (value.resolutionStatus() != SelectorIndexModel.ResolutionStatus.RESOLVED) {
                    limitations.add("SOURCE_" + value.resolutionStatus().name());
                }
                SourceRange patchRange = exactCallRange(request.projectRoot(), value);
                Encoding fileEncoding = hasBom(request.projectRoot().resolve(value.logicalPath())) ? Encoding.UTF8_BOM : Encoding.UTF8;
                declarations.add(new Declaration(value.declarationRef(), null, value.logicalPath(), file.contentHash(),
                        fileEncoding, patchRange, DeclarationKind.valueOf(value.declarationKind().name()),
                        strategy, scalar, shape, construct, declaringSymbol,
                        Freshness.CURRENT,
                        limitations));
            }
        }
        declarations.sort(Comparator.comparing(Declaration::logicalPath)
                .thenComparingInt(value -> value.range().startOffset()));
        List<String> issues = new ArrayList<>();
        index.issues().forEach(value -> issues.add(value.code()));
        index.files().forEach(file -> file.issues().forEach(value -> issues.add(value.code())));
        boolean fileLimitReached = index.issues().stream().anyMatch(value -> value.code().equals("FILES_LIMIT"));
        Coverage coverage = new Coverage(index.coverage().sourceRootsRequested(), index.coverage().sourceRootsFound(),
                index.coverage().filesDiscovered(), index.coverage().filesParsed(),
                index.coverage().symbolResolutionIssues(), !index.coverage().incompleteClasspath()
                        && index.coverage().filesFailed() == 0 && index.coverage().filesExcluded() == 0
                        && index.coverage().sourceRootsFound() == index.coverage().sourceRootsRequested()
                        && !fileLimitReached);
        return new Snapshot(SCHEMA_VERSION, declarations, coverage, issues.stream().distinct().sorted().toList());
    }

    private static ExpressionShape shape(SelectorIndexModel.DeclarationRecord value) {
        if (value.declarationKind() == SelectorIndexModel.DeclarationKind.FIND_BY_ANNOTATION
                || value.declarationKind() == SelectorIndexModel.DeclarationKind.FIND_BYS
                || value.declarationKind() == SelectorIndexModel.DeclarationKind.FIND_ALL) return ExpressionShape.ANNOTATION;
        String normalized = value.locatorExpression().normalizedExpression();
        if (normalized.contains("+")) return ExpressionShape.CONCATENATED;
        List<String> dependencies = value.locatorExpression().parameterDependencies();
        if (!dependencies.isEmpty()) {
            return ExpressionShape.PARAMETERIZED;
        }
        if (value.declarationKind() == SelectorIndexModel.DeclarationKind.HELPER_METHOD
                || value.declarationKind() == SelectorIndexModel.DeclarationKind.CUSTOM) return ExpressionShape.HELPER_GENERATED;
        if (normalized.matches("(?s).*\\([^\"']*[A-Za-z_$][A-Za-z0-9_$.]*\\).*")) return ExpressionShape.CONSTANT_BACKED;
        if (value.resolutionStatus() == SelectorIndexModel.ResolutionStatus.DYNAMIC) return ExpressionShape.DYNAMIC;
        if (value.resolutionStatus() == SelectorIndexModel.ResolutionStatus.UNSUPPORTED) return ExpressionShape.UNSUPPORTED;
        return ExpressionShape.DIRECT_LITERAL;
    }

    private static SourceRange exactCallRange(Path projectRoot, SelectorIndexModel.DeclarationRecord value) {
        if (value.declarationKind() == SelectorIndexModel.DeclarationKind.FIND_BY_ANNOTATION
                || value.declarationKind() == SelectorIndexModel.DeclarationKind.FIND_BYS
                || value.declarationKind() == SelectorIndexModel.DeclarationKind.FIND_ALL
                || value.declarationKind() == SelectorIndexModel.DeclarationKind.HELPER_METHOD) return convert(value.sourceRange());
        try {
            String source = java.nio.file.Files.readString(projectRoot.resolve(value.logicalPath()), StandardCharsets.UTF_8);
            if(source.startsWith("\uFEFF"))source=source.substring(1);
            final String currentSource=source;
            var parsed = new com.github.javaparser.JavaParser(new com.github.javaparser.ParserConfiguration()
                    .setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17)).parse(currentSource);
            if (parsed.getResult().isEmpty()) return convert(value.sourceRange());
            return parsed.getResult().orElseThrow().findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).stream()
                    .filter(call -> call.getRange().isPresent())
                    .map(call -> range(currentSource, call.getRange().orElseThrow()))
                    .filter(range -> range.startOffset() >= value.sourceRange().startOffset()
                            && range.endOffsetExclusive() <= value.sourceRange().endOffsetExclusive())
                    .filter(range -> currentSource.substring(range.startOffset(), range.endOffsetExclusive()).replaceAll("\\s+", "")
                            .equals(value.locatorExpression().normalizedExpression().replaceAll("\\s+", "")))
                    .findFirst().orElse(convert(value.sourceRange()));
        } catch (Exception ignored) { return convert(value.sourceRange()); }
    }
    private static boolean hasBom(Path path){try{byte[]b=java.nio.file.Files.readAllBytes(path);return b.length>=3&&(b[0]&255)==0xef&&(b[1]&255)==0xbb&&(b[2]&255)==0xbf;}catch(java.io.IOException e){return false;}}
    private static SourceRange convert(SelectorIndexModel.SourceRange value) { return new SourceRange(value.startLine(),value.startColumn(),value.endLine(),value.endColumn(),value.startOffset(),value.endOffsetExclusive()); }
    private static SourceRange range(String source, com.github.javaparser.Range value) {
        int start=offset(source,value.begin),end=Math.min(source.length(),offset(source,value.end)+1);
        return new SourceRange(value.begin.line,value.begin.column,value.end.line,value.end.column,start,end);
    }
    private static int offset(String source, com.github.javaparser.Position position) { int line=1,index=0;while(line<position.line&&index<source.length()){char c=source.charAt(index++);if(c=='\r'){if(index<source.length()&&source.charAt(index)=='\n')index++;line++;}else if(c=='\n')line++;}return Math.min(source.length(),index+Math.max(0,position.column-1)); }

    private static String digest(String domain, String... fields) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            put(md, domain);
            for (String field : fields) put(md, safe(field));
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static void put(MessageDigest md, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        md.update(java.nio.ByteBuffer.allocate(8).putLong(bytes.length).array());
        md.update(bytes);
    }
    private static String safe(String value) { return value == null ? "" : value; }

    public enum Encoding { UTF8, UTF8_BOM }
    public enum Freshness { CURRENT, CURRENT_WITH_LIMITATIONS, STALE, UNKNOWN }
    public enum ExpressionShape { DIRECT_LITERAL, CONSTANT_BACKED, PARAMETERIZED, CONCATENATED, HELPER_GENERATED, ANNOTATION, CUSTOM, DYNAMIC, UNSUPPORTED }
    public enum DeclarationKind { DIRECT_BY_CALL, BY_FIELD, BY_LOCAL, FIND_BY_ANNOTATION, FIND_BYS, FIND_ALL, HELPER_METHOD, CUSTOM }

    public record SourceRange(int startLine, int startColumn, int endLine, int endColumn,
                              int startOffset, int endOffsetExclusive) {
        public SourceRange { if (startOffset < 0 || endOffsetExclusive < startOffset) throw new IllegalArgumentException("range"); }
    }
    public record Declaration(String declarationRef, String componentRef, String logicalPath, String fileSha256,
                              Encoding encoding, SourceRange range, DeclarationKind declarationKind,
                              String locatorStrategy, String canonicalScalarValue, ExpressionShape expressionShape,
                              String constructIdentityDigest, String declaringSymbol, Freshness freshness,
                              List<String> limitations) {
        public Declaration {
            Objects.requireNonNull(declarationRef); Objects.requireNonNull(logicalPath); Objects.requireNonNull(fileSha256);
            Objects.requireNonNull(encoding); Objects.requireNonNull(range); Objects.requireNonNull(declarationKind);
            Objects.requireNonNull(expressionShape); Objects.requireNonNull(constructIdentityDigest);
            Objects.requireNonNull(declaringSymbol); Objects.requireNonNull(freshness);
            limitations = List.copyOf(limitations == null ? List.of() : limitations);
        }
    }
    public record Coverage(int sourceRootsRequested, int sourceRootsFound, int filesDiscovered, int filesParsed,
                           int unresolvedSymbols, boolean complete) { }
    public record Snapshot(int schemaVersion, List<Declaration> declarations, Coverage coverage, List<String> limitations) {
        public Snapshot {
            if (schemaVersion != SCHEMA_VERSION) throw new IllegalArgumentException("schemaVersion");
            declarations = List.copyOf(declarations); limitations = List.copyOf(limitations);
        }
        public Declaration declaration(String ref) {
            return declarations.stream().filter(value -> value.declarationRef().equals(ref)).findFirst().orElse(null);
        }
    }
}
