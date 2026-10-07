package io.github.testlens.application.tooling.ai.workflow.runner.provider;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;

import java.util.Map;
import java.util.Objects;

/** Explicit deterministic provider for tests and controlled CI; never selected as an automatic fallback. @since 0.5.0 */
public final class ScriptedAgentExecutorProvider implements AgentExecutorProvider {
    private final Map<AgentExecutor.Role, AgentExecutor> executors;

    public ScriptedAgentExecutorProvider(Map<AgentExecutor.Role, AgentExecutor> executors) {
        this.executors = Map.copyOf(Objects.requireNonNull(executors, "executors"));
    }

    @Override
    public ProviderAvailability availability() {
        return executors.isEmpty()
                ? new ProviderAvailability(Status.NOT_AVAILABLE, "No scripted executors were configured", null)
                : new ProviderAvailability(Status.AVAILABLE, "Explicit scripted provider is configured", "scripted");
    }

    @Override
    public ProviderAvailability preflight() { return availability(); }

    @Override
    public ProviderAvailability preflight(AgentExecutor.Role role, String logicalProfileId) {
        ProviderAvailability profile = AgentExecutorProvider.super.preflight(role, logicalProfileId);
        if (!profile.available()) return profile;
        return executors.containsKey(role) ? profile
                : new ProviderAvailability(Status.PROFILE_INVALID,
                "No scripted executor is configured for role " + role, "scripted");
    }

    @Override
    public AgentExecutor executorFor(AgentExecutor.Role role, String logicalProfileId) {
        ProviderAvailability result = preflight(role, logicalProfileId);
        if (!result.available()) throw new ProviderUnavailableException(result);
        return executors.get(role);
    }
}
