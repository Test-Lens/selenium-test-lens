package io.github.testlens.studio.projection;

import java.util.List;
import java.util.Map;

/** Provider-neutral read models. Collections are deliberately bounded by projection adapters. */
public final class StudioProjections {
    private StudioProjections() {}
    public record ProjectStatusProjection(StageProjection stage, SourceCounts source, ApplicationCounts application,
                                          CorrelationCounts correlation, int problemCount, Map<String,String> freshness) { }
    public record SourceCounts(int files, int pageObjects, int components, int tests, int locatorDeclarations, int usageEdges) { }
    public record ApplicationCounts(int pages, int states, int elements, int transitions, int sharedComponents) { }
    public record CorrelationCounts(int exact, int strong, int probable, int ambiguous, int noMatch, int conflict) { }
    public record ProjectConfigurationProjection(String projectId,String name,String root,String build,String status,
            String configurationSource,List<String> mainSourceRoots,List<String> testSourceRoots,String workspace,
            String browser,boolean headless,List<String> evidence,List<String> limitations){ }
    public record ApplicationOverviewProjection(StageProjection stage, ApplicationCounts counts,
                                                List<PageSummary> pages, List<QualityCount> selectorQuality) { }
    public record PageSummary(String id, String name, int states, int elements, int transitions, int limitations) { }
    public record QualityCount(String quality, int count) { }
    public record PageObjectCorrelationProjection(StageProjection stage, CorrelationCounts counts,
                                                  List<CorrelationItem> items, int total, int offset, int limit) { }
    public record CorrelationItem(String pageId, String applicationElementId, String sourceElementId,
                                  String sourceDeclarationRef, String state, List<String> evidence,
                                  List<String> conflicts) { }
    public record ProblemsProjection(StageProjection stage, List<Problem> problems, int total, int offset, int limit) { }
    public record Problem(String id, String category, String severity, String status, String location,
                          List<String> evidence, List<String> affectedArtifacts, String suggestedAction) { }
    public record WorkflowRunProjection(StageProjection stage, String runId, String requirement, String finalState,
                                        List<TimelineItem> timeline, WorkflowMetrics metrics) { }
    public record TimelineItem(int sequence, String name, String status, long durationMillis, String summary) { }
    public record WorkflowMetrics(long contextBytes, long agentDurationMillis, long compileDurationMillis,
                                  long executionDurationMillis, long totalDurationMillis, int attempts) { }
    public record TestPlanProjection(StageProjection stage, List<Scenario> scenarios) { }
    public record Scenario(String id, String title, String priority, List<String> preconditions,
                           List<String> steps, List<String> expected, String existingCoverage,
                           List<String> existingTests, List<String> limitations) { }
    public record ImplementationProjection(StageProjection stage, String scenarioId, String sourcePatch,
                                           String selectorAccessPolicy, List<String> pageObjectApis,
                                           List<PolicyResult> policyResults) { }
    public record PolicyResult(String rule, boolean pass, String detail) { }
    public record ExecutionProjection(StageProjection stage, String scenarioId, String compilation,
                                      String execution, long durationMillis, List<String> trace,
                                      List<String> diagnostics, String failureSummary) { }
    public record DiagnosisProjection(StageProjection stage, String category, String rootCause,
                                      List<String> affectedIds, List<String> counterEvidence) { }
    public record RepairProjection(StageProjection stage, String proposalId, String decision, String applyStatus,
                                   String logicalPath, String oldSelector, String newSelector,
                                   List<String> selectorEvidence, List<String> affectedMethods,
                                   List<String> affectedTests, List<String> risks) { }
    public record WorkflowDetailProjection(String runId, String requirement, TestPlanProjection plan,
                                           ImplementationProjection implementation, ExecutionProjection execution,
                                           DiagnosisProjection diagnosis, RepairProjection repair,
                                           List<TimelineItem> timeline, WorkflowMetrics metrics, String status,
                                           String freshness,boolean resumed,List<String> availableActions,
                                           List<String> limitations,String updatedAt) { }
    public record WorkflowSummaryProjection(String runId,String requirement,String state,String freshness,
                                            String updatedAt,boolean resumed,List<String> availableActions){ }
    public record StudioSnapshot(ProjectStatusProjection project, ApplicationOverviewProjection application,
                                 PageObjectCorrelationProjection correlations, ProblemsProjection problems,
                                 List<WorkflowSummaryProjection> workflows, List<RepairProjection> repairs) { }
}
