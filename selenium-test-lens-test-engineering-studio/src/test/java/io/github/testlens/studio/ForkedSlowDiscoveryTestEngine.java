package io.github.testlens.studio;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/** Test-only engine used to prove that subprocess deadlines include discovery. */
public final class ForkedSlowDiscoveryTestEngine implements TestEngine {
    @Override
    public String getId() {
        return "test-lens-slow-discovery";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        String profile = System.getProperty("testlens.targeted.profile", "");
        if (profile.startsWith("slow-discovery-")) {
            while (true) {
                Thread.onSpinWait();
            }
        }
        return new EngineDescriptor(uniqueId, "Test Lens slow discovery fixture");
    }

    @Override
    public void execute(ExecutionRequest request) {
        request.getEngineExecutionListener().executionStarted(request.getRootTestDescriptor());
        request.getEngineExecutionListener().executionFinished(
                request.getRootTestDescriptor(),
                org.junit.platform.engine.TestExecutionResult.successful());
    }
}
