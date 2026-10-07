---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.ai.security`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Finding` {#io-github-testlens-application-tooling-ai-security-agentartifactsecuritygate-finding}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.security`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Finding(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String detail()
```

## `io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Result` {#io-github-testlens-application-tooling-ai-security-agentartifactsecuritygate-result}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.security`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Result(io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Status, java.util.List<io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Finding>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Status status()
public java.util.List<io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Finding> findings()
```

## `io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Status` {#io-github-testlens-application-tooling-ai-security-agentartifactsecuritygate-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.security`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Status PASS
public static final io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Status BLOCKED
public static io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Status[] values()
public static io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate` {#io-github-testlens-application-tooling-ai-security-agentartifactsecuritygate}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai.security`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate()
public io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate$Result validate(java.lang.String, java.util.List<java.lang.String>, io.github.testlens.core.redaction.RedactionPolicy, java.util.List<java.lang.String>)
```
