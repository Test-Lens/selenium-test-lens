package io.github.testlens.browser;

import com.sun.net.httpserver.HttpServer;
import io.github.testlens.application.mapper.ApplicationMapper;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.AgentTask;
import io.github.testlens.application.tooling.ai.ContextSlicer;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.PromptPackRenderer;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator;
import io.github.testlens.application.tooling.ai.workflow.ScriptedAgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler;
import io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow;
import io.github.testlens.application.tooling.ai.workflow.WorkflowPolicy;
import io.github.testlens.application.tooling.source.CorrelationOverrides;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.PageObjectCorrelator;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real-browser proof that mapped elements can be bounded to an existing hand-written Page Object API. */
class ExistingProjectCorrelationBrowserIT {
    @Test
    void mapsLoginAndBuildsARelevantSourceAwareContextWithoutSelectorValues() throws Exception {
        try (LoginFixture fixture = LoginFixture.start()) {
            WebDriver driver = BrowserTestHarness.createDriver();
            try {
                driver.get(fixture.url());
                ApplicationMapper mapper = ApplicationMapper.start(driver,
                        ApplicationMapperOptions.builder("Existing project fixture").build());
                String pageId = mapper.observe().pageId();
                ApplicationModel model = mapper.model();
                ApplicationModel.PageModel login = model.pages().stream()
                        .filter(page -> page.pageId().equals(pageId)).findFirst().orElseThrow();
                List<String> requiredElements = login.elements().stream()
                        .filter(element -> List.of("usernameInput", "passwordInput", "loginSubmitButton")
                                .contains(element.semanticName()))
                        .map(ApplicationModel.ElementModel::elementId).toList();
                assertEquals(3, requiredElements.size(), login.elements().toString());

                long indexingStarted = System.nanoTime();
                ExistingProjectIndex source = indexExistingProject();
                long indexingNanos = System.nanoTime() - indexingStarted;
                assertFixtureShape(source);
                long correlationStarted = System.nanoTime();
                PageObjectCorrelation correlation = new PageObjectCorrelator()
                        .correlate(model, source, CorrelationOverrides.none());
                long correlationNanos = System.nanoTime() - correlationStarted;
                List<PageObjectCorrelation.ElementCorrelation> loginMatches = correlation.elements().stream()
                        .filter(match -> requiredElements.contains(match.applicationElementId())).toList();
                assertEquals(3, loginMatches.size());
                assertTrue(loginMatches.stream().allMatch(match -> match.state() == PageObjectCorrelation.State.STRONG),
                        loginMatches.toString());
                assertTrue(loginMatches.stream().allMatch(match ->
                        match.evidence().contains(PageObjectCorrelation.Evidence.NORMALIZED_SELECTOR_MATCH)
                                && match.evidence().contains(PageObjectCorrelation.Evidence.SAME_PAGE_IDENTITY)));

                long slicingStarted = System.nanoTime();
                AgentContextPack context = sourceAwareContext(model, pageId, requiredElements, source, correlation);
                long slicingNanos = System.nanoTime() - slicingStarted;
                assertEquals(List.of("LoginPage"), context.source().classes().stream()
                        .map(value -> simpleName(value.qualifiedName())).toList());
                assertEquals(List.of("enterPassword(String)", "enterUsername(String)",
                                "loginAs(String, String)"),
                        context.source().methods().stream().map(AgentContextPack.PageObjectMethodContext::signature)
                                .sorted().toList());
                assertEquals(List.of("invalidPassword"), context.source().tests().stream()
                        .map(test -> source.methods().stream().filter(method -> method.id().equals(test.methodId()))
                                .findFirst().orElseThrow().name()).toList());
                assertTrue(context.included().stream().anyMatch(decision ->
                        decision.kind().equals("PAGE_OBJECT_METHOD")
                                && decision.reason().contains("source declaration")));
                assertTrue(context.excluded().stream().anyMatch(decision ->
                        decision.kind().equals("PAGE_OBJECT_CLASS")
                                || decision.reason().contains("Not required")));

                String rendered = new PromptPackRenderer().render(
                        PromptPackRenderer.Role.TEST_IMPLEMENTER, context, 1_000_000);
                assertFalse(rendered.contains("[data-testid='login-submit']"));
                assertFalse(rendered.contains("[data-testid='customer-search']"));
                assertFalse(rendered.contains("CustomersPage"));
                assertTrue(rendered.contains("sha256:"), "selector identity must be a source-safe fingerprint");
                int applicationModelBytes = new io.github.testlens.application.tooling.json.ApplicationModelJson()
                        .write(model).length;
                int pageObjectIndexBytes = sourceProjection(source).getBytes(StandardCharsets.UTF_8).length;
                int usageGraphBytes = source.edges().toString().getBytes(StandardCharsets.UTF_8).length;
                int contextBytes = rendered.getBytes(StandardCharsets.UTF_8).length;
                System.out.printf("EXISTING_PROJECT_CONTEXT browser=%s files=%d pageObjects=%d declarations=%d "
                                + "usageEdges=%d indexingMs=%.3f correlationMs=%.3f slicingMs=%.3f "
                                + "applicationModelBytes=%d pageObjectIndexBytes=%d usageGraphBytes=%d contextBytes=%d%n",
                        BrowserTestHarness.browserName(),
                        source.metrics().files(), source.classes().stream().filter(value ->
                                value.classification() == ExistingProjectIndex.ClassClassification.PAGE_OBJECT).count(),
                        source.elements().size(), source.edges().size(), indexingNanos / 1_000_000d,
                        correlationNanos / 1_000_000d, slicingNanos / 1_000_000d, applicationModelBytes,
                        pageObjectIndexBytes, usageGraphBytes, contextBytes);
            } finally {
                driver.quit();
            }
        }
    }

