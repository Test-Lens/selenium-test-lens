# Guided mapping end-to-end

This tutorial mirrors the repository's real-browser vertical contract. It is an integration outline for the source-only 0.5.0 development modules, not a Maven 0.4.0 quick start.

## 1. Prepare the caller-owned session

Create WebDriver through the test suite's existing lifecycle. Restore authentication state or log in with the application's normal helper. Credentials remain in the caller-owned flow and never enter mapper options or output.

```java
driver.get(baseUrl + "/login");
existingAuthenticationFlow.login(driver); // existing helper; no secret is logged
```

If the login page itself is part of the map, start before submitting it. Otherwise start after the authenticated landing page is ready.

## 2. Start bounded guided mapping

```java
ApplicationMapperOptions options = ApplicationMapperOptions.builder("Customer portal")
        .mode(ApplicationMapperOptions.Mode.GUIDED)
        .maxDiscoveredNodes(500)
        .maxActionableElements(100)
        .maxCandidateAnalyses(100)
        .maxPages(50)
        .maxPageStates(20)
        .maxTransitions(200)
        .build();

ApplicationMapper mapper = ApplicationMapper.start(driver, options);
MappingObservation first = mapper.observe();
```

Construction is lazy: it performs no browser mapping. `observe()` performs one bounded discovery of the caller-selected browsing context, invokes Selector Intelligence for retained elements, and merges the result.

## 3. Record observed transitions

Find the source element by its stable model ID, declare the action, execute the existing flow, wait for its intended result, and observe again:

```java
String customersLinkId = mapper.model().pages().stream()
        .filter(page -> page.pageId().equals(first.pageId()))
        .flatMap(page -> page.elements().stream())
        .filter(element -> "Customers".equals(element.accessibleName()))
        .map(ApplicationModel.ElementModel::elementId)
        .findFirst()
        .orElseThrow();

mapper.beginTransition(customersLinkId, ApplicationModel.Action.OPEN);
existingDashboardPage.openCustomers();
waitForCustomersPage(); // existing deterministic synchronization
MappingObservation customers = mapper.observe();
```

Repeat this only at meaningful pages or states. For an SPA modal, declare the click and observe after the modal is visible; it can become another state of the same page. Revisiting an already known page merges the observation rather than creating a duplicate.

## 4. Save the model and bounded history

```java
ApplicationModel model = mapper.model();
Path lensRoot = Path.of(".test-lens").toAbsolutePath().normalize();
ApplicationModelStore store = new ApplicationModelStore();

store.write(lensRoot, Path.of("application/application-model.json"), model);
store.recordHistory(lensRoot.resolve("application/history"), model, 10);
```

Inspect coverage and limitations before treating the file as authoritative:

```java
if (model.coverage().completeness() == ApplicationModel.Completeness.PARTIAL) {
    model.limitations().forEach(System.err::println);
}
```

## 5. Generate and inspect Page Objects

```java
PageObjectGenerationOptions generation = PageObjectGenerationOptions.verifiedOnly(
        "com.example.test.generated",
        lensRoot.resolve("generated/src/main/java")
);

List<GeneratedPageObject> generated =
        new PageObjectGenerator().generate(model, generation);
PageObjectSourceStore.WriteResult written =
        new PageObjectSourceStore().write(generation, generated);
```

Review `GeneratedPageObject.warnings()`, compile every generated base and extension against the consumer classpath, and inspect selector evidence. Put user workflows only in the preserved extension classes.

## 6. Create a bounded agent context

Use stable IDs from the model rather than natural-language search over the whole application:

```java
ContractHeader header = new ContractHeader(
        ContractHeader.SCHEMA_VERSION,
        ContractHeader.Status.READY,
        List.of("guided browser mapping"),
        List.of(),
        ContractHeader.Confidence.OBSERVED
);

AgentTask task = new AgentTask(
        header,
        AgentTask.TaskType.CREATE_TEST,
        "A logged-in user can open a customer from the customer list",
        List.of(customersPageId, customerDetailsPageId),
        List.of(),
        List.of(openCustomerElementId),
        List.of("Page Objects only", "JUnit 5", "Java 17")
);

AgentContextPack context = new ContextSlicer().slice(
        model,
        task,
        pageObjectApisByPageId,
        List.of("No raw selectors", "No sleeps"),
        RedactionPolicy.defaults(),
        ContextSlicer.Limits.defaults()
);

String implementerPrompt = new PromptPackRenderer().render(
        PromptPackRenderer.Role.TEST_IMPLEMENTER,
        context,
        100_000
);
```

