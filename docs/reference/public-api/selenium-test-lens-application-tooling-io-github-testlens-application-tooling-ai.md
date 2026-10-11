---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.ai`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.ai.AgentContextPack$CapabilityParameter` {#io-github-testlens-application-tooling-ai-agentcontextpack-capabilityparameter}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$CapabilityParameter(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String name()
public java.lang.String type()
```

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

## `io.github.testlens.application.tooling.ai.AgentContextPack$ConstructorCapability` {#io-github-testlens-application-tooling-ai-agentcontextpack-constructorcapability}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$ConstructorCapability(java.lang.String, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$CapabilityParameter>, java.lang.String, java.lang.String, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String signature()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$CapabilityParameter> parameters()
public java.lang.String accessibility()
public java.lang.String acquisition()
public boolean implicit()
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

## `io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness` {#io-github-testlens-application-tooling-ai-agentcontextpack-manifestcompleteness}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness COMPLETE
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness PARTIAL
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness[] values()
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus` {#io-github-testlens-application-tooling-ai-agentcontextpack-manifestconstructionstatus}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus VERIFIED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus UNRESOLVED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus NOT_REQUIRED
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus[] values()
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus` {#io-github-testlens-application-tooling-ai-agentcontextpack-manifestcorrelationstatus}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus EXACT
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus STRONG
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus MIXED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus INSUFFICIENT
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus[] values()
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus` {#io-github-testlens-application-tooling-ai-agentcontextpack-manifestinheritedapistatus}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus VERIFIED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus UNRESOLVED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus NOT_APPLICABLE
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus[] values()
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage` {#io-github-testlens-application-tooling-ai-agentcontextpack-manifestoriginstage}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage SOURCE_CONTEXT_SLICE
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage RESTORED_SNAPSHOT
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage UNSPECIFIED
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage[] values()
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason` {#io-github-testlens-application-tooling-ai-agentcontextpack-manifestpartialreason}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason SOURCE_INDEX_INCOMPLETE
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason REQUIRED_CLASS_MISSING
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason REQUIRED_API_UNRESOLVED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason CONSTRUCTION_UNRESOLVED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason CORRELATION_INSUFFICIENT
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason MANIFEST_TRUNCATED
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason FINGERPRINT_MISMATCH
public static final io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason MANIFEST_STALE
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason[] values()
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance` {#io-github-testlens-application-tooling-ai-agentcontextpack-manifestprovenance}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance(java.lang.String, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage, java.lang.String, int, int, int, int, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus, boolean, int, int, int, boolean, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason>)
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance unspecified(java.lang.String, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness)
public static io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance empty()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String indexFingerprint()
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestOriginStage originStage()
public java.lang.String sourceIndexCompleteness()
public int selectedPageObjectCount()
public int verifiedClassCount()
public int verifiedMethodCount()
public int unresolvedClassCount()
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestConstructionStatus constructionStatus()
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestInheritedApiStatus inheritedApiStatus()
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCorrelationStatus correlationStatus()
public boolean truncated()
public int maxClasses()
public int maxMethods()
public int maxSerializedCharacters()
public boolean sourcesChanged()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ManifestPartialReason> partialReasons()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$MethodCapability` {#io-github-testlens-application-tooling-ai-agentcontextpack-methodcapability}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$MethodCapability(java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$CapabilityParameter>, java.lang.String, boolean, java.lang.String, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.AgentContextPack$MethodCapability(java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$CapabilityParameter>, java.lang.String, boolean, java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String methodId()
public java.lang.String name()
public java.lang.String signature()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$CapabilityParameter> parameters()
public java.lang.String returnType()
public boolean staticMethod()
public java.lang.String declaringType()
public java.lang.String classification()
public java.util.List<java.lang.String> applicationElementIds()
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

## `io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapability` {#io-github-testlens-application-tooling-ai-agentcontextpack-pageobjectcapability}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapability(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ConstructorCapability>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$MethodCapability>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String classId()
public java.lang.String pageId()
public java.lang.String qualifiedName()
public java.lang.String logicalPath()
public java.lang.String accessibility()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ConstructorCapability> constructors()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$MethodCapability> methods()
```

## `io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest` {#io-github-testlens-application-tooling-ai-agentcontextpack-pageobjectcapabilitymanifest}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest(java.lang.String, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapability>, java.util.List<java.lang.String>)
public io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest(java.lang.String, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapability>, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance)
public static io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest empty()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String sourceFingerprint()
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestCompleteness completeness()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapability> classes()
public java.util.List<java.lang.String> limitations()
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance provenance()
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
public io.github.testlens.application.tooling.ai.AgentContextPack(io.github.testlens.application.tooling.ai.ContractHeader, io.github.testlens.application.tooling.ai.AgentTask, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageContext>, java.util.Map<java.lang.String, java.util.List<java.lang.String>>, java.util.List<java.lang.String>, io.github.testlens.application.tooling.ai.AgentContextPack$SourceContext, io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision>, java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$ScopeDecision>, io.github.testlens.application.tooling.ai.AgentContextPack$Completeness)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.ContractHeader header()
public io.github.testlens.application.tooling.ai.AgentTask task()
public java.util.List<io.github.testlens.application.tooling.ai.AgentContextPack$PageContext> pages()
public java.util.Map<java.lang.String, java.util.List<java.lang.String>> pageObjectApis()
public java.util.List<java.lang.String> existingTestConventions()
public io.github.testlens.application.tooling.ai.AgentContextPack$SourceContext source()
public io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest pageObjectCapabilities()
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

## `io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category` {#io-github-testlens-application-tooling-ai-canonicalpageobjectapireference-category}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category VALID
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category INVALID_REFERENCE_STRUCTURE
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category METHOD_SEPARATOR_MISMATCH
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category PARAMETER_TYPE_FORMAT
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category METHOD_MISSING
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category ARITY_MISMATCH
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category AMBIGUOUS_OVERLOAD
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category DECLARING_CLASS_MISMATCH
public static final io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category UNTRUSTED_REFERENCE
public static io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category[] values()
public static io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Resolution` {#io-github-testlens-application-tooling-ai-canonicalpageobjectapireference-resolution}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Resolution(boolean, io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category, io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean valid()
public io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Category category()
public io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference reference()
```

## `io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference` {#io-github-testlens-application-tooling-ai-canonicalpageobjectapireference}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference(java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public java.lang.String render()
public static io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference parse(java.lang.String)
public static io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference of(java.lang.String, io.github.testlens.application.tooling.ai.AgentContextPack$MethodCapability)
public static io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference of(java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public static java.util.List<io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference> allowed(io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest)
public static io.github.testlens.application.tooling.ai.CanonicalPageObjectApiReference$Resolution resolve(java.lang.String, io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String owner()
public java.lang.String method()
public java.util.List<java.lang.String> parameterTypes()
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$Category` {#io-github-testlens-application-tooling-ai-codereviewresult-category}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category ASSERTION_INSUFFICIENT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category REQUIREMENT_NOT_COVERED
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category PAGE_OBJECT_MISUSE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category TEST_DATA_INVALID
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category TEST_LIFECYCLE_PROBLEM
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category EXECUTION_EVIDENCE_INSUFFICIENT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category UNSAFE_IMPLEMENTATION
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$Category REVIEW_UNSUBSTANTIATED
public static io.github.testlens.application.tooling.ai.CodeReviewResult$Category[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$Category valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability` {#io-github-testlens-application-tooling-ai-codereviewresult-evidenceavailability}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability VERIFIED
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability PARTIAL
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability UNAVAILABLE
public static io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$Finding` {#io-github-testlens-application-tooling-ai-codereviewresult-finding}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.CodeReviewResult$Finding(io.github.testlens.application.tooling.ai.CodeReviewResult$Category, io.github.testlens.application.tooling.ai.CodeReviewResult$Severity, java.lang.String, java.lang.String, io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory, io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction, io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation, io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability)
public io.github.testlens.application.tooling.ai.CodeReviewResult$Finding(io.github.testlens.application.tooling.ai.CodeReviewResult$Severity, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public java.lang.String code()
public java.lang.String location()
public java.lang.String detail()
public java.lang.String recommendation()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.CodeReviewResult$Category category()
public io.github.testlens.application.tooling.ai.CodeReviewResult$Severity severity()
public java.lang.String ruleId()
public java.lang.String artifactRef()
public io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory problemCategory()
public io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction requiredAction()
public io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation safeLocation()
public io.github.testlens.application.tooling.ai.CodeReviewResult$EvidenceAvailability evidenceAvailability()
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory` {#io-github-testlens-application-tooling-ai-codereviewresult-problemcategory}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory ASSERTION
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory REQUIREMENT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory PAGE_OBJECT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory TEST_DATA
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory LIFECYCLE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory EVIDENCE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory SAFETY
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory REVIEW_CONTRACT
public static io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$ProblemCategory valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction` {#io-github-testlens-application-tooling-ai-codereviewresult-requiredaction}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction ADD_ASSERTION
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction COVER_REQUIREMENT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction USE_VERIFIED_PAGE_OBJECT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction FIX_TEST_DATA
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction FIX_TEST_LIFECYCLE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction PROVIDE_EXECUTION_EVIDENCE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction REMOVE_UNSAFE_IMPLEMENTATION
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction HUMAN_REVIEW
public static io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$RequiredAction valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId` {#io-github-testlens-application-tooling-ai-codereviewresult-ruleid}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId ASSERTION_REQUIRED
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId REQUIREMENT_COVERAGE_REQUIRED
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId VERIFIED_PAGE_OBJECT_REQUIRED
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId PAGE_OBJECT_CONTRACT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId TEST_DATA_CONTRACT
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId LIFECYCLE_INTEGRITY
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId EXECUTION_EVIDENCE_REQUIRED
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId UNSAFE_CODE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId REVIEW_EVIDENCE_BINDING
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId ARTIFACT_REFERENCE_INVALID
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId EVIDENCE_UNAVAILABLE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId EVIDENCE_DIGEST_MISMATCH
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId EVIDENCE_LINEAGE_INVALID
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId IMPLEMENTATION_FINGERPRINT_MISMATCH
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId FEEDBACK_UNSUBSTANTIATED
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId DECISION_CONTRADICTORY
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId REVIEW_CONTRACT_INVALID
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId LEGACY_REVIEW_FINDING
public static io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$RuleId valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation` {#io-github-testlens-application-tooling-ai-codereviewresult-safelocation}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation GENERATED_TEST
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation TEST_METHOD
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation ASSERTION
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation PAGE_OBJECT_CALL
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation TEST_DATA
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation LIFECYCLE
public static final io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation EXECUTION_EVIDENCE
public static io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation[] values()
public static io.github.testlens.application.tooling.ai.CodeReviewResult$SafeLocation valueOf(java.lang.String)
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
public static java.util.List<java.lang.String> allowedRuleIds()
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

## `io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Assessment` {#io-github-testlens-application-tooling-ai-pageobjectmanifestreadiness-assessment}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Assessment(io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status, io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance)
public boolean ready()
public java.lang.String reasonCode()
public java.util.Map<java.lang.String, java.lang.String> safeMetadata()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status status()
public io.github.testlens.application.tooling.ai.AgentContextPack$ManifestProvenance provenance()
```

## `io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status` {#io-github-testlens-application-tooling-ai-pageobjectmanifestreadiness-status}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status READY
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status SOURCE_INDEX_INCOMPLETE
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status REQUIRED_CLASS_MISSING
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status REQUIRED_API_UNRESOLVED
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status CONSTRUCTION_UNRESOLVED
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status CORRELATION_INSUFFICIENT
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status MANIFEST_TRUNCATED
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status FINGERPRINT_MISMATCH
public static final io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status MANIFEST_STALE
public static io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status[] values()
public static io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.ai.PageObjectManifestReadiness` {#io-github-testlens-application-tooling-ai-pageobjectmanifestreadiness}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.ai`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static final java.util.Set<java.lang.String> REJECTION_REASON_CODES
public static java.lang.String reasonCode(io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Status)
public static io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Assessment assess(io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest, java.lang.String)
public static io.github.testlens.application.tooling.ai.PageObjectManifestReadiness$Assessment assess(io.github.testlens.application.tooling.ai.AgentContextPack$PageObjectCapabilityManifest, java.lang.String, java.util.Set<java.lang.String>)
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
