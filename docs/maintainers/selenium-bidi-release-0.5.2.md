# Test Lens 0.5.2 release validation

This records the completed **BiDi hardening milestone**, before the additional confirmed runtime regressions were fixed. Its test totals and frozen artifact hash identify that milestone. The [0.5.1 regression audit](selenium-051-regression-audit.md) records the final combined candidate, clean-checkout validation and replacement artifact hash; use that report for final release readiness.

Verdict: **READY WITH DOCUMENTED LIMITATIONS**. The candidate is prepared locally and has not been published, tagged, pushed, or deployed. Hosted GitHub Actions and BrowserStack remain NOT RUN. The publication policy still records **0.5.1** as actually released and declares **0.5.2** separately as prepared.

## Scope and isolation

All work was performed in `private/bidi-release-0.5.2`, branch `release/bidi-0.5.2`, based on `94189e167a123c5aacfae4d3631634a98c016717`. Thirty-two explicitly selected BiDi files were copied, including untracked sources and the independent consumer. No uncommitted Studio, Playground, TestNG adapter or selector/application changes were transferred. Existing Studio/application modules receive only the necessary parent-version bump; their production code and tests match the baseline.

The original `feat/studio-testng-playground` worktree retains its HEAD, status and index exactly. SHA-256 comparison of all **256** initial modified/untracked file paths found **zero changes**. Evidence: `target/release-052-original-preservation.json`; initial snapshots remain in the original worktree's `target/bidi-hardening-original/`. No reset, clean, automatic stash or broad checkout was used.

## Findings and corrections

- The existing BiDi compatibility implementation remained intact: cached public accessor/listener discovery, nullable response sizes, opaque listener IDs, Lens-owned cleanup, callback failure invalidation, health checks and stable public contracts were verified.
- A deterministic regression exposed an older overlapping-restart gap: an earlier `start()` could return from a delayed health check after a newer capture had started and open an unwanted third source. The previous frozen JAR failed with three sources instead of two. A private `stopGeneration()` token and a locked recheck before resetting capture state now preserve the newer generation. The regression verifies source identity, events and exactly-once cleanup; it passes in the candidate.
- `NetworkReportCorrectnessTest` compares controlled HTTP journal data with the live model, exported network JSON, trace JSON, rendered HTML and failure ZIP. It covers GET 200, POST JSON, 302→200, 404/500, repeated identical URLs, eight rapid requests, 204/no body and confidential headers. Each successful/failed phase produces exactly **18/20** expected responses. Multisets, IDs, redirect hops, correlation, method/status, assertion outcomes and redaction are checked. Available body sizes must equal bytes actually written by the uncompressed fixed-length HTTP peer, including zero for 302/204; unknown sizes remain absent in live and attached JSON. A completion latch makes journal snapshots deterministic. Diagnostic HTML/trace URLs deliberately retain the documented sanitized path preview; full URLs remain verified in network export. Existing decoder tests independently cover unavailable nullable response sizes, large values and malformed required request sizes.
- JSON, HTML and ZIP evidence persists under each matrix row's `report-artifacts/`, so CI can upload actual content. The gate requires both named report tests and rejects missing/skipped report suites; a deliberately excluded report test made the gate fail even while binary/session tests passed.
- The global catalog itself matches the released binaries when compiled from this isolated source. The previous dirty-tree failure was not accepted as an API change. A separate `@since` bug re-filtered an already projected 267-type manifest through a CSV that omitted classification-policy promotions, falsely reporting 23 additions. The gate now recognizes the explicit projected-manifest format and retains CSV filtering for legacy exhaustive manifests. Both formats have regressions. No historical tag, API annotation or generated catalog was rewritten to hide a difference.
- Release tooling now permits only an explicitly declared prepared version newer than the actual release. Unknown candidates remain rejected. New consumer POMs are explicitly classified as non-reactor fixtures. Windows CRLF metadata handling and isolated tooling fixtures were corrected without weakening assertions.
- Current documentation distinguishes published 0.5.1 from unpublished 0.5.2. The integration builder carries prepared-release metadata and requires a local candidate installation instead of claiming Maven Central availability. Historical 0.1.0–0.5.1 content is unchanged.

## Results

| Area | Result | Evidence |
|---|---|---|
| Isolated reactor | PASS | `mvn -B -q test`: 1,804 tests, zero failures/errors, 39 conditional skips; `target/release-052-reactor.log` |
| Same-JAR Selenium matrix | PASS | Nine versions below; 1,242 tests, zero failures/errors/skips |
| Local Chrome | PASS | Chrome/ChromeDriver 157.0.8103.78; lifecycle smoke and exact server/report comparison on every runtime |
| Grid | PASS | Grid 4.53.0, raw RemoteWebDriver initialization, restart, foreign listeners and report comparison on every runtime |
| BrowserStack | NOT RUN | No configured credentialed test session; no paid resources started |
| Network report correctness | PASS | 18/20-response journal phases, parsed JSON, real HTML rows, failure-bundle histogram and redaction; artifacts preserved |
| Public API compatibility | PASS | Actual Maven Central 0.5.1 JARs for all 15 published JAR modules: 791 public types and all declarations/members unchanged, including tooling/internal surfaces |
| Global API catalog | PASS | Official `check-public-api.ps1`: 791 types / 5,700 callables; no blind regeneration needed |
| Stable/advanced API since gate | PASS | 267 types / 2,089 callables; zero additions versus v0.5.1 after projection fix |
| MkDocs strict | PASS | Dev, release configuration and unchanged archived 0.1.0; pinned MkDocs 1.6.1 / Material 9.7.7 / mike 2.2.0 |
| Documentation checks | PASS | 436 signatures / 104 blocks, links, version boundaries, rendered builder/HUD/product overview, SEO and versioned-docs fixture |
| CI syntax and semantics | PASS locally | PyYAML and actionlint 1.7.12 on CI/docs/release-check workflows; 45-minute job and 300-second consumer-fork limits |
| Release artifacts | PASS | `mvn -B -q -Prelease-artifacts -DskipTests package`; 16-coordinate staging whitelist, binary/source/Javadoc/license validation |
| Hosted CI | NOT RUN | No run ID. GitHub CLI's configured credential is invalid, and the candidate branch exists only locally; no push was authorized or performed |

