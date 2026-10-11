package io.github.testlens.application.tooling.ai;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPageObjectApiReferenceTest {
    @Test
    void renderAndParseAreExactRoundTripForSupportedTypes() {
        var reference = new CanonicalPageObjectApiReference("benchmark.LoginPage", "submit", List.of(
                "java.util.List<? extends java.lang.String[]>", "byte[]", "java.lang.String..."));

        String rendered = reference.render();

        assertEquals(reference, CanonicalPageObjectApiReference.parse(rendered));
        assertEquals("benchmark.LoginPage#submit(java.util.List<?extendsjava.lang.String[]>,byte[],java.lang.String...)",
                rendered);
        assertThrows(IllegalArgumentException.class,
                () -> CanonicalPageObjectApiReference.parse("benchmark.LoginPage.submit(String)"));
    }

    @Test
    void rendersRealLoginPageZeroAndTwoArgumentMethods() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("open", "open", List.of(), "benchmark.LoginPage"),
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage")))));

        assertEquals(List.of(
                        "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)",
                        "benchmark.LoginPage#open()"),
                CanonicalPageObjectApiReference.allowed(manifest).stream()
                        .map(CanonicalPageObjectApiReference::render).toList());
    }

    @Test
    void resolvesInheritedMethodToCallableChildOwner() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.SpecializedLoginPage", List.of(
                method("inherited", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage")))));

        var declaringType = CanonicalPageObjectApiReference.resolve(
                "benchmark.LoginPage#invalidPasswordShowsExpectedError(java.lang.String, java.lang.String)", manifest);
        var callableType = CanonicalPageObjectApiReference.resolve(
                "SpecializedLoginPage.invalidPasswordShowsExpectedError(String,String)", manifest);

        assertTrue(declaringType.valid(), declaringType.category().name());
        assertTrue(callableType.valid(), callableType.category().name());
        assertEquals("benchmark.SpecializedLoginPage#invalidPasswordShowsExpectedError(String,String)",
                declaringType.reference().render());
        assertEquals(declaringType.reference(), callableType.reference());
    }

    @Test
    void resolvesOverloadsOnlyWhenParameterTypesAreUnambiguous() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("sql", "submit", List.of("java.sql.Date"), "benchmark.LoginPage"),
                method("time", "submit", List.of("java.time.Date"), "benchmark.LoginPage")))));

        var exact = CanonicalPageObjectApiReference.resolve(
                "benchmark.LoginPage.submit(java.sql.Date)", manifest);
        var ambiguous = CanonicalPageObjectApiReference.resolve("LoginPage#submit(Date)", manifest);

        assertTrue(exact.valid(), exact.category().name());
        assertEquals("benchmark.LoginPage#submit(java.sql.Date)", exact.reference().render());
        assertFalse(ambiguous.valid());
        assertEquals(CanonicalPageObjectApiReference.Category.AMBIGUOUS_OVERLOAD, ambiguous.category());
    }

    @Test
    void rejectsWrongArityMissingMethodForeignOwnerAndInvalidStructures() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage")))));

        assertEquals(CanonicalPageObjectApiReference.Category.ARITY_MISMATCH,
                CanonicalPageObjectApiReference.resolve(
                        "benchmark.LoginPage.invalidPasswordShowsExpectedError(String)", manifest).category());
        assertEquals(CanonicalPageObjectApiReference.Category.METHOD_MISSING,
                CanonicalPageObjectApiReference.resolve("benchmark.LoginPage.logout()", manifest).category());
        assertEquals(CanonicalPageObjectApiReference.Category.DECLARING_CLASS_MISMATCH,
                CanonicalPageObjectApiReference.resolve(
                        "foreign.LoginPage.invalidPasswordShowsExpectedError(String,String)", manifest).category());
        assertEquals(CanonicalPageObjectApiReference.Category.INVALID_REFERENCE_STRUCTURE,
                CanonicalPageObjectApiReference.resolve("not a method", manifest).category());
        assertEquals(CanonicalPageObjectApiReference.Category.METHOD_SEPARATOR_MISMATCH,
                CanonicalPageObjectApiReference.resolve(
                        "benchmark.LoginPage::invalidPasswordShowsExpectedError(String,String)", manifest).category());
        assertEquals(CanonicalPageObjectApiReference.Category.PARAMETER_TYPE_FORMAT,
                CanonicalPageObjectApiReference.resolve(
                        "benchmark.LoginPage.invalidPasswordShowsExpectedError(String,,String)", manifest).category());
        assertEquals(CanonicalPageObjectApiReference.Category.PARAMETER_TYPE_FORMAT,
                CanonicalPageObjectApiReference.resolve(
                        "benchmark.LoginPage.invalidPasswordShowsExpectedError(Str ing,String)", manifest).category());
    }

    @Test
    void rejectsReturnTypesArgumentValuesAndJavaCallLikeTextInsteadOfGuessing() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage")))));

        assertEquals(CanonicalPageObjectApiReference.Category.INVALID_REFERENCE_STRUCTURE,
                CanonicalPageObjectApiReference.resolve(
                        "boolean benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)", manifest)
                        .category());
        assertEquals(CanonicalPageObjectApiReference.Category.METHOD_MISSING,
                CanonicalPageObjectApiReference.resolve(
                        "benchmark.LoginPage#invalidPasswordShowsExpectedError(username,password)", manifest)
                        .category());
        assertEquals(CanonicalPageObjectApiReference.Category.PARAMETER_TYPE_FORMAT,
                CanonicalPageObjectApiReference.resolve(
                        "benchmark.LoginPage#invalidPasswordShowsExpectedError(\"user\",\"password\")", manifest)
                        .category());
        assertEquals(CanonicalPageObjectApiReference.Category.INVALID_REFERENCE_STRUCTURE,
                CanonicalPageObjectApiReference.resolve(
                        "new benchmark.LoginPage().invalidPasswordShowsExpectedError(String,String)", manifest)
                        .category());
    }

    private static AgentContextPack.PageObjectCapabilityManifest manifest(String fingerprint,
            List<AgentContextPack.PageObjectCapability> pages) {
        return new AgentContextPack.PageObjectCapabilityManifest(fingerprint,
                AgentContextPack.ManifestCompleteness.COMPLETE, pages, List.of());
    }

    private static AgentContextPack.PageObjectCapability page(String qualifiedName,
            List<AgentContextPack.MethodCapability> methods) {
        return new AgentContextPack.PageObjectCapability(qualifiedName, "login", qualifiedName,
                "src/test/java/" + qualifiedName.replace('.', '/') + ".java", "PUBLIC", List.of(), methods);
    }

    private static AgentContextPack.MethodCapability method(String id, String name, List<String> parameterTypes,
            String declaringType) {
        return new AgentContextPack.MethodCapability(id, name,
                name + "(" + String.join(",", parameterTypes) + ")",
                parameterTypes.stream().map(type -> new AgentContextPack.CapabilityParameter("value", type)).toList(),
                "boolean", false, declaringType, "ASSERTION", List.of());
    }
}
