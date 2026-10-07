package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import io.github.testlens.studio.launcher.StudioLauncherService;
import io.github.testlens.studio.project.ProjectDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StudioWorkflowResumeTest {
    @TempDir Path root;

    @Test void planAndImplementationResumeWithoutCallingAgentsAgain() throws Exception {
        prepareProject();persistModel();var descriptor=new ProjectDiscovery().discover(root);String runId;
        CountingGateway firstGateway=new CountingGateway();
        try(var host=new StudioLauncherService(firstGateway,null).launch(descriptor,new StudioLauncherService.LaunchOptions(false))){
            TestEngineeringStudioService first=host.service();runId=first.createRequirement("Login with invalid password shows an error");
            first.generatePlan(runId);first.generateImplementation(runId);
            assertEquals(1,firstGateway.planCalls.get());assertEquals(1,firstGateway.implementationCalls.get());
            assertThrows(IllegalStateException.class,()->first.generateImplementation(runId),"a stale tab cannot repeat an unavailable transition");
        }
        CountingGateway restoredGateway=new CountingGateway();
        try(var host=new StudioLauncherService(restoredGateway,null).launch(descriptor,new StudioLauncherService.LaunchOptions(false))){
            var workflow=host.service().workflow(runId);
            assertTrue(workflow.resumed());assertEquals("IMPLEMENTATION_READY_FOR_REVIEW",workflow.status());
            assertEquals("FRESH",workflow.freshness());assertNotNull(workflow.plan());assertNotNull(workflow.implementation());
            assertEquals(List.of("RUN"),workflow.availableActions());assertEquals(0,restoredGateway.planCalls.get());
            assertEquals(0,restoredGateway.implementationCalls.get());assertEquals(1,restoredGateway.restoreCalls.get());
        }
        StudioWorkflowGateway inspectionOnly=ignored->{throw new UnsupportedOperationException("workflow unavailable");};
        try(var host=new StudioLauncherService(inspectionOnly,null).launch(descriptor,new StudioLauncherService.LaunchOptions(false))){
            assertEquals("IMPLEMENTATION_READY_FOR_REVIEW",host.service().workflow(runId).status(),"inspection-only launcher must not misclassify a valid snapshot as corrupted");
        }
    }

    @Test void changedSourceMakesRestoredProposalStaleAndBlocksRun() throws Exception {
        Path sourceRoot=prepareProject();persistModel();CountingGateway firstGateway=new CountingGateway();TestEngineeringStudioService first=service(sourceRoot,firstGateway);
        String runId=first.createRequirement("Login with invalid password shows an error");first.generatePlan(runId);first.generateImplementation(runId);
        Files.writeString(sourceRoot.resolve("LoginPage.java"),"import org.openqa.selenium.By; final class LoginPage { private final By loginButton = By.id(\"changed\"); }");

        CountingGateway restoredGateway=new CountingGateway();TestEngineeringStudioService restarted=service(sourceRoot,restoredGateway);
        var workflow=restarted.workflow(runId);
        assertEquals("STALE",workflow.freshness());
        assertTrue(workflow.limitations().contains("SOURCE_CHANGED"));
        assertEquals(List.of("REGENERATE_PLAN"),workflow.availableActions());
        assertThrows(IllegalStateException.class,()->restarted.runWorkflow(runId));
        assertEquals(0,restoredGateway.restoreCalls.get(),"stale artifacts must not be injected into the workflow engine");
        restarted.generatePlan(runId);
        assertEquals(1,restoredGateway.planCalls.get());
        assertEquals("PLAN_READY_FOR_REVIEW",restarted.workflow(runId).status());
    }

    @Test void corruptedWorkflowSnapshotDoesNotPreventHostStartup() throws Exception {
        prepareProject();
        Path sessions=root.resolve(".test-lens/ai/sessions");
        Files.createDirectories(sessions);
        Files.writeString(sessions.resolve("broken-run.json"),"{not-json");
        var descriptor=new ProjectDiscovery().discover(root);
        StudioWorkflowGateway inspectionOnly=ignored->{throw new UnsupportedOperationException("workflow unavailable");};
        try(var host=new StudioLauncherService(inspectionOnly,null).launch(descriptor,new StudioLauncherService.LaunchOptions(false))){
            var restored=host.service().workflow("broken-run");
            assertEquals("CORRUPTED",restored.status());
            assertEquals("INVALID",restored.freshness());
            assertTrue(restored.limitations().stream().anyMatch(value->value.startsWith("CORRUPTED_SNAPSHOT:")));
        }
    }

    private TestEngineeringStudioService service(Path sourceRoot, StudioWorkflowGateway gateway){
        return new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(root,List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),gateway,null);
    }
    private Path prepareProject() throws Exception {Path sourceRoot=root.resolve("src/test/java");Files.createDirectories(sourceRoot);Files.writeString(root.resolve("pom.xml"),"<project><modelVersion>4.0.0</modelVersion><artifactId>fixture</artifactId></project>");Files.writeString(sourceRoot.resolve("LoginPage.java"),"import org.openqa.selenium.By; final class LoginPage { private final By loginButton = By.id(\"old-login\"); }");return sourceRoot;}
    private void persistModel() throws Exception {new StudioWorkspaceStore(root).write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,StrictJson.readObject(new ApplicationModelJson().write(model())),"initial");}

    private static final class CountingGateway implements StudioWorkflowGateway {
        private final AtomicInteger planCalls=new AtomicInteger(),implementationCalls=new AtomicInteger(),restoreCalls=new AtomicInteger();
        @Override public void prepare(String runId,io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request,AgentContextPack context){}
        @Override public TestPlan generatePlan(String runId,String requirement){planCalls.incrementAndGet();return new TestPlan(ready(),List.of());}
        @Override public TestImplementationProposal generateImplementation(String runId){implementationCalls.incrementAndGet();return implementation();}
        @Override public List<PolicyCheck> implementationPolicy(String runId){return List.of(new PolicyCheck("PAGE_OBJECTS_ONLY",true,"safe"));}
        @Override public void restoreReviewedArtifacts(String runId,TestPlan plan,TestImplementationProposal implementation){restoreCalls.incrementAndGet();}
        @Override public boolean supportsReviewedArtifactRestore(){return true;}
        @Override public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(String runId){throw new AssertionError("run must remain blocked in this test");}
        private static TestImplementationProposal implementation(){return new TestImplementationProposal(ready(),"invalid-password","package fixture; final class InvalidPasswordTest {}",TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of("LoginPage.login"),List.of());}
    }
    private static ContractHeader ready(){return new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);}
    private static ApplicationModel model(){var selector=new ApplicationModel.SelectorProjection("id","old-login","observed-login","VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),1,List.of(),List.of(),ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);var element=new ApplicationModel.ElementModel("login-button","LoginButton",ApplicationModel.ElementType.BUTTON,"button","Sign in","Sign in",List.of(ApplicationModel.Action.CLICK),selector,List.of(),ApplicationModel.SelectorQuality.VERIFIED,null,"element-fp",provenance(),List.of());var page=new ApplicationModel.PageModel("login-page","Login",new ApplicationModel.PageIdentity("/login",null,"Login",List.of("login-button"),"page-fp",ApplicationModel.EvidenceSource.OBSERVED),List.of("Login"),List.of("page-fp"),List.of(),List.of(),List.of(element),List.of(),List.of(),provenance(),List.of());return new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"fixture-app","fixture","test",Instant.EPOCH,List.of(page),List.of(),List.of(),new ApplicationModel.Coverage(ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,1,1,0,1,0,false),List.of(),provenance());}
    private static ApplicationModel.Provenance provenance(){return ApplicationModel.Provenance.observed("fixture","resume-test",List.of("fixture"));}
}
