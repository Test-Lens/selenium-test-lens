---
search:
  exclude: true
---

# selenium-test-lens-selenium: `io.github.testlens.api`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.api.ApiCallActions` {#io-github-testlens-api-apicallactions}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.api`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public io.github.testlens.api.ApiCallActions(io.github.testlens.api.ApiOverlayPanel)
public <T> T callWithModal(java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.util.concurrent.Callable<T>, java.util.function.Function<T, java.lang.String>)
```

## `io.github.testlens.api.ApiOverlayContext` {#io-github-testlens-api-apioverlaycontext}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.api`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static void set(io.github.testlens.JsOverlayDebug)
public static io.github.testlens.JsOverlayDebug get()
public static void clear()
```

## `io.github.testlens.api.ApiOverlayPlan` {#io-github-testlens-api-apioverlayplan}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.api`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static void enable(boolean)
public static boolean isEnabled()
public static void clear()
public static void addPath(java.lang.String)
public static void addKey(java.lang.String)
public static java.util.List<java.lang.String> paths()
public static java.util.List<java.lang.String> keys()
```

## `io.github.testlens.api.ApiOverlayRule` {#io-github-testlens-api-apioverlayrule}

- Artifact/module: `selenium-test-lens-selenium`
- Package: `io.github.testlens.api`
- Classification: `INTERNAL_STYLE_PUBLIC`
- Type kind: `class`

```java
public static void setUrlPattern(java.lang.String)
public static boolean matches(java.lang.String)
```
