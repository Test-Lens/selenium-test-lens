# Selenium Test Lens 0.5.2

Browser contract: Chrome/ChromeDriver 157.0.8103.78. Chrome 158 network reliability is not certified.

Exact final validation and external publication receipts are recorded in the [production release report](production-release-0.5.2.md).

## Overview

This patch fixes Selenium BiDi binary incompatibility and reproduced runtime regressions in 0.5.1. Upgrade especially if you use diagnostic network capture with Selenium 4.41 or newer, recovery actions, cancellation or managed cleanup. There are no new public APIs or Studio features in this patch.

## Compatibility

- Java 17 remains the minimum and compile baseline.
- Selenium 4.39.0 remains the minimum tested BiDi version. The newest verified version is 4.53.0.
- The same compiled Lens JAR passed on **4.39.0, 4.40.0, 4.41.0, 4.45.0, 4.46.0, 4.47.0, 4.51.0, 4.52.0 and 4.53.0**, using Java 21, Chrome 157 and Grid 4.53.0. This is an explicit list, not a promise covering every Selenium 4.x release.
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

The release gate uses Chrome/ChromeDriver **157.0.8103.78**, Java 21 and Grid 4.53.0. It validates one frozen Lens JAR across all nine listed Selenium versions, exact controlled-server request/response journals, JSON/HTML/failure-bundle data and redaction, plus 50 distinct standalone redirects each through direct ChromeDriver and Grid. API, reactor, packaging and strict documentation gates must also pass on the release source. No HTTP status, terminal-response or report assertion is relaxed.

Chrome 158 direct/Grid canaries are explicitly nonblocking and preserve their actual results. Neither a 157 PASS nor a transient 158 PASS certifies Chrome 158 network reliability. The investigation found no verified stable correction; Beta 159 contains an upstream routing correction but remains unverified and is not the release recommendation.

The final source SHA, hosted run URLs, test totals, artifact/signature inventory and isolated public-Central consumer results are recorded in the GitHub Release and its [release verification attachment](https://github.com/Test-Lens/selenium-test-lens/releases/download/v0.5.2/release-verification.json). The [production audit](https://test-lens.github.io/selenium-test-lens/0.5.2/maintainers/production-release-0.5.2/) retains the earlier milestones and failure analysis with their original scope.

## Known limitations

BrowserStack was not exercised in this release validation. Firefox across the nine-version matrix is unverified. The baseline same-connection health command can add up to 30 seconds beyond an event wait deadline. Malformed JSON rejected before Selenium callback dispatch has no common public passive failure hook. Selenium 4.39–4.45 can retain server event interest after local removal until the connection owner closes it. Already terminal core session snapshots remain immutable even if later caller-owned driver cleanup fails.

Read the [Selenium compatibility policy](https://test-lens.github.io/selenium-test-lens/0.5.2/selenium-compatibility/), [network guide](https://test-lens.github.io/selenium-test-lens/0.5.2/advanced/network/), [regression audit](https://test-lens.github.io/selenium-test-lens/0.5.2/maintainers/selenium-051-regression-audit/) and [production release evidence](https://test-lens.github.io/selenium-test-lens/0.5.2/maintainers/production-release-0.5.2/).
