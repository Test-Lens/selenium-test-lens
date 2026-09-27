# Compatibility run manifests

The compatibility manifest foundation is internal, explicit tooling for Test Lens 0.4.0 development. It captures
facts for later comparison; it does not decide whether a test is headless-compatible and does not provide a public
runtime API.

## Logical invocation and attempts

One `CompatibilityRunManifest` represents one logical test invocation. Its ordered `attempts` retain each retry's
result, bounded failure signature, recovery and behavior summary, timing, and evidence completeness. A failed first
attempt followed by a passing retry is therefore one terminal `PASSED` invocation with `passedAfterRetry=true`, not
two unrelated runs. TestNG/JUnit identity is supplied structurally. Manual capture without a trusted logical key is
recorded as unknown; display session names and raw parameter values are not parsed.

Optional facts use explicit knowledge states: `KNOWN`, `UNKNOWN`, `NOT_APPLICABLE`, `UNSUPPORTED`, `REDACTED`, and
`CONFLICTED`. Two unknown values do not prove equality. Dataset, environment, invocation, and suite/context keys are
stored as domain-separated digests. Test-source and system-under-test revisions remain separate.

## Requested and effective execution

Requested headed/headless intent and effective browser state are separate facts with provenance. A requested
headless custom factory is not proof that its browser is headless. An arbitrary pre-created driver remains
`UNKNOWN` unless a trusted descriptor attests its state or an approved typed capability reports it. The capture does
not infer state from user agent, browser name, viewport, screenshots, HUD presence, or absence of a visible GUI.

`ObservabilityMode.DEFAULT` and `FAST` are independent from headless state. HUD preset and effective presentation or
retention overrides are separately allowlisted. A successful FAST summary-only trace explicitly records partial
detailed-behavior evidence; missing events never mean that no actions occurred.

## Explicit Selenium capture

`SeleniumCompatibilityCapture` runs only when tooling calls it. It is not registered by `TestLens.attach`, framework
listeners, normal locators, or FAST mode. The bounded display capture uses at most two WebDriver commands: one
window-size read and one script returning `innerWidth`, `innerHeight`, and `devicePixelRatio`. Capability access uses
the driver's local `HasCapabilities` view and persists only browser name/version, platform, approved driver-version
fields, and a typed Firefox headless marker when actually supplied.

Complete capability maps, Chrome debugger addresses, browser binary/profile paths, session IDs, user agents, raw
window handles, URLs, DOM, screenshots, and screen content are excluded. Locale and timezone are never probed; they
remain unknown unless a trusted caller supplies them.

## Trace and failure-bundle projection

Adapters accept caller-provided bytes rather than scanning a workspace. The trace adapter requires the supported
runtime trace schema and projects only bounded counts and reason codes for retries, locators, waits, interactions,
contexts, authentication outcomes, uploads, warnings, timings, and evidence availability. It never reconstructs raw
locators. Detailed Smart Click strategy remains unknown when only prose exists; HUD/debug messages are not parsed.

The failure-bundle adapter imports only allowlisted browser, platform, viewport/window, and safe configuration facts.
Raw URLs, paths, handles, headers, cookies, storage, capabilities, and artifact content are omitted. A conflict between
trusted descriptor, direct capture, trace, or bundle evidence becomes a `CONFLICTED` fact plus a structured issue;
known facts are never silently overwritten.

## JSON, bounds, and privacy

The Jackson Core streaming codec uses schema and algorithm version 1, strict duplicate detection, fixed field order,
stable LF output, and semantic ID verification. `manifestId` is a domain-separated length-prefixed SHA-256 identity;
timestamps, random/session IDs, absolute paths, mtimes, and JSON map order do not participate. Default artifacts are
written atomically beneath:

```text
target/test-lens/compatibility/runs/compatibility-manifest-v1-sha256-<hex>.json
```

