# Installation

Selenium Test Lens 0.1.0 requires Java 17. The consuming project owns its Selenium dependency.

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens</artifactId>
    <version>0.1.0</version>
</dependency>
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
