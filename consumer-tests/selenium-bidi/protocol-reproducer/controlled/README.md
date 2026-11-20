# Controlled HTTP / BiDi transport experiment

This diagnostic branch is separate from `release/bidi-0.5.2`. Its manual-only `ci.yml` runs this experiment, not release gates. Do not merge that workflow replacement into the release branch.

The experiment downloads the already verified Linux-built Lens artifact from CI run `44692428506`, source `9f13b89b1928b7cbb9de46d3c9568733dcd5201a`, and verifies its hashes. It never builds Lens. Chrome/ChromeDriver pairs are pinned to `157.0.8103.78` and `158.0.8127.36`; Selenium Java is `4.53.0`, Java 21. Linux and Windows are measured separately in new headless Chrome.

Each case starts a fresh session and a loopback HTTP server with `/ok` (200), `/redirect` (302 to `/ok`) and `/json` (200). The journal records real request IDs, methods, full controlled URLs, received/finished times, actual status, Location and body byte count. Capture starts before navigation. Each redirect stimulus has a distinct case ID; 50 stimuli are different requests, not retries. All cases use the same assertions and three-second event deadline, with notification-based waiting. Request/response and redirect-hop correlation must be correct independently of callback arrival order.

- **A:** Selenium Java and raw BiDi, with Lens JARs and bridge classes absent from the runtime classpath.
- **B:** The same local driver and raw events, with Lens capture enabled through a separate public-API bridge.
- **C:** Raw events and Lens capture through an owned loopback Grid 4.53.0 and `RemoteWebDriver`.

All three variants run against both `127.0.0.1` and `localhost`. Additional A cases use `staging.example.org:80`, mapped exclusively to the local fixture, to distinguish the [Chromium HTTPS upgrade issue](https://github.com/microsoft/playwright/issues/43871) from loopback redirect failures. The paired feature-control case preserves the actual default disabled-feature list, then adds only `HttpsUpgrades` and `HttpsFirstBalancedModeAutoEnable`. Actual switches are independently read through the public CDP command; an unavailable or ambiguous reading is not accepted. This is an experiment, not a proposed default Lens/browser configuration.

ChromeDriver verbose logs remain private runner files. `scrub-controlled-cdp.mjs` exports only fixture URL/ID/timing/status/size/Location fields, preserving earlier ExtraInfo events and arrival order. Cookies, other headers, bodies, arbitrary URLs and session IDs are excluded. Its result explicitly distinguishes missing, partial and complete parsing. The artifact contains sanitized evidence, per-case outcomes, provenance and hashes; a green workflow does not imply every diagnostic case passed. Every FAIL and NOTRUN remains visible.

Run the isolated workflow after pushing this diagnostic branch:

```shell
gh workflow run ci.yml --ref diagnostic/bidi-http-upgrade-0.5.2
```

To execute only the public-host default/control pair, use `-f case_set=https-only`. This leaves the earlier loopback A/B/C evidence intact instead of repeating those cases. The first controlled run exposed a probe-only error: ChromeDriver merges disabled-feature arguments and can retain duplicate feature names. The verifier now treats them as set membership while still requiring every baseline feature and both HTTPS controls; no HTTP/event assertion changes.

The real release gates and publication remain on the release branch. Results from this branch must be labeled as transport diagnostics against the identified frozen artifact, not release-source CI certification.
