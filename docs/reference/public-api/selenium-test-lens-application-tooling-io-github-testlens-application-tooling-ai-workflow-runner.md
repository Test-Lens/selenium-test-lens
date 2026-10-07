---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.ai.workflow.runner`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile$OutputTransport` {#io-github-testlens-application-tooling-ai-workflow-runner-agentprofile-outputtransport}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile$OutputTransport STDOUT
public static final io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile$OutputTransport FILE
public static io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile$OutputTransport[] values()
public static io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile$OutputTransport valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile` {#io-github-testlens-application-tooling-ai-workflow-runner-agentprofile}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public static final java.lang.String SCHEMA_FILE
public static final java.lang.String OUTPUT_FILE
public static final java.lang.String WORKING_DIRECTORY
public io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile(java.lang.String, java.nio.file.Path, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.time.Duration, int, int, int, java.util.Set<java.lang.String>, io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile$OutputTransport)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.nio.file.Path executable()
public java.util.List<java.lang.String> arguments()
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role role()
public java.time.Duration timeout()
public int maxInputBytes()
public int maxOutputBytes()
public int maxDiagnosticsBytes()
public java.util.Set<java.lang.String> environmentAllowlist()
public io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile$OutputTransport outputTransport()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec$DecodedResult` {#io-github-testlens-application-tooling-ai-workflow-runner-agentprotocolcodec-decodedresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec$DecodedResult(java.lang.String, java.lang.Object)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String resultType()
public java.lang.Object payload()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec` {#io-github-testlens-application-tooling-ai-workflow-runner-agentprotocolcodec}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec()
public byte[] encodeCommand(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentCommand)
public byte[] outputSchema(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role)
public io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec$DecodedResult decode(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, byte[])
```

## `io.github.testlens.application.tooling.ai.workflow.runner.CodexCliDetector` {#io-github-testlens-application-tooling-ai-workflow-runner-codexclidetector}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static java.util.Optional<java.nio.file.Path> detect()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.CodexCliProfiles` {#io-github-testlens-application-tooling-ai-workflow-runner-codexcliprofiles}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile readOnly(java.nio.file.Path, java.lang.String, io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.time.Duration, java.util.Set<java.lang.String>)
public static java.util.Set<java.lang.String> defaultEnvironmentAllowlist()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionMetrics` {#io-github-testlens-application-tooling-ai-workflow-runner-externalagentrunner-executionmetrics}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionMetrics(java.lang.String, io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus, long, long, long)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String profileId()
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role role()
public io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus status()
public long inputBytes()
public long outputBytes()
public long durationMillis()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus` {#io-github-testlens-application-tooling-ai-workflow-runner-externalagentrunner-executionstatus}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus SUCCESS
public static final io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus FAILED
public static final io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus TIMEOUT
public static io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus[] values()
public static io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionStatus valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$SourceExcerpt` {#io-github-testlens-application-tooling-ai-workflow-runner-externalagentrunner-sourceexcerpt}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$SourceExcerpt(java.nio.file.Path, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path logicalPath()
public java.lang.String content()
public java.lang.String contentFingerprint()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner` {#io-github-testlens-application-tooling-ai-workflow-runner-externalagentrunner}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner(io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile, java.nio.file.Path)
public io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner(io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile, java.nio.file.Path, io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec, io.github.testlens.core.redaction.RedactionPolicy, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$SourceExcerpt>, java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner(io.github.testlens.application.tooling.ai.workflow.runner.AgentProfile, java.nio.file.Path, io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec, io.github.testlens.core.redaction.RedactionPolicy, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$SourceExcerpt>, java.util.Map<java.lang.String, java.lang.String>, java.util.function.Consumer<io.github.testlens.application.tooling.ai.workflow.runner.ExternalAgentRunner$ExecutionMetrics>)
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentResult execute(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentCommand) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```

## `io.github.testlens.application.tooling.ai.workflow.runner.RoleDispatchingAgentExecutor` {#io-github-testlens-application-tooling-ai-workflow-runner-roledispatchingagentexecutor}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.runner.RoleDispatchingAgentExecutor(java.util.Map<io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, ? extends io.github.testlens.application.tooling.ai.workflow.AgentExecutor>)
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentResult execute(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentCommand) throws io.github.testlens.application.tooling.ai.workflow.AgentExecutor$AgentExecutionException
```
