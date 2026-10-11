package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.AgentTask;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageObjectApiReferenceValidatorTest {
    @Test
    void acceptsLongQualifiedPasswordAndInheritedMethodReferencesMaterializedByManifest() {
        String ownerName = "com.example.enterprise.authentication.pages.deeply.nested."
                + "SpecializedPasswordLoginPage";
        AgentContextPack.MethodCapability inherited = new AgentContextPack.MethodCapability(
                "base-method", "passwordFailureIsVisible", "passwordFailureIsVisible(String,String)",
                List.of(new AgentContextPack.CapabilityParameter("username", "String"),
                        new AgentContextPack.CapabilityParameter("password", "String")),
                "boolean", false, "com.example.enterprise.authentication.pages.BasePasswordLoginPage",
                "ASSERTION", List.of());
        AgentContextPack.PageObjectCapability child = new AgentContextPack.PageObjectCapability(
                "child-page", "login", ownerName, "src/test/java/example/SpecializedPasswordLoginPage.java",
                "PUBLIC", List.of(), List.of(inherited));
        AgentContextPack context = context(new AgentContextPack.PageObjectCapabilityManifest(
                "current-fingerprint", AgentContextPack.ManifestCompleteness.COMPLETE,
                List.of(child), List.of()));
        AgentExecutor.AgentCommand command = new AgentExecutor.AgentCommand("run-1",
                AgentExecutor.Role.TEST_IMPLEMENTER,
                List.of(ArtifactEnvelope.create("context", "run-1", List.of(), 1, context)),
                Map.of(PageObjectApiReferenceValidator.FINGERPRINT_INSTRUCTION, "current-fingerprint"));

        var result = PageObjectApiReferenceValidator.from(command).validate(List.of(
                ownerName + ".passwordFailureIsVisible(String,String)"));

        assertTrue(result.valid(), result.reason());
    }

    @Test
    void failsClosedForMissingOrEmptyManifestAndForNoDeclaredReferences() {
        var missing = new PageObjectApiReferenceValidator(null, "current-fingerprint");
        var empty = new PageObjectApiReferenceValidator(AgentContextPack.PageObjectCapabilityManifest.empty(),
                "current-fingerprint");

        assertEquals(PageObjectApiReferenceValidator.MANIFEST_MISSING,
                missing.validate(List.of("example.LoginPage.login(String,String)")).reason());
        assertEquals(PageObjectApiReferenceValidator.MANIFEST_MISSING,
                empty.validate(List.of("example.LoginPage.login(String,String)")).reason());
        assertTrue(missing.validate(List.of()).valid());
        assertTrue(empty.validate(List.of()).valid());
    }

    @Test
    void missingManifestCannotTrustStructuredReferenceOrOfferAllowedReferences() {
        var validator = new PageObjectApiReferenceValidator(null, "current-fingerprint");

        assertFalse(validator.isTrustedStructuredValue("$.payload.pageObjectApisUsed[]",
                "example.LoginPage#login(String,String)"));
        assertEquals(List.of(), validator.allowedReferences());
    }

    @Test
    void supportsHashGenericsArraysAndVarargsAndRejectsSignatureMismatch() {
        var method = new AgentContextPack.MethodCapability("method", "submit",
                "submit(java.util.List<String>,byte[],String...)", List.of(
                new AgentContextPack.CapabilityParameter("users", "java.util.List<String>"),
                new AgentContextPack.CapabilityParameter("bytes", "byte[]"),
                new AgentContextPack.CapabilityParameter("labels", "String...")),
                "boolean", false, "ACTION", List.of());
        var owner = new AgentContextPack.PageObjectCapability("page", "login", "example.LoginPage",
                "src/test/java/example/LoginPage.java", "PUBLIC", List.of(), List.of(method));
        var manifest = new AgentContextPack.PageObjectCapabilityManifest("fingerprint",
                AgentContextPack.ManifestCompleteness.COMPLETE, List.of(owner), List.of());
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");

        var supported = validator.validate(List.of(
                "example.LoginPage#submit(java.util.List<String>,byte[],String...)"));
        assertTrue(supported.valid(), supported.reason());
        assertEquals(PageObjectApiReferenceValidator.METHOD_MISSING,
                validator.validate(List.of(
                        "example.LoginPage.submit(java.util.List<Integer>,byte[],String...)")).reason());
        assertEquals(PageObjectApiReferenceValidator.ARITY_MISMATCH,
                validator.validate(List.of("example.LoginPage.submit(String)")).reason());
        assertEquals(PageObjectApiReferenceValidator.ARITY_MISMATCH,
                validator.validate(List.of("LoginPage.submit(String)")).reason());
        assertEquals(PageObjectApiReferenceValidator.MANIFEST_STALE,
                new PageObjectApiReferenceValidator(manifest, "different")
                        .validate(List.of("example.LoginPage.submit(java.util.List<String>,byte[],String...)"))
                        .reason());
    }

    @Test
    void acceptsWildcardArrayBoundsAndParameterizedOwnerMemberTypes() {
        var method = new AgentContextPack.MethodCapability("complex", "submitComplex",
                "submitComplex(java.util.List<? extends String[]>,example.Outer<String>.Inner)", List.of(
                new AgentContextPack.CapabilityParameter("values", "java.util.List<? extends String[]>"),
                new AgentContextPack.CapabilityParameter("inner", "example.Outer<String>.Inner")),
                "boolean", false, "ACTION", List.of());
        var owner = new AgentContextPack.PageObjectCapability("page", "login", "example.LoginPage",
                "src/test/java/example/LoginPage.java", "PUBLIC", List.of(), List.of(method));
        var manifest = new AgentContextPack.PageObjectCapabilityManifest("fingerprint",
                AgentContextPack.ManifestCompleteness.COMPLETE, List.of(owner), List.of());

        var result = new PageObjectApiReferenceValidator(manifest, "fingerprint").validate(List.of(
                "example.LoginPage.submitComplex(java.util.List<? extends String[]>,example.Outer<String>.Inner)"));

        assertTrue(result.valid(), result.reason());
    }

    @Test
    void everyReferenceOfferedToTheImplementerIsAcceptedAndNormalizesToItself() {
        var inherited = method("inherited", "isReady", List.of(), "example.BasePage");
        var login = method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                "example.LoginPage");
        var overloadedText = method("text", "submit", List.of("java.lang.String"), "example.LoginPage");
        var overloadedNumber = method("number", "submit", List.of("java.lang.Integer"), "example.LoginPage");
        var manifest = manifest("fingerprint", List.of(
                page("example.LoginPage", List.of(login, overloadedText, overloadedNumber)),
                page("example.SpecializedLoginPage", List.of(inherited))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");

        List<String> offered = validator.allowedReferences();

        assertFalse(offered.isEmpty());
        for (String reference : offered) {
            var validation = validator.validate(List.of(reference));
            assertTrue(validation.valid(), () -> reference + ": " + validation.reason());
            assertEquals(List.of(reference), validator.normalize(List.of(reference)));
        }
    }

    @Test
    void acceptsOnlyUnambiguousEquivalentSpellingsAndReturnsCanonicalReference() {
        var manifest = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("login", "invalidPasswordShowsExpectedError", List.of("java.lang.String", "java.lang.String"),
                        "example.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");

        List<String> normalized = validator.normalize(List.of(
                "LoginPage#invalidPasswordShowsExpectedError(String, java.lang.String)"));

        assertEquals(List.of(
                "example.LoginPage#invalidPasswordShowsExpectedError(java.lang.String,java.lang.String)"), normalized);
    }

    @Test
    void rejectsAmbiguousOverloadWrongArityMissingMethodAndForeignClassWithSafeIndexedLocation() {
        var manifest = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("sql", "submit", List.of("java.sql.Date"), "example.LoginPage"),
                method("time", "submit", List.of("java.time.Date"), "example.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");

        var ambiguous = validator.validate(List.of("LoginPage.submit(Date)"));
        var arity = validator.validate(List.of("example.LoginPage.submit(Date,Date)"));
        var missing = validator.validate(List.of("example.LoginPage.cancel()"));
        var foreign = validator.validate(List.of("foreign.LoginPage.submit(java.sql.Date)"));

        assertEquals(PageObjectApiReferenceValidator.AMBIGUOUS_OVERLOAD, ambiguous.reason());
        assertEquals("$.payload.pageObjectApisUsed[0]", ambiguous.location());
        assertEquals(PageObjectApiReferenceValidator.ARITY_MISMATCH, arity.reason());
        assertEquals(PageObjectApiReferenceValidator.METHOD_MISSING, missing.reason());
        assertEquals(PageObjectApiReferenceValidator.DECLARING_CLASS_MISMATCH, foreign.reason());
    }

    @Test
    void rejectsStaleFingerprintBeforeResolvingOtherwiseValidReference() {
        var manifest = manifest("manifest-fingerprint", List.of(page("example.LoginPage", List.of(
                method("open", "open", List.of(), "example.LoginPage")))));

        var validation = new PageObjectApiReferenceValidator(manifest, "current-fingerprint")
                .validate(List.of("example.LoginPage.open()"));

        assertEquals(PageObjectApiReferenceValidator.MANIFEST_STALE, validation.reason());
        assertEquals("$.payload.pageObjectApisUsed[0]", validation.location());
    }

    @Test
    void sourceVerificationPreservesInternalFingerprintMismatchReason() {
        var complete = manifest("manifest-fingerprint", List.of(page("example.LoginPage", List.of(
                method("open", "open", List.of(), "example.LoginPage")))));
        var inconsistent = new AgentContextPack.PageObjectCapabilityManifest(complete.sourceFingerprint(),
                complete.completeness(), complete.classes(), complete.limitations(),
                AgentContextPack.ManifestProvenance.unspecified("different-index-fingerprint",
                        AgentContextPack.ManifestCompleteness.COMPLETE));

        var verification = new PageObjectApiReferenceValidator(inconsistent, "manifest-fingerprint")
                .verifySource("class T {}", List.of());

        assertRejected(verification, "PAGE_OBJECT_FINGERPRINT_MISMATCH");
        assertEquals("FINGERPRINT_MISMATCH", verification.safeMetadata().get("manifestReason"));
    }

    @Test
    void sourceVerificationRejectsAnIncompleteManifestBeforeClaimingAMethodIsMissing() {
        var complete = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("open", "open", List.of(), "example.LoginPage")))));
        var partial = new AgentContextPack.PageObjectCapabilityManifest(complete.sourceFingerprint(),
                AgentContextPack.ManifestCompleteness.PARTIAL, complete.classes(), List.of("bounded"));

        var verification = new PageObjectApiReferenceValidator(partial, "fingerprint")
                .verifySource("class T { void test() { new example.LoginPage().missing(); } }", List.of());

        assertRejected(verification, "PAGE_OBJECT_SOURCE_INDEX_INCOMPLETE");
        assertEquals("INCOMPLETE", verification.safeMetadata().get("manifestMatch"));
    }

    @Test
    void sourceUsageMustMatchDeclaredManifestReferences() {
        var manifest = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("login", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "example.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String reference = "example.LoginPage#invalidPasswordShowsExpectedError(String,String)";

        assertTrue(validator.validateSourceUsage("class T { void t() { new example.LoginPage()"
                + ".invalidPasswordShowsExpectedError(\"u\", \"p\"); } }", List.of(reference)).valid());
        assertTrue(validator.validateSourceUsage("class T {}", List.of()).valid());
        assertTrue(validator.validateSourceUsage("class T {}", List.of(reference)).valid());
        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED,
                validator.validateSourceUsage("class T { void t() { new example.LoginPage()"
                        + ".invalidPasswordShowsExpectedError(\"u\", \"p\"); ForeignPage page = new ForeignPage(); "
                        + "page.open(); } }", List.of(reference)).reason());
    }

    @Test
    void sourceUsageCannotAuthorizeAnotherSameArityOverloadOrUnrelatedReceiver() {
        var manifest = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("text", "submit", List.of("String"), "example.LoginPage"),
                method("number", "submit", List.of("Integer"), "example.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String textOverload = "example.LoginPage#submit(String)";

        var inferredOverload = validator.verifySource(
                "class T { void t() { new example.LoginPage().submit(1); } }", List.of(textOverload));
        assertVerified(inferredOverload, List.of("example.LoginPage#submit(Integer)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED,
                        PageObjectApiReferenceValidator.DECLARED_REFERENCE_UNUSED));
        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED,
                validator.validateSourceUsage("class T { void t() { Helper helper = new Helper(); "
                                + "helper.submit(\"value\"); } }",
                        List.of(textOverload)).reason());

        var qualifiedManifest = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("date", "submit", List.of("java.sql.Date"), "example.LoginPage")))));
        var qualifiedValidator = new PageObjectApiReferenceValidator(qualifiedManifest, "fingerprint");
        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED,
                qualifiedValidator.validateSourceUsage("class T { void t() { new foreign.LoginPage()"
                                + ".submit((foreign.Date) null); } }",
                        List.of("example.LoginPage#submit(java.sql.Date)")).reason());
    }

    @Test
    void sourceUsageRejectsImportedForeignOwnerWithTheSameSimpleName() {
        var manifest = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("text", "submit", List.of("String"), "example.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");

        var validation = validator.validateSourceUsage("import foreign.LoginPage; "
                        + "class T { void t() { LoginPage page = new LoginPage(); page.submit(\"value\"); } }",
                List.of("example.LoginPage#submit(String)"));

        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED, validation.reason());
    }

    @Test
    void sourceUsageDoesNotCountLocalClassAsTrustedPageObjectUsage() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");

        var validation = validator.verifySource("class LoginPage { "
                        + "boolean invalidPasswordShowsExpectedError(String username, String password) { "
                        + "return true; } } class T { void t() { new LoginPage()"
                        + ".invalidPasswordShowsExpectedError(\"user\", \"password\"); } }",
                List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"));

        assertVerified(validation, List.of(),
                List.of(PageObjectApiReferenceValidator.DECLARED_REFERENCE_UNUSED));
    }

    @Test
    void sourceUsageFailsClosedForThisFieldAndChainedReceiversItCannotVerify() {
        var manifest = manifest("fingerprint", List.of(page("example.LoginPage", List.of(
                method("text", "submit", List.of("String"), "example.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String declared = "example.LoginPage#submit(String)";

        var thisField = validator.validateSourceUsage("class T { ForeignPage page; void t() { "
                        + "new example.LoginPage().submit(\"declared\"); this.page.submit(\"foreign\"); } }",
                List.of(declared));
        var field = validator.validateSourceUsage("class T { ForeignPage page; void t() { "
                        + "new example.LoginPage().submit(\"declared\"); page.submit(\"foreign\"); } }",
                List.of(declared));
        var chained = validator.validateSourceUsage("class T { void t() { "
                        + "new example.LoginPage().submit(\"declared\"); "
                        + "provider().page().submit(\"foreign\"); } }",
                List.of(declared));

        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED, thisField.reason());
        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED, field.reason());
        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED, chained.reason());
    }

    @Test
    void sourceUsageRejectsRecordSpoofAndResolvesVariablesWithinTheirOwnMethod() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("text", "submit", List.of("String"), "benchmark.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String declared = "benchmark.LoginPage#submit(String)";

        var recordSpoof = validator.verifySource("package benchmark; class T { "
                        + "record LoginPage() { boolean submit(String value) { return true; } } "
                        + "void t() { new LoginPage().submit(\"value\"); } }", List.of(declared));
        var shadowedLocal = validator.validateSourceUsage("class T { "
                        + "void a() { benchmark.LoginPage page = null; } "
                        + "void b() { foreign.LoginPage page = new foreign.LoginPage(); page.submit(\"value\"); } }",
                List.of(declared));

        assertVerified(recordSpoof, List.of(),
                List.of(PageObjectApiReferenceValidator.DECLARED_REFERENCE_UNUSED));
        assertEquals(PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED, shadowedLocal.reason());
    }

    @Test
    void sourceUsageAcceptsTrustedStaticAssertionsAndVarButRejectsUnknownUnscopedCalls() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String declared = "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
        String valid = "import org.junit.jupiter.api.Assertions; class T { void t() { "
                + "var page = new benchmark.LoginPage(); Assertions.assertTrue("
                + "page.invalidPasswordShowsExpectedError(\"user\", \"invalid\")); } }";
        String unscoped = "import static org.junit.jupiter.api.Assertions.assertTrue; "
                + "class T extends benchmark.LoginPage { void t() { assertTrue("
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\")); "
                + "inventedPageObjectCall(); } }";

        assertTrue(validator.validateSourceUsage(valid, List.of(declared)).valid());
        assertEquals(PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST,
                validator.validateSourceUsage(unscoped, List.of(declared)).reason());
    }

    @Test
    void staticAssertionsWildcardDoesNotMaskUndeclaredInheritedPageObjectCall() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage"),
                method("open", "open", List.of(), "benchmark.LoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String declared = "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
        String control = "import static org.junit.jupiter.api.Assertions.*; "
                + "class T extends benchmark.LoginPage { void t() { assertTrue("
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\")); } }";
        String undeclaredInheritedCall = "import static org.junit.jupiter.api.Assertions.*; "
                + "class T extends benchmark.LoginPage { void t() { assertTrue("
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\")); "
                + "open(); } }";

        assertTrue(validator.validateSourceUsage(control, List.of(declared)).valid());
        assertVerified(validator.verifySource(undeclaredInheritedCall, List.of(declared)), List.of(
                        "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)",
                        "benchmark.LoginPage#open()"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    @Test
    void inheritedPageObjectMemberTakesPrecedenceOverCollidingStaticAssertionImport() {
        var normal = method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                "benchmark.LoginPage");
        var inheritedAssertion = method("inherited-assert", "assertTrue", List.of("boolean"),
                "benchmark.BaseLoginPage");
        var withoutCollision = new PageObjectApiReferenceValidator(
                manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(normal)))), "fingerprint");
        var withCollision = new PageObjectApiReferenceValidator(
                manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(normal, inheritedAssertion)))),
                "fingerprint");
        String normalReference = "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
        String inheritedReference = "benchmark.LoginPage#assertTrue(boolean)";
        String source = "import static org.junit.jupiter.api.Assertions.*; "
                + "class T extends benchmark.LoginPage { void t() { "
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\"); "
                + "assertTrue(true); } }";
        String sourceMethodPrecedence = "import static org.junit.jupiter.api.Assertions.*; "
                + "class T extends benchmark.LoginPage { void assertTrue(boolean condition) { } void t() { "
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\"); "
                + "assertTrue(true); } }";

        assertTrue(withoutCollision.validateSourceUsage(source, List.of(normalReference)).valid());
        assertVerified(withCollision.verifySource(source, List.of(normalReference)),
                List.of(inheritedReference, normalReference),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(withCollision.verifySource(source, List.of(normalReference, inheritedReference)),
                List.of(inheritedReference, normalReference), List.of());
        assertVerified(withCollision.verifySource(sourceMethodPrecedence, List.of(normalReference)),
                List.of(normalReference), List.of());
    }

    @Test
    void inheritedCollisionWithUnknownArgumentTypeFailsClosedWithoutFallingThroughToStaticImport() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage"),
                method("inherited-assert", "assertTrue", List.of("boolean"), "benchmark.BaseLoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String source = "import static org.junit.jupiter.api.Assertions.*; class Flag { boolean value; } "
                + "class T extends benchmark.LoginPage { void t(Flag flag) { "
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\"); "
                + "assertTrue(flag.value); } }";

        assertEquals(PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH,
                validator.validateSourceUsage(source, List.of(
                        "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)")).reason());
    }

    @Test
    void inheritedCollisionThroughLocalIntermediateSuperclassFailsClosed() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage"),
                method("inherited-assert", "assertTrue", List.of("boolean"), "benchmark.BaseLoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String source = "import static org.junit.jupiter.api.Assertions.*; "
                + "class Mid extends benchmark.LoginPage { } class T extends Mid { void t() { "
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\"); "
                + "assertTrue(true); } }";

        assertInheritedAssertionAndLoginVerified(validator, source);
    }

    @Test
    void unrelatedLocalSimpleNameDoesNotShadowExplicitManifestSuperclass() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage"),
                method("inherited-assert", "assertTrue", List.of("boolean"), "benchmark.BaseLoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String source = "import static org.junit.jupiter.api.Assertions.*; class LoginPage { } "
                + "class T extends benchmark.LoginPage { void t() { "
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\"); "
                + "assertTrue(true); } }";

        assertInheritedAssertionAndLoginVerified(validator, source);
    }

    @Test
    void duplicateNestedSimpleNamesDoNotHideTransitiveManifestSuperclass() {
        var manifest = manifest("fingerprint", List.of(page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage"),
                method("inherited-assert", "assertTrue", List.of("boolean"), "benchmark.BaseLoginPage")))));
        var validator = new PageObjectApiReferenceValidator(manifest, "fingerprint");
        String source = "import static org.junit.jupiter.api.Assertions.*; "
                + "class Decoy { static class Mid { } } "
                + "class Trusted { static class Mid extends benchmark.LoginPage { } } "
                + "class T extends Trusted.Mid { void t() { "
                + "new benchmark.LoginPage().invalidPasswordShowsExpectedError(\"user\", \"invalid\"); "
                + "assertTrue(true); } }";

        assertInheritedAssertionAndLoginVerified(validator, source);
    }

    @Test
    void sourceVerificationDerivesCanonicalUsageFromCompleteEmptyAndIncompleteDeclarations() {
        var validator = sourceValidator();
        String login = "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
        String ready = "benchmark.LoginPage#waitUntilReady()";
        String assertionSource = "import static org.junit.jupiter.api.Assertions.assertTrue; "
                + "class T { void test() { assertTrue(new benchmark.LoginPage()"
                + ".invalidPasswordShowsExpectedError(\"username\", \"password\")); } }";

        assertVerified(validator.verifySource(assertionSource, List.of(login)), List.of(login), List.of());
        assertVerified(validator.verifySource(assertionSource, List.of()), List.of(login),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource("class T { void test() { benchmark.LoginPage page = "
                        + "new benchmark.LoginPage(); page.invalidPasswordShowsExpectedError(\"u\", \"p\"); "
                        + "page.waitUntilReady(); } }", List.of(login)), List.of(login, ready),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource(assertionSource, List.of(login, ready)), List.of(login),
                List.of(PageObjectApiReferenceValidator.DECLARED_REFERENCE_UNUSED));
        assertVerified(validator.verifySource("class T { void test() { } }", List.of()), List.of(), List.of());
    }

    @Test
    void sourceVerificationRejectsNonexistentAndUnauthorizedCallsRegardlessOfDeclarations() {
        var validator = sourceValidator();
        String login = "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";

        assertRejected(validator.verifySource("class T { void test() { "
                        + "new benchmark.LoginPage().doesNotExist(); } }", List.of()),
                PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST);
        assertRejected(validator.verifySource("class T { void test() { "
                        + "new benchmark.LoginPage().deleteAccount(); } }", List.of(login)),
                PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST);
    }

    @Test
    void sourceVerificationUsesDistinctSafeFailureCategories() {
        var validator = sourceValidator();

        assertRejected(validator.verifySource("import foreign.LoginPage; class T { void test() { "
                        + "new LoginPage().waitUntilReady(); } }", List.of()),
                PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED);
        assertRejected(validator.verifySource("class T { void test() { "
                        + "provider().page().waitUntilReady(); } }", List.of()),
                PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED);
        assertRejected(validator.verifySource("class T { void test() { "
                        + "new benchmark.LoginPage().submit(true); } }", List.of()),
                PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH);
        assertRejected(validator.verifySource("class T { boolean condition() { return true; } void test() { "
                        + "new benchmark.LoginPage().submit(condition()); } }", List.of()),
                PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH);
        assertRejected(validator.verifySource("class T { void test( {", List.of()),
                PageObjectApiReferenceValidator.SOURCE_PARSE_FAILED);
    }

    @Test
    void foreignTypeCollidingWithTrustedSimpleNameDoesNotBypassReceiverVerification() {
        var verification = sourceValidator().verifySource("import foreign.List; class T { void test() { "
                + "List value = null; value.unauthorizedCall(); } }", List.of());

        assertRejected(verification, PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED);
        assertEquals("OWNER_NOT_FOUND", verification.sourceDiagnostic().manifestMatch());
        assertEquals("UNRESOLVED", verification.sourceDiagnostic().receiverCategory());
    }

    @Test
    void junitNamespaceSpoofDoesNotBypassReceiverVerification() {
        var verification = sourceValidator().verifySource("class T { void test() { "
                + "org.junit.evil.LoginPage value = null; value.unauthorizedCall(); } }", List.of());

        assertRejected(verification, PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED);
        assertEquals("OWNER_NOT_FOUND", verification.sourceDiagnostic().manifestMatch());
    }

    @Test
    void sourceMethodReturnTypeResolvesPageObjectArgumentsWithoutGuessing() {
        assertVerified(sourceValidator().verifySource("class T { String username() { return \"user\"; } "
                        + "void test() { new benchmark.LoginPage()"
                        + ".invalidPasswordShowsExpectedError(username(), \"password\"); } }", List.of()),
                List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    @Test
    void sourceVerificationResolvesNewLocalVarFieldThisImportAndFqcnReceivers() {
        var validator = sourceValidator();
        String login = "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
        List<String> sources = List.of(
                "class T { void test() { new benchmark.LoginPage()"
                        + ".invalidPasswordShowsExpectedError(\"u\", \"p\"); } }",
                "class T { void test() { var page = new benchmark.LoginPage(); "
                        + "page.invalidPasswordShowsExpectedError(\"u\", \"p\"); } }",
                "class T { benchmark.LoginPage page; void test() { "
                        + "page.invalidPasswordShowsExpectedError(\"u\", \"p\"); } }",
                "class T { benchmark.LoginPage page; void test() { "
                        + "this.page.invalidPasswordShowsExpectedError(\"u\", \"p\"); } }",
                "import benchmark.LoginPage; class T { void test() { LoginPage page = new LoginPage(); "
                        + "page.invalidPasswordShowsExpectedError(\"u\", \"p\"); } }");

        for (String source : sources) {
            assertVerified(validator.verifySource(source, List.of()), List.of(login),
                    List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        }
    }

    @Test
    void sourceVerificationResolvesInheritedOverloadedStaticNestedAndSafeChains() {
        var validator = sourceValidator();
        assertVerified(validator.verifySource("class T extends benchmark.LoginPage { "
                        + "void test() { waitUntilReady(); } }", List.of()),
                List.of("benchmark.LoginPage#waitUntilReady()"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource("class T { void test() { var page = new benchmark.LoginPage(); "
                        + "page.submit(\"value\"); page.submit(7); } }", List.of()),
                List.of("benchmark.LoginPage#submit(String)", "benchmark.LoginPage#submit(int)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource("class T { void test() { "
                        + "benchmark.LoginPage.staticCheck(\"value\"); } }", List.of()),
                List.of("benchmark.LoginPage#staticCheck(String)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource("class T { void test() { "
                        + "new benchmark.LoginPage().next().waitUntilReady(); } }", List.of()),
                List.of("benchmark.LoginPage#next()", "benchmark.LoginPage#waitUntilReady()"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));

        var nested = new PageObjectApiReferenceValidator(manifest("fingerprint", List.of(
                page("benchmark.Pages.LoginPage", List.of(method("ready", "waitUntilReady", List.of(),
                        "benchmark.Pages.LoginPage"))))), "fingerprint");
        assertVerified(nested.verifySource("class T { void test() { "
                        + "new benchmark.Pages.LoginPage().waitUntilReady(); } }", List.of()),
                List.of("benchmark.Pages.LoginPage#waitUntilReady()"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    @Test
    void sourceVerificationDoesNotConfuseJunitHelpersOrShadowingWithPageObjects() {
        var validator = sourceValidator();
        String login = "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
        String helpers = "import static org.junit.jupiter.api.Assertions.*; "
                + "class Helper { void record(String value) { } } class T { void test() { "
                + "new Helper().record(\"safe\"); assertTrue(new benchmark.LoginPage()"
                + ".invalidPasswordShowsExpectedError(\"u\", \"p\")); } }";
        String localOnly = "class LoginPage { boolean invalidPasswordShowsExpectedError(String u, String p) { "
                + "return true; } } class T { void test() { new LoginPage()"
                + ".invalidPasswordShowsExpectedError(\"u\", \"p\"); } }";
        String explicit = "class LoginPage { } class T { void test() { new benchmark.LoginPage()"
                + ".invalidPasswordShowsExpectedError(\"u\", \"p\"); } }";

        assertVerified(validator.verifySource(helpers, List.of()), List.of(login),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource(localOnly, List.of(login)), List.of(),
                List.of(PageObjectApiReferenceValidator.DECLARED_REFERENCE_UNUSED));
        assertVerified(validator.verifySource(explicit, List.of()), List.of(login),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    @Test
    void sourceVerificationHonorsJavaMemberPrecedenceForInheritedAndSourceMethods() {
        var inheritedAssertion = method("inherited-assert", "assertTrue", List.of("boolean"),
                "benchmark.BaseLoginPage");
        var validator = new PageObjectApiReferenceValidator(manifest("fingerprint", List.of(
                page("benchmark.LoginPage", List.of(inheritedAssertion)))), "fingerprint");
        String reference = "benchmark.LoginPage#assertTrue(boolean)";

        assertVerified(validator.verifySource("import static org.junit.jupiter.api.Assertions.*; "
                        + "class T extends benchmark.LoginPage { void test() { assertTrue(true); } }", List.of()),
                List.of(reference), List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource("import static org.junit.jupiter.api.Assertions.*; "
                        + "class T extends benchmark.LoginPage { void assertTrue(boolean value) { } "
                        + "void test() { assertTrue(true); } }", List.of(reference)), List.of(),
                List.of(PageObjectApiReferenceValidator.DECLARED_REFERENCE_UNUSED));
    }

    @Test
    void sourceVerificationRejectsForbiddenMethodReferenceAndDerivesAllowedReferences() {
        var validator = sourceValidator();
        String ready = "benchmark.LoginPage#waitUntilReady()";

        assertRejected(validator.verifySource("class T { void test() { "
                        + "Runnable action = new benchmark.LoginPage()::deleteAccount; } }", List.of()),
                PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST);
        assertVerified(validator.verifySource("class T { void test() { "
                        + "java.util.function.BooleanSupplier action = "
                        + "new benchmark.LoginPage()::waitUntilReady; } }", List.of()),
                List.of(ready), List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    @Test
    void sourceVerificationResolvesVariableAndTypeScopedMethodReferences() {
        var validator = sourceValidator();

        assertVerified(validator.verifySource("class T { void test() { benchmark.LoginPage page = "
                        + "new benchmark.LoginPage(); java.util.function.BooleanSupplier action = "
                        + "page::waitUntilReady; } }", List.of()),
                List.of("benchmark.LoginPage#waitUntilReady()"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertVerified(validator.verifySource("class T { void test() { "
                        + "java.util.function.Predicate<String> action = benchmark.LoginPage::staticCheck; } }",
                        List.of()),
                List.of("benchmark.LoginPage#staticCheck(String)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    @Test
    void sourceVerificationUsesFunctionalTargetToResolveOverloadedMethodReferencesAndFailsClosedWithoutIt() {
        var validator = sourceValidator();

        assertVerified(validator.verifySource("class T { void test() { benchmark.LoginPage page = "
                        + "new benchmark.LoginPage(); java.util.function.Consumer<String> action = page::submit; } }",
                        List.of()),
                List.of("benchmark.LoginPage#submit(String)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        var unresolved = validator.verifySource("class T { void test() { benchmark.LoginPage page = "
                + "new benchmark.LoginPage(); Object action = page::submit; } }", List.of());
        assertRejected(unresolved, PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH);
        assertEquals("UNRESOLVED", unresolved.sourceDiagnostic().signatureStatus());
    }

    @Test
    void incompatibleLocalOverloadDoesNotShadowInheritedPageObjectMethod() {
        var validator = sourceValidator();
        String ready = "benchmark.LoginPage#waitUntilReady()";

        assertVerified(validator.verifySource("class T extends benchmark.LoginPage { "
                        + "void waitUntilReady(String ignored) { } void test() { waitUntilReady(); } }", List.of()),
                List.of(ready), List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertRejected(validator.verifySource("class T extends benchmark.LoginPage { "
                        + "void deleteAccount(String ignored) { } void test() { deleteAccount(); } }", List.of()),
                PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST);
        assertVerified(validator.verifySource("class T extends benchmark.LoginPage { "
                        + "boolean waitUntilReady() { return true; } void test() { waitUntilReady(); } }", List.of(ready)),
                List.of(), List.of(PageObjectApiReferenceValidator.DECLARED_REFERENCE_UNUSED));
    }

    @Test
    void sourceVerificationEnforcesManifestConstructorsWithoutReportingThemAsMethodUsage() {
        var validator = sourceValidator();

        assertVerified(validator.verifySource("class T { void test() { new benchmark.LoginPage(); } }", List.of()),
                List.of(), List.of());
        var noMatch = validator.verifySource("class T { void test() { "
                + "new benchmark.LoginPage(\"unsupported\"); } }", List.of());
        assertRejected(noMatch, PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH);
        assertEquals("NO_MATCH", noMatch.sourceDiagnostic().signatureStatus());
    }

    private static void assertInheritedAssertionAndLoginVerified(PageObjectApiReferenceValidator validator,
            String source) {
        assertVerified(validator.verifySource(source, List.of(
                        "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)")), List.of(
                        "benchmark.LoginPage#assertTrue(boolean)",
                        "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    @Test
    void acceptsTheExplicitStudioBoundWebDriverConstructorExpression() {
        var login = new AgentContextPack.PageObjectCapability("page", "login", "benchmark.LoginPage",
                "src/test/java/benchmark/LoginPage.java", "PUBLIC", List.of(
                new AgentContextPack.ConstructorCapability("LoginPage(org.openqa.selenium.WebDriver)", List.of(
                        new AgentContextPack.CapabilityParameter("driver", "org.openqa.selenium.WebDriver")),
                        "PUBLIC", "DIRECT_CONSTRUCTION", false)), List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage")));
        var validator = new PageObjectApiReferenceValidator(manifest("fingerprint", List.of(login)), "fingerprint");
        String source = "import benchmark.LoginPage; import io.github.testlens.studio.browser.BrowserSessionContext; "
                + "class T { void test() { new LoginPage(BrowserSessionContext.currentDriver())"
                + ".invalidPasswordShowsExpectedError(\"u\", \"p\"); } }";

        assertVerified(validator.verifySource(source, List.of()),
                List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)"),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
    }

    private static void assertVerified(PageObjectApiReferenceValidator.SourceVerification verification,
            List<String> references, List<String> diagnostics) {
        assertTrue(verification.valid(), verification.reason());
        assertEquals("", verification.reason());
        assertEquals(references, verification.verifiedReferences());
        assertEquals(diagnostics, verification.diagnostics());
        assertNull(verification.sourceDiagnostic());
        assertEquals("$.payload.sourcePatch", verification.location());
    }

    private static void assertRejected(PageObjectApiReferenceValidator.SourceVerification verification,
            String reason) {
        assertFalse(verification.valid());
        assertEquals(reason, verification.reason());
        assertEquals(List.of(), verification.verifiedReferences());
        assertEquals(List.of(), verification.diagnostics());
        assertEquals("$.payload.sourcePatch", verification.location());
        assertEquals("SOURCE_API_VERIFICATION",verification.safeMetadata().get("validationStage"));
        assertTrue(verification.safeMetadata().keySet().containsAll(Set.of(
                "validationStage", "ruleId", "problemCategory", "astNodeKind", "receiverCategory",
                "manifestMatch", "fingerprintStatus", "signatureStatus")));
        assertTrue(Set.of("validationStage", "ruleId", "problemCategory", "astNodeKind", "receiverCategory",
                        "manifestMatch", "fingerprintStatus", "signatureStatus").stream()
                .noneMatch(key -> "UNKNOWN".equals(verification.safeMetadata().get(key))));
        assertFalse(verification.safeMetadata().toString().contains("benchmark.LoginPage"));
    }

    private static PageObjectApiReferenceValidator sourceValidator() {
        var login = page("benchmark.LoginPage", List.of(
                method("invalid", "invalidPasswordShowsExpectedError", List.of("String", "String"),
                        "benchmark.LoginPage"),
                method("ready", "waitUntilReady", List.of(), "benchmark.BaseLoginPage"),
                method("text", "submit", List.of("String"), "benchmark.LoginPage"),
                method("number", "submit", List.of("int"), "benchmark.LoginPage"),
                method("next", "next", List.of(), "benchmark.LoginPage", "benchmark.LoginPage", false),
                method("static", "staticCheck", List.of("String"), "benchmark.LoginPage", "boolean", true)));
        return new PageObjectApiReferenceValidator(manifest("fingerprint", List.of(login)), "fingerprint");
    }

    private static AgentContextPack.PageObjectCapabilityManifest manifest(String fingerprint,
            List<AgentContextPack.PageObjectCapability> pages) {
        return new AgentContextPack.PageObjectCapabilityManifest(fingerprint,
                AgentContextPack.ManifestCompleteness.COMPLETE, pages, List.of());
    }

    private static AgentContextPack.PageObjectCapability page(String qualifiedName,
            List<AgentContextPack.MethodCapability> methods) {
        return new AgentContextPack.PageObjectCapability(qualifiedName, "login", qualifiedName,
                "src/test/java/" + qualifiedName.replace('.', '/') + ".java", "PUBLIC",
                List.of(new AgentContextPack.ConstructorCapability(
                        qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1) + "()", List.of(),
                        "PUBLIC", "DIRECT_CONSTRUCTION", true)), methods);
    }

    private static AgentContextPack.MethodCapability method(String id, String name, List<String> parameterTypes,
            String declaringType) {
        return method(id, name, parameterTypes, declaringType, "boolean", false);
    }

    private static AgentContextPack.MethodCapability method(String id, String name, List<String> parameterTypes,
            String declaringType, String returnType, boolean staticMethod) {
        return new AgentContextPack.MethodCapability(id, name,
                name + "(" + String.join(",", parameterTypes) + ")",
                parameterTypes.stream().map(type -> new AgentContextPack.CapabilityParameter("value", type)).toList(),
                returnType, staticMethod, declaringType, "ASSERTION", List.of());
    }

    private static AgentContextPack context(AgentContextPack.PageObjectCapabilityManifest manifest) {
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
        return new AgentContextPack(header,
                new AgentTask(header, AgentTask.TaskType.CREATE_TEST, "invalid login", List.of("login"),
                        List.of(), List.of(), List.of("Page Objects only")),
                List.of(), Map.of(), List.of(), AgentContextPack.SourceContext.empty(), manifest,
                List.of(), List.of(), AgentContextPack.Completeness.COMPLETE_FOR_REQUESTED_SCOPE);
    }
}
