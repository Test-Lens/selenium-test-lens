package io.github.testlens.selector.tooling;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static io.github.testlens.selector.tooling.SelectorIndexModel.*;

final class JavaLocatorScanner {
    private static final Map<String, String> BY_STRATEGIES = Map.of(
            "id", "id",
            "cssSelector", "css selector",
            "xpath", "xpath",
            "name", "name",
            "className", "class name",
            "tagName", "tag name",
            "linkText", "link text",
            "partialLinkText", "partial link text");
    private static final Set<String> GENERATED_ROOTS = Set.of("generated-sources", "generated-test-sources");

    SelectorIndex scan(ScanRequest request) {
        Path projectRoot = requireProjectRoot(request.projectRoot());
        Limits limits = request.limits();
        if (request.sourceRoots().size() > limits.maxSourceRoots()) {
            throw new IllegalArgumentException("Too many source roots: " + request.sourceRoots().size()
                    + " (limit " + limits.maxSourceRoots() + ")");
        }

        List<Issue> scanIssues = new ArrayList<>();
        List<RootSpec> roots = resolveRoots(projectRoot, request.sourceRoots(), scanIssues);
        SolverSetup solverSetup = solver(request, roots, scanIssues);
        JavaParser parser = parser(request, solverSetup.typeSolver());
        Discovery discovery = discover(projectRoot, roots, limits, scanIssues);
        List<SourceFileIndex> files = new ArrayList<>();
        int symbolIssues = solverSetup.issues();

        for (FileSpec file : discovery.files()) {
            SourceFileIndex indexed = indexFile(projectRoot, file, request, parser);
            files.add(indexed);
            symbolIssues += (int) indexed.issues().stream()
                    .filter(issue -> issue.code().startsWith("SYMBOL_"))
                    .count();
        }
        files.sort(Comparator.comparing(SourceFileIndex::logicalPath));

        Coverage coverage = coverage(request, roots, discovery, files, solverSetup.incompleteClasspath(), symbolIssues);
        List<String> logicalRoots = roots.stream().filter(RootSpec::found).map(RootSpec::logicalPath).sorted().toList();
        ProjectMetadata project = new ProjectMetadata(request.sourceRevision(), logicalRoots, PARSER_VERSION,
                request.languageLevel(), request.encoding().name(), toolFingerprint(request),
                classpathFingerprint(request.classpathEntries()));
        return new SelectorIndex(project, files, coverage, scanIssues);
    }

