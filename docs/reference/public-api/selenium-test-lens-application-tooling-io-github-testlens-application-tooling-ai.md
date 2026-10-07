---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.ai`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.ai.AgentContextPack$Completeness` {#io-github-testlens-application-tooling-ai-agentcontextpack-completeness}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentContextPack$Completeness COMPLETE_FOR_REQUESTED_SCOPE
public static final io.github.testlens.application.tooling.ai.AgentContextPack$Completeness PARTIAL
public static final io.github.testlens.application.tooling.ai.AgentContextPack$Completeness FAILED
public static io.github.testlens.application.tooling.ai.AgentContextPack$Completeness[] values()
public static io.github.testlens.application.tooling.ai.AgentContextPack$Completeness valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ElementContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-elementcontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$ElementContext(java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String elementId()
public java.lang.String semanticName()
public java.lang.String type()
public java.util.List<java.lang.String> actions()
public java.lang.String selectorQuality()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ExistingTestContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-existingtestcontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$ExistingTestContext(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String testId()
public java.lang.String ownerClassId()
public java.lang.String methodId()
public java.lang.String framework()
public java.util.List<java.lang.String> tags()
public java.util.List<java.lang.String> groups()
public java.util.List<java.lang.String> relevantMethodIds()
public java.util.List<java.lang.String> includedBecause()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$PageContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-pagecontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$PageContext(java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$StateContext>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ElementContext>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$TransitionContext>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String canonicalName()
public java.lang.String urlPattern()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$StateContext> states()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ElementContext> elements()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$TransitionContext> transitions()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectClassContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-pageobjectclasscontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectClassContext(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String classId()
public java.lang.String pageId()
public java.lang.String qualifiedName()
public java.lang.String logicalPath()
public java.lang.String classification()
public java.lang.String origin()
public java.util.List<java.lang.String> includedBecause()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectMethodContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-pageobjectmethodcontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectMethodContext(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String methodId()
public java.lang.String ownerClassId()
public java.lang.String signature()
public java.lang.String classification()
public java.util.List<java.lang.String> actions()
public java.util.List<java.lang.String> referencedApplicationElementIds()
public java.util.List<java.lang.String> outgoingTypes()
public java.util.List<java.lang.String> includedBecause()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision` {#io-github-testlens-application-tooling-ai-agentcontextpack-scopedecision}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String kind()
public java.lang.String reason()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$SourceContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-sourcecontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$SourceContext(java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectClassContext>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectMethodContext>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$SourceDeclarationContext>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ExistingTestContext>)
public static io.github.testlens.application.tooling.ai.AgentContextPack$SourceContext empty()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectClassContext> classes()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectMethodContext> methods()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$SourceDeclarationContext> declarations()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ExistingTestContext> tests()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$SourceDeclarationContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-sourcedeclarationcontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$SourceDeclarationContext(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String declarationRef()
public java.lang.String ownerClassId()
public java.lang.String fieldName()
public java.lang.String strategy()
public java.lang.String selectorValueFingerprint()
public java.lang.String applicationElementId()
public java.lang.String correlationState()
public java.util.List<java.lang.String> includedBecause()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$StateContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-statecontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$StateContext(java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String stateId()
public java.lang.String semanticName()
public java.util.List<java.lang.String> elementIds()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$TransitionContext` {#io-github-testlens-application-tooling-ai-agentcontextpack-transitioncontext}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$TransitionContext(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String transitionId()
public java.lang.String action()
public java.lang.String elementId()
public java.lang.String targetPageId()
public java.lang.String targetStateId()
public java.lang.String confidence()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack` {#io-github-testlens-application-tooling-ai-agentcontextpack}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack(io.github.testlens.application.tooling.ai.ContractHeader, io.github.testlens.application.tooling.ai.AgentTask, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageContext>, java.util.Map<java.lang.String, java.util.List<java.lang.String>>, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision>, io.github.testlens.application.tooling.ai.AgentContextPack$Completeness)
public io.github.testlens.application.tooling.ai.AgentContextPack(io.github.testlens.application.tooling.ai.ContractHeader, io.github.testlens.application.tooling.ai.AgentTask, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageContext>, java.util.Map<java.lang.String, java.util.List<java.lang.String>>, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.AgentContextPack$SourceContext, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision>, io.github.testlens.application.tooling.ai.AgentContextPack$Completeness)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public io.github.testlens.application.tooling.ai.AgentTask task()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageContext> pages()
public java.util.Map<java.lang.String, java.util.List<java.lang.String>> pageObjectApis()
public java.util.List<java.lang.String> existingTestConventions()
public io.github.testlens.application.tooling.ai.AgentContextPack$SourceContext source()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision> included()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision> excluded()
public io.github.testlens.application.tooling.ai.AgentContextPack$Completeness completeness()
```

## `io.github.testlens.application.tooling.ai.AgentTask$TaskType` {#io-github-testlens-application-tooling-ai-agenttask-tasktype}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentTask$TaskType CREATE_TEST
public static final io.github.testlens.application.tooling.ai.AgentTask$TaskType DESIGN_SCENARIO
public static final io.github.testlens.application.tooling.ai.AgentTask$TaskType VERIFY_TEST
public static final io.github.testlens.application.tooling.ai.AgentTask$TaskType STABILIZE_TEST
public static final io.github.testlens.application.tooling.ai.AgentTask$TaskType REVIEW_TEST
public static io.github.testlens.application.tooling.ai.AgentTask$TaskType[] values()
public static io.github.testlens.application.tooling.ai.AgentTask$TaskType valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentTask` {#io-github-testlens-application-tooling-ai-agenttask}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentTask(io.github.testlens.application.tooling.ai.ContractHeader, io.github.testlens.application.tooling.ai.AgentTask$TaskType, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public io.github.testlens.application.tooling.ai.AgentTask$TaskType type()
public java.lang.String requirement()
public java.util.List<java.lang.String> requiredPageIds()
public java.util.List<java.lang.String> requiredStateIds()
public java.util.List<java.lang.String> requiredElementIds()
public java.util.List<java.lang.String> constraints()
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$Finding` {#io-github-testlens-application-tooling-ai-codereviewresult-finding}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.CodeReviewResult$Finding(io.github.testlens.application.tooling.ai.CodeReviewResult$Severity, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.CodeReviewResult$Severity severity()
public java.lang.String code()
public java.lang.String location()
public java.lang.String detail()
public java.lang.String recommendation()
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$Severity` {#io-github-testlens-application-tooling-ai-codereviewresult-severity}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Severity ERROR
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Severity WARNING
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Severity INFO
public static io.github.testlens.application.tooling.ai.CodeReviewResult$Severity[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$Severity valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict` {#io-github-testlens-application-tooling-ai-codereviewresult-verdict}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict APPROVE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict REQUEST_CHANGES
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict BLOCK
public static io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult` {#io-github-testlens-application-tooling-ai-codereviewresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.CodeReviewResult(io.github.testlens.application.tooling.ai.ContractHeader, java.util.List<io.github.testlens.application.tooling.ai.CodeReviewResult$Finding>)
public io.github.testlens.application.tooling.ai.CodeReviewResult(io.github.testlens.application.tooling.ai.ContractHeader, io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.tooling.ai.CodeReviewResult$Finding>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public io.github.testlens.application.tooling.ai.CodeReviewResult$Verdict verdict()
public java.util.List<java.lang.String> reviewedArtifactRefs()
public java.util.List<io.github.testlens.application.tooling.ai.CodeReviewResult$Finding> findings()
```

## `io.github.testlens.application.tooling.ai.ContextSlicer$Limits` {#io-github-testlens-application-tooling-ai-contextslicer-limits}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.ContextSlicer$Limits(int, int, int, int)
public io.github.testlens.application.tooling.ai.ContextSlicer$Limits(int, int, int, int, int, int, int, int, int, int)
public io.github.testlens.application.tooling.ai.ContextSlicer$Limits(int, int, int, int, int, int, int, int, int, int, int)
public static io.github.testlens.application.tooling.ai.ContextSlicer$Limits defaults()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int maxPages()
public int maxElements()
public int maxTransitions()
public int transitionDepth()
public int maxStates()
public int maxStateElementRefs()
public int maxPageObjectApis()
public int maxConventions()
public int maxStringCharacters()
public int maxTotalCharacters()
public int maxScopeDecisions()
```

## `io.github.testlens.application.tooling.ai.ContextSlicer$SourceLimits` {#io-github-testlens-application-tooling-ai-contextslicer-sourcelimits}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.ContextSlicer$SourceLimits(int, int, int, int, int)
public static io.github.testlens.application.tooling.ai.ContextSlicer$SourceLimits defaults()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int maxClasses()
public int maxMethods()
public int maxDeclarations()
public int maxTests()
public int maxSerializedCharacters()
```

## `io.github.testlens.application.tooling.ai.ContextSlicer` {#io-github-testlens-application-tooling-ai-contextslicer}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.ContextSlicer()
public io.github.testlens.application.tooling.ai.AgentContextPack slice(io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.tooling.ai.AgentTask, java.util.Map<java.lang.String, java.util.List<java.lang.String>>, java.util.List<java.lang.String>, io.github.testlens.core.redaction.RedactionPolicy, io.github.testlens.application.tooling.ai.ContextSlicer$Limits, io.github.testlens.selector.tooling.ExistingProjectIndex, io.github.testlens.application.tooling.source.PageObjectCorrelation, io.github.testlens.application.tooling.ai.ContextSlicer$SourceLimits)
public io.github.testlens.application.tooling.ai.AgentContextPack slice(io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.tooling.ai.AgentTask, java.util.Map<java.lang.String, java.util.List<java.lang.String>>, java.util.List<java.lang.String>, io.github.testlens.core.redaction.RedactionPolicy, io.github.testlens.application.tooling.ai.ContextSlicer$Limits)
```

## `io.github.testlens.application.tooling.ai.ContractHeader$Confidence` {#io-github-testlens-application-tooling-ai-contractheader-confidence}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.ContractHeader$Confidence OBSERVED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Confidence LIVE_VALIDATED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Confidence USER_DECLARED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Confidence INFERRED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Confidence AI_PROPOSED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Confidence UNKNOWN
public static io.github.testlens.application.tooling.ai.ContractHeader$Confidence[] values()
public static io.github.testlens.application.tooling.ai.ContractHeader$Confidence valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.ContractHeader$Status` {#io-github-testlens-application-tooling-ai-contractheader-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.ContractHeader$Status READY
public static final io.github.testlens.application.tooling.ai.ContractHeader$Status PARTIAL
public static final io.github.testlens.application.tooling.ai.ContractHeader$Status BLOCKED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Status COMPLETED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Status FAILED
public static final io.github.testlens.application.tooling.ai.ContractHeader$Status UNKNOWN
public static io.github.testlens.application.tooling.ai.ContractHeader$Status[] values()
public static io.github.testlens.application.tooling.ai.ContractHeader$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.ContractHeader` {#io-github-testlens-application-tooling-ai-contractheader}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.application.tooling.ai.ContractHeader(int, io.github.testlens.application.tooling.ai.ContractHeader$Status, java.util.List<java.lang.String>, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.ContractHeader$Confidence)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public io.github.testlens.application.tooling.ai.ContractHeader$Status status()
public java.util.List<java.lang.String> evidence()
public java.util.List<java.lang.String> limitations()
public io.github.testlens.application.tooling.ai.ContractHeader$Confidence confidence()
```

## `io.github.testlens.application.tooling.ai.EvidenceFailureClassifier$Signals` {#io-github-testlens-application-tooling-ai-evidencefailureclassifier-signals}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.EvidenceFailureClassifier$Signals(boolean, java.util.List<java.lang.String>, boolean, java.util.List<java.lang.String>, boolean, java.util.List<java.lang.String>, boolean, java.util.List<java.lang.String>, java.util.List<java.lang.String>, boolean, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean compileFailed()
public java.util.List<java.lang.String> compileEvidence()
public boolean authenticationFailed()
public java.util.List<java.lang.String> authenticationEvidence()
public boolean productMismatch()
public java.util.List<java.lang.String> productEvidence()
public boolean correlatedSelectorFailure()
public java.util.List<java.lang.String> selectorEvidence()
public java.util.List<java.lang.String> synchronizationEvidence()
public boolean pageObjectCapabilityMissing()
public java.util.List<java.lang.String> capabilityEvidence()
public java.util.List<java.lang.String> affectedIds()
```

## `io.github.testlens.application.tooling.ai.EvidenceFailureClassifier` {#io-github-testlens-application-tooling-ai-evidencefailureclassifier}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.EvidenceFailureClassifier()
public io.github.testlens.application.tooling.ai.FailureClassification classify(io.github.testlens.application.tooling.ai.EvidenceFailureClassifier$Signals)
```

## `io.github.testlens.application.tooling.ai.FailureClassification$Category` {#io-github-testlens-application-tooling-ai-failureclassification-category}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category PRODUCT_DEFECT
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category TEST_LOGIC_DEFECT
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category SELECTOR_INSTABILITY
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category SYNCHRONIZATION
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category TIMING_SYNCHRONIZATION
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category ASSERTION_EXPECTATION
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category TEST_DATA
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category ENVIRONMENT
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category AUTHENTICATION
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category PAGE_MODEL_DRIFT
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category PAGE_OBJECT_CAPABILITY
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category COMPILATION
public static final io.github.testlens.application.tooling.ai.FailureClassification$Category UNKNOWN
public static io.github.testlens.application.tooling.ai.FailureClassification$Category[] values()
public static io.github.testlens.application.tooling.ai.FailureClassification$Category valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.FailureClassification` {#io-github-testlens-application-tooling-ai-failureclassification}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.FailureClassification(io.github.testlens.application.tooling.ai.ContractHeader, io.github.testlens.application.tooling.ai.FailureClassification$Category, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.FailureClassification(io.github.testlens.application.tooling.ai.ContractHeader, io.github.testlens.application.tooling.ai.FailureClassification$Category, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public io.github.testlens.application.tooling.ai.FailureClassification$Category category()
public java.lang.String rootCause()
public java.util.List<java.lang.String> affectedIds()
public java.util.List<java.lang.String> evidenceRefs()
public java.util.List<java.lang.String> counterEvidenceRefs()
```

## `io.github.testlens.application.tooling.ai.PromptPackRenderer$PromptLimitException` {#io-github-testlens-application-tooling-ai-promptpackrenderer-promptlimitexception}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.PromptPackRenderer$PromptLimitException(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.PromptPackRenderer$PromptSecurityException` {#io-github-testlens-application-tooling-ai-promptpackrenderer-promptsecurityexception}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.PromptPackRenderer$PromptSecurityException(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.PromptPackRenderer$Role` {#io-github-testlens-application-tooling-ai-promptpackrenderer-role}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.PromptPackRenderer$Role TEST_ARCHITECT
public static final io.github.testlens.application.tooling.ai.PromptPackRenderer$Role TEST_SCENARIO_DESIGNER
public static final io.github.testlens.application.tooling.ai.PromptPackRenderer$Role TEST_IMPLEMENTER
public static final io.github.testlens.application.tooling.ai.PromptPackRenderer$Role TEST_VERIFIER
public static final io.github.testlens.application.tooling.ai.PromptPackRenderer$Role TEST_STABILIZER
public static final io.github.testlens.application.tooling.ai.PromptPackRenderer$Role TEST_REVIEWER
public static final io.github.testlens.application.tooling.ai.PromptPackRenderer$Role UNIT_TEST_AGENT
public static io.github.testlens.application.tooling.ai.PromptPackRenderer$Role[] values()
public static io.github.testlens.application.tooling.ai.PromptPackRenderer$Role valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.PromptPackRenderer` {#io-github-testlens-application-tooling-ai-promptpackrenderer}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.PromptPackRenderer()
public java.lang.String render(io.github.testlens.application.tooling.ai.PromptPackRenderer$Role, io.github.testlens.application.tooling.ai.AgentContextPack, int)
public java.lang.String render(io.github.testlens.application.tooling.ai.PromptPackRenderer$Role, io.github.testlens.application.tooling.ai.AgentContextPack, int, io.github.testlens.core.redaction.RedactionPolicy, java.util.List<java.lang.String>)
```

## `io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy` {#io-github-testlens-application-tooling-ai-repairproposal-applicationpolicy}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy PROPOSE_ONLY
public static io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy[] values()
public static io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.RepairProposal$SelectorEvidence` {#io-github-testlens-application-tooling-ai-repairproposal-selectorevidence}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.RepairProposal$SelectorEvidence(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String strategy()
public java.lang.String value()
public java.lang.String candidateId()
public java.lang.String validation()
public java.lang.String sameTarget()
public boolean unique()
public java.util.List<java.lang.String> stability()
public java.lang.String source()
```

## `io.github.testlens.application.tooling.ai.RepairProposal$SourceRange` {#io-github-testlens-application-tooling-ai-repairproposal-sourcerange}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.RepairProposal$SourceRange(int, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int startLine()
public int startColumn()
public int endLine()
public int endColumn()
public int startOffset()
public int endOffsetExclusive()
```

## `io.github.testlens.application.tooling.ai.RepairProposal$SourceTarget` {#io-github-testlens-application-tooling-ai-repairproposal-sourcetarget}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.RepairProposal$SourceTarget(java.lang.String, java.lang.String, java.lang.String, io.github.testlens.application.tooling.ai.RepairProposal$SourceRange, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String sourceElementId()
public java.lang.String declarationRef()
public java.lang.String logicalPath()
public io.github.testlens.application.tooling.ai.RepairProposal$SourceRange range()
public java.lang.String sourceFileFingerprint()
public java.lang.String declarationFingerprint()
public java.lang.String correlationState()
public java.lang.String oldStrategy()
public java.lang.String oldValue()
```

## `io.github.testlens.application.tooling.ai.RepairProposal` {#io-github-testlens-application-tooling-ai-repairproposal}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.RepairProposal(io.github.testlens.application.tooling.ai.ContractHeader, java.lang.String, io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.RepairProposal(io.github.testlens.application.tooling.ai.ContractHeader, java.lang.String, io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.RepairProposal(io.github.testlens.application.tooling.ai.ContractHeader, java.lang.String, io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.RepairProposal$SourceTarget, io.github.testlens.application.tooling.ai.RepairProposal$SelectorEvidence, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public java.lang.String proposalId()
public io.github.testlens.application.tooling.ai.RepairProposal$ApplicationPolicy applicationPolicy()
public java.lang.String whatChanged()
public java.lang.String why()
public java.lang.String oldSelector()
public java.lang.String newSelector()
public java.lang.String sourceDeclarationRef()
public java.lang.String oldCandidateId()
public java.lang.String newCandidateId()
public java.lang.String classificationRef()
public java.lang.String driftRef()
public java.util.List<java.lang.String> sameTargetEvidence()
public java.util.List<java.lang.String> stabilityEvidence()
public java.util.List<java.lang.String> affectedTests()
public java.util.List<java.lang.String> risks()
public java.util.List<java.lang.String> verificationPlan()
public io.github.testlens.application.tooling.ai.RepairProposal$SourceTarget sourceTarget()
public io.github.testlens.application.tooling.ai.RepairProposal$SelectorEvidence replacementEvidence()
public java.util.List<java.lang.String> affectedMethods()
```

## `io.github.testlens.application.tooling.ai.SelectorRepairPlanner` {#io-github-testlens-application-tooling-ai-selectorrepairplanner}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.ai.SelectorRepairPlanner()
public io.github.testlens.application.tooling.ai.RepairProposal propose(java.lang.String, io.github.testlens.application.tooling.ai.FailureClassification, io.github.testlens.application.model.ApplicationModel$ElementModel, io.github.testlens.application.model.ApplicationModel$ElementModel, io.github.testlens.application.tooling.source.PageObjectCorrelation$ElementCorrelation, io.github.testlens.application.tooling.source.SourceImpact, java.lang.String, java.lang.String)
public io.github.testlens.application.tooling.ai.RepairProposal propose(java.lang.String, io.github.testlens.application.tooling.ai.FailureClassification, io.github.testlens.application.model.ApplicationModel$ElementModel, io.github.testlens.application.model.ApplicationModel$ElementModel, io.github.testlens.application.tooling.source.PageObjectCorrelation$ElementCorrelation, io.github.testlens.application.tooling.source.SourceImpact, io.github.testlens.selector.tooling.ExistingProjectIndex, java.lang.String, java.lang.String)
```

## `io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome` {#io-github-testlens-application-tooling-ai-testexecutionresult-outcome}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome PASS
public static final io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome FAIL
public static final io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome NOT_RUN
public static final io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome INCONCLUSIVE
public static final io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome TIMED_OUT
public static io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome[] values()
public static io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.TestExecutionResult` {#io-github-testlens-application-tooling-ai-testexecutionresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.TestExecutionResult(io.github.testlens.application.tooling.ai.ContractHeader, java.lang.String, io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome, io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String)
public io.github.testlens.application.tooling.ai.TestExecutionResult(io.github.testlens.application.tooling.ai.ContractHeader, java.lang.String, io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome, io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome, java.lang.String, long, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public java.lang.String scenarioId()
public io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome compileOutcome()
public io.github.testlens.application.tooling.ai.TestExecutionResult$Outcome executionOutcome()
public java.lang.String frameworkResult()
public long durationMillis()
public java.util.List<java.lang.String> compileEvidenceRefs()
public java.util.List<java.lang.String> traceEvidenceRefs()
public java.util.List<java.lang.String> assertionEvidenceRefs()
public java.util.List<java.lang.String> runtimeEventRefs()
public java.util.List<java.lang.String> screenshotRefs()
public java.util.List<java.lang.String> selectorDiagnosticRefs()
public java.lang.String failureSummary()
```

## `io.github.testlens.application.tooling.ai.TestImplementationProposal$PageObjectCapabilityMissing` {#io-github-testlens-application-tooling-ai-testimplementationproposal-pageobjectcapabilitymissing}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.TestImplementationProposal$PageObjectCapabilityMissing(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String elementId()
public java.lang.String requiredCapability()
public java.lang.String reason()
```

## `io.github.testlens.application.tooling.ai.TestImplementationProposal$SelectorAccessPolicy` {#io-github-testlens-application-tooling-ai-testimplementationproposal-selectoraccesspolicy}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.TestImplementationProposal$SelectorAccessPolicy PAGE_OBJECTS_ONLY
public static final io.github.testlens.application.tooling.ai.TestImplementationProposal$SelectorAccessPolicy RAW_SELECTORS_EXPLICITLY_ALLOWED
public static io.github.testlens.application.tooling.ai.TestImplementationProposal$SelectorAccessPolicy[] values()
public static io.github.testlens.application.tooling.ai.TestImplementationProposal$SelectorAccessPolicy valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.TestImplementationProposal` {#io-github-testlens-application-tooling-ai-testimplementationproposal}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.TestImplementationProposal(io.github.testlens.application.tooling.ai.ContractHeader, java.lang.String, java.lang.String, io.github.testlens.application.tooling.ai.TestImplementationProposal$SelectorAccessPolicy, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.tooling.ai.TestImplementationProposal$PageObjectCapabilityMissing>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public java.lang.String scenarioId()
public java.lang.String sourcePatch()
public io.github.testlens.application.tooling.ai.TestImplementationProposal$SelectorAccessPolicy selectorAccessPolicy()
public java.util.List<java.lang.String> pageObjectApisUsed()
public java.util.List<io.github.testlens.application.tooling.ai.TestImplementationProposal$PageObjectCapabilityMissing> missingCapabilities()
```

## `io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition` {#io-github-testlens-application-tooling-ai-testplan-coveragedisposition}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition NEW_TEST_REQUIRED
public static final io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition EXTEND_EXISTING_TEST
public static final io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition ALREADY_COVERED
public static final io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition AMBIGUOUS
public static io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition[] values()
public static io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.TestPlan$ExpectedResult` {#io-github-testlens-application-tooling-ai-testplan-expectedresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.TestPlan$ExpectedResult(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String stateId()
public java.lang.String assertion()
public java.lang.String evidenceRef()
```

## `io.github.testlens.application.tooling.ai.TestPlan$Priority` {#io-github-testlens-application-tooling-ai-testplan-priority}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.TestPlan$Priority CRITICAL
public static final io.github.testlens.application.tooling.ai.TestPlan$Priority HIGH
public static final io.github.testlens.application.tooling.ai.TestPlan$Priority MEDIUM
public static final io.github.testlens.application.tooling.ai.TestPlan$Priority LOW
public static io.github.testlens.application.tooling.ai.TestPlan$Priority[] values()
public static io.github.testlens.application.tooling.ai.TestPlan$Priority valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.TestPlan$TestScenario` {#io-github-testlens-application-tooling-ai-testplan-testscenario}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.TestPlan$TestScenario(java.lang.String, java.lang.String, io.github.testlens.application.tooling.ai.TestPlan$Priority, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.tooling.ai.TestPlan$TestStep>, java.util.List<io.github.testlens.application.tooling.ai.TestPlan$ExpectedResult>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.TestPlan$TestScenario(java.lang.String, java.lang.String, io.github.testlens.application.tooling.ai.TestPlan$Priority, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.tooling.ai.TestPlan$TestStep>, java.util.List<io.github.testlens.application.tooling.ai.TestPlan$ExpectedResult>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String scenarioId()
public java.lang.String title()
public io.github.testlens.application.tooling.ai.TestPlan$Priority priority()
public java.util.List<java.lang.String> preconditions()
public java.util.List<io.github.testlens.application.tooling.ai.TestPlan$TestStep> steps()
public java.util.List<io.github.testlens.application.tooling.ai.TestPlan$ExpectedResult> expected()
public java.util.List<java.lang.String> requiredPageIds()
public java.util.List<java.lang.String> requiredStateIds()
public java.util.List<java.lang.String> requiredElementIds()
public java.util.List<java.lang.String> requiredData()
public java.util.List<java.lang.String> riskAreas()
public io.github.testlens.application.tooling.ai.TestPlan$CoverageDisposition existingCoverage()
public java.util.List<java.lang.String> existingTestRefs()
public java.util.List<java.lang.String> knownUnknowns()
```

## `io.github.testlens.application.tooling.ai.TestPlan$TestStep` {#io-github-testlens-application-tooling-ai-testplan-teststep}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.TestPlan$TestStep(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int order()
public java.lang.String action()
public java.lang.String pageId()
public java.lang.String elementId()
public java.lang.String valueRef()
```

## `io.github.testlens.application.tooling.ai.TestPlan` {#io-github-testlens-application-tooling-ai-testplan}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.TestPlan(io.github.testlens.application.tooling.ai.ContractHeader, java.util.List<io.github.testlens.application.tooling.ai.TestPlan$TestScenario>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public java.util.List<io.github.testlens.application.tooling.ai.TestPlan$TestScenario> scenarios()
```
