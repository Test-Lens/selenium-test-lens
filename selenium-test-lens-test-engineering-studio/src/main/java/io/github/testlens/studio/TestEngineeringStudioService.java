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
import io.github.testlens.studio.workspace.WorkflowSessionSnapshot;
import io.github.testlens.studio.project.ProjectDescriptor;
import org.openqa.selenium.WebDriver;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
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
    private final Map<String,WorkflowSessionSnapshot> sessions=new ConcurrentHashMap<>();
    private volatile ExistingProjectIndex sourceIndex;
    private volatile ApplicationModel applicationModel;
    private volatile PageObjectCorrelation correlation;
    private volatile ApplicationMapper guidedMapper;
    private volatile String hydrationFailure;
    private volatile ProjectDescriptor projectDescriptor;

    public TestEngineeringStudioService(Configuration configuration, StudioWorkflowGateway workflows,
                                        Supplier<WebDriver> driverProvider) {
        this(configuration,workflows,driverProvider,new ExistingProjectIndexer(),new PageObjectCorrelator(),new TrustedRepairApplier(),new StudioWorkspaceStore(configuration.projectRoot()));
    }
    public TestEngineeringStudioService(Configuration configuration, StudioWorkflowGateway workflows,
                                        Supplier<WebDriver> driverProvider, Path workspaceDirectory) {
        this(configuration,workflows,driverProvider,new ExistingProjectIndexer(),new PageObjectCorrelator(),
                new TrustedRepairApplier(),StudioWorkspaceStore.atWorkspace(workspaceDirectory));
    }
    TestEngineeringStudioService(Configuration configuration,StudioWorkflowGateway workflows,Supplier<WebDriver> driverProvider,
                                 ExistingProjectIndexer indexer,PageObjectCorrelator correlator,TrustedRepairApplier repairApplier,StudioWorkspaceStore store){
        this.configuration=Objects.requireNonNull(configuration);this.workflows=workflows;this.driverProvider=driverProvider;
        this.indexer=Objects.requireNonNull(indexer);this.correlator=Objects.requireNonNull(correlator);this.repairApplier=Objects.requireNonNull(repairApplier);this.store=Objects.requireNonNull(store);
        hydratePersistedProject();
        hydrateWorkflowSessions();
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
        String runId="run-"+UUID.randomUUID(); requirements.put(runId,safeRequirement);Instant now=Instant.now();sessions.put(runId,new WorkflowSessionSnapshot(1,runId,safeRequirement,WorkflowSessionSnapshot.State.CREATED,now,now,currentFingerprint(),currentFingerprint(),WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,false,List.of()));persistSession(runId);persistHistories(); return runId;
    }
    public TestPlanProjection generatePlan(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{String text=requirement(runId);WorkflowSessionSnapshot current=requireSession(runId);requireAction(runId,current.freshness()==WorkflowSessionSnapshot.Freshness.FRESH?"GENERATE_PLAN":"REGENERATE_PLAN");refreshWorkflowInputsIfStale(runId);prepareWorkflow(runId,text);beginPlanGeneration(runId);TestPlan plan;try{plan=requireWorkflow().generatePlan(runId,text);}catch(AgentExecutor.AgentExecutionException failure){updateSession(runId,WorkflowSessionSnapshot.State.AGENT_EXECUTION_INTERRUPTED,null,null,null,null,null,null,null);throw failure;}TestPlanProjection result=projections.plan(plan);plans.put(runId,result);updateSession(runId,WorkflowSessionSnapshot.State.PLAN_READY_FOR_REVIEW,plan,null,null,null,null,null,null);persistHistories();return result;});}
    public ImplementationProjection generateImplementation(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{requireAction(runId,"GENERATE_IMPLEMENTATION");assertFresh(runId);beginImplementationGeneration(runId);TestImplementationProposal proposal;try{proposal=requireWorkflow().generateImplementation(runId);}catch(AgentExecutor.AgentExecutionException failure){updateSession(runId,WorkflowSessionSnapshot.State.AGENT_EXECUTION_INTERRUPTED,null,null,null,null,null,null,null);throw failure;}ImplementationProjection result=projections.implementation(proposal,requireWorkflow().implementationPolicy(runId));implementations.put(runId,result);updateSession(runId,WorkflowSessionSnapshot.State.IMPLEMENTATION_READY_FOR_REVIEW,null,proposal,null,null,null,null,null);persistHistories();return result;});}
    public WorkflowRunProjection runWorkflow(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{requirement(runId);requireAction(runId,"RUN");assertFresh(runId);ImplementationProjection implementation=implementations.get(runId);if(implementation==null||implementation.stage().status()==StageStatus.BLOCKED)throw new IllegalStateException("Implementation must pass deterministic policy before Run");updateSession(runId,WorkflowSessionSnapshot.State.EXECUTION_RUNNING,null,null,null,null,null,null,null);WorkflowReport report;try{report=requireWorkflow().run(runId);}catch(AgentExecutor.AgentExecutionException failure){updateSession(runId,WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,null,null,null,null,null,null,null);throw failure;}reports.put(runId,report);refreshWorkflowArtifacts(runId);registerGatewayRepair(runId);RepairProposal repair=requireWorkflow().repair(runId);WorkflowSessionSnapshot.State state="SUCCESS".equals(report.finalState().name())?WorkflowSessionSnapshot.State.SUCCESS:(repair==null?WorkflowSessionSnapshot.State.EXECUTION_FAILED:WorkflowSessionSnapshot.State.REPAIR_READY_FOR_REVIEW);updateSession(runId,state,null,null,requireWorkflow().execution(runId),requireWorkflow().diagnosis(runId),repair,null,null);persistHistories();return projections.workflow(report);});}
    public WorkflowRunProjection rerun(String runId)throws AgentExecutor.AgentExecutionException,IOException{return exclusiveAgent(()->{requireAction(runId,"RERUN");assertFresh(runId);WorkflowSessionSnapshot session=requireSession(runId);RepairProposal selected=Objects.requireNonNull(session.repair(),"Applied repair is missing from workflow snapshot");RepairState repair=requireRepair(repairKey(runId,selected.proposalId()));if(!"APPROVED".equals(repair.decision())||!TrustedRepairApplier.Status.APPLIED.name().equals(repair.applyStatus()))throw new IllegalStateException("An applied repair for this workflow is required before verification");updateSession(runId,WorkflowSessionSnapshot.State.VERIFICATION_RUNNING,null,null,null,null,repair.proposal(),repair.decision(),repair.applyStatus());WorkflowReport report;try{report=requireWorkflow().rerun(runId,repair.proposal());}catch(AgentExecutor.AgentExecutionException failure){updateSession(runId,WorkflowSessionSnapshot.State.EXECUTION_INTERRUPTED,null,null,null,null,repair.proposal(),repair.decision(),repair.applyStatus());throw failure;}reports.put(runId,report);refreshWorkflowArtifacts(runId);RepairProposal nextRepair=requireWorkflow().repair(runId);boolean success="SUCCESS".equals(report.finalState().name());boolean hasNewRepair=!success&&nextRepair!=null&&!nextRepair.proposalId().equals(repair.proposal().proposalId());if(hasNewRepair)registerRepairProposal(runId,nextRepair);RepairProposal persistedRepair=hasNewRepair?nextRepair:repair.proposal();String decision=hasNewRepair?null:repair.decision();String applyStatus=hasNewRepair?null:repair.applyStatus();WorkflowSessionSnapshot.State state=success?WorkflowSessionSnapshot.State.SUCCESS:(hasNewRepair?WorkflowSessionSnapshot.State.REPAIR_READY_FOR_REVIEW:WorkflowSessionSnapshot.State.FAILED);updateSession(runId,state,null,null,requireWorkflow().execution(runId),requireWorkflow().diagnosis(runId),persistedRepair,decision,applyStatus);persistHistories();return projections.workflow(report);});}
    public ExecutionProjection execution(String runId){return executions.get(runId);}
    public DiagnosisProjection diagnosis(String runId){return diagnoses.get(runId);}
    public DiagnosisProjection diagnoseWorkflow(String runId)throws IOException{return exclusive(()->{requireAction(runId,"DIAGNOSE");FailureClassification value=requireWorkflow().diagnosis(runId);if(value==null)throw new IllegalStateException("Workflow has no diagnosis");DiagnosisProjection result=projections.diagnosis(value);diagnoses.put(runId,result);RepairProposal repair=requireWorkflow().repair(runId);WorkflowSessionSnapshot.State state=repair==null?WorkflowSessionSnapshot.State.EXECUTION_FAILED:WorkflowSessionSnapshot.State.REPAIR_READY_FOR_REVIEW;updateSession(runId,state,null,null,null,value,repair,null,null);persistHistories();return result;});}
    public RepairProjection prepareRepair(String runId)throws IOException{return exclusive(()->{WorkflowSessionSnapshot session=requireSession(runId);if(session.freshness()!=WorkflowSessionSnapshot.Freshness.FRESH||session.state()!=WorkflowSessionSnapshot.State.REPAIR_READY_FOR_REVIEW)throw new IllegalStateException("Repair preparation is not available for workflow "+runId);RepairProposal proposal=requireWorkflow().repair(runId);if(proposal==null)throw new IllegalStateException("Workflow has no repair proposal");return registerRepairProposal(runId,proposal);});}

    public RepairProjection registerRepairProposal(String runId,RepairProposal proposal)throws IOException{
        requirement(runId);Objects.requireNonNull(proposal);if(sourceIndex==null)throw new IllegalStateException("Source index is required");String key=repairKey(runId,proposal.proposalId());
        repairs.compute(key,(ignored,old)->old==null?new RepairState(runId,proposal,sourceIndex.projectFingerprint(),null,null):old);
        WorkflowSessionSnapshot session=requireSession(runId);sessions.put(runId,session.withNewRepair(proposal,Instant.now()));persistSession(runId);persistHistories();return repairProjection(key);
    }
    public RepairProjection registerRepairProposal(RepairProposal proposal)throws IOException{return registerRepairProposal(onlyRunId(),proposal);}
    public RepairProjection rejectRepair(String runId,String proposalId)throws IOException{
        return exclusive(()->{requireAction(runId,"REJECT_REPAIR");String key=repairKey(runId,proposalId);repairs.compute(key,(ignored,state)->{if(state==null)throw new IllegalArgumentException("Unknown repair proposal for workflow");if(state.decision()!=null)throw new IllegalStateException("Repair decision already recorded");return new RepairState(state.runId(),state.proposal(),state.projectFingerprint(),"REJECTED",null);});
            updateSession(runId,WorkflowSessionSnapshot.State.REPAIR_REJECTED,null,null,null,null,repairs.get(key).proposal(),"REJECTED",null);persistHistories();return repairProjection(key);});
    }
    public RepairProjection rejectRepair(String proposalId)throws IOException{return rejectRepair(onlyRunId(),proposalId);}
    public RepairProjection approveRepair(String runId,String proposalId)throws IOException{
        return exclusive(()->{requireAction(runId,"APPROVE_REPAIR");String key=repairKey(runId,proposalId);RepairState state=requireRepair(key);if(state.decision()!=null)throw new IllegalStateException("Repair decision already recorded");
            if(sourceIndex==null||!Objects.equals(sourceIndex.projectFingerprint(),state.projectFingerprint())){repairs.put(key,new RepairState(state.runId(),state.proposal(),state.projectFingerprint(),"APPROVE_FAILED",TrustedRepairApplier.Status.SOURCE_PRECONDITION_FAILED.name()));markStale(runId,"SOURCE_CHANGED");persistHistories();return repairProjection(key);}
            TrustedRepairApplier.ApplyResult applied=repairApplier.apply(configuration.projectRoot(),new TrustedRepairApplier.ApplyRequest(state.proposal(),sourceIndex,configuration.allowedRepairPrefixes(),true));
            if(applied.status()==TrustedRepairApplier.Status.APPLIED){sourceIndex=indexer.index(new ExistingProjectIndexer.Request(configuration.projectRoot(),configuration.sourceRoots(),configuration.classpathEntries()));}
            String repairFingerprint=applied.status()==TrustedRepairApplier.Status.APPLIED?sourceIndex.projectFingerprint():state.projectFingerprint();
            repairs.put(key,new RepairState(state.runId(),state.proposal(),repairFingerprint,applied.status()==TrustedRepairApplier.Status.APPLIED?"APPROVED":"APPROVE_FAILED",applied.status().name()));WorkflowSessionSnapshot.State next=applied.status()==TrustedRepairApplier.Status.APPLIED?WorkflowSessionSnapshot.State.REPAIR_APPLIED_VERIFICATION_PENDING:WorkflowSessionSnapshot.State.REPAIR_READY_FOR_REVIEW;updateSession(runId,next,null,null,null,null,state.proposal(),repairs.get(key).decision(),applied.status().name());if(applied.status()!=TrustedRepairApplier.Status.APPLIED)markStale(runId,"REPAIR_APPLY_FAILED");persistHistories();return repairProjection(key);});
    }
    public RepairProjection approveRepair(String proposalId)throws IOException{return approveRepair(onlyRunId(),proposalId);}

    public ProjectStatusProjection projectOverview(){ProjectStatusProjection value=projections.project(sourceIndex,applicationModel,correlation,freshness());if(hydrationFailure==null)return value;StageProjection stage=value.stage();List<StageProjection.Item> limitations=new ArrayList<>(stage.limitations());limitations.add(new StageProjection.Item("WORKSPACE_HYDRATION_FAILED",hydrationFailure));return new ProjectStatusProjection(new StageProjection(StageStatus.STALE,stage.evidence(),limitations,stage.artifacts(),stage.actions()),value.source(),value.application(),value.correlation(),value.problemCount(),value.freshness());}
    public ApplicationOverviewProjection applicationOverview(){return projections.application(applicationModel);}
    public PageObjectCorrelationProjection correlations(int offset,int limit){return projections.correlations(correlation,offset,limit);}
    public ProblemsProjection problems(int offset,int limit){return projections.problems(sourceIndex,applicationModel,correlation,offset,limit);}
    public List<WorkflowSummaryProjection> workflowHistory(){return sessions.values().stream().sorted(Comparator.comparing(WorkflowSessionSnapshot::updatedAt).reversed()).limit(500).map(value->new WorkflowSummaryProjection(value.workflowId(),value.requirement(),value.state().name(),value.freshness().name(),value.updatedAt().toString(),value.resumed(),value.availableActions())).toList();}
    public List<RepairProjection> repairHistory(){return repairs.keySet().stream().sorted().limit(500).map(this::repairProjection).toList();}
    public WorkflowDetailProjection workflow(String runId){
        String selected=runId;if(selected==null||selected.isBlank())selected=requirements.keySet().stream().sorted().reduce((a,b)->b).orElse(null);
        if(selected==null)return new WorkflowDetailProjection(null,null,null,null,null,null,null,List.of(),null,"NOT_STARTED","FRESH",false,List.of(),List.of(),null);
        String selectedRun=selected;WorkflowReport report=reports.get(selectedRun);WorkflowRunProjection run=report==null?null:projections.workflow(report);WorkflowSessionSnapshot snapshot=sessions.get(selectedRun);
        RepairProjection repair=snapshot==null||snapshot.repair()==null?null:repairProjection(repairKey(selectedRun,snapshot.repair().proposalId()));
        return new WorkflowDetailProjection(selectedRun,requirements.get(selectedRun),plans.get(selectedRun),implementations.get(selectedRun),
                execution(selectedRun),diagnosis(selectedRun),repair,
                run==null?List.of():run.timeline(),run==null?null:run.metrics(),snapshot==null?(run==null?"NOT_STARTED":run.finalState()):snapshot.state().name(),snapshot==null?"FRESH":snapshot.freshness().name(),snapshot!=null&&snapshot.resumed(),snapshot==null?List.of():snapshot.availableActions(),snapshot==null?List.of():snapshot.limitations(),snapshot==null?null:snapshot.updatedAt().toString());
    }
    public StudioSnapshot snapshot(){return new StudioSnapshot(projectOverview(),applicationOverview(),correlations(0,100),problems(0,100),workflowHistory(),repairHistory());}
    public boolean operationRunning(){return operationRunning.get();}
    public StudioWorkspaceStore workspace(){return store;}
    public void attachProjectDescriptor(ProjectDescriptor descriptor){this.projectDescriptor=Objects.requireNonNull(descriptor);}
    public ProjectConfigurationProjection projectConfiguration(){ProjectDescriptor value=projectDescriptor;if(value==null)return null;Path root=value.projectRoot();java.util.function.Function<Path,String> relative=path->{Path normalized=path.toAbsolutePath().normalize();return normalized.startsWith(root)?root.relativize(normalized).toString().replace('\\','/'):"<outside-project>";};return new ProjectConfigurationProjection(value.projectId(),value.applicationName(),root.toString(),value.buildSystem().name(),value.status().name(),value.configurationSource().name(),value.mainSourceRoots().stream().map(relative).toList(),value.testSourceRoots().stream().map(relative).toList(),relative.apply(value.workspaceDirectory()),value.browser().browser().name(),value.browser().headless(),value.evidence(),value.limitations());}

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
    private WorkflowSessionSnapshot requireSession(String runId){return Objects.requireNonNull(sessions.get(runId),"Workflow session is missing");}
    private void requireAction(String runId,String action){if(!requireSession(runId).availableActions().contains(action))throw new IllegalStateException("Action "+action+" is not available for workflow "+runId);}
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
    private void hydrateWorkflowSessions(){
        try{for(String workflowId:store.workflowIds())try{
                WorkflowSessionSnapshot stored=WorkflowSessionSnapshot.fromDocument(store.readWorkflow(workflowId).orElseThrow());
                WorkflowSessionSnapshot.Freshness freshness=currentFingerprint()==null?WorkflowSessionSnapshot.Freshness.MISSING:Objects.equals(stored.sourceFingerprint(),currentFingerprint())?WorkflowSessionSnapshot.Freshness.FRESH:WorkflowSessionSnapshot.Freshness.STALE;
                List<String> reasons=freshness==WorkflowSessionSnapshot.Freshness.FRESH?stored.limitations():List.of("SOURCE_CHANGED");WorkflowSessionSnapshot restored=stored.restoredAfterInterruption(freshness,reasons);validateSnapshot(restored);sessions.put(workflowId,restored);requirements.put(workflowId,restored.requirement());persistSession(workflowId);
                if(restored.plan()!=null)plans.put(workflowId,projections.plan(restored.plan()));if(restored.execution()!=null)executions.put(workflowId,projections.execution(restored.execution()));if(restored.diagnosis()!=null)diagnoses.put(workflowId,projections.diagnosis(restored.diagnosis()));
                if(restored.plan()!=null&&freshness==WorkflowSessionSnapshot.Freshness.FRESH&&workflows!=null&&workflows.supportsReviewedArtifactRestore()){prepareWorkflow(workflowId,restored.requirement());workflows.restoreReviewedArtifacts(workflowId,restored.plan(),restored.implementation());if(restored.implementation()!=null)implementations.put(workflowId,projections.implementation(restored.implementation(),workflows.implementationPolicy(workflowId)));}
                else if(restored.implementation()!=null)implementations.put(workflowId,projections.implementation(restored.implementation(),List.of()));
                if(restored.repair()!=null){String key=repairKey(workflowId,restored.repair().proposalId());repairs.put(key,new RepairState(workflowId,restored.repair(),stored.projectFingerprint(),restored.repairDecision(),restored.repairApplyStatus()));}
            }catch(Exception failure){Instant now=Instant.now();String requirement="Unavailable workflow "+workflowId;sessions.put(workflowId,new WorkflowSessionSnapshot(1,workflowId,requirement,WorkflowSessionSnapshot.State.CORRUPTED,now,now,null,null,WorkflowSessionSnapshot.Freshness.INVALID,null,null,null,null,null,null,null,true,List.of("CORRUPTED_SNAPSHOT:"+failure.getClass().getSimpleName())));requirements.put(workflowId,requirement);}
        }catch(IOException failure){hydrationFailure="WORKFLOW_SESSION_DISCOVERY_FAILED:"+failure.getClass().getSimpleName();}
    }
    private void validateSnapshot(WorkflowSessionSnapshot value){switch(value.state()){
        case PLAN_READY_FOR_REVIEW-> {if(value.plan()==null)throw new IllegalArgumentException("Plan artifact is missing");}
        case IMPLEMENTATION_READY_FOR_REVIEW,EXECUTION_FAILED,REPAIR_READY_FOR_REVIEW,REPAIR_REJECTED,REPAIR_APPLIED_VERIFICATION_PENDING,SUCCESS,FAILED,EXECUTION_INTERRUPTED->{if(value.plan()==null||value.implementation()==null)throw new IllegalArgumentException("Reviewed artifacts are missing");}
        default->{}}
        if((value.state()==WorkflowSessionSnapshot.State.REPAIR_READY_FOR_REVIEW||value.state()==WorkflowSessionSnapshot.State.REPAIR_REJECTED||value.state()==WorkflowSessionSnapshot.State.REPAIR_APPLIED_VERIFICATION_PENDING)&&value.repair()==null)throw new IllegalArgumentException("Repair artifact is missing");
    }
    private void persistSession(String runId)throws IOException{WorkflowSessionSnapshot value=sessions.get(runId);if(value!=null)store.writeWorkflow(runId,value.toDocument());}
    private void refreshWorkflowInputsIfStale(String runId)throws IOException{
        WorkflowSessionSnapshot session=Objects.requireNonNull(sessions.get(runId),"Workflow session is missing");
        if(session.freshness()==WorkflowSessionSnapshot.Freshness.FRESH)return;
        sourceIndex=indexer.index(new ExistingProjectIndexer.Request(configuration.projectRoot(),configuration.sourceRoots(),configuration.classpathEntries()));
        if(applicationModel==null)throw new IllegalStateException("Application model is required to regenerate a stale plan");
        correlation=correlator.correlate(applicationModel,sourceIndex,CorrelationOverrides.none());persistProjectArtifacts();
    }
    private void beginPlanGeneration(String runId)throws IOException{
        WorkflowSessionSnapshot old=Objects.requireNonNull(sessions.get(runId),"Workflow session is missing");Instant now=Instant.now();
        WorkflowSessionSnapshot value=new WorkflowSessionSnapshot(1,runId,old.requirement(),WorkflowSessionSnapshot.State.PLAN_GENERATING,old.createdAt(),now,currentFingerprint(),currentFingerprint(),WorkflowSessionSnapshot.Freshness.FRESH,null,null,null,null,null,null,null,old.resumed(),List.of());
        sessions.put(runId,value);plans.remove(runId);implementations.remove(runId);executions.remove(runId);diagnoses.remove(runId);persistSession(runId);
    }
    private void beginImplementationGeneration(String runId)throws IOException{
        WorkflowSessionSnapshot old=Objects.requireNonNull(sessions.get(runId),"Workflow session is missing");if(old.plan()==null)throw new IllegalStateException("Reviewed plan is required");Instant now=Instant.now();
        WorkflowSessionSnapshot value=new WorkflowSessionSnapshot(1,runId,old.requirement(),WorkflowSessionSnapshot.State.IMPLEMENTATION_GENERATING,old.createdAt(),now,currentFingerprint(),currentFingerprint(),WorkflowSessionSnapshot.Freshness.FRESH,old.plan(),null,null,null,null,null,null,old.resumed(),old.limitations());
        sessions.put(runId,value);implementations.remove(runId);executions.remove(runId);diagnoses.remove(runId);persistSession(runId);
    }
    private void updateSession(String runId,WorkflowSessionSnapshot.State state,TestPlan plan,TestImplementationProposal implementation,TestExecutionResult execution,FailureClassification diagnosis,RepairProposal repair,String decision,String applyStatus)throws IOException{
        WorkflowSessionSnapshot old=Objects.requireNonNull(sessions.get(runId),"Workflow session is missing");WorkflowSessionSnapshot value=new WorkflowSessionSnapshot(1,runId,old.requirement(),state,old.createdAt(),Instant.now(),currentFingerprint(),currentFingerprint(),old.freshness(),plan==null?old.plan():plan,implementation==null?old.implementation():implementation,execution==null?old.execution():execution,diagnosis==null?old.diagnosis():diagnosis,repair==null?old.repair():repair,decision==null?old.repairDecision():decision,applyStatus==null?old.repairApplyStatus():applyStatus,old.resumed(),old.limitations());sessions.put(runId,value);persistSession(runId);
    }
    private void markStale(String runId,String reason)throws IOException{WorkflowSessionSnapshot old=sessions.get(runId);if(old==null)return;WorkflowSessionSnapshot value=old.restored(WorkflowSessionSnapshot.Freshness.STALE,List.of(reason));sessions.put(runId,value);persistSession(runId);}
    private void assertFresh(String runId)throws IOException{WorkflowSessionSnapshot session=Objects.requireNonNull(sessions.get(runId),"Workflow session is missing");if(session.freshness()!=WorkflowSessionSnapshot.Freshness.FRESH)throw new IllegalStateException("Workflow is stale");ExistingProjectIndex current=indexer.index(new ExistingProjectIndexer.Request(configuration.projectRoot(),configuration.sourceRoots(),configuration.classpathEntries()));if(!Objects.equals(session.sourceFingerprint(),current.projectFingerprint())){sourceIndex=current;markStale(runId,"SOURCE_CHANGED");throw new IllegalStateException("Workflow is stale");}}
    private String currentFingerprint(){return sourceIndex==null?null:sourceIndex.projectFingerprint();}
    private void persistHistories()throws IOException{String fp=sourceIndex==null?null:sourceIndex.projectFingerprint();store.write(StudioWorkspaceStore.ArtifactKind.WORKFLOW_HISTORY,Map.of("requirements",new TreeMap<>(requirements),"plans",new TreeMap<>(plans),"implementations",new TreeMap<>(implementations),"executions",new TreeMap<>(executions),"diagnoses",new TreeMap<>(diagnoses),"runs",workflowHistory()),fp);store.write(StudioWorkspaceStore.ArtifactKind.REPAIR_HISTORY,repairHistory(),fp);}
    private <T> T exclusive(IoOperation<T> operation)throws IOException{if(!operationRunning.compareAndSet(false,true))throw new IllegalStateException("A Studio operation is already running");try{return operation.run();}finally{operationRunning.set(false);}}
    private <T> T exclusiveAgent(AgentOperation<T> operation)throws AgentExecutor.AgentExecutionException,IOException{if(!operationRunning.compareAndSet(false,true))throw new IllegalStateException("A Studio operation is already running");try{return operation.run();}finally{operationRunning.set(false);}}
    @FunctionalInterface private interface IoOperation<T>{T run()throws IOException;}
    @FunctionalInterface private interface AgentOperation<T>{T run()throws AgentExecutor.AgentExecutionException,IOException;}
}
