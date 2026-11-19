# Prepared 0.5.2: Selenium BiDi compatibility

**0.5.2 is prepared and unpublished.** The source changes described here are implemented and tested; Maven Central, GitHub Release and versioned documentation publication have not been performed. Published 0.5.1 remains affected by Selenium's BiDi size-getter binary changes on 4.41.0 and newer. Upgrading Selenium alone cannot repair the compiled 0.5.1 JAR. Until a corrected Lens artifact is available, disable Lens diagnostic network capture in an affected 0.5.1 application.

## What changes

The network adapter resolves public Selenium size accessors and listener signatures once during initialization. It handles the primitive/boxed size boundary at Selenium 4.41 and opaque listener IDs introduced in 4.46. Nullable response sizes remain unavailable instead of becoming invented zero values; missing required request sizes fail capture. Callback mapping and linkage failures invalidate the snapshot, wake waiters and prevent assertions from passing on incomplete data.

Lens initializes lazy connections with `HasBiDi.getBiDi()`. When required, a raw `RemoteWebDriver` receives a private augmented view of the same session; application commands keep using the original driver. Lens removes its own listener IDs, preserves foreign listeners and never quits the driver during network finalization. On Selenium 4.39 through 4.45, removing a local listener can leave server event interest until the connection owner closes the connection. Later tested APIs support selective server unsubscribe.

Overlapping restarts retain the newer generation even when an earlier health check completes late. The [final release validation](maintainers/selenium-bidi-release-0.5.2.md) includes server-journal comparisons of real request/response data against JSON, HTML and failure bundles on every listed Selenium runtime.

Before a positive assertion, matched wait, ordinary wait timeout or preserving a stopped snapshot, a public `session.status` command checks the same connection outside lifecycle locks. Failure invalidates capture. The command can add **up to 30 seconds** on the 4.39 baseline, separately from the event wait deadline. A responsive command does not guarantee every event was delivered: malformed JSON rejected before Selenium dispatch and the absence of a common passive socket-close callback remain limits.

## Verified boundary

Java **17** and Selenium **4.39.0** remain the compile and minimum tested BiDi baselines. The same frozen Lens JAR was exercised against **4.39.0, 4.40.0, 4.41.0, 4.45.0, 4.46.0, 4.47.0, 4.51.0, 4.52.0 and 4.53.0**. The newest verified stable runtime is **4.53.0**, using Java 21, Chrome 157 locally and Grid 4.53.0. The richer baseline browser suite also ran on Java 17. These results do not certify every Selenium 4.x version or every browser/provider combination.

**BrowserStack: NOT RUN.** Configure its provider-specific `seleniumBidi: true` option under `bstack:options`; the returned `webSocketUrl` must be reachable by the client. Requesting the capability, or an initially empty `maybeGetBiDi()`, does not settle capture availability. Verify activation and a known response in the actual provider environment. See [network setup and provider example](advanced/network.md#browserstack-prerequisite), [compatibility policy](selenium-compatibility.md) and [audit evidence](maintainers/selenium-bidi-audit.md).

## Upgrade a consumer

Until publication, install the prepared build locally before resolving the following coordinates. After publication, use the same versions from the released repository. Update Lens runtime and optional JUnit 5/TestNG adapters together to `0.5.2`. Keep Selenium explicitly owned by the consumer and align its modules with a BOM; explicit dependency versions can override that management.

```xml
<properties>
    <selenium.version>4.53.0</selenium.version>
</properties>
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-bom</artifactId>
            <version>${selenium.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
<dependencies>
    <dependency>
        <groupId>io.github.test-lens</groupId>
        <artifactId>selenium-test-lens</artifactId>
        <version>0.5.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Inspect the resolved graph with `mvn dependency:tree -Dincludes=org.seleniumhq.selenium`. Create the browser session with `enableBiDi()` or standard `webSocketUrl=true`, including the provider's required option. Confirm `isStarted()`, `summary().status()` and `activeCaptureMode()` before relying on capture, trigger a known endpoint, and check its response. A requested `BIDI` mode alone is insufficient. No separate `selenium-bidi` artifact or matching CDP module is required by this path.

Public network signatures remain unchanged. See [getting started](getting-started.md), [JUnit 5](integrations/junit5.md) and [TestNG](integrations/testng.md) for lifecycle examples.