Review `included`, `excluded`, and `completeness`. A task about two pages should not silently include the complete application.

## 7. Implement, execute, and classify

The implementer consumes generated Page Object APIs. If a required capability is absent, it returns `PageObjectCapabilityMissing`; raw Selenium selectors are forbidden by default. Compile the proposed test, run only the relevant scenario through normal Test Lens lifecycle, and collect trace, assertions, screenshots, runtime events, and selector diagnostics as separate evidence in `TestExecutionResult`.

On failure, create a `FailureClassification` before proposing a change. Categories include product defect, test logic, selector instability, synchronization, assertion expectation, test data, environment, authentication, page-model drift, and unknown. A failure is not automatically a flaky selector.

## 8. Review repair and rerun

For confirmed selector instability, remap the affected state and compare models:

```java
ApplicationDrift drift = new ApplicationModelDiffer().compare(previousModel, currentModel);
List<PageObjectDiff> impact =
        new PageObjectGenerator().diff(previousModel, currentModel, generation);
```

Build a `RepairProposal` only from live validation, same-target, and stability evidence. Its policy is always `PROPOSE_ONLY`. Review the proposal, regenerate the owned base class, compile it, run the targeted test again, and then review the final test patch. Do not hide failures with sleeps, blanket timeout inflation, blind retries, JavaScript clicks, or silent selector replacement.

## 9. Keep artifacts bounded

Retain only the model history and context packs needed by the project. Keep `.test-lens` artifacts out of public reports unless they have been reviewed. No provider SDK or network call is required by these modules; the generated prompt pack can be consumed by Codex, an IDE agent, CI tooling, or another external orchestrator.

## Continue from an existing Selenium project

The previous steps demonstrate browser-to-model mapping. The 0.5.0 development source also contains a vertical workflow for a project that already has hand-written Page Objects and tests. The following sequence is an integration outline: supply the consuming project's real roots, classpath, runner, and lifecycle instead of copying placeholder paths.

### 10. Index the existing source

Use the Selector Audit-backed indexer. It parses each configured Java file once and projects Page Object classes, locator declarations, API methods, tests, and bounded usage edges without executing application code.

```java
ExistingProjectIndex source = new ExistingProjectIndexer().index(
        new ExistingProjectIndexer.Request(
                projectRoot,
                List.of(
                        projectRoot.resolve("src/main/java"),
                        projectRoot.resolve("src/test/java")
                ),
                compileClasspathEntries
        )
);
```

Stop and inspect `source.limitations()` when completeness is `PARTIAL`. An incomplete classpath or a source/edge bound can make a relevant call unresolved.

### 11. Correlate and inspect the usage graph

```java
PageObjectCorrelation correlation = new PageObjectCorrelator().correlate(
        model,
        source,
        CorrelationOverrides.none()
);
```

Review `AMBIGUOUS`, `NO_MATCH`, and `CONFLICT` entries. Add a stable-ID override only after a human has verified the relation. Do not resolve two same-labelled buttons by field-name similarity alone.

For a changed application element, project the source impact:

```java
SourceImpact impact = new SourceImpactAnalyzer().analyze(
        changedElementId,
        correlation,
        source,
        10_000
);
```

This returns declaration references, Page Object method IDs, test IDs, and limitations. It is a bounded syntactic graph, not a promise of complete compiler call-graph resolution.

### 12. Wrap the requirement and slice source-aware context

Create an `AgentTask` with confirmed stable page, state, and element IDs. Then use the source-aware overload:

```java
AgentContextPack context = new ContextSlicer().slice(
        model,
        task,
        pageObjectApisByPageId,
        List.of("JUnit 5 lifecycle from BaseUiTest", "Page Objects only"),
        redactionPolicy,
        ContextSlicer.Limits.defaults(),
        source,
        correlation,
        ContextSlicer.SourceLimits.defaults()
);
```

