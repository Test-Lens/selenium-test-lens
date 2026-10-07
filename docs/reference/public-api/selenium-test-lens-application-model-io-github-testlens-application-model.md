---
search:
  exclude: true
---

# selenium-test-lens-application-model: `io.github.testlens.application.model`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.model.ApplicationIds` {#io-github-testlens-application-model-applicationids}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static java.lang.String id(java.lang.String, java.lang.String...)
public static java.lang.String semanticToken(java.lang.String)
```

## `io.github.testlens.application.model.ApplicationModel$Action` {#io-github-testlens-application-model-applicationmodel-action}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.model.ApplicationModel$Action CLICK
public static final io.github.testlens.application.model.ApplicationModel$Action TYPE
public static final io.github.testlens.application.model.ApplicationModel$Action CLEAR
public static final io.github.testlens.application.model.ApplicationModel$Action SELECT
public static final io.github.testlens.application.model.ApplicationModel$Action CHECK
public static final io.github.testlens.application.model.ApplicationModel$Action UNCHECK
public static final io.github.testlens.application.model.ApplicationModel$Action UPLOAD
public static final io.github.testlens.application.model.ApplicationModel$Action FOCUS
public static final io.github.testlens.application.model.ApplicationModel$Action OPEN
public static final io.github.testlens.application.model.ApplicationModel$Action ASSERT
public static final io.github.testlens.application.model.ApplicationModel$Action UNKNOWN
public static io.github.testlens.application.model.ApplicationModel$Action[] values()
public static io.github.testlens.application.model.ApplicationModel$Action valueOf(java.lang.String)
```

## `io.github.testlens.application.model.ApplicationModel$Completeness` {#io-github-testlens-application-model-applicationmodel-completeness}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.model.ApplicationModel$Completeness COMPLETE_FOR_OBSERVED_CONTEXT
public static final io.github.testlens.application.model.ApplicationModel$Completeness PARTIAL
public static final io.github.testlens.application.model.ApplicationModel$Completeness FAILED
public static io.github.testlens.application.model.ApplicationModel$Completeness[] values()
public static io.github.testlens.application.model.ApplicationModel$Completeness valueOf(java.lang.String)
```

## `io.github.testlens.application.model.ApplicationModel$Coverage` {#io-github-testlens-application-model-applicationmodel-coverage}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$Coverage(io.github.testlens.application.model.ApplicationModel$Completeness, int, int, int, int, int, int, int, int, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.model.ApplicationModel$Completeness completeness()
public int observations()
public int pages()
public int states()
public int discoveredNodes()
public int analyzedElements()
public int skippedElements()
public int selectorAnalyses()
public int transitions()
public boolean truncated()
```

## `io.github.testlens.application.model.ApplicationModel$ElementModel` {#io-github-testlens-application-model-applicationmodel-elementmodel}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$ElementModel(java.lang.String, java.lang.String, io.github.testlens.application.model.ApplicationModel$ElementType, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.model.ApplicationModel$Action>, io.github.testlens.application.model.ApplicationModel$SelectorProjection, java.util.List<io.github.testlens.application.model.ApplicationModel$SelectorProjection>, io.github.testlens.application.model.ApplicationModel$SelectorQuality, java.lang.String, java.lang.String, io.github.testlens.application.model.ApplicationModel$Provenance, java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String elementId()
public java.lang.String semanticName()
public io.github.testlens.application.model.ApplicationModel$ElementType type()
public java.lang.String semanticRole()
public java.lang.String label()
public java.lang.String accessibleName()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Action> actions()
public io.github.testlens.application.model.ApplicationModel$SelectorProjection preferredSelector()
public java.util.List<io.github.testlens.application.model.ApplicationModel$SelectorProjection> alternativeSelectors()
public io.github.testlens.application.model.ApplicationModel$SelectorQuality selectorQuality()
public java.lang.String regionId()
public java.lang.String fingerprint()
public io.github.testlens.application.model.ApplicationModel$Provenance provenance()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation> limitations()
```

## `io.github.testlens.application.model.ApplicationModel$ElementType` {#io-github-testlens-application-model-applicationmodel-elementtype}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.model.ApplicationModel$ElementType BUTTON
public static final io.github.testlens.application.model.ApplicationModel$ElementType INPUT
public static final io.github.testlens.application.model.ApplicationModel$ElementType TEXTAREA
public static final io.github.testlens.application.model.ApplicationModel$ElementType SELECT
public static final io.github.testlens.application.model.ApplicationModel$ElementType CHECKBOX
public static final io.github.testlens.application.model.ApplicationModel$ElementType RADIO
public static final io.github.testlens.application.model.ApplicationModel$ElementType LINK
public static final io.github.testlens.application.model.ApplicationModel$ElementType FORM
public static final io.github.testlens.application.model.ApplicationModel$ElementType TABLE
public static final io.github.testlens.application.model.ApplicationModel$ElementType TAB
public static final io.github.testlens.application.model.ApplicationModel$ElementType MENU
public static final io.github.testlens.application.model.ApplicationModel$ElementType MENU_ITEM
public static final io.github.testlens.application.model.ApplicationModel$ElementType DIALOG
public static final io.github.testlens.application.model.ApplicationModel$ElementType COMBOBOX
public static final io.github.testlens.application.model.ApplicationModel$ElementType LISTBOX
public static final io.github.testlens.application.model.ApplicationModel$ElementType OPTION
public static final io.github.testlens.application.model.ApplicationModel$ElementType NAVIGATION
public static final io.github.testlens.application.model.ApplicationModel$ElementType SEARCH
public static final io.github.testlens.application.model.ApplicationModel$ElementType CUSTOM
public static final io.github.testlens.application.model.ApplicationModel$ElementType UNKNOWN
public static io.github.testlens.application.model.ApplicationModel$ElementType[] values()
public static io.github.testlens.application.model.ApplicationModel$ElementType valueOf(java.lang.String)
```

