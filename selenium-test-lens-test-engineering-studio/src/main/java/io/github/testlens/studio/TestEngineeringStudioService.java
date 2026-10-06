package io.github.testlens.studio;

import io.github.testlens.application.mapper.ApplicationMapper;
import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier;
import io.github.testlens.application.tooling.ai.workflow.WorkflowReport;
import io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest;
import io.github.testlens.application.tooling.source.CorrelationOverrides;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.PageObjectCorrelator;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.application.tooling.json.ApplicationModelJson;
import io.github.testlens.application.tooling.json.StrictJson;
import io.github.testlens.studio.projection.*;
import io.github.testlens.studio.workspace.StudioWorkspaceStore;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.github.testlens.studio.projection.StudioProjections.*;

/** Thin application facade over S11/S12. It owns orchestration and presentation state, not domain decisions. */
public final class TestEngineeringStudioService {
    public record Configuration(Path projectRoot, List<Path> sourceRoots, List<Path> classpathEntries,
                                List<String> allowedRepairPrefixes, String applicationName) {
        public Configuration {
            Objects.requireNonNull(projectRoot); sourceRoots=List.copyOf(sourceRoots==null?List.of():sourceRoots);
            classpathEntries=List.copyOf(classpathEntries==null?List.of():classpathEntries);
            allowedRepairPrefixes=List.copyOf(allowedRepairPrefixes==null?List.of("src/test/java","src/main/java"):allowedRepairPrefixes);
            if(applicationName==null||applicationName.isBlank())applicationName=projectRoot.getFileName().toString();
        }
    }
    public record ActionResult(String actionId, String status, String detail, Object projection) { }
    private record RepairState(String runId,RepairProposal proposal,String projectFingerprint,String decision,String applyStatus) { }

    private final Configuration configuration;
    private final ExistingProjectIndexer indexer;
    private final PageObjectCorrelator correlator;
    private final TrustedRepairApplier repairApplier;
    private final StudioWorkflowGateway workflows;
    private final StudioWorkspaceStore store;
    private final StudioProjectionFactory projections=new StudioProjectionFactory();
    private final RequirementContextSelector contextSelector=new RequirementContextSelector();
    private final Supplier<WebDriver> driverProvider;
    private final RedactionPolicy redaction = RedactionPolicy.defaults();
    private final AtomicBoolean operationRunning=new AtomicBoolean();
    private final Map<String,String> requirements=new ConcurrentHashMap<>();
    private final Map<String,WorkflowReport> reports=new ConcurrentHashMap<>();
    private final Map<String,TestPlanProjection> plans=new ConcurrentHashMap<>();
    private final Map<String,ImplementationProjection> implementations=new ConcurrentHashMap<>();
    private final Map<String,ExecutionProjection> executions=new ConcurrentHashMap<>();
    private final Map<String,DiagnosisProjection> diagnoses=new ConcurrentHashMap<>();
    private final Map<String,RepairState> repairs=new ConcurrentHashMap<>();
    private volatile ExistingProjectIndex sourceIndex;
    private volatile ApplicationModel applicationModel;
    private volatile PageObjectCorrelation correlation;
    private volatile ApplicationMapper guidedMapper;
    private volatile String hydrationFailure;

    public TestEngineeringStudioService(Configuration configuration, StudioWorkflowGateway workflows,
                                        Supplier<WebDriver> driverProvider) {
        this(configuration,workflows,driverProvider,new ExistingProjectIndexer(),new PageObjectCorrelator(),new TrustedRepairApplier(),new StudioWorkspaceStore(configuration.projectRoot()));
    }
    TestEngineeringStudioService(Configuration configuration,StudioWorkflowGateway workflows,Supplier<WebDriver> driverProvider,
                                 ExistingProjectIndexer indexer,PageObjectCorrelator correlator,TrustedRepairApplier repairApplier,StudioWorkspaceStore store){
        this.configuration=Objects.requireNonNull(configuration);this.workflows=workflows;this.driverProvider=driverProvider;
        this.indexer=Objects.requireNonNull(indexer);this.correlator=Objects.requireNonNull(correlator);this.repairApplier=Objects.requireNonNull(repairApplier);this.store=Objects.requireNonNull(store);
        hydratePersistedProject();
    }

