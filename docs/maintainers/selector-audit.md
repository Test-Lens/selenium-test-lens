# Offline Selector Audit

Test Lens 0.4.0 contains an internal, non-published Selector Audit foundation. It is an offline,
declaration-first view over the static selector index, stability policies, compact selector history,
similarity families, pattern previews, and optionally supplied in-memory candidate analyses. Audit never starts a
browser, generates or validates candidates, changes source, saves policy, or creates a CI failure gate. Normal and
FAST runtime execution are unaffected.

## Inputs and analysis modes

The selector index is required. Its streaming reader requires schema version 1, validates declaration and coverage
invariants, rejects duplicate fields and unknown semantic enum values, and applies hard JSON resource limits. A
malformed required index is fatal: Audit does not emit a report that could be mistaken for project coverage.

Policies, selector history, correlation data, pattern previews, and `CandidateAnalysis` objects are optional.
Candidate analyses are accepted in memory only; there is no candidate JSON format. Malformed optional inputs are
skipped with project issues and make the report partial. Known project-fingerprint mismatches are not merged.

`STATIC_ONLY` can be complete for the requested static inputs while runtime, history, policy, and candidate
dimensions remain `NOT_PROVIDED`. `ENRICHED_OFFLINE` adds supplied evidence without invoking Selenium. Neither mode
means fully live validated.

## Findings, evidence, and recommendations

One declaration produces one audit entry, with separate component findings for composite locators. Runtime/history
subjects that cannot be attached with sufficient confidence remain runtime-only entries. Repeated polling does not
produce repeated declaration rows.

Findings retain category, code, state, severity, component, reason codes, evidence references, policy rule IDs,
candidate IDs, and family references. Severity (`INFO`, `REVIEW`, `WARNING`, `ERROR`) is independent of detector
confidence, correlation confidence, and completeness. Generated-looking is not unstable. An applicable stable policy
keeps the appearance evidence but marks it covered; declared stable is not verified. Only comparable changed history
is observed variability.

Static symbol, classpath, or analyzer uncertainty is `REVIEW` and `INCOMPLETE`, not a proven broken selector.
Dynamic and custom declarations are not defects by themselves. Internal analysis failures are project issues.
Likewise, current `WRONG_TARGET`, `NO_MATCH`, or `INVALID_SELECTOR` errors require a direct `EXACT` attachment to the
original candidate identified by `originalCandidateId`. `STRONG` evidence remains limited support, and `AMBIGUOUS`
evidence can never create a declaration-level current failure.

Audit consumes candidate ordering and recommendations without reranking or strengthening them. `KEEP_CURRENT`,
`CONSIDER_REPLACEMENT`, `REVIEW_REQUIRED`, and `TARGET_REQUIRED` retain their candidate-engine meaning. No
recommendation authorizes a source change.

## Coverage and families

Coverage is part of report correctness. Static, runtime, history, policy, candidate, and output dimensions each have
their own state and counters. Failed files, unsupported languages such as Kotlin, incomplete classpaths, redacted or
summary-only observations, pruned history, rejected candidates, and output truncation remain prominent. The summary
does not count informational findings as defects and does not publish a “bad selector” percentage.

Exact/template/structural families are aggregated once using existing selector indexes. A family is not a shared
policy or proof of the same target. Detector-backed pattern proposals and caller-supplied previews are advisory and
never save policy. Static-only usage counts remain unavailable because the source index has no usage-site index.

## Sanitized deterministic outputs

The canonical output is `target/test-lens/selector-audit/audit-v1.json`; an optional standalone viewer is
`target/test-lens/selector-audit/audit-v1.html`. JSON is the source of truth and HTML renders the same model. The
viewer contains summary cards, prominent coverage, severity filters, declaration details, project issues, and family
summaries. It contains no network resources, Lab controls, browser bridge, apply action, or policy-save action.

Both formats are sanitized by default. They omit raw selector values, normalized/source expressions, source
snippets, runtime values, policy notes, test arguments, form values, WebElements, remote IDs, window handles, and
absolute workspace paths. They retain strategies, logical source locations, safe digests, detector metadata, rule
IDs, and reason codes. Digests are not encryption and low-entropy values remain susceptible to dictionary attacks.
All HTML text is escaped.

Output is deterministically ordered, contains no generation timestamp or random identifier, uses stable LF, and is
written through a sibling temporary file with atomic replacement where supported. The hard canonical JSON limit is
64 MiB. Supporting details are bounded first (32 findings per declaration while preserving warnings/errors, five
candidate alternatives, 100 family members, and eight samples). If the required core report still cannot fit, the
write fails with `OUTPUT_LIMIT_EXCEEDED` and preserves the previous output.
