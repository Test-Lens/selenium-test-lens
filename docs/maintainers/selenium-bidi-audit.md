# Selenium BiDi compatibility audit — prepared 0.5.2

Verified 2026-11-19 against Maven Central binary and source JARs, then remediated in the development tree prepared for 0.5.2. Version 0.5.2 is not published; the executed evidence below originated in the audit tree before the isolated release preparation. These results do not apply retroactively to the published 0.5.1 JAR.

Subsequent production validation holds publication for a Chrome 158 redirect-integrity failure reproduced on hosted Linux without Lens or Grid. See the current [production release evidence](production-release-0.5.2.md); the earlier compatibility results below are retained as historical milestones, not final release authorization.

## Artifacts and reproducibility

Authoritative metadata: https://repo.maven.apache.org/maven2/org/seleniumhq/selenium/selenium-remote-driver/maven-metadata.xml

Saved metadata reports `latest=release=4.53.0`, `lastUpdated=20261117180906`. All requested releases (4.39.0, 4.40.0, 4.41.0, 4.45.0, 4.46.0, 4.47.0, 4.51.0) are published. No requested version is absent. Additional boundary checks cover 4.42.0, 4.43.0, 4.44.0, 4.50.0, 4.52.0 and 4.53.0. JARs and `*-javap.txt` are in this directory.

Artifact pattern: `https://repo.maven.apache.org/maven2/org/seleniumhq/selenium/selenium-remote-driver/V/selenium-remote-driver-V.jar`; source artifact adds `-sources` before `.jar`.

Successful download command: `node target/bidi-audit/download.cjs`; additional history and 4.52 download: `node target/bidi-audit/history.cjs`. Node HTTPS succeeded. PowerShell Invoke-WebRequest and Windows curl failed TLS in this environment. An initial unquoted PowerShell Maven `-Dartifact=...` command was misparsed, so it was not used as artifact evidence.

JVM inspection command, substituting each downloaded version:

```powershell
$classes = @('org.openqa.selenium.bidi.HasBiDi','org.openqa.selenium.bidi.BiDi','org.openqa.selenium.bidi.Connection','org.openqa.selenium.bidi.module.Network','org.openqa.selenium.bidi.network.RequestData','org.openqa.selenium.bidi.network.ResponseData','org.openqa.selenium.bidi.network.BaseParameters','org.openqa.selenium.bidi.network.BeforeRequestSent','org.openqa.selenium.bidi.network.ResponseDetails','org.openqa.selenium.bidi.network.FetchError','org.openqa.selenium.bidi.network.FetchTimingInfo','org.openqa.selenium.bidi.network.Header','org.openqa.selenium.bidi.network.BytesValue')
javap -s -classpath target/bidi-audit/selenium-remote-driver-4.39.0.jar $classes
```

The complete declarations and descriptors are saved in corresponding `selenium-remote-driver-V-javap.txt` files. Source files were extracted directly with .NET ZipFile without executing Selenium.

## Exact boundaries

| Symbol | Through 4.40.0 | First 4.41.0 and all inspected later releases |
|---|---|---|
| RequestData.getHeadersSize | `()Ljava/lang/Long;` | `()J` |
| ResponseData.getHeadersSize | `()J` | `()Ljava/lang/Long;` |
| ResponseData.getBodySize | `()J` | `()Ljava/lang/Long;` |
| RequestData constructor | `(String,String,String,List,List,long,FetchTimingInfo)` | `(String,String,String,List,List,long,Long,String,String,FetchTimingInfo)`; old constructor removed |

`RequestData.getBodySize(): Long`, `getDestination(): String`, and `getInitiatorType(): String` also first appear in 4.41.0, but baseline Lens does not call them.

BiDi constructor history: 4.39 has `(Connection)V`; 4.40 removes that constructor and has only `(Connection,Duration)V`; 4.41 restores `(Connection)V` while keeping `(Connection,Duration)V`. Lens production baseline constructs Network, not BiDi or Connection. Test fixtures that construct BiDi must handle the 4.40 distinction.

Listener IDs change first in 4.46.0, verified against 4.45.0:

