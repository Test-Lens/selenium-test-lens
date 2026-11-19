package io.github.testlens.application.tooling.ai.workflow.runner;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.body.VariableDeclarator;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference;
import io.github.testlens.application.tooling.ai.PageObjectManifestReadiness;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;

import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Validates agent-declared Page Object calls against trusted command context. */
public final class PageObjectApiReferenceValidator {
    public static final String FINGERPRINT_INSTRUCTION = "testLens.pageObjectSourceFingerprint";
    public static final String INVALID_REFERENCE_STRUCTURE = "PAGE_OBJECT_API_INVALID_REFERENCE_STRUCTURE";
    public static final String METHOD_SEPARATOR_MISMATCH = "PAGE_OBJECT_API_METHOD_SEPARATOR_MISMATCH";
    public static final String PARAMETER_TYPE_FORMAT = "PAGE_OBJECT_API_PARAMETER_TYPE_FORMAT";
    public static final String METHOD_MISSING = "PAGE_OBJECT_API_METHOD_MISSING";
    public static final String ARITY_MISMATCH = "PAGE_OBJECT_API_ARITY_MISMATCH";
    public static final String AMBIGUOUS_OVERLOAD = "PAGE_OBJECT_API_AMBIGUOUS_OVERLOAD";
    public static final String DECLARING_CLASS_MISMATCH = "PAGE_OBJECT_API_DECLARING_CLASS_MISMATCH";
    public static final String UNTRUSTED_REFERENCE = "PAGE_OBJECT_API_UNTRUSTED_REFERENCE";
    public static final String MANIFEST_MISSING = "PAGE_OBJECT_MANIFEST_MISSING";
    public static final String MANIFEST_INCOMPLETE = "PAGE_OBJECT_MANIFEST_INCOMPLETE";
    public static final String MANIFEST_STALE = "PAGE_OBJECT_MANIFEST_STALE";
    public static final String REFERENCE_REQUIRED = "PAGE_OBJECT_API_REFERENCE_REQUIRED";
    public static final String DECLARED_REFERENCE_UNUSED = "PAGE_OBJECT_API_DECLARED_REFERENCE_UNUSED";
    public static final String DECLARATION_OMITTED = "PAGE_OBJECT_API_DECLARATION_OMITTED";
    public static final String SOURCE_PARSE_FAILED = "PAGE_OBJECT_API_SOURCE_PARSE_FAILED";
    public static final String METHOD_NOT_IN_MANIFEST = "PAGE_OBJECT_API_METHOD_NOT_IN_MANIFEST";
    public static final String RECEIVER_UNRESOLVED = "PAGE_OBJECT_API_RECEIVER_UNRESOLVED";
    public static final String METHOD_SIGNATURE_MISMATCH = "PAGE_OBJECT_API_METHOD_SIGNATURE_MISMATCH";
    public static final String UNDECLARED_REFERENCE = "PAGE_OBJECT_API_UNDECLARED_REFERENCE";
    public static final String SOURCE_USAGE_UNVERIFIED = "PAGE_OBJECT_API_SOURCE_USAGE_UNVERIFIED";
    public static final Set<String> SOURCE_REJECTION_CODES = sourceRejectionCodes();
    public static final Set<String> SOURCE_WARNING_CODES = Set.of(DECLARATION_OMITTED, DECLARED_REFERENCE_UNUSED);
    @Deprecated public static final String SYNTAX_INVALID = INVALID_REFERENCE_STRUCTURE;

    private final AgentContextPack.PageObjectCapabilityManifest manifest;
    private final String currentFingerprint;

    public PageObjectApiReferenceValidator(AgentContextPack.PageObjectCapabilityManifest manifest,
                                           String currentFingerprint) {
        this.manifest = manifest;
        this.currentFingerprint = currentFingerprint;
    }

    static PageObjectApiReferenceValidator from(AgentExecutor.AgentCommand command) {
        AgentContextPack.PageObjectCapabilityManifest manifest = command.inputs().stream()
                .map(ArtifactEnvelope::payload)
                .filter(AgentContextPack.class::isInstance)
                .map(AgentContextPack.class::cast)
                .map(AgentContextPack::pageObjectCapabilities)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        return new PageObjectApiReferenceValidator(manifest,
                command.instructions().get(FINGERPRINT_INSTRUCTION));
    }

    boolean isTrustedStructuredValue(String path, String value) {
        return Set.of("$.payload.pageObjectApisUsed[]", "$.result.payload.pageObjectApisUsed[]").contains(path)
                && PageObjectManifestReadiness.assess(manifest,currentFingerprint).ready()
                && CanonicalPageObjectApiReference.resolve(value, manifest).valid();
    }

    public Validation validate(List<String> references) {
        List<String> values = references == null ? List.of() : references;
        if (values.isEmpty()) return manifest != null && !manifest.classes().isEmpty()
                ? Validation.fail(REFERENCE_REQUIRED, 0) : Validation.pass();
        if (manifest == null || manifest.classes().isEmpty()) return Validation.fail(MANIFEST_MISSING, 0);
        if (!fingerprintMatches()) return Validation.fail(MANIFEST_STALE, 0);
        for (int index = 0; index < values.size(); index++) {
            Validation validation = validateOne(values.get(index), index);
            if (!validation.valid()) return validation;
        }
        return Validation.pass();
    }

    /** Compatibility adapter; authorization is derived from source and never from declarations. */
    public Validation validateSourceUsage(String source, List<String> references) {
        SourceVerification verification = verifySource(source, references);
        return verification.valid() ? Validation.pass() : Validation.fail(verification.reason(), -1);
    }

