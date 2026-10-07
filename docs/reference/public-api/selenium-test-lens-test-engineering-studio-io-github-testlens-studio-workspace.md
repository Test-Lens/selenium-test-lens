---
search:
  exclude: true
---

# selenium-test-lens-test-engineering-studio: `io.github.testlens.studio.workspace`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind` {#io-github-testlens-studio-workspace-studioworkspacestore-artifactkind}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.workspace`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind PROJECT_STATUS
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind APPLICATION_MODEL
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind PAGE_OBJECT_INDEX
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind USAGE_GRAPH
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind CORRELATIONS
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind PROBLEMS
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind WORKFLOW_HISTORY
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind REPAIR_HISTORY
public static io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind[] values()
public static io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind valueOf(java.lang.String)
```

## `io.github.testlens.studio.workspace.StudioWorkspaceStore$Freshness` {#io-github-testlens-studio-workspace-studioworkspacestore-freshness}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.workspace`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$Freshness FRESH
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$Freshness STALE
public static final io.github.testlens.studio.workspace.StudioWorkspaceStore$Freshness MISSING
public static io.github.testlens.studio.workspace.StudioWorkspaceStore$Freshness[] values()
public static io.github.testlens.studio.workspace.StudioWorkspaceStore$Freshness valueOf(java.lang.String)
```

## `io.github.testlens.studio.workspace.StudioWorkspaceStore$StoredArtifact` {#io-github-testlens-studio-workspace-studioworkspacestore-storedartifact}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.workspace`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.studio.workspace.StudioWorkspaceStore$StoredArtifact(io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind, byte[], java.lang.String, java.time.Instant)
public byte[] json()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind kind()
public java.lang.String sourceFingerprint()
public java.time.Instant writtenAt()
```

## `io.github.testlens.studio.workspace.StudioWorkspaceStore` {#io-github-testlens-studio-workspace-studioworkspacestore}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.workspace`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.studio.workspace.StudioWorkspaceStore(java.nio.file.Path)
public static io.github.testlens.studio.workspace.StudioWorkspaceStore atWorkspace(java.nio.file.Path)
public synchronized void write(io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind, java.lang.Object, java.lang.String) throws java.io.IOException
public synchronized java.util.Optional<java.util.Map<java.lang.String, java.lang.Object>> read(io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind) throws java.io.IOException
public synchronized io.github.testlens.studio.workspace.StudioWorkspaceStore$Freshness freshness(io.github.testlens.studio.workspace.StudioWorkspaceStore$ArtifactKind, java.lang.String) throws java.io.IOException
public synchronized void writeWorkflow(java.lang.String, java.lang.Object) throws java.io.IOException
public synchronized java.util.Optional<java.util.Map<java.lang.String, java.lang.Object>> readWorkflow(java.lang.String) throws java.io.IOException
public synchronized java.util.List<java.lang.String> workflowIds() throws java.io.IOException
public java.nio.file.Path workspaceRoot()
```

## `io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness` {#io-github-testlens-studio-workspace-workflowsessionsnapshot-freshness}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.workspace`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness FRESH
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness STALE
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness MISSING
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness INVALID
public static io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness[] values()
public static io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness valueOf(java.lang.String)
```

## `io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State` {#io-github-testlens-studio-workspace-workflowsessionsnapshot-state}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.workspace`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State CREATED
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State PLAN_GENERATING
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State PLAN_READY_FOR_REVIEW
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State IMPLEMENTATION_GENERATING
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State IMPLEMENTATION_READY_FOR_REVIEW
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State EXECUTION_RUNNING
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State VERIFICATION_RUNNING
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State EXECUTION_FAILED
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State REPAIR_READY_FOR_REVIEW
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State REPAIR_REJECTED
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State REPAIR_APPLIED_VERIFICATION_PENDING
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State SUCCESS
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State FAILED
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State AGENT_EXECUTION_INTERRUPTED
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State EXECUTION_INTERRUPTED
public static final io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State CORRUPTED
public static io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State[] values()
public static io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State valueOf(java.lang.String)
```

## `io.github.testlens.studio.workspace.WorkflowSessionSnapshot` {#io-github-testlens-studio-workspace-workflowsessionsnapshot}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.workspace`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.studio.workspace.WorkflowSessionSnapshot(int, java.lang.String, java.lang.String, io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State, java.time.Instant, java.time.Instant, java.lang.String, java.lang.String, io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness, io.github.testlens.application.tooling.ai.TestPlan, io.github.testlens.application.tooling.ai.TestImplementationProposal, io.github.testlens.application.tooling.ai.TestExecutionResult, io.github.testlens.application.tooling.ai.FailureClassification, io.github.testlens.application.tooling.ai.RepairProposal, java.lang.String, java.lang.String, boolean, java.util.List<java.lang.String>)
public io.github.testlens.studio.workspace.WorkflowSessionSnapshot restored(io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness, java.util.List<java.lang.String>)
public io.github.testlens.studio.workspace.WorkflowSessionSnapshot restoredAfterInterruption(io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness, java.util.List<java.lang.String>)
public io.github.testlens.studio.workspace.WorkflowSessionSnapshot withNewRepair(io.github.testlens.application.tooling.ai.RepairProposal, java.time.Instant)
public java.util.List<java.lang.String> availableActions()
public java.util.Map<java.lang.String, java.lang.Object> toDocument()
public static io.github.testlens.studio.workspace.WorkflowSessionSnapshot fromDocument(java.util.Map<java.lang.String, java.lang.Object>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String workflowId()
public java.lang.String requirement()
public io.github.testlens.studio.workspace.WorkflowSessionSnapshot$State state()
public java.time.Instant createdAt()
public java.time.Instant updatedAt()
public java.lang.String projectFingerprint()
public java.lang.String sourceFingerprint()
public io.github.testlens.studio.workspace.WorkflowSessionSnapshot$Freshness freshness()
public io.github.testlens.application.tooling.ai.TestPlan plan()
public io.github.testlens.application.tooling.ai.TestImplementationProposal implementation()
public io.github.testlens.application.tooling.ai.TestExecutionResult execution()
public io.github.testlens.application.tooling.ai.FailureClassification diagnosis()
public io.github.testlens.application.tooling.ai.RepairProposal repair()
public java.lang.String repairDecision()
public java.lang.String repairApplyStatus()
public boolean resumed()
public java.util.List<java.lang.String> limitations()
```
