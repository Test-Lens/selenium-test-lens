---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.ai.workflow.runner.provider`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability` {#io-github-testlens-application-tooling-ai-workflow-runner-provider-agentexecutorprovider-provideravailability}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner.provider`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability(io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status, java.lang.String, java.lang.String)
public boolean available()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status status()
public java.lang.String reason()
public java.lang.String providerVersion()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderUnavailableException` {#io-github-testlens-application-tooling-ai-workflow-runner-provider-agentexecutorprovider-providerunavailableexception}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner.provider`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderUnavailableException(io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability)
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability availability()
```

## `io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status` {#io-github-testlens-application-tooling-ai-workflow-runner-provider-agentexecutorprovider-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner.provider`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public static final io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status AVAILABLE
public static final io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status NOT_AVAILABLE
public static final io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status VERSION_UNSUPPORTED
public static final io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status PROFILE_INVALID
public static io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status[] values()
public static io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider` {#io-github-testlens-application-tooling-ai-workflow-runner-provider-agentexecutorprovider}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner.provider`
- Classification: `USER_API`
- Type kind: `interface`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public abstract io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability availability()
public abstract io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability preflight()
public default io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability preflight(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.lang.String)
public abstract io.github.testlens.application.tooling.ai.workflow.AgentExecutor executorFor(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.runner.provider.CodexCliAgentExecutorProvider` {#io-github-testlens-application-tooling-ai-workflow-runner-provider-codexcliagentexecutorprovider}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner.provider`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.runner.provider.CodexCliAgentExecutorProvider(java.nio.file.Path)
public io.github.testlens.application.tooling.ai.workflow.runner.provider.CodexCliAgentExecutorProvider(java.nio.file.Path, java.nio.file.Path)
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability availability()
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability preflight()
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor executorFor(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.lang.String)
```

## `io.github.testlens.application.tooling.ai.workflow.runner.provider.ScriptedAgentExecutorProvider` {#io-github-testlens-application-tooling-ai-workflow-runner-provider-scriptedagentexecutorprovider}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.workflow.runner.provider`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.workflow.runner.provider.ScriptedAgentExecutorProvider(java.util.Map<io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, io.github.testlens.application.tooling.ai.workflow.AgentExecutor>)
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability availability()
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability preflight()
public io.github.testlens.application.tooling.ai.workflow.runner.provider.AgentExecutorProvider$ProviderAvailability preflight(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.lang.String)
public io.github.testlens.application.tooling.ai.workflow.AgentExecutor executorFor(io.github.testlens.application.tooling.ai.workflow.AgentExecutor$Role, java.lang.String)
```
