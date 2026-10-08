# Selector Intelligence

Selector Intelligence is Test Lens's foundation for understanding how Selenium selectors are declared, what
evidence exists about their stability, and which alternatives deserve review. It addresses a familiar problem:
a selector can work today while still being difficult to understand or expensive to change across a project.

Examples include selectors copied from browser developer tools, long CSS chains, positional selectors, unstable
class names, framework-generated identifiers, and the same locator repeated across Page Objects and tests. The
opposite problem matters too: an identifier can *look* random while being a stable application contract.

!!! important "Availability in 0.5.0"
    Selector engine, live analysis, and source tooling are published implementation dependencies of Test Engineering Studio. The supported user entry point is the Studio Maven goal; JavaParser-backed scanners and other technically public tooling classes are not stable USER API. Selector Lab remains an unpublished demo tool.

    Installing `selenium-test-lens:0.5.0` alone does not enable a background source scan or UI. Normal Test Lens and FAST execution remain unchanged.

## Capability map

| Capability | Browser required? | 0.5.0 availability |
|---|---:|---|
| Static declaration discovery | No | Published Studio implementation dependency |
| Selector Audit | No | Published tooling model with deterministic JSON and optional HTML viewer |
| Stability heuristics and project policy | No | Published engine and strict policy-file tooling |
| Find Similar and bounded history | No | Published tooling over a caller-supplied catalog and optional evidence |
| Selector Lab | Yes | Unpublished demo, not a 0.5.0 user entry point |
| Live candidate analysis | Yes | Published analysis used by Mapper and repair against an explicit browser target |

Publication supports Studio composition; it is not a compatibility promise for every class. Use the documented Maven goal rather than constructing scanners, live services, or repair internals directly.

## What problem does it solve?

Selector Intelligence keeps three questions separate:

1. **What can be proved from source?** For example, where a standard Selenium locator is declared and whether its
   value is a compile-time constant or a dynamic template.
2. **What does the value look like?** For example, whether it contains a UUID, long numeric run, hash-like fragment,
   framework counter, or CSS-in-JS-like token.
3. **What evidence says about stability and correctness?** Project policy, comparable observations, and optional
   validation in the current browser session answer different parts of this question.

The central rule is:

> **Suspicious-looking is not the same as proven unstable.**

A generated-looking finding is a signal for review. It is not proof that a locator changes between builds, matches
the wrong element, or will fail in the future.

## Analyze selectors without opening a browser

Here, **browser-free static analysis** means reading a supplied source project without executing the application or
starting WebDriver. It is different from running Chrome or Firefox in Selenium headless mode.

The implemented pipeline is:

```text
explicit project root and Java source roots
        -> declaration extraction and source coverage
        -> normalized selector observations
        -> appearance signals + stability policy + optional history
        -> Selector Audit findings and evidence
        -> optional candidate context for Selector Lab
```

The V1 source index understands the eight standard Selenium `By` factories: ID, CSS selector, XPath, name, class
name, tag name, link text, and partial link text. It also recognizes `By` fields and local variables, conservative
compile-time string constants, dynamic templates, and Selenium `@FindBy`, `@FindBys`, and `@FindAll` declarations.
Ordered chains and alternatives remain ordered rather than being converted into invented CSS or XPath.

Each declaration is reported as `RESOLVED`, `PARTIALLY_RESOLVED`, `DYNAMIC`, `CUSTOM`, `UNSUPPORTED`, or `ERROR`.
Dynamic and custom declarations are not defects merely because static analysis cannot reduce them to one value.
Coverage records missing roots, failed or excluded files, unsupported languages, incomplete classpaths, and symbol
resolution problems. V1 parses Java; Kotlin is reported as unsupported rather than interpreted as Java.

Trusted migration tooling also has a separate, bounded, symbol-resolved projection from shared `By` fields to Java
use sites. It can retain declaration use counts and logical file/method/line references for review. This is not a
general persisted usage-site index: unresolved symbols remain coverage issues, the canonical selector index still
stores declarations rather than every use, and Selector Audit does not infer static-only usage counts from it.

The scanner reads only explicitly supplied roots beneath an explicit project root. It does not execute project
code, annotation processors, build tools, helper methods, or static initializers. Generated Maven source trees are
excluded by default. The local index may contain selector values and should be treated as sensitive ignored build
output.

!!! note "Supported entry point"
    Use the [Test Engineering Studio Maven goal](../ai/test-engineering-studio-getting-started.md). There is no separate stable Selector Audit or Selector Lab CLI in 0.5.0.

## Generated-looking IDs are evidence, not a verdict

Frameworks and build tools commonly produce values such as counters, UUIDs, hashes, or random-looking suffixes.
For example:

```java
By.cssSelector("#react-select-17-input")
By.id("account-550e8400-e29b-41d4-a716-446655440000")
```

Text alone cannot establish whether `17` changes on a rerender or whether the UUID-like account identifier is an
intentional, stable test contract. Selector Intelligence therefore separates:

- **generated-looking** textual appearance;
- **observed variable** values from supplied, comparable evidence;
- **declared stable or unstable** project policy;
- **live validation** of syntax, match count, scope, and target identity.

An appearance detector's confidence is confidence that the text matches a known shape, not the probability that a
test will fail. Comparable evidence can show variation; unrelated datasets or builds are not silently treated as
proof of volatility.

### Project-specific stability policy

The implemented policy model lets trusted tooling represent the statement “this looks generated, but our project
treats it as stable.” Decisions are `STABLE` or `UNSTABLE` and use one of two bounded matcher forms:

- an exact strategy/value digest, without storing the raw value by default;
- a fully anchored structural pattern composed from literal, UUID, decimal, hexadecimal, or bounded opaque runs.

Rules can be scoped from a declaration or logical file/module through the project, with narrower scope taking
precedence. Explicit priority, exact-versus-structural matching, literal coverage, and placeholder count complete
the deterministic precedence order. Equal-precedence opposite decisions remain a visible conflict; file order does
not choose a winner.

The implemented paths are `.test-lens/selector-policies.json` for reviewed, tracked policy and
`target/test-lens/selector-policies.local.json` for ignored local-sensitive policy. They are internal tooling
formats in 0.4.x, not hand-authored public configuration. Do not invent a rule from this description: the trusted
tooling codec and host-side approval path own validation and writing.

A `STABLE` rule removes the appearance penalty within its scope. It does **not** prove uniqueness, correctness, or
future stability. If comparable evidence later shows change, Audit preserves both facts as a policy/evidence
conflict instead of hiding the observation.

## Selector Audit

Selector Audit is a declaration-first, browser-free view over the required static selector index and any optional
policy, history, similarity, pattern-preview, or in-memory candidate evidence supplied by trusted tooling.

One declaration produces one Audit entry, with separate component findings for composite locators. A finding keeps
its category, code, state, severity, reason codes, and evidence references. Severity is one of `INFO`, `REVIEW`,
`WARNING`, or `ERROR`; it is independent of detector confidence, correlation confidence, and report completeness.
There is no “bad selector percentage” or 0-100 selector quality score.

Interpret an Audit result as **finding + evidence + reason**:

- inspect coverage before treating absence of findings as complete;
- distinguish appearance evidence from observed variability;
- review policy conflicts rather than assuming a stable declaration wins;
- treat unresolved source or incomplete classpath findings as uncertainty, not proof of failure;
- require exact correlation before attaching current `WRONG_TARGET`, `NO_MATCH`, or `INVALID_SELECTOR` evidence to
  a declaration;
- use a recommendation as review guidance, never as authorization to edit source.

The canonical internal outputs are deterministic, sanitized
`target/test-lens/selector-audit/audit-v1.json` and an optional standalone `audit-v1.html` viewer. Audit itself does
not start a browser, generate candidates, save policy, modify source, or create a CI failure gate. Those paths are
documented for maintainers, not exposed by a public 0.4.x launcher.

## Find Similar

Find Similar answers “where else does this pattern occur?” over an explicitly supplied catalog. The catalog can
federate current static declarations, trusted local observations, compact history, and policy facts. A query does
not reparse the project or scan the filesystem.

Useful review cases include:

- locating declarations related to one brittle selector;
- finding the same exact locator or normalized template elsewhere;
- grouping generated-looking IDs with a supported structural relationship;
- estimating affected declarations and observed use sites before adopting a policy or changing application tests.

Relations are deterministic: same declaration, exact locator, same template, explicit structural pattern,
supported structural or prefix family, and appearance-only relation remain distinct. There is no fuzzy-text score.
**Similar does not mean same target**, interchangeable, or covered by the same policy.

History is optional, bounded local evidence at `target/test-lens/selector-history/history-v1.json`. It can aggregate
trusted run descriptors and comparable observations, but it does not persist raw selector values, DOM, screenshots,
WebElements, or full traces. Ordinary runtime-report locator strings remain redacted/untrusted for exact matching.
Static-only V1 does not persist general usage-site counts; those counts and samples require supplied runtime/history
evidence. Import order is not treated as chronology.

## Selector Lab: inspect before changing

Selector Lab is **read-only with respect to application test source in 0.4.x**. It is an internal browser workflow
opened only by an explicit internal call against a caller-owned WebDriver. There is no public `openLab()` method.

Inside its current frame and window, Lab can let a developer pick an element, inspect candidates in engine order,
see current validation and match counts, highlight retained matches, request Find Similar against a supplied
catalog, and preview a detector-backed policy pattern. It does not parse the project, invoke Git, rewrite a Page
Object, click the selected application element, or provide an Apply/Fix action.

Lab can prepare a pending policy change and send it back for separate trusted host approval. The browser UI never
writes policy files. A trusted tooling host must re-read both policy files, verify their digests and project
identity, require any necessary acknowledgement, and explicitly apply the change. Recommendation and inspection
therefore precede mutation.

