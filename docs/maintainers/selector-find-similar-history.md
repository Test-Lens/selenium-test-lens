# Selector similarity and cross-run history

Test Lens 0.4.0 contains an internal, offline foundation for finding structurally related selector subjects and
aggregating bounded cross-run evidence. It is not Selector Audit or Selector Lab, does not save policies, and does
not suggest that related locators identify the same element or are interchangeable. Normal runtime and FAST do not
load this index, run the classifier, issue browser commands, or write history.

## Similarity and pattern proposals

Find Similar federates an explicitly supplied catalog of current static declarations, trusted local observations,
compact history, and policies. It never reparses the project or scans the filesystem for a query. Results preserve
identity/correlation, structural relation, appearance relation, scope proximity, policy applicability, and evidence
as separate dimensions. In particular, **similar does not mean same target**, and **similar does not mean that a
policy applies**.

The deterministic relation order is same declaration, exact locator (same canonical strategy and exact digest),
same template, same explicit structural pattern, same structural family with supporting scope, structural family,
prefix family, and appearance-only relation. Module or strategy alone is insufficient. There is no fuzzy text
distance, probability, or 0-100 similarity score. Explanations are derived from relation signals and correlation
reason codes.

Pattern proposals reuse detector code-point ranges and the existing structural-pattern language. V1 automatically
maps only UUID, sufficiently long decimal, and sufficiently long hexadecimal findings to bounded placeholders.
Overlaps prefer a supported, more specific and longer finding, so a UUID is not fragmented into hex and decimal
runs. Short numeric business IDs, entropy-only findings, and framework-looking text do not create `ANY`, regex, or
opaque wildcards. Proposal and exact analysis require `SOURCE_CANONICAL` or `RUNTIME_RAW_LOCAL` input.

Policy preview evaluates the proposed pattern over the complete caller-supplied scope, not merely previous similar
results. It reports matched static and historical subjects, excluded and unsupported digest-only subjects, policy
and evidence conflicts, affected declarations, bounded usage evidence, and incomplete-history state. Preview has no
policy-file side effect.

## Identity, correlation, and comparability

Each logical component has a content-derived `componentRef`; its role and structural path distinguish a primary
locator, class-token positions, ancestor components, and ordered composite children. A history subject prefers an
exact declaration plus component identity, then a strong template/symbol identity, then a strong usage/context
identity. Ambiguous or unknown observations remain isolated rather than being merged into false volatility.

Correlation confidence is `EXACT`, `STRONG`, `AMBIGUOUS`, or `UNKNOWN` and includes supporting, conflicting, and
missing-data reason codes. A source line alone is never exact. Declaration identity can change after moves, renames,
or structural edits; cross-revision template or family matches therefore retain lower confidence.

A caller supplies a `RunDescriptor` with logical test identity, invocation discriminator, attempt, and optional
dataset, test-source revision, system-under-test revision, environment, and trusted chronology. Dataset and
environment values are persisted only as domain-separated digests. The tool does not inspect framework arguments or
derive identity from a display/session name. Retry attempts remain part of one logical run family and are not
automatically independent `NEW_RUN` evidence.

Comparability is explicitly `COMPARABLE`, `NOT_COMPARABLE`, or `UNKNOWN`. A changed digest becomes observed
variability only for comparable observations. Different datasets are not comparable by default; unknown datasets
make dynamic cross-run comparison unknown; different builds remain a visible category rather than automatic
volatility. Differences in a declaration such as `By.id("row-" + rowId)` are expected dynamic values unless static
metadata proves a non-parameterized component changed. Generated-looking is not the same as unstable, and a
cross-run difference is not volatility without comparability.

## Runtime reports and trusted inputs

Ordinary runtime-report locator values are always imported as `RUNTIME_REDACTED` because the current report schema
does not prove whether redaction changed a value. The importer never creates exact-value digests, classifier
findings, structural-family fingerprints, policy matchers, or pattern proposals from those strings, even when they
look unredacted. It may retain strategy, safe context, usage source, outcome, count, and report provenance. A
`SUMMARY_ONLY` run has no detailed locator evidence and marks coverage incomplete.

Explicit trusted local observations can additionally produce exact digests, appearance findings, detector-backed
families, and comparable evidence. Report-byte digests provide idempotent import provenance; they are not semantic
run identity. Static-only declaration usage counts are unavailable in V1 because the selector source index has no
usage-site index. Usage counts and bounded samples come only from supplied runtime/history evidence.

## Compact local history

The default tooling location is `target/test-lens/selector-history/history-v1.json`. It is a local-sensitive artifact
and is neither source-controlled nor uploaded automatically. History is separate from the source index and policy
file. It contains schema/canonicalization/detector/family/correlation versions, optional project fingerprint, runs,
subjects, value-digest aggregates, structural-family shape, evidence-domain summaries, bounded samples, coverage,
and retention metadata. It does not contain raw selector values, full trace events, DOM, screenshots, WebElements,
remote IDs, window handles, or form values.

All new persisted fingerprints use distinct versioned domains, length-prefixed UTF-8 fields, SHA-256, and lowercase
hex. Domains cover exact values, fragments, rules/candidates, datasets, environments, projects, runs, templates,
families and prefixes, components, history subjects, samples/usages, and report imports. Digests reduce accidental
plaintext exposure but are not encryption; dictionary attacks remain possible for low-entropy values.

V1 provisional limits are 100 runs, 25,000 subjects, 100,000 observation buckets, 32 samples per subject, 16
distinct digests per subject, eight usage samples per declaration, and a hard 64 MiB serialized artifact. These are
independent maxima: the byte limit wins, so not every other maximum is necessarily reachable simultaneously.
Repeated polling increments an occurrence count rather than independent evidence. Pruning prefers redundant polls,
unchanged expendable samples, non-conflict buckets, and inactive subjects while retaining distinct-value, changed,
conflict, and ambiguity exemplars where the hard bound permits.

Import order is never chronology. Trusted sequence/time metadata may order equivalent retention candidates but is
not identity, similarity, or proof of comparability. Without it, pruning uses stable fingerprints and reports that
chronology was unavailable; it does not call that order oldest. Any forced loss marks history pruned and coverage
incomplete.

The selector-tooling streaming codec uses Jackson Core only, strict duplicate detection, bounded document/nesting/
token/string/number/entity counts, deterministic field order and LF output, project `target/` path containment, and
atomic sibling replacement when supported. The selector engine stays JDK-only, selector-live gains no dependency,
and no runtime API or hot path is changed.
