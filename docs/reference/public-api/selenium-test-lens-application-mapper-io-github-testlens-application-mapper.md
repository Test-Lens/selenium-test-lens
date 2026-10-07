---
search:
  exclude: true
---

# selenium-test-lens-application-mapper: `io.github.testlens.application.mapper`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.mapper.ApplicationMapper` {#io-github-testlens-application-mapper-applicationmapper}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static io.github.testlens.application.mapper.ApplicationMapper start(org.openqa.selenium.WebDriver, io.github.testlens.application.mapper.ApplicationMapperOptions)
public io.github.testlens.application.mapper.MappingObservation observe()
public void beginTransition(java.lang.String, io.github.testlens.application.model.ApplicationModel$Action)
public int safeExplore()
public io.github.testlens.application.model.ApplicationModel model()
public io.github.testlens.application.mapper.MappingMetrics metrics()
```

## `io.github.testlens.application.mapper.ApplicationMapperOptions$ActionDecision` {#io-github-testlens-application-mapper-applicationmapperoptions-actiondecision}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.mapper.ApplicationMapperOptions$ActionDecision ALLOW
public static final io.github.testlens.application.mapper.ApplicationMapperOptions$ActionDecision DENY
public static final io.github.testlens.application.mapper.ApplicationMapperOptions$ActionDecision REQUIRE_EXPLICIT_APPROVAL
public static io.github.testlens.application.mapper.ApplicationMapperOptions$ActionDecision[] values()
public static io.github.testlens.application.mapper.ApplicationMapperOptions$ActionDecision valueOf(java.lang.String)
```

## `io.github.testlens.application.mapper.ApplicationMapperOptions$ActionPolicy` {#io-github-testlens-application-mapper-applicationmapperoptions-actionpolicy}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `interface`

```java
public abstract io.github.testlens.application.mapper.ApplicationMapperOptions$ActionDecision evaluate(io.github.testlens.application.mapper.ApplicationMapperOptions$SafeAction)
```

## `io.github.testlens.application.mapper.ApplicationMapperOptions$Builder` {#io-github-testlens-application-mapper-applicationmapperoptions-builder}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder mode(io.github.testlens.application.mapper.ApplicationMapperOptions$Mode)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxDiscoveredNodes(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxActionableElements(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxCandidateAnalyses(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxRegions(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxShadowDepth(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxPages(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxPageStates(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxTransitions(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxCrawlDepth(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder maxExploreActions(int)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder preferredTestAttributes(java.util.List<java.lang.String>)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder redactionPolicy(io.github.testlens.core.redaction.RedactionPolicy)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder selectorPolicies(io.github.testlens.selector.engine.CompiledPolicySet)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder selectorEvidence(io.github.testlens.selector.engine.ObservationEvidence)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder overrides(io.github.testlens.application.mapper.ApplicationOverrides)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder actionPolicy(io.github.testlens.application.mapper.ApplicationMapperOptions$ActionPolicy)
public io.github.testlens.application.mapper.ApplicationMapperOptions$Builder clock(java.time.Clock)
public io.github.testlens.application.mapper.ApplicationMapperOptions build()
```

## `io.github.testlens.application.mapper.ApplicationMapperOptions$Mode` {#io-github-testlens-application-mapper-applicationmapperoptions-mode}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.mapper.ApplicationMapperOptions$Mode CURRENT_PAGE
public static final io.github.testlens.application.mapper.ApplicationMapperOptions$Mode GUIDED
public static final io.github.testlens.application.mapper.ApplicationMapperOptions$Mode SAFE_EXPLORE
public static io.github.testlens.application.mapper.ApplicationMapperOptions$Mode[] values()
public static io.github.testlens.application.mapper.ApplicationMapperOptions$Mode valueOf(java.lang.String)
```

## `io.github.testlens.application.mapper.ApplicationMapperOptions$SafeAction` {#io-github-testlens-application-mapper-applicationmapperoptions-safeaction}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.mapper.ApplicationMapperOptions$SafeAction(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String elementId()
public java.lang.String semanticName()
public java.lang.String elementType()
public java.lang.String targetUrl()
```

## `io.github.testlens.application.mapper.ApplicationMapperOptions` {#io-github-testlens-application-mapper-applicationmapperoptions}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static io.github.testlens.application.mapper.ApplicationMapperOptions$Builder builder(java.lang.String)
public java.lang.String applicationName()
public io.github.testlens.application.mapper.ApplicationMapperOptions$Mode mode()
public int maxDiscoveredNodes()
public int maxActionableElements()
public int maxCandidateAnalyses()
public int maxRegions()
public int maxShadowDepth()
public int maxPages()
public int maxPageStates()
public int maxTransitions()
public int maxCrawlDepth()
public int maxExploreActions()
public java.util.List<java.lang.String> preferredTestAttributes()
public io.github.testlens.core.redaction.RedactionPolicy redactionPolicy()
public io.github.testlens.selector.engine.CompiledPolicySet selectorPolicies()
public io.github.testlens.selector.engine.ObservationEvidence selectorEvidence()
public io.github.testlens.application.mapper.ApplicationOverrides overrides()
public io.github.testlens.application.mapper.ApplicationMapperOptions$ActionPolicy actionPolicy()
public java.time.Clock clock()
```

