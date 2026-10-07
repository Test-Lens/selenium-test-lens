---
search:
  exclude: true
---

# selenium-test-lens-test-engineering-studio: `io.github.testlens.studio`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.studio.CoordinatorWorkflowGateway$Input` {#io-github-testlens-studio-coordinatorworkflowgateway-input}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.CoordinatorWorkflowGateway$Input(io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, io.github.testlens.application.tooling.ai.AgentContextPack)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request()
public io.github.testlens.application.tooling.ai.AgentContextPack context()
```

## `io.github.testlens.studio.CoordinatorWorkflowGateway` {#io-github-testlens-studio-coordinatorworkflowgateway}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.CoordinatorWorkflowGateway(io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator, java.util.function.Function<java.lang.String, io.github.testlens.studio.CoordinatorWorkflowGateway$Input>)
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```

## `io.github.testlens.studio.ForkedJUnitTargetedTestMain` {#io-github-testlens-studio-forkedjunittargetedtestmain}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static void main(java.lang.String[])
```

## `io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway$RepairSourceCompiler` {#io-github-testlens-studio-reviewablecoordinatorworkflowgateway-repairsourcecompiler}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `interface`

```java
public abstract void compile(io.github.testlens.application.tooling.ai.RepairProposal) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```

## `io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway` {#io-github-testlens-studio-reviewablecoordinatorworkflowgateway}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway(io.github.testlens.application.tooling.ai.workflow.AgentExecutor, java.util.function.Function<io.github.testlens.application.tooling.ai.workflow.AgentExecutor, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator>)
public io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway(io.github.testlens.application.tooling.ai.workflow.AgentExecutor, java.util.function.BiFunction<io.github.testlens.application.tooling.ai.workflow.AgentExecutor, io.github.testlens.studio.CoordinatorWorkflowGateway$Input, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator>)
public io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway(io.github.testlens.application.tooling.ai.workflow.AgentExecutor, java.util.function.Function<io.github.testlens.application.tooling.ai.workflow.AgentExecutor, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator>, io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator, java.util.function.Function<io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, java.nio.file.Path>, io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway$RepairSourceCompiler)
public io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway(io.github.testlens.application.tooling.ai.workflow.AgentExecutor, java.util.function.BiFunction<io.github.testlens.application.tooling.ai.workflow.AgentExecutor, io.github.testlens.studio.CoordinatorWorkflowGateway$Input, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator>, io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator, java.util.function.Function<io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, java.nio.file.Path>, io.github.testlens.studio.ReviewableCoordinatorWorkflowGateway$RepairSourceCompiler)
public void prepare(java.lang.String, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, io.github.testlens.application.tooling.ai.AgentContextPack)
public void restoreReviewedArtifacts(java.lang.String, io.github.testlens.application.tooling.ai.TestPlan, io.github.testlens.application.tooling.ai.TestImplementationProposal)
public io.github.testlens.application.tooling.ai.TestPlan generatePlan(java.lang.String, java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public io.github.testlens.application.tooling.ai.TestImplementationProposal generateImplementation(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public io.github.testlens.application.tooling.ai.TestExecutionResult execution(java.lang.String)
public io.github.testlens.application.tooling.ai.FailureClassification diagnosis(java.lang.String)
public io.github.testlens.application.tooling.ai.RepairProposal repair(java.lang.String)
public java.util.List<io.github.testlens.studio.StudioWorkflowGateway$PolicyCheck> implementationPolicy(java.lang.String)
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport rerun(java.lang.String, io.github.testlens.application.tooling.ai.RepairProposal) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public boolean supportsReviewedArtifactRestore()
```