* `BiDi.addListener(Event,Consumer)J` becomes `(Event,Consumer)Ljava/lang/String;`; the `(String,Event,Consumer)` and `(Set,Event,Consumer)` overloads change return descriptor the same way.
* `BiDi.removeListener(J)V` becomes `(Ljava/lang/String;)V`.
* `Connection.addListener(Event,Consumer)J` becomes `(String,Event,Consumer)V`; Connection.removeListener changes long to String.
* `Network.onAuthRequired(Consumer)J` becomes `(Consumer)Ljava/lang/String;`. Lens does not use onAuthRequired.
* `Network.onBeforeRequestSent`, `onResponseCompleted`, and `onFetchError` remain `(Ljava/util/function/Consumer;)V` throughout every inspected version, including 4.53.

HasBiDi.getBiDi and maybeGetBiDi descriptors remain unchanged throughout. Both become `@Deprecated(since="4.46",forRemoval=true)` in 4.46. `HasBiDi.getHandle(): Handle` is a new abstract method first in 4.47; preexisting user implementations need not implement it unless someone calls it. Newer fake implementations used by source tests must implement it. The legacy Network constructor still uses getBiDi.

## Complete baseline Lens production symbols

Owners: `selenium-test-lens-selenium/src/main/java/io/github/testlens/selenium/network/SeleniumBiDiNetworkCaptureSource.java` and its `OfficialModule`; `BiDiDriverResolver.java`. There are no other production Java BiDi imports in the repository search.

| Owner | Used members / JVM return descriptors | Compatibility |
|---|---|---|
| Network | `<init>(WebDriver)V`; onBeforeRequestSent/onResponseCompleted/onFetchError `(Consumer)V`; close `()V` | Stable all inspected releases |
| HasBiDi | getBiDi `()Lorg/openqa/selenium/bidi/BiDi;`; maybeGetBiDi `()Ljava/util/Optional;`; instanceof/checkcast | Stable descriptors; lifecycle/deprecation changes above |
| BiDi | close `()V`; type of resolver return / owned connection | Stable |
| BiDiException | instanceof for retry classification | Class exists all inspected releases; no production constructor call |
| RequestData | getRequestId/getMethod/getUrl `()Ljava/lang/String;`; getHeaders `()Ljava/util/List;`; getTimings `()Lorg/openqa/selenium/bidi/network/FetchTimingInfo;`; getHeadersSize | Only getHeadersSize changes |
| ResponseData | getUrl/getProtocol/getStatusText/getMimeType `()Ljava/lang/String;`; getStatus `()I`; isFromCache `()Z`; getHeaders `()Ljava/util/List;`; getBytesReceived `()J`; getHeadersSize/getBodySize | Only the two size getters change |
| BaseParameters | getBrowsingContextId/getNavigationId `()Ljava/lang/String;`; getRedirectCount/getTimestamp `()J`; isBlocked `()Z`; getRequest `()Lorg/openqa/selenium/bidi/network/RequestData;` | All stable |
| BeforeRequestSent | inherited getRequest/getTimestamp | Both resolve to stable BaseParameters methods |
| ResponseDetails | inherited getRequest/getTimestamp; getResponseData `()Lorg/openqa/selenium/bidi/network/ResponseData;` | Stable |
| FetchError | inherited getRequest/getTimestamp; getErrorText `()Ljava/lang/String;` | Stable |
| FetchTimingInfo | getRequestTime/getResponseEnd `()D` | Stable |
| Header | getName `()Ljava/lang/String;`; getValue `()Lorg/openqa/selenium/bidi/network/BytesValue;` | Stable |
| BytesValue | getValue `()Ljava/lang/String;`; getType `()Lorg/openqa/selenium/bidi/network/BytesValue$Type;` | Stable |
| BytesValue.Type | BASE64 static enum field | Present all inspected releases |

The preexisting local 0.5.1 consumer JAR inspected at `target/public-consumer-smoke/repository/io/github/test-lens/selenium-test-lens/0.5.1/selenium-test-lens-0.5.1.jar` confirms all three obsolete size descriptors above, plus actual Network(WebDriver) construction. This local artifact is supporting evidence, not independent proof of Maven Central publication; the dedicated frozen 4.39 reproducer independently verifies the obsolete call sites.