    private Path requireProjectRoot(Path requested) {
        try {
            Path root = requested.toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) throw new IllegalArgumentException("Project root is not a directory: " + root);
            return root.toRealPath();
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot resolve project root: " + requested, e);
        }
    }

    private List<RootSpec> resolveRoots(Path projectRoot, List<Path> requestedRoots, List<Issue> issues) {
        List<RootSpec> roots = new ArrayList<>();
        for (Path requested : requestedRoots) {
            Path candidate = requested.isAbsolute() ? requested.normalize() : projectRoot.resolve(requested).normalize();
            if (!candidate.startsWith(projectRoot)) {
                throw new IllegalArgumentException("Source root escapes project root: " + requested);
            }
            String logical = logical(projectRoot, candidate);
            if (isGeneratedRoot(candidate)) {
                issues.add(new Issue("GENERATED_ROOT_EXCLUDED", "Generated source root is excluded by default", logical));
                roots.add(new RootSpec(candidate, logical, false, true));
                continue;
            }
            if (!Files.exists(candidate)) {
                issues.add(new Issue("SOURCE_ROOT_MISSING", "Source root does not exist", logical));
                roots.add(new RootSpec(candidate, logical, false, false));
                continue;
            }
            try {
                Path real = candidate.toRealPath();
                if (!real.startsWith(projectRoot)) {
                    throw new IllegalArgumentException("Source root resolves outside project root: " + requested);
                }
                roots.add(new RootSpec(real, logical(projectRoot, real), true, false));
            } catch (IOException e) {
                issues.add(new Issue("SOURCE_ROOT_UNREADABLE", "Source root cannot be resolved", logical));
                roots.add(new RootSpec(candidate, logical, false, false));
            }
        }
        return roots;
    }

    private SolverSetup solver(ScanRequest request, List<RootSpec> roots, List<Issue> issues) {
        CombinedTypeSolver combined = new CombinedTypeSolver();
        combined.add(new ReflectionTypeSolver(false));
        boolean incomplete = false;
        int symbolIssues = 0;
        for (RootSpec root : roots) {
            if (root.found() && !root.generated()) {
                try {
                    combined.add(new JavaParserTypeSolver(root.path()));
                } catch (RuntimeException e) {
                    incomplete = true;
                    symbolIssues++;
                    issues.add(new Issue("SYMBOL_SOURCE_ROOT", "Cannot configure source type solver", root.logicalPath()));
                }
            }
        }
        for (Path entry : request.classpathEntries()) {
            Path candidate = entry.toAbsolutePath().normalize();
            if (!Files.exists(candidate)) {
                incomplete = true;
                symbolIssues++;
                issues.add(new Issue("SYMBOL_CLASSPATH_MISSING", "Classpath entry does not exist", safePath(candidate)));
                continue;
            }
            try {
                if (Files.isDirectory(candidate)) combined.add(new JavaParserTypeSolver(candidate));
                else if (candidate.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                    combined.add(new JarTypeSolver(candidate));
                } else {
                    incomplete = true;
                    symbolIssues++;
                    issues.add(new Issue("SYMBOL_CLASSPATH_UNSUPPORTED", "Classpath entry is not a directory or JAR",
                            safePath(candidate)));
                }
            } catch (IOException | RuntimeException e) {
                incomplete = true;
                symbolIssues++;
                issues.add(new Issue("SYMBOL_CLASSPATH_ERROR", "Cannot configure classpath entry", safePath(candidate)));
            }
        }
        return new SolverSetup(combined, incomplete, symbolIssues);
    }

    private JavaParser parser(ScanRequest request, CombinedTypeSolver solver) {
        if (!"JAVA_17".equals(request.languageLevel())) {
            throw new IllegalArgumentException("S05A2B supports languageLevel JAVA_17 only");
        }
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
                .setCharacterEncoding(request.encoding())
                .setSymbolResolver(new JavaSymbolSolver(solver));
        return new JavaParser(configuration);
    }

    private Discovery discover(Path projectRoot, List<RootSpec> roots, Limits limits, List<Issue> issues) {
        List<FileSpec> files = new ArrayList<>();
        int[] discovered = {0};
        int[] generatedExcluded = {(int) roots.stream().filter(RootSpec::generated).count()};
        int[] unsupportedLanguage = {0};
        boolean[] limitReached = {false};
        for (RootSpec root : roots) {
            if (!root.found()) {
                continue;
            }
            try {
                Files.walkFileTree(root.path(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (Files.isSymbolicLink(dir) || !dir.toRealPath().startsWith(projectRoot)) {
                            issues.add(new Issue("PATH_ESCAPE_BLOCKED", "Directory link resolves outside safe scan tree",
                                    logical(projectRoot, dir)));
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                        if (Set.of(".git", "node_modules", "target").contains(name) && !dir.equals(root.path())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (Files.isSymbolicLink(file) || !file.toRealPath().startsWith(projectRoot)) {
                            issues.add(new Issue("PATH_ESCAPE_BLOCKED", "File link resolves outside project root",
                                    logical(projectRoot, file)));
                            return FileVisitResult.CONTINUE;
                        }
                        String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (!lower.endsWith(".java") && !lower.endsWith(".kt")) return FileVisitResult.CONTINUE;
                        discovered[0]++;
                        if (lower.endsWith(".kt")) unsupportedLanguage[0]++;
                        if (files.size() >= limits.maxFiles()) {
                            limitReached[0] = true;
                            return FileVisitResult.TERMINATE;
                        }
                        files.add(new FileSpec(file, logical(projectRoot, file), lower.endsWith(".java") ? "java" : "kotlin"));
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                issues.add(new Issue("SOURCE_WALK_ERROR", "Source root traversal failed", root.logicalPath()));
            }
            if (limitReached[0]) break;
        }
        if (limitReached[0]) {
            issues.add(new Issue("FILES_LIMIT", "File-count safety limit reached", null));
        }
        files.sort(Comparator.comparing(FileSpec::logicalPath));
        return new Discovery(files, discovered[0], generatedExcluded[0], unsupportedLanguage[0], limitReached[0]);
    }

    private SourceFileIndex indexFile(Path projectRoot, FileSpec file, ScanRequest request, JavaParser parser) {
        List<Issue> issues = new ArrayList<>();
        byte[] bytes;
        try {
            long size = Files.size(file.path());
            if (size > request.limits().maxFileBytes()) {
                issues.add(new Issue("FILE_SIZE_LIMIT", "File exceeds the source-size safety limit", file.logicalPath()));
                return new SourceFileIndex(file.logicalPath(), file.language(), request.encoding().name(), null,
                        false, false, ParseStatus.EXCLUDED, issues, List.of());
            }
            bytes = Files.readAllBytes(file.path());
        } catch (IOException e) {
            issues.add(new Issue("FILE_READ_ERROR", "Source file cannot be read", file.logicalPath()));
            return new SourceFileIndex(file.logicalPath(), file.language(), request.encoding().name(), null,
                    false, false, ParseStatus.FAILED, issues, List.of());
        }
        String hash = "sha256:" + sha256(bytes);
        if (!"java".equals(file.language())) {
            issues.add(new Issue("UNSUPPORTED_LANGUAGE", "Java-only V1 does not parse this source language",
                    file.logicalPath()));
            return new SourceFileIndex(file.logicalPath(), file.language(), request.encoding().name(), hash,
                    false, false, ParseStatus.EXCLUDED, issues, List.of());
        }
        String source;
        try {
            source = request.encoding().newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            issues.add(new Issue("INVALID_ENCODING", "Source bytes are invalid for " + request.encoding().name(),
                    file.logicalPath()));
            return new SourceFileIndex(file.logicalPath(), "java", request.encoding().name(), hash,
                    false, false, ParseStatus.FAILED, issues, List.of());
        }

        ParseResult<CompilationUnit> result = parser.parse(source);
        if (result.getResult().isEmpty() || !result.isSuccessful()) {
            result.getProblems().forEach(problem -> issues.add(new Issue("PARSE_ERROR",
                    bounded(problem.getMessage(), 500), file.logicalPath())));
            return new SourceFileIndex(file.logicalPath(), "java", request.encoding().name(), hash,
                    false, false, ParseStatus.FAILED, issues, List.of());
        }
        CompilationUnit unit = result.getResult().orElseThrow();
        if (astDepth(unit, request.limits().maxAstDepth()) > request.limits().maxAstDepth()) {
            issues.add(new Issue("AST_DEPTH_LIMIT", "AST traversal depth limit exceeded", file.logicalPath()));
            return new SourceFileIndex(file.logicalPath(), "java", request.encoding().name(), hash,
                    false, false, ParseStatus.PARSED_WITH_ISSUES, issues, List.of());
        }

        List<DeclarationRecord> declarations = declarations(unit, source, file.logicalPath(), hash,
                request.limits(), issues);
        declarations.sort(Comparator.comparingInt(value -> value.sourceRange().startOffset()));
        ParseStatus status = issues.isEmpty() ? ParseStatus.PARSED : ParseStatus.PARSED_WITH_ISSUES;
        return new SourceFileIndex(file.logicalPath(), "java", request.encoding().name(), hash,
                false, false, status, issues, declarations);
    }

    private List<DeclarationRecord> declarations(CompilationUnit unit, String source, String logicalPath,
                                                  String contentHash, Limits limits, List<Issue> issues) {
        List<Candidate> candidates = new ArrayList<>();
        Set<MethodCallExpr> ownedCalls = java.util.Collections.newSetFromMap(new IdentityHashMap<>());

        unit.findAll(FieldDeclaration.class).forEach(field -> annotations(field, unit, source, logicalPath,
                limits, issues, candidates));

        unit.findAll(VariableDeclarator.class).forEach(variable -> {
            if (variable.getInitializer().isEmpty() || !looksLikeByType(variable.getTypeAsString())) return;
            Expression initializer = variable.getInitializer().orElseThrow();
            Optional<MethodCallExpr> rootCall = directRootCall(initializer);
            if (rootCall.isEmpty()) return;
            MethodCallExpr call = rootCall.orElseThrow();
            DeclarationKind kind = variable.findAncestor(FieldDeclaration.class).isPresent()
                    ? DeclarationKind.BY_FIELD : DeclarationKind.BY_LOCAL;
            Analysis analysis = analyzeCall(call, unit, limits, issues, logicalPath);
            if (analysis == null) analysis = customAnalysis(call);
            initializer.findAll(MethodCallExpr.class).forEach(ownedCalls::add);
            candidates.add(new Candidate(variable, kind, analysis, declaringSymbol(variable, kind, unit)));
        });

        unit.findAll(MethodDeclaration.class).forEach(method -> {
            if (!looksLikeByType(method.getTypeAsString())) return;
            List<ReturnStmt> returns = method.findAll(ReturnStmt.class);
            List<MethodCallExpr> calls = returns.stream().flatMap(value -> value.getExpression().stream())
                    .flatMap(value -> value.findAll(MethodCallExpr.class).stream())
                    .filter(value -> analyzeCall(value, unit, limits, new ArrayList<>(), logicalPath) != null)
                    .toList();
            if (calls.isEmpty()) return;
            calls.forEach(ownedCalls::add);
            Analysis analysis;
            if (calls.size() == 1) {
                Analysis single = analyzeCall(calls.get(0), unit, limits, issues, logicalPath);
                analysis = single.withKind(ExpressionKind.TEMPLATE);
            } else {
                List<LocatorExpressionChild> children = calls.stream()
                        .map(value -> child(analyzeCall(value, unit, limits, issues, logicalPath)))
                        .toList();
                analysis = new Analysis(new LocatorExpression(ExpressionKind.TEMPLATE, normalize(method),
                        method.getParameters().stream().map(value -> value.getNameAsString()).sorted().toList(), children),
                        null, ResolutionStatus.PARTIALLY_RESOLVED);
            }
            candidates.add(new Candidate(method, DeclarationKind.HELPER_METHOD, analysis,
                    declaringSymbol(method, DeclarationKind.HELPER_METHOD, unit)));
        });

        unit.findAll(MethodCallExpr.class).stream()
                .filter(call -> !ownedCalls.contains(call))
                .forEach(call -> {
                    Analysis analysis = analyzeCall(call, unit, limits, issues, logicalPath);
                    if (analysis != null) {
                        candidates.add(new Candidate(call, DeclarationKind.DIRECT_BY_CALL, analysis,
                                declaringSymbol(call, DeclarationKind.DIRECT_BY_CALL, unit)));
                    } else if (looksLikeKnownCustom(call)) {
                        candidates.add(new Candidate(call, DeclarationKind.CUSTOM, customAnalysis(call),
                                declaringSymbol(call, DeclarationKind.CUSTOM, unit)));
                    }
                });

        candidates.sort(Comparator.comparingInt(value -> range(value.node(), source).startOffset()));
        Map<String, Integer> ordinals = new HashMap<>();
        List<DeclarationRecord> records = new ArrayList<>();
        for (Candidate candidate : candidates) {
            SourceRange sourceRange = range(candidate.node(), source);
            String ordinalKey = candidate.symbol().kind() + "\u0000" + safe(candidate.symbol().qualifiedTypeName())
                    + "\u0000" + safe(candidate.symbol().memberSignature()) + "\u0000" + candidate.kind();
            int ordinal = ordinals.merge(ordinalKey, 1, Integer::sum) - 1;
            String fingerprintInput = canonicalFingerprint(logicalPath, candidate.symbol(), candidate.kind(),
                    candidate.analysis().expression().normalizedExpression(), ordinal);
            String declarationRef = "java-decl-v1:sha256:" + sha256(fingerprintInput.getBytes(StandardCharsets.UTF_8));
            records.add(new DeclarationRecord(SCHEMA_VERSION, declarationRef, "java", logicalPath, sourceRange,
                    candidate.symbol(), candidate.kind(), candidate.analysis().expression(),
                    candidate.analysis().resolved(), candidate.analysis().status(), false, false,
                    "sha256:" + sha256((fingerprintInput + "\u0000" + contentHash).getBytes(StandardCharsets.UTF_8))));
        }
        return records;
    }

    private void annotations(FieldDeclaration field, CompilationUnit unit, String source, String logicalPath,
                             Limits limits, List<Issue> issues, List<Candidate> candidates) {
        for (AnnotationExpr annotation : field.getAnnotations()) {
            AnnotationKind annotationKind = annotationKind(annotation, unit);
            if (annotationKind == null) continue;
            DeclarationKind kind = switch (annotationKind) {
                case FIND_BY -> DeclarationKind.FIND_BY_ANNOTATION;
                case FIND_BYS -> DeclarationKind.FIND_BYS;
                case FIND_ALL -> DeclarationKind.FIND_ALL;
            };
            Analysis analysis = switch (annotationKind) {
                case FIND_BY -> analyzeFindBy(annotation, limits, issues, logicalPath);
                case FIND_BYS -> analyzeCompositeAnnotation(annotation, ExpressionKind.ORDERED_CHAIN,
                        limits, issues, logicalPath);
                case FIND_ALL -> analyzeCompositeAnnotation(annotation, ExpressionKind.ALTERNATIVES,
                        limits, issues, logicalPath);
            };
            candidates.add(new Candidate(annotation, kind, analysis, declaringSymbol(field, kind, unit)));
        }
    }

    private Analysis analyzeCall(MethodCallExpr call, CompilationUnit unit, Limits limits,
                                 List<Issue> issues, String logicalPath) {
        String strategy = BY_STRATEGIES.get(call.getNameAsString());
        ByIdentification identification = strategy == null ? ByIdentification.NO : seleniumByIdentification(call, unit);
        if (identification == ByIdentification.NO) return null;
        if (call.getArguments().size() != 1) {
            issues.add(new Issue("UNSUPPORTED_ARGUMENTS", "Selenium By factory does not have one argument", logicalPath));
            return new Analysis(new LocatorExpression(ExpressionKind.SINGLE, normalize(call), List.of(), List.of()),
                    null, ResolutionStatus.UNSUPPORTED);
        }
        Eval eval = evaluate(call.getArgument(0), limits, 0, new LinkedHashSet<>(), new Counter(), issues, logicalPath);
        ResolutionStatus status = identification == ByIdentification.IMPORT_FALLBACK
                ? ResolutionStatus.PARTIALLY_RESOLVED : eval.known() ? ResolutionStatus.RESOLVED
                : eval.dynamic() ? ResolutionStatus.DYNAMIC : ResolutionStatus.PARTIALLY_RESOLVED;
        if (identification == ByIdentification.IMPORT_FALLBACK) {
            issues.add(new Issue("SYMBOL_IMPORT_FALLBACK",
                    "Selenium By was identified by an unambiguous import because symbol resolution was unavailable",
                    logicalPath));
        }
        LocatorExpression expression = new LocatorExpression(ExpressionKind.SINGLE, normalize(call),
                eval.dependencies().stream().sorted().toList(), List.of());
        return new Analysis(expression, eval.known() ? new ResolvedLocator(strategy, eval.value()) : null, status);
    }

    private Analysis analyzeFindBy(AnnotationExpr annotation, Limits limits, List<Issue> issues, String logicalPath) {
        Map<String, Expression> values = annotationValues(annotation);
        List<String> shortOrder = List.of("className", "css", "id", "linkText", "name",
                "partialLinkText", "tagName", "xpath");
        Map<String, String> strategies = Map.of(
                "className", "class name", "css", "css selector", "id", "id", "linkText", "link text",
                "name", "name", "partialLinkText", "partial link text", "tagName", "tag name", "xpath", "xpath");
        List<Map.Entry<String, Eval>> specified = new ArrayList<>();
        for (String name : shortOrder) {
            Expression value = values.get(name);
            if (value == null) continue;
            Eval eval = evaluate(value, limits, 0, new LinkedHashSet<>(), new Counter(), issues, logicalPath);
            if (!eval.known() || !eval.value().isEmpty()) specified.add(Map.entry(name, eval));
        }
        Expression usingExpr = values.get("using");
        Eval using = usingExpr == null ? Eval.known("")
                : evaluate(usingExpr, limits, 0, new LinkedHashSet<>(), new Counter(), issues, logicalPath);
        if (!using.known() || !using.value().isEmpty()) specified.add(Map.entry("using", using));
        if (specified.size() > 1) {
            issues.add(new Issue("FINDBY_MULTIPLE_STRATEGIES",
                    "@FindBy specifies more than one Selenium location strategy", logicalPath));
            return new Analysis(new LocatorExpression(ExpressionKind.SINGLE, normalize(annotation),
                    dependencies(specified), List.of()), null, ResolutionStatus.ERROR);
        }
        if (specified.isEmpty()) {
            issues.add(new Issue("FINDBY_DEFAULT_ID_OR_NAME",
                    "@FindBy without an explicit strategy uses Selenium's field-name fallback", logicalPath));
            return new Analysis(new LocatorExpression(ExpressionKind.SINGLE, normalize(annotation), List.of(), List.of()),
                    null, ResolutionStatus.PARTIALLY_RESOLVED);
        }
        Map.Entry<String, Eval> selected = specified.get(0);
        String strategy;
        if ("using".equals(selected.getKey())) {
            String how = values.containsKey("how") ? enumName(values.get("how")) : "UNSET";
            strategy = howStrategy(how);
            if (strategy == null) {
                issues.add(new Issue("FINDBY_HOW_UNSUPPORTED", "@FindBy how=" + how
                        + " is not a single standard Selenium locator", logicalPath));
                return new Analysis(new LocatorExpression(ExpressionKind.SINGLE, normalize(annotation),
                        selected.getValue().dependencies().stream().sorted().toList(), List.of()), null,
                        ResolutionStatus.PARTIALLY_RESOLVED);
            }
        } else strategy = strategies.get(selected.getKey());
        Eval eval = selected.getValue();
        ResolutionStatus status = eval.known() ? ResolutionStatus.RESOLVED
                : eval.dynamic() ? ResolutionStatus.DYNAMIC : ResolutionStatus.PARTIALLY_RESOLVED;
        return new Analysis(new LocatorExpression(ExpressionKind.SINGLE, normalize(annotation),
                eval.dependencies().stream().sorted().toList(), List.of()),
                eval.known() ? new ResolvedLocator(strategy, eval.value()) : null, status);
    }

    private Analysis analyzeCompositeAnnotation(AnnotationExpr annotation, ExpressionKind kind, Limits limits,
                                                List<Issue> issues, String logicalPath) {
        List<AnnotationExpr> children = annotationChildren(annotation);
        if (children.isEmpty()) {
            issues.add(new Issue("ANNOTATION_CHILDREN_MISSING", "Composite locator annotation has no @FindBy children",
                    logicalPath));
            return new Analysis(new LocatorExpression(kind, normalize(annotation), List.of(), List.of()), null,
                    ResolutionStatus.ERROR);
        }
        List<Analysis> analyses = children.stream()
                .map(child -> analyzeFindBy(child, limits, issues, logicalPath)).toList();
        ResolutionStatus status = analyses.stream().allMatch(value -> value.status() == ResolutionStatus.RESOLVED)
                ? ResolutionStatus.RESOLVED : ResolutionStatus.PARTIALLY_RESOLVED;
        List<String> dependencies = analyses.stream().flatMap(value -> value.expression().parameterDependencies().stream())
                .distinct().sorted().toList();
        return new Analysis(new LocatorExpression(kind, normalize(annotation), dependencies,
                analyses.stream().map(this::child).toList()), null, status);
    }

    private Eval evaluate(Expression expression, Limits limits, int depth, Set<String> visiting,
                          Counter visited, List<Issue> issues, String logicalPath) {
        if (depth > limits.maxExpressionDepth() || depth > limits.maxConstantDepth()) {
            issues.add(new Issue("EVALUATION_DEPTH_LIMIT", "Constant-expression depth limit reached", logicalPath));
            return Eval.partial(normalize(expression));
        }
        if (++visited.value > limits.maxVisitedSymbols()) {
            issues.add(new Issue("VISITED_SYMBOL_LIMIT", "Constant-symbol visit limit reached", logicalPath));
            return Eval.partial(normalize(expression));
        }
        if (expression instanceof StringLiteralExpr value) return Eval.known(value.asString());
        if (expression instanceof TextBlockLiteralExpr value) return Eval.known(value.asString());
        if (expression instanceof CharLiteralExpr value) return Eval.known(value.asChar() + "");
        if (expression instanceof EnclosedExpr value) {
            return evaluate(value.getInner(), limits, depth + 1, visiting, visited, issues, logicalPath);
        }
        if (expression instanceof BinaryExpr binary && binary.getOperator() == BinaryExpr.Operator.PLUS) {
            Eval left = evaluate(binary.getLeft(), limits, depth + 1, visiting, visited, issues, logicalPath);
            Eval right = evaluate(binary.getRight(), limits, depth + 1, visiting, visited, issues, logicalPath);
            Set<String> deps = union(left.dependencies(), right.dependencies());
            if (left.known() && right.known()) return new Eval(true, left.value() + right.value(), deps, false);
            return new Eval(false, null, deps, left.dynamic() || right.dynamic() || !deps.isEmpty());
        }
        if (expression instanceof NameExpr || expression instanceof FieldAccessExpr) {
            String dependency = normalize(expression);
            try {
                ResolvedValueDeclaration resolved = expression instanceof NameExpr name ? name.resolve()
                        : ((FieldAccessExpr) expression).resolve();
                Optional<VariableDeclarator> variableAst = resolved.toAst(VariableDeclarator.class);
                if (variableAst.isEmpty()) {
                    variableAst = resolved.toAst(FieldDeclaration.class)
                            .flatMap(field -> field.getVariables().stream()
                                    .filter(value -> value.getNameAsString().equals(resolved.getName())).findFirst());
                }
                if (variableAst.isPresent() && isStaticFinalString(variableAst.orElseThrow())
                        && variableAst.orElseThrow().getInitializer().isPresent()) {
                    VariableDeclarator variable = variableAst.orElseThrow();
                    String key = constantKey(variable);
                    if (!visiting.add(key)) {
                        issues.add(new Issue("CONSTANT_CYCLE", "Constant dependency cycle detected", logicalPath));
                        return Eval.partial(dependency);
                    }
                    Eval value = evaluate(variable.getInitializer().orElseThrow(), limits, depth + 1,
                            visiting, visited, issues, logicalPath);
                    visiting.remove(key);
                    return value;
                }
                return Eval.dynamic(dependency);
            } catch (RuntimeException e) {
                issues.add(new Issue("SYMBOL_UNRESOLVED", "Constant symbol could not be resolved", logicalPath));
                return Eval.partial(dependency);
            }
        }
        if (expression.isMethodCallExpr() || expression.isObjectCreationExpr()) return Eval.dynamic(normalize(expression));
        return Eval.dynamic(normalize(expression));
    }

    private boolean isStaticFinalString(VariableDeclarator variable) {
        return variable.getTypeAsString().equals("String")
                && variable.findAncestor(FieldDeclaration.class)
                .map(field -> field.isStatic() && field.isFinal()).orElse(false);
    }

    private ByIdentification seleniumByIdentification(MethodCallExpr call, CompilationUnit unit) {
        try {
            return "org.openqa.selenium.By".equals(call.resolve().declaringType().getQualifiedName())
                    ? ByIdentification.RESOLVED : ByIdentification.NO;
        } catch (RuntimeException ignored) {
            if (!unambiguousByImport(unit)) return ByIdentification.NO;
            if (call.getScope().isPresent()) {
                String scope = call.getScope().orElseThrow().toString();
                return "By".equals(scope) || "org.openqa.selenium.By".equals(scope)
                        ? ByIdentification.IMPORT_FALLBACK : ByIdentification.NO;
            }
            return unit.getImports().stream().anyMatch(value -> value.isStatic()
                    && (value.getNameAsString().equals("org.openqa.selenium.By." + call.getNameAsString())
                    || value.getNameAsString().equals("org.openqa.selenium.By")))
                    ? ByIdentification.IMPORT_FALLBACK : ByIdentification.NO;
        }
    }

    private boolean unambiguousByImport(CompilationUnit unit) {
        boolean selenium = unit.getImports().stream().anyMatch(value -> !value.isStatic()
                && (value.getNameAsString().equals("org.openqa.selenium.By")
                || value.isAsterisk() && value.getNameAsString().equals("org.openqa.selenium")));
        boolean conflicting = unit.getImports().stream().anyMatch(value -> !value.isStatic()
                && value.getNameAsString().endsWith(".By") && !value.getNameAsString().equals("org.openqa.selenium.By"));
        boolean local = unit.getTypes().stream().anyMatch(value -> value.getNameAsString().equals("By"));
        return selenium && !conflicting && !local;
    }

    private boolean looksLikeKnownCustom(MethodCallExpr call) {
        String scope = call.getScope().map(Object::toString).orElse("");
        return scope.endsWith("SemanticBy") || scope.endsWith("CompositeBy");
    }

    private Analysis customAnalysis(MethodCallExpr call) {
        String factorySymbol = null;
        try {
            factorySymbol = call.resolve().getQualifiedSignature();
        } catch (RuntimeException ignored) {
            // An unresolved custom helper remains useful as a conservative CUSTOM declaration.
        }
        return new Analysis(new LocatorExpression(ExpressionKind.CUSTOM, normalize(call), factorySymbol,
                dependencies(call.getArguments().stream().map(value -> Map.entry("arg", Eval.dynamic(normalize(value)))).toList()),
                List.of()), null, ResolutionStatus.CUSTOM);
    }

    private AnnotationKind annotationKind(AnnotationExpr annotation, CompilationUnit unit) {
        String simple = annotation.getName().getIdentifier();
        AnnotationKind kind = switch (simple) {
            case "FindBy" -> AnnotationKind.FIND_BY;
            case "FindBys" -> AnnotationKind.FIND_BYS;
            case "FindAll" -> AnnotationKind.FIND_ALL;
            default -> null;
        };
        if (kind == null) return null;
        try {
            String qn = annotation.resolve().getQualifiedName();
            return qn.equals("org.openqa.selenium.support." + simple) ? kind : null;
        } catch (RuntimeException ignored) {
            boolean correct = unit.getImports().stream().anyMatch(value -> !value.isStatic()
                    && (value.getNameAsString().equals("org.openqa.selenium.support." + simple)
                    || value.isAsterisk() && value.getNameAsString().equals("org.openqa.selenium.support")));
            boolean conflict = unit.getImports().stream().anyMatch(value -> !value.isStatic()
                    && value.getNameAsString().endsWith("." + simple)
                    && !value.getNameAsString().equals("org.openqa.selenium.support." + simple));
            return correct && !conflict ? kind : null;
        }
    }

    private Map<String, Expression> annotationValues(AnnotationExpr annotation) {
        Map<String, Expression> out = new LinkedHashMap<>();
        if (annotation instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) out.put(pair.getNameAsString(), pair.getValue());
        } else if (annotation instanceof SingleMemberAnnotationExpr single) out.put("value", single.getMemberValue());
        return out;
    }

    private List<AnnotationExpr> annotationChildren(AnnotationExpr annotation) {
        Expression value = annotationValues(annotation).get("value");
        if (value instanceof ArrayInitializerExpr array) {
            return array.getValues().stream().filter(Expression::isAnnotationExpr)
                    .map(Expression::asAnnotationExpr).toList();
        }
        return value != null && value.isAnnotationExpr() ? List.of(value.asAnnotationExpr()) : List.of();
    }

    private DeclaringSymbol declaringSymbol(Node node, DeclarationKind kind, CompilationUnit unit) {
        TypeDeclaration<?> enclosingType = enclosingType(node);
        String type = enclosingType == null ? null : enclosingType.getFullyQualifiedName().orElse(null);
        MethodDeclaration method = node.findAncestor(MethodDeclaration.class).orElse(null);
        String member = method != null ? method.getSignature().asString()
                : node.findAncestor(FieldDeclaration.class).flatMap(field -> field.getVariables().stream().findFirst())
                .map(VariableDeclarator::getNameAsString).orElse(null);
        String symbolKind = method != null ? "METHOD" : node.findAncestor(FieldDeclaration.class).isPresent()
                ? "FIELD" : kind == DeclarationKind.BY_LOCAL ? "LOCAL" : "EXPRESSION";
        String scope = kind == DeclarationKind.BY_LOCAL || symbolKind.equals("EXPRESSION")
                ? sha256((safe(type) + "\u0000" + safe(member)).getBytes(StandardCharsets.UTF_8)).substring(0, 16)
                : null;
        return new DeclaringSymbol(symbolKind, type, member, scope);
    }

    private TypeDeclaration<?> enclosingType(Node node) {
        Node current = node.getParentNode().orElse(null);
        while (current != null) {
            if (current instanceof TypeDeclaration<?> declaration) return declaration;
            current = current.getParentNode().orElse(null);
        }
        return null;
    }

    private Optional<MethodCallExpr> directRootCall(Expression expression) {
        Expression current = expression;
        while (current instanceof EnclosedExpr enclosed) current = enclosed.getInner();
        return current instanceof MethodCallExpr call ? Optional.of(call) : Optional.empty();
    }

    private SourceRange range(Node node, String source) {
        com.github.javaparser.Range range = node.getRange().orElseThrow();
        int start = offset(source, range.begin);
        int end = Math.min(source.length(), offset(source, range.end) + 1);
        return new SourceRange(range.begin.line, range.begin.column, range.end.line, range.end.column, start, end);
    }

    private int offset(String source, Position position) {
        int line = 1;
        int index = 0;
        while (line < position.line && index < source.length()) {
            char current = source.charAt(index++);
            if (current == '\r') {
                if (index < source.length() && source.charAt(index) == '\n') index++;
                line++;
            } else if (current == '\n') line++;
        }
        return Math.min(source.length(), index + Math.max(0, position.column - 1));
    }

    private int astDepth(Node root, int stopAfter) {
        return astDepth(root, 0, stopAfter);
    }

    private int astDepth(Node node, int depth, int stopAfter) {
        if (depth > stopAfter) return depth;
        int max = depth;
        for (Node child : node.getChildNodes()) max = Math.max(max, astDepth(child, depth + 1, stopAfter));
        return max;
    }

    private Coverage coverage(ScanRequest request, List<RootSpec> roots, Discovery discovery,
                              List<SourceFileIndex> files, boolean incompleteClasspath, int symbolIssues) {
        List<DeclarationRecord> declarations = files.stream().flatMap(value -> value.declarations().stream()).toList();
        return new Coverage(request.sourceRoots().size(), (int) roots.stream().filter(RootSpec::found).count(),
                discovery.discovered(), (int) files.stream().filter(value -> value.parseStatus() == ParseStatus.PARSED
                        || value.parseStatus() == ParseStatus.PARSED_WITH_ISSUES).count(),
                (int) files.stream().filter(value -> value.parseStatus() == ParseStatus.FAILED).count(),
                (int) files.stream().filter(value -> value.parseStatus() == ParseStatus.EXCLUDED).count(),
                discovery.generatedExcluded(), discovery.unsupportedLanguage(), declarations.size(),
                count(declarations, ResolutionStatus.RESOLVED), count(declarations, ResolutionStatus.PARTIALLY_RESOLVED),
                count(declarations, ResolutionStatus.DYNAMIC), count(declarations, ResolutionStatus.CUSTOM),
                count(declarations, ResolutionStatus.UNSUPPORTED), count(declarations, ResolutionStatus.ERROR),
                incompleteClasspath || symbolIssues > 0, symbolIssues);
    }

    private int count(List<DeclarationRecord> declarations, ResolutionStatus status) {
        return (int) declarations.stream().filter(value -> value.resolutionStatus() == status).count();
    }

    private String canonicalFingerprint(String logicalPath, DeclaringSymbol symbol, DeclarationKind kind,
                                        String expression, int ordinal) {
        List<String> fields = List.of("java", logicalPath, safe(symbol.kind()), safe(symbol.qualifiedTypeName()),
                safe(symbol.memberSignature()), safe(symbol.localScopeFingerprint()), kind.name(), expression,
                Integer.toString(ordinal));
        StringBuilder out = new StringBuilder();
        for (String field : fields) out.append(field.length()).append(':').append(field);
        return out.toString();
    }

    private String normalize(Node node) {
        Node copy = node.clone();
        copy.walk(Node::removeComment);
        return copy.toString().replace("\r\n", "\n").trim();
    }

    private String enumName(Expression expression) {
        if (expression instanceof FieldAccessExpr field) return field.getNameAsString();
        if (expression instanceof NameExpr name) return name.getNameAsString();
        return normalize(expression);
    }

    private String howStrategy(String how) {
        return switch (how) {
            case "CLASS_NAME" -> "class name";
            case "CSS" -> "css selector";
            case "ID" -> "id";
            case "LINK_TEXT" -> "link text";
            case "NAME" -> "name";
            case "PARTIAL_LINK_TEXT" -> "partial link text";
            case "TAG_NAME" -> "tag name";
            case "XPATH" -> "xpath";
            default -> null;
        };
    }

    private List<String> dependencies(List<Map.Entry<String, Eval>> values) {
        return values.stream().flatMap(value -> value.getValue().dependencies().stream()).distinct().sorted().toList();
    }

    private LocatorExpressionChild child(Analysis analysis) {
        return new LocatorExpressionChild(analysis.expression(), analysis.resolved(), analysis.status());
    }

    private Set<String> union(Set<String> left, Set<String> right) {
        Set<String> values = new LinkedHashSet<>(left);
        values.addAll(right);
        return values;
    }

    private boolean looksLikeByType(String type) {
        return type.equals("By") || type.equals("org.openqa.selenium.By") || type.endsWith(".By");
    }

    private boolean isGeneratedRoot(Path path) {
        for (int i = 0; i < path.getNameCount() - 1; i++) {
            if (path.getName(i).toString().equals("target")
                    && GENERATED_ROOTS.contains(path.getName(i + 1).toString())) return true;
        }
        return false;
    }

    private String logical(Path root, Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        if (!absolute.startsWith(root)) return safePath(absolute);
        return root.relativize(absolute).toString().replace('\\', '/');
    }

    private String safePath(Path path) {
        return path.getFileName() == null ? "<classpath>" : path.getFileName().toString();
    }

    private String toolFingerprint(ScanRequest request) {
        String value = SCHEMA_VERSION + "\u0000" + PARSER_VERSION + "\u0000" + request.languageLevel()
                + "\u0000" + request.encoding().name();
        return "sha256:" + sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String classpathFingerprint(List<Path> entries) {
        String joined = entries.stream().map(path -> path.toAbsolutePath().normalize().toString().replace('\\', '/'))
                .sorted().reduce("", (left, right) -> left + right.length() + ":" + right);
        return "sha256:" + sha256(joined.getBytes(StandardCharsets.UTF_8));
    }

    private String constantKey(VariableDeclarator variable) {
        return variable.findCompilationUnit().flatMap(CompilationUnit::getStorage)
                .map(storage -> storage.getPath().toString()).orElse("<memory>") + ":"
                + variable.getRange().map(Object::toString).orElse(variable.getNameAsString());
    }

    private String sha256(byte[] bytes) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String bounded(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max) + "…";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private enum AnnotationKind { FIND_BY, FIND_BYS, FIND_ALL }
    private enum ByIdentification { RESOLVED, IMPORT_FALLBACK, NO }

    private record RootSpec(Path path, String logicalPath, boolean found, boolean generated) { }
    private record FileSpec(Path path, String logicalPath, String language) { }
    private record Discovery(List<FileSpec> files, int discovered, int generatedExcluded,
                             int unsupportedLanguage, boolean limitReached) { }
    private record SolverSetup(CombinedTypeSolver typeSolver, boolean incompleteClasspath, int issues) { }
    private record Candidate(Node node, DeclarationKind kind, Analysis analysis, DeclaringSymbol symbol) { }
    private record Analysis(LocatorExpression expression, ResolvedLocator resolved, ResolutionStatus status) {
        Analysis withKind(ExpressionKind kind) {
            return new Analysis(new LocatorExpression(kind, expression.normalizedExpression(),
                    expression.factorySymbol(), expression.parameterDependencies(), expression.children()), resolved, status);
        }
    }
    private record Eval(boolean known, String value, Set<String> dependencies, boolean dynamic) {
        static Eval known(String value) { return new Eval(true, value, Set.of(), false); }
        static Eval dynamic(String dependency) { return new Eval(false, null, Set.of(dependency), true); }
        static Eval partial(String dependency) { return new Eval(false, null, Set.of(dependency), false); }
    }
    private static final class Counter { int value; }
}
