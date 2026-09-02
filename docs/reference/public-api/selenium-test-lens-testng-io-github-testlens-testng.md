---
search:
  exclude: true
---

# selenium-test-lens-testng: `io.github.testlens.testng`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.testng.TestLensTestNg` {#io-github-testlens-testng-testlenstestng}

- Artifact/module: `selenium-test-lens-testng`
- Package: `io.github.testlens.testng`
- Classification: `USER_API`
- Type kind: `interface`
- Functional documentation: [docs/integrations/testng.md](../../integrations/testng.md)

```java
public abstract java.lang.Class<? extends io.github.testlens.testng.TestLensTestNgFactory> factory()
```

## `io.github.testlens.testng.TestLensTestNgContext` {#io-github-testlens-testng-testlenstestngcontext}

- Artifact/module: `selenium-test-lens-testng`
- Package: `io.github.testlens.testng`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/integrations/testng.md](../../integrations/testng.md)

```java
public static io.github.testlens.testng.TestLensTestNgContext current()
public org.openqa.selenium.WebDriver driver()
public io.github.testlens.TestLens lens()
public io.github.testlens.core.trace.UiTestLensSession session()
```

## `io.github.testlens.testng.TestLensTestNgFactory` {#io-github-testlens-testng-testlenstestngfactory}

- Artifact/module: `selenium-test-lens-testng`
- Package: `io.github.testlens.testng`
- Classification: `USER_API`
- Type kind: `interface`
- Functional documentation: [docs/integrations/testng.md](../../integrations/testng.md)

```java
public abstract org.openqa.selenium.WebDriver createDriver()
public default io.github.testlens.TestLensOptions lensOptions()
public default java.lang.String sessionName(org.testng.ITestResult)
```

## `io.github.testlens.testng.TestLensTestNgListener` {#io-github-testlens-testng-testlenstestnglistener}

- Artifact/module: `selenium-test-lens-testng`
- Package: `io.github.testlens.testng`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/integrations/testng.md](../../integrations/testng.md)

```java
public io.github.testlens.testng.TestLensTestNgListener()
public void beforeInvocation(org.testng.IInvokedMethod, org.testng.ITestResult)
public void afterInvocation(org.testng.IInvokedMethod, org.testng.ITestResult)
```
