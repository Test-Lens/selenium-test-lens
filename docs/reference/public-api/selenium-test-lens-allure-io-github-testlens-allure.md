---
search:
  exclude: true
---

# selenium-test-lens-allure: `io.github.testlens.allure`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.allure.AllureAttachResult` {#io-github-testlens-allure-allureattachresult}

- Artifact/module: `selenium-test-lens-allure`
- Package: `io.github.testlens.allure`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/integrations/allure.md](../../integrations/allure.md)

```java
public io.github.testlens.allure.AllureAttachResult(io.github.testlens.allure.AllureAttachStatus, int, int, java.util.List<java.lang.String>, java.util.List<java.lang.String>)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.allure.AllureAttachStatus status()
public int attachedCount()
public int skippedCount()
public java.util.List<java.lang.String> missingArtifacts()
public java.util.List<java.lang.String> failures()
```

## `io.github.testlens.allure.AllureAttachStatus` {#io-github-testlens-allure-allureattachstatus}

- Artifact/module: `selenium-test-lens-allure`
- Package: `io.github.testlens.allure`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/integrations/allure.md](../../integrations/allure.md)

```java
public static final io.github.testlens.allure.AllureAttachStatus ATTACHED
public static final io.github.testlens.allure.AllureAttachStatus PARTIALLY_ATTACHED
public static final io.github.testlens.allure.AllureAttachStatus SKIPPED_NO_ACTIVE_CONTEXT
public static final io.github.testlens.allure.AllureAttachStatus SKIPPED_ALREADY_ATTACHED
public static final io.github.testlens.allure.AllureAttachStatus SKIPPED_BY_POLICY
public static final io.github.testlens.allure.AllureAttachStatus SKIPPED_NO_AVAILABLE_ARTIFACTS
public static final io.github.testlens.allure.AllureAttachStatus FAILED
public static io.github.testlens.allure.AllureAttachStatus[] values()
public static io.github.testlens.allure.AllureAttachStatus valueOf(java.lang.String)
```

## `io.github.testlens.allure.AllureTestLens` {#io-github-testlens-allure-alluretestlens}

- Artifact/module: `selenium-test-lens-allure`
- Package: `io.github.testlens.allure`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/integrations/allure.md](../../integrations/allure.md)

```java
public static io.github.testlens.allure.AllureAttachResult attach(io.github.testlens.TestLensFinalizationResult)
public static io.github.testlens.allure.AllureAttachResult attach(io.github.testlens.TestLensFinalizationResult, io.github.testlens.allure.AllureTestLensOptions)
```

## `io.github.testlens.allure.AllureTestLensOptions$Builder` {#io-github-testlens-allure-alluretestlensoptions-builder}

- Artifact/module: `selenium-test-lens-allure`
- Package: `io.github.testlens.allure`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/integrations/allure.md](../../integrations/allure.md)

```java
public io.github.testlens.allure.AllureTestLensOptions$Builder()
public io.github.testlens.allure.AllureTestLensOptions$Builder attachDiagnosticScreenshot(boolean)
public io.github.testlens.allure.AllureTestLensOptions$Builder attachCleanScreenshot(boolean)
public io.github.testlens.allure.AllureTestLensOptions$Builder attachHtmlReport(boolean)
public io.github.testlens.allure.AllureTestLensOptions$Builder attachTrace(boolean)
public io.github.testlens.allure.AllureTestLensOptions$Builder attachFailureBundle(boolean)
public io.github.testlens.allure.AllureTestLensOptions$Builder attachNonFailedSessions(boolean)
public io.github.testlens.allure.AllureTestLensOptions build()
```

## `io.github.testlens.allure.AllureTestLensOptions` {#io-github-testlens-allure-alluretestlensoptions}

- Artifact/module: `selenium-test-lens-allure`
- Package: `io.github.testlens.allure`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/integrations/allure.md](../../integrations/allure.md)

```java
public io.github.testlens.allure.AllureTestLensOptions(boolean, boolean, boolean, boolean, boolean, boolean)
public static io.github.testlens.allure.AllureTestLensOptions defaults()
public static io.github.testlens.allure.AllureTestLensOptions$Builder builder()
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public boolean diagnosticScreenshot()
public boolean cleanScreenshot()
public boolean htmlReport()
public boolean trace()
public boolean failureBundle()
public boolean nonFailedSessions()
```