The Windows-safe filename encodes the same digest as `manifestId`. The reader caps documents at 16 MiB, nesting at
32, strings at 65,536 characters, names at 256, numeric tokens at 64 characters, tokens at 2,000,000, attempts at
64, grouped behavior entries at 128, manifest issues at 128, and evidence digests at 64. Unknown semantic enum values
are rejected; top-level additive extensions must use an `x-*` name.

Manifests contain no passwords, tokens, cookies, authorization headers, storage values, arbitrary environment or
system-property dumps, raw test arguments, full exception messages, machine paths, WebElement/session/window IDs,
page source, or full capabilities. Imported messages are omitted after a conservative normalized digest is built.

## Current boundary

Both compatibility artifacts are non-published. `selenium-test-lens-compatibility-engine` is JDK-only;
`selenium-test-lens-compatibility-tooling` adds Jackson Core and the narrow Selenium API. Normal runtime and selector
modules do not depend on either artifact. S09B2 can consume the manifest reader, explicit knowledge/provenance,
attempts, evidence completeness, semantic configuration digest, and safe browser/display/context summaries to build
comparability without adding normal-run browser commands or I/O.

## Offline comparison reports

The compare engine requires an explicit `ComparisonIntent`; it never guesses whether the caller meant to isolate
headless mode, observability, viewport, browser, or another axis. For a headless comparison, effective (not merely
requested) state must prove the headed/headless difference while observability and the remaining critical dimensions
are controlled. For an observability comparison, `DEFAULT`/`FAST` is the intended difference and effective headless
state must remain known and equal. B1 cannot always distinguish an explicit presentation override from a preset
default, so that fact is reported as a limitation rather than invented equality.

Comparability is `COMPARABLE`, `COMPARABLE_WITH_LIMITATIONS`, `NOT_COMPARABLE`, or `UNKNOWN`. Per-dimension states
preserve known equality, intended difference, mismatch, left/right/both unknown, conflict, and not-applicable. Two
unknown datasets, revisions, driver versions, or viewports are never treated as controlled equality. A viewport
mismatch takes configuration-first precedence: align the viewport and rerun before considering a locator change.

Matching uses the structured test identity and invocation discriminator. Duplicate manifest IDs are coalesced, while
multiple distinct candidates for the same semantic key become an ambiguous coverage entry; file or list order is
never used to pair tests. Attempts remain attached to their logical invocation. A one-attempt pass versus a pass
after retry is a pass/pass behavioral delta, not two independently paired attempts.

Outcome, comparability, and causal attribution are independent. A variant regression describes the observed result
relationship but does not prove root cause. Findings separately retain severity, causal state (`OBSERVATION`,
`HYPOTHESIS`, or `CONFIRMED_CAUSE`), causal confidence, and structured evidence statements. A failed baseline is
always retained: two proven-identical failures, proven-different failures, an unknown relationship, and a variant
improvement have distinct outcomes. The engine never says that headless broke an already-red baseline.

Behavior is split into execution, observability, and expected-preset channels. Every group checks evidence
completeness before comparing values. In particular, a FAST `SUMMARY_ONLY` manifest does not mean zero locator
activity, retries, waits, or fallbacks. Expected FAST reductions such as missing HUD batches and reduced successful
trace retention are informational observability differences; a real `PASS` to `FAIL` transition remains an execution
regression under the observability axis.

Canonical sanitized outputs are written explicitly beneath:

```text
target/test-lens/compatibility/reports/compatibility-v1.json
target/test-lens/compatibility/reports/compatibility-v1.html
```

JSON and standalone HTML render the same comparison model. The HTML has local filters and no external resources,
live Lab action, or apply/fix control. Both formats are capped at 64 MiB and contain manifest IDs and safe logical
references rather than raw trace events, exception messages, parameters, paths, handles, capabilities, or secrets.
The report provides exact matched, unmatched, ambiguous, limited, regression, and baseline-red counts; it does not
publish a compatibility percentage or claim whole-application coverage. No automatic patch or STATIC source
heuristic is implemented at this stage.
