package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.*;
import io.github.testlens.application.tooling.json.StrictJson;

import java.util.Objects;
import java.util.function.Function;

/** Compatibility adapter retaining the existing monolithic AgentWorkflowCoordinator. */
public final class CoordinatorWorkflowGateway implements StudioWorkflowGateway {
    public record Input(TestEngineeringRequest request, AgentContextPack context) { public Input{Objects.requireNonNull(request);Objects.requireNonNull(context);} }
    private final AgentWorkflowCoordinator coordinator;
    private final Function<String,Input> inputs;
    public CoordinatorWorkflowGateway(AgentWorkflowCoordinator coordinator,Function<String,Input> inputs){this.coordinator=Objects.requireNonNull(coordinator);this.inputs=Objects.requireNonNull(inputs);}
    @Override public WorkflowReport run(String runId)throws AgentExecutor.AgentExecutionException{
        Input input=Objects.requireNonNull(inputs.apply(runId),"No workflow input for run " + runId);
        AgentWorkflowCoordinator.Result result=coordinator.run(runId,input.request(),input.context());
        return result.toWorkflowReport(StrictJson.write(input.context()).length,
                input.context().header().limitations());
    }
}
