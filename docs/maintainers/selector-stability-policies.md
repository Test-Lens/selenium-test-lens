# Selector stability classification and policies

Test Lens 0.4.0 contains an internal, offline engine for describing generated-looking locator values and applying
explicit project policy. This foundation is not Selector Audit or Selector Lab, does not rank candidates, and does
not claim that a selector has been validated or will remain stable.

The engine deliberately keeps four dimensions separate:

- **generated-looking** describes deterministic textual appearance;
- **observed variable** requires supplied, comparable observation evidence;
- **declared stable/unstable** is project policy;
- validation remains **not evaluated** until a later stage checks syntax, context, match count, and target identity.

A UUID-like value therefore produces an appearance signal, not an instability verdict. An exact `STABLE` rule can
produce `DECLARED_STABLE` while the UUID signal remains visible. If comparable later evidence reports a changed
value, the result is `POLICY_EVIDENCE_CONFLICT`; the rule and evidence are both retained.

## Detector catalog V1

Catalog V1 reports UUID-like fragments, epoch-millisecond-like values, numeric runs of at least nine digits,
hexadecimal runs of 10–64 characters, high-entropy-looking 12–24 character suffixes, CSS-in-JS-like and CSS
Modules-like tokens, framework-counter-like IDs, React-useId-like text, and readable-prefix/suspicious-suffix
composites. Confidence describes confidence in recognizing that textual pattern, not probability of test failure.

Calibration intentionally excludes ordinary years such as `2024`, business versions such as `202409`,
`customer-123456`, short color hex values, `stable-button`, and long readable identifiers. Direct `id`, `name`, and
`class name` values are inspected. Only unambiguous single-token CSS forms (`#id` and `.class`) are inspected;
compound CSS and XPath are not regex-parsed as DOM attributes.

## Policies and precedence

Persisted decisions are only `STABLE` and `UNSTABLE`. `STABLE` removes an appearance penalty; it does not mean
unique, valid, correctly scoped, or validated. V1 matchers are:

- `EXACT_VALUE_DIGEST`: strategy plus a length-prefixed canonical SHA-256 digest;
- `STRUCTURAL_PATTERN`: a fully anchored sequence of literal, UUID, decimal, hex, or bounded-alphabet opaque runs.

Arbitrary regular expressions, lookarounds, backreferences, and unbounded wildcards are not accepted. Policy
precedence is scope specificity, explicit priority, exact over structural, then greater literal coverage and fewer
placeholders. Equal-precedence opposite decisions produce `POLICY_POLICY_CONFLICT`; file order never decides.

Declaration scope is narrower than context/source-symbol scope, followed by logical path, module, and project.
Context rules require a known stable structural fingerprint. Window handles, session IDs, remote element IDs, and
partial contexts are never persisted as policy keys.

## Policy files and security

The recommended source-controlled path is `.test-lens/selector-policies.json`. Optional local-sensitive overrides
belong under ignored `target/test-lens/selector-policies.local.json`. Files are loaded only when tooling explicitly
requests analysis; Selenium actions, FAST, native observation, and runtime reports do not load policies or run the
classifier.

Exact rules store only `strategy` and `valueDigest`; the exact locator value and `displayHint` are absent by default.
The digest prevents accidental plaintext disclosure but is not encryption and remains susceptible to dictionary
guessing for common values. Structural literals and human notes may also be sensitive, so review tracked policy
files before committing them. A redacted runtime value cannot create an exact-value rule.

The strict streaming JSON codec uses bounded document, nesting, token, string, number, rule, segment, note, and
display sizes; duplicate fields and malformed semantic values are rejected. Canonical output sorts rules by their
content-derived IDs, contains no timestamps or absolute machine paths, and is replaced atomically where supported.
`displayHint` is written only when a caller explicitly provides a reviewed, non-sensitive hint.

## Offline module boundary

`selenium-test-lens-selector-engine` is JDK-only and has no production dependencies. The non-published
`selenium-test-lens-selector-tooling` adapter consumes the static declaration index and owns the streaming
`jackson-core` policy codec. Runtime modules depend on neither component, so this foundation adds no Selenium hot
path work and no browser command.

The static adapter analyzes resolved locator strategy/value pairs, treats dynamic declarations as templates, leaves
custom locators insufficient unless a trusted scalar exists, and analyzes composed annotation children separately.
Future Audit and Lab stages can reuse the same signals, compiled policy indexes, evidence model, explanations, and
preview operation without putting JavaParser or policy matching into runtime execution.
