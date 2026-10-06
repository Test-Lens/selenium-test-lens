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

