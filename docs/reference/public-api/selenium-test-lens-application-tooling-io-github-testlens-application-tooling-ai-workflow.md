---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.ai.workflow`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentCommand` {#io-github-testlens-application-tooling-ai-workflow-agentexecutor-agentcommand}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `ADVANCED_API`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentCommand(java.lang.String, io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.util.List<io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?>>, java.util.Map<java.lang.String, java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String runId()
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role role()
public java.util.List<io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?>> inputs()
public java.util.Map<java.lang.String, java.lang.String> instructions()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException` {#io-github-testlens-application-tooling-ai-workflow-agentexecutor-agentexecutionexception}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException(java.lang.String)
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException(java.lang.String, java.lang.Throwable)
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode, java.lang.String)
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode, java.lang.String, java.lang.Throwable)
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode code()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode` {#io-github-testlens-application-tooling-ai-workflow-agentexecutor-agentfailurecode}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `ADVANCED_API`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode AGENT_NOT_AVAILABLE
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode AGENT_TIMEOUT
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode AGENT_PROCESS_FAILED
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode AGENT_OUTPUT_INVALID
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode AGENT_CONTEXT_REJECTED
public static io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode[] values()
public static io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentFailureCode valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentResult` {#io-github-testlens-application-tooling-ai-workflow-agentexecutor-agentresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `ADVANCED_API`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentResult(java.lang.String, io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String providerRequestId()
public io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?> artifact()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role` {#io-github-testlens-application-tooling-ai-workflow-agentexecutor-role}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role PLANNER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role IMPLEMENTER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role FAILURE_CLASSIFIER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role REPAIRER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role REVIEWER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role TEST_ARCHITECT
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role SCENARIO_DESIGNER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role TEST_IMPLEMENTER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role TEST_VERIFIER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role STABILIZER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role CODE_REVIEWER
public static final io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role UNIT_TEST_AGENT
public static io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role[] values()
public static io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.AgentExecutor` {#io-github-testlens-application-tooling-ai-workflow-agentexecutor}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `ADVANCED_API`
- Type kind: `interface`

```java
public abstract io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentResult execute(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentCommand) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$CompilationFeedback$Diagnostic` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-compilationfeedback-diagnostic}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$CompilationFeedback$Diagnostic(java.lang.String, java.lang.String, long, long, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String path()
public long line()
public long column()
public java.lang.String message()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$CompilationFeedback` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-compilationfeedback}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$CompilationFeedback(java.util.List<io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$CompilationFeedback$Diagnostic>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$CompilationFeedback$Diagnostic> diagnostics()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Configuration` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-configuration}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Configuration(java.nio.file.Path, java.lang.String, java.lang.String, int, java.lang.String, int, java.time.Duration, java.util.List<java.lang.String>, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path sourcePath()
public java.lang.String binaryName()
public java.lang.String currentSource()
public int release()
public java.lang.String classpath()
public int maxDiagnostics()
public java.time.Duration executionTimeout()
public java.util.List<java.lang.String> testSelectors()
public int maxImplementationAttempts()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$FailureClassifier` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-failureclassifier}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `interface`

```java
public abstract io.github.testlens.application.tooling.ai.FailureClassification classify(io.github.testlens.application.tooling.ai.TestExecutionResult)
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Metrics` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-metrics}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Metrics(long, long, long, long, long, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public long agentInputBytes()
public long agentOutputBytes()
public long agentDurationMillis()
public long compileDurationMillis()
public long executionDurationMillis()
public int agentAttempts()
public int compileAttempts()
public int executionAttempts()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Result` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-result}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Result(io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun, java.util.List<io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Step>, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Metrics)
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport toWorkflowReport(long, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun run()
public java.util.List<io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Step> steps()
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Metrics metrics()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Stabilizer` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-stabilizer}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `interface`

```java
public abstract java.util.Optional<io.github.testlens.application.tooling.ai.RepairProposal> propose(io.github.testlens.application.tooling.ai.FailureClassification, io.github.testlens.application.tooling.ai.TestExecutionResult)
public static io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Stabilizer none()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status PASS
public static final io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status FAIL
public static final io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status BLOCKED
public static final io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status PROPOSE_ONLY
public static final io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status APPROVE
public static final io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status REQUEST_CHANGES
public static io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status[] values()
public static io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Step` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator-step}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Step(int, java.lang.String, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status, long, long, long, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int sequence()
public java.lang.String name()
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Status status()
public long durationMillis()
public long inputBytes()
public long outputBytes()
public java.lang.String summary()
```

## `io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator` {#io-github-testlens-application-tooling-ai-workflow-agentworkflowcoordinator}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator(io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow, io.github.testlens.application.tooling.ai.workflow.AgentExecutor, io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler, io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor, io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$FailureClassifier, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Stabilizer, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Configuration)
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator(io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow, io.github.testlens.application.tooling.ai.workflow.AgentExecutor, io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler, io.github.testlens.application.tooling.ai.workflow.CompiledTargetedTestExecutor, io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$FailureClassifier, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Stabilizer, io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Configuration)
public io.github.testlens.application.tooling.ai.workflow.AgentWorkflowCoordinator$Result run(java.lang.String, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, io.github.testlens.application.tooling.ai.AgentContextPack) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```

## `io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope` {#io-github-testlens-application-tooling-ai-workflow-artifactenvelope}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `ADVANCED_API`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope(java.lang.String, java.lang.String, java.util.List<java.lang.String>, int, java.lang.String, T)
public static <T> io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<T> create(java.lang.String, java.lang.String, java.util.List<java.lang.String>, int, T)
public static java.lang.String digest(java.lang.Object)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String artifactId()
public java.lang.String runId()
public java.util.List<java.lang.String> parents()
public int attempt()
public java.lang.String payloadDigest()
public T payload()
```

## `io.github.testlens.application.tooling.ai.workflow.CompiledTargetedTestExecutor` {#io-github-testlens-application-tooling-ai-workflow-compiledtargetedtestexecutor}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `interface`

```java
public abstract io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionResult execute(io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionRequest, io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompiledOutput) throws java.lang.Exception
```

## `io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$ApplyRequest` {#io-github-testlens-application-tooling-ai-workflow-controlledsourceapplier-applyrequest}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$ApplyRequest(java.nio.file.Path, java.lang.String, java.lang.String, java.util.List<java.lang.String>, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path relativePath()
public java.lang.String expectedContentFingerprint()
public java.lang.String replacementContent()
public java.util.List<java.lang.String> allowedPathPrefixes()
public boolean trustedApply()
```

## `io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$ApplyResult` {#io-github-testlens-application-tooling-ai-workflow-controlledsourceapplier-applyresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$ApplyResult(io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status status()
public java.lang.String previousFingerprint()
public java.lang.String appliedFingerprint()
```

## `io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status` {#io-github-testlens-application-tooling-ai-workflow-controlledsourceapplier-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status APPLIED
public static final io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status TRUST_REQUIRED
public static final io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status PATH_BLOCKED
public static final io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status SOURCE_PRECONDITION_FAILED
public static io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status[] values()
public static io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier` {#io-github-testlens-application-tooling-ai-workflow-controlledsourceapplier}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier()
public io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$ApplyResult apply(java.nio.file.Path, io.github.testlens.application.tooling.ai.workflow.ControlledSourceApplier$ApplyRequest) throws java.io.IOException
```

## `io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Policy` {#io-github-testlens-application-tooling-ai-workflow-generatedtestpolicyvalidator-policy}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Policy(java.util.List<java.lang.String>, int, boolean, boolean, boolean, boolean, boolean, boolean)
public static io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Policy defaults()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<java.lang.String> forbiddenPathPrefixes()
public int maxCharacters()
public boolean blockRawBy()
public boolean blockDriverLookup()
public boolean blockSleeps()
public boolean blockJavascript()
public boolean blockRetryAnnotations()
public boolean blockLoops()
```

## `io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule` {#io-github-testlens-application-tooling-ai-workflow-generatedtestpolicyvalidator-rule}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule RAW_BY
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule DRIVER_LOOKUP
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule THREAD_SLEEP
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule JAVASCRIPT
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule RETRY_ANNOTATION
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule RETRY_LOOP
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule ASSERTION_FAILURE_SWALLOWED
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule FORBIDDEN_PATH
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule OUTSIDE_ALLOWED_PATHS
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule SIZE_LIMIT
public static final io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule SOURCE_PARSE_FAILED
public static io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule[] values()
public static io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$ValidationResult` {#io-github-testlens-application-tooling-ai-workflow-generatedtestpolicyvalidator-validationresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$ValidationResult(java.util.List<io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Violation>)
public boolean accepted()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Violation> violations()
```

## `io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Violation` {#io-github-testlens-application-tooling-ai-workflow-generatedtestpolicyvalidator-violation}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Violation(io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule, int, int, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Rule rule()
public int line()
public int column()
public java.lang.String evidence()
```

## `io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator` {#io-github-testlens-application-tooling-ai-workflow-generatedtestpolicyvalidator}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator(io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$Policy)
public io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$ValidationResult validate(java.nio.file.Path, java.lang.String)
public io.github.testlens.application.tooling.ai.workflow.GeneratedTestPolicyValidator$ValidationResult validate(io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, java.nio.file.Path, java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing` {#io-github-testlens-application-tooling-ai-workflow-pageobjectcapabilitymissing}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String capability()
public java.lang.String reason()
```

## `io.github.testlens.application.tooling.ai.workflow.PageObjectExtensionProposal` {#io-github-testlens-application-tooling-ai-workflow-pageobjectextensionproposal}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.PageObjectExtensionProposal(io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing, java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.PageObjectCapabilityMissing missing()
public java.lang.String extensionClass()
public java.lang.String proposedMethod()
public java.util.List<java.lang.String> evidence()
```

## `io.github.testlens.application.tooling.ai.workflow.ScriptedAgentExecutor` {#io-github-testlens-application-tooling-ai-workflow-scriptedagentexecutor}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.ScriptedAgentExecutor(java.util.Collection<io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentResult>)
public synchronized io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentResult execute(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentCommand) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
public synchronized int remaining()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationDiagnostic` {#io-github-testlens-application-tooling-ai-workflow-targetedjavacompiler-compilationdiagnostic}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationDiagnostic(java.lang.String, java.nio.file.Path, long, long, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.nio.file.Path path()
public long line()
public long column()
public java.lang.String message()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationRequest` {#io-github-testlens-application-tooling-ai-workflow-targetedjavacompiler-compilationrequest}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationRequest(java.util.List<io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$SourceUnit>, int, java.lang.String, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$SourceUnit> sources()
public int release()
public java.lang.String classpath()
public int maxDiagnostics()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationResult` {#io-github-testlens-application-tooling-ai-workflow-targetedjavacompiler-compilationresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationResult(boolean, java.util.List<io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationDiagnostic>, io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompiledOutput)
public int generatedClasses()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean successful()
public java.util.List<io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationDiagnostic> diagnostics()
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompiledOutput output()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompiledOutput` {#io-github-testlens-application-tooling-ai-workflow-targetedjavacompiler-compiledoutput}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public int classCount()
public java.lang.Class<?> loadClass(java.lang.String, java.lang.ClassLoader) throws java.lang.ClassNotFoundException
public void writeTo(java.nio.file.Path) throws java.io.IOException
public java.lang.String toString()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$SourceUnit` {#io-github-testlens-application-tooling-ai-workflow-targetedjavacompiler-sourceunit}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$SourceUnit(java.nio.file.Path, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path path()
public java.lang.String binaryName()
public java.lang.String content()
public java.lang.String expectedCurrentFingerprint()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler` {#io-github-testlens-application-tooling-ai-workflow-targetedjavacompiler}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler()
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler(javax.tools.JavaCompiler)
public io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationResult compile(io.github.testlens.application.tooling.ai.workflow.TargetedJavaCompiler$CompilationRequest, java.util.Map<java.nio.file.Path, java.lang.String>)
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionRequest` {#io-github-testlens-application-tooling-ai-workflow-targetedtestexecutor-executionrequest}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionRequest(java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.time.Duration)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String runId()
public java.lang.String testClass()
public java.util.List<java.lang.String> selectors()
public java.time.Duration timeout()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionResult` {#io-github-testlens-application-tooling-ai-workflow-targetedtestexecutor-executionresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionResult(boolean, int, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionResult(io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status, int, java.util.List<java.lang.String>)
public boolean successful()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status status()
public int tests()
public java.util.List<java.lang.String> boundedEvidence()
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status` {#io-github-testlens-application-tooling-ai-workflow-targetedtestexecutor-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status PASS
public static final io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status FAIL
public static final io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status TIMED_OUT
public static io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status[] values()
public static io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor` {#io-github-testlens-application-tooling-ai-workflow-targetedtestexecutor}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `interface`

```java
public abstract io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionResult execute(io.github.testlens.application.tooling.ai.workflow.TargetedTestExecutor$ExecutionRequest) throws java.lang.Exception
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$ExecutionPolicy` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrequest-executionpolicy}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$ExecutionPolicy DISABLED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$ExecutionPolicy TARGETED_AFTER_COMPILE
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$ExecutionPolicy[] values()
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$ExecutionPolicy valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Framework` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrequest-framework}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Framework(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String name()
public java.lang.String version()
public java.lang.String testEngine()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Requirement` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrequest-requirement}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Requirement(java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String summary()
public java.util.List<java.lang.String> acceptanceCriteria()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Scope` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrequest-scope}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Scope(java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<java.lang.String> included()
public java.util.List<java.lang.String> excluded()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Target` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrequest-target}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Target(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String module()
public java.lang.String testClass()
public java.lang.String scenarioId()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrequest}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest(io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Requirement, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Scope, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Framework, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Target, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$ExecutionPolicy)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Requirement requirement()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Scope scope()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Framework framework()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$Target target()
public java.util.List<java.lang.String> allowedPaths()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest$ExecutionPolicy executionPolicy()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$AuditEntry` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrun-auditentry}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$AuditEntry(int, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State, io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int sequence()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State from()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State to()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType event()
public java.lang.String evidence()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$Metrics` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrun-metrics}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$Metrics(int, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int corrections()
public int reruns()
public int repairs()
public int agentCalls()
public int compilations()
public int executions()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrun-state}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State CREATED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State CONTEXT_PREPARED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State PLAN_REQUESTED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State PLAN_READY
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State IMPLEMENTATION_REQUESTED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State IMPLEMENTATION_READY
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State COMPILE_FAILED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State EXECUTION_READY
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State EXECUTION_FAILED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State FAILURE_CLASSIFIED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State REPAIR_PROPOSED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State AWAITING_TRUSTED_APPLY
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State RERUN_REQUIRED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State REVIEW_REQUESTED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State SUCCESS
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State REJECTED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State BLOCKED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State FAILED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State NEEDS_HUMAN_REVIEW
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State[] values()
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State valueOf(java.lang.String)
public boolean terminal()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun` {#io-github-testlens-application-tooling-ai-workflow-testengineeringrun}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun(java.lang.String, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State, java.util.List<io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?>>, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$Metrics, java.util.List<io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$AuditEntry>, java.lang.String)
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun create(java.lang.String, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest)
public boolean terminal()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String runId()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRequest request()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State state()
public java.util.List<io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?>> artifacts()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$Metrics metrics()
public java.util.List<io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$AuditEntry> audit()
public java.lang.String trustedApplyMarker()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$Event` {#io-github-testlens-application-tooling-ai-workflow-testengineeringworkflow-event}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$Event(io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType, io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?>, java.lang.String, java.lang.String)
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$Event of(io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType)
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$Event withArtifact(io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType, io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?>)
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$Event trustedApply(java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType type()
public io.github.testlens.application.tooling.ai.workflow.ArtifactEnvelope<?> artifact()
public java.lang.String evidence()
public java.lang.String trustedApplyMarker()
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType` {#io-github-testlens-application-tooling-ai-workflow-testengineeringworkflow-eventtype}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType PREPARE_CONTEXT
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REQUEST_PLAN
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType PLAN_PRODUCED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REQUEST_IMPLEMENTATION
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType IMPLEMENTATION_PRODUCED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType COMPILE_SUCCEEDED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType COMPILE_FAILED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REQUEST_CORRECTION
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType EXECUTION_SUCCEEDED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType EXECUTION_FAILED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType CLASSIFY_FAILURE
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType PROPOSE_REPAIR
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REQUEST_TRUSTED_APPLY
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType TRUSTED_APPLY_RECORDED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType RERUN_REQUESTED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REVIEW_APPROVED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REVIEW_REJECTED
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType CAPABILITY_MISSING
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REJECT
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType FAIL
public static final io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType REQUIRE_HUMAN_REVIEW
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType[] values()
public static io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$EventType valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow` {#io-github-testlens-application-tooling-ai-workflow-testengineeringworkflow}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow(io.github.testlens.application.tooling.ai.workflow.WorkflowPolicy)
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow(io.github.testlens.application.tooling.ai.workflow.WorkflowPolicy, io.github.testlens.core.redaction.RedactionPolicy, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun reduce(io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun, io.github.testlens.application.tooling.ai.workflow.TestEngineeringWorkflow$Event)
```

## `io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$ApplyRequest` {#io-github-testlens-application-tooling-ai-workflow-trustedrepairapplier-applyrequest}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$ApplyRequest(io.github.testlens.application.tooling.ai.RepairProposal, io.github.testlens.selector.tooling.ExistingProjectIndex, java.util.List<java.lang.String>, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.RepairProposal proposal()
public io.github.testlens.selector.tooling.ExistingProjectIndex sourceIndex()
public java.util.List<java.lang.String> allowedPathPrefixes()
public boolean trustedApply()
```

## `io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$ApplyResult` {#io-github-testlens-application-tooling-ai-workflow-trustedrepairapplier-applyresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$ApplyResult(io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status status()
public java.lang.String logicalPath()
public java.lang.String previousFingerprint()
public java.lang.String appliedFingerprint()
```

## `io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status` {#io-github-testlens-application-tooling-ai-workflow-trustedrepairapplier-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status APPLIED
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status TRUST_REQUIRED
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status PROPOSAL_INVALID
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status CORRELATION_BLOCKED
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status PATH_BLOCKED
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status SOURCE_PRECONDITION_FAILED
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status DECLARATION_PRECONDITION_FAILED
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status OLD_SELECTOR_MISMATCH
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status UNSUPPORTED_DECLARATION
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status UNSUPPORTED_SELECTOR
public static final io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status UNSUPPORTED_SOURCE_ENCODING
public static io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status[] values()
public static io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier` {#io-github-testlens-application-tooling-ai-workflow-trustedrepairapplier}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier()
public io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$ApplyResult apply(java.nio.file.Path, io.github.testlens.application.tooling.ai.workflow.TrustedRepairApplier$ApplyRequest) throws java.io.IOException
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$ArtifactDocument` {#io-github-testlens-application-tooling-ai-workflow-workflowartifactstore-artifactdocument}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$ArtifactDocument(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String name()
public java.lang.String content()
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$Retention` {#io-github-testlens-application-tooling-ai-workflow-workflowartifactstore-retention}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$Retention(int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int maxRuns()
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$StoredArtifact` {#io-github-testlens-application-tooling-ai-workflow-workflowartifactstore-storedartifact}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$StoredArtifact(java.lang.String, java.lang.String, long)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String logicalPath()
public java.lang.String sha256()
public long bytes()
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore` {#io-github-testlens-application-tooling-ai-workflow-workflowartifactstore}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore()
public io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$StoredArtifact store(java.nio.file.Path, java.lang.String, io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$ArtifactDocument, io.github.testlens.core.redaction.RedactionPolicy, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.workflow.WorkflowArtifactStore$Retention) throws java.io.IOException
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowPolicy` {#io-github-testlens-application-tooling-ai-workflow-workflowpolicy}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.WorkflowPolicy(int, int, int, int, int)
public static io.github.testlens.application.tooling.ai.workflow.WorkflowPolicy defaults()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int maxCorrections()
public int maxReruns()
public int maxRepairs()
public int maxArtifacts()
public int maxAuditEntries()
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Metrics` {#io-github-testlens-application-tooling-ai-workflow-workflowreport-metrics}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Metrics(long, long, long, long, long, long, long, long, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public long contextBytes()
public long agentInputBytes()
public long agentOutputBytes()
public long externalAgentDurationMillis()
public long deterministicDurationMillis()
public long compileDurationMillis()
public long executionDurationMillis()
public long totalDurationMillis()
public int agentAttempts()
public int compileAttempts()
public int executionAttempts()
public int changedSourceFiles()
public int repairProposals()
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status` {#io-github-testlens-application-tooling-ai-workflow-workflowreport-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status PASS
public static final io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status FAIL
public static final io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status BLOCKED
public static final io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status NOT_RUN
public static final io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status PROPOSE_ONLY
public static final io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status APPROVE
public static final io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status REQUEST_CHANGES
public static io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status[] values()
public static io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Step` {#io-github-testlens-application-tooling-ai-workflow-workflowreport-step}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Step(int, java.lang.String, io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status, long, long, long, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int sequence()
public java.lang.String name()
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Status status()
public long durationMillis()
public long inputBytes()
public long outputBytes()
public java.lang.String summary()
public java.util.List<java.lang.String> evidenceRefs()
```

## `io.github.testlens.application.tooling.ai.workflow.WorkflowReport` {#io-github-testlens-application-tooling-ai-workflow-workflowreport}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport(int, java.lang.String, java.lang.String, io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State, java.util.List<io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Step>, io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Metrics, java.util.List<java.lang.String>)
public static io.github.testlens.application.tooling.ai.workflow.WorkflowReport from(io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun, java.util.List<io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Step>, io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Metrics, java.util.List<java.lang.String>)
public byte[] json()
public java.lang.String text()
public java.lang.String jsonText()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String runId()
public java.lang.String requirement()
public io.github.testlens.application.tooling.ai.workflow.TestEngineeringRun$State finalState()
public java.util.List<io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Step> steps()
public io.github.testlens.application.tooling.ai.workflow.WorkflowReport$Metrics metrics()
public java.util.List<java.lang.String> limitations()
```