## `io.github.testlens.application.mapper.ApplicationOverrides` {#io-github-testlens-application-mapper-applicationoverrides}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.application.mapper.ApplicationOverrides(int, java.util.Map<java.lang.String, java.lang.String>, java.util.Map<java.lang.String, java.lang.String>, java.util.Set<java.lang.String>, java.util.Set<java.lang.String>, java.util.Map<java.lang.String, java.lang.String>, java.util.Map<java.lang.String, java.lang.String>)
public io.github.testlens.application.mapper.ApplicationOverrides(int, java.util.Map<java.lang.String, java.lang.String>, java.util.Map<java.lang.String, java.lang.String>, java.util.Set<java.lang.String>, java.util.Set<java.lang.String>)
public io.github.testlens.application.mapper.ApplicationOverrides(int, java.util.Map<java.lang.String, java.lang.String>, java.util.Map<java.lang.String, java.lang.String>, java.util.Set<java.lang.String>, java.util.Set<java.lang.String>, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.application.mapper.ApplicationOverrides empty()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.util.Map<java.lang.String, java.lang.String> pageNamesByUrlPattern()
public java.util.Map<java.lang.String, java.lang.String> elementNamesByFingerprint()
public java.util.Set<java.lang.String> sharedRegionFingerprints()
public java.util.Set<java.lang.String> deniedUrlPatterns()
public java.util.Map<java.lang.String, java.lang.String> pageIdentityGroupsByObservationFingerprint()
public java.util.Map<java.lang.String, java.lang.String> pageNamesByIdentityGroup()
```

## `io.github.testlens.application.mapper.MappingException$Code` {#io-github-testlens-application-mapper-mappingexception-code}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.mapper.MappingException$Code SCAN_LIMIT_REACHED
public static final io.github.testlens.application.mapper.MappingException$Code PAGE_IDENTITY_AMBIGUOUS
public static final io.github.testlens.application.mapper.MappingException$Code TARGET_STALE
public static final io.github.testlens.application.mapper.MappingException$Code UNSUPPORTED_CLOSED_SHADOW
public static final io.github.testlens.application.mapper.MappingException$Code SELECTOR_REVIEW_REQUIRED
public static final io.github.testlens.application.mapper.MappingException$Code MODEL_SERIALIZATION_FAILED
public static final io.github.testlens.application.mapper.MappingException$Code GENERATION_CONFLICT
public static final io.github.testlens.application.mapper.MappingException$Code EXISTING_SOURCE_CONFLICT
public static final io.github.testlens.application.mapper.MappingException$Code CRAWL_ACTION_BLOCKED
public static final io.github.testlens.application.mapper.MappingException$Code BROWSER_SCRIPT_FAILED
public static final io.github.testlens.application.mapper.MappingException$Code PAGE_STATE_UNRETAINED
public static io.github.testlens.application.mapper.MappingException$Code[] values()
public static io.github.testlens.application.mapper.MappingException$Code valueOf(java.lang.String)
```

## `io.github.testlens.application.mapper.MappingException` {#io-github-testlens-application-mapper-mappingexception}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.application.mapper.MappingException(io.github.testlens.application.mapper.MappingException$Code, java.lang.String)
public io.github.testlens.application.mapper.MappingException(io.github.testlens.application.mapper.MappingException$Code, java.lang.String, java.lang.Throwable)
public io.github.testlens.application.mapper.MappingException$Code code()
```

## `io.github.testlens.application.mapper.MappingMetrics` {#io-github-testlens-application-mapper-mappingmetrics}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.mapper.MappingMetrics(long, long, long, int, int, int, int, int, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public long discoveryNanos()
public long selectorAnalysisNanos()
public long mergeNanos()
public int discoveryScriptCalls()
public int seleniumCommands()
public int observations()
public int discoveredNodes()
public int analyzedElements()
public int skippedElements()
public int selectorAnalyses()
public int pages()
public int states()
public int transitions()
```

## `io.github.testlens.application.mapper.MappingObservation` {#io-github-testlens-application-mapper-mappingobservation}

- Artifact/module: `selenium-test-lens-application-mapper`
- Package: `io.github.testlens.application.mapper`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.mapper.MappingObservation(java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, io.github.testlens.application.model.ApplicationModel$Completeness, java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation>, io.github.testlens.application.mapper.MappingMetrics)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String observationId()
public java.lang.String pageId()
public java.lang.String stateId()
public java.util.List<java.lang.String> elementIds()
public io.github.testlens.application.model.ApplicationModel$Completeness completeness()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation> limitations()
public io.github.testlens.application.mapper.MappingMetrics metrics()
```