    @Test
    void scriptedWorkflowCompilesAndExecutesPageObjectOnlyTestThenApprovesReview() throws Exception {
        String runId = "browser-existing-project";
        String generatedSource = """
                package io.github.testlens.browser;
                public final class GeneratedInvalidPasswordTest {
                    public static boolean execute(ExistingProjectCorrelationBrowserIT.RuntimeLoginPage page,
                                                  ExistingProjectCorrelationBrowserIT.TestCredentials credentials) {
                        page.loginAs(credentials.username(), credentials.password());
                        return page.loginErrorVisible();
                    }
                }
                """;
        TestEngineeringRequest request = workflowRequest();
        ContractHeader ready = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("bounded existing Page Object context"), List.of(), ContractHeader.Confidence.AI_PROPOSED);
        TestPlan testPlan = new TestPlan(ready, List.of(new TestPlan.TestScenario("invalid-password",
                "Invalid password is rejected", TestPlan.Priority.HIGH, List.of("Login page is open"),
                List.of(new TestPlan.TestStep(1, "CALL", "LOGIN", "LOGIN_SUBMIT", "INVALID_PASSWORD")),
                List.of(new TestPlan.ExpectedResult("LOGIN", "LOGIN_ERROR", "Error is visible", "runtime")),
                List.of("LOGIN"), List.of("LOGIN_ERROR"), List.of("LOGIN_SUBMIT"),
                List.of("INVALID_PASSWORD"), List.of("Authentication response"),
                TestPlan.CoverageDisposition.EXTEND_EXISTING_TEST, List.of("LoginTest.invalidPassword"), List.of())));
        TestImplementationProposal implementationProposal = new TestImplementationProposal(ready,
                "invalid-password", generatedSource, TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                List.of("RuntimeLoginPage.loginAs(String,String)"), List.of());
        CodeReviewResult reviewResult = new CodeReviewResult(ready, CodeReviewResult.Verdict.APPROVE,
                List.of("implementation", "execution"), List.of());
        ScriptedAgentExecutor agents = new ScriptedAgentExecutor(List.of(
                new AgentExecutor.AgentResult("architect-1",
                        ArtifactEnvelope.create("plan", runId, List.of(), 1, testPlan)),
                new AgentExecutor.AgentResult("implementer-1",
                        ArtifactEnvelope.create("implementation", runId, List.of("plan"), 1, implementationProposal)),
                new AgentExecutor.AgentResult("reviewer-1",
                        ArtifactEnvelope.create("review", runId, List.of("execution"), 1, reviewResult))));
        TestEngineeringWorkflow workflow = new TestEngineeringWorkflow(WorkflowPolicy.defaults(),
                RedactionPolicy.defaults(), List.of("fixture-user-secret", "auth-secret-canary-8391"));
        TestEngineeringRun run = TestEngineeringRun.create(runId, request);
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.PREPARE_CONTEXT));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_PLAN));
        AgentExecutor.AgentResult plan = agents.execute(new AgentExecutor.AgentCommand(runId,
                AgentExecutor.Role.TEST_ARCHITECT, List.of(), Map.of("contract", "TestPlan only")));
        ArtifactEnvelope<?> planArtifact = plan.artifact();
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.PLAN_PRODUCED, planArtifact));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION));
        AgentExecutor.AgentResult implementation = agents.execute(new AgentExecutor.AgentCommand(runId,
                AgentExecutor.Role.TEST_IMPLEMENTER, List.of(planArtifact),
                Map.of("policy", "Page Objects only; no selectors")));
        TestImplementationProposal implementationPayload =
                (TestImplementationProposal) implementation.artifact().payload();
        GeneratedTestPolicyValidator validator = new GeneratedTestPolicyValidator(
                GeneratedTestPolicyValidator.Policy.defaults());
        Path generatedPath = Path.of("src/test/java/io/github/testlens/browser/GeneratedInvalidPasswordTest.java");
        assertTrue(validator.validate(request, generatedPath, implementationPayload.sourcePatch()).accepted());
        assertTrue(implementationPayload.sourcePatch().contains("RuntimeLoginPage"));
        assertFalse(implementationPayload.sourcePatch().contains("By."));
        assertFalse(implementationPayload.sourcePatch().contains("findElement"));
        assertFalse(validator.validate(request, generatedPath,
                "class Bad { Object x = By.id(\"error\"); }").accepted(),
                "raw-selector fallback must be rejected deterministically");
        ArtifactEnvelope<?> implementationArtifact = implementation.artifact();
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.IMPLEMENTATION_PRODUCED, implementationArtifact));

        TargetedJavaCompiler.CompilationResult compilation = new TargetedJavaCompiler().compile(
                new TargetedJavaCompiler.CompilationRequest(List.of(new TargetedJavaCompiler.SourceUnit(
                        generatedPath, "io.github.testlens.browser.GeneratedInvalidPasswordTest",
                        implementationPayload.sourcePatch(), ArtifactEnvelope.digest(""))), 17,
                        System.getProperty("java.class.path"), 20), Map.of());
        assertTrue(compilation.successful(), compilation.diagnostics().toString());
        assertTrue(compilation.generatedClasses() > 0);
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.COMPILE_SUCCEEDED));

        try (LoginFixture fixture = LoginFixture.start()) {
            WebDriver driver = BrowserTestHarness.createDriver();
            try {
                driver.get(fixture.url());
                TargetedTestExecutor executor = executionRequest -> {
                    assertEquals(runId, executionRequest.runId());
                    Class<?> generated = compilation.output().loadClass(
                            "io.github.testlens.browser.GeneratedInvalidPasswordTest", getClass().getClassLoader());
                    boolean visible;
                    try {
                        visible = (boolean) generated.getMethod("execute", RuntimeLoginPage.class, TestCredentials.class)
                                .invoke(null, new RuntimeLoginPage(driver),
                                        new TestCredentials("fixture-user-secret", "auth-secret-canary-8391"));
                    } catch (ReflectiveOperationException failure) {
                        throw new IllegalStateException("Compiled generated test could not be executed", failure);
                    }
                    return new TargetedTestExecutor.ExecutionResult(visible, 1,
                            List.of("runtime page-object action completed", "login error visible=" + visible));
                };
                TargetedTestExecutor.ExecutionResult execution = executor.execute(
                        new TargetedTestExecutor.ExecutionRequest(runId,
                                "io.github.testlens.browser.GeneratedInvalidPasswordTest",
                                List.of("invalid password"), Duration.ofSeconds(10)));
                assertTrue(execution.successful(), execution.boundedEvidence().toString());
                TestExecutionResult executionResult = new TestExecutionResult(ready, "invalid-password",
                        TestExecutionResult.Outcome.PASS,
                        execution.successful() ? TestExecutionResult.Outcome.PASS : TestExecutionResult.Outcome.FAIL,
                        "JUnit 5 targeted fixture", 0, List.of("compiled-generated-class"),
                        List.of("runtime-page-object-action"), List.of("login-error-visible"),
                        execution.boundedEvidence(), List.of(), List.of(),
                        execution.successful() ? null : "Generated test execution failed");
                ArtifactEnvelope<TestExecutionResult> executionArtifact = ArtifactEnvelope.create(
                        "execution", runId, List.of("implementation"), 1, executionResult);
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                        TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED, executionArtifact));
                assertEquals(TestEngineeringRun.State.REVIEW_REQUESTED, run.state());
                AgentExecutor.AgentResult review = agents.execute(new AgentExecutor.AgentCommand(runId,
                        AgentExecutor.Role.CODE_REVIEWER, List.of(implementationArtifact, executionArtifact),
                        Map.of("reject", "raw selectors, sleeps, retries, assertion weakening")));
                run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(
                        TestEngineeringWorkflow.EventType.REVIEW_APPROVED,
                        review.artifact()));
                assertEquals(TestEngineeringRun.State.SUCCESS, run.state());
                assertEquals(0, agents.remaining());
                assertEquals(1, run.metrics().compilations());
                assertEquals(1, run.metrics().executions());
                String retainedArtifacts = run.artifacts().toString();
                assertFalse(retainedArtifacts.contains("fixture-user-secret"));
                assertFalse(retainedArtifacts.contains("auth-secret-canary-8391"));
            } finally {
                driver.quit();
            }
        }
    }

    private ExistingProjectIndex indexExistingProject() throws Exception {
        URI resource = ExistingProjectCorrelationBrowserIT.class.getResource("/existing-project/").toURI();
        Path project = Path.of(resource);
        List<Path> classpath = Arrays.stream(System.getProperty("java.class.path").split(
                        java.util.regex.Pattern.quote(System.getProperty("path.separator"))))
                .filter(value -> !value.isBlank()).map(Path::of).toList();
        return new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(project,
                List.of(project.resolve("src/main/java"), project.resolve("src/test/java")), classpath));
    }

    private void assertFixtureShape(ExistingProjectIndex source) {
        assertTrue(source.classes().stream().anyMatch(value -> value.simpleName().equals("BasePage")
                && value.classification() == ExistingProjectIndex.ClassClassification.BASE_PAGE));
        assertTrue(source.classes().stream().anyMatch(value -> value.simpleName().equals("TopNavigation")
                && value.classification() == ExistingProjectIndex.ClassClassification.COMPONENT));
        assertTrue(source.classes().stream().anyMatch(value -> value.simpleName().equals("CustomersPage")));
        assertTrue(source.elements().stream().anyMatch(value -> value.name().equals("searchInput")));
        assertTrue(source.tests().stream().anyMatch(value -> value.framework() == ExistingProjectIndex.TestFramework.JUNIT5));
        assertTrue(source.tests().stream().anyMatch(value -> value.framework() == ExistingProjectIndex.TestFramework.TESTNG));
        ExistingProjectIndex.TestEntry loginTest = source.tests().stream()
                .filter(value -> source.methods().stream().anyMatch(method -> method.id().equals(value.methodId())
                        && method.name().equals("invalidPassword"))).findFirst().orElseThrow();
        ExistingProjectIndex.MethodEntry loginAs = source.methods().stream()
                .filter(value -> value.name().equals("loginAs")).findFirst().orElseThrow();
        assertTrue(source.edges().stream().anyMatch(edge -> edge.type() == ExistingProjectIndex.EdgeType.TEST_TO_METHOD
                && edge.fromId().equals(loginTest.id()) && edge.toId().equals(loginAs.id())));
        assertTrue(source.edges().stream().anyMatch(edge -> edge.type() == ExistingProjectIndex.EdgeType.METHOD_TO_DECLARATION
                && edge.fromId().equals(loginAs.id())));
    }

    private AgentContextPack sourceAwareContext(ApplicationModel model, String pageId,
                                                 List<String> requiredElements,
                                                 ExistingProjectIndex source,
                                                 PageObjectCorrelation correlation) {
        ContractHeader header = new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("real browser mapping", "deterministic source index"), List.of(),
                ContractHeader.Confidence.OBSERVED);
        AgentTask task = new AgentTask(header, AgentTask.TaskType.CREATE_TEST,
                "Add an invalid password login test", List.of(pageId), List.of(), requiredElements,
                List.of("Page Objects only", "No raw selectors"));
        return new ContextSlicer().slice(model, task, Map.of(), List.of("JUnit 5"),
                RedactionPolicy.defaults(), ContextSlicer.Limits.defaults(), source, correlation,
                ContextSlicer.SourceLimits.defaults());
    }

    private static String simpleName(String qualifiedName) {
        return qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    }

    private static String sourceProjection(ExistingProjectIndex source) {
        return source.schemaVersion() + "|" + source.projectFingerprint() + "|" + source.classes() + "|"
                + source.elements() + "|" + source.methods() + "|" + source.tests() + "|"
                + source.completeness() + "|" + source.limitations() + "|" + source.metrics();
    }

    private static TestEngineeringRequest workflowRequest() {
        return new TestEngineeringRequest(
                new TestEngineeringRequest.Requirement("Add an invalid password login test",
                        List.of("The login error is visible")),
                new TestEngineeringRequest.Scope(List.of("LoginPage.loginAs"), List.of("CustomersPage")),
                new TestEngineeringRequest.Framework("JUnit", "5", "junit-jupiter"),
                new TestEngineeringRequest.Target("browser-tests", "GeneratedInvalidPasswordTest",
                        "invalid-password"),
                List.of("src/test/java"), TestEngineeringRequest.ExecutionPolicy.TARGETED_AFTER_COMPILE);
    }

    public static final class RuntimeLoginPage {
        private final WebDriver driver;
        private final org.openqa.selenium.By username = org.openqa.selenium.By.id("username");
        private final org.openqa.selenium.By password = org.openqa.selenium.By.id("password");
        private final org.openqa.selenium.By loginButton =
                org.openqa.selenium.By.cssSelector("[data-testid='login-submit']");

        public RuntimeLoginPage(WebDriver driver) {
            this.driver = driver;
        }

        public void loginAs(String usernameValue, String passwordValue) {
            driver.findElement(username).sendKeys(usernameValue);
            driver.findElement(password).sendKeys(passwordValue);
            driver.findElement(loginButton).click();
        }

        public boolean loginErrorVisible() {
            return driver.findElement(org.openqa.selenium.By.id("login-error")).isDisplayed();
        }
    }

    /** Trusted-host fixture data; credential values never enter an agent or workflow artifact. */
    public record TestCredentials(String username, String password) { }

    private static final class LoginFixture implements AutoCloseable {
        private final HttpServer server;

        private LoginFixture(HttpServer server) {
            this.server = server;
        }

        static LoginFixture start() throws Exception {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            byte[] html = """
                    <!doctype html><html><head><title>Login</title></head><body>
                    <main aria-label="Login">
                      <label for="username">Username</label><input id="username" autocomplete="username">
                      <label for="password">Password</label><input id="password" type="password" autocomplete="current-password">
                      <button type="button" data-testid="login-submit">Log in</button>
                      <p id="login-error" role="alert" hidden>Invalid username or password</p>
                      <script>document.querySelector('[data-testid="login-submit"]').addEventListener('click',
                        () => document.getElementById('login-error').hidden = false);</script>
                    </main></body></html>
                    """.getBytes(StandardCharsets.UTF_8);
            server.createContext("/login", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, html.length);
                try (var body = exchange.getResponseBody()) {
                    body.write(html);
                }
            });
            server.start();
            return new LoginFixture(server);
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/login";
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
