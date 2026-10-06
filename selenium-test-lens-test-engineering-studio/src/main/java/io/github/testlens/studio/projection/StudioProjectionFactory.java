package io.github.testlens.studio.projection;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.WorkflowReport;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.studio.StudioWorkflowGateway;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.testlens.studio.projection.StageProjection.*;
import static io.github.testlens.studio.projection.StudioProjections.*;

/** Pure adapters: they expose domain results and never recalculate domain decisions. */
public final class StudioProjectionFactory {
    public static final int MAX_LIST_ITEMS = 500;

    public ProjectStatusProjection project(ExistingProjectIndex source, ApplicationModel app,
                                           PageObjectCorrelation correlation, Map<String,String> freshness) {
        SourceCounts sourceCounts = source == null ? new SourceCounts(0,0,0,0,0,0) : new SourceCounts(
                source.metrics().files(), countClasses(source, ExistingProjectIndex.ClassClassification.PAGE_OBJECT),
                countClasses(source, ExistingProjectIndex.ClassClassification.COMPONENT), source.metrics().tests(),
                source.metrics().declarations(), source.metrics().edges());
        ApplicationCounts appCounts = applicationCounts(app);
        CorrelationCounts correlationCounts = correlationCounts(correlation);
        int problems = problems(source, app, correlation, 0, MAX_LIST_ITEMS).total();
        StageStatus status = source == null ? StageStatus.NOT_STARTED : app == null ? StageStatus.PARTIAL
                : correlation == null ? StageStatus.PARTIAL : problems > 0 ? StageStatus.NEEDS_REVIEW : StageStatus.PASS;
        List<Item> limitations = new ArrayList<>();
        if (source != null) source.limitations().forEach(v -> limitations.add(new Item(v.code(), text(v.logicalPath(), v.subject()))));
        if (app != null) app.limitations().forEach(v -> limitations.add(new Item(v.code(), v.detail())));
        if (correlation != null) correlation.limitations().forEach(v -> limitations.add(new Item("CORRELATION", v)));
        List<Artifact> artifacts = new ArrayList<>();
        if (source != null) { artifacts.add(new Artifact("page-object-index", "PageObjectIndex", fresh(freshness,"PageObjectIndex"))); artifacts.add(new Artifact("usage-graph", "UsageGraph", fresh(freshness,"UsageGraph"))); }
        if (app != null) artifacts.add(new Artifact("application-model", "ApplicationModel", fresh(freshness,"ApplicationModel")));
        if (correlation != null) artifacts.add(new Artifact("correlations", "PageObjectCorrelation", fresh(freshness,"Correlations")));
        List<Action> actions = List.of(new Action("scan-project","Scan project",true),
                new Action("map-application","Map application",true),
                new Action("correlate","Correlate Page Objects",source != null && app != null),
                new Action("refresh-project","Refresh project",true));
        return new ProjectStatusProjection(new StageProjection(status,List.of(),limitations,artifacts,actions),
                sourceCounts, appCounts, correlationCounts, problems, sortedMap(freshness));
    }

    public ApplicationOverviewProjection application(ApplicationModel model) {
        if (model == null) return new ApplicationOverviewProjection(StageProjection.empty("map-application","Start mapping"), applicationCounts(null), List.of(), List.of());
        List<PageSummary> pages = model.pages().stream().limit(MAX_LIST_ITEMS).map(page -> new PageSummary(page.pageId(),
                page.canonicalName(), page.states().size(), page.elements().size(), page.transitions().size(), page.limitations().size())).toList();
        Map<String,Long> quality = model.pages().stream().flatMap(v -> v.elements().stream())
                .collect(Collectors.groupingBy(v -> v.selectorQuality().name(), TreeMap::new, Collectors.counting()));
        StageStatus status = model.coverage().completeness() == ApplicationModel.Completeness.FAILED ? StageStatus.FAILED
                : model.coverage().completeness() == ApplicationModel.Completeness.PARTIAL || !model.limitations().isEmpty()
                ? StageStatus.PARTIAL : StageStatus.PASS;
        return new ApplicationOverviewProjection(new StageProjection(status, List.of(new Item("OBSERVATIONS",Integer.toString(model.coverage().observations()))),
                model.limitations().stream().map(v -> new Item(v.code(),v.detail())).toList(),
                List.of(new Artifact("application-model","ApplicationModel","FRESH")),
                List.of(new Action("map-application","Map application",true))), applicationCounts(model), pages,
                quality.entrySet().stream().map(v -> new QualityCount(v.getKey(), Math.toIntExact(v.getValue()))).toList());
    }

