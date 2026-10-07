---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.source`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.source.CorrelationOverrides` {#io-github-testlens-application-tooling-source-correlationoverrides}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.source.CorrelationOverrides(java.util.Map<java.lang.String, java.lang.String>, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.application.tooling.source.CorrelationOverrides none()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.Map<java.lang.String, java.lang.String> classToPage()
public java.util.Map<java.lang.String, java.lang.String> sourceElementToApplicationElement()
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelation$ClassCorrelation` {#io-github-testlens-application-tooling-source-pageobjectcorrelation-classcorrelation}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.source.PageObjectCorrelation$ClassCorrelation(java.lang.String, java.lang.String, io.github.testlens.application.tooling.source.PageObjectCorrelation$State, java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String classId()
public io.github.testlens.application.tooling.source.PageObjectCorrelation$State state()
public java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence> evidence()
public java.util.List<java.lang.String> conflicts()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelation$Completeness` {#io-github-testlens-application-tooling-source-pageobjectcorrelation-completeness}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Completeness COMPLETE
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Completeness PARTIAL
public static io.github.testlens.application.tooling.source.PageObjectCorrelation$Completeness[] values()
public static io.github.testlens.application.tooling.source.PageObjectCorrelation$Completeness valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelation$ElementCorrelation` {#io-github-testlens-application-tooling-source-pageobjectcorrelation-elementcorrelation}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.source.PageObjectCorrelation$ElementCorrelation(java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.application.tooling.source.PageObjectCorrelation$State, java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String applicationElementId()
public java.lang.String sourceElementId()
public java.lang.String sourceDeclarationRef()
public io.github.testlens.application.tooling.source.PageObjectCorrelation$State state()
public java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence> evidence()
public java.util.List<java.lang.String> conflicts()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence` {#io-github-testlens-application-tooling-source-pageobjectcorrelation-evidence}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence SAME_CANDIDATE_ID
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence SAME_DECLARATION_PROVENANCE
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence LIVE_SAME_TARGET
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence NORMALIZED_SELECTOR_MATCH
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence SAME_PAGE_IDENTITY
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence SEMANTIC_ROLE_MATCH
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence ACCESSIBLE_NAME_MATCH
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence USER_OVERRIDE
public static io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence[] values()
public static io.github.testlens.application.tooling.source.PageObjectCorrelation$Evidence valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelation$Metrics` {#io-github-testlens-application-tooling-source-pageobjectcorrelation-metrics}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.source.PageObjectCorrelation$Metrics(int, int, int, int, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int pages()
public int applicationElements()
public int sourceElements()
public int exact()
public int strong()
public int probable()
public int ambiguous()
public int noMatch()
public int conflicts()
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelation$State` {#io-github-testlens-application-tooling-source-pageobjectcorrelation-state}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$State EXACT
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$State STRONG
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$State PROBABLE
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$State AMBIGUOUS
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$State NO_MATCH
public static final io.github.testlens.application.tooling.source.PageObjectCorrelation$State CONFLICT
public static io.github.testlens.application.tooling.source.PageObjectCorrelation$State[] values()
public static io.github.testlens.application.tooling.source.PageObjectCorrelation$State valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelation` {#io-github-testlens-application-tooling-source-pageobjectcorrelation}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.application.tooling.source.PageObjectCorrelation(int, java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$ClassCorrelation>, java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$ElementCorrelation>, io.github.testlens.application.tooling.source.PageObjectCorrelation$Completeness, java.util.List<java.lang.String>, io.github.testlens.application.tooling.source.PageObjectCorrelation$Metrics)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$ClassCorrelation> classes()
public java.util.List<io.github.testlens.application.tooling.source.PageObjectCorrelation$ElementCorrelation> elements()
public io.github.testlens.application.tooling.source.PageObjectCorrelation$Completeness completeness()
public java.util.List<java.lang.String> limitations()
public io.github.testlens.application.tooling.source.PageObjectCorrelation$Metrics metrics()
```

## `io.github.testlens.application.tooling.source.PageObjectCorrelator` {#io-github-testlens-application-tooling-source-pageobjectcorrelator}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.source.PageObjectCorrelator()
public io.github.testlens.application.tooling.source.PageObjectCorrelation correlate(io.github.testlens.application.model.ApplicationModel, io.github.testlens.selector.tooling.ExistingProjectIndex, io.github.testlens.application.tooling.source.CorrelationOverrides)
```

## `io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action` {#io-github-testlens-application-tooling-source-pageobjectgenerationplanner-action}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action REUSE_EXISTING
public static final io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action GENERATE
public static final io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action CONFLICT
public static io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action[] values()
public static io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Decision` {#io-github-testlens-application-tooling-source-pageobjectgenerationplanner-decision}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Decision(io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action, java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Action action()
public java.lang.String classId()
public java.lang.String qualifiedName()
public java.util.List<java.lang.String> evidence()
```

## `io.github.testlens.application.tooling.source.PageObjectGenerationPlanner` {#io-github-testlens-application-tooling-source-pageobjectgenerationplanner}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.source.PageObjectGenerationPlanner()
public io.github.testlens.application.tooling.source.PageObjectGenerationPlanner$Decision decide(java.lang.String, io.github.testlens.application.tooling.source.PageObjectCorrelation, io.github.testlens.selector.tooling.ExistingProjectIndex)
```

## `io.github.testlens.application.tooling.source.SourceImpact` {#io-github-testlens-application-tooling-source-sourceimpact}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.source.SourceImpact(java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String applicationElementId()
public java.util.List<java.lang.String> sourceDeclarationRefs()
public java.util.List<java.lang.String> methodIds()
public java.util.List<java.lang.String> testIds()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.application.tooling.source.SourceImpactAnalyzer` {#io-github-testlens-application-tooling-source-sourceimpactanalyzer}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.source`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.source.SourceImpactAnalyzer()
public io.github.testlens.application.tooling.source.SourceImpact analyze(java.lang.String, io.github.testlens.application.tooling.source.PageObjectCorrelation, io.github.testlens.selector.tooling.ExistingProjectIndex, int)
```
