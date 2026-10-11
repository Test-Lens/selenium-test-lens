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

## `io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentFinding` {#io-github-testlens-core-redaction-redactionpolicy-sensitivecontentfinding}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.redaction`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/security/redaction.md](../../security/redaction.md)

```java
public io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentFinding(io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule rule()
public java.lang.String category()
```

## `io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule` {#io-github-testlens-core-redaction-redactionpolicy-sensitivecontentrule}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.redaction`
- Classification: `USER_API`
- Type kind: `enum`
- Functional documentation: [docs/security/redaction.md](../../security/redaction.md)

```java
public static final io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule AUTHORIZATION_CREDENTIAL
public static final io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule CREDENTIAL_URL
public static final io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule JWT
public static final io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule LITERAL_SECRET
public static final io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule SENSITIVE_KEY_VALUE
public static final io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule SENSITIVE_JSON_KEY_VALUE
public static io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule[] values()
public static io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentRule valueOf(java.lang.String)
```

## `io.github.testlens.core.redaction.RedactionPolicy$StructuredRedactionFinding` {#io-github-testlens-core-redaction-redactionpolicy-structuredredactionfinding}

- Artifact/module: `selenium-test-lens-core`
- Package: `io.github.testlens.core.redaction`
- Classification: `USER_API`
- Type kind: `record`
- Functional documentation: [docs/security/redaction.md](../../security/redaction.md)

```java
public io.github.testlens.core.redaction.RedactionPolicy$StructuredRedactionFinding(java.lang.String, java.lang.String, java.lang.String)
public final java.lang.String toString()
public final int hashCode()
public final boolean equals(java.lang.Object)
public java.lang.String rule()
public java.lang.String category()
public java.lang.String location()
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
public java.lang.String redactStructuredAgentOutput(java.lang.String, java.util.Set<java.lang.String>)
public java.util.List<io.github.testlens.core.redaction.RedactionPolicy$StructuredRedactionFinding> inspectStructuredAgentOutput(java.lang.String, java.util.Set<java.lang.String>)
public java.util.List<io.github.testlens.core.redaction.RedactionPolicy$StructuredRedactionFinding> inspectStructuredAgentOutput(java.lang.String, java.util.Set<java.lang.String>, java.util.function.BiPredicate<java.lang.String, java.lang.String>)
public java.util.List<io.github.testlens.core.redaction.RedactionPolicy$SensitiveContentFinding> inspectGeneratedSource(java.lang.String)
public java.lang.String redact(java.lang.String)
public java.lang.String redact(java.lang.String, java.lang.String)
public java.lang.String redactUrl(java.lang.String)
```
