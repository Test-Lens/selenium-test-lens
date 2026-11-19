package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest;
import io.github.testlens.application.tooling.ai.workflow.WorkflowReport;

import java.util.List;

/** Host-supplied bridge to the real S12 workflow. No domain decisions belong here. */
public interface StudioWorkflowGateway {
    default void prepare(String runId, TestEngineeringRequest request, AgentContextPack context) { }
    default boolean supportsReviewedArtifactRestore() { return false; }
    default void restoreReviewedArtifacts(String runId, TestPlan plan, TestImplementationProposal implementation) {
        throw unsupported("workflow restore");
    }
    default boolean supportsTrustedRepairContextRefresh() { return false; }
    default void refreshTrustedRepairContext(String runId, AgentContextPack context) {
        throw unsupported("trusted repair context refresh");
    }
    default TestPlan generatePlan(String runId, String requirement) throws AgentExecutor.AgentExecutionException { throw unsupported("plan"); }
    default TestImplementationProposal generateImplementation(String runId) throws AgentExecutor.AgentExecutionException { throw unsupported("implementation"); }
    default List<PolicyCheck> implementationPolicy(String runId) { return List.of(); }
    default TestImplementationProposal implementation(String runId){return null;}
    WorkflowReport run(String runId) throws AgentExecutor.AgentExecutionException;
    default TestExecutionResult execution(String runId){return null;}
    default FailureClassification diagnosis(String runId){return null;}
    default RepairProposal repair(String runId){return null;}
    default CodeReviewResult review(String runId){return null;}
    default List<ReviewCycle> reviewHistory(String runId){return List.of();}
    default WorkflowReport rerun(String runId, RepairProposal appliedRepair) throws AgentExecutor.AgentExecutionException {
        if(appliedRepair!=null)throw unsupported("repair verification with repaired-source compilation");
        return run(runId);
    }
    record PolicyCheck(String rule, boolean passed, String detail) {
        public PolicyCheck { if(rule==null||rule.isBlank())throw new IllegalArgumentException("policy rule is required");detail=detail==null?"":detail; }
    }
    record ReviewCycle(int implementationVersion,String implementationArtifactRef,String executionArtifactRef,
                       String reviewArtifactRef,TestExecutionResult execution,CodeReviewResult review) {
        public ReviewCycle {
            if(implementationVersion<1||implementationArtifactRef==null||implementationArtifactRef.isBlank()
                    ||executionArtifactRef==null||executionArtifactRef.isBlank()
                    ||reviewArtifactRef==null||reviewArtifactRef.isBlank()||execution==null||review==null)
                throw new IllegalArgumentException("Complete bounded review-cycle evidence is required");
        }
    }
    private static UnsupportedOperationException unsupported(String operation){return new UnsupportedOperationException("Workflow gateway does not expose staged "+operation);}
}
