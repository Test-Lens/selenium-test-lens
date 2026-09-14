# Managed Test State & Resources

Managed Test State & Resources removes lifecycle glue around in-memory scenario data, intentionally shared run data, and temporary objects that must be deleted. It follows the existing Test Lens session boundary rather than introducing a second scenario lifecycle.

This feature is part of the `0.3.0-SNAPSHOT` development line.

## Scenario state: one physical invocation

`ScenarioStateManager` belongs to one started `TestLens` session: one parameter row, repetition, DataProvider invocation, or retry attempt. It is not keyed by a test class, method name, display name, or parameter text.

```java
lens.scenarioState().put("orderId", order.id());

String orderId = lens.scenarioState()
        .require("orderId", String.class);
```

`get(key, type)` returns an `Optional`; `require(key, type)` fails clearly when the value is missing or has an incompatible type. Values are non-null. `computeIfAbsent` atomically publishes at most one successful value inside the invocation; a failing supplier is not cached.

Parallel invocations use separate managers even when they run the same method with the same key. A retry is a fresh invocation and therefore receives fresh scenario state. Finalization clears the state, and access after `finishPassed`, `finishFailed`, or `finishSkipped` fails instead of silently opening another scope.

<!-- API SIGNATURES: io.github.testlens.ScenarioStateManager -->
```java
public <T> void put(String key, T value)
public <T> Optional<T> get(String key, Class<T> type)
public <T> T require(String key, Class<T> type)
public boolean remove(String key)
public boolean contains(String key)
public <T> T computeIfAbsent(String key, Class<T> type, Supplier<? extends T> supplier)
public int size()
```

## Suite state: explicit sharing

`SuiteStateManager` is deliberately shared inside one logical runner suite or manual run scope. It is thread-safe and supports atomic initialization:

```java
Tenant tenant = lens.suiteState().computeIfAbsent(
        "tenant",
        Tenant.class,
        this::createTenant
);
```

The JUnit 5 adapter owns one run scope in the root extension context. The TestNG adapter owns one scope per `ISuite`. Closing that runner scope clears the store, so another suite in the same JVM cannot see its values.

<!-- API SIGNATURES: io.github.testlens.SuiteStateManager -->
```java
public <T> void put(String key, T value)
public <T> Optional<T> get(String key, Class<T> type)
public <T> T require(String key, Class<T> type)
public boolean remove(String key)
public boolean contains(String key)
public <T> T computeIfAbsent(String key, Class<T> type, Supplier<? extends T> supplier)
public int size()
```

Runner-neutral code creates the boundary explicitly and attaches each invocation through it:

```java
try (TestRunScope run = TestRunScope.open()) {
    TestLens lens = run.attach(driver, options);
    lens.startSession("creates an order");

    Tenant tenant = lens.suiteState()
            .computeIfAbsent("tenant", Tenant.class, this::createTenant);

    lens.finishPassed();
}
```

Calling `suiteState()` on a Lens attached without a runner-managed or explicit `TestRunScope` fails fast. Test Lens does not substitute an unbounded JVM-global map.

<!-- API SIGNATURES: io.github.testlens.TestRunScope -->
```java
public static TestRunScope open()
public TestLens attach(WebDriver driver)
public TestLens attach(WebDriver driver, TestLensOptions options)
public SuiteStateManager suiteState()
public void close()
```

## Scenario resources: cleanup tied to finalization

Register an existing resource or create and register one in one operation:

```java
User user = lens.resources().create(
        "temporary-user",
        api::createUser,
        created -> api.deleteUser(created.id())
);
```

The factory must finish successfully before cleanup is registered. Registered callbacks run exactly once in reverse registration order (LIFO) for passed, failed, and skipped invocations. A retry cleans the first attempt's resources before its fresh invocation starts.

All callbacks are attempted even when one fails. If the test already failed, its throwable remains primary and the aggregated cleanup error is suppressed. If an otherwise passed invocation cannot clean a resource, finalization fails. Registration after cleanup starts is rejected.

Resource cleanup runs after initial failure evidence has been captured for an already failed test, preserving the browser failure state. Trace and reports are finalized afterward, so the final session outcome includes a cleanup failure. Test Lens never closes the consumer's driver through this API.

<!-- API SIGNATURES: io.github.testlens.ScenarioResourceManager -->
```java
public <T> T register(String name, T resource, ThrowingConsumer<? super T> cleanup)
public <T> T create(String name, ThrowingSupplier<? extends T> factory, ThrowingConsumer<? super T> cleanup) throws Exception
```

## Security boundary

Scenario and suite values live only in memory. Test Lens does not automatically serialize state values or resource objects into the HUD, trace, reports, failure bundles, or Allure attachments. Keys and logical resource names are not emitted per operation, and resource or callback `toString()` methods are not used for diagnostics.

Cleanup exception text that enters Test Lens diagnostic output is passed through the configured `RedactionPolicy`; the original Java throwable is retained for the test runner. Application logging remains the consumer's responsibility. These managers are lifecycle tools, not a secret vault.

## Deliberate limits

- No disk persistence or cross-JVM/distributed sharing.
- No resource dependency graph, retries, or cleanup retry loop.
- No automatic test-data generation or data feeder.
- No coupling to Managed Auth State; persisted browser authentication remains a separate subsystem.
- No special Allure adapter; Allure consumes the ordinary finalized Test Lens result.

See [JUnit 5](../integrations/junit5.md), [TestNG](../integrations/testng.md), and the [TestLens lifecycle](../reference/test-lens.md).
