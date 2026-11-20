# Selenium compatibility (0.5.2)

This policy applies to **Test Lens 0.5.2**. Maven Central Test Lens 0.5.1 was compiled with Selenium 4.39.0 and still contains the incompatible size calls. Replacing Selenium alone does not patch that JAR. Keep Lens's diagnostic network trace disabled in an affected 0.5.1 consumer until upgrading to the corrected 0.5.2 artifact; other application BiDi features can remain enabled.

!!! warning "Release-gating browser configuration: Windows stock 157"
    **Release-gating browser configuration: Windows stock Chrome/ChromeDriver 157.0.8103.78.** Stock Chrome/ChromeDriver **157.0.8103.78 on Windows** passed the unchanged nine-version Selenium matrix, local/Grid report checks and 50 standalone redirects through each transport. Required CI builds the frozen JAR once on Linux and verifies that same artifact on Windows. Linux stock 157/158 network failures remain known; this Windows result does not certify them. No mapper override or HTTPS-feature override is used in the release gate.

Final release-specific CI, artifact/signature inventory and isolated public-consumer receipts belong in the [GitHub Release](https://github.com/Test-Lens/selenium-test-lens/releases/tag/v0.5.2) and its [release verification attachment](https://github.com/Test-Lens/selenium-test-lens/releases/download/v0.5.2/release-verification.json) after actual completion. Prepared source is not an external publication receipt.

The qualified configuration uses the stock mapper bundled with Windows Chrome/ChromeDriver 157.0.8103.78. Linux stock 157/158 network integrity remains uncertified. Controlled raw-Selenium/Lens/Grid diagnostics reproduce request-start gaps and terminal-response failures on Linux; a failing stock-158 CDP trace and exact embedded mapper source establish incorrect ExtraInfo association between redirect hops for the captured case. The original loopback/high-port HTTP failures contain no HTTPS rewrite. A separate public-host HTTPS-upgrade control does not justify adding security-disabling flags to Lens. Chrome 156 was rejected because its mapper reports total transfer bytes as body size. The official npm 159 override on stable Chrome 158 also failed hosted verification; no cross-version mapper compatibility is established. See [production release evidence](maintainers/production-release-0.5.2.md) for exact runs and separate HTTP-control outcomes.

The compile and minimum tested BiDi baseline remains **Selenium 4.39.0**, Java 17. For **Lens 0.5.2**, the newest verified runtime is **Selenium 4.53.0**. The same-JAR matrix covers **4.39.0, 4.40.0, 4.41.0, 4.45.0, 4.46.0, 4.47.0, 4.51.0, 4.52.0 and 4.53.0**, executed on Java 21. Required [CI at `9f13b89b`](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44692428506) built once on Linux (1332 binary PASS; 36 browser scenarios explicitly NOT RUN there), then reused the frozen JAR on Windows (**1368 PASS, zero failures/skips**, plus 50 standalone redirects each direct/Grid 4.53.0). Windows-158 diagnostic loopback passes do not promote 158 to the release gate. BrowserStack is **NOT RUN**. Earlier Selenium releases and every intermediate 4.x version are not certified. The explicit runtime matrix is maintained in [`consumer-tests/selenium-bidi/matrix.json`](https://github.com/Test-Lens/selenium-test-lens/blob/v0.5.2/consumer-tests/selenium-bidi/matrix.json). Browser/provider support is a separate dimension from JVM compatibility.

## Keep Selenium modules aligned

Lens declares Selenium optional; the application owns its Selenium runtime. Its JUnit 5 and TestNG adapters do not select a separate BiDi version. Selenium's legacy `org.openqa.selenium.bidi` classes are packaged in `selenium-remote-driver`; do not infer that a separate `selenium-bidi` dependency is required from the Java package name.

Use a single Selenium version for `selenium-java`, `selenium-api`, `selenium-remote-driver`, browser drivers and supporting Selenium artifacts. The project imports `selenium-bom` while retaining 4.39.0 as its compile baseline. Consumers should also import the BOM for their selected **verified** runtime version:

```xml
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
```

Check the actual runtime graph, including explicit dependency versions that override BOM management:

```text
mvn dependency:tree -Dincludes=org.seleniumhq.selenium
```

For Gradle use `platform("org.seleniumhq.selenium:selenium-bom:$seleniumVersion")` and `dependencyInsight` on the test runtime classpath. Resolving a version successfully is not compatibility evidence.

## Diagnose binary linkage failures

`NoSuchMethodError` identifies the owner, method and expected JVM return type. `()J` (`long`) and `()Ljava/lang/Long;` (`Long`) are different descriptors. Autoboxing only helps when compiling source. It cannot repair an already compiled call site. Check the effective Selenium graph, loaded JAR locations and Lens release, then compare with the version-specific audit. Do not downgrade Selenium blindly when the application uses newer APIs.

The fix resolves public size accessors and listener registration/removal signatures once per adapter initialization. It supports primitive and nullable boxed sizes without per-event method discovery. Listener IDs remain opaque and Lens removes only IDs it registered. It never uses Selenium Network's global event-type cleanup on a shared connection.

In Selenium 4.39–4.45, public `removeListener(long)` removes the local callback without unsubscribing the server's event interest. That interest can remain until the connection owner closes the connection. Lens preserves foreign listeners instead of issuing a destructive event-type unsubscribe. Selenium 4.46+ supports selective server unsubscribe by subscription ID. Java listener registrations are removed in both cases.

## Verify the connection and the capture

Create the session with `enableBiDi()` or `webSocketUrl=true`. For Grid the returned `webSocketUrl` must be reachable from the client. Lens initializes lazy connections with `HasBiDi.getBiDi()`; an empty `maybeGetBiDi()` before initialization is not proof that BiDi is unavailable. Raw remote drivers are augmented privately when required by the Selenium version. See [Network diagnostics](advanced/network.md) for provider configuration.

A reachable socket or successful start does not prove that events are being mapped correctly. Assert capture activation, trigger a known endpoint and verify its response. Callback and binary incompatibility failures invalidate the capture snapshot, wake waiting callers and prevent network assertions from passing on incomplete data. Preserve failure categories and causes when diagnosing transport, subscription and callback failures. `network.fetchError` means a failed HTTP request, not a failed BiDi transport.

Before a positive network assertion, a matched wait, an ordinary wait timeout, or preserving a stopped snapshot, Lens sends the public `session.status` command over the same BiDi connection. It ignores the returned `ready` flag (that flag concerns accepting new sessions). An unsuccessful health command fails capture with transport/protocol diagnostics instead of permitting a PASS; an unsupported command is a protocol failure, not proof of a disconnected socket. The command runs outside lifecycle locks. Its Selenium timeout can add up to 30 seconds on the 4.39 baseline; this is separate from the event wait deadline. There is no public passive socket-close callback common to the tested Selenium APIs. Invalid JSON rejected by Selenium before event dispatch remains outside Lens's callback boundary; a responsive health command cannot prove delivery of every event.

Nullable **response** header/body sizes are retained as unavailable (attribute omitted), including with Selenium 4.39's primitive model. Known nullable fields are normalized in copied maps before model decoding; foreign consumers' payloads are not mutated. Required request `headersSize` is an unsigned integer in the [BiDi protocol](https://w3c.github.io/webdriver-bidi/#type-network-RequestData); a missing/null value is a protocol error and invalidates capture. Unknown size is never fabricated as zero.

## Regression gate and updates

Run from the repository root:

```powershell
./scripts/test-selenium-bidi-compatibility.ps1
# Optional real Chrome plus a loopback Grid with access to the local fixture:
./scripts/test-selenium-bidi-compatibility.ps1 -BrowserTests -GridUrl http://127.0.0.1:4444
```

The script builds Lens once against 4.39.0, copies its JAR and the core/overlay JARs, records SHA-256 hashes, and uses an independent Maven consumer for every runtime version. Consumers check loaded Lens provenance, `BuildInfo`, loaded API/remote/Chrome class locations and every Selenium artifact's resolved Maven path on the runtime classpath. Selenium's published JARs omit Maven metadata and versioned manifests, so these checks use the actual resolved paths rather than nonexistent embedded `pom.properties`. The legacy reproducer is also compiled once on 4.39.0 and demonstrates the original `NoSuchMethodError` after the size descriptor transition. Network behavior tests then exercise the fixed JAR. The gate rejects missing/skipped unit suites and unexecuted required browser scenarios. Browser tests are opt-in locally and enabled with a temporary Grid in CI. No BrowserStack credentials or paid sessions are created by this gate.

Before every Lens release, check [Maven Central Selenium metadata](https://repo.maven.apache.org/maven2/org/seleniumhq/selenium/selenium-remote-driver/maven-metadata.xml), add the newest stable version to the matrix, inspect public API/descriptor differences, and run the full gate. Retain the minimum and versions immediately before/after meaningful changes (currently 4.40/4.41 and 4.45/4.46). A new Selenium release remains unverified until the gate and relevant browser scenarios pass. Update the report and recommendation together; dependency-update automation must not promote a version to supported merely because it resolves.
