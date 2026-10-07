---
search:
  exclude: true
---

# selenium-test-lens-selector-live: `io.github.testlens.selector.live`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selector.live.LiveCandidateAnalysis` {#io-github-testlens-selector-live-livecandidateanalysis}

- Artifact/module: `selenium-test-lens-selector-live`
- Package: `io.github.testlens.selector.live`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int MAX_RETAINED_PER_CANDIDATE
public static final int MAX_TOTAL_RETAINED
public io.github.testlens.selector.engine.CandidateAnalysis analysis()
public java.lang.String analysisId()
public org.openqa.selenium.WebElement target()
public java.lang.String documentGeneration()
public java.util.List<org.openqa.selenium.WebElement> retainedMatches(java.lang.String)
public boolean matchesNotRetainedForHighlight(java.lang.String)
public int retainedCount()
public boolean closed()
public void close()
```

## `io.github.testlens.selector.live.LiveCandidateAnalysisService` {#io-github-testlens-selector-live-livecandidateanalysisservice}

- Artifact/module: `selenium-test-lens-selector-live`
- Package: `io.github.testlens.selector.live`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.selector.live.LiveCandidateAnalysisService()
public io.github.testlens.selector.engine.CandidateAnalysis analyze(io.github.testlens.selector.live.LiveCandidateRequest)
public io.github.testlens.selector.live.LiveCandidateAnalysis analyzeRetainingMatches(io.github.testlens.selector.live.LiveCandidateRequest, java.lang.String, java.lang.String)
```

## `io.github.testlens.selector.live.LiveCandidateRequest` {#io-github-testlens-selector-live-livecandidaterequest}

- Artifact/module: `selenium-test-lens-selector-live`
- Package: `io.github.testlens.selector.live`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.live.LiveCandidateRequest(org.openqa.selenium.WebDriver, org.openqa.selenium.SearchContext, org.openqa.selenium.WebElement, io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent, org.openqa.selenium.By, java.lang.String, boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.engine.CompiledPolicySet, io.github.testlens.selector.engine.ObservationEvidence, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public org.openqa.selenium.WebDriver driver()
public org.openqa.selenium.SearchContext searchContext()
public org.openqa.selenium.WebElement target()
public io.github.testlens.selector.engine.CandidateAnalysis$UsageIntent usageIntent()
public org.openqa.selenium.By originalBy()
public java.lang.String contextFingerprint()
public boolean shadowContext()
public java.lang.String declarationRef()
public java.lang.String modulePath()
public java.lang.String logicalPath()
public java.lang.String declaringSymbol()
public java.lang.String usageClass()
public java.lang.String usageMethod()
public io.github.testlens.selector.engine.CompiledPolicySet policies()
public io.github.testlens.selector.engine.ObservationEvidence evidence()
public java.util.List<java.lang.String> additionalPreferredTestAttributes()
```
