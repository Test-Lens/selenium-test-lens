---
search:
  exclude: true
---

# selenium-test-lens-junit5: `io.github.testlens.junit5`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.junit5.TestLensExtension$Builder` {#io-github-testlens-junit5-testlensextension-builder}

- Artifact/module: `selenium-test-lens-junit5`
- Package: `io.github.testlens.junit5`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/integrations/junit5.md](../../integrations/junit5.md)

```java
public io.github.testlens.junit5.TestLensExtension$Builder lensOptions(io.github.testlens.TestLensOptions)
public io.github.testlens.junit5.TestLensExtension$Builder sessionName(java.util.function.Function<org.junit.jupiter.api.extension.ExtensionContext, java.lang.String>)
public io.github.testlens.junit5.TestLensExtension build()
```

## `io.github.testlens.junit5.TestLensExtension` {#io-github-testlens-junit5-testlensextension}

- Artifact/module: `selenium-test-lens-junit5`
- Package: `io.github.testlens.junit5`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/integrations/junit5.md](../../integrations/junit5.md)

```java
public static io.github.testlens.junit5.TestLensExtension$Builder builder(java.util.function.Supplier<? extends org.openqa.selenium.WebDriver>)
public void beforeEach(org.junit.jupiter.api.extension.ExtensionContext) throws java.lang.Exception
public void afterEach(org.junit.jupiter.api.extension.ExtensionContext) throws java.lang.Exception
public boolean supportsParameter(org.junit.jupiter.api.extension.ParameterContext, org.junit.jupiter.api.extension.ExtensionContext)
public java.lang.Object resolveParameter(org.junit.jupiter.api.extension.ParameterContext, org.junit.jupiter.api.extension.ExtensionContext)
```