## `io.github.testlens.application.model.ApplicationModel$EvidenceSource` {#io-github-testlens-application-model-applicationmodel-evidencesource}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.model.ApplicationModel$EvidenceSource OBSERVED
public static final io.github.testlens.application.model.ApplicationModel$EvidenceSource LIVE_CANDIDATE_ANALYSIS
public static final io.github.testlens.application.model.ApplicationModel$EvidenceSource USER_DECLARED
public static final io.github.testlens.application.model.ApplicationModel$EvidenceSource INFERRED
public static final io.github.testlens.application.model.ApplicationModel$EvidenceSource STATIC_AUDIT
public static final io.github.testlens.application.model.ApplicationModel$EvidenceSource AI_PROPOSED
public static final io.github.testlens.application.model.ApplicationModel$EvidenceSource UNKNOWN
public static io.github.testlens.application.model.ApplicationModel$EvidenceSource[] values()
public static io.github.testlens.application.model.ApplicationModel$EvidenceSource valueOf(java.lang.String)
```

## `io.github.testlens.application.model.ApplicationModel$Limitation` {#io-github-testlens-application-model-applicationmodel-limitation}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$Limitation(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String detail()
```

## `io.github.testlens.application.model.ApplicationModel$PageIdentity` {#io-github-testlens-application-model-applicationmodel-pageidentity}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$PageIdentity(java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.lang.String, io.github.testlens.application.model.ApplicationModel$EvidenceSource)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String normalizedUrlPattern()
public java.lang.String route()
public java.lang.String titlePattern()
public java.util.List<java.lang.String> landmarkElementIds()
public java.lang.String structuralFingerprint()
public io.github.testlens.application.model.ApplicationModel$EvidenceSource source()
```

## `io.github.testlens.application.model.ApplicationModel$PageModel` {#io-github-testlens-application-model-applicationmodel-pagemodel}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$PageModel(java.lang.String, java.lang.String, io.github.testlens.application.model.ApplicationModel$PageIdentity, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.model.ApplicationModel$PageState>, java.util.List<io.github.testlens.application.model.ApplicationModel$Region>, java.util.List<io.github.testlens.application.model.ApplicationModel$ElementModel>, java.util.List<java.lang.String>, java.util.List<io.github.testlens.application.model.ApplicationModel$Transition>, io.github.testlens.application.model.ApplicationModel$Provenance, java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String canonicalName()
public io.github.testlens.application.model.ApplicationModel$PageIdentity identity()
public java.util.List<java.lang.String> titles()
public java.util.List<java.lang.String> fingerprints()
public java.util.List<io.github.testlens.application.model.ApplicationModel$PageState> states()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Region> regions()
public java.util.List<io.github.testlens.application.model.ApplicationModel$ElementModel> elements()
public java.util.List<java.lang.String> sharedComponentRefs()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Transition> transitions()
public io.github.testlens.application.model.ApplicationModel$Provenance provenance()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation> limitations()
```

## `io.github.testlens.application.model.ApplicationModel$PageState` {#io-github-testlens-application-model-applicationmodel-pagestate}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$PageState(java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, io.github.testlens.application.model.ApplicationModel$Provenance)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String stateId()
public java.lang.String semanticName()
public java.util.List<java.lang.String> distinguishingSignals()
public java.util.List<java.lang.String> elementIds()
public io.github.testlens.application.model.ApplicationModel$Provenance provenance()
```

## `io.github.testlens.application.model.ApplicationModel$Provenance` {#io-github-testlens-application-model-applicationmodel-provenance}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$Provenance(io.github.testlens.application.model.ApplicationModel$EvidenceSource, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.Map<java.lang.String, java.lang.String>)
public static io.github.testlens.application.model.ApplicationModel$Provenance observed(java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.model.ApplicationModel$EvidenceSource source()
public java.lang.String observationId()
public java.lang.String contextDescriptor()
public java.util.List<java.lang.String> evidence()
public java.util.Map<java.lang.String, java.lang.String> attributes()
```