```powershell
javap -p -c -s -classpath target/public-consumer-smoke/repository/io/github/test-lens/selenium-test-lens/0.5.1/selenium-test-lens-0.5.1.jar 'io.github.testlens.selenium.network.SeleniumBiDiNetworkCaptureSource' 'io.github.testlens.selenium.network.SeleniumBiDiNetworkCaptureSource$OfficialModule' 'io.github.testlens.selenium.network.BiDiDriverResolver'
```

## Lifecycle and observability

`Network.close()` never closes BiDi or its WebSocket. All inspected versions call `BiDi.clearListener(Event)` globally for beforeRequestSent, responseStarted, responseCompleted and authRequired; from 4.50 they also clear fetchError. Earlier versions leak fetchError registrations on a shared connection. This clearing affects other consumers of the same event type on the same connection, rather than only this Network instance's callbacks. Scope-aware clearListener overloads introduced elsewhere do not change this Network implementation.

In both 4.39 and 4.53 `Connection.Listener.onText` dispatches asynchronously to an executor; event mapping happens before the consumer callback. Event deserialization exceptions can therefore prevent Lens's callback from running. Catching exceptions in Lens callback logic cannot observe these failures. WebSocket onClose only logs and marks the underlying socket closed in inspected sources. A server `network.fetchError` event is a request failure, not an observation of WebSocket transport failure. A synchronous subscribe/command failure can reach caller code; later asynchronous transport/parser failures are a separate limitation.

No inspected BiDi, Connection, HasBiDi or Handle public API exposes `isConnected` or a transport-close callback. Handle methods are package-private. `BiDi.getBidiSessionStatus(): BiDiSessionStatus` is a stable public synchronous `session.status` command sent over the same WebSocket. A successful response establishes responsiveness at probe time; its `isReady()` value concerns session readiness and must not itself be treated as connection health. A failing probe can prevent assertions from passing on a disconnected capture stream, but unsupported status commands and timeouts are probe failures rather than definitive proof of closure. Selenium 4.39's BiDi timeout is fixed at 30 seconds; `BiDi.send(Command,Duration)` is public from 4.40 and can bound a manually constructed status probe in later versions. This does not detect an event-mapper failure when transport remains healthy. Browser tests must validate status support before relying on the hook.

Additional direct source boundary evidence: 4.49.0 source JAR downloaded; `4.49.0-Network.java` still omits fetchError in close, while 4.50 includes it. Script `node target/bidi-audit/check-getters.cjs` verifies all 27 non-size getters in the table across 13 inspected binary outputs: PASS.

## Official history

Release attribution verified against https://raw.githubusercontent.com/SeleniumHQ/selenium/trunk/java/CHANGELOG . GitHub PR API merge identifiers saved in `pr-N.json`.

| First release | PR / change | Merge commit |
|---|---|---|
| 4.40 | https://github.com/SeleniumHQ/selenium/pull/16796 configurable timeout | 4cd4b09913bce68512962c99ae09505f696315c8 |
| 4.41 | https://github.com/SeleniumHQ/selenium/pull/17014 restore constructor | 0f71ed6f8e980ddd8e62750666caa37c982bc18e |
| 4.41 | https://github.com/SeleniumHQ/selenium/pull/17061 network nullability / sizes | 678f21cf03a0235a59bac3063a8c5a7e37fa3151 |
| 4.46 | https://github.com/SeleniumHQ/selenium/pull/17751 deprecate connection access | 3936678078291c88270bbd0fc83a75ebea03d8ea |
| 4.46 | https://github.com/SeleniumHQ/selenium/pull/17753 RemoteWebDriver creates BiDi | 41778880e8d2eb6a1e11bdf8b9e82cb9e3298e08 |
| 4.46 | https://github.com/SeleniumHQ/selenium/pull/17759 subscription IDs | 9f6ccde23171bd076a05d2c43e8274c227e19ccf |
| 4.47 | https://github.com/SeleniumHQ/selenium/pull/17758 transport layer / Handle | ed56a5b531b4803d96b0dd254d4a257252fa8b04 |
| 4.47 | https://github.com/SeleniumHQ/selenium/pull/17792 builder initialization fix | 2e284f202ab163c0c9a0bf714a17e9105061ab75 |
| 4.50 | https://github.com/SeleniumHQ/selenium/pull/17903 close fetchError | 83906a70c0dfbb52fc838dbf58b0f81620390f3e |

