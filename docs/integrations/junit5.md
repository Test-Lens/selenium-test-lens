# JUnit 5 integration

The `selenium-test-lens-junit5` module is the recommended JUnit Jupiter integration. The coordinate below follows the unreleased `0.2.0-SNAPSHOT` source tree and therefore requires a local source build or a configured snapshot repository. It creates one `WebDriver`, `TestLens`, and trace session for every test invocation, injects the driver and Lens into the test method, finalizes diagnostics, and only then closes the driver.

## Install

Add the extension in test scope and keep Selenium explicit at the version managed by your project:

```xml
<dependency>
    <groupId>io.github.test-lens</groupId>
    <artifactId>selenium-test-lens-junit5</artifactId>
    <version>0.2.0-SNAPSHOT</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.seleniumhq.selenium</groupId>
    <artifactId>selenium-java</artifactId>
    <version>${selenium.version}</version>
    <scope>test</scope>
</dependency>
```

The extension artifact depends on `selenium-test-lens` and `junit-jupiter-api`. Its Selenium dependency is optional. JUnit engine, parameterized-test support, and Platform TestKit are not runtime dependencies of the artifact.

## Register and inject

<!-- API SIGNATURES: io.github.testlens.junit5.TestLensExtension -->
```java
public static TestLensExtension.Builder builder(java.util.function.Supplier<? extends org.openqa.selenium.WebDriver>)
```

<!-- API SIGNATURES: io.github.testlens.junit5.TestLensExtension$Builder -->
```java
public TestLensExtension.Builder lensOptions(io.github.testlens.TestLensOptions)
public TestLensExtension.Builder sessionName(java.util.function.Function<org.junit.jupiter.api.extension.ExtensionContext, java.lang.String>)
public TestLensExtension build()
```

```java
import io.github.testlens.TestLens;
import io.github.testlens.junit5.TestLensExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

class OrderTest {
    @RegisterExtension
    final TestLensExtension testLens =
            TestLensExtension.builder(ChromeDriver::new).build();

    @Test
    void savesOrder(WebDriver driver, TestLens lens) {
        driver.get("http://localhost:8080/orders/new");
        lens.getByTestId("save").click();
    }
}
```

The `driverFactory` is called exactly once for each invocation. `WebDriver` and `TestLens` parameters refer to that same invocation and the Lens is attached to that exact driver. Other parameter types are left to JUnit or other registered resolvers.

Do not call `driver.quit()` in `@AfterEach`: the extension owns the returned driver. It finalizes Lens and its JSON/HTML reports first, then calls `quit()` exactly once. `TestLens` itself still never closes the driver.

## Configure Lens and names

```java
@RegisterExtension
final TestLensExtension testLens = TestLensExtension.builder(ChromeDriver::new)
        .lensOptions(TestLensOptions.builder()
                .outputRoot(Path.of("target", "ui-test-lens"))
                .build())
        .sessionName(context -> context.getRequiredTestClass().getSimpleName()
                + " - " + context.getDisplayName())
        .build();
```

Without `sessionName(...)`, the default combines the test class, invocation display name, and a short identifier derived from JUnit's unique context ID. State isolation does not depend on that name: the extension stores invocation state under the complete unique ID.

## Outcome and cleanup mapping

| JUnit invocation outcome | Lens finalizer | Trace status |
| --- | --- | --- |
| no execution exception | `finishPassed()` | `PASSED` |
| `TestAbortedException`, including assumptions | `finishSkipped(reason)` | `SKIPPED` |
| any other `Throwable` | `finishFailed(originalFailure)` | `FAILED` |

An empty aborted message receives a safe fallback. Diagnostics returned by Lens finalization remain best-effort and do not change the JUnit result. If a failed or aborted test also encounters a driver cleanup failure, that cleanup error is suppressed on the original throwable. If a passed test cannot close its driver, the callback fails so cleanup corruption is visible.

If setup fails after driver creation, the extension closes that driver and rethrows the original setup failure; a concurrent quit failure is suppressed on it. A null driver from the factory fails setup with a clear message. `@Disabled` tests do not execute `beforeEach`, so they create neither a driver nor an empty Lens session.

## Parameterized, repeated, nested, and parallel tests

Each `@ParameterizedTest` value and each `@RepeatedTest` repetition receives a different driver, Lens, session ID, and report directory. Nested tests are isolated the same way. The same extension object can serve concurrent invocations because current state is never kept in extension fields, a global singleton, or a `ThreadLocal`; it resides in `ExtensionContext.Store` under `context.getUniqueId()`.

Your driver factory and application test data must still be parallel-safe. Do not share a driver returned by the factory across invocations.

## Recovery-retry policy

Pass the policy through `lensOptions(...)`. If `finishPassed()` raises `RetryPolicyViolationException`, the extension fails that invocation after JSON/HTML and the optional failure screenshot are produced, then quits the driver. A quit failure is suppressed on the policy violation, and the session is not finalized a second time. Parameterized and repeated invocations each evaluate their own session summary.

If a test starts `lens.network()`, all three Lens finalizers stop its active capture before session completion and before the extension calls `quit()`. To use `BIDI` or `AUTO`, the extension's driver factory must create options with `enableBiDi()`; the adapter does not retrofit the capability or fall back to manual capture.

For final `FAILED`, the extension waits for the complete failure-bundle pipeline—including reports, HUD cleanup, manifest, and ZIP—before `driver.quit()`. Partial evidence never replaces the test throwable. Configure it through the same `lensOptions(...)`; raw page source and browser console remain opt-in.