## `io.github.testlens.application.model.ApplicationModel$Region` {#io-github-testlens-application-model-applicationmodel-region}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$Region(java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.application.model.ApplicationModel$Provenance)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String regionId()
public java.lang.String semanticName()
public java.lang.String role()
public java.lang.String fingerprint()
public io.github.testlens.application.model.ApplicationModel$Provenance provenance()
```

## `io.github.testlens.application.model.ApplicationModel$SelectorProjection` {#io-github-testlens-application-model-applicationmodel-selectorprojection}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$SelectorProjection(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, boolean, java.util.List<java.lang.String>, int, java.util.List<java.lang.String>, java.util.List<java.lang.String>, io.github.testlens.application.model.ApplicationModel$EvidenceSource)
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
public int rankingPosition()
public java.util.List<java.lang.String> reasons()
public java.util.List<java.lang.String> limitations()
public io.github.testlens.application.model.ApplicationModel$EvidenceSource source()
```

## `io.github.testlens.application.model.ApplicationModel$SelectorQuality` {#io-github-testlens-application-model-applicationmodel-selectorquality}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.model.ApplicationModel$SelectorQuality VERIFIED
public static final io.github.testlens.application.model.ApplicationModel$SelectorQuality REVIEW_REQUIRED
public static final io.github.testlens.application.model.ApplicationModel$SelectorQuality UNAVAILABLE
public static io.github.testlens.application.model.ApplicationModel$SelectorQuality[] values()
public static io.github.testlens.application.model.ApplicationModel$SelectorQuality valueOf(java.lang.String)
```

## `io.github.testlens.application.model.ApplicationModel$SharedComponent` {#io-github-testlens-application-model-applicationmodel-sharedcomponent}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$SharedComponent(java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, io.github.testlens.application.model.ApplicationModel$EvidenceSource, io.github.testlens.application.model.ApplicationModel$Provenance)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String componentId()
public java.lang.String canonicalName()
public java.util.List<java.lang.String> pageIds()
public java.util.List<java.lang.String> elementIds()
public java.util.List<java.lang.String> fingerprints()
public io.github.testlens.application.model.ApplicationModel$EvidenceSource source()
public io.github.testlens.application.model.ApplicationModel$Provenance provenance()
```

## `io.github.testlens.application.model.ApplicationModel$Transition` {#io-github-testlens-application-model-applicationmodel-transition}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.application.model.ApplicationModel$Transition(java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.application.model.ApplicationModel$Action, java.lang.String, java.lang.String, io.github.testlens.application.model.ApplicationModel$TransitionConfidence, io.github.testlens.application.model.ApplicationModel$Provenance)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String transitionId()
public java.lang.String sourcePageId()
public java.lang.String sourceStateId()
public java.lang.String elementId()
public io.github.testlens.application.model.ApplicationModel$Action action()
public java.lang.String targetPageId()
public java.lang.String targetStateId()
public io.github.testlens.application.model.ApplicationModel$TransitionConfidence confidence()
public io.github.testlens.application.model.ApplicationModel$Provenance provenance()
```

## `io.github.testlens.application.model.ApplicationModel$TransitionConfidence` {#io-github-testlens-application-model-applicationmodel-transitionconfidence}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.application.model.ApplicationModel$TransitionConfidence OBSERVED
public static final io.github.testlens.application.model.ApplicationModel$TransitionConfidence USER_DECLARED
public static final io.github.testlens.application.model.ApplicationModel$TransitionConfidence INFERRED
public static final io.github.testlens.application.model.ApplicationModel$TransitionConfidence UNKNOWN
public static io.github.testlens.application.model.ApplicationModel$TransitionConfidence[] values()
public static io.github.testlens.application.model.ApplicationModel$TransitionConfidence valueOf(java.lang.String)
```

## `io.github.testlens.application.model.ApplicationModel` {#io-github-testlens-application-model-applicationmodel}

- Artifact/module: `selenium-test-lens-application-model`
- Package: `io.github.testlens.application.model`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.application.model.ApplicationModel(int, java.lang.String, java.lang.String, java.lang.String, java.time.Instant, java.util.List<io.github.testlens.application.model.ApplicationModel$PageModel>, java.util.List<io.github.testlens.application.model.ApplicationModel$SharedComponent>, java.util.List<io.github.testlens.application.model.ApplicationModel$Transition>, io.github.testlens.application.model.ApplicationModel$Coverage, java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation>, io.github.testlens.application.model.ApplicationModel$Provenance)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String applicationId()
public java.lang.String applicationName()
public java.lang.String generatorVersion()
public java.time.Instant generatedAt()
public java.util.List<io.github.testlens.application.model.ApplicationModel$PageModel> pages()
public java.util.List<io.github.testlens.application.model.ApplicationModel$SharedComponent> sharedComponents()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Transition> transitions()
public io.github.testlens.application.model.ApplicationModel$Coverage coverage()
public java.util.List<io.github.testlens.application.model.ApplicationModel$Limitation> limitations()
public io.github.testlens.application.model.ApplicationModel$Provenance provenance()
```
