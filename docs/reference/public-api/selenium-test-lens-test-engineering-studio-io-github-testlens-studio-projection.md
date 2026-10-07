---
search:
  exclude: true
---

# selenium-test-lens-test-engineering-studio: `io.github.testlens.studio.projection`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.studio.projection.StageProjection$Action` {#io-github-testlens-studio-projection-stageprojection-action}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StageProjection$Action(java.lang.String, java.lang.String, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String label()
public boolean enabled()
```

## `io.github.testlens.studio.projection.StageProjection$Artifact` {#io-github-testlens-studio-projection-stageprojection-artifact}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StageProjection$Artifact(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String kind()
public java.lang.String freshness()
```

## `io.github.testlens.studio.projection.StageProjection$Item` {#io-github-testlens-studio-projection-stageprojection-item}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StageProjection$Item(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String detail()
```

## `io.github.testlens.studio.projection.StageProjection` {#io-github-testlens-studio-projection-stageprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StageProjection(io.github.testlens.studio.projection.StageStatus, java.util.List<io.github.testlens.studio.projection.StageProjection$Item>, java.util.List<io.github.testlens.studio.projection.StageProjection$Item>, java.util.List<io.github.testlens.studio.projection.StageProjection$Artifact>, java.util.List<io.github.testlens.studio.projection.StageProjection$Action>)
public static io.github.testlens.studio.projection.StageProjection empty(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageStatus status()
public java.util.List<io.github.testlens.studio.projection.StageProjection$Item> evidence()
public java.util.List<io.github.testlens.studio.projection.StageProjection$Item> limitations()
public java.util.List<io.github.testlens.studio.projection.StageProjection$Artifact> artifacts()
public java.util.List<io.github.testlens.studio.projection.StageProjection$Action> actions()
```

## `io.github.testlens.studio.projection.StageStatus` {#io-github-testlens-studio-projection-stagestatus}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.projection.StageStatus NOT_STARTED
public static final io.github.testlens.studio.projection.StageStatus RUNNING
public static final io.github.testlens.studio.projection.StageStatus PASS
public static final io.github.testlens.studio.projection.StageStatus PARTIAL
public static final io.github.testlens.studio.projection.StageStatus FAILED
public static final io.github.testlens.studio.projection.StageStatus BLOCKED
public static final io.github.testlens.studio.projection.StageStatus NEEDS_REVIEW
public static final io.github.testlens.studio.projection.StageStatus STALE
public static io.github.testlens.studio.projection.StageStatus[] values()
public static io.github.testlens.studio.projection.StageStatus valueOf(java.lang.String)
```

## `io.github.testlens.studio.projection.StudioProjectionFactory` {#io-github-testlens-studio-projection-studioprojectionfactory}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static final int MAX_LIST_ITEMS
public io.github.testlens.studio.projection.StudioProjectionFactory()
public io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection project(io.github.testlens.selector.tooling.ExistingProjectIndex, io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.tooling.source.PageObjectCorrelation, java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.studio.projection.StudioProjections$ApplicationOverviewProjection application(io.github.testlens.application.model.ApplicationModel)
public io.github.testlens.studio.projection.StudioProjections$PageObjectCorrelationProjection correlations(io.github.testlens.application.tooling.source.PageObjectCorrelation, int, int)
public io.github.testlens.studio.projection.StudioProjections$ProblemsProjection problems(io.github.testlens.selector.tooling.ExistingProjectIndex, io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.tooling.source.PageObjectCorrelation, int, int)
public io.github.testlens.studio.projection.StudioProjections$WorkflowRunProjection workflow(io.github.testlens.application.tooling.ai.workflow.WorkflowReport)
public io.github.testlens.studio.projection.StudioProjections$TestPlanProjection plan(io.github.testlens.application.tooling.ai.TestPlan)
public io.github.testlens.studio.projection.StudioProjections$ImplementationProjection implementation(io.github.testlens.application.tooling.ai.TestImplementationProposal, java.util.List<io.github.testlens.studio.StudioWorkflowGateway$PolicyCheck>)
public io.github.testlens.studio.projection.StudioProjections$ExecutionProjection execution(io.github.testlens.application.tooling.ai.TestExecutionResult)
public io.github.testlens.studio.projection.StudioProjections$DiagnosisProjection diagnosis(io.github.testlens.application.tooling.ai.FailureClassification)
public io.github.testlens.studio.projection.StudioProjections$RepairProjection repair(io.github.testlens.application.tooling.ai.RepairProposal, java.lang.String, java.lang.String, boolean)
```