## Remediation decisions

| Changed contract | First version | Lens impact | Applied solution |
|---|---|---|---|
| Request headers `()Ljava/lang/Long;` → `()J` | 4.41.0 | `NoSuchMethodError` on request mapping | Cached public method resolution; primitive/boxed results retain full long precision |
| Response headers/body `()J` → `()Ljava/lang/Long;` | 4.41.0 | Linkage failure; real nullable response sizes also fail legacy model decoding | Cached getters plus copied-map normalization of valid nullable response sizes; unknown size omitted, never zero |
| RequestData constructor and required destination/initiator fields | 4.41.0 | Older synthetic event fixtures invalid on newer parser | Lens never calls the constructor; test JSON includes the actual protocol fields |
| BiDi constructor `(Connection)V` removed temporarily | 4.40.0, restored 4.41.0 | No production call site | Use the driver-owned public BiDi connection; no version-specific constructor binding |
| BiDi listener ID return `J` → `Ljava/lang/String;`, remove parameter `J` → `Ljava/lang/String;` | 4.46.0 | Direct ID-based cleanup would break if compiled to either descriptor | Cache registration/removal methods, store opaque IDs, remove only Lens IDs |
| RemoteWebDriver exposes BiDi directly | 4.46.0 | Augmentation is no longer always necessary | Feature discovery retains native interface and lazy initialization; augment older remote views privately |
| HasBiDi abstract getHandle added | 4.47.0 | New consumer code can use it, older compiled Lens need not | No direct dependency on new Handle API; test doubles use compatible interfaces/proxies |
| Network.close adds fetchError cleanup | 4.50.0 | Earlier leak and event-type-wide foreign-listener removal | Lens uses public Event/addListener/removeListener APIs and never calls Network.close |

Reflection is limited to unstable getter and subscription descriptors and cached by public owner/method. A single implementation covers the tested versions. MethodHandles would still require dynamic signature adaptation and boxing for nullable values; cached reflection keeps this small compatibility boundary inspectable without adding dependencies. The stable event/request/response/timing APIs remain typed. No Selenium internals or version-number dispatch are used.

The callback boundary covers both official model decoding and Lens mapping. Failures retain causes, include Selenium release/category/stage, invalidate the generation, wake waits, and emit redacted failure logs. A same-connection public `session.status` probe precedes positive assertions/waits, ordinary timeout results and stop snapshot preservation. The probe ignores `ready`, runs outside lifecycle locks, and fails closed on transport/protocol errors. Baseline Selenium can spend up to 30 seconds on that command. Selenium JSON parsing before event dispatch remains outside Lens's boundary; transport responsiveness cannot guarantee complete protocol event delivery.