    public PageObjectCorrelationProjection correlations(PageObjectCorrelation value, int offset, int limit) {
        int safeLimit = limit(limit); int safeOffset = Math.max(0, offset);
        if (value == null) return new PageObjectCorrelationProjection(StageProjection.empty("correlate","Correlate Page Objects"), correlationCounts(null), List.of(),0,safeOffset,safeLimit);
        List<CorrelationItem> items = value.elements().stream().skip(safeOffset).limit(safeLimit).map(v -> new CorrelationItem(
                v.pageId(),v.applicationElementId(),v.sourceElementId(),v.sourceDeclarationRef(),v.state().name(),
                v.evidence().stream().map(Enum::name).toList(),v.conflicts())).toList();
        StageStatus status = value.completeness() == PageObjectCorrelation.Completeness.COMPLETE ? StageStatus.PASS : StageStatus.NEEDS_REVIEW;
        return new PageObjectCorrelationProjection(new StageProjection(status,List.of(),value.limitations().stream().map(v -> new Item("CORRELATION",v)).toList(),
                List.of(new Artifact("correlations","PageObjectCorrelation","FRESH")),List.of()), correlationCounts(value),items,value.elements().size(),safeOffset,safeLimit);
    }

    public ProblemsProjection problems(ExistingProjectIndex source, ApplicationModel app,
                                       PageObjectCorrelation correlation, int offset, int limit) {
        List<Problem> all = new ArrayList<>();
        if (source != null) source.limitations().forEach(v -> all.add(problem("source-"+all.size(),
                v.code().contains("DYNAMIC_CALL")?"UNRESOLVED_DYNAMIC_CALL":"MODEL_LIMITATION", "WARNING", text(v.logicalPath(),v.subject()), List.of(v.code()), "Rescan after resolving source limitation")));
        if (app != null) for (var page : app.pages()) for (var element : page.elements())
            if (element.selectorQuality() != ApplicationModel.SelectorQuality.VERIFIED)
                all.add(problem("selector-"+element.elementId(),"SELECTOR_REVIEW_REQUIRED","WARNING",page.canonicalName()+" / "+element.semanticName(),
                        element.limitations().stream().map(ApplicationModel.Limitation::code).toList(),"Review selector evidence"));
        if (correlation != null) for (var item : correlation.elements()) if (item.state() == PageObjectCorrelation.State.AMBIGUOUS
                || item.state() == PageObjectCorrelation.State.NO_MATCH || item.state() == PageObjectCorrelation.State.CONFLICT) {
            String category = switch (item.state()) { case AMBIGUOUS -> "AMBIGUOUS_CORRELATION"; case NO_MATCH -> "NO_SOURCE_CORRELATION"; default -> "PAGE_MODEL_DRIFT"; };
            all.add(problem("correlation-"+item.applicationElementId(),category,item.state()==PageObjectCorrelation.State.CONFLICT?"ERROR":"WARNING",
                    item.pageId()+" / "+item.applicationElementId(),item.conflicts(),"Review correlation evidence"));
        }
        all.sort(Comparator.comparing(Problem::severity).thenComparing(Problem::category).thenComparing(Problem::id));
        int safeOffset=Math.max(0,offset), safeLimit=limit(limit);
        List<Problem> page=all.stream().skip(safeOffset).limit(safeLimit).toList();
        return new ProblemsProjection(new StageProjection(all.isEmpty()?StageStatus.PASS:StageStatus.NEEDS_REVIEW,List.of(),List.of(),List.of(),List.of()),page,all.size(),safeOffset,safeLimit);
    }