## `io.github.testlens.studio.projection.StudioProjections$ApplicationCounts` {#io-github-testlens-studio-projection-studioprojections-applicationcounts}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$ApplicationCounts(int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int pages()
public int states()
public int elements()
public int transitions()
public int sharedComponents()
```

## `io.github.testlens.studio.projection.StudioProjections$ApplicationOverviewProjection` {#io-github-testlens-studio-projection-studioprojections-applicationoverviewprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$ApplicationOverviewProjection(io.github.testlens.studio.projection.StageProjection, io.github.testlens.studio.projection.StudioProjections$ApplicationCounts, java.util.List<io.github.testlens.studio.projection.StudioProjections$PageSummary>, java.util.List<io.github.testlens.studio.projection.StudioProjections$QualityCount>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public io.github.testlens.studio.projection.StudioProjections$ApplicationCounts counts()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$PageSummary> pages()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$QualityCount> selectorQuality()
```

## `io.github.testlens.studio.projection.StudioProjections$Capability` {#io-github-testlens-studio-projection-studioprojections-capability}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$Capability(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String status()
public java.lang.String reason()
```

## `io.github.testlens.studio.projection.StudioProjections$CorrelationCounts` {#io-github-testlens-studio-projection-studioprojections-correlationcounts}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$CorrelationCounts(int, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int exact()
public int strong()
public int probable()
public int ambiguous()
public int noMatch()
public int conflict()
```

## `io.github.testlens.studio.projection.StudioProjections$CorrelationItem` {#io-github-testlens-studio-projection-studioprojections-correlationitem}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$CorrelationItem(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String applicationElementId()
public java.lang.String sourceElementId()
public java.lang.String sourceDeclarationRef()
public java.lang.String state()
public java.util.List<java.lang.String> evidence()
public java.util.List<java.lang.String> conflicts()
```

## `io.github.testlens.studio.projection.StudioProjections$DiagnosisProjection` {#io-github-testlens-studio-projection-studioprojections-diagnosisprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$DiagnosisProjection(io.github.testlens.studio.projection.StageProjection, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public java.lang.String category()
public java.lang.String rootCause()
public java.util.List<java.lang.String> affectedIds()
public java.util.List<java.lang.String> counterEvidence()
```

## `io.github.testlens.studio.projection.StudioProjections$ExecutionProjection` {#io-github-testlens-studio-projection-studioprojections-executionprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$ExecutionProjection(io.github.testlens.studio.projection.StageProjection, java.lang.String, java.lang.String, java.lang.String, long, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public java.lang.String scenarioId()
public java.lang.String compilation()
public java.lang.String execution()
public long durationMillis()
public java.util.List<java.lang.String> trace()
public java.util.List<java.lang.String> diagnostics()
public java.lang.String failureSummary()
```

## `io.github.testlens.studio.projection.StudioProjections$ImplementationProjection` {#io-github-testlens-studio-projection-studioprojections-implementationprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$ImplementationProjection(io.github.testlens.studio.projection.StageProjection, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<io.github.testlens.studio.projection.StudioProjections$PolicyResult>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public java.lang.String scenarioId()
public java.lang.String sourcePatch()
public java.lang.String selectorAccessPolicy()
public java.util.List<java.lang.String> pageObjectApis()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$PolicyResult> policyResults()
```

## `io.github.testlens.studio.projection.StudioProjections$PageObjectCorrelationProjection` {#io-github-testlens-studio-projection-studioprojections-pageobjectcorrelationprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$PageObjectCorrelationProjection(io.github.testlens.studio.projection.StageProjection, io.github.testlens.studio.projection.StudioProjections$CorrelationCounts, java.util.List<io.github.testlens.studio.projection.StudioProjections$CorrelationItem>, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public io.github.testlens.studio.projection.StudioProjections$CorrelationCounts counts()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$CorrelationItem> items()
public int total()
public int offset()
public int limit()
```

## `io.github.testlens.studio.projection.StudioProjections$PageSummary` {#io-github-testlens-studio-projection-studioprojections-pagesummary}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$PageSummary(java.lang.String, java.lang.String, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String name()
public int states()
public int elements()
public int transitions()
public int limitations()
```

## `io.github.testlens.studio.projection.StudioProjections$PolicyResult` {#io-github-testlens-studio-projection-studioprojections-policyresult}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$PolicyResult(java.lang.String, boolean, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String rule()
public boolean pass()
public java.lang.String detail()
```

## `io.github.testlens.studio.projection.StudioProjections$Problem` {#io-github-testlens-studio-projection-studioprojections-problem}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$Problem(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String category()
public java.lang.String severity()
public java.lang.String status()
public java.lang.String location()
public java.util.List<java.lang.String> evidence()
public java.util.List<java.lang.String> affectedArtifacts()
public java.lang.String suggestedAction()
```

