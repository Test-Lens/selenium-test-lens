package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.AgentContextPack;
import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest;
import io.github.testlens.application.tooling.ai.workflow.WorkflowReport;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import io.github.testlens.studio.workspace.WorkflowSessionSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class StudioRunLifecycleRecoveryTest {
    @TempDir Path root;

    @Test void runningMarkerIsDurableBeforeGatewayAndUncheckedFailureBecomesDurableInterruption() throws Exception {
        Path sourceRoot=prepareReadyProject();
        InspectingFailureGateway gateway=new InspectingFailureGateway(new StudioWorkspaceStore(root));
        TestEngineeringStudioService service=readyService(sourceRoot,gateway);
        String runId=readyRun(service);

        TestEngineeringStudioService.RunLifecycleException failure=assertThrows(
                TestEngineeringStudioService.RunLifecycleException.class,()->service.runWorkflow(runId));

        assertEquals(WorkflowSessionSnapshot.RunReason.EXECUTION_RESULT_UNKNOWN,failure.reason());
        assertEquals(WorkflowSessionSnapshot.RunStatus.INTERRUPTED,failure.status());
        assertTrue(failure.recoveryPersisted());
        assertEquals(WorkflowSessionSnapshot.State.EXECUTION_RUNNING,gateway.stateObservedInsideRun);
        WorkflowSessionSnapshot stored=stored(service,runId);
        assertEquals(WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,stored.state());
        assertEquals(new WorkflowSessionSnapshot.RunDiagnosticSnapshot(
                WorkflowSessionSnapshot.RunReason.EXECUTION_RESULT_UNKNOWN,
                WorkflowSessionSnapshot.RunStatus.INTERRUPTED,"RUN",true),stored.runDiagnostic());
        assertEquals(List.of(),stored.availableActions());
        assertNull(stored.execution(),"an executor exception must not manufacture execution evidence");
        assertThrows(IllegalStateException.class,()->service.runWorkflow(runId));
        assertEquals(1,gateway.runCalls.get(),"the unresolved run must never be replayed");
    }

    @Test void uncheckedIoFailureAfterRunningMarkerHasTheSameFailClosedDurableOutcome() throws Exception {
        Path sourceRoot=prepareReadyProject();
        ThrowingGateway gateway=new ThrowingGateway(new UncheckedIOException(new java.io.IOException("fixture")));
        TestEngineeringStudioService service=readyService(sourceRoot,gateway);
        String runId=readyRun(service);

        var failure=assertThrows(TestEngineeringStudioService.RunLifecycleException.class,
                ()->service.runWorkflow(runId));

        assertEquals(WorkflowSessionSnapshot.RunReason.EXECUTION_RESULT_UNKNOWN,failure.reason());
        WorkflowSessionSnapshot stored=stored(service,runId);
        assertEquals(WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,stored.state());
        assertTrue(stored.runDiagnostic().recoveryPersisted());
        assertEquals(List.of(),service.workflow(runId).availableActions());
    }

    @Test void interruptionIsPreservedAndPersistedAsInterruptedRatherThanTestFailure() throws Exception {
        assertFalse(Thread.interrupted(),"the fixture must begin without a pre-existing interrupt signal");
        Path sourceRoot=prepareReadyProject();
        ThrowingGateway gateway=new ThrowingGateway(new IllegalStateException(new InterruptedException("fixture")));
        TestEngineeringStudioService service=readyService(sourceRoot,gateway);
        String runId=readyRun(service);
        try{
            var failure=assertThrows(TestEngineeringStudioService.RunLifecycleException.class,
                    ()->service.runWorkflow(runId));
            assertEquals(WorkflowSessionSnapshot.RunReason.THREAD_INTERRUPTED,failure.reason());
            assertTrue(Thread.currentThread().isInterrupted(),"the cancellation signal must remain set");
            assertEquals(WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,stored(service,runId).state());
            assertNull(stored(service,runId).execution());
        }finally{Thread.interrupted();}
    }

    @Test void failureAfterExecutorReportIsNotReclassifiedAsFailedTest() throws Exception {
        Path sourceRoot=prepareReadyProject();
        PostProcessingFailureGateway gateway=new PostProcessingFailureGateway();
        TestEngineeringStudioService service=readyService(sourceRoot,gateway);
        String runId=readyRun(service);

        var failure=assertThrows(TestEngineeringStudioService.RunLifecycleException.class,
                ()->service.runWorkflow(runId));

        assertEquals(WorkflowSessionSnapshot.RunReason.POST_EXECUTION_PROCESSING_FAILED,failure.reason());
        WorkflowSessionSnapshot stored=stored(service,runId);
        assertEquals(WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,stored.state());
        assertNull(stored.execution(),"a report without validated evidence must not manufacture an execution result");
        assertEquals(List.of(),stored.availableActions());
    }

    @Test void normalPassAndFailedReportsRetainTheirActualOutcomes() throws Exception {
        Path passRoot=root.resolve("pass");
        Path passSource=prepareReadyProject(passRoot);
        TerminalGateway passGateway=new TerminalGateway(TestEngineeringRun.State.SUCCESS);
        TestEngineeringStudioService passService=readyService(passRoot,passSource,passGateway);
        String passRun=readyRun(passService);

        assertEquals("SUCCESS",passService.runWorkflow(passRun).finalState());
        assertEquals(WorkflowSessionSnapshot.State.SUCCESS,stored(passService,passRun).state());
        assertNull(stored(passService,passRun).runDiagnostic());

        Path failedRoot=root.resolve("failed");
        Path failedSource=prepareReadyProject(failedRoot);
        TerminalGateway failedGateway=new TerminalGateway(TestEngineeringRun.State.FAILED);
        TestEngineeringStudioService failedService=readyService(failedRoot,failedSource,failedGateway);
        String failedRun=readyRun(failedService);

        assertEquals("FAILED",failedService.runWorkflow(failedRun).finalState());
        assertEquals(WorkflowSessionSnapshot.State.EXECUTION_FAILED,stored(failedService,failedRun).state());
        assertNull(stored(failedService,failedRun).runDiagnostic());
    }

    @Test void concurrentAndRepeatedRunRequestsDoNotExecuteTheGatewayTwice() throws Exception {
        Path sourceRoot=prepareReadyProject();
        BlockingGateway gateway=new BlockingGateway();
        TestEngineeringStudioService service=readyService(sourceRoot,gateway);
        String runId=readyRun(service);
        var executor=Executors.newSingleThreadExecutor();
        try{
            var first=executor.submit(()->service.runWorkflow(runId));
            assertTrue(gateway.entered.await(5,TimeUnit.SECONDS));
            assertEquals(WorkflowSessionSnapshot.State.EXECUTION_RUNNING,stored(service,runId).state());

            IllegalStateException concurrent=assertThrows(IllegalStateException.class,
                    ()->service.runWorkflow(runId));
            assertEquals("A Studio operation is already running",concurrent.getMessage());
            gateway.release.countDown();
            assertEquals("SUCCESS",first.get(5,TimeUnit.SECONDS).finalState());
        }finally{
            gateway.release.countDown();
            executor.shutdownNow();
        }

        assertThrows(IllegalStateException.class,()->service.runWorkflow(runId));
        assertEquals(1,gateway.runCalls.get());
    }

    @Test void persistenceFailureBeforeExecutionDoesNotInvokeGatewayOrPublishRunningInMemory() throws Exception {
        Path sourceRoot=prepareReadyProject();
        TerminalGateway gateway=new TerminalGateway(TestEngineeringRun.State.SUCCESS);
        TestEngineeringStudioService service=readyService(sourceRoot,gateway);
        String runId=readyRun(service);
        Path sessions=service.workspace().workspaceRoot().resolve("ai/sessions");
        Files.delete(sessions.resolve(runId+".json"));
        Files.delete(sessions);
        Files.createFile(sessions);

        TestEngineeringStudioService.RunLifecycleException failure=assertThrows(
                TestEngineeringStudioService.RunLifecycleException.class,()->service.runWorkflow(runId));

        assertEquals(WorkflowSessionSnapshot.RunReason.START_PERSISTENCE_FAILED,failure.reason());
        assertEquals(WorkflowSessionSnapshot.RunStatus.NOT_STARTED,failure.status());
        assertFalse(failure.recoveryPersisted());
        assertEquals(0,gateway.runCalls.get());
        var workflow=service.workflow(runId);
        assertEquals("IMPLEMENTATION_READY_FOR_REVIEW",workflow.status());
        assertEquals("START_PERSISTENCE_FAILED",workflow.runDiagnostic().reason());
        assertEquals("NOT_STARTED",workflow.runDiagnostic().status());
        assertFalse(workflow.runDiagnostic().recoveryPersisted());
    }

    private TestEngineeringStudioService readyService(Path sourceRoot,StudioWorkflowGateway gateway){
        return readyService(root,sourceRoot,gateway);
    }

    private static TestEngineeringStudioService readyService(Path projectRoot,Path sourceRoot,
                                                               StudioWorkflowGateway gateway){
        return new TestEngineeringStudioService(new TestEngineeringStudioService.Configuration(projectRoot,
                List.of(sourceRoot),List.of(),List.of("src/test/java"),"fixture"),gateway,null);
    }

    private static String readyRun(TestEngineeringStudioService service) throws Exception {
        String runId=service.createRequirement("Login with invalid password shows an error");
        service.generatePlan(runId);
        service.generateImplementation(runId);
        return runId;
    }

    private static WorkflowSessionSnapshot stored(TestEngineeringStudioService service,String runId) throws Exception {
        return WorkflowSessionSnapshot.fromDocument(service.workspace().readWorkflow(runId).orElseThrow());
    }

    private Path prepareReadyProject() throws Exception { return prepareReadyProject(root); }

    private static Path prepareReadyProject(Path projectRoot) throws Exception {
        Path sourceRoot=projectRoot.resolve("src/test/java");
        Files.createDirectories(sourceRoot);
        Files.writeString(projectRoot.resolve("pom.xml"),
                "<project><modelVersion>4.0.0</modelVersion><artifactId>fixture</artifactId></project>");
        Files.writeString(sourceRoot.resolve("LoginPage.java"),
                "package example; import org.openqa.selenium.By; public final class LoginPage { " +
                        "private final By loginButton = By.id(\"old-login\"); public LoginPage(){} " +
                        "public boolean login(){ return true; } }");
        new StudioWorkspaceStore(projectRoot).write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,
                StrictJson.readObject(new ApplicationModelJson().write(model())),"initial");
        return sourceRoot;
    }

    private abstract static class ReadyGateway implements StudioWorkflowGateway {
        final AtomicInteger runCalls=new AtomicInteger();
        @Override public void prepare(String runId,TestEngineeringRequest request,AgentContextPack context) { }
        @Override public TestPlan generatePlan(String runId,String requirement){return new TestPlan(ready(),List.of());}
        @Override public TestImplementationProposal generateImplementation(String runId){
            return new TestImplementationProposal(ready(),"invalid-password",
                    "package fixture; final class InvalidPasswordTest {}",
                    TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                    List.of("LoginPage.login"),List.of());
        }
        @Override public List<PolicyCheck> implementationPolicy(String runId){
            return List.of(new PolicyCheck("PAGE_OBJECTS_ONLY",true,"safe"));
        }
    }

    private static final class InspectingFailureGateway extends ReadyGateway {
        private final StudioWorkspaceStore store;
        private WorkflowSessionSnapshot.State stateObservedInsideRun;
        private InspectingFailureGateway(StudioWorkspaceStore store){this.store=store;}
        @Override public WorkflowReport run(String runId){
            runCalls.incrementAndGet();
            try{
                stateObservedInsideRun=WorkflowSessionSnapshot.fromDocument(
                        store.readWorkflow(runId).orElseThrow()).state();
            }catch(java.io.IOException failure){throw new UncheckedIOException(failure);}
            throw new IllegalStateException("fixture executor failure");
        }
    }

    private static final class ThrowingGateway extends ReadyGateway {
        private final RuntimeException failure;
        private ThrowingGateway(RuntimeException failure){this.failure=failure;}
        @Override public WorkflowReport run(String runId){runCalls.incrementAndGet();throw failure;}
    }

    private static final class PostProcessingFailureGateway extends TerminalGateway {
        private PostProcessingFailureGateway(){super(TestEngineeringRun.State.SUCCESS);}
        @Override public TestImplementationProposal implementation(String runId){
            throw new IllegalStateException("fixture evidence projection failure");
        }
    }

    private static class TerminalGateway extends ReadyGateway {
        private final TestEngineeringRun.State result;
        private TerminalGateway(TestEngineeringRun.State result){this.result=result;}
        @Override public WorkflowReport run(String runId){
            runCalls.incrementAndGet();return report(runId,result);
        }
    }

    private static final class BlockingGateway extends TerminalGateway {
        private final CountDownLatch entered=new CountDownLatch(1);
        private final CountDownLatch release=new CountDownLatch(1);
        private BlockingGateway(){super(TestEngineeringRun.State.SUCCESS);}
        @Override public WorkflowReport run(String runId){
            runCalls.incrementAndGet();entered.countDown();
            try{
                if(!release.await(5,TimeUnit.SECONDS))throw new IllegalStateException("fixture timeout");
            }catch(InterruptedException failure){Thread.currentThread().interrupt();throw new IllegalStateException(failure);}
            return report(runId,TestEngineeringRun.State.SUCCESS);
        }
    }

    private static WorkflowReport report(String runId,TestEngineeringRun.State result){
        return new WorkflowReport(WorkflowReport.SCHEMA_VERSION,runId,"requirement",result,List.of(),
                new WorkflowReport.Metrics(0,0,0,0,0,0,0,0,0,0,1,0,0),List.of());
    }

    private static ContractHeader ready(){
        return new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,
                List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);
    }

    private static ApplicationModel model(){
        var selector=new ApplicationModel.SelectorProjection("id","old-login","observed-login",
                "VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),1,List.of(),List.of(),
                ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS);
        var element=new ApplicationModel.ElementModel("login-button","LoginButton",
                ApplicationModel.ElementType.BUTTON,"button","Sign in","Sign in",
                List.of(ApplicationModel.Action.CLICK),selector,List.of(),ApplicationModel.SelectorQuality.VERIFIED,
                null,"element-fp",provenance(),List.of());
        var page=new ApplicationModel.PageModel("login-page","Login",
                new ApplicationModel.PageIdentity("/login",null,"Login",List.of("login-button"),"page-fp",
                        ApplicationModel.EvidenceSource.OBSERVED),List.of("Login"),List.of("page-fp"),List.of(),
                List.of(),List.of(element),List.of(),List.of(),provenance(),List.of());
        return new ApplicationModel(ApplicationModel.SCHEMA_VERSION,"fixture-app","fixture","test",Instant.EPOCH,
                List.of(page),List.of(),List.of(),new ApplicationModel.Coverage(
                ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT,1,1,0,1,1,0,1,0,false),
                List.of(),provenance());
    }

    private static ApplicationModel.Provenance provenance(){
        return ApplicationModel.Provenance.observed("fixture","run-lifecycle-test",List.of("fixture"));
    }
}