    public WorkflowRunProjection workflow(WorkflowReport report) {
        StageStatus status = switch (report.finalState().name()) { case "COMPLETED" -> StageStatus.PASS; case "FAILED" -> StageStatus.FAILED; case "HUMAN_REVIEW_REQUIRED" -> StageStatus.NEEDS_REVIEW; default -> StageStatus.PARTIAL; };
        var m=report.metrics();
        return new WorkflowRunProjection(new StageProjection(status,List.of(),report.limitations().stream().map(v->new Item("WORKFLOW",v)).toList(),
                List.of(new Artifact(report.runId(),"WorkflowReport","FRESH")),List.of()),report.runId(),report.requirement(),report.finalState().name(),
                report.steps().stream().limit(MAX_LIST_ITEMS).map(v->new TimelineItem(v.sequence(),v.name(),v.status().name(),v.durationMillis(),v.summary())).toList(),
                new WorkflowMetrics(m.contextBytes(),m.externalAgentDurationMillis(),m.compileDurationMillis(),m.executionDurationMillis(),m.totalDurationMillis(),m.agentAttempts()+m.compileAttempts()+m.executionAttempts()));
    }

    public TestPlanProjection plan(TestPlan plan) {
        return new TestPlanProjection(header(plan.header(),"TestPlan"),plan.scenarios().stream().limit(MAX_LIST_ITEMS).map(s->new Scenario(s.scenarioId(),s.title(),s.priority().name(),s.preconditions(),
                s.steps().stream().map(v->v.order()+". "+v.action()).toList(),s.expected().stream().map(TestPlan.ExpectedResult::assertion).toList(),s.existingCoverage().name(),s.existingTestRefs(),s.knownUnknowns())).toList());
    }
    public ImplementationProjection implementation(TestImplementationProposal value, List<StudioWorkflowGateway.PolicyCheck> policyChecks) {
        List<PolicyResult> policyResults=(policyChecks==null?List.<StudioWorkflowGateway.PolicyCheck>of():policyChecks).stream().limit(100)
                .map(check->new PolicyResult(check.rule(),check.passed(),bounded(check.detail(),16_384))).toList();
        StageProjection base=header(value.header(),"ImplementationProposal");
        boolean blocked=policyResults.isEmpty()||policyResults.stream().anyMatch(result->!result.pass());
        StageProjection stage=new StageProjection(blocked?StageStatus.BLOCKED:base.status(),base.evidence(),
                blocked?merge(base.limitations(),new Item("POLICY","Deterministic generated-test policy rejected or was not provided")):base.limitations(),
                base.artifacts(),base.actions());
        return new ImplementationProjection(stage,value.scenarioId(),bounded(value.sourcePatch(),1_000_000),value.selectorAccessPolicy().name(),value.pageObjectApisUsed(),policyResults);
    }
    public ExecutionProjection execution(TestExecutionResult value) {
        return new ExecutionProjection(header(value.header(),"ExecutionResult"),value.scenarioId(),value.compileOutcome().name(),value.executionOutcome().name(),value.durationMillis(),
                bounded(value.traceEvidenceRefs(),MAX_LIST_ITEMS),bounded(value.selectorDiagnosticRefs(),MAX_LIST_ITEMS),bounded(value.failureSummary(),16_384));
    }
    public DiagnosisProjection diagnosis(FailureClassification value) {
        StageProjection stage=header(value.header(),"FailureClassification");
        stage=new StageProjection(stage.status(),value.evidenceRefs().stream().map(v->new Item("EVIDENCE",v)).toList(),stage.limitations(),stage.artifacts(),stage.actions());
        return new DiagnosisProjection(stage,value.category().name(),value.rootCause(),value.affectedIds(),value.counterEvidenceRefs());
    }
    public RepairProjection repair(RepairProposal value, String decision, String applyStatus, boolean stale) {
        List<String> evidence=new ArrayList<>(value.sameTargetEvidence()); evidence.addAll(value.stabilityEvidence());
        StageStatus status=stale?StageStatus.STALE:"REJECTED".equals(decision)?StageStatus.NEEDS_REVIEW:
                "APPLIED".equals(applyStatus)?StageStatus.PASS:StageStatus.NEEDS_REVIEW;
        return new RepairProjection(new StageProjection(status,evidence.stream().map(v->new Item("SELECTOR",v)).toList(),
                value.risks().stream().map(v->new Item("RISK",v)).toList(),List.of(new Artifact(value.proposalId(),"RepairProposal",stale?"STALE":"FRESH")),
                List.of(new Action("approve-repair","Approve and apply",!stale&&decision==null),new Action("reject-repair","Reject",decision==null))),
                value.proposalId(),decision,applyStatus,value.sourceTarget()==null?null:value.sourceTarget().logicalPath(),value.oldSelector(),value.newSelector(),evidence,value.affectedMethods(),value.affectedTests(),value.risks());
    }

