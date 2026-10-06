package io.github.testlens.application.tooling.ai.workflow;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/** Deterministic executor useful for offline orchestration and tests. @since 0.5.0 */
public final class ScriptedAgentExecutor implements AgentExecutor {
    private final Deque<AgentResult> results;

    public ScriptedAgentExecutor(Collection<AgentResult> results) {
        this.results = new ArrayDeque<>(results == null ? java.util.List.of() : results);
    }

    @Override public synchronized AgentResult execute(AgentCommand command) throws AgentExecutionException {
        if (results.isEmpty()) throw new AgentExecutionException("No scripted agent result remains for " + command.role());
        AgentResult result=results.removeFirst();if(!supports(command.role(),result.artifact().payload()))throw new AgentExecutionException("Structured result does not satisfy role contract: "+command.role());return result;
    }

    private static boolean supports(Role role,Object payload){return switch(role){
        case PLANNER,TEST_ARCHITECT,SCENARIO_DESIGNER->payload instanceof io.github.testlens.application.tooling.ai.TestPlan;
        case IMPLEMENTER,TEST_IMPLEMENTER,UNIT_TEST_AGENT->payload instanceof io.github.testlens.application.tooling.ai.TestImplementationProposal||payload instanceof PageObjectCapabilityMissing||payload instanceof PageObjectExtensionProposal;
        case TEST_VERIFIER->payload instanceof io.github.testlens.application.tooling.ai.TestExecutionResult;
        case FAILURE_CLASSIFIER->payload instanceof io.github.testlens.application.tooling.ai.FailureClassification;
        case REPAIRER,STABILIZER->payload instanceof io.github.testlens.application.tooling.ai.RepairProposal||payload instanceof io.github.testlens.application.tooling.ai.FailureClassification;
        case REVIEWER,CODE_REVIEWER->payload instanceof io.github.testlens.application.tooling.ai.CodeReviewResult;};}

    public synchronized int remaining() { return results.size(); }
}
