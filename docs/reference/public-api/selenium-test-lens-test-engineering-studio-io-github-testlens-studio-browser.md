---
search:
  exclude: true
---

# selenium-test-lens-test-engineering-studio: `io.github.testlens.studio.browser`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.studio.browser.Browser` {#io-github-testlens-studio-browser-browser}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public static final io.github.testlens.studio.browser.Browser CHROME
public static final io.github.testlens.studio.browser.Browser FIREFOX
public static io.github.testlens.studio.browser.Browser[] values()
public static io.github.testlens.studio.browser.Browser valueOf(java.lang.String)
```

## `io.github.testlens.studio.browser.BrowserAvailability` {#io-github-testlens-studio-browser-browseravailability}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public static final io.github.testlens.studio.browser.BrowserAvailability AVAILABLE
public static final io.github.testlens.studio.browser.BrowserAvailability NOT_AVAILABLE
public static final io.github.testlens.studio.browser.BrowserAvailability UNSUPPORTED
public static final io.github.testlens.studio.browser.BrowserAvailability CONFIGURATION_INVALID
public static io.github.testlens.studio.browser.BrowserAvailability[] values()
public static io.github.testlens.studio.browser.BrowserAvailability valueOf(java.lang.String)
```

## `io.github.testlens.studio.browser.BrowserRequest` {#io-github-testlens-studio-browser-browserrequest}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public io.github.testlens.studio.browser.BrowserRequest(io.github.testlens.studio.browser.Purpose, io.github.testlens.studio.browser.Browser, io.github.testlens.studio.browser.Ownership, boolean, java.lang.String)
public io.github.testlens.studio.browser.BrowserRequest(io.github.testlens.studio.browser.Purpose, io.github.testlens.studio.browser.Browser, io.github.testlens.studio.browser.Ownership, boolean)
public static io.github.testlens.studio.browser.BrowserRequest studioOwned(io.github.testlens.studio.browser.Purpose, io.github.testlens.studio.browser.Browser)
public static io.github.testlens.studio.browser.BrowserRequest callerOwned(io.github.testlens.studio.browser.Purpose, io.github.testlens.studio.browser.Browser)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.studio.browser.Purpose purpose()
public io.github.testlens.studio.browser.Browser browser()
public io.github.testlens.studio.browser.Ownership ownership()
public boolean headless()
public java.lang.String profileId()
```

## `io.github.testlens.studio.browser.BrowserSession` {#io-github-testlens-studio-browser-browsersession}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public io.github.testlens.studio.browser.BrowserSession(org.openqa.selenium.WebDriver, io.github.testlens.studio.browser.Ownership)
public org.openqa.selenium.WebDriver driver()
public io.github.testlens.studio.browser.Ownership ownership()
public boolean closed()
public void close()
```

## `io.github.testlens.studio.browser.BrowserSessionContext$Scope` {#io-github-testlens-studio-browser-browsersessioncontext-scope}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public void close()
```

## `io.github.testlens.studio.browser.BrowserSessionContext` {#io-github-testlens-studio-browser-browsersessioncontext}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `INTERNAL`
- Type kind: `class`

```java
public static org.openqa.selenium.WebDriver currentDriver()
public static io.github.testlens.studio.browser.BrowserSessionContext$Scope bind(org.openqa.selenium.WebDriver)
```

## `io.github.testlens.studio.browser.BrowserSessionProvider` {#io-github-testlens-studio-browser-browsersessionprovider}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `USER_API`
- Type kind: `interface`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public abstract io.github.testlens.studio.browser.BrowserAvailability preflight(io.github.testlens.studio.browser.BrowserRequest)
public abstract io.github.testlens.studio.browser.BrowserSession open(io.github.testlens.studio.browser.BrowserRequest)
```

## `io.github.testlens.studio.browser.DefaultLocalBrowserSessionProvider` {#io-github-testlens-studio-browser-defaultlocalbrowsersessionprovider}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `ADVANCED_API`
- Type kind: `class`

```java
public io.github.testlens.studio.browser.DefaultLocalBrowserSessionProvider()
public io.github.testlens.studio.browser.BrowserAvailability preflight(io.github.testlens.studio.browser.BrowserRequest)
public io.github.testlens.studio.browser.BrowserSession open(io.github.testlens.studio.browser.BrowserRequest)
```

## `io.github.testlens.studio.browser.Ownership` {#io-github-testlens-studio-browser-ownership}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public static final io.github.testlens.studio.browser.Ownership STUDIO_OWNED
public static final io.github.testlens.studio.browser.Ownership CALLER_OWNED
public static io.github.testlens.studio.browser.Ownership[] values()
public static io.github.testlens.studio.browser.Ownership valueOf(java.lang.String)
```

## `io.github.testlens.studio.browser.Purpose` {#io-github-testlens-studio-browser-purpose}

- Artifact/module: `selenium-test-lens-test-engineering-studio`
- Package: `io.github.testlens.studio.browser`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/ai/test-engineering-studio-getting-started.md](../../ai/test-engineering-studio-getting-started.md)

```java
public static final io.github.testlens.studio.browser.Purpose MAPPING
public static final io.github.testlens.studio.browser.Purpose TEST_EXECUTION
public static io.github.testlens.studio.browser.Purpose[] values()
public static io.github.testlens.studio.browser.Purpose valueOf(java.lang.String)
```
