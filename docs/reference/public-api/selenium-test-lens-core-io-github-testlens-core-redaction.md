---
search:
  exclude: true
---

# selenium-test-lens-core: `io.github.testlens.core.redaction`

Generated binary-surface details. For behavior and examples, return to the [functional reference](../index.md) or follow the mapped documentation link.

## `io.github.testlens.core.redaction.RedactionPolicy$Builder` {#io-github-testlens-core-redaction-redactionpolicy-builder}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.redaction`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/security/redaction.md](../../security/redaction.md)

```java
public io.github.testlens.core.redaction.RedactionPolicy$Builder enabled(boolean)
public io.github.testlens.core.redaction.RedactionPolicy$Builder replacement(java.lang.String)
public io.github.testlens.core.redaction.RedactionPolicy$Builder sensitiveKey(java.lang.String)
public io.github.testlens.core.redaction.RedactionPolicy$Builder secret(java.lang.String)
public io.github.testlens.core.redaction.RedactionPolicy build()
```

## `io.github.testlens.core.redaction.RedactionPolicy` {#io-github-testlens-core-redaction-redactionpolicy}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.redaction`
- Classification: `USER_API`
- Type kind: `class`
- Functional documentation: [docs/security/redaction.md](../../security/redaction.md)

```java
public static io.github.testlens.core.redaction.RedactionPolicy defaults()
public static io.github.testlens.core.redaction.RedactionPolicy disabled()
public static io.github.testlens.core.redaction.RedactionPolicy$Builder builder()
public boolean enabled()
public java.lang.String replacement()
public int additionalSensitiveKeyCount()
public int literalSecretCount()
public java.lang.String redact(java.lang.String)
public java.lang.String redact(java.lang.String, java.lang.String)
public java.lang.String redactUrl(java.lang.String)
```
