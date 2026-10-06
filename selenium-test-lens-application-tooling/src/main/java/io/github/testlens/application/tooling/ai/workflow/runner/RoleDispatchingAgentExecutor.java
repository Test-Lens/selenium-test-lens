package io.github.testlens.application.tooling.ai.workflow.runner;

import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Exact role-to-runner dispatch without an implicit scripted or provider fallback. @since 0.5.0 */
public final class RoleDispatchingAgentExecutor implements AgentExecutor {
    private final Map<Role, AgentExecutor> delegates;

    public RoleDispatchingAgentExecutor(Map<Role, ? extends AgentExecutor> delegates) {
        EnumMap<Role, AgentExecutor> copy = new EnumMap<>(Role.class);
        if (delegates != null) delegates.forEach((role, delegate) ->
                copy.put(Objects.requireNonNull(role, "role"), Objects.requireNonNull(delegate, "delegate")));
        this.delegates = Map.copyOf(copy);
    }

    @Override public AgentResult execute(AgentCommand command) throws AgentExecutionException {
        Objects.requireNonNull(command, "command");
        AgentExecutor delegate = delegates.get(command.role());
        if (delegate == null) {
            throw new AgentExecutionException(AgentFailureCode.AGENT_NOT_AVAILABLE,
                    "No external agent profile is configured for role " + command.role());
        }
        return delegate.execute(command);
    }
}
