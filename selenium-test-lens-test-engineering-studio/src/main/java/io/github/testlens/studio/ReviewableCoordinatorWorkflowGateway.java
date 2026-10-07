package io.github.testlens.studio;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.*;
import io.github.testlens.application.tooling.json.StrictJson;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.nio.file.Path;

/**
 * Reviewable adapter for the S12 coordinator. Architect and implementer results are produced once,
 * exposed to Studio for human review, then replayed into the unmodified compile/run pipeline.
 * No test source is applied and no browser is started before {@link #run(String)}.
 */
public final class ReviewableCoordinatorWorkflowGateway implements StudioWorkflowGateway {
    private final AgentExecutor agents;
    private final BiFunction<AgentExecutor, CoordinatorWorkflowGateway.Input, AgentWorkflowCoordinator> coordinators;
    private final GeneratedTestPolicyValidator policyValidator;
    private final Function<TestEngineeringRequest,Path> generatedSourcePath;
    private final RepairSourceCompiler repairSourceCompiler;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, CoordinatorWorkflowGateway.Input> inputs = new ConcurrentHashMap<>();

    public ReviewableCoordinatorWorkflowGateway(
            AgentExecutor agents,
            Function<AgentExecutor, AgentWorkflowCoordinator> coordinators) {
        this(agents,(executor,ignored)->coordinators.apply(executor),new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ReviewableCoordinatorWorkflowGateway::defaultGeneratedSourcePath,proposal->{throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED,"Repair verification compiler is not configured");});
    }

    /** Builds the deterministic compiler/execution pipeline from the reviewed request and its project context. */
    public ReviewableCoordinatorWorkflowGateway(
            AgentExecutor agents,
            BiFunction<AgentExecutor, CoordinatorWorkflowGateway.Input, AgentWorkflowCoordinator> coordinators) {
        this(agents,coordinators,new GeneratedTestPolicyValidator(GeneratedTestPolicyValidator.Policy.defaults()),
                ReviewableCoordinatorWorkflowGateway::defaultGeneratedSourcePath,proposal->{throw new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_PROCESS_FAILED,"Repair verification compiler is not configured");});
    }

    public ReviewableCoordinatorWorkflowGateway(AgentExecutor agents,
            Function<AgentExecutor, AgentWorkflowCoordinator> coordinators,
            GeneratedTestPolicyValidator policyValidator,
            Function<TestEngineeringRequest,Path> generatedSourcePath,
            RepairSourceCompiler repairSourceCompiler) {
        this(agents, (executor, ignored) -> coordinators.apply(executor), policyValidator,
                generatedSourcePath, repairSourceCompiler);
    }

    public ReviewableCoordinatorWorkflowGateway(AgentExecutor agents,
            BiFunction<AgentExecutor, CoordinatorWorkflowGateway.Input, AgentWorkflowCoordinator> coordinators,
            GeneratedTestPolicyValidator policyValidator,
            Function<TestEngineeringRequest,Path> generatedSourcePath,
            RepairSourceCompiler repairSourceCompiler) {
        this.agents = Objects.requireNonNull(agents, "agents");
        this.coordinators = Objects.requireNonNull(coordinators, "coordinators");
        this.policyValidator=Objects.requireNonNull(policyValidator,"policyValidator");
        this.generatedSourcePath=Objects.requireNonNull(generatedSourcePath,"generatedSourcePath");
        this.repairSourceCompiler=Objects.requireNonNull(repairSourceCompiler,"repairSourceCompiler");
    }

    @Override public void prepare(String runId, TestEngineeringRequest request, AgentContextPack context) {
        sessions.remove(runId);
        inputs.put(runId, new CoordinatorWorkflowGateway.Input(request, context));
    }

    @Override public void restoreReviewedArtifacts(String runId,TestPlan plan,TestImplementationProposal implementation){
        CoordinatorWorkflowGateway.Input input=requireInput(runId);
        if(plan==null)throw new IllegalArgumentException("A reviewed plan is required for restore");
        AgentExecutor.AgentResult planResult=new AgentExecutor.AgentResult("restored",ArtifactEnvelope.create("plan-1",runId,List.of("context"),1,plan));
        AgentExecutor.AgentResult implementationResult=null;List<StudioWorkflowGateway.PolicyCheck> checks=List.of();
        if(implementation!=null){implementationResult=new AgentExecutor.AgentResult("restored",ArtifactEnvelope.create("implementation-1",runId,List.of("plan-1"),1,implementation));
            GeneratedTestPolicyValidator.ValidationResult validation=policyValidator.validate(input.request(),generatedSourcePath.apply(input.request()),implementation.sourcePatch());
            List<StudioWorkflowGateway.PolicyCheck> restoredChecks=new java.util.ArrayList<>();restoredChecks.add(new StudioWorkflowGateway.PolicyCheck("EXISTING_PAGE_OBJECTS_ONLY",implementation.selectorAccessPolicy()==TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,implementation.selectorAccessPolicy().name()));
            for(GeneratedTestPolicyValidator.Rule rule:GeneratedTestPolicyValidator.Rule.values())restoredChecks.add(new StudioWorkflowGateway.PolicyCheck(rule.name(),validation.violations().stream().noneMatch(value->value.rule()==rule),"RESTORED_AND_REVALIDATED"));checks=List.copyOf(restoredChecks);}
        sessions.put(runId,new Session(input,planResult,implementationResult,null,checks));
    }

    @Override
    public TestPlan generatePlan(String runId, String requirement) throws AgentExecutor.AgentExecutionException {
        CoordinatorWorkflowGateway.Input input = requireInput(runId);
        ArtifactEnvelope<AgentContextPack> context = context(runId, input.context());
        AgentExecutor.AgentResult result = agents.execute(new AgentExecutor.AgentCommand(runId,
                AgentExecutor.Role.TEST_ARCHITECT, List.of(context), Map.of("expectedResult", "TestPlan")));
        if (!(result.artifact().payload() instanceof TestPlan plan)) throw invalid("Architect", "TestPlan");
        sessions.put(runId, new Session(input, result, null, null));
        return plan;
    }

    @Override
    public TestImplementationProposal generateImplementation(String runId)
            throws AgentExecutor.AgentExecutionException {
        Session session = requireSession(runId);
        if (!(session.plan().artifact().payload() instanceof TestPlan plan)) throw invalid("Architect", "TestPlan");
        ArtifactEnvelope<AgentContextPack> context = context(runId, session.input().context());
        ArtifactEnvelope<TestPlan> planArtifact = ArtifactEnvelope.create("plan-1", runId,
                List.of(context.artifactId()), 1, plan);
        AgentExecutor.AgentResult result = agents.execute(new AgentExecutor.AgentCommand(runId,
                AgentExecutor.Role.TEST_IMPLEMENTER, List.of(context, planArtifact),
                Map.of("expectedResult", "TestImplementationProposal", "attempt", "1")));
        if (!(result.artifact().payload() instanceof TestImplementationProposal implementation)) {
            throw invalid("Implementer", "TestImplementationProposal");
        }
        GeneratedTestPolicyValidator.ValidationResult validation=policyValidator.validate(session.input().request(),
                generatedSourcePath.apply(session.input().request()),implementation.sourcePatch());
        List<StudioWorkflowGateway.PolicyCheck> checks=new java.util.ArrayList<>();
        checks.add(new StudioWorkflowGateway.PolicyCheck("EXISTING_PAGE_OBJECTS_ONLY",
                implementation.selectorAccessPolicy()==TestImplementationProposal.SelectorAccessPolicy.PAGE_OBJECTS_ONLY,
                implementation.selectorAccessPolicy().name()));
        for(GeneratedTestPolicyValidator.Rule rule:GeneratedTestPolicyValidator.Rule.values()){
            List<GeneratedTestPolicyValidator.Violation> violations=validation.violations().stream().filter(value->value.rule()==rule).toList();
            checks.add(new StudioWorkflowGateway.PolicyCheck(rule.name(),violations.isEmpty(),violations.isEmpty()?"PASS":violations.stream().map(value->"line "+value.line()+", column "+value.column()).distinct().sorted().toList().toString()));
        }
        sessions.put(runId, new Session(session.input(), session.plan(), result, null,List.copyOf(checks)));
        return implementation;
    }

    @Override
    public WorkflowReport run(String runId) throws AgentExecutor.AgentExecutionException {
        Session session = requireSession(runId);
        if (session.implementation() == null) {
            throw new IllegalStateException("Implementation must be reviewed before Run");
        }
        if(session.policy().isEmpty()||session.policy().stream().anyMatch(value->!value.passed()))throw new IllegalStateException("Implementation is blocked by deterministic policy");
        AgentExecutor replay = command -> switch (command.role()) {
            case TEST_ARCHITECT -> session.plan();
            case TEST_IMPLEMENTER -> session.implementation();
            default -> agents.execute(command);
        };
        AgentWorkflowCoordinator.Result result = coordinators.apply(replay, session.input()).run(runId,
                session.input().request(), session.input().context());
        sessions.put(runId, new Session(session.input(), session.plan(), session.implementation(), result,session.policy()));
        return result.toWorkflowReport(StrictJson.write(session.input().context()).length,
                session.input().context().header().limitations());
    }

    @Override public TestExecutionResult execution(String runId) { return artifact(runId, TestExecutionResult.class); }
    @Override public FailureClassification diagnosis(String runId) { return artifact(runId, FailureClassification.class); }
    @Override public RepairProposal repair(String runId) { return artifact(runId, RepairProposal.class); }
    @Override public List<StudioWorkflowGateway.PolicyCheck> implementationPolicy(String runId){return requireSession(runId).policy();}
    @Override public WorkflowReport rerun(String runId,RepairProposal appliedRepair)throws AgentExecutor.AgentExecutionException{
        if(appliedRepair==null)throw new IllegalStateException("Applied repair is required for repair verification");
        repairSourceCompiler.compile(appliedRepair);
        return run(runId);
    }

    private <T> T artifact(String runId, Class<T> type) {
        Session session = sessions.get(runId);
        if (session == null || session.result() == null) return null;
        return session.result().run().artifacts().stream().map(ArtifactEnvelope::payload)
                .filter(type::isInstance).map(type::cast).reduce((first, last) -> last).orElse(null);
    }

    private CoordinatorWorkflowGateway.Input requireInput(String runId) {
        return Objects.requireNonNull(inputs.get(runId), "No workflow input for run " + runId);
    }
    private Session requireSession(String runId) {
        Session session = sessions.get(runId);
        if (session == null) throw new IllegalStateException("Plan must be generated before this action");
        return session;
    }
    private static ArtifactEnvelope<AgentContextPack> context(String runId, AgentContextPack value) {
        return ArtifactEnvelope.create("context", runId, List.of(), 1, value);
    }
    private static AgentExecutor.AgentExecutionException invalid(String role, String expected) {
        return new AgentExecutor.AgentExecutionException(AgentExecutor.AgentFailureCode.AGENT_OUTPUT_INVALID,
                role + " did not return " + expected);
    }
    private static Path defaultGeneratedSourcePath(TestEngineeringRequest request){String binary=request.target().testClass().replace('.','/');String root=request.allowedPaths().isEmpty()?"src/test/java":request.allowedPaths().get(0);return Path.of(root).resolve(binary+".java");}
    @FunctionalInterface public interface RepairSourceCompiler{void compile(RepairProposal proposal)throws AgentExecutor.AgentExecutionException;}
    private record Session(CoordinatorWorkflowGateway.Input input, AgentExecutor.AgentResult plan,
                           AgentExecutor.AgentResult implementation, AgentWorkflowCoordinator.Result result,
                           List<StudioWorkflowGateway.PolicyCheck> policy) {
        private Session(CoordinatorWorkflowGateway.Input input,AgentExecutor.AgentResult plan,AgentExecutor.AgentResult implementation,AgentWorkflowCoordinator.Result result){this(input,plan,implementation,result,List.of());}
    }
    @Override public boolean supportsReviewedArtifactRestore(){return true;}
}