    public ProjectStatusProjection scanProject() throws IOException {
        return exclusive(()->{sourceIndex=indexer.index(new ExistingProjectIndexer.Request(configuration.projectRoot(),configuration.sourceRoots(),configuration.classpathEntries()));
            correlation=null;persistProjectArtifacts();return projectOverview();});
    }
    public ApplicationOverviewProjection mapApplication(ApplicationMapperOptions.Mode mode) throws IOException {
        return exclusive(()->{if(driverProvider==null)throw new IllegalStateException("No caller-owned WebDriver provider configured");WebDriver driver=Objects.requireNonNull(driverProvider.get(),"Driver provider returned null");
            ApplicationMapper mapper;
            if(mode==ApplicationMapperOptions.Mode.GUIDED&&guidedMapper!=null)mapper=guidedMapper;
            else { mapper=ApplicationMapper.start(driver,ApplicationMapperOptions.builder(configuration.applicationName()).mode(Objects.requireNonNull(mode)).build());if(mode==ApplicationMapperOptions.Mode.GUIDED)guidedMapper=mapper; }
            mapper.observe(); if(mode==ApplicationMapperOptions.Mode.SAFE_EXPLORE)mapper.safeExplore(); applicationModel=mapper.model(); correlation=null;persistProjectArtifacts();return projections.application(applicationModel);});
    }
    public PageObjectCorrelationProjection correlate() throws IOException {
        return exclusive(()->{if(sourceIndex==null||applicationModel==null)throw new IllegalStateException("Scan and map are required before correlation");
            correlation=correlator.correlate(applicationModel,sourceIndex,CorrelationOverrides.none());persistProjectArtifacts();return projections.correlations(correlation,0,100);});
    }
    public ProjectStatusProjection refreshProject() throws IOException {
        return exclusive(()->{String previous=sourceIndex==null?null:sourceIndex.projectFingerprint();ExistingProjectIndex refreshed=indexer.index(new ExistingProjectIndexer.Request(configuration.projectRoot(),configuration.sourceRoots(),configuration.classpathEntries()));
            sourceIndex=refreshed;if(!Objects.equals(previous,refreshed.projectFingerprint()))correlation=null;persistProjectArtifacts();return projectOverview();});
    }
    public String createRequirement(String requirement) throws IOException {
        if(requirement==null||requirement.isBlank()||requirement.length()>16_384)throw new IllegalArgumentException("Requirement must contain 1..16384 characters");
        String safeRequirement=redaction.redact(requirement.trim());
        if(safeRequirement==null||safeRequirement.isBlank())throw new IllegalArgumentException("Requirement was rejected by redaction policy");
        String runId="run-"+UUID.randomUUID(); requirements.put(runId,safeRequirement); persistHistories(); return runId;
    }
    public TestPlanProjection generatePlan(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{String text=requirement(runId);prepareWorkflow(runId,text);TestPlanProjection result=projections.plan(requireWorkflow().generatePlan(runId,text));plans.put(runId,result);persistHistories();return result;});}
    public ImplementationProjection generateImplementation(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{TestImplementationProposal proposal=requireWorkflow().generateImplementation(runId);ImplementationProjection result=projections.implementation(proposal,requireWorkflow().implementationPolicy(runId));implementations.put(runId,result);persistHistories();return result;});}
    public WorkflowRunProjection runWorkflow(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{requirement(runId);ImplementationProjection implementation=implementations.get(runId);if(implementation==null||implementation.stage().status()==StageStatus.BLOCKED)throw new IllegalStateException("Implementation must pass deterministic policy before Run");WorkflowReport report=requireWorkflow().run(runId);reports.put(runId,report);refreshWorkflowArtifacts(runId);registerGatewayRepair(runId);persistHistories();return projections.workflow(report);});}
    public WorkflowRunProjection rerun(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{RepairState repair=repairs.values().stream().filter(value->value.runId().equals(runId)&&"APPROVED".equals(value.decision())&&TrustedRepairApplier.Status.APPLIED.name().equals(value.applyStatus())).findFirst().orElseThrow(()->new IllegalStateException("An applied repair for this workflow is required before verification"));WorkflowReport report=requireWorkflow().rerun(runId,repair.proposal());reports.put(runId,report);refreshWorkflowArtifacts(runId);registerGatewayRepair(runId);persistHistories();return projections.workflow(report);});}
    public ExecutionProjection execution(String runId){return executions.get(runId);}
    public DiagnosisProjection diagnosis(String runId){return diagnoses.get(runId);}
    public DiagnosisProjection diagnoseWorkflow(String runId)throws IOException{return exclusive(()->{FailureClassification value=requireWorkflow().diagnosis(runId);if(value==null)throw new IllegalStateException("Workflow has no diagnosis");DiagnosisProjection result=projections.diagnosis(value);diagnoses.put(runId,result);persistHistories();return result;});}
    public RepairProjection prepareRepair(String runId)throws IOException{RepairProposal proposal=requireWorkflow().repair(runId);if(proposal==null)throw new IllegalStateException("Workflow has no repair proposal");return registerRepairProposal(runId,proposal);}

    public RepairProjection registerRepairProposal(String runId,RepairProposal proposal)throws IOException{
        requirement(runId);Objects.requireNonNull(proposal);if(sourceIndex==null)throw new IllegalStateException("Source index is required");String key=repairKey(runId,proposal.proposalId());
        repairs.compute(key,(ignored,old)->old==null?new RepairState(runId,proposal,sourceIndex.projectFingerprint(),null,null):old);
        persistHistories();return repairProjection(key);
    }
    public RepairProjection registerRepairProposal(RepairProposal proposal)throws IOException{return registerRepairProposal(onlyRunId(),proposal);}
    public RepairProjection rejectRepair(String runId,String proposalId)throws IOException{
        String key=repairKey(runId,proposalId);repairs.compute(key,(ignored,state)->{if(state==null)throw new IllegalArgumentException("Unknown repair proposal for workflow");if(state.decision()!=null)throw new IllegalStateException("Repair decision already recorded");return new RepairState(state.runId(),state.proposal(),state.projectFingerprint(),"REJECTED",null);});
        persistHistories();return repairProjection(key);
    }
    public RepairProjection rejectRepair(String proposalId)throws IOException{return rejectRepair(onlyRunId(),proposalId);}
    public RepairProjection approveRepair(String runId,String proposalId)throws IOException{
        return exclusive(()->{String key=repairKey(runId,proposalId);RepairState state=requireRepair(key);if(state.decision()!=null)throw new IllegalStateException("Repair decision already recorded");
            if(sourceIndex==null||!Objects.equals(sourceIndex.projectFingerprint(),state.projectFingerprint())){repairs.put(key,new RepairState(state.runId(),state.proposal(),state.projectFingerprint(),"APPROVE_FAILED",TrustedRepairApplier.Status.SOURCE_PRECONDITION_FAILED.name()));persistHistories();return repairProjection(key);}
            TrustedRepairApplier.ApplyResult applied=repairApplier.apply(configuration.projectRoot(),new TrustedRepairApplier.ApplyRequest(state.proposal(),sourceIndex,configuration.allowedRepairPrefixes(),true));
            repairs.put(key,new RepairState(state.runId(),state.proposal(),state.projectFingerprint(),applied.status()==TrustedRepairApplier.Status.APPLIED?"APPROVED":"APPROVE_FAILED",applied.status().name()));persistHistories();return repairProjection(key);});
    }
    public RepairProjection approveRepair(String proposalId)throws IOException{return approveRepair(onlyRunId(),proposalId);}

    public ProjectStatusProjection projectOverview(){ProjectStatusProjection value=projections.project(sourceIndex,applicationModel,correlation,freshness());if(hydrationFailure==null)return value;StageProjection stage=value.stage();List<StageProjection.Item> limitations=new ArrayList<>(stage.limitations());limitations.add(new StageProjection.Item("WORKSPACE_HYDRATION_FAILED",hydrationFailure));return new ProjectStatusProjection(new StageProjection(StageStatus.STALE,stage.evidence(),limitations,stage.artifacts(),stage.actions()),value.source(),value.application(),value.correlation(),value.problemCount(),value.freshness());}
    public ApplicationOverviewProjection applicationOverview(){return projections.application(applicationModel);}
    public PageObjectCorrelationProjection correlations(int offset,int limit){return projections.correlations(correlation,offset,limit);}
    public ProblemsProjection problems(int offset,int limit){return projections.problems(sourceIndex,applicationModel,correlation,offset,limit);}
    public List<WorkflowRunProjection> workflowHistory(){return reports.values().stream().sorted(Comparator.comparing(WorkflowReport::runId)).limit(500).map(projections::workflow).toList();}
    public List<RepairProjection> repairHistory(){return repairs.keySet().stream().sorted().limit(500).map(this::repairProjection).toList();}
    public WorkflowDetailProjection workflow(String runId){
        String selected=runId;if(selected==null||selected.isBlank())selected=requirements.keySet().stream().sorted().reduce((a,b)->b).orElse(null);
        if(selected==null)return new WorkflowDetailProjection(null,null,null,null,null,null,null,List.of(),null,"NOT_STARTED");
        String selectedRun=selected;WorkflowReport report=reports.get(selectedRun);WorkflowRunProjection run=report==null?null:projections.workflow(report);
        RepairProjection repair=repairs.entrySet().stream().filter(value->value.getValue().runId().equals(selectedRun)).sorted(Map.Entry.comparingByKey()).map(value->repairProjection(value.getKey())).reduce((a,b)->b).orElse(null);
        return new WorkflowDetailProjection(selectedRun,requirements.get(selectedRun),plans.get(selectedRun),implementations.get(selectedRun),
                execution(selectedRun),diagnosis(selectedRun),repair,
                run==null?List.of():run.timeline(),run==null?null:run.metrics(),run==null?"NOT_STARTED":run.finalState());
    }
    public StudioSnapshot snapshot(){return new StudioSnapshot(projectOverview(),applicationOverview(),correlations(0,100),problems(0,100),workflowHistory(),repairHistory());}
    public boolean operationRunning(){return operationRunning.get();}
    public StudioWorkspaceStore workspace(){return store;}

    private void registerGatewayRepair(String runId)throws IOException{RepairProposal proposal=workflows.repair(runId);if(proposal!=null)registerRepairProposal(runId,proposal);}
    private void refreshWorkflowArtifacts(String runId){TestExecutionResult execution=requireWorkflow().execution(runId);if(execution==null)executions.remove(runId);else executions.put(runId,projections.execution(execution));FailureClassification diagnosis=requireWorkflow().diagnosis(runId);if(diagnosis==null)diagnoses.remove(runId);else diagnoses.put(runId,projections.diagnosis(diagnosis));}
    private RepairProjection repairProjection(String key){RepairState state=requireRepair(key);boolean stale=sourceIndex==null||!Objects.equals(sourceIndex.projectFingerprint(),state.projectFingerprint());return projections.repair(state.proposal(),state.decision(),state.applyStatus(),stale);}
    private RepairState requireRepair(String id){RepairState value=repairs.get(id);if(value==null)throw new IllegalArgumentException("Unknown repair proposal");return value;}
    private StudioWorkflowGateway requireWorkflow(){return Objects.requireNonNull(workflows,"No workflow gateway configured");}
    private void prepareWorkflow(String runId,String requirement){
        if(sourceIndex==null||applicationModel==null||correlation==null)throw new IllegalStateException("Scan, map and correlation are required before generating a plan");
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,
                List.of("Studio requirement","Deterministic project artifacts"),List.of(),ContractHeader.Confidence.OBSERVED);
        RequirementContextSelector.Selection selected=contextSelector.select(requirement,applicationModel,sourceIndex,correlation);
        List<String>pageIds=selected.pageIds();
        AgentTask task=new AgentTask(header,AgentTask.TaskType.CREATE_TEST,requirement,pageIds,selected.stateIds(),selected.elementIds(),
                List.of("Use existing Page Objects only","Do not invent selectors"));
        Map<String,List<String>>apis=new TreeMap<>();
        Map<String,String>classPages=new HashMap<>();correlation.classes().stream().filter(value->value.state()==PageObjectCorrelation.State.EXACT||value.state()==PageObjectCorrelation.State.STRONG).forEach(value->classPages.put(value.classId(),value.pageId()));
        sourceIndex.methods().stream().filter(value->classPages.containsKey(value.ownerClassId())).forEach(value->apis.computeIfAbsent(classPages.get(value.ownerClassId()),ignored->new ArrayList<>()).add(value.signature()));
        AgentContextPack context=new ContextSlicer().slice(applicationModel,task,apis,List.of("Existing project conventions"),redaction,
                ContextSlicer.Limits.defaults(),sourceIndex,correlation,ContextSlicer.SourceLimits.defaults());
        String className="GeneratedStudioTest"+runId.replaceAll("[^A-Za-z0-9]","");
        TestEngineeringRequest request=new TestEngineeringRequest(new TestEngineeringRequest.Requirement(requirement,List.of()),
                new TestEngineeringRequest.Scope(pageIds,List.of()),new TestEngineeringRequest.Framework("JUnit","5","junit-jupiter"),
                new TestEngineeringRequest.Target(configuration.applicationName(),className,"studio-requirement"),
                configuration.allowedRepairPrefixes(),TestEngineeringRequest.ExecutionPolicy.TARGETED_AFTER_COMPILE);
        requireWorkflow().prepare(runId,request,context);
    }
    private String requirement(String runId){String value=requirements.get(runId);if(value==null)throw new IllegalArgumentException("Unknown workflow run");return value;}
    private String onlyRunId(){if(requirements.size()!=1)throw new IllegalStateException("runId is required when the workspace does not contain exactly one workflow");return requirements.keySet().iterator().next();}
    private static String repairKey(String runId,String proposalId){if(runId==null||runId.isBlank()||proposalId==null||proposalId.isBlank())throw new IllegalArgumentException("runId and proposalId are required");return runId+'\u0000'+proposalId;}
    private Map<String,String> freshness(){Map<String,String> value=new TreeMap<>();String fp=sourceIndex==null?null:sourceIndex.projectFingerprint();for(var kind:StudioWorkspaceStore.ArtifactKind.values())try{value.put(label(kind),store.freshness(kind,fp).name());}catch(IOException ignored){value.put(label(kind),"STALE");}return value;}
    private static String label(StudioWorkspaceStore.ArtifactKind kind){return switch(kind){case APPLICATION_MODEL->"ApplicationModel";case PAGE_OBJECT_INDEX->"PageObjectIndex";case USAGE_GRAPH->"UsageGraph";case CORRELATIONS->"Correlations";default->kind.name();};}
    private void persistProjectArtifacts()throws IOException{String fp=sourceIndex==null?null:sourceIndex.projectFingerprint();if(sourceIndex!=null){store.write(StudioWorkspaceStore.ArtifactKind.PAGE_OBJECT_INDEX,Map.of("metrics",sourceIndex.metrics(),"completeness",sourceIndex.completeness(),"limitations",sourceIndex.limitations()),fp);store.write(StudioWorkspaceStore.ArtifactKind.USAGE_GRAPH,Map.of("edges",sourceIndex.edges().stream().limit(500).toList(),"total",sourceIndex.edges().size()),fp);}if(applicationModel!=null){ApplicationModelJson codec=new ApplicationModelJson();store.write(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL,StrictJson.readObject(codec.write(applicationModel)),fp);}if(correlation!=null)store.write(StudioWorkspaceStore.ArtifactKind.CORRELATIONS,projections.correlations(correlation,0,500),fp);store.write(StudioWorkspaceStore.ArtifactKind.PROBLEMS,problems(0,500),fp);store.write(StudioWorkspaceStore.ArtifactKind.PROJECT_STATUS,projectOverview(),fp);}
    private void hydratePersistedProject(){
        try{
            Optional<Map<String,Object>> document=store.read(StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL);if(document.isEmpty())return;
            Map<String,Object> envelope=document.orElseThrow();if(!Objects.equals(envelope.get("kind"),StudioWorkspaceStore.ArtifactKind.APPLICATION_MODEL.name())||!(envelope.get("projection") instanceof Map<?,?>))throw new IllegalArgumentException("Invalid persisted ApplicationModel envelope");
            ApplicationModel restored=new ApplicationModelJson().read(StrictJson.write(envelope.get("projection")));
            ExistingProjectIndex reindexed=indexer.index(new ExistingProjectIndexer.Request(configuration.projectRoot(),configuration.sourceRoots(),configuration.classpathEntries()));
            PageObjectCorrelation recomputed=correlator.correlate(restored,reindexed,CorrelationOverrides.none());
            applicationModel=restored;sourceIndex=reindexed;correlation=recomputed;hydrationFailure=null;
        }catch(IOException|RuntimeException failure){applicationModel=null;sourceIndex=null;correlation=null;hydrationFailure=failure.getClass().getSimpleName();}
    }
    private void persistHistories()throws IOException{String fp=sourceIndex==null?null:sourceIndex.projectFingerprint();store.write(StudioWorkspaceStore.ArtifactKind.WORKFLOW_HISTORY,Map.of("requirements",new TreeMap<>(requirements),"plans",new TreeMap<>(plans),"implementations",new TreeMap<>(implementations),"executions",new TreeMap<>(executions),"diagnoses",new TreeMap<>(diagnoses),"runs",workflowHistory()),fp);store.write(StudioWorkspaceStore.ArtifactKind.REPAIR_HISTORY,repairHistory(),fp);}
    private <T> T exclusive(IoOperation<T> operation)throws IOException{if(!operationRunning.compareAndSet(false,true))throw new IllegalStateException("A Studio operation is already running");try{return operation.run();}finally{operationRunning.set(false);}}
    private <T> T exclusiveAgent(AgentOperation<T> operation)throws AgentExecutor.AgentExecutionException,IOException{if(!operationRunning.compareAndSet(false,true))throw new IllegalStateException("A Studio operation is already running");try{return operation.run();}finally{operationRunning.set(false);}}
    @FunctionalInterface private interface IoOperation<T>{T run()throws IOException;}
    @FunctionalInterface private interface AgentOperation<T>{T run()throws AgentExecutor.AgentExecutionException,IOException;}
}
