---
search:
  exclude: true
---

# selenium-test-lens-selector-engine: `io.github.testlens.selector.engine`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selector.engine.AppearanceClassifier` {#io-github-testlens-selector-engine-appearanceclassifier}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int DETECTOR_CATALOG_VERSION
public io.github.testlens.selector.engine.AppearanceClassifier()
public java.util.List<io.github.testlens.selector.engine.AppearanceSignal> classify(io.github.testlens.selector.engine.SelectorSubject)
```

## `io.github.testlens.selector.engine.AppearanceSignal$Confidence` {#io-github-testlens-selector-engine-appearancesignal-confidence}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.AppearanceSignal$Confidence LOW
public static final io.github.testlens.selector.engine.AppearanceSignal$Confidence MEDIUM
public static final io.github.testlens.selector.engine.AppearanceSignal$Confidence HIGH
public static io.github.testlens.selector.engine.AppearanceSignal$Confidence[] values()
public static io.github.testlens.selector.engine.AppearanceSignal$Confidence valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.AppearanceSignal$Family` {#io-github-testlens-selector-engine-appearancesignal-family}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.AppearanceSignal$Family UUID_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family EPOCH_TIMESTAMP_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family LONG_NUMERIC_SEQUENCE_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family HEX_HASH_FRAGMENT_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family HIGH_ENTROPY_SUFFIX_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family CSS_IN_JS_TOKEN_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family CSS_MODULE_TOKEN_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family FRAMEWORK_COUNTER_ID_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family REACT_USEID_LIKE
public static final io.github.testlens.selector.engine.AppearanceSignal$Family STABLE_PREFIX_SUSPICIOUS_SUFFIX
public static io.github.testlens.selector.engine.AppearanceSignal$Family[] values()
public static io.github.testlens.selector.engine.AppearanceSignal$Family valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.AppearanceSignal` {#io-github-testlens-selector-engine-appearancesignal}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.AppearanceSignal(io.github.testlens.selector.engine.AppearanceSignal$Family, io.github.testlens.selector.engine.AppearanceSignal$Confidence, java.lang.String, int, java.lang.String, int, int, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.AppearanceSignal$Family family()
public io.github.testlens.selector.engine.AppearanceSignal$Confidence confidence()
public java.lang.String detector()
public int detectorVersion()
public java.lang.String subjectPart()
public int startCodePoint()
public int endCodePoint()
public java.lang.String fragmentDigest()
public java.lang.String reasonCode()
public java.lang.String explanation()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Candidate` {#io-github-testlens-selector-engine-candidateanalysis-candidate}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$Candidate(java.lang.String, io.github.testlens.selector.engine.CandidateAnalysis$Locator, boolean, java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Origin>, java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$StabilityComponent>, io.github.testlens.selector.engine.CandidateAnalysis$Validation, io.github.testlens.selector.engine.CandidateAnalysis$Complexity, java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Reason>, java.util.List<java.lang.String>, boolean)
public io.github.testlens.selector.engine.CandidateAnalysis$Candidate withValidation(io.github.testlens.selector.engine.CandidateAnalysis$Validation)
public io.github.testlens.selector.engine.CandidateAnalysis$Candidate withReasons(java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Reason>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String candidateId()
public io.github.testlens.selector.engine.CandidateAnalysis$Locator locator()
public boolean opaqueOriginal()
public java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Origin> origins()
public java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$StabilityComponent> stabilityComponents()
public io.github.testlens.selector.engine.CandidateAnalysis$Validation validation()
public io.github.testlens.selector.engine.CandidateAnalysis$Complexity complexity()
public java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Reason> reasons()
public java.util.List<java.lang.String> limitations()
public boolean original()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Completeness` {#io-github-testlens-selector-engine-candidateanalysis-completeness}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$Completeness(boolean, boolean, boolean, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean targetAvailable()
public boolean metadataTruncated()
public boolean candidateLimitReached()
public int instrumentationNodesExcluded()
public int generatedBeforeDedup()
public int candidatesAfterDedup()
public int validationCommands()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Complexity` {#io-github-testlens-selector-engine-candidateanalysis-complexity}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$Complexity(io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility, io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility scopeFragility()
public io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference semanticPreference()
public int ancestorDepth()
public int combinators()
public int locatorCodePoints()
public int componentCount()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Issue` {#io-github-testlens-selector-engine-candidateanalysis-issue}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$Issue(java.lang.String, java.lang.String, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String detail()
public boolean fatal()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Locator` {#io-github-testlens-selector-engine-candidateanalysis-locator}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$Locator(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String strategy()
public java.lang.String value()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Origin` {#io-github-testlens-selector-engine-candidateanalysis-origin}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin ORIGINAL
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin ID
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin TEST_ATTRIBUTE
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin NAME
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin CLASS_TOKEN
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin TAG_CLASS
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin TAG
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin LINK_TEXT
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin TEXT_XPATH
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin STABLE_ANCESTOR
public static final io.github.testlens.selector.engine.CandidateAnalysis$Origin CUSTOM
public static io.github.testlens.selector.engine.CandidateAnalysis$Origin[] values()
public static io.github.testlens.selector.engine.CandidateAnalysis$Origin valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Reason` {#io-github-testlens-selector-engine-candidateanalysis-reason}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$Reason(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String message()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Recommendation` {#io-github-testlens-selector-engine-candidateanalysis-recommendation}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CandidateAnalysis$Recommendation KEEP_CURRENT
public static final io.github.testlens.selector.engine.CandidateAnalysis$Recommendation CONSIDER_REPLACEMENT
public static final io.github.testlens.selector.engine.CandidateAnalysis$Recommendation REVIEW_REQUIRED
public static final io.github.testlens.selector.engine.CandidateAnalysis$Recommendation NO_VALID_CANDIDATE
public static final io.github.testlens.selector.engine.CandidateAnalysis$Recommendation TARGET_REQUIRED
public static io.github.testlens.selector.engine.CandidateAnalysis$Recommendation[] values()
public static io.github.testlens.selector.engine.CandidateAnalysis$Recommendation valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility` {#io-github-testlens-selector-engine-candidateanalysis-scopefragility}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility DIRECT
public static final io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility SIMPLE_COMPOUND
public static final io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility ANCESTOR_COMPOUND
public static final io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility TEXT_XPATH
public static io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility[] values()
public static io.github.testlens.selector.engine.CandidateAnalysis$ScopeFragility valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference` {#io-github-testlens-selector-engine-candidateanalysis-semanticpreference}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference PREFERRED_TEST_ATTRIBUTE
public static final io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference ID
public static final io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference NAME
public static final io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference CLASS_OR_TAG_CLASS
public static final io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference LINK_TEXT
public static final io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference ARIA_ATTRIBUTE
public static final io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference GENERIC_TEXT_OR_TAG
public static io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference[] values()
public static io.github.testlens.selector.engine.CandidateAnalysis$SemanticPreference valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateAnalysis$StabilityComponent` {#io-github-testlens-selector-engine-candidateanalysis-stabilitycomponent}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$StabilityComponent(java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.engine.StabilityAssessment)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String role()
public java.lang.String strategy()
public java.lang.String value()
public io.github.testlens.selector.engine.StabilityAssessment assessment()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison` {#io-github-testlens-selector-engine-candidateanalysis-targetcomparison}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison SAME_TARGET
public static final io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison DIFFERENT_TARGET
public static final io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison TARGET_UNAVAILABLE
public static final io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison STALE_TARGET
public static final io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison UNKNOWN
public static io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison[] values()
public static io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent` {#io-github-testlens-selector-engine-candidateanalysis-usageintent}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent FIND_ONE
public static final io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent FIND_MANY
public static final io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent UNKNOWN
public static io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent[] values()
public static io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateAnalysis$Validation` {#io-github-testlens-selector-engine-candidateanalysis-validation}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateAnalysis$Validation(io.github.testlens.selector.engine.CandidateAnalysis$ValidationState, int, io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison, java.util.List<java.lang.String>)
public static io.github.testlens.selector.engine.CandidateAnalysis$Validation notLive()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.CandidateAnalysis$ValidationState state()
public int matchCount()
public io.github.testlens.selector.engine.CandidateAnalysis$TargetComparison targetComparison()
public java.util.List<java.lang.String> issues()
```

## `io.github.testlens.selector.engine.CandidateAnalysis$ValidationState` {#io-github-testlens-selector-engine-candidateanalysis-validationstate}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState NOT_LIVE_VALIDATED
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState VERIFIED_IN_SCOPE
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState VALID_FOR_INTENT
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState VALID_BUT_AMBIGUOUS
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState WRONG_TARGET
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState NO_MATCH
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState INVALID_SELECTOR
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState STALE_TARGET
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState TARGET_UNAVAILABLE
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState CONTEXT_UNAVAILABLE
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState UNSUPPORTED
public static final io.github.testlens.selector.engine.CandidateAnalysis$ValidationState SESSION_LOST
public static io.github.testlens.selector.engine.CandidateAnalysis$ValidationState[] values()
public static io.github.testlens.selector.engine.CandidateAnalysis$ValidationState valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateAnalysis` {#io-github-testlens-selector-engine-candidateanalysis}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.selector.engine.CandidateAnalysis(int, io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Candidate>, io.github.testlens.selector.engine.CandidateAnalysis$Recommendation, io.github.testlens.selector.engine.CandidateAnalysis$Completeness, java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Issue>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent usageIntent()
public java.lang.String contextFingerprint()
public java.lang.String declarationRef()
public java.lang.String logicalPath()
public java.lang.String originalCandidateId()
public java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Candidate> candidates()
public io.github.testlens.selector.engine.CandidateAnalysis$Recommendation recommendation()
public io.github.testlens.selector.engine.CandidateAnalysis$Completeness completeness()
public java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Issue> issues()
```

## `io.github.testlens.selector.engine.CandidateGenerator$Generated` {#io-github-testlens-selector-engine-candidategenerator-generated}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateGenerator$Generated(java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Candidate>, int, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Candidate> candidates()
public int generatedBeforeDedup()
public boolean candidateLimitReached()
```

## `io.github.testlens.selector.engine.CandidateGenerator$Request` {#io-github-testlens-selector-engine-candidategenerator-request}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CandidateGenerator$Request(io.github.testlens.selector.engine.CandidateAnalysis$Locator, java.util.List<java.lang.String>, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.engine.CompiledPolicySet, io.github.testlens.selector.engine.ObservationEvidence)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.CandidateAnalysis$Locator original()
public java.util.List<java.lang.String> preferredTestAttributes()
public java.lang.String contextFingerprint()
public java.lang.String declarationRef()
public java.lang.String modulePath()
public java.lang.String logicalPath()
public java.lang.String declaringSymbol()
public java.lang.String usageClass()
public java.lang.String usageMethod()
public io.github.testlens.selector.engine.CompiledPolicySet policies()
public io.github.testlens.selector.engine.ObservationEvidence evidence()
```

## `io.github.testlens.selector.engine.CandidateGenerator` {#io-github-testlens-selector-engine-candidategenerator}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int MAX_CANDIDATES
public io.github.testlens.selector.engine.CandidateGenerator()
public io.github.testlens.selector.engine.CandidateGenerator$Generated generate(io.github.testlens.selector.engine.TargetSnapshot, io.github.testlens.selector.engine.CandidateGenerator$Request)
public static java.lang.String cssString(java.lang.String)
public static java.lang.String cssIdentifier(java.lang.String)
public static java.lang.String xpathLiteral(java.lang.String)
```

## `io.github.testlens.selector.engine.CandidateIds` {#io-github-testlens-selector-engine-candidateids}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static java.lang.String id(io.github.testlens.selector.engine.CandidateAnalysis$Locator, java.lang.String)
public static java.lang.String opaqueOriginalId()
```

## `io.github.testlens.selector.engine.CandidateRanker` {#io-github-testlens-selector-engine-candidateranker}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.selector.engine.CandidateRanker()
public java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Candidate> rank(java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Candidate>)
public io.github.testlens.selector.engine.CandidateAnalysis$Recommendation recommend(java.util.List<io.github.testlens.selector.engine.CandidateAnalysis$Candidate>, java.lang.String, boolean)
```

## `io.github.testlens.selector.engine.CanonicalDigests$ContextKnowledge` {#io-github-testlens-selector-engine-canonicaldigests-contextknowledge}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextKnowledge KNOWN
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextKnowledge PARTIAL
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextKnowledge UNKNOWN
public static io.github.testlens.selector.engine.CanonicalDigests$ContextKnowledge[] values()
public static io.github.testlens.selector.engine.CanonicalDigests$ContextKnowledge valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind` {#io-github-testlens-selector-engine-canonicaldigests-contextreferencekind}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind NONE
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind LOCATOR_DIGEST
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind INDEX
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind NAME_OR_ID
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind SESSION_LOCAL_HANDLE
public static final io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind REMOTE_ELEMENT_ID
public static io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind[] values()
public static io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.CanonicalDigests$ContextSegment` {#io-github-testlens-selector-engine-canonicaldigests-contextsegment}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.CanonicalDigests$ContextSegment(java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String kind()
public java.lang.String locatorStrategy()
public java.lang.String locatorValueDigest()
public io.github.testlens.selector.engine.CanonicalDigests$ContextReferenceKind referenceKind()
public java.lang.String referenceValue()
```

## `io.github.testlens.selector.engine.CanonicalDigests` {#io-github-testlens-selector-engine-canonicaldigests}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int CANONICALIZATION_VERSION
public static java.lang.String exactValueDigest(io.github.testlens.selector.engine.SelectorSubject)
public static java.lang.String digest(java.lang.String, java.lang.String...)
public static java.lang.String digestBytes(java.lang.String, byte[])
public static java.lang.String contextFingerprint(io.github.testlens.selector.engine.CanonicalDigests$ContextKnowledge, java.util.List<io.github.testlens.selector.engine.CanonicalDigests$ContextSegment>)
```

## `io.github.testlens.selector.engine.Comparability$ComparisonMode` {#io-github-testlens-selector-engine-comparability-comparisonmode}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.Comparability$ComparisonMode EXACT_VALUE
public static final io.github.testlens.selector.engine.Comparability$ComparisonMode STRUCTURAL_COMPONENTS_ONLY
public static final io.github.testlens.selector.engine.Comparability$ComparisonMode EXPECTED_DYNAMIC_VALUE
public static final io.github.testlens.selector.engine.Comparability$ComparisonMode NOT_COMPARABLE
public static io.github.testlens.selector.engine.Comparability$ComparisonMode[] values()
public static io.github.testlens.selector.engine.Comparability$ComparisonMode valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.Comparability$Result` {#io-github-testlens-selector-engine-comparability-result}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.Comparability$Result(io.github.testlens.selector.engine.Comparability$State, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.Comparability$State state()
public java.util.List<java.lang.String> reasonCodes()
```

## `io.github.testlens.selector.engine.Comparability$State` {#io-github-testlens-selector-engine-comparability-state}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.Comparability$State COMPARABLE
public static final io.github.testlens.selector.engine.Comparability$State NOT_COMPARABLE
public static final io.github.testlens.selector.engine.Comparability$State UNKNOWN
public static io.github.testlens.selector.engine.Comparability$State[] values()
public static io.github.testlens.selector.engine.Comparability$State valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.Comparability` {#io-github-testlens-selector-engine-comparability}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static io.github.testlens.selector.engine.Comparability$Result evaluate(io.github.testlens.selector.engine.RunDescriptor, io.github.testlens.selector.engine.RunDescriptor, io.github.testlens.selector.engine.ObservationEvidence$Domain, io.github.testlens.selector.engine.Comparability$ComparisonMode)
```

## `io.github.testlens.selector.engine.CompiledPolicySet` {#io-github-testlens-selector-engine-compiledpolicyset}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static io.github.testlens.selector.engine.CompiledPolicySet compile(io.github.testlens.selector.engine.SelectorPolicy$Document)
public static io.github.testlens.selector.engine.CompiledPolicySet empty()
public java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Rule> rules()
```

## `io.github.testlens.selector.engine.ComponentIdentity` {#io-github-testlens-selector-engine-componentidentity}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.ComponentIdentity(java.lang.String, java.util.List<java.lang.String>, java.lang.String)
public static io.github.testlens.selector.engine.ComponentIdentity of(java.lang.String, java.lang.String...)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String componentRole()
public java.util.List<java.lang.String> componentPath()
public java.lang.String componentRef()
```

## `io.github.testlens.selector.engine.FederatedPatternPreview$Entry` {#io-github-testlens-selector-engine-federatedpatternpreview-entry}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.FederatedPatternPreview$Entry(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String subjectRef()
public java.lang.String declarationRef()
public java.lang.String reasonCode()
```

## `io.github.testlens.selector.engine.FederatedPatternPreview$Result` {#io-github-testlens-selector-engine-federatedpatternpreview-result}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.FederatedPatternPreview$Result(io.github.testlens.selector.engine.PatternProposal$Result, java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry>, java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry>, java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry>, java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry>, java.util.List<io.github.testlens.selector.engine.StabilityAssessment>, java.util.List<io.github.testlens.selector.engine.StabilityAssessment>, java.util.List<java.lang.String>, boolean, java.util.List<java.lang.String>, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.PatternProposal$Result proposal()
public java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry> matchedStaticSubjects()
public java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry> matchedHistorySubjects()
public java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry> excludedNearMatches()
public java.util.List<io.github.testlens.selector.engine.FederatedPatternPreview$Entry> unsupportedDigestOnlySubjects()
public java.util.List<io.github.testlens.selector.engine.StabilityAssessment> policyConflicts()
public java.util.List<io.github.testlens.selector.engine.StabilityAssessment> evidenceConflicts()
public java.util.List<java.lang.String> limitations()
public boolean incompleteHistory()
public java.util.List<java.lang.String> affectedDeclarationRefs()
public int usageCount()
```

## `io.github.testlens.selector.engine.FederatedPatternPreview` {#io-github-testlens-selector-engine-federatedpatternpreview}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.selector.engine.FederatedPatternPreview()
public io.github.testlens.selector.engine.FederatedPatternPreview$Result preview(io.github.testlens.selector.engine.PatternProposal$Result, java.util.List<io.github.testlens.selector.engine.SimilaritySubject>, io.github.testlens.selector.engine.CompiledPolicySet, java.util.Map<java.lang.String, io.github.testlens.selector.engine.ObservationEvidence>, boolean)
```

## `io.github.testlens.selector.engine.ObservationEvidence$Comparison` {#io-github-testlens-selector-engine-observationevidence-comparison}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.ObservationEvidence$Comparison(java.lang.String, io.github.testlens.selector.engine.ObservationEvidence$Domain, boolean, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String evidenceRef()
public io.github.testlens.selector.engine.ObservationEvidence$Domain domain()
public boolean comparable()
public boolean changed()
```

## `io.github.testlens.selector.engine.ObservationEvidence$Domain` {#io-github-testlens-selector-engine-observationevidence-domain}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.ObservationEvidence$Domain SAME_DOCUMENT
public static final io.github.testlens.selector.engine.ObservationEvidence$Domain REMOUNT
public static final io.github.testlens.selector.engine.ObservationEvidence$Domain RELOAD
public static final io.github.testlens.selector.engine.ObservationEvidence$Domain NEW_SESSION
public static final io.github.testlens.selector.engine.ObservationEvidence$Domain NEW_RUN
public static final io.github.testlens.selector.engine.ObservationEvidence$Domain DIFFERENT_DATASET
public static final io.github.testlens.selector.engine.ObservationEvidence$Domain DIFFERENT_BUILD
public static io.github.testlens.selector.engine.ObservationEvidence$Domain[] values()
public static io.github.testlens.selector.engine.ObservationEvidence$Domain valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.ObservationEvidence$State` {#io-github-testlens-selector-engine-observationevidence-state}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.ObservationEvidence$State NOT_AVAILABLE
public static final io.github.testlens.selector.engine.ObservationEvidence$State INSUFFICIENT
public static final io.github.testlens.selector.engine.ObservationEvidence$State UNCHANGED_IN_SCOPE
public static final io.github.testlens.selector.engine.ObservationEvidence$State CHANGED
public static io.github.testlens.selector.engine.ObservationEvidence$State[] values()
public static io.github.testlens.selector.engine.ObservationEvidence$State valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.ObservationEvidence` {#io-github-testlens-selector-engine-observationevidence}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.ObservationEvidence(io.github.testlens.selector.engine.ObservationEvidence$State, java.util.List<io.github.testlens.selector.engine.ObservationEvidence$Comparison>)
public static io.github.testlens.selector.engine.ObservationEvidence unavailable()
public static io.github.testlens.selector.engine.ObservationEvidence of(java.util.List<io.github.testlens.selector.engine.ObservationEvidence$Comparison>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.ObservationEvidence$State state()
public java.util.List<io.github.testlens.selector.engine.ObservationEvidence$Comparison> comparisons()
```

## `io.github.testlens.selector.engine.PatternProposal$Result` {#io-github-testlens-selector-engine-patternproposal-result}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PatternProposal$Result(int, io.github.testlens.selector.engine.SelectorPolicy$StructuralPattern, java.util.List<io.github.testlens.selector.engine.AppearanceSignal>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public boolean useful()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public io.github.testlens.selector.engine.SelectorPolicy$StructuralPattern pattern()
public java.util.List<io.github.testlens.selector.engine.AppearanceSignal> findings()
public java.util.List<java.lang.String> suppressedDetectorIds()
public java.util.List<java.lang.String> issues()
```

## `io.github.testlens.selector.engine.PatternProposal` {#io-github-testlens-selector-engine-patternproposal}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.selector.engine.PatternProposal()
public io.github.testlens.selector.engine.PatternProposal$Result propose(io.github.testlens.selector.engine.SelectorSubject)
```

## `io.github.testlens.selector.engine.PolicyDraft$Action` {#io-github-testlens-selector-engine-policydraft-action}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyDraft$Action ADD
public static final io.github.testlens.selector.engine.PolicyDraft$Action REMOVE
public static final io.github.testlens.selector.engine.PolicyDraft$Action REPLACE
public static io.github.testlens.selector.engine.PolicyDraft$Action[] values()
public static io.github.testlens.selector.engine.PolicyDraft$Action valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyDraft$BlockingIssue` {#io-github-testlens-selector-engine-policydraft-blockingissue}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue POLICY_POLICY_CONFLICT
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue ALREADY_EXISTS
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue UNSAFE_SCOPE
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue PREVIEW_REQUIRED
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue INVALID_CANDIDATE
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue UNSUPPORTED_MATCHER
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue MALFORMED_WORKSPACE
public static final io.github.testlens.selector.engine.PolicyDraft$BlockingIssue CROSS_ORIGIN_REPLACE
public static io.github.testlens.selector.engine.PolicyDraft$BlockingIssue[] values()
public static io.github.testlens.selector.engine.PolicyDraft$BlockingIssue valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement` {#io-github-testlens-selector-engine-policydraft-hostacknowledgement}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement POLICY_EVIDENCE_CONFLICT
public static final io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement PREVIEW_INCOMPLETE
public static final io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement AMBIGUOUS_LIVE_VALIDATION
public static io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement[] values()
public static io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyDraft$OriginPrecondition` {#io-github-testlens-selector-engine-policydraft-originprecondition}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyDraft$OriginPrecondition(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState expectedState()
public java.lang.String fileDigest()
public java.lang.String ruleSetDigest()
```

## `io.github.testlens.selector.engine.PolicyDraft$PendingPolicyChange` {#io-github-testlens-selector-engine-policydraft-pendingpolicychange}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyDraft$PendingPolicyChange(io.github.testlens.selector.engine.PolicyDraft, java.lang.String, java.util.Set<io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement>, boolean)
public static io.github.testlens.selector.engine.PolicyDraft$PendingPolicyChange transfer(io.github.testlens.selector.engine.PolicyDraft, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.PolicyDraft draft()
public java.lang.String sanitizedSummary()
public java.util.Set<io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement> requiredHostAcknowledgements()
public boolean hostApprovalRequired()
```

## `io.github.testlens.selector.engine.PolicyDraft$Preview` {#io-github-testlens-selector-engine-policydraft-preview}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyDraft$Preview(io.github.testlens.selector.engine.PolicyDraft$ScopeChoice, java.util.List<io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, int, int, int, int, boolean, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.PolicyDraft$ScopeChoice scopeChoice()
public java.util.List<io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition> dispositions()
public java.util.List<java.lang.String> selectedRuleIds()
public java.util.List<java.lang.String> shadowedRuleIds()
public int staticMatches()
public int historyMatches()
public int affectedDeclarations()
public int usageCount()
public boolean complete()
public java.util.List<java.lang.String> reasonCodes()
```

## `io.github.testlens.selector.engine.PolicyDraft$ScopeChoice` {#io-github-testlens-selector-engine-policydraft-scopechoice}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyDraft$ScopeChoice DECLARATION
public static final io.github.testlens.selector.engine.PolicyDraft$ScopeChoice FILE
public static final io.github.testlens.selector.engine.PolicyDraft$ScopeChoice MODULE
public static final io.github.testlens.selector.engine.PolicyDraft$ScopeChoice PROJECT
public static final io.github.testlens.selector.engine.PolicyDraft$ScopeChoice RUNTIME_NARROW
public static io.github.testlens.selector.engine.PolicyDraft$ScopeChoice[] values()
public static io.github.testlens.selector.engine.PolicyDraft$ScopeChoice valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyDraft$SecurityClassification` {#io-github-testlens-selector-engine-policydraft-securityclassification}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyDraft$SecurityClassification DIGEST_ONLY
public static final io.github.testlens.selector.engine.PolicyDraft$SecurityClassification REVIEW_LITERAL_CONTENT
public static final io.github.testlens.selector.engine.PolicyDraft$SecurityClassification LOCAL_RECOMMENDED
public static io.github.testlens.selector.engine.PolicyDraft$SecurityClassification[] values()
public static io.github.testlens.selector.engine.PolicyDraft$SecurityClassification valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition` {#io-github-testlens-selector-engine-policydraft-simulationdisposition}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition WOULD_BE_SELECTED
public static final io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition ALREADY_EXISTS
public static final io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition SHADOWED_RULE
public static final io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition WOULD_SHADOW_RULE
public static final io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition POLICY_POLICY_CONFLICT
public static final io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition POLICY_EVIDENCE_CONFLICT
public static final io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition PREVIEW_INCOMPLETE
public static io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition[] values()
public static io.github.testlens.selector.engine.PolicyDraft$SimulationDisposition valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyDraft$Source` {#io-github-testlens-selector-engine-policydraft-source}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyDraft$Source(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String analysisId()
public java.lang.String candidateId()
public java.lang.String declarationRef()
public java.lang.String patternProposalId()
```

## `io.github.testlens.selector.engine.PolicyDraft$Warning` {#io-github-testlens-selector-engine-policydraft-warning}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyDraft$Warning PREVIEW_INCOMPLETE
public static final io.github.testlens.selector.engine.PolicyDraft$Warning POLICY_EVIDENCE_CONFLICT
public static final io.github.testlens.selector.engine.PolicyDraft$Warning AMBIGUOUS_LIVE_VALIDATION
public static final io.github.testlens.selector.engine.PolicyDraft$Warning DIGEST_DICTIONARY_RISK
public static final io.github.testlens.selector.engine.PolicyDraft$Warning PATTERN_LITERAL_CONTENT
public static io.github.testlens.selector.engine.PolicyDraft$Warning[] values()
public static io.github.testlens.selector.engine.PolicyDraft$Warning valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyDraft$WorkspacePreconditions` {#io-github-testlens-selector-engine-policydraft-workspacepreconditions}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyDraft$WorkspacePreconditions(java.lang.String, io.github.testlens.selector.engine.PolicyDraft$OriginPrecondition, io.github.testlens.selector.engine.PolicyDraft$OriginPrecondition, java.util.Set<java.lang.String>, java.util.Set<java.lang.String>, java.lang.String)
public static io.github.testlens.selector.engine.PolicyDraft$WorkspacePreconditions from(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot, java.util.Set<java.lang.String>, java.util.Set<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String workspaceDigest()
public io.github.testlens.selector.engine.PolicyDraft$OriginPrecondition tracked()
public io.github.testlens.selector.engine.PolicyDraft$OriginPrecondition local()
public java.util.Set<java.lang.String> expectedRuleIdsPresent()
public java.util.Set<java.lang.String> expectedRuleIdsAbsent()
public java.lang.String projectFingerprint()
```

## `io.github.testlens.selector.engine.PolicyDraft` {#io-github-testlens-selector-engine-policydraft}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.selector.engine.PolicyDraft(int, java.lang.String, io.github.testlens.selector.engine.PolicyDraft$Action, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin, io.github.testlens.selector.engine.SelectorPolicy$Rule, java.lang.String, io.github.testlens.selector.engine.PolicyDraft$Source, io.github.testlens.selector.engine.PolicyDraft$Preview, java.util.List<io.github.testlens.selector.engine.PolicyDraft$Warning>, java.util.List<io.github.testlens.selector.engine.PolicyDraft$BlockingIssue>, java.util.Set<io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement>, io.github.testlens.selector.engine.PolicyDraft$SecurityClassification, io.github.testlens.selector.engine.PolicyDraft$WorkspacePreconditions)
public static io.github.testlens.selector.engine.PolicyDraft create(io.github.testlens.selector.engine.PolicyDraft$Action, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin, io.github.testlens.selector.engine.SelectorPolicy$Rule, java.lang.String, io.github.testlens.selector.engine.PolicyDraft$Source, io.github.testlens.selector.engine.PolicyDraft$Preview, java.util.List<io.github.testlens.selector.engine.PolicyDraft$Warning>, java.util.List<io.github.testlens.selector.engine.PolicyDraft$BlockingIssue>, java.util.Set<io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement>, io.github.testlens.selector.engine.PolicyDraft$SecurityClassification, io.github.testlens.selector.engine.PolicyDraft$WorkspacePreconditions)
public boolean transferable()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String draftId()
public io.github.testlens.selector.engine.PolicyDraft$Action action()
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin destination()
public io.github.testlens.selector.engine.SelectorPolicy$Rule proposedRule()
public java.lang.String oldRuleId()
public io.github.testlens.selector.engine.PolicyDraft$Source source()
public io.github.testlens.selector.engine.PolicyDraft$Preview preview()
public java.util.List<io.github.testlens.selector.engine.PolicyDraft$Warning> warnings()
public java.util.List<io.github.testlens.selector.engine.PolicyDraft$BlockingIssue> blockingIssues()
public java.util.Set<io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement> requiredHostAcknowledgements()
public io.github.testlens.selector.engine.PolicyDraft$SecurityClassification securityClassification()
public io.github.testlens.selector.engine.PolicyDraft$WorkspacePreconditions workspacePreconditions()
```

## `io.github.testlens.selector.engine.PolicyDraftService$Preparation` {#io-github-testlens-selector-engine-policydraftservice-preparation}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyDraftService$Preparation(io.github.testlens.selector.engine.PolicyDraft, java.util.List<java.lang.String>)
public static io.github.testlens.selector.engine.PolicyDraftService$Preparation rejected(java.lang.String)
public boolean prepared()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.PolicyDraft draft()
public java.util.List<java.lang.String> issues()
```

## `io.github.testlens.selector.engine.PolicyDraftService` {#io-github-testlens-selector-engine-policydraftservice}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.selector.engine.PolicyDraftService()
public io.github.testlens.selector.engine.PolicyDraftService$Preparation prepareExact(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot, io.github.testlens.selector.engine.CandidateAnalysis$Candidate, io.github.testlens.selector.engine.SelectorSubject, io.github.testlens.selector.engine.SelectorPolicy$Decision, io.github.testlens.selector.engine.PolicyDraft$ScopeChoice, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin, io.github.testlens.selector.engine.ObservationEvidence)
public io.github.testlens.selector.engine.PolicyDraftService$Preparation preparePattern(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot, io.github.testlens.selector.engine.PatternProposal$Result, io.github.testlens.selector.engine.SelectorSubject, io.github.testlens.selector.engine.SelectorPolicy$Decision, io.github.testlens.selector.engine.PolicyDraft$ScopeChoice, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin, io.github.testlens.selector.engine.FederatedPatternPreview$Result, io.github.testlens.selector.engine.ObservationEvidence, java.lang.String, java.lang.String)
public io.github.testlens.selector.engine.PolicyDraftService$Preparation prepareRemove(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot, java.lang.String)
public io.github.testlens.selector.engine.PolicyDraftService$Preparation prepareReplace(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot, java.lang.String, io.github.testlens.selector.engine.SelectorPolicy$Decision, io.github.testlens.selector.engine.SelectorPolicy$Scope)
```

## `io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState` {#io-github-testlens-selector-engine-policyworkspacesnapshot-filestate}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState EXPECTED_ABSENT
public static final io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState EXPECTED_PRESENT
public static final io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState INVALID
public static final io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState UNAVAILABLE
public static io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState[] values()
public static io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin` {#io-github-testlens-selector-engine-policyworkspacesnapshot-origin}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin TRACKED
public static final io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin LOCAL
public static io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin[] values()
public static io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument` {#io-github-testlens-selector-engine-policyworkspacesnapshot-origindocument}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Rule>)
public static io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument absent(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin origin()
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$FileState fileState()
public java.lang.String fileDigest()
public java.lang.String semanticRuleSetDigest()
public java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Rule> rules()
```

## `io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginRule` {#io-github-testlens-selector-engine-policyworkspacesnapshot-originrule}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginRule(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin, io.github.testlens.selector.engine.SelectorPolicy$Rule)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin origin()
public io.github.testlens.selector.engine.SelectorPolicy$Rule rule()
```

## `io.github.testlens.selector.engine.PolicyWorkspaceSnapshot` {#io-github-testlens-selector-engine-policyworkspacesnapshot}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot(int, int, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument, java.lang.String, java.lang.String)
public static io.github.testlens.selector.engine.PolicyWorkspaceSnapshot create(io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument, java.lang.String)
public java.util.List<io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginRule> originRules()
public io.github.testlens.selector.engine.CompiledPolicySet compiled()
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginRule find(java.lang.String)
public static java.lang.String rawFileDigest(byte[])
public static java.lang.String semanticDigest(java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Rule>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public int canonicalizationVersion()
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument tracked()
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$OriginDocument local()
public java.lang.String effectiveWorkspaceDigest()
public java.lang.String projectFingerprint()
```

## `io.github.testlens.selector.engine.RunDescriptor` {#io-github-testlens-selector-engine-rundescriptor}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.RunDescriptor(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.time.Instant, java.lang.String, java.lang.Long)
public java.lang.String datasetKeyDigest()
public java.lang.String environmentKeyDigest()
public java.lang.String logicalTestIdentityRef()
public java.lang.String runRef()
public boolean chronologyKnown()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String framework()
public java.lang.String testClass()
public java.lang.String testMethod()
public java.lang.String logicalTestKey()
public java.lang.String invocationDiscriminator()
public int attempt()
public java.lang.String datasetKey()
public java.lang.String testSourceRevision()
public java.lang.String systemUnderTestRevision()
public java.lang.String environmentKey()
public java.time.Instant observedAt()
public java.lang.String chronologyKey()
public java.lang.Long runSequence()
```

## `io.github.testlens.selector.engine.SelectorAuditProjection$Declaration` {#io-github-testlens-selector-engine-selectorauditprojection-declaration}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorAuditProjection$Declaration(java.lang.String, io.github.testlens.selector.engine.SelectorAuditProjection$Source, java.util.List<io.github.testlens.selector.engine.SelectorAuditProjection$Finding>, io.github.testlens.selector.engine.SelectorAuditProjection$Summary, io.github.testlens.selector.engine.SelectorAuditProjection$Summary, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String declarationRef()
public io.github.testlens.selector.engine.SelectorAuditProjection$Source source()
public java.util.List<io.github.testlens.selector.engine.SelectorAuditProjection$Finding> findings()
public io.github.testlens.selector.engine.SelectorAuditProjection$Summary evidence()
public io.github.testlens.selector.engine.SelectorAuditProjection$Summary policy()
public java.lang.String recommendation()
public java.util.List<java.lang.String> familyRefs()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.selector.engine.SelectorAuditProjection$Finding` {#io-github-testlens-selector-engine-selectorauditprojection-finding}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorAuditProjection$Finding(java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String severity()
public java.lang.String state()
public java.lang.String code()
public java.util.List<java.lang.String> reasonCodes()
```

## `io.github.testlens.selector.engine.SelectorAuditProjection$Source` {#io-github-testlens-selector-engine-selectorauditprojection-source}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorAuditProjection$Source(java.lang.String, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String logicalPath()
public int startLine()
public int startColumn()
public int endLine()
public int endColumn()
```

## `io.github.testlens.selector.engine.SelectorAuditProjection$Summary` {#io-github-testlens-selector-engine-selectorauditprojection-summary}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorAuditProjection$Summary(java.lang.String, java.util.List<java.lang.String>, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String state()
public java.util.List<java.lang.String> reasonCodes()
public boolean incomplete()
```

## `io.github.testlens.selector.engine.SelectorAuditProjection` {#io-github-testlens-selector-engine-selectorauditprojection}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.selector.engine.SelectorAuditProjection(int, java.util.List<io.github.testlens.selector.engine.SelectorAuditProjection$Declaration>, java.util.List<java.lang.String>)
public io.github.testlens.selector.engine.SelectorAuditProjection$Declaration declaration(java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.util.List<io.github.testlens.selector.engine.SelectorAuditProjection$Declaration> declarations()
public java.util.List<java.lang.String> coverageLimitations()
```

## `io.github.testlens.selector.engine.SelectorPolicy$Decision` {#io-github-testlens-selector-engine-selectorpolicy-decision}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SelectorPolicy$Decision STABLE
public static final io.github.testlens.selector.engine.SelectorPolicy$Decision UNSTABLE
public static io.github.testlens.selector.engine.SelectorPolicy$Decision[] values()
public static io.github.testlens.selector.engine.SelectorPolicy$Decision valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SelectorPolicy$Document` {#io-github-testlens-selector-engine-selectorpolicy-document}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorPolicy$Document(int, int, java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Rule>)
public static io.github.testlens.selector.engine.SelectorPolicy$Document empty()
public io.github.testlens.selector.engine.SelectorPolicy$Document canonical()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public int canonicalizationVersion()
public java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Rule> rules()
```

## `io.github.testlens.selector.engine.SelectorPolicy$ExactMatcher` {#io-github-testlens-selector-engine-selectorpolicy-exactmatcher}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorPolicy$ExactMatcher(java.lang.String, java.lang.String, java.lang.String)
public static io.github.testlens.selector.engine.SelectorPolicy$ExactMatcher from(io.github.testlens.selector.engine.SelectorSubject)
public io.github.testlens.selector.engine.SelectorPolicy$MatcherKind kind()
public boolean matches(io.github.testlens.selector.engine.SelectorSubject)
public java.lang.String canonicalForm()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String strategy()
public java.lang.String valueDigest()
public java.lang.String displayHint()
```

## `io.github.testlens.selector.engine.SelectorPolicy$Matcher` {#io-github-testlens-selector-engine-selectorpolicy-matcher}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `interface`

```java
public abstract io.github.testlens.selector.engine.SelectorPolicy$MatcherKind kind()
public abstract java.lang.String strategy()
public abstract boolean matches(io.github.testlens.selector.engine.SelectorSubject)
public abstract java.lang.String canonicalForm()
public default int matcherSpecificity()
public default int literalCoverage()
public default int placeholderCount()
```

## `io.github.testlens.selector.engine.SelectorPolicy$MatcherKind` {#io-github-testlens-selector-engine-selectorpolicy-matcherkind}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SelectorPolicy$MatcherKind EXACT_VALUE_DIGEST
public static final io.github.testlens.selector.engine.SelectorPolicy$MatcherKind STRUCTURAL_PATTERN
public static io.github.testlens.selector.engine.SelectorPolicy$MatcherKind[] values()
public static io.github.testlens.selector.engine.SelectorPolicy$MatcherKind valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SelectorPolicy$Reason` {#io-github-testlens-selector-engine-selectorpolicy-reason}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorPolicy$Reason(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String note()
```

## `io.github.testlens.selector.engine.SelectorPolicy$Rule` {#io-github-testlens-selector-engine-selectorpolicy-rule}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorPolicy$Rule(java.lang.String, io.github.testlens.selector.engine.SelectorPolicy$Decision, int, io.github.testlens.selector.engine.SelectorPolicy$Scope, io.github.testlens.selector.engine.SelectorPolicy$Matcher, io.github.testlens.selector.engine.SelectorPolicy$Reason)
public static io.github.testlens.selector.engine.SelectorPolicy$Rule create(io.github.testlens.selector.engine.SelectorPolicy$Decision, int, io.github.testlens.selector.engine.SelectorPolicy$Scope, io.github.testlens.selector.engine.SelectorPolicy$Matcher, io.github.testlens.selector.engine.SelectorPolicy$Reason)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String ruleId()
public io.github.testlens.selector.engine.SelectorPolicy$Decision decision()
public int priority()
public io.github.testlens.selector.engine.SelectorPolicy$Scope scope()
public io.github.testlens.selector.engine.SelectorPolicy$Matcher matcher()
public io.github.testlens.selector.engine.SelectorPolicy$Reason reason()
```

## `io.github.testlens.selector.engine.SelectorPolicy$Scope` {#io-github-testlens-selector-engine-selectorpolicy-scope}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorPolicy$Scope(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public static io.github.testlens.selector.engine.SelectorPolicy$Scope project()
public boolean matches(io.github.testlens.selector.engine.SelectorSubject)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String modulePath()
public java.lang.String logicalPath()
public java.lang.String declarationRef()
public java.lang.String declaringSymbol()
public java.lang.String usageClass()
public java.lang.String usageMethod()
public java.lang.String contextFingerprint()
```

## `io.github.testlens.selector.engine.SelectorPolicy$Segment` {#io-github-testlens-selector-engine-selectorpolicy-segment}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorPolicy$Segment(io.github.testlens.selector.engine.SelectorPolicy$SegmentKind, java.lang.String, int, int, java.lang.String)
public static io.github.testlens.selector.engine.SelectorPolicy$Segment literal(java.lang.String)
public static io.github.testlens.selector.engine.SelectorPolicy$Segment run(io.github.testlens.selector.engine.SelectorPolicy$SegmentKind, int, int)
public static io.github.testlens.selector.engine.SelectorPolicy$Segment opaque(java.lang.String, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.SelectorPolicy$SegmentKind kind()
public java.lang.String literal()
public int minLength()
public int maxLength()
public java.lang.String alphabet()
```

## `io.github.testlens.selector.engine.SelectorPolicy$SegmentKind` {#io-github-testlens-selector-engine-selectorpolicy-segmentkind}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SelectorPolicy$SegmentKind LITERAL
public static final io.github.testlens.selector.engine.SelectorPolicy$SegmentKind UUID_LIKE
public static final io.github.testlens.selector.engine.SelectorPolicy$SegmentKind DECIMAL_RUN
public static final io.github.testlens.selector.engine.SelectorPolicy$SegmentKind HEX_RUN
public static final io.github.testlens.selector.engine.SelectorPolicy$SegmentKind OPAQUE_RUN
public static io.github.testlens.selector.engine.SelectorPolicy$SegmentKind[] values()
public static io.github.testlens.selector.engine.SelectorPolicy$SegmentKind valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SelectorPolicy$StructuralPattern` {#io-github-testlens-selector-engine-selectorpolicy-structuralpattern}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorPolicy$StructuralPattern(java.lang.String, java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Segment>)
public io.github.testlens.selector.engine.SelectorPolicy$MatcherKind kind()
public boolean matches(io.github.testlens.selector.engine.SelectorSubject)
public java.lang.String canonicalForm()
public int literalCoverage()
public int placeholderCount()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String strategy()
public java.util.List<io.github.testlens.selector.engine.SelectorPolicy$Segment> segments()
```

## `io.github.testlens.selector.engine.SelectorPolicy` {#io-github-testlens-selector-engine-selectorpolicy}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int SCHEMA_VERSION
public static final int MAX_RULES
public static final int MAX_SEGMENTS
public static final int MAX_LITERAL_CODE_POINTS
public static final int MAX_RUN_LENGTH
public static final int MAX_NOTE_CODE_POINTS
```

## `io.github.testlens.selector.engine.SelectorStabilityEngine$Preview` {#io-github-testlens-selector-engine-selectorstabilityengine-preview}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SelectorStabilityEngine$Preview(java.util.List<io.github.testlens.selector.engine.StabilityAssessment>, java.util.List<io.github.testlens.selector.engine.SelectorSubject>, java.util.List<io.github.testlens.selector.engine.StabilityAssessment>, java.util.List<io.github.testlens.selector.engine.SelectorSubject>, java.util.List<java.lang.String>, java.util.List<io.github.testlens.selector.engine.ObservationEvidence$Domain>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<io.github.testlens.selector.engine.StabilityAssessment> assessments()
public java.util.List<io.github.testlens.selector.engine.SelectorSubject> matchedSubjects()
public java.util.List<io.github.testlens.selector.engine.StabilityAssessment> conflicts()
public java.util.List<io.github.testlens.selector.engine.SelectorSubject> incompleteSubjects()
public java.util.List<java.lang.String> affectedDeclarationRefs()
public java.util.List<io.github.testlens.selector.engine.ObservationEvidence$Domain> evidenceDomains()
```

## `io.github.testlens.selector.engine.SelectorStabilityEngine` {#io-github-testlens-selector-engine-selectorstabilityengine}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final java.lang.String ENGINE_NAME
public static final java.lang.String ENGINE_VERSION
public static final int ANALYSIS_SCHEMA_VERSION
public static final int MAX_PREVIEW_SUBJECTS
public io.github.testlens.selector.engine.SelectorStabilityEngine()
public io.github.testlens.selector.engine.StabilityAssessment analyze(io.github.testlens.selector.engine.SelectorSubject, io.github.testlens.selector.engine.ObservationEvidence, io.github.testlens.selector.engine.CompiledPolicySet)
public io.github.testlens.selector.engine.StabilityAssessment analyzeIndexed(io.github.testlens.selector.engine.SelectorSubject, java.util.List<io.github.testlens.selector.engine.AppearanceSignal>, java.lang.String, io.github.testlens.selector.engine.StructuralFamily, io.github.testlens.selector.engine.ObservationEvidence, io.github.testlens.selector.engine.CompiledPolicySet)
public io.github.testlens.selector.engine.SelectorStabilityEngine$Preview preview(java.util.List<io.github.testlens.selector.engine.SelectorSubject>, io.github.testlens.selector.engine.ObservationEvidence, io.github.testlens.selector.engine.CompiledPolicySet)
```

## `io.github.testlens.selector.engine.SelectorSubject$InputTrust` {#io-github-testlens-selector-engine-selectorsubject-inputtrust}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SelectorSubject$InputTrust SOURCE_CANONICAL
public static final io.github.testlens.selector.engine.SelectorSubject$InputTrust RUNTIME_RAW_LOCAL
public static final io.github.testlens.selector.engine.SelectorSubject$InputTrust RUNTIME_REDACTED
public static io.github.testlens.selector.engine.SelectorSubject$InputTrust[] values()
public static io.github.testlens.selector.engine.SelectorSubject$InputTrust valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SelectorSubject$SubjectKind` {#io-github-testlens-selector-engine-selectorsubject-subjectkind}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SelectorSubject$SubjectKind STATIC_DECLARATION
public static final io.github.testlens.selector.engine.SelectorSubject$SubjectKind RUNTIME_OBSERVATION
public static final io.github.testlens.selector.engine.SelectorSubject$SubjectKind TEMPLATE
public static final io.github.testlens.selector.engine.SelectorSubject$SubjectKind CUSTOM
public static io.github.testlens.selector.engine.SelectorSubject$SubjectKind[] values()
public static io.github.testlens.selector.engine.SelectorSubject$SubjectKind valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SelectorSubject$ValueState` {#io-github-testlens-selector-engine-selectorsubject-valuestate}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SelectorSubject$ValueState KNOWN
public static final io.github.testlens.selector.engine.SelectorSubject$ValueState UNAVAILABLE
public static io.github.testlens.selector.engine.SelectorSubject$ValueState[] values()
public static io.github.testlens.selector.engine.SelectorSubject$ValueState valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SelectorSubject` {#io-github-testlens-selector-engine-selectorsubject}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int MAX_VALUE_CODE_POINTS
public io.github.testlens.selector.engine.SelectorSubject(io.github.testlens.selector.engine.SelectorSubject$SubjectKind, java.lang.String, io.github.testlens.selector.engine.SelectorSubject$ValueState, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.engine.SelectorSubject$InputTrust, java.lang.String)
public static io.github.testlens.selector.engine.SelectorSubject trusted(java.lang.String, java.lang.String)
public boolean hasTrustedExactValue()
public static java.lang.String normalizeStrategy(java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.SelectorSubject$SubjectKind kind()
public java.lang.String strategy()
public io.github.testlens.selector.engine.SelectorSubject$ValueState valueState()
public java.lang.String canonicalValue()
public java.lang.String declarationRef()
public java.lang.String modulePath()
public java.lang.String logicalPath()
public java.lang.String declaringSymbol()
public java.lang.String usageClass()
public java.lang.String usageMethod()
public java.lang.String contextFingerprint()
public io.github.testlens.selector.engine.SelectorSubject$InputTrust inputTrust()
public java.lang.String templateInformation()
```

## `io.github.testlens.selector.engine.SessionCandidateFeedback$State` {#io-github-testlens-selector-engine-sessioncandidatefeedback-state}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SessionCandidateFeedback$State USER_ACCEPTED_ONCE
public static io.github.testlens.selector.engine.SessionCandidateFeedback$State[] values()
public static io.github.testlens.selector.engine.SessionCandidateFeedback$State valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SessionCandidateFeedback` {#io-github-testlens-selector-engine-sessioncandidatefeedback}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SessionCandidateFeedback(java.lang.String, java.lang.String, io.github.testlens.selector.engine.SessionCandidateFeedback$State, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String analysisId()
public java.lang.String candidateId()
public io.github.testlens.selector.engine.SessionCandidateFeedback$State state()
public boolean ambiguityWarning()
```

## `io.github.testlens.selector.engine.SimilarityIndex` {#io-github-testlens-selector-engine-similarityindex}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int MAX_SUBJECTS
public io.github.testlens.selector.engine.SimilarityIndex(java.util.List<io.github.testlens.selector.engine.SimilaritySubject>)
public io.github.testlens.selector.engine.SimilarityIndex(java.util.List<io.github.testlens.selector.engine.SimilaritySubject>, boolean, java.util.List<java.lang.String>)
public io.github.testlens.selector.engine.SimilarityResult find(io.github.testlens.selector.engine.SimilaritySubject, io.github.testlens.selector.engine.SimilarityResult$QueryScope, io.github.testlens.selector.engine.CompiledPolicySet, java.util.Map<java.lang.String, io.github.testlens.selector.engine.ObservationEvidence>)
```

## `io.github.testlens.selector.engine.SimilarityResult$Completeness` {#io-github-testlens-selector-engine-similarityresult-completeness}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SimilarityResult$Completeness(boolean, int, int, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean complete()
public int catalogSubjects()
public int consideredSubjects()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.selector.engine.SimilarityResult$QueryScope` {#io-github-testlens-selector-engine-similarityresult-queryscope}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SimilarityResult$QueryScope SAME_DECLARATION
public static final io.github.testlens.selector.engine.SimilarityResult$QueryScope SAME_FILE
public static final io.github.testlens.selector.engine.SimilarityResult$QueryScope SAME_MODULE
public static final io.github.testlens.selector.engine.SimilarityResult$QueryScope PROJECT
public static final io.github.testlens.selector.engine.SimilarityResult$QueryScope SUPPLIED_HISTORY
public static io.github.testlens.selector.engine.SimilarityResult$QueryScope[] values()
public static io.github.testlens.selector.engine.SimilarityResult$QueryScope valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SimilarityResult$RelatedSubject` {#io-github-testlens-selector-engine-similarityresult-relatedsubject}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SimilarityResult$RelatedSubject(java.lang.String, io.github.testlens.selector.engine.SimilarityResult$RelationClass, java.util.List<io.github.testlens.selector.engine.SimilarityResult$Signal>, io.github.testlens.selector.engine.SimilaritySubject$Confidence, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.engine.SimilaritySubject$UsageReference>, int, java.util.List<io.github.testlens.selector.engine.AppearanceSignal$Family>, io.github.testlens.selector.engine.StabilityAssessment, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String subjectRef()
public io.github.testlens.selector.engine.SimilarityResult$RelationClass relationClass()
public java.util.List<io.github.testlens.selector.engine.SimilarityResult$Signal> signals()
public io.github.testlens.selector.engine.SimilaritySubject$Confidence correlationConfidence()
public java.util.List<java.lang.String> supportingSignals()
public java.util.List<java.lang.String> conflictingSignals()
public java.lang.String declarationRef()
public java.lang.String logicalPath()
public java.util.List<io.github.testlens.selector.engine.SimilaritySubject$UsageReference> usageSamples()
public int usageCount()
public java.util.List<io.github.testlens.selector.engine.AppearanceSignal$Family> appearanceFamilies()
public io.github.testlens.selector.engine.StabilityAssessment policyAndEvidence()
public java.util.List<java.lang.String> limitations()
public java.lang.String explanation()
```

## `io.github.testlens.selector.engine.SimilarityResult$RelationClass` {#io-github-testlens-selector-engine-similarityresult-relationclass}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SimilarityResult$RelationClass SAME_DECLARATION
public static final io.github.testlens.selector.engine.SimilarityResult$RelationClass EXACT_LOCATOR
public static final io.github.testlens.selector.engine.SimilarityResult$RelationClass SAME_TEMPLATE
public static final io.github.testlens.selector.engine.SimilarityResult$RelationClass EXPLICIT_PATTERN_RELATED
public static final io.github.testlens.selector.engine.SimilarityResult$RelationClass SAME_STRUCTURAL_FAMILY
public static final io.github.testlens.selector.engine.SimilarityResult$RelationClass APPEARANCE_RELATED
public static final io.github.testlens.selector.engine.SimilarityResult$RelationClass INSUFFICIENT_RELATION
public static io.github.testlens.selector.engine.SimilarityResult$RelationClass[] values()
public static io.github.testlens.selector.engine.SimilarityResult$RelationClass valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SimilarityResult$Signal` {#io-github-testlens-selector-engine-similarityresult-signal}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_DECLARATION_REF
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_EXACT_DIGEST
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_TEMPLATE_FINGERPRINT
public static final io.github.testlens.selector.engine.SimilarityResult$Signal MATCHES_SAME_EXPLICIT_PATTERN
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_STRUCTURAL_FAMILY
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_PREFIX_FAMILY
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_APPEARANCE_FAMILY
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_STRATEGY
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_COMPONENT_REF
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_COMPONENT_ROLE
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_CONTEXT_FINGERPRINT
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_DECLARING_SYMBOL
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_LOGICAL_PATH
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_MODULE
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_USAGE_SOURCE_FAMILY
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_CANDIDATE_ORIGIN
public static final io.github.testlens.selector.engine.SimilarityResult$Signal SAME_SEMANTIC_ATTRIBUTE_FAMILY
public static io.github.testlens.selector.engine.SimilarityResult$Signal[] values()
public static io.github.testlens.selector.engine.SimilarityResult$Signal valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SimilarityResult` {#io-github-testlens-selector-engine-similarityresult}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public static final int ALGORITHM_VERSION
public io.github.testlens.selector.engine.SimilarityResult(int, int, java.lang.String, io.github.testlens.selector.engine.SimilarityResult$QueryScope, io.github.testlens.selector.engine.SimilarityResult$Completeness, java.util.List<io.github.testlens.selector.engine.SimilarityResult$RelatedSubject>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public int algorithmVersion()
public java.lang.String sourceSubjectRef()
public io.github.testlens.selector.engine.SimilarityResult$QueryScope scope()
public io.github.testlens.selector.engine.SimilarityResult$Completeness completeness()
public java.util.List<io.github.testlens.selector.engine.SimilarityResult$RelatedSubject> relatedSubjects()
```

## `io.github.testlens.selector.engine.SimilaritySubject$Confidence` {#io-github-testlens-selector-engine-similaritysubject-confidence}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SimilaritySubject$Confidence EXACT
public static final io.github.testlens.selector.engine.SimilaritySubject$Confidence STRONG
public static final io.github.testlens.selector.engine.SimilaritySubject$Confidence AMBIGUOUS
public static final io.github.testlens.selector.engine.SimilaritySubject$Confidence UNKNOWN
public static io.github.testlens.selector.engine.SimilaritySubject$Confidence[] values()
public static io.github.testlens.selector.engine.SimilaritySubject$Confidence valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SimilaritySubject$Correlation` {#io-github-testlens-selector-engine-similaritysubject-correlation}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SimilaritySubject$Correlation(io.github.testlens.selector.engine.SimilaritySubject$Confidence, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.SimilaritySubject$Confidence confidence()
public java.util.List<java.lang.String> supportingSignals()
public java.util.List<java.lang.String> conflictingSignals()
public java.util.List<java.lang.String> missingData()
public int algorithmVersion()
```

## `io.github.testlens.selector.engine.SimilaritySubject$SourceState` {#io-github-testlens-selector-engine-similaritysubject-sourcestate}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.SimilaritySubject$SourceState CURRENT
public static final io.github.testlens.selector.engine.SimilaritySubject$SourceState STALE_SOURCE_REVISION
public static final io.github.testlens.selector.engine.SimilaritySubject$SourceState UNRESOLVED_DECLARATION
public static final io.github.testlens.selector.engine.SimilaritySubject$SourceState CROSS_BUILD
public static final io.github.testlens.selector.engine.SimilaritySubject$SourceState UNKNOWN
public static io.github.testlens.selector.engine.SimilaritySubject$SourceState[] values()
public static io.github.testlens.selector.engine.SimilaritySubject$SourceState valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.SimilaritySubject$UsageReference` {#io-github-testlens-selector-engine-similaritysubject-usagereference}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SimilaritySubject$UsageReference(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.Integer)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String usageRef()
public java.lang.String logicalPath()
public java.lang.String usageClass()
public java.lang.String usageMethod()
public java.lang.Integer line()
```

## `io.github.testlens.selector.engine.SimilaritySubject` {#io-github-testlens-selector-engine-similaritysubject}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.SimilaritySubject(io.github.testlens.selector.engine.SelectorSubject, java.lang.String, io.github.testlens.selector.engine.ComponentIdentity, java.lang.String, io.github.testlens.selector.engine.StructuralFamily, java.util.List<io.github.testlens.selector.engine.AppearanceSignal>, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.engine.SimilaritySubject$UsageReference>, io.github.testlens.selector.engine.SimilaritySubject$Correlation, io.github.testlens.selector.engine.SimilaritySubject$SourceState)
public static io.github.testlens.selector.engine.SimilaritySubject create(io.github.testlens.selector.engine.SelectorSubject, io.github.testlens.selector.engine.ComponentIdentity, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.engine.SimilaritySubject$UsageReference>, io.github.testlens.selector.engine.SimilaritySubject$Correlation, io.github.testlens.selector.engine.SimilaritySubject$SourceState)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.SelectorSubject subject()
public java.lang.String subjectRef()
public io.github.testlens.selector.engine.ComponentIdentity component()
public java.lang.String templateFingerprint()
public io.github.testlens.selector.engine.StructuralFamily structuralFamily()
public java.util.List<io.github.testlens.selector.engine.AppearanceSignal> appearanceSignals()
public java.lang.String exactDigest()
public java.util.List<java.lang.String> explicitPatternRefs()
public java.util.List<java.lang.String> candidateOrigins()
public java.lang.String semanticAttributeFamily()
public java.lang.String usageSourceFamily()
public java.util.List<io.github.testlens.selector.engine.SimilaritySubject$UsageReference> usages()
public io.github.testlens.selector.engine.SimilaritySubject$Correlation correlation()
public io.github.testlens.selector.engine.SimilaritySubject$SourceState sourceState()
```

## `io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition` {#io-github-testlens-selector-engine-stabilityassessment-effectivedisposition}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition INSUFFICIENT_DATA
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition NO_APPEARANCE_SIGNAL
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition REVIEW_GENERATED_LOOKING
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition DECLARED_STABLE
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition DECLARED_UNSTABLE
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition OBSERVED_VARIABLE
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition POLICY_POLICY_CONFLICT
public static final io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition POLICY_EVIDENCE_CONFLICT
public static io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition[] values()
public static io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.StabilityAssessment$EngineMetadata` {#io-github-testlens-selector-engine-stabilityassessment-enginemetadata}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.StabilityAssessment$EngineMetadata(java.lang.String, java.lang.String, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String name()
public java.lang.String version()
public int detectorCatalogVersion()
public int canonicalizationVersion()
```

## `io.github.testlens.selector.engine.StabilityAssessment$Explanation` {#io-github-testlens-selector-engine-stabilityassessment-explanation}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.StabilityAssessment$Explanation(java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String reasonCode()
public java.util.List<java.lang.String> detectorIds()
public java.util.List<java.lang.String> ruleIds()
public java.util.List<java.lang.String> evidenceRefs()
public java.lang.String message()
```

## `io.github.testlens.selector.engine.StabilityAssessment$Issue` {#io-github-testlens-selector-engine-stabilityassessment-issue}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.StabilityAssessment$Issue(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String detail()
```

## `io.github.testlens.selector.engine.StabilityAssessment$PolicyEvaluation` {#io-github-testlens-selector-engine-stabilityassessment-policyevaluation}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.StabilityAssessment$PolicyEvaluation(java.util.List<java.lang.String>, java.lang.String, io.github.testlens.selector.engine.SelectorPolicy$Decision, boolean, java.util.List<java.lang.String>)
public static io.github.testlens.selector.engine.StabilityAssessment$PolicyEvaluation none()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<java.lang.String> matchedRuleIds()
public java.lang.String selectedRuleId()
public io.github.testlens.selector.engine.SelectorPolicy$Decision selectedDecision()
public boolean conflict()
public java.util.List<java.lang.String> conflictingRuleIds()
```

## `io.github.testlens.selector.engine.StabilityAssessment$Validation` {#io-github-testlens-selector-engine-stabilityassessment-validation}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.StabilityAssessment$Validation NOT_EVALUATED
public static io.github.testlens.selector.engine.StabilityAssessment$Validation[] values()
public static io.github.testlens.selector.engine.StabilityAssessment$Validation valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.StabilityAssessment` {#io-github-testlens-selector-engine-stabilityassessment}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.StabilityAssessment(int, io.github.testlens.selector.engine.StabilityAssessment$EngineMetadata, io.github.testlens.selector.engine.SelectorSubject, java.util.List<io.github.testlens.selector.engine.AppearanceSignal>, io.github.testlens.selector.engine.ObservationEvidence, io.github.testlens.selector.engine.StabilityAssessment$PolicyEvaluation, io.github.testlens.selector.engine.StabilityAssessment$Validation, io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition, java.util.List<io.github.testlens.selector.engine.StabilityAssessment$Issue>, java.util.List<io.github.testlens.selector.engine.StabilityAssessment$Explanation>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public io.github.testlens.selector.engine.StabilityAssessment$EngineMetadata engine()
public io.github.testlens.selector.engine.SelectorSubject subject()
public java.util.List<io.github.testlens.selector.engine.AppearanceSignal> appearanceSignals()
public io.github.testlens.selector.engine.ObservationEvidence evidenceSummary()
public io.github.testlens.selector.engine.StabilityAssessment$PolicyEvaluation policyEvaluation()
public io.github.testlens.selector.engine.StabilityAssessment$Validation validation()
public io.github.testlens.selector.engine.StabilityAssessment$EffectiveDisposition effectiveDisposition()
public java.util.List<io.github.testlens.selector.engine.StabilityAssessment$Issue> issues()
public java.util.List<io.github.testlens.selector.engine.StabilityAssessment$Explanation> explanations()
```

## `io.github.testlens.selector.engine.StructuralFamily$Kind` {#io-github-testlens-selector-engine-structuralfamily-kind}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.StructuralFamily$Kind LITERAL_DIGEST
public static final io.github.testlens.selector.engine.StructuralFamily$Kind UUID_LIKE
public static final io.github.testlens.selector.engine.StructuralFamily$Kind DECIMAL_RUN
public static final io.github.testlens.selector.engine.StructuralFamily$Kind HEX_RUN
public static io.github.testlens.selector.engine.StructuralFamily$Kind[] values()
public static io.github.testlens.selector.engine.StructuralFamily$Kind valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.StructuralFamily$Match` {#io-github-testlens-selector-engine-structuralfamily-match}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.engine.StructuralFamily$Match MATCH
public static final io.github.testlens.selector.engine.StructuralFamily$Match NO_MATCH
public static final io.github.testlens.selector.engine.StructuralFamily$Match UNSUPPORTED_FROM_DIGEST_ONLY
public static io.github.testlens.selector.engine.StructuralFamily$Match[] values()
public static io.github.testlens.selector.engine.StructuralFamily$Match valueOf(java.lang.String)
```

## `io.github.testlens.selector.engine.StructuralFamily$Segment` {#io-github-testlens-selector-engine-structuralfamily-segment}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.StructuralFamily$Segment(io.github.testlens.selector.engine.StructuralFamily$Kind, int, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.engine.StructuralFamily$Kind kind()
public int length()
public java.lang.String literalDigest()
```

## `io.github.testlens.selector.engine.StructuralFamily` {#io-github-testlens-selector-engine-structuralfamily}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int VERSION
public io.github.testlens.selector.engine.StructuralFamily(int, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.engine.StructuralFamily$Segment>, java.lang.String, java.lang.String)
public static io.github.testlens.selector.engine.StructuralFamily from(io.github.testlens.selector.engine.SelectorSubject, io.github.testlens.selector.engine.ComponentIdentity, io.github.testlens.selector.engine.PatternProposal$Result)
public io.github.testlens.selector.engine.StructuralFamily$Match matchDigestOnly(io.github.testlens.selector.engine.SelectorPolicy$StructuralPattern)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int version()
public java.lang.String strategy()
public java.lang.String componentRef()
public java.util.List<io.github.testlens.selector.engine.StructuralFamily$Segment> segments()
public java.lang.String fingerprint()
public java.lang.String prefixFingerprint()
```

## `io.github.testlens.selector.engine.TargetSnapshot$AncestorHint` {#io-github-testlens-selector-engine-targetsnapshot-ancestorhint}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.TargetSnapshot$AncestorHint(int, java.lang.String, java.lang.String, java.util.Map<java.lang.String, java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int depth()
public java.lang.String tagName()
public java.lang.String id()
public java.util.Map<java.lang.String, java.lang.String> testAttributes()
public java.util.List<java.lang.String> classTokens()
```

## `io.github.testlens.selector.engine.TargetSnapshot` {#io-github-testlens-selector-engine-targetsnapshot}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int MAX_TEXT_CODE_POINTS
public static final int MAX_LOCATOR_CODE_POINTS
public static final int MAX_CLASS_TOKENS
public static final int MAX_TEST_ATTRIBUTES
public static final int MAX_ANCESTOR_DEPTH
public static final int MAX_SNAPSHOT_BYTES
public io.github.testlens.selector.engine.TargetSnapshot(java.lang.String, java.lang.String, java.lang.String, java.util.List<java.lang.String>, java.util.Map<java.lang.String, java.lang.String>, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.engine.TargetSnapshot$AncestorHint>, int, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String tagName()
public java.lang.String id()
public java.lang.String name()
public java.util.List<java.lang.String> classTokens()
public java.util.Map<java.lang.String, java.lang.String> testAttributes()
public java.lang.String visibleText()
public java.lang.String accessibleName()
public java.lang.String ariaRole()
public java.util.List<io.github.testlens.selector.engine.TargetSnapshot$AncestorHint> ancestors()
public int instrumentationNodesExcluded()
public boolean truncated()
```

## `io.github.testlens.selector.engine.UseOnceSelection` {#io-github-testlens-selector-engine-useonceselection}

- Artifact/module: `selenium-test-lens-selector-engine`
- Package: `io.github.testlens.selector.engine`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.engine.UseOnceSelection(java.lang.String, io.github.testlens.selector.engine.SelectorSubject, io.github.testlens.selector.engine.SelectorPolicy$Decision)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String decisionContextId()
public io.github.testlens.selector.engine.SelectorSubject subject()
public io.github.testlens.selector.engine.SelectorPolicy$Decision decision()
```