For an invalid-password requirement, the expected projection contains the login page, its correlated declarations and Page Object methods, and relevant login tests. It should exclude customer pages and unrelated tests. Check `included`, `excluded`, and `completeness` before invoking an external agent.

The host workflow wraps the same business input in `TestEngineeringRequest`: requirement and acceptance criteria, included/excluded scope, test framework, target module/class/scenario, allowed relative paths, and targeted execution policy. Do not place credentials or production test data in this object.

### 13. Plan, then implement through Page Objects

Send the context to the `TEST_ARCHITECT` role. Its structured `TestPlan` identifies existing coverage, the new or extended scenario, preconditions, actions, expected outcomes, data requirements, risk, and known unknowns. The architect does not write Java.

After plan approval, the `TEST_IMPLEMENTER` receives only the chosen scenario, projected Page Object API, nearby test conventions, and relevant tests. Validate the resulting source with `GeneratedTestPolicyValidator` before compilation. The default policy rejects raw `By`, `driver.findElement`, sleeps, direct JavaScript, retry workarounds, and paths outside the request.

If the Page Object cannot express the required action, record `PageObjectCapabilityMissing`. A `PageObjectExtensionProposal` may use an already-correlated declaration. If no suitable declaration exists, return to Selector Intelligence; do not let the generated test invent one.

### 14. Compile with source preconditions

Use `TargetedJavaCompiler` for the proposed sources. Each `SourceUnit` names a relative path, binary name, content, and the fingerprint of the content expected at that path. A concurrent edit produces `SOURCE_PRECONDITION_FAILED`. Bound the compiler diagnostics passed to a correction step and never continue to browser execution after a compile failure.

### 15. Execute only the target and collect Lens evidence

Provide a host `TargetedTestExecutor` that invokes the project's JUnit 5 or TestNG runner for the requested class/scenario and timeout. Keep the existing driver ownership and Test Lens lifecycle. Project the result into `TestExecutionResult`, keeping compile diagnostics, framework result, trace references, assertions, runtime events, screenshot references, and selector diagnostics distinct.

Do not attach raw Surefire output, environment variables, system properties, page source, cookies, auth state, console dumps, screenshots, or videos to an agent context. A trusted host can keep those artifacts locally while producing a small redacted textual projection.

### 16. Classify before changing anything

Use observed evidence to distinguish compilation, authentication, product behavior, selector instability, synchronization, Page Object capability, and unknown failures. A product defect produces no test or selector repair. A synchronization repair should add the correct condition rather than a sleep. Unknown evidence remains unknown.

For a confirmed selector failure, `SelectorRepairPlanner` accepts only an unambiguous declaration correlation plus a live-validated, unique, same-target replacement selected by the existing Selector Intelligence pipeline. It emits a `PROPOSE_ONLY` `RepairProposal` with source impact and verification plan.

### 17. Apply only at a trusted host boundary

Review the repair outside the browser. `ControlledSourceApplier` requires an explicitly trusted apply, an allowed relative path, and the expected current source fingerprint. A mismatch returns `SOURCE_PRECONDITION_FAILED`. After an accepted patch, compile and rerun only affected tests, then request `CODE_REVIEWER` approval.

### 18. Preserve the audit trail

Drive `TestEngineeringWorkflow` through legal events and retain its artifact ancestry, transitions, and bounded metrics. `WorkflowArtifactStore` writes only approved, already-redacted JSON artifact names under a validated run directory and enforces run retention. Before storage or external transport, require `AgentArtifactSecurityGate` to pass.

The final successful chain is:

```text
existing Page Objects and tests
  -> source index and usage graph
  -> evidence-bearing ApplicationModel correlation
  -> small redacted AgentContextPack
  -> TestPlan
  -> Page-Objects-only test proposal
  -> targeted compile
  -> targeted run and Test Lens evidence
  -> classification
  -> optional PROPOSE_ONLY repair and trusted apply
  -> rerun and code review
```

See [Existing Page Object correlation](existing-page-objects.md) for source-model limits and [AI workflow orchestration](../../ai/workflow-orchestration.md) for the complete state machine and security gates.