The isolated reactor count differs from the prior dirty-tree run (2,012) because unrelated uncommitted work and its tests were excluded, not because tests were removed.

## Same-JAR evidence

Java 21.0.10, Windows 11. Compile baseline remains Java 17 / Selenium 4.39.0. Every final row used Lens 0.5.2 SHA-256:

`CB3280067FB10789174D9B29E2BCDE6C26D4B0BD9BF8B4C4ED96261D7262A0A8`

The final packaged main JAR has this same hash. Core and overlay hashes are also frozen and checked. The independent consumer verifies loaded Lens provenance, actual Selenium classes/runtime classpath and the original descriptor-failure reproducer.

| Selenium | Binary/unit | Chrome | Grid | Chrome report data | Grid report data |
|---|---|---|---|---|---|
| 4.39.0 | PASS | PASS | PASS | PASS | PASS |
| 4.40.0 | PASS | PASS | PASS | PASS | PASS |
| 4.41.0 | PASS | PASS | PASS | PASS | PASS |
| 4.45.0 | PASS | PASS | PASS | PASS | PASS |
| 4.46.0 | PASS | PASS | PASS | PASS | PASS |
| 4.47.0 | PASS | PASS | PASS | PASS | PASS |
| 4.51.0 | PASS | PASS | PASS | PASS | PASS |
| 4.52.0 | PASS | PASS | PASS | PASS | PASS |
| 4.53.0 | PASS | PASS | PASS | PASS | PASS |

Final byte-validated results: `target/selenium-bidi-compatibility/ccf1629fa53747038ac338a4ac3d8b75/results.json`, per-version logs/Surefire XML and `report-artifacts/`; the earlier full run is `8fa44af445fb450fb211dfe6b4ab866a`. Supplemental completed-journal tests passed locally and through Grid on both 4.39.0 and 4.53.0 in `target/release-052-completed-journal/`. Frozen JARs originate from `11a72022826f43a7ab1e20940ba0a1c4/frozen`. An initial local attempt started before Grid became reachable and failed its first two remote rows with ConnectException; the complete runs followed an explicit successful Grid readiness check, used the same JARs and passed without test retries. The temporary local Grid is stopped after final validation.

Local package staging: `target/release-staging-0.5.2`. It contains 15 binary, 15 source and 15 Javadoc JARs plus the 16 POM coordinates. It is a local review artifact, not a Maven Central upload.

## Reproduction and hosted handoff

```powershell
./scripts/check-reactor-versions.ps1 -ExpectedVersion 0.5.2 -IncludeBrowserIt
./scripts/test-prepared-release-policy.ps1
./scripts/test-release-tooling.ps1
mvn -B -q test
./scripts/check-public-api.ps1
./scripts/check-public-api-since.ps1
./scripts/check-doc-signatures.ps1
./scripts/test-selenium-bidi-compatibility.ps1 -BrowserTests -GridUrl http://127.0.0.1:4444
mvn -B -q -Prelease-artifacts -DskipTests package
./scripts/validate-release-packaging.ps1 -Version 0.5.2
./scripts/check-patch-release.ps1 -ExpectedVersion 0.5.2
```

Documentation commands use a local portable Python 3.13.9 installed only under `target/docs-tools`, with the pinned `requirements-docs.txt`; no global installation was changed. Strict commands include `python -m mkdocs build --strict`, the same command with `--config-file mkdocs-release.yml`, and with `--config-file mkdocs-0.1.0.yml`. Versioned-docs simulation preserves `latest=0.5.1` and prepares `dev=0.5.2` in a disposable local Git fixture, never the real documentation remote.

After credentials are repaired and an owner separately authorizes making this branch available on GitHub, run:

```text
gh workflow run ci.yml --repo Test-Lens/selenium-test-lens --ref release/bidi-0.5.2
gh run list --repo Test-Lens/selenium-test-lens --workflow ci.yml --branch release/bidi-0.5.2
```

The workflow runs no release publication. Hosted Linux results cannot be inferred from the local Windows execution; record its run ID before promoting that environment to verified.

## Readiness and limitations

This is a patch release with unchanged public signatures and dependency baseline. Minimal supported/tested Selenium remains **4.39.0**; newest verified stable is **4.53.0**. Only the listed versions are certified for these network scenarios. BrowserStack and cross-version Firefox remain unverified.

The baseline same-connection health command can add up to 30 seconds. No common public passive transport-close hook exists; malformed JSON rejected before event dispatch remains outside Lens's callback boundary. Selenium 4.39–4.45 can retain server event interest after local listener removal until the shared connection owner closes it. These limits are documented in the [compatibility policy](../selenium-compatibility.md) and [release notes](../whats-new-0.5.2.md).

No local functional blocker remains. The remaining steps are hosted CI and optional provider certification, followed by separately authorized release/tag/publication actions. The original working tree is not the release candidate; use the isolated branch and its logically scoped commits.
