package io.github.testlens.selector.tooling;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class SelectorIndexModel {
    static final int SCHEMA_VERSION = 1;
    static final String GENERATOR_NAME = "Test Lens selector tooling";
    static final String GENERATOR_VERSION = "0.4.0";
    static final String PARSER_VERSION = "3.28.2";

    private SelectorIndexModel() {
    }

    enum DeclarationKind {
        DIRECT_BY_CALL, BY_FIELD, BY_LOCAL, FIND_BY_ANNOTATION, FIND_BYS, FIND_ALL, HELPER_METHOD, CUSTOM
    }

    enum ExpressionKind {
        SINGLE, ORDERED_CHAIN, ALTERNATIVES, TEMPLATE, CUSTOM
    }

    enum ResolutionStatus {
        RESOLVED, PARTIALLY_RESOLVED, DYNAMIC, CUSTOM, UNSUPPORTED, ERROR
    }

    enum ParseStatus {
        PARSED, PARSED_WITH_ISSUES, FAILED, EXCLUDED
    }

    record Limits(long maxFileBytes, int maxFiles, int maxSourceRoots, int maxAstDepth,
                  int maxExpressionDepth, int maxConstantDepth, int maxVisitedSymbols) {
        static Limits defaults() {
            return new Limits(4L * 1024L * 1024L, 20_000, 64, 512, 64, 32, 4_096);
        }

        Limits {
            if (maxFileBytes <= 0 || maxFiles <= 0 || maxSourceRoots <= 0 || maxAstDepth <= 0
                    || maxExpressionDepth <= 0 || maxConstantDepth <= 0 || maxVisitedSymbols <= 0) {
                throw new IllegalArgumentException("Selector tooling limits must be positive");
            }
        }
    }

    record ScanRequest(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries,
                       String languageLevel, Charset encoding, String sourceRevision, Limits limits) {
        ScanRequest(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries) {
            this(projectRoot, sourceRoots, classpathEntries, "JAVA_17", Charset.forName("UTF-8"), null,
                    Limits.defaults());
        }

        ScanRequest {
            Objects.requireNonNull(projectRoot, "projectRoot");
            sourceRoots = List.copyOf(Objects.requireNonNull(sourceRoots, "sourceRoots"));
            classpathEntries = List.copyOf(Objects.requireNonNull(classpathEntries, "classpathEntries"));
            Objects.requireNonNull(languageLevel, "languageLevel");
            Objects.requireNonNull(encoding, "encoding");
            Objects.requireNonNull(limits, "limits");
        }
    }

    record SelectorIndex(ProjectMetadata project, List<SourceFileIndex> files, Coverage coverage,
                         List<Issue> issues) {
        SelectorIndex {
            files = List.copyOf(files);
            issues = List.copyOf(issues);
        }
    }

    record ProjectMetadata(String sourceRevision, List<String> sourceRoots, String parserVersion,
                           String languageLevel, String encoding, String toolConfigFingerprint,
                           String classpathFingerprint) {
        ProjectMetadata {
            sourceRoots = List.copyOf(sourceRoots);
        }
    }

    record SourceFileIndex(String logicalPath, String sourceLanguage, String encoding, String contentHash,
                           boolean generated, boolean readOnly, ParseStatus parseStatus, List<Issue> issues,
                           List<DeclarationRecord> declarations) {
        SourceFileIndex {
            issues = List.copyOf(issues);
            declarations = List.copyOf(declarations);
        }
    }

    record DeclarationRecord(int schemaVersion, String declarationRef, String sourceLanguage,
                             String logicalPath, SourceRange sourceRange, DeclaringSymbol declaringSymbol,
                             DeclarationKind declarationKind, LocatorExpression locatorExpression,
                             ResolvedLocator resolvedLocator, ResolutionStatus resolutionStatus,
                             boolean generated, boolean readOnly, String contentFingerprint) {
    }

    record SourceRange(int startLine, int startColumn, int endLine, int endColumn,
                       int startOffset, int endOffsetExclusive) {
    }

    record DeclaringSymbol(String kind, String qualifiedTypeName, String memberSignature,
                           String localScopeFingerprint) {
    }

    record LocatorExpression(ExpressionKind kind, String normalizedExpression, String factorySymbol,
                             List<String> parameterDependencies, List<LocatorExpressionChild> children) {
        LocatorExpression(ExpressionKind kind, String normalizedExpression,
                          List<String> parameterDependencies, List<LocatorExpressionChild> children) {
            this(kind, normalizedExpression, null, parameterDependencies, children);
        }

        LocatorExpression {
            parameterDependencies = List.copyOf(parameterDependencies);
            children = List.copyOf(children);
        }
    }

    record LocatorExpressionChild(LocatorExpression expression, ResolvedLocator resolvedLocator,
                                  ResolutionStatus resolutionStatus) {
    }

    record ResolvedLocator(String strategy, String value) {
    }

    record Issue(String code, String message, String logicalPath) {
    }

    record Coverage(int sourceRootsRequested, int sourceRootsFound, int filesDiscovered, int filesParsed,
                    int filesFailed, int filesExcluded, int generatedFilesExcluded,
                    int unsupportedLanguageFiles, int declarationsFound, int resolved,
                    int partiallyResolved, int dynamic, int custom, int unsupported, int errors,
                    boolean incompleteClasspath, int symbolResolutionIssues) {
    }
}
