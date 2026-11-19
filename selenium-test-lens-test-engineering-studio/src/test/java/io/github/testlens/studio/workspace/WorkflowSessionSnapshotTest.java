package io.github.testlens.studio.workspace;

import io.github.testlens.application.tooling.ai.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowSessionSnapshotTest{
    @Test void roundTripRetainsReviewedArtifactsAndAvailableAction(){
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);
        TestPlan plan=new TestPlan(header,List.of());TestImplementationProposal implementation=new TestImplementationProposal(header,"invalid-password","class Test {}",TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of("LoginPage.login"),List.of());
        Instant now=Instant.parse("2026-01-01T00:00:00Z");WorkflowSessionSnapshot snapshot=new WorkflowSessionSnapshot(1,"run-1","invalid password",WorkflowSessionSnapshot.State.IMPLEMENTATION_READY_FOR_REVIEW,now,now,"project","source",WorkflowSessionSnapshot.Freshness.FRESH,plan,implementation,null,null,null,null,null,false,List.of());
        WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(snapshot.toDocument());
        assertEquals(snapshot.workflowId(),restored.workflowId());assertEquals(implementation.sourcePatch(),restored.implementation().sourcePatch());assertEquals(List.of("RUN"),restored.availableActions());
    }
    @Test void staleReviewedImplementationCannotRun(){Instant now=Instant.EPOCH;WorkflowSessionSnapshot snapshot=new WorkflowSessionSnapshot(1,"run-2","requirement",WorkflowSessionSnapshot.State.IMPLEMENTATION_READY_FOR_REVIEW,now,now,"p","s",WorkflowSessionSnapshot.Freshness.STALE,null,null,null,null,null,null,null,true,List.of("SOURCE_CHANGED","RESUME_UNAVAILABLE"));assertEquals(List.of(),snapshot.availableActions());}
    @Test void futureSchemaAndMissingArtifactsFailClosed(){Map<String,Object> document=new LinkedHashMap<>();Instant now=Instant.EPOCH;WorkflowSessionSnapshot valid=new WorkflowSessionSnapshot(1,"run-3","requirement",WorkflowSessionSnapshot.State.CREATED,now,now,null,null,WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,false,List.of());document.putAll(valid.toDocument());document.put("schemaVersion",2);assertThrows(RuntimeException.class,()->WorkflowSessionSnapshot.fromDocument(document));}
    @Test void runningOperationsRestoreAsInterruptedWithoutImplicitRetry(){Instant now=Instant.EPOCH;WorkflowSessionSnapshot generating=new WorkflowSessionSnapshot(1,"run-4","requirement",WorkflowSessionSnapshot.State.PLAN_GENERATING,now,now,"p","s",WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,false,List.of());WorkflowSessionSnapshot restored=generating.restoredAfterInterruption(WorkflowSessionSnapshot.Freshness.FRESH,List.of("RESUME_UNAVAILABLE"));assertEquals(WorkflowSessionSnapshot.State.AGENT_EXECUTION_INTERRUPTED,restored.state());assertEquals(List.of(),restored.availableActions());}
    @Test void interruptedInitialExecutionCannotBeReplayedWithoutVerifiedLineage(){ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of(),List.of(),ContractHeader.Confidence.OBSERVED);TestImplementationProposal implementation=new TestImplementationProposal(header,"scenario","class Test {}",TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(),List.of());Instant now=Instant.EPOCH;WorkflowSessionSnapshot interrupted=new WorkflowSessionSnapshot(1,"run-5","requirement",WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,now,now,"p","s",WorkflowSessionSnapshot.Freshness.FRESH,null,implementation,null,null,null,null,null,true,List.of("EXECUTION_INTERRUPTED","RESUME_UNAVAILABLE"));assertEquals(List.of(),interrupted.availableActions());}
    @Test void legacyRunningSnapshotRestoresAsUnknownInterruptedExecutionWithoutReplay(){
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of(),List.of(),ContractHeader.Confidence.OBSERVED);
        TestImplementationProposal implementation=new TestImplementationProposal(header,"scenario","class Test {}",TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(),List.of());
        Instant now=Instant.EPOCH;
        WorkflowSessionSnapshot legacyRunning=new WorkflowSessionSnapshot(1,"run-legacy-running","requirement",
                WorkflowSessionSnapshot.State.EXECUTION_RUNNING,now,now,"p","s",WorkflowSessionSnapshot.Freshness.FRESH,
                null,implementation,null,null,null,null,null,false,List.of());

        WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(legacyRunning.toDocument())
                .restoredAfterInterruption(WorkflowSessionSnapshot.Freshness.FRESH,List.of("RESUME_UNAVAILABLE"));

        assertEquals(WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,restored.state());
        assertTrue(restored.resumed());
        assertEquals(List.of(),restored.availableActions());
        assertEquals(new WorkflowSessionSnapshot.RunDiagnosticSnapshot(
                WorkflowSessionSnapshot.RunReason.EXECUTION_RESULT_UNKNOWN,
                WorkflowSessionSnapshot.RunStatus.INTERRUPTED,"RUN",true),restored.runDiagnostic());
        assertNull(restored.execution());
    }
    @Test void legacyVerificationSnapshotRestoresWithRerunLocationAndWithoutImplicitReplay(){
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,
                List.of(),List.of(),ContractHeader.Confidence.OBSERVED);
        TestImplementationProposal implementation=new TestImplementationProposal(header,"scenario","class Test {}",
                TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,List.of(),List.of());
        Instant now=Instant.EPOCH;
        WorkflowSessionSnapshot verifying=new WorkflowSessionSnapshot(1,"run-legacy-rerun","requirement",
                WorkflowSessionSnapshot.State.VERIFICATION_RUNNING,now,now,"p","s",
                WorkflowSessionSnapshot.Freshness.FRESH,null,implementation,null,null,null,"APPROVED","APPLIED",
                false,List.of());

        WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(verifying.toDocument())
                .restoredAfterInterruption(WorkflowSessionSnapshot.Freshness.FRESH,List.of("RESUME_UNAVAILABLE"));

        assertEquals(WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,restored.state());
        assertEquals(new WorkflowSessionSnapshot.RunDiagnosticSnapshot(
                WorkflowSessionSnapshot.RunReason.EXECUTION_RESULT_UNKNOWN,
                WorkflowSessionSnapshot.RunStatus.INTERRUPTED,"RERUN",true),restored.runDiagnostic());
        assertEquals(List.of(),restored.availableActions());
        assertNull(restored.execution());
    }
    @Test void runDiagnosticSurvivesPersistenceRoundTripWithoutOpeningRunOrRerun(){
        Instant now=Instant.EPOCH;
        var diagnostic=new WorkflowSessionSnapshot.RunDiagnosticSnapshot(
                WorkflowSessionSnapshot.RunReason.POST_EXECUTION_PROCESSING_FAILED,
                WorkflowSessionSnapshot.RunStatus.INTERRUPTED,"RUN",true);
        var interrupted=new WorkflowSessionSnapshot(WorkflowSessionSnapshot.SCHEMA_VERSION,"run-diagnostic",
                "requirement",WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,now,now,"p","s",
                WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,false,List.of(),null,null,
                List.of(),diagnostic);

        WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(interrupted.toDocument());

        assertEquals(diagnostic,restored.runDiagnostic());
        assertEquals(List.of(),restored.availableActions());
    }
    @Test void distinctRepairClearsOldApprovalAcrossPersistenceRoundTrip(){ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("verified"),List.of(),ContractHeader.Confidence.LIVE_VALIDATED);RepairProposal.SelectorEvidence evidence=new RepairProposal.SelectorEvidence("cssSelector","[data-testid='new']","candidate-new","VERIFIED_IN_SCOPE","SAME_TARGET",true,List.of("STABLE"),"LIVE_CANDIDATE_ANALYSIS");RepairProposal.SourceTarget target=new RepairProposal.SourceTarget("element","declaration","src/test/java/Page.java",new RepairProposal.SourceRange(1,1,1,10,0,9),"file-fp","declaration-fp","EXACT","id","old");RepairProposal next=new RepairProposal(header,"repair-new",RepairProposal.ApplicationPolicy.PROPOSE_ONLY,"selector","changed","id:old","cssSelector:[data-testid='new']","declaration","candidate-old","candidate-new","classification","drift",List.of("SAME_TARGET"),List.of("STABLE"),List.of("Test"),List.of(),List.of("rerun"),target,evidence,List.of());Instant now=Instant.EPOCH;WorkflowSessionSnapshot approved=new WorkflowSessionSnapshot(1,"run-6","requirement",WorkflowSessionSnapshot.State.REPAIR_APPLIED_VERIFICATION_PENDING,now,now,"p","s",WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,next,"APPROVED","APPLIED",false,List.of());WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(approved.withNewRepair(next,now.plusSeconds(1)).toDocument());assertEquals(WorkflowSessionSnapshot.State.REPAIR_READY_FOR_REVIEW,restored.state());assertNull(restored.repairDecision());assertNull(restored.repairApplyStatus());assertEquals(List.of("DIAGNOSE","APPROVE_REPAIR","REJECT_REPAIR"),restored.availableActions());}

    @Test void agentFailureRoundTripRetainsBoundedStructuredDiagnosticsAndRetry(){
        Instant now=Instant.parse("2026-10-10T12:00:00Z");
        var failure=new WorkflowSessionSnapshot.AgentFailureSnapshot("AGENT_OUTPUT_INVALID","PLAN_GENERATION",
                "SCHEMA_INVALID","stdout","Planner output omitted the required scenarios array.",
                Map.of("role","TEST_ARCHITECT","resultType","TEST_PLAN"));
        var snapshot=new WorkflowSessionSnapshot(1,"run-failed","requirement",WorkflowSessionSnapshot.State.AGENT_FAILED,
                now,now,"project","source",WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,
                false,List.of(),failure);

        WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(snapshot.toDocument());

        assertEquals(failure,restored.agentFailure());
        assertEquals(WorkflowSessionSnapshot.State.AGENT_FAILED,restored.state());
        assertEquals(List.of("GENERATE_PLAN"),restored.availableActions());
    }

    @Test void agentFailureAcceptsBoundedJsonPathArrayLocations(){
        var failure=new WorkflowSessionSnapshot.AgentFailureSnapshot("AGENT_OUTPUT_INVALID","TEST_IMPLEMENTER",
                "SCHEMA_INVALID","$.payload.missingCapabilities[0].requiredCapability",
                "The Implementer output did not satisfy the response schema.",Map.of("role","TEST_IMPLEMENTER"));
        assertEquals("$.payload.missingCapabilities[0].requiredCapability",failure.location());
    }

    @Test void legacyVersionOneSnapshotWithoutAgentFailureStillLoads(){
        Instant now=Instant.EPOCH;
        var legacy=new WorkflowSessionSnapshot(1,"run-legacy","requirement",WorkflowSessionSnapshot.State.CREATED,
                now,now,null,null,WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,false,List.of())
                .toDocument();
        legacy.remove("agentFailure");
        legacy.remove("review");
        legacy.remove("reviewHistory");
        legacy.remove("runDiagnostic");

        WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(legacy);

        assertNull(restored.agentFailure());
        assertEquals(List.of("GENERATE_PLAN"),restored.availableActions());
    }

    @Test void requestChangesFeedbackAndVersionedReviewHistoryRoundTripWithoutFreeFormText(){
        Instant now=Instant.parse("2026-10-11T12:00:00Z");
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.COMPLETED,
                List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);
        CodeReviewResult.Finding finding=new CodeReviewResult.Finding(
                CodeReviewResult.Category.ASSERTION_INSUFFICIENT,CodeReviewResult.Severity.ERROR,
                "ASSERTION_REQUIRED","implementation-1",CodeReviewResult.ProblemCategory.ASSERTION,
                CodeReviewResult.RequiredAction.ADD_ASSERTION,CodeReviewResult.SafeLocation.ASSERTION,
                CodeReviewResult.EvidenceAvailability.VERIFIED);
        CodeReviewResult review=new CodeReviewResult(header,CodeReviewResult.Verdict.REQUEST_CHANGES,
                List.of("implementation-1","quality-1","compile-success-1","execution-1"),List.of(finding));
        var cycle=new WorkflowSessionSnapshot.ReviewCycleSnapshot(1,"implementation-1","execution-1",
                "review-1","PASS","PASS",2361,review);
        var snapshot=new WorkflowSessionSnapshot(WorkflowSessionSnapshot.SCHEMA_VERSION,"run-review","requirement",
                WorkflowSessionSnapshot.State.NEEDS_HUMAN_REVIEW,now,now,"project-fp","source-fp",
                WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,false,List.of(),null,
                review,List.of(cycle));

        WorkflowSessionSnapshot restored=WorkflowSessionSnapshot.fromDocument(snapshot.toDocument());

        assertEquals(CodeReviewResult.Verdict.REQUEST_CHANGES,restored.review().verdict());
        assertEquals(finding,restored.review().findings().get(0));
        assertEquals(1,restored.reviewHistory().size());
        assertEquals("implementation-1",restored.reviewHistory().get(0).implementationArtifactRef());
        String serialized=new String(io.github.testlens.application.tooling.json.StrictJson.write(snapshot.toDocument()),
                java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(serialized.contains("detail"));
        assertFalse(serialized.contains("recommendation"));
        assertTrue(serialized.contains("ASSERTION_REQUIRED"));
    }

    @Test void persistedReviewerDecisionRequiresCompletedContractStatus(){
        Instant now=Instant.parse("2026-10-11T12:00:00Z");
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.COMPLETED,
                List.of("fixture"),List.of(),ContractHeader.Confidence.OBSERVED);
        CodeReviewResult review=new CodeReviewResult(header,CodeReviewResult.Verdict.APPROVE,
                List.of("implementation-1","quality-1","compile-success-1","execution-1"),List.of());
        var snapshot=new WorkflowSessionSnapshot(WorkflowSessionSnapshot.SCHEMA_VERSION,"run-review-ready",
                "requirement",WorkflowSessionSnapshot.State.SUCCESS,now,now,"project-fp","source-fp",
                WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,false,List.of(),null,
                review,List.of());
        Map<String,Object> document=new LinkedHashMap<>(snapshot.toDocument());
        @SuppressWarnings("unchecked") Map<String,Object> persistedReview=
                new LinkedHashMap<>((Map<String,Object>)document.get("review"));
        @SuppressWarnings("unchecked") Map<String,Object> persistedHeader=
                new LinkedHashMap<>((Map<String,Object>)persistedReview.get("header"));
        persistedHeader.put("status",ContractHeader.Status.READY.name());
        persistedReview.put("header",persistedHeader);document.put("review",persistedReview);

        assertThrows(RuntimeException.class,()->WorkflowSessionSnapshot.fromDocument(document),
                "a READY reviewer response must not survive persistence as a completed decision");
    }
}
