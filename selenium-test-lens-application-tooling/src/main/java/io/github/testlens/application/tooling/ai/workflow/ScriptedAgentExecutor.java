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
        return results.removeFirst();
    }

    public synchronized int remaining() { return results.size(); }
}