    /** Derives the Page Object APIs actually used by source against the fingerprint-bound manifest. */
    public SourceVerification verifySource(String source, List<String> declarations) {
        if (manifest == null || manifest.classes().isEmpty()) return SourceVerification.fail(MANIFEST_MISSING,
                SourceDiagnostic.of("MANIFEST_REQUIRED", "MANIFEST", "MANIFEST", "NOT_APPLICABLE",
                        "MISSING", "NOT_EVALUATED", "NOT_EVALUATED"));
        PageObjectManifestReadiness.Assessment readiness=PageObjectManifestReadiness.assess(manifest,currentFingerprint);
        if (readiness.status()==PageObjectManifestReadiness.Status.FINGERPRINT_MISMATCH
                ||readiness.status()==PageObjectManifestReadiness.Status.MANIFEST_STALE) {
            return SourceVerification.fail(readiness.reasonCode(),
                    SourceDiagnostic.of("MANIFEST_FINGERPRINT", "MANIFEST", "MANIFEST", "NOT_APPLICABLE",
                            "STALE", "MISMATCH", "NOT_EVALUATED"),readiness.safeMetadata());
        }
        if (!readiness.ready()) {
            return SourceVerification.fail(readiness.reasonCode(),
                    SourceDiagnostic.of("MANIFEST_REQUIRED", "MANIFEST", "MANIFEST", "NOT_APPLICABLE",
                            "INCOMPLETE", "MATCHED", "NOT_EVALUATED"),readiness.safeMetadata());
        }
        var parsed = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)).parse(source == null ? "" : source);
        if (!parsed.isSuccessful() || parsed.getResult().isEmpty()) {
            return SourceVerification.fail(SOURCE_PARSE_FAILED,
                    SourceDiagnostic.of("SOURCE_PARSE", "SOURCE_SYNTAX", "COMPILATION_UNIT", "NOT_APPLICABLE",
                            "NOT_EVALUATED", "MATCHED", "NOT_EVALUATED"));
        }
        CompilationUnit unit = parsed.getResult().orElseThrow();
        List<MethodCallExpr> calls = unit.findAll(MethodCallExpr.class);
        SourceTypes sourceTypes = SourceTypes.from(unit);
        Set<CanonicalPageObjectApiReference> verified = new HashSet<>();
        for (MethodCallExpr call : calls) {
            if (call.getScope().isEmpty()) {
                LocalMethodMatch local = localSourceMethodMatch(call, unit, sourceTypes);
                if (local == LocalMethodMatch.MATCH) continue;
                if (local == LocalMethodMatch.UNKNOWN) {
                    return methodFailure(METHOD_SIGNATURE_MISMATCH, "METHOD_SIGNATURE", "UNRESOLVED",
                            "NOT_EVALUATED", "UNRESOLVED");
                }
            }
            String receiver;
            boolean staticOnly;
            if (call.getScope().isEmpty()) {
                InheritedResolution inherited = inheritedManifestCalls(call, unit, sourceTypes);
                if (inherited.matches().size() > 1 || (inherited.matches().isEmpty() && inherited.unknown())) {
                    return methodFailure(METHOD_SIGNATURE_MISMATCH, "METHOD_SIGNATURE", "KNOWN_PAGE_OBJECT",
                            "MATCHED", inherited.matches().size() > 1 ? "AMBIGUOUS" : "UNRESOLVED");
                }
                if (inherited.matches().isEmpty() && inherited.manifestOwner()) {
                    String trustedStaticOwner = sourceTypes.staticImportOwner(call.getNameAsString());
                    if (trustedStaticOwner != null && trustedLibraryType(trustedStaticOwner)) {
                        receiver = trustedStaticOwner;
                        staticOnly = true;
                    } else {
                        String reason = inherited.namedMethod() ? METHOD_SIGNATURE_MISMATCH : METHOD_NOT_IN_MANIFEST;
                        return methodFailure(reason, inherited.namedMethod() ? "METHOD_SIGNATURE" : "METHOD_MEMBERSHIP",
                                "KNOWN_PAGE_OBJECT", inherited.namedMethod() ? "MATCHED" : "METHOD_NOT_FOUND",
                                inherited.namedMethod() ? "NO_MATCH" : "NOT_EVALUATED");
                    }
                } else {
                    receiver = inherited.matches().isEmpty()
                            ? sourceTypes.staticImportOwner(call.getNameAsString())
                            : inherited.matches().get(0).owner();
                    staticOnly = inherited.matches().isEmpty();
                }
            } else {
                receiver = receiverTypeFromSource(call, unit, sourceTypes, new HashSet<>());
                staticOnly = typeScopedReceiver(call, unit, sourceTypes);
            }
            if (receiver == null) return methodFailure(RECEIVER_UNRESOLVED, "METHOD_RECEIVER", "UNRESOLVED",
                    "NOT_EVALUATED", "UNRESOLVED");
            List<ManifestMethod> ownerMethods = manifestMethods(receiver);
            if (ownerMethods.isEmpty()) {
                if (receiver.startsWith(SourceTypes.SOURCE_DECLARED + ".")) continue;
                if (handledByDeterministicPolicy(call, receiver)) continue;
                if (!trustedLibraryType(receiver)) return methodFailure(RECEIVER_UNRESOLVED, "METHOD_RECEIVER",
                        "UNRESOLVED", "OWNER_NOT_FOUND", "UNRESOLVED");
                continue;
            }
            List<ManifestMethod> named = ownerMethods.stream()
                    .filter(candidate -> candidate.reference().method().equals(call.getNameAsString()))
                    .filter(candidate -> !staticOnly || candidate.method().staticMethod()).toList();
            if (named.isEmpty()) return methodFailure(METHOD_NOT_IN_MANIFEST, "METHOD_MEMBERSHIP",
                    "KNOWN_PAGE_OBJECT", "METHOD_NOT_FOUND", "NOT_EVALUATED");
            List<ArgumentMatch> resolutions = named.stream()
                    .map(candidate -> argumentMatch(call, candidate.reference(), unit, sourceTypes)).toList();
            List<ManifestMethod> matches = named.stream().filter(candidate ->
                    argumentMatch(call, candidate.reference(), unit, sourceTypes) == ArgumentMatch.MATCH).toList();
            if (matches.size() != 1 || resolutions.contains(ArgumentMatch.UNKNOWN)) {
                String signatureStatus = matches.size() > 1 ? "AMBIGUOUS"
                        : resolutions.contains(ArgumentMatch.UNKNOWN) ? "UNRESOLVED" : "NO_MATCH";
                return methodFailure(METHOD_SIGNATURE_MISMATCH, "METHOD_SIGNATURE",
                        "KNOWN_PAGE_OBJECT", "MATCHED", signatureStatus);
            }
            verified.add(matches.get(0).reference());
        }
        SourceVerification constructorFailure = verifyConstructors(unit, sourceTypes);
        if (constructorFailure != null) return constructorFailure;
        SourceVerification methodReferenceFailure = verifyMethodReferences(unit, sourceTypes, verified);
        if (methodReferenceFailure != null) return methodReferenceFailure;
        List<String> canonical = verified.stream().map(CanonicalPageObjectApiReference::render).sorted().toList();
        List<String> diagnostics = declarationDiagnostics(declarations, verified);
        return SourceVerification.pass(canonical, diagnostics);
    }

    private static Set<String> sourceRejectionCodes() {
        Set<String> codes = new LinkedHashSet<>(Set.of(MANIFEST_MISSING, MANIFEST_INCOMPLETE, MANIFEST_STALE,
                SOURCE_PARSE_FAILED, METHOD_NOT_IN_MANIFEST, RECEIVER_UNRESOLVED, METHOD_SIGNATURE_MISMATCH));
        codes.addAll(PageObjectManifestReadiness.REJECTION_REASON_CODES);
        return Set.copyOf(codes);
    }

    private static SourceVerification methodFailure(String reason, String ruleId, String receiverCategory,
                                                    String manifestMatch, String signatureStatus) {
        return SourceVerification.fail(reason, SourceDiagnostic.of(ruleId,
                reason.equals(RECEIVER_UNRESOLVED) ? "RECEIVER_RESOLUTION"
                        : reason.equals(METHOD_NOT_IN_MANIFEST) ? "METHOD_MEMBERSHIP" : "SIGNATURE_RESOLUTION",
                "METHOD_CALL", receiverCategory, manifestMatch, "MATCHED", signatureStatus));
    }

    private List<String> declarationDiagnostics(List<String> declarations,
                                                Set<CanonicalPageObjectApiReference> verified) {
        List<String> values = declarations == null ? List.of() : declarations;
        Set<CanonicalPageObjectApiReference> validDeclared = new HashSet<>();
        boolean unused = false;
        for (String value : values) {
            CanonicalPageObjectApiReference.Resolution resolution =
                    CanonicalPageObjectApiReference.resolve(value, manifest);
            if (!resolution.valid() || !verified.contains(resolution.reference())) unused = true;
            if (resolution.valid()) validDeclared.add(resolution.reference());
        }
        List<String> diagnostics = new java.util.ArrayList<>();
        if (!validDeclared.containsAll(verified)) diagnostics.add(DECLARATION_OMITTED);
        if (unused) diagnostics.add(DECLARED_REFERENCE_UNUSED);
        return List.copyOf(diagnostics);
    }

    private List<ManifestMethod> manifestMethods(String receiver) {
        List<ManifestMethod> result = new java.util.ArrayList<>();
        for (AgentContextPack.PageObjectCapability owner : manifest.classes()) {
            if (!sameResolvedType(receiver, owner.qualifiedName())) continue;
            for (AgentContextPack.MethodCapability method : owner.methods()) {
                result.add(new ManifestMethod(owner, method,
                        CanonicalPageObjectApiReference.of(owner.qualifiedName(), method)));
            }
        }
        return result;
    }

    private String receiverTypeFromSource(MethodCallExpr call, CompilationUnit unit, SourceTypes sourceTypes,
                                          Set<MethodCallExpr> resolving) {
        if (!resolving.add(call) || call.getScope().isEmpty()) return null;
        var scope = call.getScope().orElseThrow();
        if (scope instanceof ObjectCreationExpr creation) return sourceTypes.resolve(creation.getTypeAsString());
        if (scope instanceof NameExpr name) {
            String variable = lexicalVariableType(name.getNameAsString(), call, unit, sourceTypes);
            return variable == null ? sourceTypes.resolve(name.getNameAsString()) : variable;
        }
        if (scope instanceof FieldAccessExpr access) {
            if (access.getScope().isThisExpr()) {
                return lexicalFieldType(access.getNameAsString(), call, sourceTypes);
            }
            if (access.getScope().isSuperExpr()) return null;
            return sourceTypes.resolve(access.toString());
        }
        if (scope instanceof MethodCallExpr chained) {
            String sourceReturn = sourceMethodReturnType(chained, sourceTypes);
            if (sourceReturn != null) return sourceReturn;
            ManifestMethod method = resolveManifestCall(chained, unit, sourceTypes, resolving);
            return method == null ? null : sourceTypes.resolve(method.method().returnType());
        }
        return null;
    }

    private static boolean typeScopedReceiver(MethodCallExpr call, CompilationUnit unit, SourceTypes sourceTypes) {
        if (call.getScope().isEmpty()) return false;
        var scope = call.getScope().orElseThrow();
        if (scope instanceof NameExpr name) {
            return lexicalVariableType(name.getNameAsString(), call, unit, sourceTypes) == null;
        }
        return scope instanceof FieldAccessExpr access
                && !access.getScope().isThisExpr() && !access.getScope().isSuperExpr();
    }

    private ManifestMethod resolveManifestCall(MethodCallExpr call, CompilationUnit unit, SourceTypes sourceTypes,
                                               Set<MethodCallExpr> resolving) {
        String receiver = receiverTypeFromSource(call, unit, sourceTypes, resolving);
        if (receiver == null) return null;
        List<ManifestMethod> matches = manifestMethods(receiver).stream()
                .filter(candidate -> argumentMatch(call, candidate.reference(), unit, sourceTypes) == ArgumentMatch.MATCH)
                .toList();
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private static String sourceMethodReturnType(MethodCallExpr call, SourceTypes sourceTypes) {
        if (call.getScope().isPresent() && !call.getScope().orElseThrow().isThisExpr()) return null;
        TypeDeclaration<?> owner = call.findAncestor(TypeDeclaration.class).orElse(null);
        if (owner == null) return null;
        List<MethodDeclaration> matches = owner.getMembers().stream().filter(MethodDeclaration.class::isInstance)
                .map(MethodDeclaration.class::cast)
                .filter(method -> method.getNameAsString().equals(call.getNameAsString()))
                .filter(method -> method.getParameters().stream().anyMatch(Parameter::isVarArgs)
                        ? call.getArguments().size() >= method.getParameters().size() - 1
                        : call.getArguments().size() == method.getParameters().size())
                .toList();
        return matches.size() == 1 ? sourceTypes.resolve(matches.get(0).getTypeAsString()) : null;
    }

    private static String receiverType(MethodCallExpr call, CompilationUnit unit,
                                       SourceTypes sourceTypes) {
        if (call.getScope().isEmpty()) return null;
        var scope = call.getScope().orElseThrow();
        if (scope instanceof ObjectCreationExpr creation) return sourceTypes.resolve(creation.getTypeAsString());
        if (scope instanceof NameExpr name) {
            String variable = lexicalVariableType(name.getNameAsString(), call, unit, sourceTypes);
            return variable == null ? sourceTypes.resolve(name.getNameAsString()) : variable;
        }
        if (scope instanceof FieldAccessExpr access && !access.toString().startsWith("this.")
                && !access.toString().startsWith("super.")) return sourceTypes.resolve(access.toString());
        // Resolving a field or a chained expression requires lexical/member type information. Treat it as
        // security-relevant and unverifiable instead of silently ignoring a possible Page Object call.
        if (scope instanceof FieldAccessExpr || scope instanceof MethodCallExpr) return null;
        return null;
    }

    private static boolean callMatches(MethodCallExpr call, CanonicalPageObjectApiReference reference,
                                       CompilationUnit unit, SourceTypes sourceTypes) {
        return argumentMatch(call, reference, unit, sourceTypes) == ArgumentMatch.MATCH;
    }

    private static ArgumentMatch argumentMatch(MethodCallExpr call, CanonicalPageObjectApiReference reference,
                                               CompilationUnit unit, SourceTypes sourceTypes) {
        if (!call.getNameAsString().equals(reference.method())) return ArgumentMatch.NO_MATCH;
        boolean varargs = !reference.parameterTypes().isEmpty()
                && reference.parameterTypes().get(reference.parameterTypes().size() - 1).endsWith("...");
        int fixed = varargs ? reference.parameterTypes().size() - 1 : reference.parameterTypes().size();
        if ((!varargs && call.getArguments().size() != fixed) || (varargs && call.getArguments().size() < fixed)) {
            return ArgumentMatch.NO_MATCH;
        }
        boolean unknown = false;
        for (int index = 0; index < call.getArguments().size(); index++) {
            String expected = index < fixed ? reference.parameterTypes().get(index)
                    : reference.parameterTypes().get(reference.parameterTypes().size() - 1)
                    .substring(0, reference.parameterTypes().get(reference.parameterTypes().size() - 1).length() - 3);
            String actual = expressionType(call.getArgument(index), call, unit, sourceTypes);
            if (actual == null) {
                unknown = true;
            } else if (!compatibleType(actual, expected)) {
                return ArgumentMatch.NO_MATCH;
            }
        }
        return unknown ? ArgumentMatch.UNKNOWN : ArgumentMatch.MATCH;
    }

    private static String expressionType(com.github.javaparser.ast.expr.Expression expression,
                                         com.github.javaparser.ast.Node call, CompilationUnit unit,
                                         SourceTypes sourceTypes) {
        if (expression.isStringLiteralExpr()) return "String";
        if (expression.isBooleanLiteralExpr()) return "boolean";
        if (expression.isCharLiteralExpr()) return "char";
        if (expression.isIntegerLiteralExpr()) return expression.asIntegerLiteralExpr().getValue().matches(".*[lL]")
                ? "long" : "int";
        if (expression.isDoubleLiteralExpr()) return expression.asDoubleLiteralExpr().getValue().matches(".*[fF]")
                ? "float" : "double";
        if (expression.isNameExpr()) return lexicalVariableType(
                expression.asNameExpr().getNameAsString(), call, unit, sourceTypes);
        if (expression.isCastExpr()) return sourceTypes.resolve(expression.asCastExpr().getTypeAsString());
        if (expression.isMethodCallExpr()) {
            MethodCallExpr methodCall = expression.asMethodCallExpr();
            if (isBrowserSessionDriverFactory(methodCall, sourceTypes)) return "org.openqa.selenium.WebDriver";
            String sourceReturnType = sourceMethodReturnType(methodCall, sourceTypes);
            if (sourceReturnType != null) return sourceReturnType;
        }
        return null;
    }

    private static boolean isBrowserSessionDriverFactory(MethodCallExpr call, SourceTypes sourceTypes) {
        if (!call.getNameAsString().equals("currentDriver") || !call.getArguments().isEmpty()
                || call.getScope().isEmpty()) return false;
        String owner = sourceTypes.resolve(call.getScope().orElseThrow().toString());
        return "io.github.testlens.studio.browser.BrowserSessionContext".equals(owner);
    }

    private static String lexicalVariableType(String name, com.github.javaparser.ast.Node call, CompilationUnit unit,
                                              SourceTypes sourceTypes) {
        MethodDeclaration method = call.findAncestor(MethodDeclaration.class).orElse(null);
        if (method != null) {
            List<VariableDeclarator> locals = method.findAll(VariableDeclarator.class).stream()
                    .filter(value -> value.getNameAsString().equals(name))
                    .filter(value -> value.getRange().isEmpty() || call.getRange().isEmpty()
                            || value.getRange().orElseThrow().begin.isBefore(call.getRange().orElseThrow().begin))
                    .toList();
            if (locals.size() > 1) return null;
            if (locals.size() == 1) return sourceTypes.resolve(variableType(locals.get(0)));
            List<Parameter> parameters = method.getParameters().stream()
                    .filter(value -> value.getNameAsString().equals(name)).toList();
            if (parameters.size() == 1) return sourceTypes.resolve(parameters.get(0).getTypeAsString());
        }
        TypeDeclaration<?> owner = call.findAncestor(TypeDeclaration.class).orElse(null);
        if (owner == null) return null;
        List<VariableDeclarator> fields = owner.getMembers().stream().filter(FieldDeclaration.class::isInstance)
                .map(FieldDeclaration.class::cast).flatMap(value -> value.getVariables().stream())
                .filter(value -> value.getNameAsString().equals(name)).toList();
        return fields.size() == 1 ? sourceTypes.resolve(variableType(fields.get(0))) : null;
    }

    private static String lexicalFieldType(String name, com.github.javaparser.ast.Node call, SourceTypes sourceTypes) {
        TypeDeclaration<?> owner = call.findAncestor(TypeDeclaration.class).orElse(null);
        if (owner == null) return null;
        List<VariableDeclarator> fields = owner.getMembers().stream().filter(FieldDeclaration.class::isInstance)
                .map(FieldDeclaration.class::cast).flatMap(value -> value.getVariables().stream())
                .filter(value -> value.getNameAsString().equals(name)).toList();
        return fields.size() == 1 ? sourceTypes.resolve(variableType(fields.get(0))) : null;
    }

    private static String variableType(VariableDeclarator variable) {
        if (!"var".equals(variable.getTypeAsString())) return variable.getTypeAsString();
        if (variable.getInitializer().isPresent() && variable.getInitializer().orElseThrow().isObjectCreationExpr()) {
            return variable.getInitializer().orElseThrow().asObjectCreationExpr().getTypeAsString();
        }
        if (variable.getInitializer().isPresent() && variable.getInitializer().orElseThrow().isCastExpr()) {
            return variable.getInitializer().orElseThrow().asCastExpr().getTypeAsString();
        }
        return null;
    }

    private static boolean sourceMethodDeclared(MethodCallExpr call) {
        TypeDeclaration<?> owner = call.findAncestor(TypeDeclaration.class).orElse(null);
        if (owner == null) return false;
        return owner.getMembers().stream().filter(MethodDeclaration.class::isInstance)
                .map(MethodDeclaration.class::cast).anyMatch(method -> method.getNameAsString().equals(call.getNameAsString())
                        && (method.getParameters().stream().anyMatch(Parameter::isVarArgs)
                        ? call.getArguments().size() >= method.getParameters().size() - 1
                        : call.getArguments().size() == method.getParameters().size()));
    }

    private static LocalMethodMatch localSourceMethodMatch(MethodCallExpr call, CompilationUnit unit,
                                                            SourceTypes sourceTypes) {
        TypeDeclaration<?> owner = call.findAncestor(TypeDeclaration.class).orElse(null);
        if (owner == null) return LocalMethodMatch.NO_MATCH;
        List<MethodDeclaration> candidates = owner.getMembers().stream().filter(MethodDeclaration.class::isInstance)
                .map(MethodDeclaration.class::cast)
                .filter(method -> method.getNameAsString().equals(call.getNameAsString())).toList();
        int matches = 0;
        boolean unknown = false;
        for (MethodDeclaration candidate : candidates) {
            ArgumentMatch match = sourceArgumentsMatch(call, candidate, unit, sourceTypes);
            if (match == ArgumentMatch.MATCH) matches++;
            if (match == ArgumentMatch.UNKNOWN) unknown = true;
        }
        if (unknown || matches > 1) return LocalMethodMatch.UNKNOWN;
        return matches == 1 ? LocalMethodMatch.MATCH : LocalMethodMatch.NO_MATCH;
    }

    private static ArgumentMatch sourceArgumentsMatch(MethodCallExpr call, MethodDeclaration method,
                                                       CompilationUnit unit, SourceTypes sourceTypes) {
        boolean varargs = !method.getParameters().isEmpty()
                && method.getParameter(method.getParameters().size() - 1).isVarArgs();
        int fixed = varargs ? method.getParameters().size() - 1 : method.getParameters().size();
        if ((!varargs && call.getArguments().size() != fixed) || (varargs && call.getArguments().size() < fixed)) {
            return ArgumentMatch.NO_MATCH;
        }
        boolean unknown = false;
        for (int index = 0; index < call.getArguments().size(); index++) {
            Parameter parameter = method.getParameter(index < fixed ? index : method.getParameters().size() - 1);
            String expected = sourceTypes.resolve(parameter.getTypeAsString());
            String actual = expressionType(call.getArgument(index), call, unit, sourceTypes);
            if (actual == null) unknown = true;
            else if (!compatibleType(actual, expected)) return ArgumentMatch.NO_MATCH;
        }
        return unknown ? ArgumentMatch.UNKNOWN : ArgumentMatch.MATCH;
    }

    private SourceVerification verifyConstructors(CompilationUnit unit, SourceTypes sourceTypes) {
        for (ObjectCreationExpr creation : unit.findAll(ObjectCreationExpr.class)) {
            String ownerType = sourceTypes.resolve(creation.getTypeAsString());
            AgentContextPack.PageObjectCapability owner = manifest.classes().stream()
                    .filter(candidate -> sameResolvedType(ownerType, candidate.qualifiedName()))
                    .findFirst().orElse(null);
            if (owner == null) continue;
            List<AgentContextPack.ConstructorCapability> supported = owner.constructors().stream()
                    .filter(candidate -> "PUBLIC".equals(candidate.accessibility()))
                    .filter(candidate -> "DIRECT_CONSTRUCTION".equals(candidate.acquisition())).toList();
            int matches = 0;
            boolean unknown = false;
            for (AgentContextPack.ConstructorCapability constructor : supported) {
                ArgumentMatch match = capabilityArgumentsMatch(creation.getArguments(), constructor.parameters(),
                        creation, unit, sourceTypes);
                if (match == ArgumentMatch.MATCH) matches++;
                if (match == ArgumentMatch.UNKNOWN) unknown = true;
            }
            if (unknown || matches != 1) {
                String signatureStatus = matches > 1 ? "AMBIGUOUS" : unknown ? "UNRESOLVED" : "NO_MATCH";
                return SourceVerification.fail(METHOD_SIGNATURE_MISMATCH,
                        SourceDiagnostic.of("CONSTRUCTOR_SIGNATURE", "SIGNATURE_RESOLUTION", "OBJECT_CREATION",
                                "KNOWN_PAGE_OBJECT", "MATCHED", "MATCHED", signatureStatus));
            }
        }
        return null;
    }

    private SourceVerification verifyMethodReferences(CompilationUnit unit, SourceTypes sourceTypes,
                                                      Set<CanonicalPageObjectApiReference> verified) {
        for (MethodReferenceExpr reference : unit.findAll(MethodReferenceExpr.class)) {
            MethodReferenceReceiver receiver = methodReferenceReceiver(reference, unit, sourceTypes);
            if (receiver == null || receiver.type() == null) return methodReferenceFailure(
                    RECEIVER_UNRESOLVED, "UNRESOLVED", "NOT_EVALUATED", "UNRESOLVED");
            List<ManifestMethod> ownerMethods = manifestMethods(receiver.type());
            if (ownerMethods.isEmpty()) {
                if (receiver.type().startsWith(SourceTypes.SOURCE_DECLARED + ".")
                        || trustedLibraryType(receiver.type())) continue;
                return methodReferenceFailure(RECEIVER_UNRESOLVED, "UNRESOLVED", "OWNER_NOT_FOUND", "UNRESOLVED");
            }
            List<ManifestMethod> named = ownerMethods.stream()
                    .filter(candidate -> candidate.reference().method().equals(reference.getIdentifier()))
                    .filter(candidate -> receiver.typeScoped() || !candidate.method().staticMethod())
                    .toList();
            if (named.isEmpty()) return methodReferenceFailure(
                    METHOD_NOT_IN_MANIFEST, "KNOWN_PAGE_OBJECT", "METHOD_NOT_FOUND", "NOT_EVALUATED");
            FunctionalTarget target = functionalTarget(reference, sourceTypes);
            if (target != null) named = named.stream()
                    .filter(candidate -> methodReferenceTargetMatches(
                            candidate, receiver.typeScoped(), receiver.type(), target))
                    .toList();
            if (named.size() != 1) return methodReferenceFailure(METHOD_SIGNATURE_MISMATCH,
                    "KNOWN_PAGE_OBJECT", "MATCHED", target == null ? "UNRESOLVED"
                            : named.isEmpty() ? "NO_MATCH" : "AMBIGUOUS");
            verified.add(named.get(0).reference());
        }
        return null;
    }

    private static SourceVerification methodReferenceFailure(String reason, String receiverCategory,
                                                             String manifestMatch, String signatureStatus) {
        return SourceVerification.fail(reason,
                SourceDiagnostic.of("METHOD_REFERENCE_SIGNATURE",
                        reason.equals(RECEIVER_UNRESOLVED) ? "RECEIVER_RESOLUTION"
                                : reason.equals(METHOD_NOT_IN_MANIFEST) ? "METHOD_MEMBERSHIP"
                                : "SIGNATURE_RESOLUTION",
                        "METHOD_REFERENCE", receiverCategory, manifestMatch, "MATCHED", signatureStatus));
    }

    private static boolean methodReferenceTargetMatches(ManifestMethod candidate, boolean typeScoped,
                                                        String receiverType, FunctionalTarget target) {
        int parameters = candidate.reference().parameterTypes().size();
        boolean varargs = parameters > 0 && candidate.reference().parameterTypes().get(parameters - 1).endsWith("...");
        int receiverParameters = typeScoped && !candidate.method().staticMethod() ? 1 : 0;
        int required = (varargs ? parameters - 1 : parameters) + receiverParameters;
        if (varargs ? target.arity() < required : target.arity() != required) return false;
        if (target.parameterTypes().isEmpty()) return target.arity() == 0;
        int offset = 0;
        if (receiverParameters == 1) {
            if (!compatibleType(target.parameterTypes().get(0), receiverType)) return false;
            offset = 1;
        }
        for (int index = offset; index < target.parameterTypes().size(); index++) {
            int methodIndex = index - offset;
            String expected = candidate.reference().parameterTypes().get(
                    methodIndex < parameters ? methodIndex : parameters - 1);
            if (varargs && methodIndex >= parameters - 1) {
                expected = expected.substring(0, expected.length() - 3);
            }
            if (!compatibleType(target.parameterTypes().get(index), expected)) return false;
        }
        return true;
    }

    private static FunctionalTarget functionalTarget(MethodReferenceExpr reference, SourceTypes sourceTypes) {
        VariableDeclarator variable = reference.findAncestor(VariableDeclarator.class).orElse(null);
        com.github.javaparser.ast.type.Type type = variable == null ? null : variable.getType();
        if (type == null) {
            com.github.javaparser.ast.expr.CastExpr cast = reference
                    .findAncestor(com.github.javaparser.ast.expr.CastExpr.class).orElse(null);
            if (cast != null) type = cast.getType();
        }
        if (type == null || !type.isClassOrInterfaceType()) return null;
        var functionalType = type.asClassOrInterfaceType();
        String simple = functionalType.getNameAsString();
        List<String> arguments = functionalType.getTypeArguments().orElseGet(
                com.github.javaparser.ast.NodeList::new).stream()
                .map(value -> sourceTypes.resolve(value.asString())).toList();
        if (arguments.stream().anyMatch(Objects::isNull)) return null;
        return switch (simple) {
            case "Runnable" -> new FunctionalTarget(0, List.of());
            case "Supplier", "Callable" -> arguments.size() == 1
                    ? new FunctionalTarget(0, List.of()) : null;
            case "Consumer", "Predicate" -> arguments.size() == 1
                    ? new FunctionalTarget(1, List.of(arguments.get(0))) : null;
            case "Function" -> arguments.size() == 2
                    ? new FunctionalTarget(1, List.of(arguments.get(0))) : null;
            case "UnaryOperator" -> arguments.size() == 1
                    ? new FunctionalTarget(1, List.of(arguments.get(0))) : null;
            case "BiConsumer", "BiPredicate" -> arguments.size() == 2
                    ? new FunctionalTarget(2, List.of(arguments.get(0), arguments.get(1))) : null;
            case "BiFunction" -> arguments.size() == 3
                    ? new FunctionalTarget(2, List.of(arguments.get(0), arguments.get(1))) : null;
            case "BinaryOperator" -> arguments.size() == 1
                    ? new FunctionalTarget(2, List.of(arguments.get(0), arguments.get(0))) : null;
            default -> null;
        };
    }

    private static MethodReferenceReceiver methodReferenceReceiver(MethodReferenceExpr reference,
                                                                    CompilationUnit unit,
                                                                    SourceTypes sourceTypes) {
        com.github.javaparser.ast.expr.Expression scope = reference.getScope();
        while (scope.isEnclosedExpr()) scope = scope.asEnclosedExpr().getInner();
        if (scope instanceof ObjectCreationExpr creation) {
            return new MethodReferenceReceiver(sourceTypes.resolve(creation.getTypeAsString()), false);
        }
        if (scope instanceof NameExpr name) {
            String variable = lexicalVariableType(name.getNameAsString(), reference, unit, sourceTypes);
            return new MethodReferenceReceiver(variable == null ? sourceTypes.resolve(name.getNameAsString()) : variable,
                    variable == null);
        }
        if (scope instanceof FieldAccessExpr access) {
            if (access.getScope().isThisExpr()) {
                return new MethodReferenceReceiver(lexicalFieldType(access.getNameAsString(), reference, sourceTypes),
                        false);
            }
            if (access.getScope().isSuperExpr()) return null;
            return new MethodReferenceReceiver(sourceTypes.resolve(access.toString()), true);
        }
        if (scope.isTypeExpr()) {
            String text = scope.asTypeExpr().getTypeAsString();
            String variable = lexicalVariableType(text, reference, unit, sourceTypes);
            return new MethodReferenceReceiver(variable == null ? sourceTypes.resolve(text) : variable,
                    variable == null);
        }
        return null;
    }

    private static ArgumentMatch capabilityArgumentsMatch(
            com.github.javaparser.ast.NodeList<com.github.javaparser.ast.expr.Expression> arguments,
            List<AgentContextPack.CapabilityParameter> parameters, com.github.javaparser.ast.Node context,
            CompilationUnit unit, SourceTypes sourceTypes) {
        boolean varargs = !parameters.isEmpty() && parameters.get(parameters.size() - 1).type().endsWith("...");
        int fixed = varargs ? parameters.size() - 1 : parameters.size();
        if ((!varargs && arguments.size() != fixed) || (varargs && arguments.size() < fixed)) {
            return ArgumentMatch.NO_MATCH;
        }
        boolean unknown = false;
        for (int index = 0; index < arguments.size(); index++) {
            String expected = parameters.get(index < fixed ? index : parameters.size() - 1).type();
            if (varargs && index >= fixed) expected = expected.substring(0, expected.length() - 3);
            String actual = expressionType(arguments.get(index), context, unit, sourceTypes);
            if (actual == null) unknown = true;
            else if (!compatibleType(actual, expected)) return ArgumentMatch.NO_MATCH;
        }
        return unknown ? ArgumentMatch.UNKNOWN : ArgumentMatch.MATCH;
    }

    private static boolean sameResolvedType(String left, String right) {
        return canonicalSourceType(left).equals(canonicalSourceType(right));
    }

    private static boolean sameType(String left, String right) {
        String leftType = canonicalSourceType(left);
        String rightType = canonicalSourceType(right);
        if (leftType.contains(".") && rightType.contains(".")) return leftType.equals(rightType);
        return simpleType(leftType).equals(simpleType(rightType));
    }

    private static boolean compatibleType(String actual, String expected) {
        if (sameType(actual, expected)) return true;
        return switch (simpleType(canonicalSourceType(actual))) {
            case "boolean" -> simpleType(canonicalSourceType(expected)).equals("Boolean");
            case "byte" -> simpleType(canonicalSourceType(expected)).equals("Byte");
            case "short" -> simpleType(canonicalSourceType(expected)).equals("Short");
            case "int" -> simpleType(canonicalSourceType(expected)).equals("Integer");
            case "long" -> simpleType(canonicalSourceType(expected)).equals("Long");
            case "float" -> simpleType(canonicalSourceType(expected)).equals("Float");
            case "double" -> simpleType(canonicalSourceType(expected)).equals("Double");
            case "char" -> simpleType(canonicalSourceType(expected)).equals("Character");
            default -> false;
        };
    }

    private static String canonicalSourceType(String value) {
        String type = value == null ? "" : value.replaceAll("\\s+", "").replace("...", "[]");
        int generic = type.indexOf('<');
        if (generic >= 0) type = type.substring(0, generic);
        return type;
    }

    private static String simpleType(String value) {
        return value.substring(value.lastIndexOf('.') + 1);
    }

    private static boolean handledByDeterministicPolicy(MethodCallExpr call, String receiver) {
        return ("io.github.testlens.studio.browser.BrowserSessionContext".equals(canonicalSourceType(receiver))
                && call.getNameAsString().equals("currentDriver") && call.getArguments().isEmpty())
                || "org.openqa.selenium.By".equals(canonicalSourceType(receiver))
                && Set.of("id", "name", "cssSelector", "xpath", "className", "tagName",
                "linkText", "partialLinkText").contains(call.getNameAsString());
    }

    private InheritedResolution inheritedManifestCalls(MethodCallExpr call, CompilationUnit unit,
                                                        SourceTypes sourceTypes) {
        com.github.javaparser.ast.body.ClassOrInterfaceDeclaration sourceClass = call
                .findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .orElse(null);
        if (sourceClass == null || sourceClass.isInterface()) {
            return new InheritedResolution(List.of(), false, false, false);
        }
        Set<String> superOwners = manifestSuperOwners(sourceClass, unit, sourceTypes, new HashSet<>());
        List<CanonicalPageObjectApiReference> matches = new java.util.ArrayList<>();
        boolean unknown = false;
        boolean named = false;
        for (AgentContextPack.PageObjectCapability owner : manifest.classes()) {
            if (!superOwners.contains(owner.qualifiedName())) continue;
            for (AgentContextPack.MethodCapability method : owner.methods()) {
                if (method.name().equals(call.getNameAsString())) named = true;
                CanonicalPageObjectApiReference reference =
                        CanonicalPageObjectApiReference.of(owner.qualifiedName(), method);
                ArgumentMatch match = argumentMatch(call, reference, unit, sourceTypes);
                if (match == ArgumentMatch.MATCH) matches.add(reference);
                if (match == ArgumentMatch.UNKNOWN) unknown = true;
            }
        }
        return new InheritedResolution(matches.stream().distinct().toList(), unknown,
                !superOwners.isEmpty(), named);
    }

    private Set<String> manifestSuperOwners(
            com.github.javaparser.ast.body.ClassOrInterfaceDeclaration sourceClass,
            CompilationUnit unit, SourceTypes sourceTypes, Set<String> visited) {
        String sourceIdentity = sourceClass.getNameAsString() + "@"
                + sourceClass.getBegin().map(position -> position.line + ":" + position.column).orElse("?");
        if (!visited.add(sourceIdentity)) return Set.of();
        Set<String> owners = new HashSet<>();
        java.util.stream.Stream.concat(sourceClass.getExtendedTypes().stream(),
                        sourceClass.getImplementedTypes().stream())
                .forEach(parent -> {
                    String rawParent = parent.getNameWithScope();
                    List<String> explicitlyQualifiedOwners = manifest.classes().stream()
                            .map(AgentContextPack.PageObjectCapability::qualifiedName)
                            .filter(owner -> rawParent.contains(".")
                                    && canonicalSourceType(rawParent).equals(canonicalSourceType(owner)))
                            .toList();
                    if (!explicitlyQualifiedOwners.isEmpty()) {
                        owners.addAll(explicitlyQualifiedOwners);
                        return;
                    }
                    String resolved = sourceTypes.resolve(rawParent);
                    if (resolved == null) return;
                    if (resolved.startsWith(SourceTypes.SOURCE_DECLARED + ".")) {
                        String localIdentity = resolved.substring((SourceTypes.SOURCE_DECLARED + ".").length());
                        unit.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class).stream()
                                .filter(candidate -> declaredTypeIdentity(candidate).equals(localIdentity))
                                .forEach(candidate -> owners.addAll(
                                        manifestSuperOwners(candidate, unit, sourceTypes, visited)));
                        return;
                    }
                    manifest.classes().stream()
                            .map(AgentContextPack.PageObjectCapability::qualifiedName)
                            .filter(owner -> sameResolvedType(resolved, owner))
                            .forEach(owners::add);
                });
        return owners;
    }

    private static boolean trustedWildcardStaticMethod(String owner, String method) {
        if ("org.junit.jupiter.api.Assertions".equals(owner)) {
            return Set.of("assertAll", "assertArrayEquals", "assertDoesNotThrow", "assertEquals",
                    "assertFalse", "assertInstanceOf", "assertIterableEquals", "assertLinesMatch",
                    "assertNotEquals", "assertNotNull", "assertNotSame", "assertNull", "assertSame",
                    "assertThrows", "assertThrowsExactly", "assertTimeout", "assertTimeoutPreemptively",
                    "assertTrue", "fail").contains(method);
        }
        return "org.junit.jupiter.api.Assumptions".equals(owner)
                && Set.of("abort", "assumeFalse", "assumeThat", "assumeTrue").contains(method);
    }

    private static boolean trustedLibraryType(String type) {
        String normalized = canonicalSourceType(type);
        return Set.of("String", "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double",
                "Character").contains(normalized)
                || normalized.startsWith("java.")
                || Set.of("org.junit.jupiter.api.Assertions", "org.junit.jupiter.api.Assumptions")
                .contains(normalized);
    }

    private static String declaredTypeIdentity(TypeDeclaration<?> type) {
        java.util.ArrayDeque<String> names = new java.util.ArrayDeque<>();
        names.addFirst(type.getNameAsString());
        com.github.javaparser.ast.Node current = type;
        while (current.getParentNode().isPresent()) {
            current = current.getParentNode().orElseThrow();
            if (current instanceof TypeDeclaration<?> parent) names.addFirst(parent.getNameAsString());
        }
        return String.join(".", names);
    }

    private enum ArgumentMatch { MATCH, NO_MATCH, UNKNOWN }
    private enum LocalMethodMatch { MATCH, NO_MATCH, UNKNOWN }

    private record InheritedResolution(List<CanonicalPageObjectApiReference> matches, boolean unknown,
                                       boolean manifestOwner, boolean namedMethod) {}

    private record ManifestMethod(AgentContextPack.PageObjectCapability owner,
                                  AgentContextPack.MethodCapability method,
                                  CanonicalPageObjectApiReference reference) {}

    private record MethodReferenceReceiver(String type, boolean typeScoped) {}
    private record FunctionalTarget(int arity, List<String> parameterTypes) {}

    /** Conservative source-level type resolution for the syntax accepted before compilation. */
    private record SourceTypes(Map<String, String> explicitImports, Map<String, String> staticImports,
                               Set<String> wildcardStaticImports, Set<String> declaredSimpleNames,
                               Set<String> declaredQualifiedNames,
                               String packageName) {
        private static final String SOURCE_DECLARED = "<source-declared>";

        private static SourceTypes from(CompilationUnit unit) {
            Map<String, String> imports = new HashMap<>();
            Set<String> ambiguous = new HashSet<>();
            unit.getImports().stream().filter(value -> !value.isStatic() && !value.isAsterisk())
                    .map(value -> value.getNameAsString()).forEach(qualified -> {
                        String simple = simpleType(qualified);
                        String previous = imports.putIfAbsent(simple, qualified);
                        if (previous != null && !previous.equals(qualified)) ambiguous.add(simple);
                    });
            ambiguous.forEach(imports::remove);
            Map<String, String> staticMembers = new HashMap<>();
            Set<String> wildcardStatics = new HashSet<>();
            unit.getImports().stream().filter(value -> value.isStatic()).forEach(value -> {
                if (value.isAsterisk()) wildcardStatics.add(value.getNameAsString());
                else {
                    String member = simpleType(value.getNameAsString());
                    String owner = value.getNameAsString().substring(0,
                            value.getNameAsString().length() - member.length() - 1);
                    if (staticMembers.putIfAbsent(member, owner) != null) staticMembers.put(member, "");
                }
            });
            Set<String> declared = new HashSet<>();
            Set<String> declaredQualified = new HashSet<>();
            unit.findAll(TypeDeclaration.class).forEach(value -> {
                declared.add(value.getNameAsString());
                declaredQualified.add(declaredTypeIdentity(value));
            });
            return new SourceTypes(Map.copyOf(imports), Map.copyOf(staticMembers), Set.copyOf(wildcardStatics),
                    Set.copyOf(declared), Set.copyOf(declaredQualified),
                    unit.getPackageDeclaration().map(value -> value.getNameAsString()).orElse(""));
        }

        private String staticImportOwner(String method) {
            String exact = staticImports.get(method);
            if (exact != null) return exact.isEmpty() ? null : exact;
            List<String> trustedOwners = wildcardStaticImports.stream()
                    .filter(owner -> trustedWildcardStaticMethod(owner, method))
                    .toList();
            return trustedOwners.size() == 1 ? trustedOwners.get(0) : null;
        }

        private String resolve(String rawType) {
            if (rawType == null || rawType.isBlank()) return null;
            String canonical = canonicalSourceType(rawType);
            String simple = simpleType(canonical);
            if (canonical.contains(".")) return declaredQualifiedNames.contains(canonical)
                    ? SOURCE_DECLARED + "." + canonical : canonical;
            if (declaredSimpleNames.contains(simple)) return SOURCE_DECLARED + "." + simple;
            if (isPrimitive(simple)) return simple;
            String imported = explicitImports.get(simple);
            if (imported != null) return imported;
            if (Set.of("String", "Boolean", "Byte", "Short", "Integer", "Long", "Float", "Double",
                    "Character", "Object").contains(simple)) return "java.lang." + simple;
            return packageName.isEmpty() ? canonical : packageName + "." + canonical;
        }

        private static boolean isPrimitive(String type) {
            return Set.of("boolean", "byte", "short", "int", "long", "float", "double", "char", "void")
                    .contains(type);
        }
    }

    private Validation validateOne(String value, int index) {
        CanonicalPageObjectApiReference.Resolution resolution =
                CanonicalPageObjectApiReference.resolve(value, manifest);
        return resolution.valid() ? Validation.pass() : Validation.fail(reason(resolution.category()), index);
    }

    private boolean fingerprintMatches() {
        if (manifest == null || manifest.classes().isEmpty()) return false;
        String expected = manifest.sourceFingerprint();
        return expected != null && !expected.isBlank()
                && currentFingerprint != null && !currentFingerprint.isBlank()
                && expected.equals(currentFingerprint);
    }

    public List<String> allowedReferences() {
        if (!fingerprintMatches()) return List.of();
        return CanonicalPageObjectApiReference.allowed(manifest).stream()
                .map(CanonicalPageObjectApiReference::render).toList();
    }

    public List<String> normalize(List<String> references) {
        Validation validation = validate(references);
        if (!validation.valid()) throw new IllegalArgumentException(validation.reason());
        return (references == null ? List.<String>of() : references).stream()
                .map(value -> CanonicalPageObjectApiReference.resolve(value, manifest).reference().render())
                .distinct().sorted().toList();
    }

    private static String reason(CanonicalPageObjectApiReference.Category category) {
        return switch (category) {
            case INVALID_REFERENCE_STRUCTURE -> INVALID_REFERENCE_STRUCTURE;
            case METHOD_SEPARATOR_MISMATCH -> METHOD_SEPARATOR_MISMATCH;
            case PARAMETER_TYPE_FORMAT -> PARAMETER_TYPE_FORMAT;
            case METHOD_MISSING -> METHOD_MISSING;
            case ARITY_MISMATCH -> ARITY_MISMATCH;
            case AMBIGUOUS_OVERLOAD -> AMBIGUOUS_OVERLOAD;
            case DECLARING_CLASS_MISMATCH -> DECLARING_CLASS_MISMATCH;
            case UNTRUSTED_REFERENCE -> UNTRUSTED_REFERENCE;
            case VALID -> throw new IllegalArgumentException("A failure category is required");
        };
    }

    public record Validation(boolean valid, String reason, int itemIndex) {
        static Validation pass() { return new Validation(true, "", -1); }
        static Validation fail(String reason, int itemIndex) { return new Validation(false, reason, itemIndex); }
        public String location() { return itemIndex < 0 ? "$.payload.pageObjectApisUsed"
                : "$.payload.pageObjectApisUsed[" + itemIndex + "]"; }
    }

    public record SourceDiagnostic(String validationStage, String ruleId, String problemCategory,
                                   String astNodeKind, String receiverCategory, String manifestMatch,
                                   String fingerprintStatus, String signatureStatus) {
        public SourceDiagnostic {
            requireAllowed(validationStage, Set.of("SOURCE_API_VERIFICATION"), "validationStage");
            requireAllowed(ruleId, Set.of("MANIFEST_REQUIRED", "MANIFEST_FINGERPRINT", "SOURCE_PARSE",
                    "METHOD_RECEIVER", "METHOD_MEMBERSHIP", "METHOD_SIGNATURE", "CONSTRUCTOR_SIGNATURE",
                    "METHOD_REFERENCE_SIGNATURE"), "ruleId");
            requireAllowed(problemCategory, Set.of("MANIFEST", "SOURCE_SYNTAX", "RECEIVER_RESOLUTION",
                    "METHOD_MEMBERSHIP", "SIGNATURE_RESOLUTION"), "problemCategory");
            requireAllowed(astNodeKind, Set.of("MANIFEST", "COMPILATION_UNIT", "METHOD_CALL",
                    "OBJECT_CREATION", "METHOD_REFERENCE"), "astNodeKind");
            requireAllowed(receiverCategory, Set.of("KNOWN_PAGE_OBJECT", "KNOWN_NON_PAGE_OBJECT", "UNRESOLVED",
                    "NOT_APPLICABLE"), "receiverCategory");
            requireAllowed(manifestMatch, Set.of("MATCHED", "MISSING", "INCOMPLETE", "STALE", "OWNER_NOT_FOUND",
                    "METHOD_NOT_FOUND", "NOT_EVALUATED"), "manifestMatch");
            requireAllowed(fingerprintStatus, Set.of("MATCHED", "MISMATCH", "NOT_EVALUATED"),
                    "fingerprintStatus");
            requireAllowed(signatureStatus, Set.of("RESOLVED", "NO_MATCH", "AMBIGUOUS", "UNRESOLVED",
                    "NOT_EVALUATED"), "signatureStatus");
        }

        private static void requireAllowed(String value, Set<String> allowed, String field) {
            if (!allowed.contains(value)) throw new IllegalArgumentException(field + " is not a safe diagnostic value");
        }

        static SourceDiagnostic of(String ruleId, String problemCategory, String astNodeKind,
                                   String receiverCategory, String manifestMatch, String fingerprintStatus,
                                   String signatureStatus) {
            return new SourceDiagnostic("SOURCE_API_VERIFICATION", ruleId, problemCategory, astNodeKind,
                    receiverCategory, manifestMatch, fingerprintStatus, signatureStatus);
        }

        public Map<String, String> safeMetadata() {
            return Map.of("validationStage", validationStage, "ruleId", ruleId,
                    "problemCategory", problemCategory, "astNodeKind", astNodeKind,
                    "receiverCategory", receiverCategory, "manifestMatch", manifestMatch,
                    "fingerprintStatus", fingerprintStatus, "signatureStatus", signatureStatus);
        }
    }

    public record SourceVerification(boolean valid, String reason, List<String> verifiedReferences,
                                     List<String> diagnostics, SourceDiagnostic sourceDiagnostic,
                                     Map<String,String> manifestMetadata) {
        public SourceVerification {
            reason = reason == null ? "" : reason;
            verifiedReferences = List.copyOf(verifiedReferences == null ? List.of() : verifiedReferences);
            diagnostics = List.copyOf(diagnostics == null ? List.of() : diagnostics);
            manifestMetadata=Map.copyOf(manifestMetadata==null?Map.of():manifestMetadata);
        }
        public SourceVerification(boolean valid, String reason, List<String> verifiedReferences,
                                  List<String> diagnostics) {
            this(valid, reason, verifiedReferences, diagnostics, null,Map.of());
        }
        public SourceVerification(boolean valid, String reason, List<String> verifiedReferences,
                                  List<String> diagnostics,SourceDiagnostic sourceDiagnostic) {
            this(valid,reason,verifiedReferences,diagnostics,sourceDiagnostic,Map.of());
        }
        static SourceVerification pass(List<String> references, List<String> diagnostics) {
            return new SourceVerification(true, "", references, diagnostics, null,Map.of());
        }
        static SourceVerification fail(String reason, SourceDiagnostic diagnostic) {
            return new SourceVerification(false, reason, List.of(), List.of(), diagnostic,Map.of());
        }
        static SourceVerification fail(String reason,SourceDiagnostic diagnostic,Map<String,String>manifestMetadata){
            return new SourceVerification(false,reason,List.of(),List.of(),diagnostic,manifestMetadata);
        }
        public String location() { return "$.payload.sourcePatch"; }
        public Map<String, String> safeMetadata() {
            if(sourceDiagnostic==null)return manifestMetadata;
            Map<String,String>result=new java.util.LinkedHashMap<>(sourceDiagnostic.safeMetadata());
            result.putAll(manifestMetadata);return Map.copyOf(result);
        }
    }

}
