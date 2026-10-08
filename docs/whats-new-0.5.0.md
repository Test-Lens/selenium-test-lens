# Test Lens 0.5.0

Test Lens 0.5.0 adds an opt-in Test Engineering toolchain around the existing lightweight Selenium runtime. It observes a real application, correlates that model with existing Page Objects, prepares bounded context for an external agent, compiles and executes a targeted test in an isolated JVM, and presents evidence for human review.

## Test Engineering Studio

Launch Studio from an existing Maven project:

```bash
mvn io.github.test-lens:test-lens-test-engineering-maven-plugin:0.5.0:studio
```

The loopback-only Studio guides project scanning, current-page or guided mapping, Page Object correlation, requirement and plan review, Page-Object-only test proposals, targeted execution, diagnosis, and repair review. Workflows and review decisions persist under the local `.test-lens/` workspace and can be resumed without silently rerunning an agent, browser test, or repair.

## Application and source intelligence

- A versioned, deterministic `ApplicationModel` records pages, states, meaningful elements, transitions, provenance, coverage, and limitations without retaining form values or authentication secrets.
- Existing Java Page Objects, locator declarations, methods, inheritance, and JUnit/TestNG usages are indexed conservatively.
- Correlation uses selector and live same-target evidence; names alone never establish identity.
- The source usage graph exposes the methods and tests affected by a locator or Page Object change.

The published selector and application artifacts are tooling dependencies. Their technically public implementation classes are not all part of the stable Java compatibility promise.

## Bounded agent workflow

Studio sends a task-specific projection rather than an entire repository or raw DOM. Provider-neutral contracts separate the architect, implementer, verifier, stabilizer, and reviewer roles. The default Codex CLI adapter is an external subprocess integration; CI can use the deterministic scripted provider.

Generated tests are checked before compilation. Raw selectors, direct `WebDriver.findElement(...)`, `Thread.sleep(...)`, direct JavaScript execution, and writes outside the approved test target are rejected by default.

## Execution and repair safety

Targeted JUnit execution runs in a child JVM. Its absolute deadline covers class loading, static initialization, setup, test execution, Lens finalization, and cleanup. Timeout or `System.exit(...)` terminates only the child boundary, and the parent returns a bounded structured `TestExecutionResult`.

A selector replacement is never invented by an agent. Selector Intelligence generates, live-validates, same-target checks, and ranks candidates. The safety boundary is explicit:

```text
AI proposes
→ Lens validates
→ human approves or rejects
→ trusted host revalidates and applies
```

Creating a `RepairProposal` does not modify source. Stale fingerprints, ambiguous correlation, unsafe paths, and double apply are rejected.

## Browser and agent providers

Local Chrome and Firefox sessions use Selenium Manager by default. Custom `BrowserSessionProvider` implementations support caller-specific factories while preserving explicit ownership. A provider used by forked targeted execution must be reconstructable in the child JVM; the supported programmatic form is a public provider class with a public no-argument constructor and configuration available through the documented project/local configuration boundary. Anonymous, captured, or otherwise stateful provider instances cannot be transferred to the child JVM.

Agent execution remains provider-neutral. The public provider boundary selects an executor by logical role/profile and reports availability without silently falling back to a scripted agent.

## Runtime compatibility

Existing 0.4.x-style runtime integrations require no migration. The base `selenium-test-lens` dependency does not pull in Studio, JavaParser, application mapping, the HTTP host, or the agent runner, and it starts no tooling services in the background.

Continue with [Studio getting started](ai/test-engineering-studio-getting-started.md), [Application Mapping](advanced/application-mapping/index.md), and [Selector Intelligence](features/selector-intelligence.md).
