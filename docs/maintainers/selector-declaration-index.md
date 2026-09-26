# Static Java locator declaration index

Test Lens 0.4.0 contains an internal, offline foundation for discovering Java locator declarations. It is not a
Selector Audit, selector generator, stability score, or automatic migration tool. The module is deliberately not
published and is not a dependency of the Test Lens runtime.

## Scope

The Java-only V1 recognizes the eight standard Selenium `By` factories, `By` fields and local variables, conservative
compile-time string constants, dynamic locator templates, and Selenium 4.39 `@FindBy`, `@FindBys`, and `@FindAll`.
`@FindBys` remains an ordered chain and `@FindAll` remains an ordered set of alternatives; neither is flattened into
a synthetic CSS selector or XPath.

Discovery uses these statuses:

- `RESOLVED` means the strategy and value were statically proven.
- `PARTIALLY_RESOLVED` means only part of the declaration could be proven, commonly because the classpath was
  incomplete.
- `DYNAMIC` means the declaration legitimately depends on a runtime parameter or expression.
- `CUSTOM` identifies a project-specific locator abstraction without executing it.
- `UNSUPPORTED` and `ERROR` report unsupported constructs and analysis failures separately.

Dynamic and custom locators are not classified as bad selectors. Kotlin is reported as an unsupported language in
V1 rather than parsed as Java.

## Safety boundary

The scanner reads only explicitly supplied source roots below an explicit project root. It does not load application
classes, run annotation processors, execute helpers or static initializers, invoke Maven or Gradle, follow links out
of the project, contact URLs found in source, or modify source files. JavaParser and its symbol solver exist only in
`selenium-test-lens-selector-tooling`; normal Test Lens and FAST consumers do not receive that dependency.

Missing classpath entries and symbol failures are visible through coverage. The scanner never upgrades an uncertain
import-based match to a fully resolved declaration.

## Local index and sensitive values

The canonical `selector-index.json` is a local tooling artifact and belongs under ignored `target/` output. It is not
uploaded or included in runtime reports automatically. Locator literals can contain account IDs, tokens, emails, or
other application data, so the index should be handled as sensitive local build output.

Runtime `locatorObservation` and the source index have different trust boundaries:

- runtime observations are bounded and redacted before trace retention;
- the local source index preserves proven locator values for source analysis and is not processed with runtime
  `RedactionPolicy`.

The index intentionally does **not** persist exact source text. Each file stores a SHA-256 hash of the exact bytes,
while each declaration stores a source range, normalized AST expression, and deterministic declaration reference.
Future patch tooling must reopen the local file and verify its hash and range before proposing a change.

Offsets use zero-based UTF-16 code-unit indexes into the decoded Java string. Lines and columns use JavaParser's
one-based positions. Logical paths use `/` and are project-relative; absolute paths and timestamps are absent, so
the same input and configuration produce byte-for-byte identical JSON.

## Coverage and composition

The index reports requested and found roots, parsed/failed/excluded files, generated and unsupported-language files,
declarations by resolution status, incomplete-classpath state, and symbol-resolution issues. Generated sources under
`target/generated-sources` and `target/generated-test-sources` are excluded by default.

One declaration remains one record even when used from many locations. In particular, the `By.id(...)` call inside
a `static final By` initializer is owned by the field and does not produce a competing direct-call declaration.
Persisted usage-site records and runtime declaration-reference injection are intentionally deferred. A later stage
can correlate the index with runtime `usageSource` using project-relative paths and static usage locations without
changing the existing runtime locator schema in this stage.
