package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator;
import io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest;
import io.github.testlens.application.tooling.ai.workflow.WorkflowReport;
import io.github.testlens.application.tooling.ai.workflow.runner.PageObjectApiReferenceValidator;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudioSourceApiVerificationFixtureTest {
    private static final String LOGIN_API =
            "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";
    private static final String INHERITED_API = "benchmark.DerivedLoginPage#inheritedReady()";
    private static final Set<String> SAFE_METADATA_KEYS = Set.of(
            "validationStage", "ruleId", "problemCategory", "astNodeKind", "receiverCategory",
            "manifestMatch", "fingerprintStatus", "signatureStatus");

    @TempDir
    Path project;

    @Test
    void productionManifestVerifiesTheCertificationFixtureWithPreciseSafeFailures() throws Exception {
        Path sourceRoot = prepareProjectFromCertificationFixture();
        AgentContextPack context = prepareThroughStudio(sourceRoot);
        AgentContextPack.PageObjectCapabilityManifest manifest = context.pageObjectCapabilities();
        String indexedFingerprint = new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                project, List.of(sourceRoot), List.of())).projectFingerprint();

        assertEquals(indexedFingerprint, manifest.sourceFingerprint(),
                "the validator manifest must remain bound to the actual indexed fixture");
        assertTrue(manifest.classes().stream().anyMatch(value -> value.qualifiedName().equals("benchmark.LoginPage")),
                () -> "prepared capability classes: " + manifest.classes().stream()
                        .map(AgentContextPack.PageObjectCapability::qualifiedName).toList());
        assertTrue(manifest.classes().stream().flatMap(value -> value.methods().stream())
                .anyMatch(value -> value.declaringType().equals("benchmark.LoginPage")
                        && value.name().equals("invalidPasswordShowsExpectedError")
                        && value.parameters().stream().map(AgentContextPack.CapabilityParameter::type).toList()
                        .equals(List.of("String", "String"))));

        PageObjectApiReferenceValidator validator =
                new PageObjectApiReferenceValidator(manifest, indexedFingerprint);

        String direct = junitClass("new LoginPage().invalidPasswordShowsExpectedError(username, password);");
        assertPass(validator.verifySource(direct, List.of(LOGIN_API)), List.of(LOGIN_API), List.of());

        String nested = junitClass("assertTrue(new LoginPage()"
                + ".invalidPasswordShowsExpectedError(username, password));");
        assertPass(validator.verifySource(nested, List.of(LOGIN_API)), List.of(LOGIN_API), List.of());
        assertPass(validator.verifySource(nested, List.of()), List.of(LOGIN_API),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));

        assertFailure(validator.verifySource(junitClass("new LoginPage().missingMethod();"), List.of()),
                PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST, "KNOWN_PAGE_OBJECT");
        assertFailure(validator.verifySource(junitClass(
                        "new LoginPage().invalidPasswordShowsExpectedError(7, password);"), List.of()),
                PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH, "KNOWN_PAGE_OBJECT");
        assertFailure(validator.verifySource(junitClass(
                        "new MissingLoginPage().invalidPasswordShowsExpectedError(username, password);"), List.of()),
                PageObjectApiReferenceValidator.RECEIVER_UNRESOLVED, "UNRESOLVED");

        var junitVerification = validator.verifySource(nested, List.of());
        assertPass(junitVerification, List.of(LOGIN_API),
                List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));
        assertFalse(junitVerification.verifiedReferences().stream().anyMatch(value -> value.contains("assertTrue")),
                "JUnit assertions are not Page Object API calls");

        assertTrue(manifest.classes().stream().flatMap(value -> value.methods().stream())
                        .anyMatch(value -> value.declaringType().equals("benchmark.BaseLoginPage")
                                && value.name().equals("inheritedReady")),
                "ContextSlicer must materialize the indexed inherited public method");
        assertPass(validator.verifySource(derivedJunitClass(
                        "new DerivedLoginPage().inheritedReady();"), List.of()),
                List.of(INHERITED_API), List.of(PageObjectApiReferenceValidator.DECLARATION_OMITTED));

        assertFailure(new PageObjectApiReferenceValidator(manifest, indexedFingerprint + "-stale")
                        .verifySource(nested, List.of(LOGIN_API)),
                PageObjectApiReferenceValidator.MANIFEST_STALE, "NOT_APPLICABLE");
        assertFailure(validator.verifySource("package benchmark; class Broken { void test( {", List.of()),
                PageObjectApiReferenceValidator.SOURCE_PARSE_FAILED, "NOT_APPLICABLE");

        String exactLoginPage = Files.readString(sourceRoot.resolve("example/LoginPage.java"));
        AtomicInteger compileInvocations = new AtomicInteger();
        var validVerification = validator.verifySource(nested, List.of(LOGIN_API));
        assertTrue(validVerification.valid(), validVerification.reason());
        var policy = new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults())
                .validate(Path.of("src/test/java/benchmark/GeneratedFixtureTest.java"), nested);
        assertTrue(policy.accepted(), () -> "AST policy violations: " + policy.violations());
        TargetedJavaCompiler.CompilationResult compilation = compile(
                nested, exactLoginPage, compileInvocations);
        assertTrue(compilation.successful(), () -> "compiler diagnostics: " + compilation.diagnostics());
        assertTrue(compilation.generatedClasses() >= 2);
        assertEquals(1, compileInvocations.get());

        String unauthorized = junitClass("new LoginPage().missingMethod();");
        var unauthorizedVerification = validator.verifySource(unauthorized, List.of());
        assertFailure(unauthorizedVerification, PageObjectApiReferenceValidator.METHOD_NOT_IN_MANIFEST,
                "KNOWN_PAGE_OBJECT");
        if (unauthorizedVerification.valid()) {
            compile(unauthorized, exactLoginPage, compileInvocations);
        }
        assertEquals(1, compileInvocations.get(),
                "an unauthorized Page Object API must stop before the compiler boundary");
    }

    private Path prepareProjectFromCertificationFixture() throws Exception {
        Path fixture = repositoryRoot().resolve(
                "scripts/fixtures/s15-external-consumer/src/test/java/example/LoginPage.java");
        byte[] fixtureBytes = Files.readAllBytes(fixture);
        Path sourceRoot = project.resolve("src/test/java");
        Path loginPage = sourceRoot.resolve("example/LoginPage.java");
        Files.createDirectories(loginPage.getParent());
        Files.write(loginPage, fixtureBytes);
        assertEquals(List.of(fixtureBytes.length, java.util.Arrays.hashCode(fixtureBytes)),
                List.of((int) Files.size(loginPage), java.util.Arrays.hashCode(Files.readAllBytes(loginPage))),
                "the indexed LoginPage must be an exact copy of the certification fixture");

        Path derived = sourceRoot.resolve("benchmark/DerivedLoginPage.java");
        Files.createDirectories(derived.getParent());
        Files.writeString(derived, """
                package benchmark;
                import org.openqa.selenium.By;
                class BaseLoginPage {
                    public boolean inheritedReady() { return true; }
                }
                public final class DerivedLoginPage extends BaseLoginPage {
                    private final By marker = By.id("derived-login");
                    public DerivedLoginPage() {}
                }
                """);
        Files.writeString(project.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion><artifactId>source-api-fixture</artifactId></project>");
        new StudioWorkspaceStore(project).write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(applicationModel())), "fixture-bootstrap");
        return sourceRoot;
    }

    private AgentContextPack prepareThroughStudio(Path sourceRoot) throws Exception {
        CapturingGateway gateway = new CapturingGateway();
        try (TestEngineeringStudioService service = new TestEngineeringStudioService(
                new TestEngineeringStudioService.Configuration(project, List.of(sourceRoot), List.of(),
                        List.of("src/test/java"), "fixture"), gateway, null)) {
            String runId = service.createRequirement("Create a login test and a derived login test using "
                    + "LoginPage.invalidPasswordShowsExpectedError and DerivedLoginPage.inheritedReady");
            service.generatePlan(runId);
        }
        assertNotNull(gateway.context);
        assertEquals(AgentContextPack.ManifestCompleteness.COMPLETE,
                gateway.context.pageObjectCapabilities().completeness());
        return gateway.context;
    }

    private static String junitClass(String statement) {
        return """
                package benchmark;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                final class GeneratedFixtureTest {
                    private final String username = "user";
                    private final String password = "invalid";
                    @Test void invalidPassword() { %s }
                }
                """.formatted(statement);
    }

    private static String derivedJunitClass(String statement) {
        return """
                package benchmark;
                import org.junit.jupiter.api.Test;
                final class GeneratedDerivedFixtureTest {
                    @Test void inheritedCapability() { %s }
                }
                """.formatted(statement);
    }

    private static TargetedJavaCompiler.CompilationResult compile(String generatedSource, String exactLoginPage,
                                                                   AtomicInteger invocations) {
        invocations.incrementAndGet();
        Path loginPath = Path.of("src/test/java/benchmark/LoginPage.java");
        Path generatedPath = Path.of("src/test/java/benchmark/GeneratedFixtureTest.java");
        List<TargetedJavaCompiler.SourceUnit> units = List.of(
                new TargetedJavaCompiler.SourceUnit(loginPath, "benchmark.LoginPage", exactLoginPage,
                        ArtifactEnvelope.digest(exactLoginPage)),
                new TargetedJavaCompiler.SourceUnit(generatedPath, "benchmark.GeneratedFixtureTest", generatedSource,
                        ArtifactEnvelope.digest(generatedSource)));
        return new TargetedJavaCompiler().compile(
                new TargetedJavaCompiler.CompilationRequest(units, 17,
                        System.getProperty("java.class.path"), 50),
                Map.of(loginPath, exactLoginPage, generatedPath, generatedSource));
    }

    private static void assertPass(PageObjectApiReferenceValidator.SourceVerification verification,
                                   List<String> references, List<String> diagnostics) {
        assertTrue(verification.valid(), verification.reason());
        assertEquals("", verification.reason());
        assertEquals(references, verification.verifiedReferences());
        assertEquals(diagnostics, verification.diagnostics());
        assertEquals("$.payload.sourcePatch", verification.location());
        assertEquals(Map.of(), verification.safeMetadata());
    }

    private static void assertFailure(PageObjectApiReferenceValidator.SourceVerification verification,
                                      String reason, String receiverCategory) {
        assertFalse(verification.valid());
        assertEquals(reason, verification.reason());
        assertEquals("$.payload.sourcePatch", verification.location());
        assertTrue(verification.safeMetadata().keySet().containsAll(SAFE_METADATA_KEYS));
        assertEquals("SOURCE_API_VERIFICATION", verification.safeMetadata().get("validationStage"));
        assertEquals(receiverCategory, verification.safeMetadata().get("receiverCategory"));
        assertTrue(SAFE_METADATA_KEYS.stream().map(verification.safeMetadata()::get)
                        .allMatch(value -> value != null && value.matches("[A-Z][A-Z0-9_]*")),
                "diagnostics must contain only bounded static categories");
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve(
                    "scripts/fixtures/s15-external-consumer/src/test/java/example/LoginPage.java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root with the certification fixture was not found");
    }

    private static ApplicationModel applicationModel() {
        List<ApplicationModel.PageModel> pages = List.of(
                page("login-page", "Login", List.of(
                        element("username", "Username", "username", ApplicationModel.ElementType.INPUT),
                        element("password", "Password", "password", ApplicationModel.ElementType.INPUT),
                        element("login-button", "LoginButton", "old-login-button", ApplicationModel.ElementType.BUTTON),
                        element("login-error", "Error", "error", ApplicationModel.ElementType.BUTTON))),
                page("derived-login-page", "Derived Login", List.of(
                        element("derived-marker", "DerivedMarker", "derived-login",
                                ApplicationModel.ElementType.BUTTON))));
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION, "fixture-app", "fixture", "test",
                Instant.EPOCH, pages, List.of(), List.of(),
                new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,
                        2, 2, 0, 5, 5, 0, 5, 0, false), List.of(), provenance());
    }

    private static ApplicationModel.PageModel page(String id, String name,
                                                    List<ApplicationModel.ElementModel> elements) {
        return new ApplicationModel.PageModel(id, name,
                new ApplicationModel.PageIdentity("/" + id, null, name,
                        elements.stream().map(ApplicationModel.ElementModel::elementId).toList(),
                        "page-" + id, ApplicationModel.EvidenceSource.OBSERVED),
                List.of(name), List.of("page-" + id), List.of(), List.of(), elements,
                List.of(), List.of(), provenance(), List.of());
    }

    private static ApplicationModel.ElementModel element(String id, String name, String selector,
                                                          ApplicationModel.ElementType type) {
        var projection = new ApplicationModel.SelectorProjection("id", selector, "candidate-" + id,
                "VERIFIED_IN_SCOPE", "SAME_TARGET", true, List.of("STABLE"), 1,
                List.of(), List.of(), ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        return new ApplicationModel.ElementModel(id, name, type, "div", name, name,
                List.of(ApplicationModel.Action.CLICK), projection, List.of(),
                ApplicationModel.SelectorQuality.VERIFIED, null, "fp-" + id, provenance(), List.of());
    }

    private static ApplicationModel.Provenance provenance() {
        return ApplicationModel.Provenance.observed("fixture", "source-api-verification", List.of("fixture"));
    }

    private static ContractHeader ready() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }

    private static final class CapturingGateway implements StudioWorkflowGateway {
        private AgentContextPack context;

        @Override
        public void prepare(String runId, TestEngineeringRequest request, AgentContextPack context) {
            this.context = context;
        }

        @Override
        public TestPlan generatePlan(String runId, String requirement) {
            return new TestPlan(ready(), List.of());
        }

        @Override
        public io.github.testlens.application.tooling.ai.TestImplementationProposal generateImplementation(String runId) {
            throw new AssertionError("Implementer is outside this deterministic source-verification test");
        }

        @Override
        public WorkflowReport run(String runId) throws AgentExecutor.AgentExecutionException {
            throw new AssertionError("Execution is outside this deterministic source-verification test");
        }
    }
}
