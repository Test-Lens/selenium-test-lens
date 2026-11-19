package io.github.testlens.application.tooling.ai.workflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, comment- and literal-aware policy validation for complete generated Java test sources.
 * Assertion-failure swallowing is an independent mandatory safety rule. The existing
 * {@link Policy#blockLoops()} option continues to control only retry-loop detection.
 *
 * @since 0.5.0
 */
public final class GeneratedTestPolicyValidator {
    private static final int MAX_EVIDENCE_CHARACTERS = 160;
    private static final int CONTINUES = 1;
    private static final int THROWS = 2;
    private static final int EXITS_WITHOUT_THROWING = 4;
    private static final Set<String> BY_FACTORIES = Set.of("id", "name", "className", "cssSelector", "xpath",
            "tagName", "linkText", "partialLinkText");
    private final Policy policy;

    public GeneratedTestPolicyValidator(Policy policy) { this.policy = policy == null ? Policy.defaults() : policy; }

    public ValidationResult validate(Path relativePath, String source) {
        if (relativePath == null || relativePath.isAbsolute()) throw new IllegalArgumentException("relativePath must be relative");
        if (source == null) throw new IllegalArgumentException("source is required");
        List<Violation> violations = new ArrayList<>();
        String normalizedPath = relativePath.normalize().toString().replace('\\', '/');
        if (normalizedPath.startsWith("../") || normalizedPath.equals("..")) {
            violations.add(new Violation(Rule.FORBIDDEN_PATH, 1, 1, bounded(normalizedPath)));
        }
        for (String forbidden : policy.forbiddenPathPrefixes()) {
            String prefix = forbidden.replace('\\', '/');
            if (normalizedPath.equals(prefix) || normalizedPath.startsWith(prefix.endsWith("/") ? prefix : prefix + "/")) {
                violations.add(new Violation(Rule.FORBIDDEN_PATH, 1, 1, bounded(normalizedPath)));
            }
        }
        if (source.length() > policy.maxCharacters()) {
            violations.add(new Violation(Rule.SIZE_LIMIT, 1, 1, "characters=" + source.length()));
            return new ValidationResult(deduplicate(violations));
        }

        CompilationUnit unit;
        try {
            JavaParser parser = new JavaParser(new ParserConfiguration()
                    .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17));
            ParseResult<CompilationUnit> parsed = parser.parse(source);
            if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
                String evidence = parsed.getProblems().isEmpty()
                        ? "Java source could not be parsed"
                        : parsed.getProblems().get(0).getMessage();
                violations.add(new Violation(Rule.SOURCE_PARSE_FAILED, 1, 1, bounded(evidence)));
                return new ValidationResult(deduplicate(violations));
            }
            unit = parsed.getResult().orElseThrow();
        } catch (RuntimeException failure) {
            violations.add(new Violation(Rule.SOURCE_PARSE_FAILED, 1, 1,
                    bounded("Java source could not be parsed: " + failure.getClass().getSimpleName())));
            return new ValidationResult(deduplicate(violations));
        }

        List<Violation> analysis = new ArrayList<>();
        if (policy.blockRawBy()) detectRawBy(unit, source, analysis);
        if (policy.blockDriverLookup()) detectDriverLookup(unit, source, analysis);
        if (policy.blockJavascript()) detectJavascript(unit, source, analysis);
        if (policy.blockSleeps()) detectSleeps(unit, source, analysis);
        if (policy.blockRetryAnnotations()) detectRetryAnnotations(unit, source, analysis);
        if (policy.blockLoops()) detectRetryLoops(unit, source, analysis);
        detectSwallowedAssertionFailures(unit, source, analysis);
        analysis.sort(Comparator.comparingInt(Violation::line)
                .thenComparingInt(Violation::column)
                .thenComparing(value -> value.rule().ordinal())
                .thenComparing(Violation::evidence));
        violations.addAll(analysis);
        return new ValidationResult(deduplicate(violations));
    }

    public ValidationResult validate(TestEngineeringRequest request, Path relativePath, String source) {
        ValidationResult base = validate(relativePath, source);
        String path = relativePath.normalize().toString().replace('\\', '/');
        boolean allowed = request.allowedPaths().stream().map(value -> value.replace('\\', '/'))
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/"));
        if (allowed) return base;
        List<Violation> violations = new ArrayList<>(base.violations());
        violations.add(new Violation(Rule.OUTSIDE_ALLOWED_PATHS, 1, 1, bounded(path)));
        return new ValidationResult(deduplicate(violations));
    }

    private static void detectRawBy(CompilationUnit unit, String source, List<Violation> violations) {
        for (SimpleName name : unit.findAll(SimpleName.class, candidate -> candidate.asString().equals("By"))) {
            Node owner = name.getParentNode().orElse(null);
            if (owner instanceof NameExpr && isMemberScope(owner)
                    || owner instanceof FieldAccessExpr access && access.getName().equals(name) && isMemberScope(access)) {
                add(violations, Rule.RAW_BY, name, source);
            }
        }
        Set<String> staticallyImportedFactories = unit.getImports().stream()
                .filter(importDeclaration -> importDeclaration.isStatic())
                .filter(importDeclaration -> importDeclaration.getNameAsString().startsWith("org.openqa.selenium.By"))
                .flatMap(importDeclaration -> importDeclaration.isAsterisk()
                        ? BY_FACTORIES.stream()
                        : java.util.stream.Stream.of(importDeclaration.getName().getIdentifier()))
                .filter(BY_FACTORIES::contains)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        unit.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getScope().isEmpty())
                .filter(call -> staticallyImportedFactories.contains(call.getNameAsString()))
                .forEach(call -> add(violations, Rule.RAW_BY, call.getName(), source));
    }

    private static boolean isMemberScope(Node node) {
        Node parent = node.getParentNode().orElse(null);
        return parent instanceof MethodCallExpr call && call.getScope().filter(node::equals).isPresent()
                || parent instanceof FieldAccessExpr access && access.getScope().equals(node)
                || parent instanceof MethodReferenceExpr reference && reference.getScope().equals(node);
    }

    private static void detectDriverLookup(CompilationUnit unit, String source, List<Violation> violations) {
        unit.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getScope().isPresent())
                .filter(call -> Set.of("findElement", "findElements").contains(call.getNameAsString()))
                .forEach(call -> add(violations, Rule.DRIVER_LOOKUP, call.getName(), source));
    }

    private static void detectJavascript(CompilationUnit unit, String source, List<Violation> violations) {
        unit.findAll(SimpleName.class).stream()
                .filter(name -> name.asString().equals("JavascriptExecutor"))
                .forEach(name -> add(violations, Rule.JAVASCRIPT, name, source));
        unit.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getNameAsString().equals("executeScript"))
                .forEach(call -> add(violations, Rule.JAVASCRIPT, call.getName(), source));
    }

    private static void detectSleeps(CompilationUnit unit, String source, List<Violation> violations) {
        boolean staticallyImported = unit.getImports().stream().anyMatch(importDeclaration -> importDeclaration.isStatic()
                && (importDeclaration.getNameAsString().equals("java.lang.Thread.sleep")
                || importDeclaration.isAsterisk() && importDeclaration.getNameAsString().equals("java.lang.Thread")));
        unit.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getNameAsString().equals("sleep"))
                .filter(call -> call.getScope().map(GeneratedTestPolicyValidator::isThreadScope).orElse(staticallyImported))
                .forEach(call -> add(violations, Rule.THREAD_SLEEP, call.getName(), source));
        unit.findAll(MethodReferenceExpr.class).stream()
                .filter(reference -> reference.getIdentifier().equals("sleep"))
                .filter(reference -> isThreadScope(reference.getScope()))
                .forEach(reference -> add(violations, Rule.THREAD_SLEEP, reference, source));
    }

    private static boolean isThreadScope(Node scope) {
        String value = scope.toString();
        return value.equals("Thread") || value.equals("java.lang.Thread");
    }

    private static void detectRetryAnnotations(CompilationUnit unit, String source, List<Violation> violations) {
        unit.findAll(AnnotationExpr.class).stream()
                .filter(annotation -> isRetryAnnotation(annotation.getNameAsString()))
                .forEach(annotation -> add(violations, Rule.RETRY_ANNOTATION, annotation, source));
    }

    private static void detectRetryLoops(CompilationUnit unit, String source, List<Violation> violations) {
        unit.findAll(Statement.class).stream()
                .filter(statement -> statement instanceof ForStmt || statement instanceof ForEachStmt
                        || statement instanceof WhileStmt || statement instanceof DoStmt)
                .filter(GeneratedTestPolicyValidator::containsRetrySignal)
                .forEach(loop -> add(violations, Rule.RETRY_LOOP, loop, source));
    }

    private static boolean containsRetrySignal(Statement loop) {
        boolean namedSignal = loop.findAll(SimpleName.class).stream()
                .map(SimpleName::asString)
                .anyMatch(GeneratedTestPolicyValidator::isRetrySignal);
        return namedSignal || loop.findAll(TryStmt.class).stream()
                .anyMatch(GeneratedTestPolicyValidator::isStructuralRetryAttempt);
    }

    private static boolean isStructuralRetryAttempt(TryStmt attempt) {
        boolean exitsOnSuccess = !attempt.getTryBlock().findAll(ReturnStmt.class).isEmpty()
                || !attempt.getTryBlock().findAll(BreakStmt.class).isEmpty();
        boolean caughtFailureContinuesLoop = attempt.getCatchClauses().stream()
                .anyMatch(catchClause -> flow(catchClause.getBody()) != THROWS);
        return exitsOnSuccess && caughtFailureContinuesLoop;
    }

    private static boolean isRetrySignal(String identifier) {
        String lower = identifier.toLowerCase(Locale.ROOT).replace("_", "");
        return lower.contains("retry") || lower.contains("rerun") || lower.equals("runagain");
    }

    private static void detectSwallowedAssertionFailures(
            CompilationUnit unit, String source, List<Violation> violations) {
        unit.findAll(CatchClause.class).stream()
                .filter(catchClause -> catchesAssertionFailure(catchClause.getParameter().getType()))
                .filter(catchClause -> flow(catchClause.getBody()) != THROWS)
                .forEach(catchClause -> add(violations, Rule.ASSERTION_FAILURE_SWALLOWED, catchClause, source));
    }

    private static boolean catchesAssertionFailure(Type type) {
        if (type instanceof UnionType union) return union.getElements().stream()
                .anyMatch(GeneratedTestPolicyValidator::catchesAssertionFailure);
        if (!type.isClassOrInterfaceType()) return false;
        String name = type.asClassOrInterfaceType().getNameAsString();
        return name.equals("AssertionError") || name.equals("Error") || name.equals("Throwable");
    }

    private static int flow(Statement statement) {
        if (statement instanceof ThrowStmt) return THROWS;
        if (statement instanceof ReturnStmt || statement instanceof BreakStmt || statement instanceof ContinueStmt) {
            return EXITS_WITHOUT_THROWING;
        }
        if (statement instanceof BlockStmt block) {
            int outcomes = CONTINUES;
            for (Statement child : block.getStatements()) {
                if ((outcomes & CONTINUES) == 0) break;
                outcomes = outcomes & ~CONTINUES | flow(child);
            }
            return outcomes;
        }
        if (statement instanceof IfStmt conditional) {
            int otherwise = conditional.getElseStmt().map(GeneratedTestPolicyValidator::flow).orElse(CONTINUES);
            return flow(conditional.getThenStmt()) | otherwise;
        }
        if (statement instanceof SynchronizedStmt synchronizedStatement) {
            return flow(synchronizedStatement.getBody());
        }
        if (statement instanceof TryStmt tryStatement) {
            int outcomes = flow(tryStatement.getTryBlock());
            for (CatchClause catchClause : tryStatement.getCatchClauses()) outcomes |= flow(catchClause.getBody());
            if (tryStatement.getFinallyBlock().isEmpty()) return outcomes;
            int finallyOutcomes = flow(tryStatement.getFinallyBlock().orElseThrow());
            int result = finallyOutcomes & ~CONTINUES;
            if ((finallyOutcomes & CONTINUES) != 0) result |= outcomes;
            return result;
        }
        if (statement.isExpressionStmt() || statement.isLocalClassDeclarationStmt()
                || statement.isExplicitConstructorInvocationStmt() || statement.isEmptyStmt()
                || statement.isAssertStmt()) return CONTINUES;
        return CONTINUES | EXITS_WITHOUT_THROWING;
    }

    private static void add(List<Violation> violations, Rule rule, Node node, String source) {
        Position begin = node.getBegin().orElse(new Position(1, 1));
        violations.add(new Violation(rule, begin.line, begin.column, evidence(node, source)));
    }

    private static String evidence(Node node, String source) {
        if (node.getRange().isEmpty()) return bounded(node.toString());
        int line = node.getRange().orElseThrow().begin.line;
        String[] lines = source.split("\\R", -1);
        return line > 0 && line <= lines.length ? bounded(lines[line - 1].strip()) : bounded(node.toString());
    }

    private static String bounded(String evidence) {
        String singleLine = evidence == null ? "" : evidence.replace('\n', ' ').replace('\r', ' ').strip();
        return singleLine.length() <= MAX_EVIDENCE_CHARACTERS
                ? singleLine : singleLine.substring(0, MAX_EVIDENCE_CHARACTERS);
    }

    private static List<Violation> deduplicate(List<Violation> violations) {
        Map<ViolationKey, Violation> unique = new LinkedHashMap<>();
        for (Violation violation : violations) {
            unique.putIfAbsent(new ViolationKey(violation.rule(), violation.line(), violation.column()), violation);
        }
        return List.copyOf(unique.values());
    }

    private static boolean isRetryAnnotation(String token) {
        String simpleName = token.substring(token.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return simpleName.contains("retry") || simpleName.equals("repeatedtest");
    }

    public record Policy(List<String> forbiddenPathPrefixes, int maxCharacters, boolean blockRawBy,
                         boolean blockDriverLookup, boolean blockSleeps, boolean blockJavascript,
                         boolean blockRetryAnnotations, boolean blockLoops) {
        public Policy {
            forbiddenPathPrefixes = List.copyOf(forbiddenPathPrefixes == null ? List.of() : forbiddenPathPrefixes);
            if (maxCharacters < 1 || maxCharacters > 4_000_000) throw new IllegalArgumentException("invalid maxCharacters");
        }
        public static Policy defaults() {
            return new Policy(List.of("src/main", "pom.xml", ".github"), 500_000, true, true, true, true, true, true);
        }
    }

    public record ValidationResult(List<Violation> violations) {
        public ValidationResult { violations = List.copyOf(violations); }
        public boolean accepted() { return violations.isEmpty(); }
    }
    public record Violation(Rule rule, int line, int column, String evidence) { }
    public enum Rule { RAW_BY, DRIVER_LOOKUP, THREAD_SLEEP, JAVASCRIPT, RETRY_ANNOTATION, RETRY_LOOP,
        ASSERTION_FAILURE_SWALLOWED, FORBIDDEN_PATH, OUTSIDE_ALLOWED_PATHS, SIZE_LIMIT, SOURCE_PARSE_FAILED }
    private record ViolationKey(Rule rule, int line, int column) { }
}
