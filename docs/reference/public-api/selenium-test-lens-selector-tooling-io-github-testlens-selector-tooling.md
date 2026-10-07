---
search:
  exclude: true
---

# selenium-test-lens-selector-tooling: `io.github.testlens.selector.tooling`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification` {#io-github-testlens-selector-tooling-existingprojectindex-classclassification}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification PAGE_OBJECT
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification COMPONENT
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification BASE_PAGE
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification TEST_HELPER
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification UNKNOWN
public static io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification[] values()
public static io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$ClassEntry` {#io-github-testlens-selector-tooling-existingprojectindex-classentry}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$ClassEntry(java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification, io.github.testlens.selector.tooling.ExistingProjectIndex$Origin, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String logicalPath()
public java.lang.String qualifiedName()
public java.lang.String simpleName()
public io.github.testlens.selector.tooling.ExistingProjectIndex$ClassClassification classification()
public io.github.testlens.selector.tooling.ExistingProjectIndex$Origin origin()
public java.util.List<java.lang.String> extendsTypes()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness` {#io-github-testlens-selector-tooling-existingprojectindex-completeness}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness COMPLETE
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness PARTIAL
public static io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness[] values()
public static io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$Edge` {#io-github-testlens-selector-tooling-existingprojectindex-edge}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$Edge(java.lang.String, io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType type()
public java.lang.String fromId()
public java.lang.String toId()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType` {#io-github-testlens-selector-tooling-existingprojectindex-edgetype}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType TEST_TO_METHOD
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType METHOD_TO_METHOD
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType METHOD_TO_DECLARATION
public static io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType[] values()
public static io.github.testlens.selector.tooling.ExistingProjectIndex$EdgeType valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$ElementEntry` {#io-github-testlens-selector-tooling-existingprojectindex-elemententry}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$ElementEntry(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.tooling.ExistingProjectIndex$ValueProjection, io.github.testlens.selector.tooling.ExistingProjectIndex$SourceRange)
public io.github.testlens.selector.tooling.ExistingProjectIndex$ElementEntry(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.tooling.ExistingProjectIndex$ValueProjection, io.github.testlens.selector.tooling.ExistingProjectIndex$SourceRange, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String ownerClassId()
public java.lang.String name()
public java.lang.String declarationRef()
public java.lang.String strategy()
public io.github.testlens.selector.tooling.ExistingProjectIndex$ValueProjection valueProjection()
public io.github.testlens.selector.tooling.ExistingProjectIndex$SourceRange range()
public java.lang.String declarationFingerprint()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$Limitation` {#io-github-testlens-selector-tooling-existingprojectindex-limitation}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$Limitation(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String code()
public java.lang.String logicalPath()
public java.lang.String subject()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification` {#io-github-testlens-selector-tooling-existingprojectindex-methodclassification}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification ACTION
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification QUERY
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification ASSERTION
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification NAVIGATION
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification WORKFLOW
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification UTILITY
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification UNKNOWN
public static io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification[] values()
public static io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$MethodEntry` {#io-github-testlens-selector-tooling-existingprojectindex-methodentry}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$MethodEntry(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Parameter>, java.lang.String, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>, io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String ownerClassId()
public java.lang.String name()
public java.lang.String signature()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Parameter> parameters()
public java.lang.String returnType()
public java.util.List<java.lang.String> actions()
public java.util.List<java.lang.String> declarationRefs()
public java.util.List<java.lang.String> outgoingTypes()
public io.github.testlens.selector.tooling.ExistingProjectIndex$MethodClassification classification()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$Metrics` {#io-github-testlens-selector-tooling-existingprojectindex-metrics}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$Metrics(int, int, int, int, int, int, int, int, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int sourceRoots()
public int files()
public int declarations()
public int classes()
public int elements()
public int methods()
public int tests()
public int edges()
public int parsedFiles()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$Origin` {#io-github-testlens-selector-tooling-existingprojectindex-origin}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$Origin HAND_WRITTEN
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$Origin GENERATED_BASE
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$Origin GENERATED_EXTENSION
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$Origin UNKNOWN
public static io.github.testlens.selector.tooling.ExistingProjectIndex$Origin[] values()
public static io.github.testlens.selector.tooling.ExistingProjectIndex$Origin valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$Parameter` {#io-github-testlens-selector-tooling-existingprojectindex-parameter}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$Parameter(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String name()
public java.lang.String type()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$SourceFile` {#io-github-testlens-selector-tooling-existingprojectindex-sourcefile}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$SourceFile(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String logicalPath()
public java.lang.String contentFingerprint()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$SourceRange` {#io-github-testlens-selector-tooling-existingprojectindex-sourcerange}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$SourceRange(int, int, int, int, int, int)
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

## `io.github.testlens.selector.tooling.ExistingProjectIndex$TestEntry` {#io-github-testlens-selector-tooling-existingprojectindex-testentry}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$TestEntry(java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework, java.util.List<java.lang.String>, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String id()
public java.lang.String ownerClassId()
public java.lang.String methodId()
public io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework framework()
public java.util.List<java.lang.String> tags()
public java.util.List<java.lang.String> groups()
public java.util.List<java.lang.String> calls()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework` {#io-github-testlens-selector-tooling-existingprojectindex-testframework}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework JUNIT5
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework TESTNG
public static final io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework UNKNOWN
public static io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework[] values()
public static io.github.testlens.selector.tooling.ExistingProjectIndex$TestFramework valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex$ValueProjection` {#io-github-testlens-selector-tooling-existingprojectindex-valueprojection}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndex$ValueProjection(java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String resolution()
public java.lang.String fingerprint()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndex` {#io-github-testlens-selector-tooling-existingprojectindex}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public static final int SCHEMA_VERSION
public io.github.testlens.selector.tooling.ExistingProjectIndex(int, java.lang.String, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$ClassEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$ElementEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$MethodEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$TestEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Edge>, io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Limitation>, io.github.testlens.selector.tooling.ExistingProjectIndex$Metrics)
public io.github.testlens.selector.tooling.ExistingProjectIndex(int, java.lang.String, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$SourceFile>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$ClassEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$ElementEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$MethodEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$TestEntry>, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Edge>, io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness, java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Limitation>, io.github.testlens.selector.tooling.ExistingProjectIndex$Metrics)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.lang.String projectFingerprint()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$SourceFile> sourceFiles()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$ClassEntry> classes()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$ElementEntry> elements()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$MethodEntry> methods()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$TestEntry> tests()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Edge> edges()
public io.github.testlens.selector.tooling.ExistingProjectIndex$Completeness completeness()
public java.util.List<io.github.testlens.selector.tooling.ExistingProjectIndex$Limitation> limitations()
public io.github.testlens.selector.tooling.ExistingProjectIndex$Metrics metrics()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndexer$Bounds` {#io-github-testlens-selector-tooling-existingprojectindexer-bounds}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndexer$Bounds(int, int, int, int, int, int, int)
public static io.github.testlens.selector.tooling.ExistingProjectIndexer$Bounds defaults()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int maxSourceRoots()
public int maxFiles()
public int maxDeclarations()
public int maxTypes()
public int maxMethods()
public int maxTests()
public int maxEdges()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndexer$Request` {#io-github-testlens-selector-tooling-existingprojectindexer-request}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndexer$Request(java.nio.file.Path, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>)
public io.github.testlens.selector.tooling.ExistingProjectIndexer$Request(java.nio.file.Path, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, java.nio.charset.Charset, io.github.testlens.selector.tooling.ExistingProjectIndexer$Bounds)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path projectRoot()
public java.util.List<java.nio.file.Path> sourceRoots()
public java.util.List<java.nio.file.Path> classpathEntries()
public java.nio.charset.Charset encoding()
public io.github.testlens.selector.tooling.ExistingProjectIndexer$Bounds bounds()
```

## `io.github.testlens.selector.tooling.ExistingProjectIndexer` {#io-github-testlens-selector-tooling-existingprojectindexer}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.selector.tooling.ExistingProjectIndexer()
public static java.lang.String selectorValueFingerprint(java.lang.String)
public io.github.testlens.selector.tooling.ExistingProjectIndex index(io.github.testlens.selector.tooling.ExistingProjectIndexer$Request)
```

## `io.github.testlens.selector.tooling.SelectorAuditProjectionAdapter` {#io-github-testlens-selector-tooling-selectorauditprojectionadapter}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static io.github.testlens.selector.engine.SelectorAuditProjection project(io.github.testlens.selector.tooling.SelectorAuditModel$AuditReport)
```

## `io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$ApplyResult` {#io-github-testlens-selector-tooling-selectorpolicydraftapplier-applyresult}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$ApplyResult(io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status, java.lang.String, java.lang.String, io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status status()
public java.lang.String oldRuleId()
public java.lang.String newRuleId()
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot$Origin destination()
public java.lang.String resultingWorkspaceDigest()
public java.lang.String message()
```

## `io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status` {#io-github-testlens-selector-tooling-selectorpolicydraftapplier-status}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status APPLIED
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status ALREADY_EXISTS
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status ALREADY_REMOVED
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status STALE_DRAFT
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status ACKNOWLEDGEMENT_REQUIRED
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status POLICY_FILE_INVALID
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status PROJECT_MISMATCH
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status PATH_REJECTED
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status WRITE_FAILED
public static final io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status BLOCKED_CONFLICT
public static io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status[] values()
public static io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$Status valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$TrustedPolicyWorkspace` {#io-github-testlens-selector-tooling-selectorpolicydraftapplier-trustedpolicyworkspace}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$TrustedPolicyWorkspace(java.nio.file.Path, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.nio.file.Path projectRoot()
public java.lang.String projectFingerprint()
```

## `io.github.testlens.selector.tooling.SelectorPolicyDraftApplier` {#io-github-testlens-selector-tooling-selectorpolicydraftapplier}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `class`

```java
public io.github.testlens.selector.tooling.SelectorPolicyDraftApplier()
public io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$ApplyResult apply(io.github.testlens.selector.tooling.SelectorPolicyDraftApplier$TrustedPolicyWorkspace, io.github.testlens.selector.engine.PolicyDraft$PendingPolicyChange, java.util.Set<io.github.testlens.selector.engine.PolicyDraft$HostAcknowledgement>)
```

## `io.github.testlens.selector.tooling.SelectorPolicyWorkspace` {#io-github-testlens-selector-tooling-selectorpolicyworkspace}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final java.nio.file.Path TRACKED
public static final java.nio.file.Path LOCAL
public io.github.testlens.selector.tooling.SelectorPolicyWorkspace()
public io.github.testlens.selector.engine.PolicyWorkspaceSnapshot load(java.nio.file.Path, java.lang.String) throws java.io.IOException
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Coverage` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-coverage}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Coverage(int, int, int, int, int, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int sourceRootsRequested()
public int sourceRootsFound()
public int filesDiscovered()
public int filesParsed()
public int unresolvedSymbols()
public boolean complete()
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Declaration` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-declaration}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Declaration(java.lang.String, java.lang.String, java.lang.String, java.lang.String, io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Encoding, io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$SourceRange, io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind, java.lang.String, java.lang.String, io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape, java.lang.String, java.lang.String, io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String declarationRef()
public java.lang.String componentRef()
public java.lang.String logicalPath()
public java.lang.String fileSha256()
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Encoding encoding()
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$SourceRange range()
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind declarationKind()
public java.lang.String locatorStrategy()
public java.lang.String canonicalScalarValue()
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape expressionShape()
public java.lang.String constructIdentityDigest()
public java.lang.String declaringSymbol()
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness freshness()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-declarationkind}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind DIRECT_BY_CALL
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind BY_FIELD
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind BY_LOCAL
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind FIND_BY_ANNOTATION
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind FIND_BYS
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind FIND_ALL
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind HELPER_METHOD
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind CUSTOM
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind[] values()
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$DeclarationKind valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Encoding` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-encoding}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Encoding UTF8
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Encoding UTF8_BOM
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Encoding[] values()
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Encoding valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-expressionshape}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape DIRECT_LITERAL
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape CONSTANT_BACKED
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape PARAMETERIZED
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape CONCATENATED
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape HELPER_GENERATED
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape ANNOTATION
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape CUSTOM
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape DYNAMIC
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape UNSUPPORTED
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape[] values()
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$ExpressionShape valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-freshness}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `enum`

```java
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness CURRENT
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness CURRENT_WITH_LIMITATIONS
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness STALE
public static final io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness UNKNOWN
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness[] values()
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Freshness valueOf(java.lang.String)
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Snapshot` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-snapshot}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Snapshot(int, java.util.List<io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Declaration>, io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Coverage, java.util.List<java.lang.String>)
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Declaration declaration(java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int schemaVersion()
public java.util.List<io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Declaration> declarations()
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Coverage coverage()
public java.util.List<java.lang.String> limitations()
```

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$SourceRange` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection-sourcerange}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$SourceRange(int, int, int, int, int, int)
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

## `io.github.testlens.selector.tooling.TrustedSelectorSourceProjection` {#io-github-testlens-selector-tooling-trustedselectorsourceprojection}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int SCHEMA_VERSION
public static final int MAX_DETAILED_USE_SITES
public static io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Snapshot scan(java.nio.file.Path, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>)
```

## `io.github.testlens.selector.tooling.TrustedSelectorUseGraph$Coverage` {#io-github-testlens-selector-tooling-trustedselectorusegraph-coverage}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.TrustedSelectorUseGraph$Coverage(int, int, int, int, int, boolean)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public int sourceRootsParsed()
public int sourceRootsRequested()
public int totalKnownUseCount()
public int retainedUseCount()
public int unresolvedSymbolIssues()
public boolean complete()
```

## `io.github.testlens.selector.tooling.TrustedSelectorUseGraph$Graph` {#io-github-testlens-selector-tooling-trustedselectorusegraph-graph}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.TrustedSelectorUseGraph$Graph(java.util.List<io.github.testlens.selector.tooling.TrustedSelectorUseGraph$UseSite>, io.github.testlens.selector.tooling.TrustedSelectorUseGraph$Coverage, java.util.Map<java.lang.String, java.lang.Integer>, java.util.List<java.lang.String>)
public java.util.List<io.github.testlens.selector.tooling.TrustedSelectorUseGraph$UseSite> forDeclaration(java.lang.String)
public int countForDeclaration(java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<io.github.testlens.selector.tooling.TrustedSelectorUseGraph$UseSite> useSites()
public io.github.testlens.selector.tooling.TrustedSelectorUseGraph$Coverage coverage()
public java.util.Map<java.lang.String, java.lang.Integer> declarationUseCounts()
public java.util.List<java.lang.String> issues()
```

## `io.github.testlens.selector.tooling.TrustedSelectorUseGraph$UseSite` {#io-github-testlens-selector-tooling-trustedselectorusegraph-usesite}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `record`

```java
public io.github.testlens.selector.tooling.TrustedSelectorUseGraph$UseSite(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String useSiteRef()
public java.lang.String declarationRef()
public java.lang.String logicalPath()
public java.lang.String declaringSymbol()
public java.lang.String astRole()
public int line()
public java.lang.String affectedTestRef()
```

## `io.github.testlens.selector.tooling.TrustedSelectorUseGraph` {#io-github-testlens-selector-tooling-trustedselectorusegraph}

- Artifact/module: `selenium-test-lens-selector-tooling`
- Package: `io.github.testlens.selector.tooling`
- Classification: `TOOLING`
- Type kind: `class`

```java
public static final int MAX_RETAINED_USE_SITES
public static final int MAX_DETAILED_USE_SITES
public static final int MAX_UNRESOLVED_ISSUES
public static io.github.testlens.selector.tooling.TrustedSelectorUseGraph$Graph analyze(java.nio.file.Path, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, io.github.testlens.selector.tooling.TrustedSelectorSourceProjection$Snapshot)
```
