package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.ContractHeader;
import io.github.testlens.application.tooling.ai.TestPlan;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleDispatchingAgentExecutorTest {
    @Test void dispatchesOnlyAnExplicitlyConfiguredRole() throws Exception {
        AgentExecutor architect = command -> new AgentExecutor.AgentResult("architect",
                ArtifactEnvelope.create("plan", command.runId(), List.of(), 1,
                        new TestPlan(header(), List.of())));
        var dispatcher = new RoleDispatchingAgentExecutor(Map.of(AgentExecutor.Role.TEST_ARCHITECT, architect));

        assertEquals("architect", dispatcher.execute(command(AgentExecutor.Role.TEST_ARCHITECT)).providerRequestId());
        var failure = assertThrows(AgentExecutor.AgentExecutionException.class,
                () -> dispatcher.execute(command(AgentExecutor.Role.TEST_IMPLEMENTER)));
        assertEquals(AgentExecutor.AgentFailureCode.AGENT_NOT_AVAILABLE, failure.code());
    }

    private static AgentExecutor.AgentCommand command(AgentExecutor.Role role) {
        return new AgentExecutor.AgentCommand("run", role, List.of(), Map.of());
    }

    private static ContractHeader header() {
        return new ContractHeader(ContractHeader.SCHEMA_VERSION, ContractHeader.Status.READY,
                List.of("fixture"), List.of(), ContractHeader.Confidence.OBSERVED);
    }
}