## `io.github.testlens.studio.StudioWorkflowGateway$PolicyCheck` {#io-github-testlens-studio-studioworkflowgateway-policycheck}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.StudioWorkflowGateway$PolicyCheck(java.lang.String, boolean, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String rule()
public boolean passed()
public java.lang.String detail()
```

## `io.github.testlens.studio.StudioWorkflowGateway` {#io-github-testlens-studio-studioworkflowgateway}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `interface`

```java
public default void prepare(java.lang.String, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, io.github.testlens.application.tooling.ai.AgentContextPack)
public default boolean supportsReviewedArtifactRestore()
public default void restoreReviewedArtifacts(java.lang.String, io.github.testlens.application.tooling.ai.TestPlan, io.github.testlens.application.tooling.ai.TestImplementationProposal)
public default io.github.testlens.application.tooling.ai.TestPlan generatePlan(java.lang.String, java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public default io.github.testlens.application.tooling.ai.TestImplementationProposal generateImplementation(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public default java.util.List<io.github.testlens.studio.StudioWorkflowGateway$PolicyCheck> implementationPolicy(java.lang.String)
public abstract io.github.testlens.application.tooling.ai.workflow.WorkflowReport run(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public default io.github.testlens.application.tooling.ai.TestExecutionResult execution(java.lang.String)
public default io.github.testlens.application.tooling.ai.FailureClassification diagnosis(java.lang.String)
public default io.github.testlens.application.tooling.ai.RepairProposal repair(java.lang.String)
public default io.github.testlens.application.tooling.ai.workflow.WorkflowReport rerun(java.lang.String, io.github.testlens.application.tooling.ai.RepairProposal) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```

## `io.github.testlens.studio.TargetedRepairSourceCompiler` {#io-github-testlens-studio-targetedrepairsourcecompiler}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.TargetedRepairSourceCompiler(java.nio.file.Path, int, java.lang.String)
public void compile(io.github.testlens.application.tooling.ai.RepairProposal) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```

## `io.github.testlens.studio.TestEngineeringStudio$LaunchHandle` {#io-github-testlens-studio-testengineeringstudio-launchhandle}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public java.net.URI uri()
public java.lang.String projectId()
public void await() throws java.lang.InterruptedException
public void close()
```

## `io.github.testlens.studio.TestEngineeringStudio$LaunchRequest` {#io-github-testlens-studio-testengineeringstudio-launchrequest}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public io.github.testlens.studio.TestEngineeringStudio$LaunchRequest(java.nio.file.Path, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path projectRoot()
public java.util.List<java.nio.file.Path> mainSourceRoots()
public java.util.List<java.nio.file.Path> testSourceRoots()
public java.util.List<java.nio.file.Path> testClasspath()
public boolean openBrowser()
```

## `io.github.testlens.studio.TestEngineeringStudio` {#io-github-testlens-studio-testengineeringstudio}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public static io.github.testlens.studio.TestEngineeringStudio$LaunchHandle launch(io.github.testlens.studio.TestEngineeringStudio$LaunchRequest) throws java.io.IOException
public static io.github.testlens.studio.TestEngineeringStudio$LaunchHandle launch(io.github.testlens.studio.TestEngineeringStudio$LaunchRequest, io.github.testlens.studio.browser.BrowserSessionProvider, io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider) throws java.io.IOException
```

## `io.github.testlens.studio.TestEngineeringStudioService$ActionResult` {#io-github-testlens-studio-testengineeringstudioservice-actionresult}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.TestEngineeringStudioService$ActionResult(java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String actionId()
public java.lang.String status()
public java.lang.String detail()
public java.lang.Object projection()
```

## `io.github.testlens.studio.TestEngineeringStudioService$Configuration` {#io-github-testlens-studio-testengineeringstudioservice-configuration}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.TestEngineeringStudioService$Configuration(java.nio.file.Path, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path projectRoot()
public java.util.List<java.nio.file.Path> sourceRoots()
public java.util.List<java.nio.file.Path> classpathEntries()
public java.util.List<java.lang.String> allowedRepairPrefixes()
public java.lang.String applicationName()
```

## `io.github.testlens.studio.TestEngineeringStudioService` {#io-github-testlens-studio-testengineeringstudioservice}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.TestEngineeringStudioService(io.github.testlens.studio.TestEngineeringStudioService$Configuration, io.github.testlens.studio.StudioWorkflowGateway, java.util.function.Supplier<org.openqa.selenium.WebDriver>)
public io.github.testlens.studio.TestEngineeringStudioService(io.github.testlens.studio.TestEngineeringStudioService$Configuration, io.github.testlens.studio.StudioWorkflowGateway, java.util.function.Supplier<org.openqa.selenium.WebDriver>, java.nio.file.Path)
public io.github.testlens.studio.TestEngineeringStudioService(io.github.testlens.studio.TestEngineeringStudioService$Configuration, io.github.testlens.studio.StudioWorkflowGateway, io.github.testlens.studio.browser.BrowserSessionProvider, java.nio.file.Path)
public io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection scanProject() throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$ApplicationOverviewProjection mapApplication(io.github.testlens.application.mapper.ApplicationMapperOptions$Mode) throws java.io.IOException
public void close()
public io.github.testlens.studio.projection.StudioProjections$PageObjectCorrelationProjection correlate() throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection refreshProject() throws java.io.IOException
public java.lang.String createRequirement(java.lang.String) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$TestPlanProjection generatePlan(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException, java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$ImplementationProjection generateImplementation(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException, java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$WorkflowRunProjection runWorkflow(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException, java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$WorkflowRunProjection rerun(java.lang.String) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException, java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$ExecutionProjection execution(java.lang.String)
public io.github.testlens.studio.projection.StudioProjections$DiagnosisProjection diagnosis(java.lang.String)
public io.github.testlens.studio.projection.StudioProjections$DiagnosisProjection diagnoseWorkflow(java.lang.String) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$RepairProjection prepareRepair(java.lang.String) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$RepairProjection registerRepairProposal(java.lang.String, io.github.testlens.application.tooling.ai.RepairProposal) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$RepairProjection registerRepairProposal(io.github.testlens.application.tooling.ai.RepairProposal) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$RepairProjection rejectRepair(java.lang.String, java.lang.String) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$RepairProjection rejectRepair(java.lang.String) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$RepairProjection approveRepair(java.lang.String, java.lang.String) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$RepairProjection approveRepair(java.lang.String) throws java.io.IOException
public io.github.testlens.studio.projection.StudioProjections$ProjectStatusProjection projectOverview()
public io.github.testlens.studio.projection.StudioProjections$ApplicationOverviewProjection applicationOverview()
public io.github.testlens.studio.projection.StudioProjections$PageObjectCorrelationProjection correlations(int, int)
public io.github.testlens.studio.projection.StudioProjections$ProblemsProjection problems(int, int)
public java.util.List<io.github.testlens.studio.projection.StudioProjections$WorkflowSummaryProjection> workflowHistory()
public java.util.List<io.github.testlens.studio.projection.StudioProjections$RepairProjection> repairHistory()
public io.github.testlens.studio.projection.StudioProjections$WorkflowDetailProjection workflow(java.lang.String)
public io.github.testlens.studio.projection.StudioProjections$StudioSnapshot snapshot()
public boolean operationRunning()
public io.github.testlens.studio.workspace.StudioWorkspaceStore workspace()
public void attachProjectDescriptor(io.github.testlens.studio.project.ProjectDescriptor)
public void attachBrowserProfile(java.lang.String)
public void attachCapabilities(io.github.testlens.studio.projection.StudioProjections$Capability, io.github.testlens.studio.projection.StudioProjections$Capability, io.github.testlens.studio.projection.StudioProjections$Capability)
public io.github.testlens.studio.projection.StudioProjections$ProjectConfigurationProjection projectConfiguration()
```
