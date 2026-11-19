package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec;
import io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class StudioPageObjectCapabilityContextTest {
    @TempDir Path root;

    @Test void realStudioPreparationSuppliesFingerprintBoundConstructiblePageObjectManifest() throws Exception {
        Path sourceRoot=project();persistModel();CapturingGateway gateway=new CapturingGateway();
        try(TestEngineeringStudioService service=service(sourceRoot,gateway)){
            String runId=service.createRequirement("Create a login test using LoginPage.invalidPasswordShowsExpectedError and assert its boolean result");
            service.generatePlan(runId);

            assertNotNull(gateway.context);AgentContextPack context=gateway.context;
            assertEquals(AgentContextPack.ManifestCompleteness.COMPLETE,context.pageObjectCapabilities().completeness());
            assertNotNull(context.pageObjectCapabilities().sourceFingerprint());
            assertFalse(context.pageObjectCapabilities().sourceFingerprint().isBlank());
            var capability=assertSingle(context.pageObjectCapabilities().classes());
            assertEquals("benchmark.LoginPage",capability.qualifiedName());
            assertEquals("PUBLIC",capability.accessibility());
            assertTrue(capability.constructors().stream().anyMatch(value->value.signature().equals("LoginPage()")
                    &&value.accessibility().equals("PUBLIC")&&value.acquisition().equals("DIRECT_CONSTRUCTION")));
            var method=capability.methods().stream().filter(value->value.name().equals("invalidPasswordShowsExpectedError")).findFirst().orElseThrow();
            assertEquals("boolean",method.returnType());
            assertEquals("benchmark.LoginPage",method.declaringType());
            assertEquals(List.of("String","String"),method.parameters().stream().map(AgentContextPack.CapabilityParameter::type).toList());
            assertEquals(List.of("login-button","login-error"),method.applicationElementIds());
            assertTrue(capability.methods().stream().anyMatch(value->value.name().equals("loginErrorVisible")),
                    "public query methods must remain visible even when they do not own a selected locator");
            assertEquals(List.of("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)",
                            "benchmark.LoginPage#loginErrorVisible()"),
                    context.pageObjectApis().get("login-page"));
            assertTrue(context.task().requiredElementIds().containsAll(List.of("login-button","login-error")));
            assertFalse(context.task().requiredElementIds().contains("login-help"),
                    "an exact method requirement must not pull an unrelated element through the generic login token");
            assertTrue(gateway.request.target().testClass().startsWith("benchmark.GeneratedStudioTest"));
            assertTrue(gateway.request.allowedPaths().contains("src/test/java"));
        }
    }

    @Test void pomChangeAfterPlanningIsRejectedBeforeImplementerReceivesStaleManifest() throws Exception {
        Path sourceRoot=project();persistModel();CapturingGateway gateway=new CapturingGateway();
        try(TestEngineeringStudioService service=service(sourceRoot,gateway)){
            String runId=service.createRequirement("Create a login test using LoginPage.invalidPasswordShowsExpectedError");
            service.generatePlan(runId);
            Files.writeString(sourceRoot.resolve("benchmark/LoginPage.java"),pageObject().replace("loginErrorVisible", "renamedLoginErrorVisible"));

            assertThrows(IllegalStateException.class,()->service.generateImplementation(runId));
            assertEquals(0,gateway.implementationCalls.get());
            assertEquals("STALE",service.workflow(runId).freshness());
        }
    }

    @Test void importedSeleniumWebDriverConstructorBecomesAnExplicitStudioRuntimeExpression() throws Exception {
        Path sourceRoot=project();
        Files.writeString(sourceRoot.resolve("benchmark/LoginPage.java"),pageObject().replace(
                "import org.openqa.selenium.By;", "import org.openqa.selenium.By;\nimport org.openqa.selenium.WebDriver;").replace(
                "public LoginPage() {}", "public LoginPage(WebDriver driver) {}"));
        persistModel();CapturingGateway gateway=new CapturingGateway();
        try(TestEngineeringStudioService service=service(sourceRoot,gateway)){
            String runId=service.createRequirement("Create a login test using LoginPage.invalidPasswordShowsExpectedError");
            service.generatePlan(runId);

            var constructor=assertSingle(assertSingle(gateway.context.pageObjectCapabilities().classes()).constructors());
            assertEquals("org.openqa.selenium.WebDriver",constructor.parameters().get(0).type());
            String runtime=StudioPageObjectRuntimeContract.render(gateway.context);
            assertTrue(runtime.contains("STUDIO_BOUND_WEBDRIVER"));
            assertTrue(runtime.contains("BrowserSessionContext.currentDriver()"));
        }
    }

    @Test void encodedImplementerCommandContainsTheSelectedFingerprintBoundRuntimeContract() throws Exception {
        Path sourceRoot=project();
        Files.writeString(sourceRoot.resolve("benchmark/LoginPage.java"),pageObject().replace(
                "import org.openqa.selenium.By;", "import org.openqa.selenium.By;\nimport org.openqa.selenium.WebDriver;").replace(
                "public LoginPage() {}", "public LoginPage(WebDriver driver) {}"));
        persistModel();
        String fingerprint=new ExistingProjectIndexer().index(new ExistingProjectIndexer.Request(
                root,List.of(sourceRoot),List.of())).projectFingerprint();
        AtomicReference<AgentExecutor.AgentCommand> implementerCommand=new AtomicReference<>();
        TestPlan plan=new TestPlan(ready(),List.of(new TestPlan.TestScenario("invalid-password","Invalid password",
                TestPlan.Priority.HIGH,List.of(),List.of(new TestPlan.TestStep(1,
                "loginPage.invalidPasswordShowsExpectedError(\"test-user\", \"invalid-value\")","login-page",null,null)),
                List.of(new TestPlan.ExpectedResult("login-page",null,
                        "assertTrue(loginPage.invalidPasswordShowsExpectedError(\"test-user\", \"invalid-value\"))",null)),
                List.of("login-page"),List.of(),List.of(),List.of(),List.of())));
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                "package benchmark; import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.assertTrue; "
                        + "public final class GeneratedTest { @Test void invalidPassword() { "
                        + "assertTrue(new LoginPage(io.github.testlens.studio.browser.BrowserSessionContext.currentDriver())"
                        + ".invalidPasswordShowsExpectedError(\"test-user\", \"invalid-value\")); } }",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                List.of("benchmark.LoginPage.invalidPasswordShowsExpectedError(String,String)"),List.of());
        AgentExecutor executor=command->{
            if(command.role()==AgentExecutor.Role.TEST_IMPLEMENTER)implementerCommand.set(command);
            Object payload=command.role()==AgentExecutor.Role.TEST_ARCHITECT?plan:implementation;
            return new AgentExecutor.AgentResult("captured",ArtifactEnvelope.create("captured-"+command.role(),
                    command.runId(),List.of(),1,payload));
        };
        ReviewableCoordinatorWorkflowGateway gateway=new ReviewableCoordinatorWorkflowGateway(executor,
                (replay,ignored)->{throw new AssertionError("RUN is not expected");},
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ignored->Path.of("src/test/java/benchmark/GeneratedTest.java"),proposal->{},()->fingerprint);
        try(TestEngineeringStudioService service=service(sourceRoot,gateway)){
            String runId=service.createRequirement("Create a login test using LoginPage.invalidPasswordShowsExpectedError");
            service.generatePlan(runId);service.generateImplementation(runId);
        }

        byte[] encoded=new AgentProtocolCodec().encodeCommand(implementerCommand.get());
        String document=new String(encoded,java.nio.charset.StandardCharsets.UTF_8);
        assertAll(
                ()->assertTrue(document.contains("benchmark.LoginPage")),
                ()->assertTrue(document.contains("invalidPasswordShowsExpectedError")),
                ()->assertTrue(document.contains("allowedPageObjectApiReferences")),
                ()->assertTrue(document.contains("benchmark.LoginPage#invalidPasswordShowsExpectedError(String,String)")),
                ()->assertFalse(document.contains("boolean invalidPasswordShowsExpectedError(String, String)"),
                        "the obsolete return-type-prefixed representation must not reach the Implementer"),
                ()->assertTrue(document.contains("BrowserSessionContext.currentDriver()")),
                ()->assertTrue(document.contains("BOOLEAN_ASSERTION_COMPATIBLE")),
                ()->assertTrue(document.contains("benchmark.GeneratedStudioTest")),
                ()->assertTrue(document.contains("JUnit 5")),
                ()->assertTrue(document.contains(fingerprint)),
                ()->assertTrue(document.contains("CAPABILITY_PRESENT_BUT_REJECTED") == false,
                        "a fresh command must not contain a prior refusal classification"));
    }

    private TestEngineeringStudioService service(Path sourceRoot,StudioWorkflowGateway gateway){
        return new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,List.of(sourceRoot),List.of(),
                List.of("src/test/java"),"fixture"),gateway,null);
    }
    private Path project() throws Exception {
        Path sourceRoot=root.resolve("src/test/java"),source=sourceRoot.resolve("benchmark/LoginPage.java");Files.createDirectories(source.getParent());
        Files.writeString(root.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><artifactId>fixture</artifactId></project>");
        Files.writeString(source,pageObject());return sourceRoot;
    }
    private static String pageObject(){return """
            package benchmark;
            import org.openqa.selenium.By;
            public final class LoginPage {
                private final By loginButton = By.id("login-submit");
                private final By loginError = By.id("login-error");
                private final By loginHelpButton = By.id("login-help");
                public LoginPage() {}
                public boolean invalidPasswordShowsExpectedError(String user, String passwordValue) {
                    return loginButton != null && loginError != null;
                }
                public boolean loginErrorVisible() { return true; }
                private boolean internalProbe() { return loginHelpButton != null; }
            }
            """;}
    private void persistModel() throws Exception {
        new StudioWorkspaceStore(root).write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(model())),"initial");
    }
    private static ApplicationModel model(){
        List<ApplicationModel.ElementModel>elements=List.of(element("login-button","LoginButton","login-submit"),
                element("login-error","LoginError","login-error"),element("login-help","LoginHelpButton","login-help"));
        var page=new ApplicationModel.PageModel("login-page","Login",new ApplicationModel.PageIdentity("/login",null,"Login",
                List.of("login-button","login-error"),"page-fp",ApplicationModel.EvidenceSource.OBSERVED),List.of("Login"),
                List.of("page-fp"),List.of(),List.of(),List.of(elements.get(0),elements.get(1),elements.get(2)),List.of(),List.of(),provenance(),List.of());
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"fixture-app","fixture","test",Instant.EPOCH,List.of(page),
                List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,3,3,0,3,0,false),List.of(),provenance());
    }
    private static ApplicationModel.ElementModel element(String id,String name,String selector){
        var projection=new ApplicationModel.SelectorProjection("id",selector,"candidate-"+id,"VERIFIED_IN_SCOPE","SAME_TARGET",true,
                List.of("STABLE"),1,List.of(),List.of(),ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        return new ApplicationModel.ElementModel(id,name,ApplicationModel.ElementType.BUTTON,"button",name,name,List.of(ApplicationModel.Action.CLICK),
                projection,List.of(),ApplicationModel.SelectorQuality.VERIFIED,null,"fp-"+id,provenance(),List.of());
    }
    private static ApplicationModel.Provenance provenance(){return ApplicationModel.Provenance.observed("fixture","capability-test",List.of("fixture"));}
    private static ContractHeader ready(){return new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);}
    private static <T>T assertSingle(List<T>values){assertEquals(1,values.size());return values.get(0);}

    private static final class CapturingGateway implements StudioWorkflowGateway {
        private TestEngineeringRequest request;private AgentContextPack context;private final AtomicInteger implementationCalls=new AtomicInteger();
        @Override public void prepare(String runId,TestEngineeringRequest request,AgentContextPack context){this.request=request;this.context=context;}
        @Override public TestPlan generatePlan(String runId,String requirement){return new TestPlan(ready(),List.of());}
        @Override public TestImplementationProposal generateImplementation(String runId){implementationCalls.incrementAndGet();throw new AssertionError("stale context reached Implementer");}
        @Override public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(String runId){throw new AssertionError("run not expected");}
    }
}