    private static StageProjection header(ContractHeader h,String artifact) {
        StageStatus status=switch(h.status()){case READY,COMPLETED->StageStatus.PASS;case PARTIAL->StageStatus.PARTIAL;case BLOCKED->StageStatus.BLOCKED;case FAILED->StageStatus.FAILED;case UNKNOWN->StageStatus.NEEDS_REVIEW;};
        return new StageProjection(status,h.evidence().stream().map(v->new Item("EVIDENCE",v)).toList(),h.limitations().stream().map(v->new Item("LIMITATION",v)).toList(),List.of(new Artifact(artifact.toLowerCase(Locale.ROOT),artifact,"FRESH")),List.of());
    }
    private static Problem problem(String id,String category,String severity,String location,List<String> evidence,String action){return new Problem(id,category,severity,"OPEN",location,bounded(evidence,100),List.of(),action);}
    private static int countClasses(ExistingProjectIndex s,ExistingProjectIndex.ClassClassification c){return(int)s.classes().stream().filter(v->v.classification()==c).count();}
    private static ApplicationCounts applicationCounts(ApplicationModel a){if(a==null)return new ApplicationCounts(0,0,0,0,0);return new ApplicationCounts(a.pages().size(),a.pages().stream().mapToInt(v->v.states().size()).sum(),a.pages().stream().mapToInt(v->v.elements().size()).sum(),a.coverage().transitions(),a.sharedComponents().size());}
    private static CorrelationCounts correlationCounts(PageObjectCorrelation c){if(c==null)return new CorrelationCounts(0,0,0,0,0,0);var m=c.metrics();return new CorrelationCounts(m.exact(),m.strong(),m.probable(),m.ambiguous(),m.noMatch(),m.conflicts());}
    private static int limit(int value){return Math.max(1,Math.min(MAX_LIST_ITEMS,value));}
    private static String bounded(String value,int max){return value==null?null:value.length()<=max?value:value.substring(0,max);}
    private static List<String> bounded(List<String> values,int max){return(values==null?List.<String>of():values).stream().filter(Objects::nonNull).distinct().sorted().limit(max).toList();}
    private static <T> List<T> merge(List<T> values,T value){List<T> result=new ArrayList<>(values);result.add(value);return result;}
    private static String text(String a,String b){return Stream.of(a,b).filter(Objects::nonNull).collect(Collectors.joining(" / "));}
    private static String fresh(Map<String,String> values,String key){return values==null?"UNKNOWN":values.getOrDefault(key,"UNKNOWN");}
    private static Map<String,String> sortedMap(Map<String,String> values){return Collections.unmodifiableMap(new TreeMap<>(values==null?Map.of():values));}
}
