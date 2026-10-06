package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.CodeReviewResult;
import io.github.testlens.application.tooling.ai.FailureClassification;
import io.github.testlens.application.tooling.ai.RepairProposal;
import io.github.testlens.application.tooling.ai.TestExecutionResult;
import io.github.testlens.application.tooling.ai.TestImplementationProposal;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.core.redaction.RedactionPolicy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TestEngineeringWorkflowTest {
    private final TestEngineeringWorkflow workflow = new TestEngineeringWorkflow(WorkflowPolicy.defaults());

    @Test void followsSuccessfulLifecycleAndKeepsArtifactProvenance() {
        TestEngineeringRun run = TestEngineeringRun.create("run-1", request());
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.PREPARE_CONTEXT));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_PLAN));
        ArtifactEnvelope<TestPlan> plan = ArtifactEnvelope.create("plan", "run-1", List.of(), 1, plan());
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PLAN_PRODUCED, plan));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION));
        ArtifactEnvelope<TestImplementationProposal> implementation = ArtifactEnvelope.create("implementation", "run-1", List.of("plan"), 1, implementation());
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.IMPLEMENTATION_PRODUCED, implementation));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.COMPILE_SUCCEEDED));
        ArtifactEnvelope<TestExecutionResult> execution = ArtifactEnvelope.create("execution", "run-1", List.of("implementation"), 1, execution(TestExecutionResult.Outcome.PASS));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED, execution));
        assertEquals(TestEngineeringRun.State.REVIEW_REQUESTED, run.state());
        ArtifactEnvelope<CodeReviewResult> review = ArtifactEnvelope.create("review", "run-1", List.of("execution"), 1, review(CodeReviewResult.Verdict.APPROVE));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.REVIEW_APPROVED, review));

        assertEquals(TestEngineeringRun.State.SUCCESS, run.state());
        assertEquals(4, run.artifacts().size());
        assertEquals(1, run.metrics().compilations());
        assertEquals(1, run.metrics().executions());
        assertEquals(8, run.audit().size());
    }

    @ParameterizedTest @MethodSource("illegalTransitions")
    void rejectsIllegalTransitions(TestEngineeringRun.State state, TestEngineeringWorkflow.EventType event) {
        TestEngineeringRun run = new TestEngineeringRun("run-1", request(), state, List.of(),
                new TestEngineeringRun.Metrics(0, 0, 0, 0, 0, 0), List.of(), null);
        TestEngineeringWorkflow.Event transition = event == TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED
                ? TestEngineeringWorkflow.Event.withArtifact(event,
                    ArtifactEnvelope.create("event", "run-1", List.of(), 1, execution(TestExecutionResult.Outcome.PASS)))
                : TestEngineeringWorkflow.Event.of(event);
        assertThrows(IllegalStateException.class, () -> workflow.reduce(run, transition));
    }

    static Stream<Arguments> illegalTransitions() {
        return Stream.of(
                Arguments.of(TestEngineeringRun.State.CREATED, TestEngineeringWorkflow.EventType.REQUEST_PLAN),
                Arguments.of(TestEngineeringRun.State.PLAN_READY, TestEngineeringWorkflow.EventType.COMPILE_SUCCEEDED),
                Arguments.of(TestEngineeringRun.State.COMPILE_FAILED, TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED),
                Arguments.of(TestEngineeringRun.State.AWAITING_TRUSTED_APPLY, TestEngineeringWorkflow.EventType.RERUN_REQUESTED));
    }

    @ParameterizedTest @MethodSource("legalTransitions")
    void performsRepresentativeLegalTransitions(TestEngineeringRun.State state,
            TestEngineeringWorkflow.EventType event, TestEngineeringRun.State expected) {
        TestEngineeringRun run = new TestEngineeringRun("run-1", request(), state, List.of(),
                new TestEngineeringRun.Metrics(0, 0, 0, 0, 0, 0), List.of(), null);
        assertEquals(expected, workflow.reduce(run, TestEngineeringWorkflow.Event.of(event)).state());
    }

    static Stream<Arguments> legalTransitions() {
        return Stream.of(
                Arguments.of(TestEngineeringRun.State.CREATED, TestEngineeringWorkflow.EventType.PREPARE_CONTEXT,
                        TestEngineeringRun.State.CONTEXT_PREPARED),
                Arguments.of(TestEngineeringRun.State.CONTEXT_PREPARED, TestEngineeringWorkflow.EventType.REQUEST_PLAN,
                        TestEngineeringRun.State.PLAN_REQUESTED),
                Arguments.of(TestEngineeringRun.State.PLAN_READY, TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION,
                        TestEngineeringRun.State.IMPLEMENTATION_REQUESTED),
                Arguments.of(TestEngineeringRun.State.REPAIR_PROPOSED, TestEngineeringWorkflow.EventType.REQUEST_TRUSTED_APPLY,
                        TestEngineeringRun.State.AWAITING_TRUSTED_APPLY),
                Arguments.of(TestEngineeringRun.State.RERUN_REQUIRED, TestEngineeringWorkflow.EventType.RERUN_REQUESTED,
                        TestEngineeringRun.State.IMPLEMENTATION_READY));
    }

    @Test void rejectsCrossRunUnknownParentAndDigestMismatch() {
        TestEngineeringRun requested = atPlanRequested();
        ArtifactEnvelope<TestPlan> otherRun = ArtifactEnvelope.create("plan", "other", List.of(), 1, plan());
        assertThrows(IllegalArgumentException.class, () -> workflow.reduce(requested,
                TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PLAN_PRODUCED, otherRun)));
        ArtifactEnvelope<TestPlan> unknownParent = ArtifactEnvelope.create("plan", "run-1", List.of("missing"), 1, plan());
        assertThrows(IllegalArgumentException.class, () -> workflow.reduce(requested,
                TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PLAN_PRODUCED, unknownParent)));
        assertThrows(IllegalArgumentException.class,
                () -> new ArtifactEnvelope<>("id", "run-1", List.of(), 1, "wrong", plan()));
    }

    @Test void compileFailureCannotExecuteAndCorrectionBoundRequiresHumanReview() {
        TestEngineeringWorkflow bounded = new TestEngineeringWorkflow(new WorkflowPolicy(0, 1, 1, 20, 50));
        TestEngineeringRun run = readyToCompile(bounded);
        var failedCompilation = new TargetedJavaCompiler().compile(new TargetedJavaCompiler.CompilationRequest(
                List.of(new TargetedJavaCompiler.SourceUnit(java.nio.file.Path.of("Broken.java"), "Broken", "class Broken {", ArtifactEnvelope.digest(""))),
                17, System.getProperty("java.class.path"), 10), java.util.Map.of());
        ArtifactEnvelope<TargetedJavaCompiler.CompilationResult> diagnostic = ArtifactEnvelope.create("compile-1", "run-1", List.of("implementation"), 1, failedCompilation);
        run = bounded.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.COMPILE_FAILED, diagnostic));
        TestEngineeringRun failed = run;
        assertThrows(IllegalStateException.class, () -> bounded.reduce(failed,
                TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED,
                        ArtifactEnvelope.create("exec", "run-1", List.of("compile-1"), 1, execution(TestExecutionResult.Outcome.PASS)))));
        run = bounded.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_CORRECTION));
        assertEquals(TestEngineeringRun.State.NEEDS_HUMAN_REVIEW, run.state());
        assertEquals(0, run.metrics().corrections());
    }

    @Test void repairRequiresExplicitTrustedApplyMarkerBeforeRerun() {
        TestEngineeringRun run = readyToCompile(workflow);
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.COMPILE_SUCCEEDED));
        ArtifactEnvelope<TestExecutionResult> failed = ArtifactEnvelope.create("execution", "run-1", List.of("implementation"), 1, execution(TestExecutionResult.Outcome.FAIL));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.EXECUTION_FAILED, failed));
        ArtifactEnvelope<FailureClassification> classification = ArtifactEnvelope.create("classification", "run-1", List.of("execution"), 1, classification());
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.CLASSIFY_FAILURE, classification));
        RepairProposal proposal = new RepairProposal(
                new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                        List.of("failure"), List.of(), ContractHeader.Confidence.AI_PROPOSED),
                "repair-1", RepairProposal.ApplicationPolicy.PROPOSE_ONLY, "change wait", "classified failure",
                null, null, List.of("same page"), List.of("stable"), List.of("LoginTest"));
        ArtifactEnvelope<RepairProposal> repair = ArtifactEnvelope.create("repair", "run-1", List.of("classification"), 1, proposal);
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PROPOSE_REPAIR, repair));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_TRUSTED_APPLY));
        TestEngineeringRun awaiting = run;
        assertThrows(IllegalArgumentException.class, () -> workflow.reduce(awaiting,
                TestEngineeringWorkflow.Event.trustedApply("")));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.trustedApply("apply:fingerprint-123"));
        assertEquals(TestEngineeringRun.State.RERUN_REQUIRED, run.state());
        assertEquals("apply:fingerprint-123", run.trustedApplyMarker());
    }

    @Test void rerunAndRepairBoundsFailClosedWithoutExceedingMetrics() {
        TestEngineeringRun rerun = new TestEngineeringRun("run-1", request(), TestEngineeringRun.State.RERUN_REQUIRED,
                List.of(), new TestEngineeringRun.Metrics(0, 2, 0, 0, 0, 0), List.of(), "trusted");
        rerun = workflow.reduce(rerun, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.RERUN_REQUESTED));
        assertEquals(TestEngineeringRun.State.NEEDS_HUMAN_REVIEW, rerun.state());
        assertEquals(2, rerun.metrics().reruns());

        RepairProposal proposal = new RepairProposal(
                new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                        List.of("failure"), List.of(), ContractHeader.Confidence.AI_PROPOSED),
                "repair-limit", RepairProposal.ApplicationPolicy.PROPOSE_ONLY, "change", "reason",
                null, null, List.of(), List.of(), List.of());
        TestEngineeringRun repair = new TestEngineeringRun("run-1", request(), TestEngineeringRun.State.FAILURE_CLASSIFIED,
                List.of(), new TestEngineeringRun.Metrics(0, 0, 2, 0, 0, 0), List.of(), null);
        repair = workflow.reduce(repair, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PROPOSE_REPAIR,
                ArtifactEnvelope.create("repair-limit", "run-1", List.of(), 3, proposal)));
        assertEquals(TestEngineeringRun.State.NEEDS_HUMAN_REVIEW, repair.state());
        assertEquals(2, repair.metrics().repairs());
    }

    @Test void missingPageObjectCapabilityBlocksWithExtensionProposalInsteadOfSelector() {
        TestEngineeringRun run = TestEngineeringRun.create("run-1", request());
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.PREPARE_CONTEXT));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_PLAN));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PLAN_PRODUCED,
                ArtifactEnvelope.create("plan", "run-1", List.of(), 1, plan())));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION));
        PageObjectExtensionProposal proposal = new PageObjectExtensionProposal(
                new PageObjectCapabilityMissing("login", "submit", "no submit method"),
                "LoginPage", "public void submit()", List.of("page-api"));
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.CAPABILITY_MISSING,
                ArtifactEnvelope.create("capability", "run-1", List.of("plan"), 1, proposal)));
        assertEquals(TestEngineeringRun.State.BLOCKED, run.state());
    }

    @Test void rejectsWrongPayloadAndReviewVerdictInsteadOfAdvancingState() {
        TestEngineeringRun requested = atPlanRequested();
        assertThrows(IllegalArgumentException.class, () -> workflow.reduce(requested,
                TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PLAN_PRODUCED,
                        ArtifactEnvelope.create("wrong", "run-1", List.of(), 1, implementation()))));

        TestEngineeringRun reviewRequested = readyToCompile(workflow);
        reviewRequested = workflow.reduce(reviewRequested, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.COMPILE_SUCCEEDED));
        reviewRequested = workflow.reduce(reviewRequested, TestEngineeringWorkflow.Event.withArtifact(
                TestEngineeringWorkflow.EventType.EXECUTION_SUCCEEDED,
                ArtifactEnvelope.create("execution", "run-1", List.of("implementation"), 1,
                        execution(TestExecutionResult.Outcome.PASS))));
        TestEngineeringRun finalReviewRequested = reviewRequested;
        assertThrows(IllegalArgumentException.class, () -> workflow.reduce(finalReviewRequested,
                TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.REVIEW_APPROVED,
                        ArtifactEnvelope.create("review", "run-1", List.of("execution"), 1,
                                review(CodeReviewResult.Verdict.REQUEST_CHANGES)))));
    }

    @Test void rejectsSecretCanaryBeforeArtifactEntersWorkflowRun() {
        TestEngineeringWorkflow secured = new TestEngineeringWorkflow(WorkflowPolicy.defaults(),
                RedactionPolicy.defaults(), List.of("real-password-value"));
        TestEngineeringRun run = TestEngineeringRun.create("run-1", request());
        run = secured.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.PREPARE_CONTEXT));
        run = secured.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_PLAN));
        run = secured.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PLAN_PRODUCED,
                ArtifactEnvelope.create("plan", "run-1", List.of(), 1, plan())));
        run = secured.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION));
        TestEngineeringRun requested = run;
        TestImplementationProposal unsafe = new TestImplementationProposal(ready(), "scenario",
                "page.loginAs(\"user\", \"real-password-value\");",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY, List.of(), List.of());
        assertThrows(SecurityException.class, () -> secured.reduce(requested,
                TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.IMPLEMENTATION_PRODUCED,
                        ArtifactEnvelope.create("unsafe", "run-1", List.of("plan"), 1, unsafe))));
        assertEquals(1, requested.artifacts().size());
    }

    private TestEngineeringRun atPlanRequested() {
        TestEngineeringRun run = TestEngineeringRun.create("run-1", request());
        run = workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.PREPARE_CONTEXT));
        return workflow.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_PLAN));
    }

    private static TestEngineeringRun readyToCompile(TestEngineeringWorkflow reducer) {
        TestEngineeringRun run = TestEngineeringRun.create("run-1", request());
        run = reducer.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.PREPARE_CONTEXT));
        run = reducer.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_PLAN));
        run = reducer.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.PLAN_PRODUCED,
                ArtifactEnvelope.create("plan", "run-1", List.of(), 1, plan())));
        run = reducer.reduce(run, TestEngineeringWorkflow.Event.of(TestEngineeringWorkflow.EventType.REQUEST_IMPLEMENTATION));
        return reducer.reduce(run, TestEngineeringWorkflow.Event.withArtifact(TestEngineeringWorkflow.EventType.IMPLEMENTATION_PRODUCED,
                ArtifactEnvelope.create("implementation", "run-1", List.of("plan"), 1, implementation())));
    }

    private static TestEngineeringRequest request() {
        return new TestEngineeringRequest(
                new TestEngineeringRequest.Requirement("test login", List.of("user logs in")),
                new TestEngineeringRequest.Scope(List.of("login"), List.of()),
                new TestEngineeringRequest.Framework("JUnit", "5", "junit-jupiter"),
                new TestEngineeringRequest.Target("app", "LoginTest", "login"),
                List.of("src/test/java"), TestEngineeringRequest.ExecutionPolicy.TARGETED_AFTER_COMPILE);
    }

    private static ContractHeader ready() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("test"), List.of(), ContractHeader.Confidence.OBSERVED);
    }

    private static TestPlan plan() { return new TestPlan(ready(), List.of()); }

    private static TestImplementationProposal implementation() {
        return new TestImplementationProposal(ready(), "scenario", "final class GeneratedTest {}",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY, List.of(), List.of());
    }

    private static TestExecutionResult execution(TestExecutionResult.Outcome outcome) {
        return new TestExecutionResult(ready(), "scenario", TestExecutionResult.Outcome.PASS, outcome,
                List.of("trace"), List.of(), List.of(), outcome == TestExecutionResult.Outcome.FAIL ? "failed" : null);
    }

    private static FailureClassification classification() {
        return new FailureClassification(ready(), FailureClassification.Category.UNKNOWN, "unknown",
                List.of("trace"), List.of(), List.of());
    }

    private static CodeReviewResult review(CodeReviewResult.Verdict verdict) {
        return new CodeReviewResult(ready(), verdict, List.of("implementation"), List.of());
    }
}