## `io.github.testlens.studio.projection.StudioProjections$ProblemsProjection` {#io-github-testlens-studio-projection-studioprojections-problemsprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$ProblemsProjection(io.github.testlens.studio.projection.StageProjection, java.util.List<io.github.testlens.studio.projection.StudioProjections$Problem>, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$Problem> problems()
public int total()
public int offset()
public int limit()
```

## `io.github.testlens.studio.projection.StudioProjections$ProjectConfigurationProjection` {#io-github-testlens-studio-projection-studioprojections-projectconfigurationprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$ProjectConfigurationProjection(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String, java.lang.String, boolean, io.github.testlens.studio.projection.StudioProjections$Capability, io.github.testlens.studio.projection.StudioProjections$Capability, io.github.testlens.studio.projection.StudioProjections$Capability, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String projectId()
public java.lang.String name()
public java.lang.String root()
public java.lang.String build()
public java.lang.String status()
public java.lang.String configurationSource()
public java.util.List<java.lang.String> mainSourceRoots()
public java.util.List<java.lang.String> testSourceRoots()
public java.lang.String workspace()
public java.lang.String browser()
public boolean headless()
public io.github.testlens.studio.projection.StudioProjections$Capability browserCapability()
public io.github.testlens.studio.projection.StudioProjections$Capability agentCapability()
public io.github.testlens.studio.projection.StudioProjections$Capability compilationCapability()
public java.util.List<java.lang.String> evidence()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection` {#io-github-testlens-studio-projection-studioprojections-projectstatusprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection(io.github.testlens.studio.projection.StageProjection, io.github.testlens.studio.projection.StudioProjections$SourceCounts, io.github.testlens.studio.projection.StudioProjections$ApplicationCounts, io.github.testlens.studio.projection.StudioProjections$CorrelationCounts, int, java.util.Map<java.lang.String, java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public io.github.testlens.studio.projection.StudioProjections$SourceCounts source()
public io.github.testlens.studio.projection.StudioProjections$ApplicationCounts application()
public io.github.testlens.studio.projection.StudioProjections$CorrelationCounts correlation()
public int problemCount()
public java.util.Map<java.lang.String, java.lang.String> freshness()
```

## `io.github.testlens.studio.projection.StudioProjections$QualityCount` {#io-github-testlens-studio-projection-studioprojections-qualitycount}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$QualityCount(java.lang.String, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String quality()
public int count()
```

## `io.github.testlens.studio.projection.StudioProjections$RepairProjection` {#io-github-testlens-studio-projection-studioprojections-repairprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$RepairProjection(io.github.testlens.studio.projection.StageProjection, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public java.lang.String proposalId()
public java.lang.String decision()
public java.lang.String applyStatus()
public java.lang.String logicalPath()
public java.lang.String oldSelector()
public java.lang.String newSelector()
public java.util.List<java.lang.String> selectorEvidence()
public java.util.List<java.lang.String> affectedMethods()
public java.util.List<java.lang.String> affectedTests()
public java.util.List<java.lang.String> risks()
```

## `io.github.testlens.studio.projection.StudioProjections$Scenario` {#io-github-testlens-studio-projection-studioprojections-scenario}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$Scenario(java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String title()
public java.lang.String priority()
public java.util.List<java.lang.String> preconditions()
public java.util.List<java.lang.String> steps()
public java.util.List<java.lang.String> expected()
public java.lang.String existingCoverage()
public java.util.List<java.lang.String> existingTests()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.studio.projection.StudioProjections$SourceCounts` {#io-github-testlens-studio-projection-studioprojections-sourcecounts}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$SourceCounts(int, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int files()
public int pageObjects()
public int components()
public int tests()
public int locatorDeclarations()
public int usageEdges()
```

## `io.github.testlens.studio.projection.StudioProjections$StudioSnapshot` {#io-github-testlens-studio-projection-studioprojections-studiosnapshot}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$StudioSnapshot(io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection, io.github.testlens.studio.projection.StudioProjections$ApplicationOverviewProjection, io.github.testlens.studio.projection.StudioProjections$PageObjectCorrelationProjection, io.github.testlens.studio.projection.StudioProjections$ProblemsProjection, java.util.List<io.github.testlens.studio.projection.StudioProjections$WorkflowSummaryProjection>, java.util.List<io.github.testlens.studio.projection.StudioProjections$RepairProjection>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection project()
public io.github.testlens.studio.projection.StudioProjections$ApplicationOverviewProjection application()
public io.github.testlens.studio.projection.StudioProjections$PageObjectCorrelationProjection correlations()
public io.github.testlens.studio.projection.StudioProjections$ProblemsProjection problems()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$WorkflowSummaryProjection> workflows()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$RepairProjection> repairs()
```

## `io.github.testlens.studio.projection.StudioProjections$TestPlanProjection` {#io-github-testlens-studio-projection-studioprojections-testplanprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$TestPlanProjection(io.github.testlens.studio.projection.StageProjection, java.util.List<io.github.testlens.studio.projection.StudioProjections$Scenario>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$Scenario> scenarios()
```

## `io.github.testlens.studio.projection.StudioProjections$TimelineItem` {#io-github-testlens-studio-projection-studioprojections-timelineitem}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$TimelineItem(int, java.lang.String, java.lang.String, long, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int sequence()
public java.lang.String name()
public java.lang.String status()
public long durationMillis()
public java.lang.String summary()
```

## `io.github.testlens.studio.projection.StudioProjections$WorkflowDetailProjection` {#io-github-testlens-studio-projection-studioprojections-workflowdetailprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$WorkflowDetailProjection(java.lang.String, java.lang.String, io.github.testlens.studio.projection.StudioProjections$TestPlanProjection, io.github.testlens.studio.projection.StudioProjections$ImplementationProjection, io.github.testlens.studio.projection.StudioProjections$ExecutionProjection, io.github.testlens.studio.projection.StudioProjections$DiagnosisProjection, io.github.testlens.studio.projection.StudioProjections$RepairProjection, java.util.List<io.github.testlens.studio.projection.StudioProjections$TimelineItem>, io.github.testlens.studio.projection.StudioProjections$WorkflowMetrics, java.lang.String, java.lang.String, boolean, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String runId()
public java.lang.String requirement()
public io.github.testlens.studio.projection.StudioProjections$TestPlanProjection plan()
public io.github.testlens.studio.projection.StudioProjections$ImplementationProjection implementation()
public io.github.testlens.studio.projection.StudioProjections$ExecutionProjection execution()
public io.github.testlens.studio.projection.StudioProjections$DiagnosisProjection diagnosis()
public io.github.testlens.studio.projection.StudioProjections$RepairProjection repair()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$TimelineItem> timeline()
public io.github.testlens.studio.projection.StudioProjections$WorkflowMetrics metrics()
public java.lang.String status()
public java.lang.String freshness()
public boolean resumed()
public java.util.List<java.lang.String> availableActions()
public java.util.List<java.lang.String> limitations()
public java.lang.String updatedAt()
```

## `io.github.testlens.studio.projection.StudioProjections$WorkflowMetrics` {#io-github-testlens-studio-projection-studioprojections-workflowmetrics}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$WorkflowMetrics(long, long, long, long, long, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public long contextBytes()
public long agentDurationMillis()
public long compileDurationMillis()
public long executionDurationMillis()
public long totalDurationMillis()
public int attempts()
```

## `io.github.testlens.studio.projection.StudioProjections$WorkflowRunProjection` {#io-github-testlens-studio-projection-studioprojections-workflowrunprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$WorkflowRunProjection(io.github.testlens.studio.projection.StageProjection, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.studio.projection.StudioProjections$TimelineItem>, io.github.testlens.studio.projection.StudioProjections$WorkflowMetrics)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.projection.StageProjection stage()
public java.lang.String runId()
public java.lang.String requirement()
public java.lang.String finalState()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$TimelineItem> timeline()
public io.github.testlens.studio.projection.StudioProjections$WorkflowMetrics metrics()
```

## `io.github.testlens.studio.projection.StudioProjections$WorkflowSummaryProjection` {#io-github-testlens-studio-projection-studioprojections-workflowsummaryprojection}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.projection.StudioProjections$WorkflowSummaryProjection(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String runId()
public java.lang.String requirement()
public java.lang.String state()
public java.lang.String freshness()
public java.lang.String updatedAt()
public boolean resumed()
public java.util.List<java.lang.String> availableActions()
```

## `io.github.testlens.studio.projection.StudioProjections` {#io-github-testlens-studio-projection-studioprojections}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.projection`
- Classification: `INTERNAL`
- Type kind: `class`

```java
```
