---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.codegen`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.codegen.GeneratedPageObject$ElementBinding` {#io-github-testlens-application-tooling-codegen-generatedpageobject-elementbinding}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.codegen.GeneratedPageObject$ElementBinding(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String elementId()
public java.lang.String fieldName()
public java.lang.String candidateId()
public java.lang.String selectorQuality()
```

## `io.github.testlens.application.tooling.codegen.GeneratedPageObject` {#io-github-testlens-application-tooling-codegen-generatedpageobject}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.codegen.GeneratedPageObject(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.tooling.codegen.GeneratedPageObject$ElementBinding>, java.util.List<java.lang.String>)
public java.util.List<io.github.testlens.application.tooling.codegen.GeneratedPageObject$ElementBinding> bindings()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String generatedClassName()
public java.lang.String extensionClassName()
public java.lang.String generatedSource()
public java.lang.String extensionSource()
public java.util.List<io.github.testlens.application.tooling.codegen.GeneratedPageObject$ElementBinding> elementBindings()
public java.util.List<java.lang.String> warnings()
```

## `io.github.testlens.application.tooling.codegen.PageObjectDiff` {#io-github-testlens-application-tooling-codegen-pageobjectdiff}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.codegen.PageObjectDiff(java.lang.String, java.lang.String, java.lang.String, java.util.List<io.github.testlens.application.tooling.drift.ApplicationDrift$Change>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String pageId()
public java.lang.String beforeSource()
public java.lang.String afterSource()
public java.util.List<io.github.testlens.application.tooling.drift.ApplicationDrift$Change> semanticChanges()
```

## `io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions$ReviewSelectorPolicy` {#io-github-testlens-application-tooling-codegen-pageobjectgenerationoptions-reviewselectorpolicy}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `enum`

```java
public static final io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions$ReviewSelectorPolicy SKIP
public static final io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions$ReviewSelectorPolicy GENERATE_WITH_WARNING
public static io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions$ReviewSelectorPolicy[] values()
public static io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions$ReviewSelectorPolicy valueOf(java.lang.String)
```

## `io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions` {#io-github-testlens-application-tooling-codegen-pageobjectgenerationoptions}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions(java.lang.String, java.nio.file.Path, io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions$ReviewSelectorPolicy, boolean)
public static io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions verifiedOnly(java.lang.String, java.nio.file.Path)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String packageName()
public java.nio.file.Path outputDirectory()
public io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions$ReviewSelectorPolicy reviewSelectorPolicy()
public boolean createUserExtensions()
```

## `io.github.testlens.application.tooling.codegen.PageObjectGenerator` {#io-github-testlens-application-tooling-codegen-pageobjectgenerator}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.codegen.PageObjectGenerator()
public java.util.List<io.github.testlens.application.tooling.codegen.GeneratedPageObject> generate(io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions)
public java.util.List<io.github.testlens.application.tooling.codegen.PageObjectDiff> diff(io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.model.ApplicationModel, io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions)
```

## `io.github.testlens.application.tooling.codegen.PageObjectSourceStore$GenerationException` {#io-github-testlens-application-tooling-codegen-pageobjectsourcestore-generationexception}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.codegen.PageObjectSourceStore$GenerationException(java.lang.String, java.lang.Throwable)
```

## `io.github.testlens.application.tooling.codegen.PageObjectSourceStore$WriteResult` {#io-github-testlens-application-tooling-codegen-pageobjectsourcestore-writeresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.codegen.PageObjectSourceStore$WriteResult(java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>, java.util.List<java.nio.file.Path>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.util.List<java.nio.file.Path> generatedBases()
public java.util.List<java.nio.file.Path> createdExtensions()
public java.util.List<java.nio.file.Path> preservedExtensions()
```

## `io.github.testlens.application.tooling.codegen.PageObjectSourceStore` {#io-github-testlens-application-tooling-codegen-pageobjectsourcestore}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.codegen.PageObjectSourceStore()
public io.github.testlens.application.tooling.codegen.PageObjectSourceStore$WriteResult write(io.github.testlens.application.tooling.codegen.PageObjectGenerationOptions, java.util.List<io.github.testlens.application.tooling.codegen.GeneratedPageObject>)
```

## `io.github.testlens.application.tooling.codegen.SelectorJavaExpression` {#io-github-testlens-application-tooling-codegen-selectorjavaexpression}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.codegen`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static java.lang.String byExpression(io.github.testlens.application.model.ApplicationModel$SelectorProjection)
public static java.lang.String byExpression(java.lang.String, java.lang.String)
```
