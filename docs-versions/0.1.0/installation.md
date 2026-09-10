# Installation

**Selenium Test Lens 0.1.0 — stable**

Selenium Test Lens 0.1.0 requires Java 17. The consuming project owns its Selenium dependency.

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
```

The main artifact deliberately leaves the Selenium version to the consuming project. For example, 0.1.0 was validated with:

```xml
<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>4.39.0</version>
</dependency>
```

Gradle:

```groovy
testImplementation 'io.github.test-lens:selenium-test-lens:0.1.0'
testImplementation 'org.seleniumhq.selenium:selenium-java:4.39.0'
```

Published 0.1.0 coordinates are:

| Artifact | Purpose |
|---|---|
| `selenium-test-lens-parent` | Maven parent/BOM metadata |
| `selenium-test-lens-core` | Logging, trace, and report model |
| `selenium-test-lens-overlay` | HUD and overlay resources |
| `selenium-test-lens` | Main Selenium facade and runtime |
| `selenium-test-lens-react` | Optional React helpers |

Dedicated runner-adapter artifacts are not part of 0.1.0. Integrate the main facade with the lifecycle already owned by your test framework.

Add `selenium-test-lens-react:0.1.0` only when using the [React helpers](react-integration.md). The examples module in the source tree was not a published library artifact.
