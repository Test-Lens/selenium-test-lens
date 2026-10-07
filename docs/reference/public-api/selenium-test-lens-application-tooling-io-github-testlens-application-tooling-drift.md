---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.drift`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.drift.ApplicationDrift$Change` {#io-github-testlens-application-tooling-drift-applicationdrift-change}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.drift`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.drift.ApplicationDrift$Change(io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType, io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.application.tooling.drift.ApplicationDrift$SourceCorrelation, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType type()
public io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType entityType()
public java.lang.String entityId()
public java.lang.String parentId()
public java.lang.String beforeValue()
public java.lang.String afterValue()
public java.lang.String reason()
public io.github.testlens.application.tooling.drift.ApplicationDrift$SourceCorrelation sourceCorrelation()
public java.util.List<java.lang.String> evidence()
```

## `io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType` {#io-github-testlens-application-tooling-drift-applicationdrift-changetype}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.drift`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType PAGE_ADDED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType PAGE_REMOVED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType STATE_ADDED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType STATE_REMOVED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType ELEMENT_ADDED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType ELEMENT_REMOVED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType ELEMENT_RENAMED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType SELECTOR_CHANGED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType SELECTOR_BECAME_UNSTABLE
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType SELECTOR_IMPROVED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType TRANSITION_ADDED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType TRANSITION_REMOVED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType TRANSITION_CHANGED
public static io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType[] values()
public static io.github.testlens.application.tooling.drift.ApplicationDrift$ChangeType valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType` {#io-github-testlens-application-tooling-drift-applicationdrift-entitytype}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.drift`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType PAGE
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType STATE
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType ELEMENT
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType TRANSITION
public static io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType[] values()
public static io.github.testlens.application.tooling.drift.ApplicationDrift$EntityType valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.drift.ApplicationDrift$SourceCorrelation` {#io-github-testlens-application-tooling-drift-applicationdrift-sourcecorrelation}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.drift`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$SourceCorrelation CORRELATED
public static final io.github.testlens.application.tooling.drift.ApplicationDrift$SourceCorrelation NO_SOURCE_CORRELATION
public static io.github.testlens.application.tooling.drift.ApplicationDrift$SourceCorrelation[] values()
public static io.github.testlens.application.tooling.drift.ApplicationDrift$SourceCorrelation valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.drift.ApplicationDrift` {#io-github-testlens-application-tooling-drift-applicationdrift}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.drift`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.application.tooling.drift.ApplicationDrift(int, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.tooling.drift.ApplicationDrift$Change>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String applicationId()
public java.lang.String beforeFingerprint()
public java.lang.String afterFingerprint()
public java.util.List<io.github.testlens.application.tooling.drift.ApplicationDrift$Change> changes()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.application.tooling.drift.ApplicationModelDiffer` {#io-github-testlens-application-tooling-drift-applicationmodeldiffer}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.drift`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.drift.ApplicationModelDiffer()
public io.github.testlens.application.tooling.drift.ApplicationDrift compare(io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.model.ApplicationModel)
```
