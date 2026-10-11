package io.github.testlens.studio;

import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.PageObjectManifestReadiness;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest;
import io.github.testlens.application.tooling.ai.workflow.WorkflowReport;
import io.github.testlens.application.tooling.ai.workflow.runner.PageObjectApiReferenceValidator;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import io.github.testlens.studio.projection.StudioProjections.Capability;
import io.github.testlens.studio.project.ProjectDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudioManifestReadinessIntegrationTest {
    private static final String REQUIREMENT =
            "Create a JUnit 5 test using LoginPage.invalidPasswordShowsExpectedError(String,String)";
    private static final String API =
            "benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)";

    @TempDir
    Path project;

    @Test
    void exactCertificationFixtureKeepsOneFingerprintAcrossPreflightContextAndSourceVerification()
            throws Exception {
        Path sourceRoot = prepareExactFixture();
        CapturingGateway gateway = new CapturingGateway();

        AgentContextPack context = prepareAndPlan(sourceRoot, gateway);
        ExistingProjectIndex current = index(sourceRoot);
        var manifest = context.pageObjectCapabilities();
        String requiredMethodId = manifest.classes().stream()
                .filter(value -> value.qualifiedName().equals("benchmark.LoginPage"))
                .flatMap(value -> value.methods().stream())
                .filter(value -> value.name().equals("invalidPasswordShowsExpectedError"))
                .map(AgentContextPack.MethodCapability::methodId)
                .findFirst().orElseThrow();
        PageObjectManifestReadiness.Assessment readiness = PageObjectManifestReadiness.assess(
                manifest, current.projectFingerprint(), Set.of(requiredMethodId));

        assertEquals(ExistingProjectIndex.Completeness.COMPLETE, current.completeness());
        assertEquals(AgentContextPack.ManifestCompleteness.COMPLETE, manifest.completeness());
        assertEquals(PageObjectManifestReadiness.Status.READY, readiness.status());
        assertEquals(current.projectFingerprint(), manifest.sourceFingerprint());
        assertEquals(current.projectFingerprint(), manifest.provenance().indexFingerprint());
        assertEquals("COMPLETE", manifest.provenance().sourceIndexCompleteness());
        assertFalse(manifest.provenance().sourcesChanged());
        assertTrue(manifest.provenance().partialReasons().isEmpty());
        assertEquals(1, gateway.prepareCalls.get());
        assertEquals(1, gateway.architectCalls.get());

        String generated = generatedSource();
        var verification = new PageObjectApiReferenceValidator(manifest, current.projectFingerprint())
                .verifySource(generated, List.of(API));
        assertTrue(verification.valid(), verification.reason());
        assertEquals(List.of(API), verification.verifiedReferences());

        PageObjectManifestReadiness.Assessment stale = PageObjectManifestReadiness.assess(
                manifest, current.projectFingerprint() + "-changed", Set.of(requiredMethodId));
        assertEquals(PageObjectManifestReadiness.Status.MANIFEST_STALE, stale.status());
        assertEquals("PAGE_OBJECT_MANIFEST_STALE", stale.reasonCode());
        assertEquals("TRUE", stale.safeMetadata().get("manifestSourcesChanged"),
                "the compared fingerprint deterministically marks a between-stage source change");
        assertEquals(0, gateway.implementerCalls.get());
        assertEquals(0, gateway.pipelineCalls.get());
    }

    @Test
    void unresolvedSelectedInheritanceStopsBeforeArchitectAndPipeline() throws Exception {
        Path sourceRoot = prepareExactFixture();
        Path page = sourceRoot.resolve("example/LoginPage.java");
        Files.writeString(page, Files.readString(page).replace(
                "public final class LoginPage {", "public final class LoginPage extends MissingBasePage {"));
        CapturingGateway gateway = new CapturingGateway();

        PageObjectCapabilityException failure = assertPreflightFailure(sourceRoot, gateway);

        assertEquals("PAGE_OBJECT_REQUIRED_API_UNRESOLVED", failure.reasonCode());
        assertEquals("REQUIRED_API_UNRESOLVED", failure.diagnosticMetadata().get("manifestReason"));
        assertBlockedBeforeAgentsAndPipeline(gateway);
    }

    @Test
    void missingSupportedConstructorStopsBeforeArchitectAndPipeline() throws Exception {
        Path sourceRoot = prepareExactFixture();
        Path page = sourceRoot.resolve("example/LoginPage.java");
        String source = Files.readString(page)
                .replace("public LoginPage(){", "private LoginPage(){")
                .replace("public LoginPage(WebDriver driver){", "private LoginPage(WebDriver driver){");
        Files.writeString(page, source);
        CapturingGateway gateway = new CapturingGateway();

        PageObjectCapabilityException failure = assertPreflightFailure(sourceRoot, gateway);

        assertEquals("PAGE_OBJECT_CONSTRUCTION_UNRESOLVED", failure.reasonCode());
        assertEquals("UNRESOLVED", failure.diagnosticMetadata().get("constructorStatus"));
        assertBlockedBeforeAgentsAndPipeline(gateway);
    }

    @Test
    void defaultManifestMethodLimitStopsBeforeArchitectAndPipeline() throws Exception {
        Path sourceRoot = prepareExactFixture();
        Path page = sourceRoot.resolve("example/LoginPage.java");
        String source = Files.readString(page);
        int closingBrace = source.lastIndexOf('}');
        StringBuilder methods = new StringBuilder();
        for (int index = 0; index < 161; index++) {
            methods.append(" public boolean extraCapability").append(index).append("(){ return true; }\n");
        }
        Files.writeString(page, source.substring(0, closingBrace) + methods + source.substring(closingBrace));
        CapturingGateway gateway = new CapturingGateway();

        PageObjectCapabilityException failure = assertPreflightFailure(sourceRoot, gateway);

        assertEquals("PAGE_OBJECT_MANIFEST_TRUNCATED", failure.reasonCode());
        assertEquals("TRUE", failure.diagnosticMetadata().get("manifestTruncated"));
        assertEquals("160", failure.diagnosticMetadata().get("manifestMaxMethods"));
        assertBlockedBeforeAgentsAndPipeline(gateway);
    }

    @Test
    void sourceChangeAfterPlanningStopsBeforeImplementerAndPipeline() throws Exception {
        Path sourceRoot = prepareExactFixture();
        CapturingGateway gateway = new CapturingGateway();
        String runId;
        try (TestEngineeringStudioService service = preparedService(sourceRoot, gateway)) {
            runId = service.createRequirement(REQUIREMENT);
            service.generatePlan(runId);
            Path page = sourceRoot.resolve("example/LoginPage.java");
            Files.writeString(page, Files.readString(page).replace("old-login-button", "changed-login-button"));

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> service.generateImplementation(runId));

            assertTrue(failure.getMessage().contains("stale"));
            assertEquals("STALE", service.workflow(runId).freshness());
        }
        assertEquals(1, gateway.architectCalls.get());
        assertEquals(0, gateway.implementerCalls.get());
        assertEquals(0, gateway.pipelineCalls.get());
    }

    @Test
    void sourceChangeAfterRequirementIsReindexedAndBlockedBeforeArchitect() throws Exception {
        Path sourceRoot = prepareExactFixture();
        CapturingGateway gateway = new CapturingGateway();
        try (TestEngineeringStudioService service = preparedService(sourceRoot, gateway)) {
            String runId = service.createRequirement(REQUIREMENT);
            Path page = sourceRoot.resolve("example/LoginPage.java");
            String changed = Files.readString(page)
                    .replace("public LoginPage(){", "private LoginPage(){")
                    .replace("public LoginPage(WebDriver driver){", "private LoginPage(WebDriver driver){");
            Files.writeString(page, changed);

            PageObjectCapabilityException failure = assertThrows(PageObjectCapabilityException.class,
                    () -> service.generatePlan(runId));

            assertEquals("PAGE_OBJECT_CONSTRUCTION_UNRESOLVED", failure.reasonCode());
        }
        assertBlockedBeforeAgentsAndPipeline(gateway);
    }

    private PageObjectCapabilityException assertPreflightFailure(Path sourceRoot, CapturingGateway gateway)
            throws Exception {
        try (TestEngineeringStudioService service = preparedService(sourceRoot, gateway)) {
            String runId = service.createRequirement(REQUIREMENT);
            return assertThrows(PageObjectCapabilityException.class, () -> service.generatePlan(runId));
        }
    }

    private AgentContextPack prepareAndPlan(Path sourceRoot, CapturingGateway gateway) throws Exception {
        try (TestEngineeringStudioService service = preparedService(sourceRoot, gateway)) {
            String runId = service.createRequirement(REQUIREMENT);
            service.generatePlan(runId);
        }
        assertNotNull(gateway.context);
        return gateway.context;
    }

    private TestEngineeringStudioService preparedService(Path sourceRoot, CapturingGateway gateway)
            throws Exception {
        var descriptor = new ProjectDiscovery().discover(project);
        TestEngineeringStudioService service = new TestEngineeringStudioService(
                new TestEngineeringStudioService.Configuration(project, descriptor.sourceRoots(),
                        descriptor.classpathEntries(),
                        List.of("src/test/java"), "certification-fixture"),
                gateway, new MappingDriverFixture()::driver, project.resolve("workspace"));
        service.attachCapabilities(new Capability("AVAILABLE", "fixture browser"),
                new Capability("AVAILABLE", "counted agent"), new Capability("AVAILABLE", "JDK"));
        service.scanProject();
        service.mapApplication(ApplicationMapperOptions.Mode.CURRENT_PAGE);
        service.correlate();
        return service;
    }

    private Path prepareExactFixture() throws Exception {
        Path fixtureProject = repositoryRoot().resolve("scripts/fixtures/s15-external-consumer");
        Path fixtureRoot = fixtureProject.resolve("src/test/java");
        Path fixture = fixtureRoot.resolve("example/LoginPage.java");
        byte[] expected = Files.readAllBytes(fixture);
        try (var files = Files.walk(fixtureProject)) {
            for (Path source : files.filter(Files::isRegularFile).toList()) {
                Path target = project.resolve(fixtureProject.relativize(source));
                Files.createDirectories(target.getParent());
                Files.copy(source, target);
            }
        }
        Path sourceRoot = project.resolve("src/test/java");
        Path target = sourceRoot.resolve("example/LoginPage.java");
        assertEquals(List.of(expected.length, java.util.Arrays.hashCode(expected)),
                List.of((int) Files.size(target), java.util.Arrays.hashCode(Files.readAllBytes(target))));
        return sourceRoot;
    }

    private ExistingProjectIndex index(Path sourceRoot) {
        var descriptor = new ProjectDiscovery().discover(project);
        return new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                project, descriptor.sourceRoots(), descriptor.classpathEntries()));
    }

    private static String generatedSource() {
        return """
                package benchmark;
                import org.junit.jupiter.api.Test;
                import static org.junit.jupiter.api.Assertions.assertTrue;
                final class GeneratedManifestReadinessTest {
                    @Test void invalidPassword() {
                        assertTrue(new LoginPage().invalidPasswordShowsExpectedError("user", "invalid"));
                    }
                }
                """;
    }

    private static void assertBlockedBeforeAgentsAndPipeline(CapturingGateway gateway) {
        assertEquals(0, gateway.prepareCalls.get());
        assertEquals(0, gateway.architectCalls.get());
        assertEquals(0, gateway.implementerCalls.get());
        assertEquals(0, gateway.pipelineCalls.get());
    }

    private static Path repositoryRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isRegularFile(current.resolve(
                    "scripts/fixtures/s15-external-consumer/src/test/java/example/LoginPage.java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Repository root with certification fixture was not found");
    }

    private static ContractHeader ready() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("deterministic fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }

    private static final class CapturingGateway implements StudioWorkflowGateway {
        private final AtomicInteger prepareCalls = new AtomicInteger();
        private final AtomicInteger architectCalls = new AtomicInteger();
        private final AtomicInteger implementerCalls = new AtomicInteger();
        private final AtomicInteger pipelineCalls = new AtomicInteger();
        private AgentContextPack context;

        @Override
        public void prepare(String runId, TestEngineeringRequest request, AgentContextPack context) {
            prepareCalls.incrementAndGet();
            this.context = context;
        }

        @Override
        public TestPlan generatePlan(String runId, String requirement) {
            architectCalls.incrementAndGet();
            return new TestPlan(ready(), List.of());
        }

        @Override
        public TestImplementationProposal generateImplementation(String runId) {
            implementerCalls.incrementAndGet();
            throw new AssertionError("Implementer must not be reached by readiness tests");
        }

        @Override
        public WorkflowReport run(String runId) {
            pipelineCalls.incrementAndGet();
            throw new AssertionError("Compile/execute pipeline must not be reached by readiness tests");
        }
    }

    private static final class MappingDriverFixture {
        private final Map<String, WebElement> byId = new LinkedHashMap<>();
        private final Map<WebElement, String> ids = new IdentityHashMap<>();

        private MappingDriverFixture() {
            element("username", "input", "text", "textbox", "Username");
            element("password", "input", "password", "textbox", "Password");
            element("old-login-button", "button", "button", "button", "Login");
            element("error", "div", null, "alert", "Invalid password error");
            element("unmapped-help", "a", null, "link", "Help");
        }

        private WebDriver driver() {
            return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, arguments) ->
                            switch (method.getName()) {
                                case "getCurrentUrl" -> "https://example.test/login";
                                case "getTitle" -> "Login";
                                case "getWindowHandle" -> "window-1";
                                case "executeScript" -> executeScript((String) arguments[0],
                                        arguments.length > 1 ? (Object[]) arguments[1] : new Object[0]);
                                case "findElements" -> find((By) arguments[0]);
                                case "findElement" -> find((By) arguments[0]).stream().findFirst().orElse(null);
                                case "quit", "close" -> null;
                                default -> defaultValue(method.getReturnType());
                            });
        }

        private Object executeScript(String script, Object[] arguments) {
            if (script.contains("const max=arguments[0]")) {
                List<Map<String, Object>> elements = new ArrayList<>();
                byId.forEach((id, element) -> elements.add(discovered(id, element)));
                return Map.of("elements", elements, "regions", List.of(), "discovered", elements.size() + 1,
                        "truncated", true, "closedShadowPossible", 0, "shadowRoots", 0,
                        "shadowDepthReached", 0);
            }
            WebElement target = java.util.Arrays.stream(arguments).filter(WebElement.class::isInstance)
                    .map(WebElement.class::cast).findFirst().orElse(null);
            return selectorSnapshot(target == null ? "username" : ids.get(target));
        }

        private Map<String, Object> discovered(String id, WebElement element) {
            String tag = id.equals("username") || id.equals("password") ? "input"
                    : id.equals("old-login-button") ? "button" : id.equals("unmapped-help") ? "a" : "div";
            String type = id.equals("password") ? "password" : tag.equals("input") ? "text"
                    : tag.equals("button") ? "button" : null;
            String role = tag.equals("input") ? "textbox" : tag.equals("button") ? "button"
                    : tag.equals("a") ? "link" : "alert";
            String label = switch (id) {
                case "username" -> "Username";
                case "password" -> "Password";
                case "old-login-button" -> "Login";
                case "unmapped-help" -> "Help";
                default -> "Invalid password error";
            };
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("element", element);
            value.put("shadow", false);
            value.put("tag", tag);
            value.put("type", type);
            value.put("role", role);
            value.put("label", label);
            value.put("accessibleName", label);
            value.put("text", label);
            value.put("disabled", false);
            value.put("regionKey", "");
            value.put("structuralHint", "main|" + tag + "|" + id);
            value.put("testAttributes", Map.of());
            return value;
        }

        private Map<String, Object> selectorSnapshot(String id) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("tagName", id.equals("username") || id.equals("password") ? "input" : "button");
            snapshot.put("id", id);
            snapshot.put("name", id);
            snapshot.put("classList", List.of());
            snapshot.put("testAttributes", Map.of());
            snapshot.put("ariaLabel", "");
            snapshot.put("ancestors", List.of());
            snapshot.put("ownedNodes", List.of());
            snapshot.put("instrumentationNodesExcluded", 0);
            snapshot.put("truncated", false);
            return snapshot;
        }

        private List<WebElement> find(By selector) {
            String rendered = selector.toString();
            return byId.entrySet().stream().filter(entry -> rendered.contains(entry.getKey()))
                    .map(Map.Entry::getValue).toList();
        }

        private void element(String id, String tag, String type, String role, String label) {
            WebElement element = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebElement.class}, (proxy, method, arguments) -> switch (method.getName()) {
                        case "getText", "getAccessibleName" -> label;
                        case "getAriaRole" -> role;
                        case "getTagName" -> tag;
                        case "isDisplayed", "isEnabled" -> true;
                        case "isSelected" -> false;
                        case "getAttribute", "getDomAttribute", "getDomProperty" -> {
                            String name = (String) arguments[0];
                            yield name.equals("id") ? id : name.equals("type") ? type : null;
                        }
                        case "equals" -> proxy == arguments[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "fixture-element:" + id;
                        default -> defaultValue(method.getReturnType());
                    });
            byId.put(id, element);
            ids.put(element, id);
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0F;
            if (type == double.class) return 0D;
            return null;
        }
    }
}
