# Selenium Test Lens 0.5.2

**Release-gating browser configuration: Windows stock Chrome/ChromeDriver 157.0.8103.78.** Stock Chrome/ChromeDriver **157.0.8103.78 on Windows** passed the unchanged nine-version Selenium matrix, local/Grid report checks and 50 standalone redirects through each transport. Required CI builds the frozen JAR once on Linux and verifies that same artifact on Windows. Linux stock 157/158 network failures remain known; this Windows result does not certify them. No mapper override or HTTPS-feature override is used in the release gate.

The [production release report](production-release-0.5.2.md) records pre-publication milestones. Final release-specific CI, artifact/signature inventory and isolated public-consumer receipts belong in the [GitHub Release](https://github.com/Test-Lens/selenium-test-lens/releases/tag/v0.5.2) and its [release verification attachment](https://github.com/Test-Lens/selenium-test-lens/releases/download/v0.5.2/release-verification.json) after actual completion. Prepared source is not an external publication receipt.

## Overview

This patch fixes Selenium BiDi binary incompatibility and reproduced runtime regressions in 0.5.1. Upgrade especially if you use diagnostic network capture with Selenium 4.41 or newer, recovery actions, cancellation or managed cleanup. There are no new public APIs or Studio features in this patch.

## Compatibility

- Java 17 remains the minimum and compile baseline.
- Selenium 4.39.0 remains the minimum tested BiDi version. The newest verified version is 4.53.0.
- The same Linux-built Lens JAR passed on **4.39.0, 4.40.0, 4.41.0, 4.45.0, 4.46.0, 4.47.0, 4.51.0, 4.52.0 and 4.53.0**, using Java 21, Chrome 157 and Grid 4.53.0. This is an explicit runtime list qualified on Windows stock Chrome/ChromeDriver 157.0.8103.78, not a promise covering every Selenium 4.x release or Linux redirect integrity.
- All 791 published public types retain their 0.5.1 declarations and members.

## Bug fixes

- BiDi size getter primitive/boxed changes at Selenium 4.41 and string listener IDs at 4.46 are handled by a cached internal compatibility boundary. Unavailable response sizes remain unavailable.
- Callback failures invalidate capture, wake waiters and prevent assertions against incomplete traffic. Lazy RemoteWebDriver initialization, partial subscription cleanup, concurrent stop and overlapping restart are covered.
- Lens removes its own listeners and preserves application listeners on shared connections.
- Redirect hops whose `beforeRequestSent` event is absent retain the real `RequestData` supplied by `responseStarted` or `responseCompleted`. This fixes an observation gap reproduced in hosted Chrome 158. Request observations are deduplicated by request ID and redirect hop; timestamps identify actual observation, not an invented request start. Trace metadata records `requestObservationSource`. Responses are never fabricated, and public signatures are unchanged.
- Ambiguous transport errors no longer trigger another potentially delivered click/input. Checkbox confirmation cannot repeat an already completed click.
- Interrupted overlay waits/actions and React recovery stop before further work while retaining cancellation. Dismissal observes disappearance within the existing 500 ms cap instead of always sleeping for 500 ms. Null input behavior is consistent across overlay modes.

## Reliability improvements

- Critical cleanup failures remain observable even when a primary test failure exists; resources are attempted once, secondary failures are retained and cleanup interruption restores the thread flag.
- Unexpected finalization failures trigger emergency cleanup and fail an active session. Fatal JVM/linkage errors in locator metadata are propagated while optional diagnostic fallbacks remain available.
- Public retry/fallback configuration and the caller's ownership of WebDriver are unchanged. JavaScript fallback is a programmatic dispatch, not a physical click.

## Reporting and HUD

- Existing measured operation durations reach JSON and HTML timelines. Sub-millisecond operations may correctly display 0 ms; descriptive empty steps receive no invented elapsed time.
- Late nonterminal HUD updates cannot overwrite completed operation status.
- HTML reports reject executable artifact-link schemes and keep rejected values escaped as text.
- Regression tests verify HTTP status/method/IDs, redirects, repeated requests, response bytes, unavailable sizes, custom-secret redaction, Unicode and failure bundles against controlled HTTP server journals.

## Upgrade instructions

Update `io.github.test-lens:selenium-test-lens` and any Lens adapters/integrations together to `0.5.2`. No test-source or public-API migration is required. Keep one consumer-owned Selenium version across all its modules, preferably with `org.seleniumhq.selenium:selenium-bom`.

If you disabled only Lens diagnostic capture as a 0.5.1 workaround, re-enable it after upgrading, request BiDi when creating the browser session and verify a known response. Application BiDi usage can continue independently. Do not add blind retries around ambiguous action failures.

## Verification

Runtime source **`9f13b89b1928b7cbb9de46d3c9568733dcd5201a`** passed [required CI](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44692428506), [automatic CI](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44692436487), [strict documentation](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44692436568) and [Release Check](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44692432759). The reactor recorded **1817 PASS / 34 intentional SKIP / 0 FAIL**. Linux binary checks recorded **1332 PASS**, with 36 browser scenarios explicitly NOT RUN in that binary-only job. Windows consumed the same frozen Lens JAR (`DD1D2D55E6D99FE1C47E5D030A30C378AB8F75574DC17E7809E886BB12EC1EE6`) and passed **1368 tests, zero failures/skips**, plus **50 distinct redirects each** through direct ChromeDriver and real Grid 4.53.0. Chrome and headed integration profiles each passed **121 tests with 11 classified optional skips**; Firefox integration and consumer checks passed. The subsequent documentation-only candidate requires its own checks. These results do not claim external publication.

Controlled [A/B/C HTTP diagnostics](https://github.com/Test-Lens/selenium-test-lens/actions/runs/44695845304) compared raw Selenium without Lens, raw Selenium plus the frozen Lens JAR, and raw/Lens through Grid. Windows 157/158 each passed all six loopback cases (50 stimuli each); this diagnostic Windows-158 pass does not certify Linux or change the release gate. Linux 157/158 exposed absent final-hop request-start events and, in failing cases, missing terminal responses. A failing stock-158 passive CDP trace and exact browser-tag mapper source identify early ExtraInfo assigned to the previous redirect hop, causing false 200 redirect status and missing final completion. All URLs remained HTTP on loopback high ports; HTTPS upgrading is a separate public-host condition. Chrome 156 was rejected for reporting 144 body bytes where the server sent 11: its mapper used total received bytes instead of encoded body bytes. No HTTP status, body-size, count or completion assertion was relaxed. The [production report](production-release-0.5.2.md) retains the exact three failed CI signatures, differential matrix, source links and separate HTTPS control results.

## Known limitations

BrowserStack was not exercised in this release validation. Firefox across the nine-version matrix is unverified. The baseline same-connection health command can add up to 30 seconds beyond an event wait deadline. Malformed JSON rejected before Selenium callback dispatch has no common public passive failure hook. Selenium 4.39–4.45 can retain server event interest after local removal until the connection owner closes it. Already terminal core session snapshots remain immutable even if later caller-owned driver cleanup fails.

Read the [Selenium compatibility policy](https://test-lens.github.io/selenium-test-lens/0.5.2/selenium-compatibility/), [network guide](https://test-lens.github.io/selenium-test-lens/0.5.2/advanced/network/), [regression audit](https://test-lens.github.io/selenium-test-lens/0.5.2/maintainers/selenium-051-regression-audit/) and [production release evidence](https://test-lens.github.io/selenium-test-lens/0.5.2/maintainers/production-release-0.5.2/).
