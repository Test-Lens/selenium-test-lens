---
search:
  exclude: true
---

# selenium-test-lens-application-tooling: `io.github.testlens.application.tooling.store`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.application.tooling.store.ApplicationModelFingerprint` {#io-github-testlens-application-tooling-store-applicationmodelfingerprint}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.store`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static java.lang.String semantic(io.github.testlens.application.model.ApplicationModel)
```

## `io.github.testlens.application.tooling.store.ApplicationModelStore$HistoryResult` {#io-github-testlens-application-tooling-store-applicationmodelstore-historyresult}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.store`
- Classification: `INTERNAL`
- Type kind: `record`

```java
public io.github.testlens.application.tooling.store.ApplicationModelStore$HistoryResult(java.lang.String, java.nio.file.Path, boolean, int)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String semanticFingerprint()
public java.nio.file.Path path()
public boolean created()
public int removedEntries()
```

## `io.github.testlens.application.tooling.store.ApplicationModelStore$StoreException` {#io-github-testlens-application-tooling-store-applicationmodelstore-storeexception}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.store`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.store.ApplicationModelStore$StoreException(java.lang.String)
public io.github.testlens.application.tooling.store.ApplicationModelStore$StoreException(java.lang.String, java.lang.Throwable)
```

## `io.github.testlens.application.tooling.store.ApplicationModelStore` {#io-github-testlens-application-tooling-store-applicationmodelstore}

- Artifact/module: `selenium-test-lens-application-tooling`
- Package: `io.github.testlens.application.tooling.store`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public io.github.testlens.application.tooling.store.ApplicationModelStore()
public io.github.testlens.application.tooling.store.ApplicationModelStore(io.github.testlens.application.tooling.json.ApplicationModelJson)
public void write(java.nio.file.Path, io.github.testlens.application.model.ApplicationModel)
public void write(java.nio.file.Path, java.nio.file.Path, io.github.testlens.application.model.ApplicationModel)
public io.github.testlens.application.model.ApplicationModel read(java.nio.file.Path)
public io.github.testlens.application.model.ApplicationModel read(java.nio.file.Path, java.nio.file.Path)
public io.github.testlens.application.tooling.store.ApplicationModelStore$HistoryResult recordHistory(java.nio.file.Path, io.github.testlens.application.model.ApplicationModel, int)
```
