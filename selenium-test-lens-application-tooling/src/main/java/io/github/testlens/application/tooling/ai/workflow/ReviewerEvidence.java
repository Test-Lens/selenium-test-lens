package io.github.testlens.application.tooling.ai.workflow;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic, bounded evidence supplied to code review. @since 0.5.0 */
public final class ReviewerEvidence {
    private ReviewerEvidence() { }

    public static QualityEvidence analyze(TestImplementationProposal implementation, String sourceFingerprint) {
        String source = implementation.sourcePatch();
        String digest = ArtifactEnvelope.digest(source.getBytes(StandardCharsets.UTF_8));
        var parsed = new JavaParser(new ParserConfiguration()).parse(source);
        if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
            return new QualityEvidence(QualityStatus.PARTIAL, 0, 0, RequiredApiAssertionStatus.UNRESOLVED,
                    ControlPathStatus.UNRESOLVED, SwallowedFailureStatus.UNRESOLVED, digest,
                    normalizedFingerprint(sourceFingerprint));
        }
        CompilationUnit unit = parsed.getResult().orElseThrow();
        List<MethodDeclaration> tests = unit.findAll(MethodDeclaration.class).stream()
                .filter(ReviewerEvidence::isJunitTest).toList();
        Set<ApiKey> requiredMethods = referencedMethods(implementation.pageObjectApisUsed());
        int assertions = 0;
        Set<ApiKey> assertedMethods = new HashSet<>();
        for (MethodDeclaration test : tests) {
            for (MethodCallExpr call : test.findAll(MethodCallExpr.class)) {
                if (!isJunitAssertion(unit, call)) continue;
                assertions++;
                for (Expression valueArgument : assertedValueArguments(call)) {
                    for (MethodCallExpr nested : valueArgument.findAll(MethodCallExpr.class)) {
                        receiverType(unit, nested).ifPresent(owner -> {
                            ApiKey key = new ApiKey(owner, nested.getNameAsString(),
                                    nested.getArguments().size());
                            if (requiredMethods.contains(key)) assertedMethods.add(key);
                        });
                    }
                }
            }
        }
        boolean earlyReturn = tests.stream().anyMatch(test -> !test.findAll(ReturnStmt.class).isEmpty());
        boolean swallowed = tests.stream().flatMap(test -> test.findAll(CatchClause.class).stream())
                .anyMatch(clause -> swallowsFailure(unit, clause));
        QualityStatus status = tests.isEmpty() ? QualityStatus.PARTIAL : QualityStatus.COMPLETE;
        RequiredApiAssertionStatus apiStatus = requiredMethods.isEmpty()
                ? RequiredApiAssertionStatus.NOT_APPLICABLE
                : assertedMethods.containsAll(requiredMethods)
                ? RequiredApiAssertionStatus.VERIFIED : RequiredApiAssertionStatus.NOT_VERIFIED;
        return new QualityEvidence(status, tests.size(), assertions, apiStatus,
                earlyReturn ? ControlPathStatus.EARLY_RETURN_PRESENT : ControlPathStatus.UNCONDITIONAL,
                swallowed ? SwallowedFailureStatus.PRESENT : SwallowedFailureStatus.NONE,
                digest, normalizedFingerprint(sourceFingerprint));
    }

    private static boolean isJunitTest(MethodDeclaration method) {
        return method.getAnnotations().stream().anyMatch(annotation -> {
            String name = annotation.getNameAsString();
            return name.equals("Test") || name.equals("org.junit.jupiter.api.Test");
        });
    }

    private static final Set<String> JUNIT_ASSERTIONS = Set.of(
            "assertTrue", "assertFalse", "assertEquals", "assertNotEquals", "assertNull", "assertNotNull",
            "assertSame", "assertNotSame", "assertThrows", "assertDoesNotThrow", "assertInstanceOf", "fail");

    private static boolean isJunitAssertion(CompilationUnit unit, MethodCallExpr call) {
        String name = call.getNameAsString();
        if (!JUNIT_ASSERTIONS.contains(name)) return false;
        if (call.findAncestor(TypeDeclaration.class).stream()
                .flatMap(type -> ((TypeDeclaration<?>) type).getMethods().stream())
                .anyMatch(method -> method.getNameAsString().equals(name)
                        && method.getParameters().size() == call.getArguments().size())) return false;
        if (call.getScope().isPresent()) {
            String scope = call.getScope().orElseThrow().toString();
            if (scope.equals("org.junit.jupiter.api.Assertions")) return true;
            return scope.equals("Assertions") && unit.getImports().stream().anyMatch(imported -> !imported.isStatic()
                    && imported.getNameAsString().equals("org.junit.jupiter.api.Assertions"));
        }
        return unit.getImports().stream().anyMatch(imported -> imported.isStatic()
                && imported.getNameAsString().equals("org.junit.jupiter.api.Assertions")
                && imported.isAsterisk()
                || imported.isStatic() && imported.getNameAsString()
                .equals("org.junit.jupiter.api.Assertions." + name));
    }

    private static List<Expression> assertedValueArguments(MethodCallExpr call) {
        int size = call.getArguments().size();
        return switch (call.getNameAsString()) {
            case "assertTrue", "assertFalse", "assertNull", "assertNotNull" ->
                    size >= 1 ? List.of(call.getArgument(0)) : List.of();
            case "assertEquals", "assertNotEquals", "assertSame", "assertNotSame" ->
                    size >= 2 ? List.of(call.getArgument(0), call.getArgument(1)) : List.of();
            case "assertInstanceOf" -> size >= 2 ? List.of(call.getArgument(1)) : List.of();
            case "assertThrows" -> size >= 2 ? List.of(call.getArgument(1)) : List.of();
            case "assertDoesNotThrow" -> size >= 1 ? List.of(call.getArgument(0)) : List.of();
            default -> List.of();
        };
    }

    private static boolean swallowsFailure(CompilationUnit unit, CatchClause clause) {
        if (clause.getBody().isEmpty()) return true;
        return clause.getBody().findAll(MethodCallExpr.class).stream().noneMatch(call -> isJunitAssertion(unit, call))
                && clause.getBody().findAll(com.github.javaparser.ast.stmt.ThrowStmt.class).isEmpty();
    }

    private static Set<ApiKey> referencedMethods(List<String> references) {
        var counts = new HashMap<ApiKey, Integer>();
        for (String reference : references == null ? List.<String>of() : references) {
            try {
                var parsed = CanonicalPageObjectApiReference.parse(reference);
                counts.merge(new ApiKey(parsed.owner(), parsed.method(), parsed.parameterTypes().size()),
                        1, Integer::sum);
            } catch (IllegalArgumentException ignored) {
                return Set.of(new ApiKey("<invalid>", "<invalid>", -1));
            }
        }
        if (counts.values().stream().anyMatch(count -> count > 1)) {
            return Set.of(new ApiKey("<ambiguous>", "<ambiguous>", -1));
        }
        return Set.copyOf(counts.keySet());
    }

    private static java.util.Optional<String> receiverType(CompilationUnit unit, MethodCallExpr call) {
        if (call.getScope().isEmpty()) return java.util.Optional.empty();
        Expression scope = call.getScope().orElseThrow();
        if (scope instanceof ObjectCreationExpr creation) {
            return qualifiedType(unit, creation.getType().asString());
        }
        String variable = null;
        if (scope instanceof NameExpr name) variable = name.getNameAsString();
        else if (scope instanceof FieldAccessExpr field && field.getScope().isThisExpr()) {
            variable = field.getNameAsString();
        }
        if (variable == null) return java.util.Optional.empty();
        Set<String> declaredTypes = new HashSet<>();
        for (VariableDeclarator declaration : unit.findAll(VariableDeclarator.class)) {
            if (declaration.getNameAsString().equals(variable)) declaredTypes.add(declaration.getType().asString());
        }
        for (com.github.javaparser.ast.body.Parameter parameter
                : unit.findAll(com.github.javaparser.ast.body.Parameter.class)) {
            if (parameter.getNameAsString().equals(variable)) declaredTypes.add(parameter.getType().asString());
        }
        if (declaredTypes.size() == 1) return qualifiedType(unit, declaredTypes.iterator().next());
        if (declaredTypes.isEmpty() && Character.isUpperCase(variable.charAt(0))) {
            return qualifiedType(unit, variable);
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<String> qualifiedType(CompilationUnit unit, String type) {
        String value = type;
        int generic = value.indexOf('<');
        if (generic >= 0) value = value.substring(0, generic);
        String normalized = value.replace("[]", "");
        List<String> declared = unit.findAll(TypeDeclaration.class).stream()
                .map(typeDeclaration -> ((TypeDeclaration<?>) typeDeclaration).getFullyQualifiedName().orElse(""))
                .filter(name -> !name.isBlank() && (name.endsWith("." + normalized)
                        || name.substring(name.lastIndexOf('.') + 1).equals(normalized)))
                .distinct().toList();
        if (declared.size() == 1) return java.util.Optional.of(declared.get(0));
        if (declared.size() > 1) return java.util.Optional.empty();
        String first = normalized.substring(0, normalized.indexOf('.') < 0
                ? normalized.length() : normalized.indexOf('.'));
        for (var imported : unit.getImports()) {
            if (!imported.isStatic() && !imported.isAsterisk()
                    && imported.getName().getIdentifier().equals(first)) {
                String suffix = normalized.length() == first.length() ? "" : normalized.substring(first.length());
                return java.util.Optional.of(imported.getNameAsString() + suffix);
            }
        }
        if (!normalized.isEmpty() && Character.isLowerCase(normalized.charAt(0)) && normalized.contains(".")) {
            return java.util.Optional.of(normalized);
        }
        if (normalized.contains(".")) {
            return unit.getPackageDeclaration().map(declaration -> declaration.getNameAsString() + "." + normalized);
        }
        return unit.getPackageDeclaration().map(declaration -> declaration.getNameAsString() + "." + normalized);
    }

    private record ApiKey(String owner, String name, int arity) { }

    private static String normalizedFingerprint(String value) { return value == null ? "" : value; }

    public record QualityEvidence(QualityStatus status, int junitTestMethodCount, int assertionCount,
                                  RequiredApiAssertionStatus requiredApiInsideAssertion,
                                  ControlPathStatus controlPath, SwallowedFailureStatus swallowedFailures,
                                  String implementationDigest, String sourceFingerprint) {
        public QualityEvidence {
            if (status == null || junitTestMethodCount < 0 || assertionCount < 0
                    || requiredApiInsideAssertion == null || controlPath == null || swallowedFailures == null
                    || implementationDigest == null || sourceFingerprint == null) {
                throw new IllegalArgumentException("complete bounded quality evidence is required");
            }
        }
        public boolean completeForApproval() {
            return status == QualityStatus.COMPLETE && junitTestMethodCount > 0 && assertionCount > 0
                    && requiredApiInsideAssertion == RequiredApiAssertionStatus.VERIFIED
                    && controlPath == ControlPathStatus.UNCONDITIONAL
                    && swallowedFailures == SwallowedFailureStatus.NONE
                    && !sourceFingerprint.isBlank();
        }

        /** Analysis is determinate enough for a reviewer to request a typed correction. */
        public boolean safeForReview() {
            return status == QualityStatus.COMPLETE && junitTestMethodCount > 0
                    && requiredApiInsideAssertion != RequiredApiAssertionStatus.UNRESOLVED
                    && controlPath != ControlPathStatus.UNRESOLVED
                    && swallowedFailures != SwallowedFailureStatus.UNRESOLVED
                    && !sourceFingerprint.isBlank();
        }
    }

    public record CompilationEvidence(Status status, int generatedClassCount, String implementationDigest) {
        public CompilationEvidence {
            if (status == null || generatedClassCount < 0 || implementationDigest == null
                    || implementationDigest.isBlank()) throw new IllegalArgumentException("valid compilation evidence is required");
        }
        public enum Status { PASS }
    }

    public enum QualityStatus { COMPLETE, PARTIAL }
    public enum RequiredApiAssertionStatus { VERIFIED, NOT_VERIFIED, NOT_APPLICABLE, UNRESOLVED }
    public enum ControlPathStatus { UNCONDITIONAL, EARLY_RETURN_PRESENT, UNRESOLVED }
    public enum SwallowedFailureStatus { NONE, PRESENT, UNRESOLVED }
}
