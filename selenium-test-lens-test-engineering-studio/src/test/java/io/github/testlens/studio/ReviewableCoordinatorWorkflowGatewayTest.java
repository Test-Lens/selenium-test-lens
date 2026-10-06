package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ReviewableCoordinatorWorkflowGatewayTest {
    @Test void reviewStagesDoNotCompileOrExecuteAndRunReusesReviewedArtifacts() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        TestPlan plan = new TestPlan(ready(), List.of());
        TestImplementationProposal implementation = new TestImplementationProposal(ready(), "invalid-password",
                "package example; public final class GeneratedTest { public static void execute() {} }",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY, List.of("LoginPage.login"), List.of());
        ScriptedAgentExecutor agents = scripted(plan, implementation,
                new CodeReviewResult(ready(), CodeReviewResult.Verdict.APPROVE, List.of("reviewed"), List.of()));
        var input = new CoordinatorWorkflowGateway.Input(request(), context());
        ReviewableCoordinatorWorkflowGateway gateway = new ReviewableCoordinatorWorkflowGateway(agents,
                replay -> coordinator(replay, executions));
        gateway.prepare("run-1", input.request(), input.context());

        assertSame(plan, gateway.generatePlan("run-1", "Invalid password shows an error"));
        assertEquals(0, executions.get(), "plan review must not execute a test");
        assertSame(implementation, gateway.generateImplementation("run-1"));
        assertFalse(gateway.implementationPolicy("run-1").isEmpty());
        assertTrue(gateway.implementationPolicy("run-1").stream().allMatch(StudioWorkflowGateway.PolicyCheck::passed));
        assertEquals(0, executions.get(), "implementation review must not execute a test");

        WorkflowReport report = gateway.run("run-1");

        assertEquals(TestEngineeringRun.State.SUCCESS, report.finalState());
        assertEquals(1, executions.get());
        assertEquals(0, agents.remaining(), "reviewed plan and implementation are replayed, not regenerated");
        assertNotNull(gateway.execution("run-1"));
    }

    @Test void runIsBlockedUntilImplementationWasReviewed() throws Exception {
        var gateway = new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(), List.of())),
                replay -> coordinator(replay, new AtomicInteger()));
        gateway.prepare("run-2", request(), context());
        gateway.generatePlan("run-2", "requirement");
        assertThrows(IllegalStateException.class, () -> gateway.run("run-2"));
    }

    @Test void deterministicPolicyBlocksExecutionBeforeCompilerOrBrowser() throws Exception {
        AtomicInteger executions=new AtomicInteger();
        TestImplementationProposal unsafe=new TestImplementationProposal(ready(),"unsafe",
                "class Unsafe { Object x = org.openqa.selenium.By.id(\"raw\"); }",
                TestImplementationProposal.SelectorAccessPolicy.RAW_SELECTORS_EXPLICITLY_ALLOWED,List.of(),List.of());
        var gateway=new ReviewableCoordinatorWorkflowGateway(scripted(new TestPlan(ready(),List.of()),unsafe),
                replay->coordinator(replay,executions));
        gateway.prepare("run-policy",request(),context());gateway.generatePlan("run-policy","requirement");gateway.generateImplementation("run-policy");
        assertTrue(gateway.implementationPolicy("run-policy").stream().anyMatch(check->!check.passed()));
        assertThrows(IllegalStateException.class,()->gateway.run("run-policy"));
        assertEquals(0,executions.get());
    }

    @Test void repairRerunCompilesHandwrittenSourceThenUsesFreshCoordinatorAndArtifacts() throws Exception {
        AtomicInteger executions=new AtomicInteger(),repairCompiles=new AtomicInteger();
        TestPlan plan=new TestPlan(ready(),List.of());
        TestImplementationProposal implementation=new TestImplementationProposal(ready(),"invalid-password",
                "package example; public final class GeneratedTest { public static void execute() {} }",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of("LoginPage.login"),List.of());
        ScriptedAgentExecutor agents=scripted(plan,implementation,review(),review());
        var gateway=new ReviewableCoordinatorWorkflowGateway(agents,replay->coordinator(replay,executions),
                new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),ignored->Path.of("src/test/java/example/GeneratedTest.java"),
                proposal->{assertEquals("repair-1",proposal.proposalId());repairCompiles.incrementAndGet();});
        gateway.prepare("run-rerun",request(),context());gateway.generatePlan("run-rerun","requirement");gateway.generateImplementation("run-rerun");gateway.run("run-rerun");
        WorkflowReport rerun=gateway.rerun("run-rerun",repair());
        assertEquals(TestEngineeringRun.State.SUCCESS,rerun.finalState());
        assertEquals(1,repairCompiles.get());assertEquals(2,executions.get());assertNotNull(gateway.execution("run-rerun"));
    }

    private static AgentWorkflowCoordinator coordinator(AgentExecutor agents, AtomicInteger executions) {
        return new AgentWorkflowCoordinator(new TestEngineeringWorkflow(WorkflowPolicy.defaults()), agents,
                new TargetedJavaCompiler(), (request, output) -> {
                    executions.incrementAndGet();
                    return new TargetedTestExecutor.ExecutionResult(true, 1, List.of("Lens PASS"));
                }, new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                execution -> new FailureClassification(ready(), FailureClassification.Category.UNKNOWN,
                        "unclassified", execution.traceEvidenceRefs(), List.of(), List.of()),
                AgentWorkflowCoordinator.Stabilizer.none(),
                new AgentWorkflowCoordinator.Configuration(Path.of("src/test/java/example/GeneratedTest.java"),
                        "example.GeneratedTest", null, 17, "", 10, Duration.ofSeconds(5),
                        List.of("example.GeneratedTest"), 2));
    }

    private static ScriptedAgentExecutor scripted(Object... payloads) {
        List<AgentExecutor.AgentResult> results = new ArrayList<>();
        for (int i = 0; i < payloads.length; i++) results.add(new AgentExecutor.AgentResult("scripted-" + i,
                ArtifactEnvelope.create("artifact-" + i, "fixture", List.of(), 1, payloads[i])));
        return new ScriptedAgentExecutor(results);
    }
    private static TestEngineeringRequest request() {
        return new TestEngineeringRequest(new TestEngineeringRequest.Requirement("Invalid password shows an error", List.of("error visible")),
                new TestEngineeringRequest.Scope(List.of("login"), List.of()),
                new TestEngineeringRequest.Framework("JUnit", "5", "junit-jupiter"),
                new TestEngineeringRequest.Target("app", "GeneratedTest", "invalid-password"),
                List.of("src/test/java"), TestEngineeringRequest.ExecutionPolicy.TARGETED_AFTER_COMPILE);
    }
    private static AgentContextPack context() {
        return new AgentContextPack(ready(), new AgentTask(ready(), AgentTask.TaskType.CREATE_TEST,
                "Invalid password shows an error", List.of("login"), List.of("login-error"),
                List.of("login-submit"), List.of("Page Objects only")), List.of(),
                Map.of("LoginPage", List.of("login(String,String)", "errorVisible()")),
                List.of("JUnit 5"), List.of(), List.of(), AgentContextPack.Completeness.COMPLETE_FOR_REQUESTED_SCOPE);
    }
    private static CodeReviewResult review(){return new CodeReviewResult(ready(),CodeReviewResult.Verdict.APPROVE,List.of("reviewed"),List.of());}
    private static RepairProposal repair(){return new RepairProposal(ready(),"repair-1",RepairProposal.ApplicationPolicy.PROPOSE_ONLY,
            "selector","validated","id:old","css:[data-testid='new']",List.of("SAME_TARGET"),List.of("STABLE"),List.of("InvalidLoginTest"));}
    private static ContractHeader ready() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }
}
