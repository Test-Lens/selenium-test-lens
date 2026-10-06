package io.github.testlens.application.tooling.ai;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PromptPackSecurityTest {
    @Test
    void rejectsContextThatWasNotRedactedBeforeRendering() {
        ContractHeader header = new ContractHeader(1, ContractHeader.Status.READY, List.of(), List.of(),
                ContractHeader.Confidence.OBSERVED);
        AgentTask task = new AgentTask(header, AgentTask.TaskType.CREATE_TEST, "login with api-secret-value",
                List.of(), List.of(), List.of(), List.of());
        AgentContextPack context = new AgentContextPack(header, task, List.of(), Map.of(), List.of(),
                List.of(), List.of(), AgentContextPack.Completeness.COMPLETE_FOR_REQUESTED_SCOPE);
        RedactionPolicy policy = RedactionPolicy.builder().secret("api-secret-value").build();

        assertThrows(PromptPackRenderer.PromptSecurityException.class,
                () -> new PromptPackRenderer().render(PromptPackRenderer.Role.TEST_IMPLEMENTER, context,
                        100_000, policy, List.of()));
    }
}
