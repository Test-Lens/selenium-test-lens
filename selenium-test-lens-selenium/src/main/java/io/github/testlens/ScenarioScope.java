package io.github.testlens;

import java.util.List;

final class ScenarioScope {
    private final ScenarioStateManager state = new ScenarioStateManager();
    private final ScenarioResourceManager resources = new ScenarioResourceManager();
    private boolean closed;

    ScenarioScope() {
        ManagedStateSupport.scenarioOpened();
    }

    ScenarioStateManager state() { return state; }
    ScenarioResourceManager resources() { return resources; }

    synchronized List<Throwable> close() {
        if (closed) return List.of();
        closed = true;
        state.close();
        try {
            return resources.cleanup();
        } finally {
            ManagedStateSupport.scenarioClosed();
        }
    }
}
