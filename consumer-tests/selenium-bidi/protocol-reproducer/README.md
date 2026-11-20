# Raw Selenium BiDi redirect protocol reproducer

This standalone Maven consumer has no Lens dependency. It subscribes through public Selenium BiDi APIs to raw `network.beforeRequestSent`, `network.responseStarted`, `network.responseCompleted`, and `network.fetchError` maps before generating traffic, matching the four event subscriptions used by the capture source.

One test performs 50 distinct native JavaScript fetches to a controlled HTTP server. Each `/redirect/N` sends HTTP 302 to `/final/N`, which sends HTTP 200. For each stimulus the test requires both matching `responseCompleted` events within three seconds. It stops at the first mismatch; there are no retries and no synthesized responses. A successful local run does not establish that the intermittent hosted Linux/Chrome protocol issue is fixed.

Run from this directory with Java 17 or newer (newer Selenium releases may require Java 21 at runtime):

```powershell
mvn test '-Dselenium.version=4.53.0' '-Dbidi.grid=http://127.0.0.1:4444' '-Dtest.chrome.binary=C:/path/to/chrome.exe'
```

Omit `bidi.grid` for an owned local Chrome session (`test.grid.url` is also accepted). `headed=true` enables the visible browser. For a remote Grid node, set `test.network.bind=0.0.0.0` and `test.network.host` to an address through which that node can reach the test JVM; the fixture uses an ephemeral port. The Chrome binary path is interpreted on the node when Grid is used. No external site or service is required.

Evidence is always written to `target/protocol-redirect-evidence.json` in UTF-8 and printed to the console. Override its directory with `-Dprobe.output=/path/to/artifacts/protocol-reproducer`. It contains bounded phase rows and the controlled server journal: event type, timestamp, per-run request pseudonym, request/response paths, redirect count, and status. It excludes headers, credentials, query strings, and raw request IDs. Preserve the evidence and Surefire report when reporting an upstream mismatch. `PROTOCOL_MISMATCH` is a failing test result, never an accepted alternative outcome. The probe owns and closes its listeners, BiDi connection, browser session, and fixture server.
