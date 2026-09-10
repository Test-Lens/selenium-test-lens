# Known limitations

**Selenium Test Lens 0.1.0 — stable**

This page records the historical boundary of the published release.

- Semantic helpers are CSS/XPath factories. Role/name matching is not the browser's complete accessibility computation.
- There is no compositional or parent-scoped locator pipeline, collection filtering, shadow-root traversal, page assertion facade, or count wait.
- Action and assertion polling can emit retry-shaped trace events, but 0.1.0 has no aggregated retry summary or retry outcome policy.
- `waitForNetworkIdle` observes only XHR/fetch started after tracker injection, and its timeout was diagnostic rather than a guaranteed thrown failure.
- Network diagnostics require manual events for usable capture. BiDi and performance-log modes are unsupported.
- Auth-state APIs existed, but later origin-isolation and atomic storage guards were not part of 0.1.0. Restore only on the exact application origin; do not use it for cross-origin SSO storage.
- Session/facade finalization was not idempotent. Invoke one terminal path once.
- Screenshots are viewport-only. Screenshots and attached video are not pixel-redacted.
- There is no central diagnostic-redaction policy, automatic failure bundle, report uploader, JUnit 5 artifact, TestNG artifact, or browser-integration artifact.
- HUD injection is best-effort and can be constrained by CSP, frames, shadow DOM, or page lifecycle changes.

See the [development documentation](https://test-lens.github.io/selenium-test-lens/dev/) for unreleased work, but do not use its examples with 0.1.0 unless the symbol is also present in the [0.1.0 Javadoc](https://javadoc.io/doc/io.github.test-lens/selenium-test-lens/0.1.0/).