Independent review found and fixed a close/dispatcher lock inversion, a stopped-snapshot false PASS after disconnection, and an overlapping-stop snapshot-validity race. Deterministic regressions cover each. Required `request.headersSize` null/missing is rejected with PROTOCOL_ERROR in accordance with the [protocol](https://w3c.github.io/webdriver-bidi/#type-network-RequestData); only nullable response fields receive compatibility normalization. Shared payload maps are copied, preserving foreign listeners' data.

Cleanup limitation verified in 4.39 and 4.45 sources: `removeListener(long)` removes only the local callback. Server event interest can persist until the shared connection owner closes it; clearing by event type would also unsubscribe foreign consumers. From 4.46, `removeListener(String)` sends selective `session.unsubscribe` with the registered subscription ID and then removes the local callback. Lens uses these public semantics and never owns/ends a consumer's shared connection.

## Final artifact inspection

Prior compatibility-audit Lens SHA-256 (before 0.5.2 versioning and final hardening): `EA117AE0869471D13BE6E4E0B51CBC0E055445F2978B3B04ADC69BD9051CDFE3`. The [0.5.2 release validation](selenium-bidi-release-0.5.2.md) supersedes this artifact with the final candidate hash and report-correctness matrix.

The original frozen artifact is under `target/selenium-bidi-compatibility/38b8ab457d9c4c8b9d7ed5150ea064aa/frozen`. Subsequent consumer executions reuse these exact files after hash verification. Forty external BiDi method/field constant-pool references were checked with `javap -p -c -v` against all nine matrix releases: zero missing/mismatched descriptors. This includes the public Event constructor, three static model decoders, `getBidiSessionStatus`, inherited getters and enum fields. No direct constant-pool call remains to the unstable size/listener descriptors or Network/BiDi/Connection constructors. Twenty public network type signatures match the preexisting baseline JAR exactly.

## Changed paths and scope

- Production: `selenium-test-lens-selenium/src/main/java/io/github/testlens/selenium/network/{SeleniumBiDiCompatibility,SeleniumBiDiNetworkCaptureSource,NetworkDiagnostics,NetworkCaptureSink,NetworkCaptureSource,BiDiCaptureException,BiDiFailureCategory}.java`.
- Regression tests: the same module's `network/{SeleniumBiDiCompatibilityTest,SeleniumBiDiNetworkCaptureSourceTest,NetworkDiagnosticsBiDiTest,BiDiDriverResolverTest}.java`; richer local/Grid coverage in `selenium-test-lens-browser-tests/.../NetworkBiDiBrowserIT.java`.
- Independent consumer: `consumer-tests/selenium-bidi/` contains the explicit matrix, standalone Maven build, baseline legacy reproducer, provenance checks and real Chrome/raw-remote smoke tests. `scripts/test-selenium-bidi-compatibility.ps1` freezes artifacts once and rejects missing/skipped required tests. `.github/workflows/ci.yml` adds one gate with a temporary Grid.
- Dependencies: root `pom.xml` imports Selenium BOM while keeping 4.39.0 compilation and optional Selenium runtime ownership.
- Current development docs: README, CHANGELOG Unreleased, getting-started, Network, JUnit/TestNG integration, browser testing, compatibility policy, AI integration page and generator. Historical versioned documentation is unchanged.

Existing Studio/application/selector changes and workflow configuration deletions present at task start were preserved. No unrelated functionality was refactored.

## Executed compatibility matrix

Environment: Java 21.0.10, Windows 11, Chrome/ChromeDriver 157.0.8103.78, loopback Selenium Grid 4.53.0. Lens was compiled once on Selenium 4.39.0. Every row used the exact SHA-256 above. Each runtime executed 133 binary/unit tests and 2 real browser tests: **1,215 tests total, zero failures/errors/skips**.

| Selenium runtime | Same-JAR binary/unit | Local Chrome | Raw RemoteWebDriver / Grid | BrowserStack |
|---|---|---|---|---|
| 4.39.0 | PASS | PASS | PASS | NOT RUN |
| 4.40.0 | PASS | PASS | PASS | NOT RUN |
| 4.41.0 | PASS | PASS | PASS | NOT RUN |
| 4.45.0 | PASS | PASS | PASS | NOT RUN |
| 4.46.0 | PASS | PASS | PASS | NOT RUN |
| 4.47.0 | PASS | PASS | PASS | NOT RUN |
| 4.51.0 | PASS | PASS | PASS | NOT RUN |
| 4.52.0 | PASS | PASS | PASS | NOT RUN |
| 4.53.0 | PASS | PASS | PASS | NOT RUN |

BrowserStack was not run: no configured credentialed test session was used, and no paid resources were started. Firefox cross-version certification was not run; the existing CI browser suite remains the Firefox gate. Hosted GitHub Actions has not been invoked from this working tree; the equivalent new gate was executed locally.

Final evidence: `target/selenium-bidi-compatibility/b7191540e092410999cb25a53e796268/results.json`, per-version `maven.log` and Surefire XML; `frozen-artifacts-path.txt` records the original frozen directory. Executed command (PowerShell, JAVA_HOME pointing to JDK 21):

```powershell
./scripts/test-selenium-bidi-compatibility.ps1 -BrowserTests -GridUrl http://127.0.0.1:4444
# Following fixture corrections, explicitly reused the same final production JARs:
./scripts/test-selenium-bidi-compatibility.ps1 -ReuseFrozenRun 'D:/Java Projects/JsTestTools/target/selenium-bidi-compatibility/38b8ab457d9c4c8b9d7ed5150ea064aa' -BrowserTests -GridUrl http://127.0.0.1:4444
```

The original 4.39 direct-call bytecode demonstrably throws `NoSuchMethodError` with the changed getter descriptors on 4.41+; that expected failure is asserted independently from successful mapping by the fixed Lens JAR. The 4.39 parser's real nullable-response failure was reproduced in Chrome before normalization and the same rich scenario passed afterward. Original malformed/missing required event fields in test fixtures and obsolete assumptions that all raw remote drivers need augmentation were corrected to follow the verified protocol and runtime capabilities; assertions were retained and no version was skipped.

Additional verification:

- Focused BiDi/compatibility tests on the baseline: 76 PASS, zero skips.
- Rich `NetworkBiDiBrowserIT` on Java 17 / Selenium 4.39 / Chrome 157: 7 PASS, 1 NOT RUN (its optional raw-Grid property was absent; the independent same-JAR Grid tests above all ran). This covers redirects, real failed/truncated requests, redaction, exports/failure bundles, restart, foreign listeners and actual closed connection.
- Negative gate: `MAVEN_ARGS=-DskipTests` with `-Versions 4.39.0` correctly returned failure for missing required reports. Evidence: `target/bidi-gate-negative.log`.
- `node scripts/test-ai-integration-builder.mjs`: PASS.
- `./scripts/check-doc-signatures.ps1`: PASS (436 signatures, 104 blocks).
- `./scripts/check-public-api-since.ps1`: PASS.
- Network public API comparison using `javap -public`: PASS (20 types unchanged).
- `git diff --check`: PASS.
- Original audit MkDocs strict render: NOT RUN because no Python installation was available in that audit environment. The isolated 0.5.2 preparation subsequently bootstrapped portable Python 3.13.9 and the pinned `requirements-docs.txt` dependencies under `target/docs-tools`, then passed strict development, release and historical 0.1.0 renders. See the release preparation report for the final integrated results.

Full reactor: `mvn -B -q test` **PASS**, executed outside the filesystem sandbox after the original sandboxed invocation failed `AccessDeniedException` in selector-tooling temporary directories. Final evidence: `target/bidi-reactor-unrestricted.log`, Maven exit 0; 348 Surefire suites / 2,012 tests, zero failures/errors, 39 existing conditional skips. Workspace-relative temporary-directory experiments were not used as passing evidence. No assertions were weakened to obtain this result.

Global `./scripts/check-public-api.ps1`: **FAIL — generated API catalog is stale** in the existing dirty working tree (`target/bidi-public-api-check.log`). The broader tree already contained substantial Studio/application/selector edits at task start. This check was not run on a pristine baseline, so its failure is not labeled a proven baseline failure. The unchanged 20-type network API comparison and since/signature checks above are independent evidence for this task. The global generated catalog was not rewritten across unrelated user changes.

## Compatibility and release readiness

The minimum tested BiDi baseline remains Selenium 4.39.0. The newest verified stable runtime for the prepared 0.5.2 build is 4.53.0 on Java 21 / Chrome 157 / Grid 4.53. Only the listed versions have same-JAR execution evidence; 4.42–4.44/4.50 were inspected for descriptor boundaries without consumer/browser certification. Java 17 baseline behavior passed the richer existing suite. BrowserStack and cross-version Firefox require separate provider/browser certification; no generic “all Selenium 4.x” claim is made.

The focused correction is suitable for a **patch release**: public signatures and compile baseline are unchanged, and the changes repair binary linkage, capture correctness and lifecycle. It has passed the local binary/browser/functional gates. The entire current branch is not certified for publication: reconcile the global generated API catalog, run hosted CI (including strict MkDocs rendering) and retain the documented provider limitations before releasing. No release, tag or push was performed.

Remaining practical risks are the baseline health command's timeout, lack of a common passive transport-error hook, pre-dispatch malformed JSON outside Lens's boundary, and legacy server event-interest retention described above. These limits are explicit rather than treated as successful capture evidence. For normal active connections every selected runtime passed request/response correlation, restart, foreign-listener preservation and raw remote initialization.

## Git handoff

Branch: `feat/studio-testng-playground`. HEAD remains `94189e167a123c5aacfae4d3631634a98c016717`. No commits were created. Changes are unstaged working-tree modifications and new files, alongside the preserved preexisting user modifications/staged deletions. Final status snapshot: `target/bidi-git-status.txt`. No reset/stash, push, release publication or tag. The temporary local Grid started for this audit was stopped after validation.