Navigation or document replacement invalidates the current target and candidates. Lab works only in the current
frame/window, does not switch context, and cannot enter closed shadow roots.

## Add live browser evidence when static analysis is not enough

Static analysis is the first layer. Internal live candidate analysis can add evidence from the current browser
session:

```text
static declarations + policy + optional history
                    +
current target + current DOM + current search context
                    -> candidate generation and one bounded validation pass
                    -> match count, target comparison, reasons, limitations, recommendation
```

Candidate sources include the original locator, ID, preferred test attributes (`data-testid` by default), name,
individual class tokens, tag/class, tag, exact link text, exact-text XPath, and bounded stable-ancestor forms. ARIA
role and accessible name can contribute rationale, but the engine does not invent nonexistent Selenium `By.role`
or `By.accessibleName` locators.

Each executable candidate is resolved once with `findElements` in the supplied context. The result provides current
cardinality and same-target comparison. Analysis does not click, type, focus, scroll, navigate, perform a JavaScript
click, or mutate the application DOM. A current-session match is not a cross-run stability guarantee.

Live analysis complements browser-free Audit; it does not make static evidence obsolete. It answers questions that
source alone cannot, while static declaration identity, project policy, and history supply context that one DOM
snapshot cannot.

## Examples: separate facts from conclusions

### A. Positional CSS chain

```css
div:nth-child(7) > div:nth-child(2) > button
```

This is worth human review because it couples the test to document position. V1 can index a resolved
`By.cssSelector(...)` declaration, but its generated-looking detector does not decompose arbitrary compound CSS into
DOM attributes. Do not claim a specific Audit warning unless additional candidate evidence supplies one.

### B. Semantic test attribute

```css
[data-testid='checkout-submit']
```

This expresses an application-owned test convention and live candidate generation prefers configured test
attributes. It is still not automatically proven unique or permanent; current match count and team ownership remain
relevant evidence.

### C. Framework counter

```css
#react-select-17-input
```

The readable prefix plus framework-like counter can produce generated-looking appearance evidence. The correct next
step is to check policy, comparable observations, similar declarations, and—when available—the current DOM. The
shape alone does not prove that `17` changes.

### D. UUID-like but project-declared stable

```java
By.id("account-550e8400-e29b-41d4-a716-446655440000")
```

A UUID signal remains visible. A scoped exact `STABLE` policy can mark the appearance finding as covered, while
later contradictory comparable evidence remains a conflict. Shape alone does not decide stability in either
direction.

### E. A repeated structural family

Suppose review starts from `By.id("invoice-550e8400-e29b-41d4-a716-446655440000")`. A conservative UUID-backed
pattern proposal can be previewed against the complete supplied scope. Find Similar may expose related invoice
declarations and bounded historical subjects while excluding unsupported or merely appearance-related values. The
result estimates review scope; it does not assert that every match finds the same element.

## Typical workflow

Studio orchestrates the first six steps through reviewed project actions:

1. Trusted tooling builds the Java declaration index from an explicit project root, source roots, and optional
   classpath entries.
2. Selector Audit reports source coverage and high-signal findings without starting a browser.
3. Reviewed project knowledge can be represented as scoped `STABLE` or `UNSTABLE` policy through trusted tooling;
   generated-looking evidence remains visible.
4. Find Similar queries an already supplied catalog to estimate related declarations and evidence.
5. Selector Lab can inspect and highlight candidates in an explicitly opened live WebDriver session. It does not
   edit test code.
6. Live analysis verifies candidates only in the current context and DOM state.
7. A developer deliberately changes selectors in the application's own test source, reviews the diff, and reruns
   the relevant tests.
8. Re-scan and rerun the relevant tests to close the loop.

## What Selector Intelligence does not do

!!! warning "Evidence is not an oracle"
    - It does not guarantee future selector stability.
    - Static analysis does not know application semantics or current DOM behavior.
    - A UUID, hash, counter, or entropy-like suffix is not automatically unstable.
    - A stable policy does not override a wrong target, invalid selector, or no match.
    - Similar selectors are not necessarily interchangeable or attached to the same element.
    - Live evidence describes the observed session, context, and DOM state—not every application state.
    - Selector Lab does not silently rewrite source; Studio repair remains proposal-first and requires explicit trusted approval.
    - Audit does not automatically fail CI.
    - Kotlin source analysis and closed shadow roots are not supported in V1.
    - Published selector tooling remains outside the stable USER API unless a type is explicitly classified otherwise.

## Deeper implementation contracts

The user concepts above are implemented under strict internal boundaries. Contributors can continue with:

- [Static Java locator declaration index](../maintainers/selector-declaration-index.md)
- [Selector stability classification and policies](../maintainers/selector-stability-policies.md)
- [Selector similarity and cross-run history](../maintainers/selector-find-similar-history.md)
- [Offline Selector Audit](../maintainers/selector-audit.md)
- [Selector candidate live analysis](../maintainers/selector-candidate-live-analysis.md)
- [Read-only Selector Lab foundation](../maintainers/selector-lab-readonly.md)
