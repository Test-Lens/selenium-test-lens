package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import io.github.testlens.application.tooling.ai.workflow.ScriptedAgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile;
import io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec;
import io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner;
import io.github.testlens.application.tooling.ai.workflow.runner.RoleDispatchingAgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.runner.PageObjectApiReferenceValidator;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.studio.transport.TestEngineeringStudioServer;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import io.github.testlens.studio.workspace.WorkflowSessionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StudioAgentFailureContractTest {
    @TempDir Path root;

    @Test void invalidPlanRetainsStructuredFailureForRetryAndNeverCallsImplementer() throws Exception {
        Path sourceRoot=prepareReadyProject();
        FailingPlanGateway gateway=new FailingPlanGateway();
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,
                List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),gateway,null);
        String runId=service.createRequirement("Login with invalid password shows an error");

        try(var server=new TestEngineeringStudioServer(service)){
            server.start();
            URI actions=server.uri().resolve("api/actions");
            var request=HttpRequest.newBuilder(actions)
                    .header(TestEngineeringStudioServer.TOKEN_HEADER,server.sessionToken())
                    .header("Origin",server.uri().toString().replaceAll("/$",""))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"action\":\"GENERATE_PLAN\",\"runId\":\""+runId+"\"}"))
                    .build();

            var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());

            assertEquals(502,response.statusCode());
            Map<String,Object> body=StrictJson.readObject(response.body().getBytes(StandardCharsets.UTF_8));
            assertEquals("AGENT_OUTPUT_INVALID",body.get("error"));
            assertTrue(body.get("stage") instanceof String stage&&!stage.isBlank());
            assertEquals("SCHEMA_INVALID",body.get("reasonCode"));
            assertEquals("payload.scenarios",body.get("location"));
            assertTrue(body.get("message") instanceof String message&&message.contains("Agent output did not match the required contract."));
            assertTrue(body.get("message") instanceof String message&&message.contains("[REDACTED]"));
            assertEquals(Map.of("resultType","TEST_PLAN"),body.get("metadata"));
            assertFalse(response.body().contains("top-secret"));
            assertTrue(response.body().length()<2_000,"agent failure responses must remain bounded");
        }

        var workflow=service.workflow(runId);
        assertEquals("AGENT_FAILED",workflow.status());
        assertEquals(List.of("GENERATE_PLAN"),workflow.availableActions());
        assertNotNull(workflow.agentFailure());
        assertEquals("AGENT_OUTPUT_INVALID",workflow.agentFailure().code());
        assertEquals("SCHEMA_INVALID",workflow.agentFailure().reasonCode());
        assertEquals("payload.scenarios",workflow.agentFailure().location());
        assertThrows(IllegalStateException.class,()->service.generateImplementation(runId));
        assertEquals(0,gateway.implementationCalls.get(),"Implementer must not run without a valid reviewed plan");
    }

    @Test void implementationFailureKeepsReviewedPlanAndOriginalFailureForRetry() throws Exception {
        Path sourceRoot=prepareReadyProject();
        FailingImplementationGateway gateway=new FailingImplementationGateway();
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,
                List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),gateway,null);
        String runId=service.createRequirement("Login with invalid password shows an error");
        service.generatePlan(runId);

        var thrown=assertThrows(AgentExecutor.AgentExecutionException.class,()->service.generateImplementation(runId));

        assertEquals(AgentExecutor.AgentFailureCode.AGENT_TIMEOUT,thrown.code());
        var workflow=service.workflow(runId);
        assertEquals("AGENT_FAILED",workflow.status());
        assertNotNull(workflow.plan(),"the reviewed plan remains available after Implementer failure");
        assertNull(workflow.implementation());
        assertEquals(List.of("GENERATE_IMPLEMENTATION"),workflow.availableActions());
        assertEquals("AGENT_TIMEOUT",workflow.agentFailure().code());
        assertEquals("Implementer",workflow.agentFailure().stage());
        assertEquals("PROCESS_TIMEOUT",workflow.agentFailure().reasonCode());
        assertEquals(1,gateway.implementationCalls.get());
    }

    @Test void reviewerBoundaryFailureIsPersistedWithReviewerStageAndExactReason() throws Exception {
        Path sourceRoot=prepareReadyProject();
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,
                List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),
                new FailingReviewerGateway(),null);
        String runId=service.createRequirement("Login with invalid password shows an error");
        service.generatePlan(runId);
        service.generateImplementation(runId);

        var thrown=assertThrows(AgentExecutor.AgentExecutionException.class,()->service.runWorkflow(runId));

        assertEquals(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,thrown.code());
        var workflow=service.workflow(runId);
        assertEquals("AGENT_FAILED",workflow.status());
        assertNotNull(workflow.agentFailure());
        assertEquals("Reviewer",workflow.agentFailure().stage());
        assertEquals("RULE_ID_INVALID",workflow.agentFailure().reasonCode());
        assertEquals("$.result.payload.findings[0].ruleId",workflow.agentFailure().location());
        var restored=WorkflowSessionSnapshot.fromDocument(service.workspace().readWorkflow(runId).orElseThrow());
        assertEquals("RULE_ID_INVALID",restored.agentFailure().reasonCode());
        assertNull(workflow.review());
    }

    @Test void capabilityRefusalIsAConflictNotAnInvalidAgentOutputAndDoesNotRun() throws Exception {
        Path sourceRoot=prepareReadyProject();
        CapabilityRefusalGateway gateway=new CapabilityRefusalGateway();
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,
                List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),gateway,null);
        String runId=service.createRequirement("Login with invalid password shows an error");
        service.generatePlan(runId);

        try(var server=new TestEngineeringStudioServer(service)){
            server.start();
            var request=HttpRequest.newBuilder(server.uri().resolve("api/actions"))
                    .header(TestEngineeringStudioServer.TOKEN_HEADER,server.sessionToken())
                    .header("Origin",server.uri().toString().replaceAll("/$",""))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"action\":\"GENERATE_IMPLEMENTATION\",\"runId\":\""+runId+"\"}"))
                    .build();
            var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
            assertEquals(409,response.statusCode());
            Map<String,Object> body=StrictJson.readObject(response.body().getBytes(StandardCharsets.UTF_8));
            assertEquals("PAGE_OBJECT_CAPABILITY_MISSING",body.get("error"));
            assertEquals("$.payload.capability",body.get("location"));
            assertEquals("REVISE_REQUIREMENT_OR_PAGE_OBJECT",body.get("nextAction"));
            assertEquals("PAGE_OBJECT_METHOD_MISSING",body.get("capabilityCategory"));
            assertEquals(Boolean.TRUE,body.get("sourceFingerprintMatched"));
            assertEquals("login",body.get("verifiedPageId"));
            assertFalse(response.body().contains("untrusted model detail"));
            assertFalse(response.body().contains("raw capability canary"));
            assertFalse(response.body().contains("raw reason canary"));
            assertFalse(response.body().contains("sensitive-canary"));
        }

        var workflow=service.workflow(runId);
        assertEquals("PLAN_READY_FOR_REVIEW",workflow.status());
        assertEquals(List.of("GENERATE_IMPLEMENTATION"),workflow.availableActions());
        assertNull(workflow.agentFailure());
        assertNull(workflow.implementation());
        assertEquals(0,gateway.runCalls.get());
    }

    @Test void externalImplementerSecretReachesHttpAsValueFreeDiagnosticAndIsNeverPersistedOrCompiled() throws Exception {
        Path sourceRoot = prepareReadyProject();
        AtomicInteger coordinatorCreations = new AtomicInteger();
        AgentProfile profile = new AgentProfile("studio-security-fixture", javaExecutable(),
                List.of("-cp", absoluteClasspath(), StudioExternalAgentFixtureMain.class.getName(), "secret"),
                AgentExecutor.Role.TEST_IMPLEMENTER, Duration.ofSeconds(10), 8_192, 4_096, 512,
                Set.of(), AgentProfile.OutputTransport.STDOUT);
        ExternalAgentRunner implementer = new ExternalAgentRunner(profile, root.resolve("agent-runtime"),
                new AgentProtocolCodec(), RedactionPolicy.builder().secret("configured-secret-canary").build(),
                List.of(), List.of(), Map.of());
        ScriptedAgentExecutor architect = new ScriptedAgentExecutor(List.of(new AgentExecutor.AgentResult("scripted",
                ArtifactEnvelope.create("plan", "fixture", List.of(), 1, new TestPlan(ready(), List.of())))));
        AgentExecutor agents = new RoleDispatchingAgentExecutor(Map.of(
                AgentExecutor.Role.TEST_ARCHITECT, architect,
                AgentExecutor.Role.TEST_IMPLEMENTER, implementer));
        var gateway = new ReviewableCoordinatorWorkflowGateway(agents, replay -> {
            coordinatorCreations.incrementAndGet();
            throw new AssertionError("security-rejected output must not create the compiler pipeline");
        });
        var service = new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,
                List.of(sourceRoot), List.of(), List.of("src/test/java"), "fixture"), gateway, null);
        String runId = service.createRequirement("Login with invalid password shows an error");
        service.generatePlan(runId);

        String responseText;
        try (var server = new TestEngineeringStudioServer(service)) {
            server.start();
            var request = HttpRequest.newBuilder(server.uri().resolve("api/actions"))
                    .header(TestEngineeringStudioServer.TOKEN_HEADER, server.sessionToken())
                    .header("Origin", server.uri().toString().replaceAll("/$", ""))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"action\":\"GENERATE_IMPLEMENTATION\",\"runId\":\"" + runId + "\"}"))
                    .build();
            var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(502, response.statusCode());
            responseText = response.body();
            Map<String, Object> body = StrictJson.readObject(responseText.getBytes(StandardCharsets.UTF_8));
            assertEquals("AGENT_OUTPUT_INVALID", body.get("error"));
            assertEquals("LITERAL_SECRET", body.get("reasonCode"));
            assertEquals("$.result.payload.sourcePatch", body.get("location"));
        }

        assertFalse(responseText.contains("configured-secret-canary"));
        assertEquals(0, coordinatorCreations.get());
        assertNull(service.workflow(runId).implementation());
        assertEquals("LITERAL_SECRET", service.workflow(runId).agentFailure().reasonCode());
        String session = service.workspace().readWorkflow(runId).orElseThrow().toString();
        String history = service.workspace().read(StudioWorkspaceStore.ArtifactKind.WORKFLOW_HISTORY)
                .orElseThrow().toString();
        assertFalse(session.contains("configured-secret-canary"));
        assertFalse(history.contains("configured-secret-canary"));
        try (var files = Files.walk(root)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith("GeneratedStudioTest")),
                    "security-rejected source must not be written");
        }
    }

    @Test void sourceVerificationDiagnosticSurvivesTheCapabilityHttpBoundaryWithoutDynamicText() throws Exception {
        Path sourceRoot=prepareReadyProject();
        var service=new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,
                List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),
                new SourceVerificationFailureGateway(),null);
        String runId=service.createRequirement("Login with invalid password shows an error");
        service.generatePlan(runId);

        try(var server=new TestEngineeringStudioServer(service)){
            server.start();
            var request=HttpRequest.newBuilder(server.uri().resolve("api/actions"))
                    .header(TestEngineeringStudioServer.TOKEN_HEADER,server.sessionToken())
                    .header("Origin",server.uri().toString().replaceAll("/$",""))
                    .header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "{\"action\":\"GENERATE_IMPLEMENTATION\",\"runId\":\""+runId+"\"}"))
                    .build();
            var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
            assertEquals(409,response.statusCode());
            Map<String,Object> body=StrictJson.readObject(response.body().getBytes(StandardCharsets.UTF_8));
            assertEquals(PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH,body.get("reasonCode"));
            assertEquals("$.payload.sourcePatch",body.get("location"));
            Map<?,?> metadata=assertInstanceOf(Map.class,body.get("metadata"));
            assertEquals("SOURCE_API_VERIFICATION",metadata.get("validationStage"));
            assertEquals("METHOD_CALL",metadata.get("astNodeKind"));
            assertEquals("KNOWN_PAGE_OBJECT",metadata.get("receiverCategory"));
            assertEquals("NO_MATCH",metadata.get("signatureStatus"));
            assertFalse(response.body().contains("LoginPage"));
            assertFalse(response.body().contains("invalidPasswordShowsExpectedError"));
        }
    }

    private Path prepareReadyProject() throws Exception {
        Path sourceRoot=root.resolve("src/test/java");
        Files.createDirectories(sourceRoot);
        Files.writeString(root.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><artifactId>fixture</artifactId></project>");
        Files.writeString(sourceRoot.resolve("LoginPage.java"),"package example; import org.openqa.selenium.By; public final class LoginPage { private final By loginButton = By.id(\"old-login\"); public LoginPage(){} public boolean login(){ return true; } }");
        new StudioWorkspaceStore(root).write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(model())),"initial");
        return sourceRoot;
    }

    private static final class FailingPlanGateway implements StudioWorkflowGateway {
        private final AtomicInteger implementationCalls=new AtomicInteger();
        @Override public void prepare(String runId,io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request,AgentContextPack context){}
        @Override public TestPlan generatePlan(String runId,String requirement) throws AgentExecutor.AgentExecutionException {
            throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                    "Agent output did not match the required contract. password=top-secret",
                    null,
                    new AgentExecutor.AgentFailureDiagnostic("SCHEMA_INVALID","payload.scenarios",Map.of("resultType","TEST_PLAN")));
        }
        @Override public TestImplementationProposal generateImplementation(String runId){implementationCalls.incrementAndGet();throw new AssertionError("Implementer must not run");}
        @Override public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(String runId){throw new AssertionError("run must not execute");}
    }

    private static final class FailingImplementationGateway implements StudioWorkflowGateway {
        private final AtomicInteger implementationCalls=new AtomicInteger();
        @Override public void prepare(String runId,io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request,AgentContextPack context){}
        @Override public TestPlan generatePlan(String runId,String requirement){return new TestPlan(ready(),List.of());}
        @Override public TestImplementationProposal generateImplementation(String runId) throws AgentExecutor.AgentExecutionException {
            implementationCalls.incrementAndGet();
            throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_TIMEOUT,
                    "Implementer exceeded the configured deadline.",
                    null,
                    new AgentExecutor.AgentFailureDiagnostic("PROCESS_TIMEOUT","process.wait",Map.of("durationMillis","180000")));
        }
        @Override public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(String runId){throw new AssertionError("run must not execute");}
    }

    private static final class FailingReviewerGateway implements StudioWorkflowGateway {
        @Override public void prepare(String runId,io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request,AgentContextPack context){}
        @Override public TestPlan generatePlan(String runId,String requirement){return new TestPlan(ready(),List.of());}
        @Override public TestImplementationProposal generateImplementation(String runId){
            return new TestImplementationProposal(ready(),"scenario",
                    "package example; final class GeneratedTest {}",
                    TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(),List.of());
        }
        @Override public List<PolicyCheck> implementationPolicy(String runId){
            return List.of(new PolicyCheck("FIXTURE_POLICY",true,"PASS"));
        }
        @Override public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(String runId)
                throws AgentExecutor.AgentExecutionException {
            throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                    "Reviewer returned an invalid structured decision.",null,
                    new AgentExecutor.AgentFailureDiagnostic("RULE_ID_INVALID",
                            "$.result.payload.findings[0].ruleId",Map.of("role","CODE_REVIEWER")));
        }
    }

    private static final class CapabilityRefusalGateway implements StudioWorkflowGateway {
        private final AtomicInteger runCalls=new AtomicInteger();
        @SuppressWarnings("unused") private final String rawCapability="raw capability canary sensitive-canary";
        @SuppressWarnings("unused") private final String rawReason="raw reason canary sensitive-canary";
        @Override public void prepare(String runId,io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request,AgentContextPack context){}
        @Override public TestPlan generatePlan(String runId,String requirement){return new TestPlan(ready(),List.of());}
        @Override public TestImplementationProposal generateImplementation(String runId){
            throw new PageObjectCapabilityException("PAGE_OBJECT_CAPABILITY_MISSING","$.payload.capability",
                    PageObjectCapabilityException.RefusalCategory.PAGE_OBJECT_METHOD_MISSING,"login",true);
        }
        @Override public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(String runId){
            runCalls.incrementAndGet();throw new AssertionError("domain refusal must not compile or execute");
        }
    }

    private static final class SourceVerificationFailureGateway implements StudioWorkflowGateway {
        @Override public void prepare(String runId,io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request,AgentContextPack context){}
        @Override public TestPlan generatePlan(String runId,String requirement){return new TestPlan(ready(),List.of());}
        @Override public TestImplementationProposal generateImplementation(String runId){
            var diagnostic=new PageObjectApiReferenceValidator.SourceDiagnostic("SOURCE_API_VERIFICATION",
                    "METHOD_SIGNATURE","SIGNATURE_RESOLUTION","METHOD_CALL","KNOWN_PAGE_OBJECT",
                    "MATCHED","MATCHED","NO_MATCH");
            var verification=new PageObjectApiReferenceValidator.SourceVerification(false,
                    PageObjectApiReferenceValidator.METHOD_SIGNATURE_MISMATCH,List.of(),List.of(),diagnostic);
            throw PageObjectCapabilityException.sourceVerification(verification);
        }
        @Override public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(String runId){
            throw new AssertionError("source verification refusal must not run");
        }
    }

    private static ContractHeader ready(){return new ContractHeader(ContractHeader.SCHEMA_VERSION,
            ContractHeader.Status.READY,List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);}

    private static ApplicationModel model(){var selector=new ApplicationModel.SelectorProjection("id","old-login","observed-login","VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),1,List.of(),List.of(),ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);var element=new ApplicationModel.ElementModel("login-button","LoginButton",ApplicationModel.ElementType.BUTTON,"button","Sign in","Sign in",List.of(ApplicationModel.Action.CLICK),selector,List.of(),ApplicationModel.SelectorQuality.VERIFIED,null,"element-fp",provenance(),List.of());var page=new ApplicationModel.PageModel("login-page","Login",new ApplicationModel.PageIdentity("/login",null,"Login",List.of("login-button"),"page-fp",ApplicationModel.EvidenceSource.OBSERVED),List.of("Login"),List.of("page-fp"),List.of(),List.of(),List.of(element),List.of(),List.of(),provenance(),List.of());return new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"fixture-app","fixture","test",Instant.EPOCH,List.of(page),List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,1,1,0,1,0,false),List.of(),provenance());}
    private static ApplicationModel.Provenance provenance(){return ApplicationModel.Provenance.observed("fixture","agent-failure-test",List.of("fixture"));}
    private static Path javaExecutable() {
        String name = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", name).toAbsolutePath();
    }
    private static String absoluteClasspath() {
        Path base = Path.of("").toAbsolutePath();
        return Arrays.stream(System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(Path::of).map(path -> path.isAbsolute() ? path : base.resolve(path).normalize())
                .map(Path::toString).collect(java.util.stream.Collectors.joining(File.pathSeparator));
    }
}
